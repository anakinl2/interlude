package l2d.game.model.entity;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.ThreadPoolManager;
import l2d.game.idfactory.IdFactory;
import l2d.game.instancemanager.AuctionManager;
import l2d.game.instancemanager.ClanHallManager;
import l2d.game.instancemanager.PlayerMessageStack;
import l2d.game.model.L2Clan;
import l2d.game.model.L2ClanMember;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.entity.residence.ClanHall;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ClanTable;
import l2d.game.tables.ItemTable;

public class Auction
{
	protected static Logger _log = Logger.getLogger(Auction.class.getName());

	// =========================================================
	public static enum ItemTypeEnum
	{
		ClanHall
	}

	public static String[] ItemTypeName = { "ClanHall" };

	public static String getItemTypeName(final ItemTypeEnum value)
	{
		return ItemTypeName[value.ordinal()];
	}

	// =========================================================
	// Schedule Task
	public class AutoEndTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				final long timeRemaining = getTimeRemaining();
				if(timeRemaining > 1800000) // 30 mins or more
					ThreadPoolManager.getInstance().scheduleGeneral(new AutoEndTask(), timeRemaining - 1800000);
				else if(timeRemaining > 600000) // 30 - 10 mins
					ThreadPoolManager.getInstance().scheduleGeneral(new AutoEndTask(), 60000);
				else if(timeRemaining > 0) // 10 - 0 mins
					ThreadPoolManager.getInstance().scheduleGeneral(new AutoEndTask(), 5000);
				else
					endAuction();
			}
			catch(final Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void StartAutoTask(final boolean forced)
	{
		correctAuctionTime(forced);
		ThreadPoolManager.getInstance().scheduleGeneral(new AutoEndTask(), 1000);
	}

	private void correctAuctionTime(final boolean forced)
	{
		boolean corrected = false;

		if(_EndDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() || forced)
		{
			// Since auction has past reschedule it to the next one (7 days)
			// This is usually caused by server being down
			corrected = true;
			if(forced)
				setNextAuctionDate();
			else
				endAuction(); // end auction normally in case it had bidders and server was down when it ended
		}

		_EndDate.set(Calendar.MINUTE, 0);
		_EndDate.set(Calendar.SECOND, 0);
		_EndDate.set(Calendar.MILLISECOND, 0);

		if(corrected)
			saveAuctionDate();
	}

	private void setNextAuctionDate()
	{
		while(_EndDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
			// Set next auction date if auction has passed
			_EndDate.add(Calendar.DAY_OF_MONTH, 7); // Schedule to happen in 7 days
	}

	private void saveAuctionDate()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE auction SET endDate = ? WHERE id = ?");
			statement.setLong(1, _EndDate.getTimeInMillis());
			statement.setInt(2, _Id);
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.log(Level.SEVERE, "Exception: saveAuctionDate(): " + e.getMessage(), e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public class Bidder
	{
		private String _Name;
		private String _ClanName;
		private int _Bid;
		private Calendar _timeBid;

		public Bidder(final String name, final String clanName, final int bid, final long timeBid)
		{
			_Name = name;
			_ClanName = clanName;
			_Bid = bid;
			_timeBid = Calendar.getInstance();
			_timeBid.setTimeInMillis(timeBid);
		}

		public String getName()
		{
			return _Name;
		}

		public String getClanName()
		{
			return _ClanName;
		}

		public int getBid()
		{
			return _Bid;
		}

		public Calendar getTimeBid()
		{
			return _timeBid;
		}

		public void setTimeBid(final long timeBid)
		{
			_timeBid.setTimeInMillis(timeBid);
		}

		public void setBid(final int bid)
		{
			_Bid = bid;
		}
	}

	// =========================================================
	// Data Field
	private int _Id = 0;

	private final static int ADENA_ID = 57;

	private Calendar _EndDate;

	private int _HighestBidderId = 0;
	private String _HighestBidderName = "";
	private int _HighestBidderMaxBid = 0;

	private int _ItemId = 0;
	private String _ItemName = "";
	private int _ItemObjectId = 0;
	private int _ItemQuantity = 0;
	private String _ItemType = "";

	private int _SellerId = 0;
	private String _SellerClanName = "";
	private String _SellerName = "";

	private int _CurrentBid = 0;
	private int _StartingBid = 0;

	private Map<Integer, Bidder> _bidders = new FastMap<Integer, Bidder>();

	// =========================================================
	// Constructor
	public Auction(final int auctionId)
	{
		_Id = auctionId;
		load();

		// end auction automatically
		StartAutoTask(false);
	}

	public Auction(final int itemId, final L2Clan Clan, final long delay, final int bid, final String name)
	{
		_Id = itemId;
		_EndDate = Calendar.getInstance();
		_EndDate.setTimeInMillis(Calendar.getInstance().getTimeInMillis() + delay);
		_EndDate.set(Calendar.MINUTE, 0);
		_ItemId = itemId;
		_ItemName = name;
		_ItemType = "ClanHall";
		_SellerId = Clan.getLeaderId();
		_SellerName = Clan.getLeaderName();
		_SellerClanName = Clan.getName();
		_StartingBid = bid;
	}

	// =========================================================
	// Method - Public
	public void setBid(final L2Player bidder, final int bid)
	{
		if(!CanBid(bidder))
			return;

		if(bid > 2100000000)
		{
			bidder.sendPacket(new SystemMessage(SystemMessage.YOUR_BID_CANNOT_EXCEED_2_1_BILLION));
			return;
		}
		if(bid <= getHighestBidderMaxBid() || bid <= getStartingBid())
		{
			bidder.sendPacket(new SystemMessage(SystemMessage.YOUR_BID_MUST_BE_HIGHER_THAN_THE_CURRENT_HIGHEST_BID));
			return;
		}

		int requiredAdena = bid;
		// Update bid if new bid is higher
		if(_bidders.get(bidder.getClanId()) != null)
		{
			requiredAdena = bid - _bidders.get(bidder.getClanId()).getBid();
			if(requiredAdena < 1)
			{
				bidder.sendPacket(new SystemMessage(SystemMessage.THE_SECOND_BID_AMOUNT_MUST_BE_HIGHER_THAN_THE_ORIGINAL));
				return;
			}
		}

		final long timeRemaining = getTimeRemaining();
		if(timeRemaining < 10000)
		{
			bidder.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_PARTICIPATE_IN_AN_AUCTION));
			return;
		}

		if(takeItem(bidder, requiredAdena))
		{
			if(_bidders.size() > 0 && timeRemaining < 120000) // за 2 минуты до конца
			{
				_EndDate.add(Calendar.MINUTE, 5);
				bidder.sendPacket(new SystemMessage(SystemMessage.BIDDER_EXISTS__THE_AUCTION_TIME_HAS_BEEN_EXTENDED_BY_5_MINUTES));
			}
			else if(_bidders.size() > 0 && timeRemaining < 300000) // за 5 минут до конца
			{
				_EndDate.add(Calendar.MINUTE, 3);
				bidder.sendPacket(new SystemMessage(SystemMessage.BIDDER_EXISTS__AUCTION_TIME_HAS_BEEN_EXTENDED_BY_3_MINUTES));
			}

			updateInDB(bidder, bid);
			bidder.getClan().setAuctionBiddedAt(_Id);
		}
	}

	private int getClanHallGrade()
	{
		final ClanHall ch = ClanHallManager.getInstance().getClanHall(getItemId());
		return ch == null ? 0 : ch.getGrade();
	}

	private int getMinClanLevel()
	{
		final int grade = getClanHallGrade();
		if(grade == 1)
			return Config.CH_BID_GRADE1_MINCLANLEVEL;
		if(grade == 2)
			return Config.CH_BID_GRADE2_MINCLANLEVEL;
		if(grade == 3)
			return Config.CH_BID_GRADE3_MINCLANLEVEL;
		return 2;
	}

	private int getMinClanMembers()
	{
		final int grade = getClanHallGrade();
		if(grade == 1)
			return Config.CH_BID_GRADE1_MINCLANMEMBERS;
		if(grade == 2)
			return Config.CH_BID_GRADE2_MINCLANMEMBERS;
		if(grade == 3)
			return Config.CH_BID_GRADE3_MINCLANMEMBERS;
		return 1;
	}

	private int getMinClanMembersAvgLevel()
	{
		final int grade = getClanHallGrade();
		if(grade == 1)
			return Config.CH_BID_GRADE1_MINCLANMEMBERSLEVEL;
		if(grade == 2)
			return Config.CH_BID_GRADE2_MINCLANMEMBERSLEVEL;
		if(grade == 3)
			return Config.CH_BID_GRADE3_MINCLANMEMBERSLEVEL;
		return 1;
	}

	// =========================================================
	// Method - Private
	private void load()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rs = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM auction WHERE id = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();

			while(rs.next())
			{
				_CurrentBid = rs.getInt("currentBid");
				_EndDate = Calendar.getInstance();
				_EndDate.setTimeInMillis(rs.getLong("endDate"));
				_ItemId = rs.getInt("itemId");
				_ItemName = rs.getString("itemName");
				_ItemObjectId = rs.getInt("itemObjectId");
				_ItemType = rs.getString("itemType");
				_SellerId = rs.getInt("sellerId");
				_SellerClanName = rs.getString("sellerClanName");
				_SellerName = rs.getString("sellerName");
				_StartingBid = rs.getInt("startingBid");
			}

			if(getTimeRemaining() < -1800000)
			{
				// если аукцион закончился более чем пол часа назад значит сервер лежал, потому выставляем конец аукциона на сутки после запуска
				_EndDate = Calendar.getInstance();
				_EndDate.add(Calendar.HOUR, 24);
			}
			else if(getTimeRemaining() < 1800000)
			{
				// если аукцион закончился или закончится менее чем через пол часа то был недолгий рестарт, потому выставляем конец аукциона на пол часа после запуска
				_EndDate = Calendar.getInstance();
				_EndDate.add(Calendar.MINUTE, 30);
			}
			_EndDate.set(Calendar.MINUTE, 0);
			_EndDate.set(Calendar.SECOND, 0);
			_EndDate.set(Calendar.MILLISECOND, 0);

			loadBid();
		}
		catch(final Exception e)
		{
			System.out.println("Exception: Auction.load(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rs);
		}
	}

	private void loadBid()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rs = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT bidderId, bidderName, maxBid, clan_name, time_bid FROM auction_bid WHERE auctionId = ? ORDER BY maxBid DESC");
			statement.setInt(1, getId());
			rs = statement.executeQuery();

			while(rs.next())
			{
				if(rs.isFirst())
				{
					_HighestBidderId = rs.getInt("bidderId");
					_HighestBidderName = rs.getString("bidderName");
					_HighestBidderMaxBid = rs.getInt("maxBid");
				}
				else if(rs.getInt("maxBid") > _HighestBidderMaxBid)
				{
					_HighestBidderId = rs.getInt("bidderId");
					_HighestBidderName = rs.getString("bidderName");
					_HighestBidderMaxBid = rs.getInt("maxBid");
				}
				_bidders.put(rs.getInt("bidderId"), new Bidder(rs.getString("bidderName"), rs.getString("clan_name"), rs.getInt("maxBid"), rs.getLong("time_bid")));
			}

		}
		catch(final Exception e)
		{
			System.out.println("Exception: Auction.loadBid(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rs);
		}
	}

	private void returnItem(final String Clan, final int quantity, final boolean penalty)
	{
		final L2Clan clan = ClanTable.getInstance().getClanByName(Clan);
		returnItem(clan, quantity, penalty);
	}

	private void returnItem(final L2Clan clan, int quantity, final boolean penalty)
	{
		if(clan == null)
			return;
		if(penalty)
			quantity *= 0.9; // take 10% tax fee if needed
		final L2ItemInstance item = ItemTable.getInstance().createItem(ADENA_ID);
		item.setCount(quantity);
		clan.getWarehouse().addItem(item, null);
	}

	private boolean takeItem(final L2Player bidder, final int quantity)
	{
		// Take item from bidder
		/*
		 * if (this.getItemType()== getItemTypeName(ItemTypeEnum.ClanHall))
		 * {
		 */
		// Take item from clan warehouse
		if(bidder.getClan() != null && bidder.getClan().getAdena().getIntegerLimitedCount() >= quantity)
		{
			bidder.getClan().getWarehouse().destroyItem(ADENA_ID, quantity);
			return true;
		}

		if(bidder.getAdena() >= quantity)
		{
			bidder.reduceAdena(quantity);
			return true;
		}

		bidder.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_ADENA_FOR_THIS_BID));
		return false;
	}

	private void updateInDB(final L2Player bidder, final int bid)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			if(getBidders().get(bidder.getClanId()) != null)
			{
				statement = con.prepareStatement("UPDATE auction_bid SET bidderId=?, bidderName=?, maxBid=?, time_bid=? WHERE auctionId=? AND bidderId=?");
				statement.setInt(1, bidder.getClanId());
				statement.setString(2, bidder.getClan().getLeaderName());
				statement.setInt(3, bid);
				statement.setLong(4, Calendar.getInstance().getTimeInMillis());
				statement.setInt(5, getId());
				statement.setInt(6, bidder.getClanId());
				statement.execute();
			}
			else
			{
				statement = con.prepareStatement("INSERT INTO auction_bid (id, auctionId, bidderId, bidderName, maxBid, clan_name, time_bid) VALUES (?, ?, ?, ?, ?, ?, ?)");
				statement.setInt(1, IdFactory.getInstance().getNextId());
				statement.setInt(2, getId());
				statement.setInt(3, bidder.getClanId());
				statement.setString(4, bidder.getName());
				statement.setInt(5, bid);
				statement.setString(6, bidder.getClan().getName());
				statement.setLong(7, Calendar.getInstance().getTimeInMillis());
				statement.execute();

				if(_HighestBidderId > 0)
					PlayerMessageStack.getInstance().mailto(_HighestBidderId, new SystemMessage(SystemMessage.YOU_HAVE_BEEN_OUTBID));
			}

			// Update internal var
			if(bid > _HighestBidderMaxBid)
			{
				_HighestBidderId = bidder.getClanId();
				_HighestBidderMaxBid = bid;
				_HighestBidderName = bidder.getClan().getLeaderName();
			}
			if(_bidders.get(bidder.getClanId()) == null)
				_bidders.put(bidder.getClanId(), new Bidder(bidder.getClan().getLeaderName(), bidder.getClan().getName(), bid, Calendar.getInstance().getTimeInMillis()));
			else
			{
				_bidders.get(bidder.getClanId()).setBid(bid);
				_bidders.get(bidder.getClanId()).setTimeBid(Calendar.getInstance().getTimeInMillis());
			}

			bidder.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_SUBMITTED_A_BID_IN_THE_AUCTION_OF_S1).addString(getItemName()));
			if(_SellerId > 0)
				PlayerMessageStack.getInstance().mailto(_SellerId, new SystemMessage(SystemMessage.YOU_HAVE_BID_IN_A_CLAN_HALL_AUCTION));
		}
		catch(final Exception e)
		{
			_log.log(Level.SEVERE, "Exception: Auction.updateInDB(L2Player bidder, int bid): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	private void removeBids()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=?");
			statement.setInt(1, getId());
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.log(Level.SEVERE, "Exception: Auction.deleteFromDB(): " + e.getMessage(), e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		for(final Bidder bidder : _bidders.values())
			try
			{
				final L2Clan bidder_clan = ClanTable.getInstance().getClanByName(bidder.getClanName());
				if(bidder_clan == null)
					continue;

				if(bidder_clan.getHasHideout() == 0)
				{
					returnItem(bidder_clan, bidder.getBid(), true); // 10 % tax
					final String s = "You haven't won ClanHall " + getItemName() + ". Your bid returned";
					PlayerMessageStack.getInstance().mailto(bidder_clan.getLeaderId(), SystemMessage.sendString(s));
				}
				bidder_clan.setAuctionBiddedAt(0);
			}
			catch(final Exception e)
			{
				e.printStackTrace();
			}
		_bidders.clear();
	}

	private void deleteAuctionFromDB()
	{
		AuctionManager.getInstance().getAuctions().remove(this);
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM auction WHERE itemId=?");
			statement.setInt(1, _ItemId);
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.log(Level.SEVERE, "Exception: Auction.deleteFromDB(): " + e.getMessage(), e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void endAuction()
	{
		final SystemMessage announce_msg = new SystemMessage(SystemMessage.S1_S_AUCTION_HAS_ENDED).addString(getItemName());
		for(final L2Player player : L2World.getAllPlayers())
			if(player != null)
				player.sendPacket(announce_msg);

		if(_HighestBidderId == 0 && _SellerId == 0)
		{
			StartAutoTask(true);
			return;
		}
		if(_HighestBidderId == 0 && _SellerId > 0)
		{
			deleteAuctionFromDB();
			PlayerMessageStack.getInstance().mailto(_SellerId, new SystemMessage(SystemMessage.THE_CLAN_HALL_WHICH_HAD_BEEN_PUT_UP_FOR_AUCTION_WAS_NOT_SOLD_AND_THEREFORE_HAS_BEEN_RELISTED));
			return;
		}

		final ClanHall ch = ClanHallManager.getInstance().getClanHall(getItemId());
		if(ch == null)
			_log.warning("ClanHall is null for id " + _ItemId + ". WTF?");

		L2Clan HighestBidderClan = null;
		if(_bidders.get(_HighestBidderId) == null)
			_log.warning("Bidder with id " + _HighestBidderId + "is null. WTF?");
		else
		{
			HighestBidderClan = ClanTable.getInstance().getClanByName(_bidders.get(_HighestBidderId).getClanName());
			if(HighestBidderClan == null)
				_log.warning("Clan with name " + _bidders.get(_HighestBidderId).getClanName() + "is null. WTF?");
		}

		if(ch != null && HighestBidderClan != null)
		{
			if(_SellerId > 0)
			{
				returnItem(_SellerClanName, _HighestBidderMaxBid, true);
				returnItem(_SellerClanName, ch.getLease(), false);
				PlayerMessageStack.getInstance().mailto(_SellerId, new SystemMessage(SystemMessage.THE_CLAN_HALL_WHICH_WAS_PUT_UP_FOR_AUCTION_HAS_BEEN_AWARDED_TO_S1_CLAN).addString(HighestBidderClan.getName()));
			}

			ch.changeOwner(HighestBidderClan);
			final String s = "Congratulation! You have won ClanHall " + getItemName() + ". " + ch.getDesc();
			PlayerMessageStack.getInstance().mailto(HighestBidderClan.getLeaderId(), SystemMessage.sendString(s));
		}
		deleteAuctionFromDB();
		removeBids();
	}

	public void cancelBid(final int bidder)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=? AND bidderId=?");
			statement.setInt(1, getId());
			statement.setInt(2, bidder);
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.log(Level.SEVERE, "Exception: Auction.cancelBid(String bidder): " + e.getMessage(), e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		returnItem(_bidders.get(bidder).getClanName(), _bidders.get(bidder).getBid(), true);
		ClanTable.getInstance().getClanByName(_bidders.get(bidder).getClanName()).setAuctionBiddedAt(0);
		_bidders.remove(bidder);
	}

	public void cancelAuction()
	{
		deleteAuctionFromDB();
		removeBids();
	}

	public void confirmAuction()
	{
		AuctionManager.getInstance().getAuctions().add(this);
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO auction (id, sellerId, sellerName, sellerClanName, itemType, itemId, itemObjectId, itemName, itemQuantity, startingBid, currentBid, endDate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, getId());
			statement.setInt(2, _SellerId);
			statement.setString(3, _SellerName);
			statement.setString(4, _SellerClanName);
			statement.setString(5, _ItemType);
			statement.setInt(6, _ItemId);
			statement.setInt(7, _ItemObjectId);
			statement.setString(8, _ItemName);
			statement.setInt(9, _ItemQuantity);
			statement.setInt(10, _StartingBid);
			statement.setInt(11, _CurrentBid);
			statement.setLong(12, _EndDate.getTimeInMillis());
			statement.execute();
			loadBid();
		}
		catch(final Exception e)
		{
			_log.log(Level.SEVERE, "Exception: Auction.confirmAuction(): " + e.getMessage(), e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	// =========================================================
	// Property
	public final int getId()
	{
		return _Id;
	}

	public final int getCurrentBid()
	{
		return _CurrentBid;
	}

	public final Calendar getEndDate()
	{
		return _EndDate;
	}

	public final int getHighestBidderId()
	{
		return _HighestBidderId;
	}

	public final String getHighestBidderName()
	{
		return _HighestBidderName;
	}

	public final int getHighestBidderMaxBid()
	{
		return _HighestBidderMaxBid;
	}

	public final int getItemId()
	{
		return _ItemId;
	}

	public final String getItemName()
	{
		return _ItemName;
	}

	public final int getItemObjectId()
	{
		return _ItemObjectId;
	}

	public final int getItemQuantity()
	{
		return _ItemQuantity;
	}

	public final String getItemType()
	{
		return _ItemType;
	}

	public final int getSellerId()
	{
		return _SellerId;
	}

	public final String getSellerName()
	{
		return _SellerName;
	}

	public final String getSellerClanName()
	{
		return _SellerClanName;
	}

	public final int getStartingBid()
	{
		return _StartingBid;
	}

	public final Map<Integer, Bidder> getBidders()
	{
		return _bidders;
	}

	public final boolean CanBid(final L2Player bidder)
	{
		final L2Clan bidder_clan = bidder.getClan();

		if(bidder_clan == null || bidder_clan.getLeaderId() != bidder.getObjectId() || bidder_clan.getLevel() < getMinClanLevel())
		{
			if(getMinClanLevel() == 2)
				bidder.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
			else
				bidder.sendMessage(new CustomMessage("l2d.game.model.entity.Auction.MinClanLevel", bidder).addNumber(getMinClanLevel()));
			return false;
		}

		if(bidder_clan.getHasHideout() > 0)
		{
			bidder.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_PARTICIPATE_IN_AN_AUCTION));
			return false;
		}

		if(bidder_clan.getAuctionBiddedAt() > 0 && bidder_clan.getAuctionBiddedAt() != getId())
		{
			bidder.sendPacket(new SystemMessage(SystemMessage.SINCE_YOU_HAVE_ALREADY_SUBMITTED_A_BID_YOU_ARE_NOT_ALLOWED_TO_PARTICIPATE_IN_ANOTHER_AUCTION_AT_THIS_TIME));
			return false;
		}

		for(final Auction auction : AuctionManager.getInstance().getAuctions())
			if(!equals(auction) && auction._bidders.containsKey(bidder_clan.getClanId()))
			{
				bidder.sendPacket(new SystemMessage(SystemMessage.SINCE_YOU_HAVE_ALREADY_SUBMITTED_A_BID_YOU_ARE_NOT_ALLOWED_TO_PARTICIPATE_IN_ANOTHER_AUCTION_AT_THIS_TIME));
				return false;
			}

		if(bidder_clan.getMembersCount() < getMinClanMembers())
		{
			bidder.sendMessage(new CustomMessage("l2d.game.model.entity.Auction.MinClanMembers", bidder).addNumber(getMinClanMembers()));
			return false;
		}

		if(getMinClanMembersAvgLevel() > 1)
		{
			float avg_level = 0;
			int avg_level_count = 0;
			for(final L2ClanMember member : bidder_clan.getMembers())
				if(member != null)
				{
					avg_level += member.getLevel();
					avg_level_count++;
				}

			avg_level /= avg_level_count;
			if(avg_level < getMinClanMembersAvgLevel())
			{
				bidder.sendMessage(new CustomMessage("l2d.game.model.entity.Auction.MinClanMembersAvgLevel", bidder).addNumber(getMinClanMembersAvgLevel()).addNumber((int) Math.ceil(avg_level)));
				return false;
			}
		}

		return true;
	}

	public long getTimeRemaining()
	{
		return getEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
	}
}