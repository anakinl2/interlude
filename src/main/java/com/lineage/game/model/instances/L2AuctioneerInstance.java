package com.lineage.game.model.instances;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.instancemanager.AuctionManager;
import com.lineage.game.instancemanager.ClanHallManager;
import com.lineage.game.instancemanager.ServerVariables;
import com.lineage.game.instancemanager.TownManager;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.Auction;
import com.lineage.game.model.entity.Auction.Bidder;
import com.lineage.game.model.entity.residence.ClanHall;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.MapRegion;
import com.lineage.game.templates.L2NpcTemplate;

public final class L2AuctioneerInstance extends L2NpcInstance
{
	//private static Logger _log = Logger.getLogger(L2AuctioneerInstance.class.getName());

	private static int Cond_All_False = 0;
	private static int Cond_Busy_Because_Of_Siege = 1;
	private static int Cond_Regular = 3;

	private Map<Integer, Auction> _pendingAuctions = new FastMap<Integer, Auction>();

	public L2AuctioneerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(!isInRange(player, INTERACTION_DISTANCE))
			return;
		player.sendActionFailed();

		int condition = validateCondition();
		if(condition == Cond_All_False)
		{
			//TODO: html
			player.sendMessage("Wrong conditions.");
			return;
		}
		if(condition == Cond_Busy_Because_Of_Siege)
		{
			player.sendPacket(new SystemMessage(SystemMessage.IT_IS_NOT_AN_AUCTION_PERIOD));
			return;
		}
		else if(condition == Cond_Regular)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command

			String val = "";
			if(st.countTokens() >= 1)
				val = st.nextToken();

			if(actualCommand.equalsIgnoreCase("auction"))
			{
				if(val.equals(""))
					return;

				try
				{
					int days = Integer.parseInt(val);
					try
					{
						int bid = 0;
						if(st.countTokens() >= 1)
							bid = Integer.parseInt(st.nextToken());

						Auction a = new Auction(player.getClan().getHasHideout(), player.getClan(), days * 86400000, bid, player.getClanHall().getName());
						if(_pendingAuctions.get(a.getId()) != null)
							_pendingAuctions.remove(a.getId());

						_pendingAuctions.put(a.getId(), a);

						String filename = "data/html/auction/AgitSale3.htm";
						NpcHtmlMessage html = new NpcHtmlMessage(player, this);
						html.setFile(filename);
						html.replace("%x%", val);
						html.replace("%AGIT_AUCTION_END_YY%", String.valueOf(a.getEndDate().get(Calendar.YEAR)));
						html.replace("%AGIT_AUCTION_END_MM%", String.valueOf(a.getEndDate().get(Calendar.MONTH) + 1));
						html.replace("%AGIT_AUCTION_END_DD%", String.valueOf(a.getEndDate().get(Calendar.DAY_OF_MONTH)));
						html.replace("%AGIT_AUCTION_END_HH%", String.valueOf(a.getEndDate().get(Calendar.HOUR_OF_DAY)));
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_MIN%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_DESC%", player.getClanHall().getDesc());
						player.sendPacket(html);
					}
					catch(Exception e)
					{
						player.sendMessage("Invalid bid!");
					}
				}
				catch(Exception e)
				{
					player.sendMessage("Invalid auction duration!");
				}
				return;
			}
			if(actualCommand.equalsIgnoreCase("confirmAuction"))
			{
				try
				{
					Auction a = _pendingAuctions.get(player.getClan().getHasHideout());
					ClanHall ch = ClanHallManager.getInstance().getClanHall(a.getItemId());
					if(ch.getLease() == 0)
						throw new Exception();
					a.confirmAuction();
					_pendingAuctions.remove(player.getClan().getHasHideout());
				}
				catch(Exception e)
				{
					player.sendMessage("Invalid auction");
				}
				return;
			}
			else if(actualCommand.equalsIgnoreCase("bidding"))
			{
				if(val.equals(""))
					return;
				if(Config.DEBUG)
					player.sendMessage("bidding show successful");

				try
				{
					int auctionId = Integer.parseInt(val);
					if(Config.DEBUG)
						player.sendMessage("auction test started");
					String filename = "data/html/auction/AgitAuctionInfo.htm";
					Auction a = AuctionManager.getInstance().getAuction(auctionId);

					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile(filename);
					html.replace("%AGIT_NAME%", a.getItemName());
					html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
					html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
					html.replace("%AGIT_SIZE%", "30 ");
					html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHall(a.getItemId()).getLease()));
					html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHall(a.getItemId()).getLocation());
					html.replace("%AGIT_AUCTION_END_YY%", String.valueOf(a.getEndDate().get(Calendar.YEAR)));
					html.replace("%AGIT_AUCTION_END_MM%", String.valueOf(a.getEndDate().get(Calendar.MONTH) + 1));
					html.replace("%AGIT_AUCTION_END_DD%", String.valueOf(a.getEndDate().get(Calendar.DAY_OF_MONTH)));
					html.replace("%AGIT_AUCTION_END_HH%", String.valueOf(a.getEndDate().get(Calendar.HOUR_OF_DAY)));
					html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 3600000) + " hours " + String.valueOf(((a.getEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 60000 % 60)) + " minutes");
					html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
					html.replace("%AGIT_AUCTION_COUNT%", String.valueOf(a.getBidders().size()));
					html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHall(a.getItemId()).getDesc());
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_list");
					html.replace("%AGIT_LINK_BIDLIST%", "bypass -h npc_" + getObjectId() + "_bidlist " + a.getId());
					html.replace("%AGIT_LINK_RE%", "bypass -h npc_" + getObjectId() + "_bid1 " + a.getId());
					player.sendPacket(html);
				}
				catch(Exception e)
				{
					player.sendMessage("Invalid auction!");
				}

				return;
			}
			else if(actualCommand.equalsIgnoreCase("bid"))
			{
				if(val.equals(""))
					return;

				try
				{
					Auction auction = AuctionManager.getInstance().getAuction(Integer.parseInt(val));
					if(auction == null)
					{
						player.sendMessage("Invalid auction!");
						return;
					}
					try
					{
						int bid = 0;
						if(st.countTokens() >= 1)
							bid = Integer.parseInt(st.nextToken());
						auction.setBid(player, bid);
					}
					catch(Exception e)
					{
						player.sendMessage("Invalid bid!");
					}
				}
				catch(Exception e)
				{
					player.sendMessage("Invalid auction!");
				}

				return;
			}
			else if(actualCommand.equalsIgnoreCase("bid1"))
			{
				if(val.equals(""))
					return;

				Auction auction;
				try
				{
					auction = AuctionManager.getInstance().getAuction(Integer.parseInt(val));
				}
				catch(Exception E)
				{
					player.sendMessage("Invalid auction!");
					return;
				}

				if(auction == null)
				{
					player.sendMessage("Invalid auction!");
					return;
				}

				if(!auction.CanBid(player))
					return;

				try
				{
					String filename = "data/html/auction/AgitBid1.htm";

					int minimumBid = auction.getHighestBidderMaxBid();
					if(minimumBid == 0)
						minimumBid = auction.getStartingBid();

					L2ItemInstance adena = player.getClan().getAdena();

					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile(filename);
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_bidding " + val);
					html.replace("%PLEDGE_ADENA%", String.valueOf(adena == null ? 0 : adena.getCount()));
					html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(minimumBid));
					html.replace("npc_%objectId%_bid", "npc_" + getObjectId() + "_bid " + val);
					player.sendPacket(html);
					return;
				}
				catch(Exception e)
				{
					player.sendMessage("Invalid auction!");
				}
				return;
			}
			else if(actualCommand.equalsIgnoreCase("list"))
			{
				//char spec = '"';
				//if(Config.DEBUG) player.sendMessage("cmd list: auction test started");
				String items = "";
				List<Auction> auctions = AuctionManager.getInstance().getAuctions();
				String here = TownManager.getInstance().getClosestTown(player).getName();
				for(Auction a : auctions)
				{
					String loc = ClanHallManager.getInstance().getClanHall(a.getItemId()).getLocation();
					if(!here.contains(loc))
						continue;

					items += "<tr>" + "<td>" + ClanHallManager.getInstance().getClanHall(a.getItemId()).getLocation() + "</td><td><a action=\"bypass -h npc_" + getObjectId() + "_bidding " + a.getId() + "\">" + a.getItemName() + "</a></td><td>" + a.getEndDate().get(Calendar.YEAR) + "/" + (a.getEndDate().get(Calendar.MONTH) + 1) + "/" + a.getEndDate().get(Calendar.DATE) + "</td><td>" + a.getStartingBid() + "</td>" + "</tr>";
				}
				String filename = "data/html/auction/AgitAuctionList.htm";

				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile(filename);
				html.replace("%location%", here);
				html.replace("%itemsField%", items);
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				player.sendPacket(html);
				return;
			}
			else if(actualCommand.equalsIgnoreCase("bidlist"))
			{
				int auctionId;
				if(val.equals(""))
				{
					if(player.getClan().getAuctionBiddedAt() <= 0)
						return;
					auctionId = player.getClan().getAuctionBiddedAt();
				}
				else
					auctionId = Integer.parseInt(val);
				if(Config.DEBUG)
					player.sendMessage("cmd bidlist: auction test started");
				String biders = "";
				Map<Integer, Bidder> bidders = AuctionManager.getInstance().getAuction(auctionId).getBidders();
				for(Bidder b : bidders.values())
					biders += "<tr>" + "<td>" + b.getClanName() + "</td><td>" + b.getTimeBid().get(Calendar.YEAR) + "/" + (b.getTimeBid().get(Calendar.MONTH) + 1) + "/" + b.getTimeBid().get(Calendar.DATE) + "</td><td>" + b.getBid() + "</td>" + "</tr>";
				String filename = "data/html/auction/AgitBidderList.htm";

				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile(filename);
				html.replace("%AGIT_LIST%", biders);
				html.replace("%x%", val);
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_list");
				player.sendPacket(html);
				return;
			}
			else if(actualCommand.equalsIgnoreCase("selectedItems"))
			{
				if(player.getClan() != null && player.getClan().getHasHideout() == 0 && player.getClan().getAuctionBiddedAt() > 0)
				{
					String filename = "data/html/auction/AgitBidInfo.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile(filename);
					Auction a = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt());
					if(a == null)
					{
						player.sendActionFailed();
						return;
					}
					html.replace("%AGIT_NAME%", a.getItemName());
					html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
					html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
					html.replace("%AGIT_SIZE%", "30 ");
					html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHall(a.getItemId()).getLease()));
					html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHall(a.getItemId()).getLocation());
					html.replace("%AGIT_AUCTION_END_YY%", String.valueOf(a.getEndDate().get(Calendar.YEAR)));
					html.replace("%AGIT_AUCTION_END_MM%", String.valueOf(a.getEndDate().get(Calendar.MONTH) + 1));
					html.replace("%AGIT_AUCTION_END_DD%", String.valueOf(a.getEndDate().get(Calendar.DAY_OF_MONTH)));
					html.replace("%AGIT_AUCTION_END_HH%", String.valueOf(a.getEndDate().get(Calendar.HOUR_OF_DAY)));
					html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 3600000) + " hours " + String.valueOf(((a.getEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 60000 % 60)) + " minutes");
					html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
					if(a.getBidders() != null && a.getBidders().get(player.getClanId()) != null)
						html.replace("%AGIT_AUCTION_MYBID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
					else
						html.replace("%AGIT_AUCTION_MYBID%", "");
					html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHall(a.getItemId()).getDesc());
					player.sendPacket(html);
					return;
				}
				else if(player.getClan() != null && AuctionManager.getInstance().getAuction(player.getClan().getHasHideout()) != null)
				{
					String filename = "data/html/auction/AgitSaleInfo.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile(filename);
					Auction a = AuctionManager.getInstance().getAuction(player.getClan().getHasHideout());
					html.replace("%AGIT_NAME%", a.getItemName());
					html.replace("%AGIT_OWNER_PLEDGE_NAME%", a.getSellerClanName());
					html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
					html.replace("%AGIT_SIZE%", "30 ");
					html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHall(a.getItemId()).getLease()));
					html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHall(a.getItemId()).getLocation());
					html.replace("%AGIT_AUCTION_END_YY%", String.valueOf(a.getEndDate().get(Calendar.YEAR)));
					html.replace("%AGIT_AUCTION_END_MM%", String.valueOf(a.getEndDate().get(Calendar.MONTH) + 1));
					html.replace("%AGIT_AUCTION_END_DD%", String.valueOf(a.getEndDate().get(Calendar.DAY_OF_MONTH)));
					html.replace("%AGIT_AUCTION_END_HH%", String.valueOf(a.getEndDate().get(Calendar.HOUR_OF_DAY)));
					html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 3600000) + " hours " + String.valueOf(((a.getEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 60000 % 60)) + " minutes");
					html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
					html.replace("%AGIT_AUCTION_BIDCOUNT%", String.valueOf(a.getBidders().size()));
					html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHall(a.getItemId()).getDesc());
					html.replace("%id%", String.valueOf(a.getId()));
					player.sendPacket(html);
					return;
				}
				else if(player.getClan() != null && player.getClan().getHasHideout() != 0)
				{
					int ItemId = player.getClan().getHasHideout();
					String filename = "data/html/auction/AgitInfo.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile(filename);
					html.replace("%AGIT_NAME%", ClanHallManager.getInstance().getClanHall(ItemId).getName());
					html.replace("%AGIT_OWNER_PLEDGE_NAME%", player.getClan().getName());
					html.replace("%OWNER_PLEDGE_MASTER%", player.getClan().getLeaderName());
					html.replace("%AGIT_SIZE%", "30 ");
					html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHall(ItemId).getLease()));
					html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHall(ItemId).getLocation());
					player.sendPacket(html);
					return;
				}
			}
			else if(actualCommand.equalsIgnoreCase("cancelBid"))
			{
				int bid = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()).getBidders().get(player.getClanId()).getBid();
				String filename = "data/html/auction/AgitBidCancel.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile(filename);
				html.replace("%AGIT_BID%", String.valueOf(bid));
				NumberFormat nf = NumberFormat.getIntegerInstance();
				nf.setGroupingUsed(false);
				html.replace("%AGIT_BID_REMAIN%", nf.format(bid * 0.9));
				player.sendPacket(html);
				return;
			}
			else if(actualCommand.equalsIgnoreCase("doCancelBid"))
			{
				if(AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()) != null)
				{
					AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()).cancelBid(player.getClanId());
					player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_CANCELED_YOUR_BID));
				}
				return;
			}
			else if(actualCommand.equalsIgnoreCase("cancelAuction"))
			{
				if((player.getClanPrivileges() & L2Clan.CP_CH_AUCTION) != L2Clan.CP_CH_AUCTION)
				{
					player.sendMessage(new CustomMessage("common.Privilleges", player));
					return;
				}
				String filename = "data/html/auction/AgitSaleCancel.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile(filename);
				html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				player.sendPacket(html);
				return;
			}
			else if(actualCommand.equalsIgnoreCase("doCancelAuction"))
			{
				Auction auction = AuctionManager.getInstance().getAuction(player.getClan().getHasHideout());
				if(auction != null)
				{
					Calendar penalty = Calendar.getInstance();
					penalty.add(Calendar.DAY_OF_MONTH, 7);
					ServerVariables.set("auction_penalty_ch" + auction.getId(), penalty.getTimeInMillis());
					auction.cancelAuction();
					player.sendMessage(new CustomMessage("l2d.game.model.instances.L2AuctioneerInstance.CanceledAuction", player));
				}
				return;
			}
			else if(actualCommand.equalsIgnoreCase("sale2"))
			{
				String filename = "data/html/auction/AgitSale2.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile(filename);
				html.replace("%AGIT_LAST_PRICE%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				player.sendPacket(html);
				return;
			}
			else if(actualCommand.equalsIgnoreCase("sale"))
			{
				int ItemId = player.getClan().getHasHideout();
				if(ItemId == 0)
					return;
				ClanHall ch = ClanHallManager.getInstance().getClanHall(ItemId);
				if(ch == null)
					return;
				if((player.getClanPrivileges() & L2Clan.CP_CH_AUCTION) != L2Clan.CP_CH_AUCTION)
				{
					player.sendMessage(new CustomMessage("common.Privilleges", player));
					return;
				}
				long penalty = ServerVariables.getLong("auction_penalty_ch" + ch.getId(), 0);
				if(penalty > 0)
				{
					if(Calendar.getInstance().getTimeInMillis() < penalty)
					{
						player.sendPacket(new SystemMessage(SystemMessage.IT_HAS_NOT_YET_BEEN_SEVEN_DAYS_SINCE_CANCELING_AN_AUCTION));
						return;
					}
					ServerVariables.unset("auction_penalty_ch" + ch.getId());
				}

				int money = 0;
				L2ItemInstance adena = player.getClan().getAdena();
				if(adena != null)
					money = adena.getIntegerLimitedCount();

				String filename = "data/html/auction/AgitSale1.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile(filename);
				html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				html.replace("%AGIT_PLEDGE_ADENA%", String.valueOf(money));
				player.sendPacket(html);
				return;
			}
			else if(actualCommand.equalsIgnoreCase("rebid"))
			{
				if((player.getClanPrivileges() & L2Clan.CP_CH_AUCTION) != L2Clan.CP_CH_AUCTION)
				{
					player.sendMessage(new CustomMessage("common.Privilleges", player));
					return;
				}
				try
				{
					String filename = "data/html/auction/AgitBid2.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile(filename);
					Auction a = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt());
					html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
					html.replace("%AGIT_AUCTION_BID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
					html.replace("%AGIT_AUCTION_END_YY%", String.valueOf(a.getEndDate().get(Calendar.YEAR)));
					html.replace("%AGIT_AUCTION_END_MM%", String.valueOf(a.getEndDate().get(Calendar.MONTH) + 1));
					html.replace("%AGIT_AUCTION_END_DD%", String.valueOf(a.getEndDate().get(Calendar.DAY_OF_MONTH)));
					html.replace("%AGIT_AUCTION_END_HH%", String.valueOf(a.getEndDate().get(Calendar.HOUR_OF_DAY)));
					html.replace("npc_%objectId%_bid1", "npc_" + getObjectId() + "_bid1 " + a.getId());
					player.sendPacket(html);
				}
				catch(Exception e)
				{
					player.sendMessage("Invalid auction!");
				}
				return;
			}
			else if(actualCommand.equalsIgnoreCase("location"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/auction/location.htm");
				html.replace("%location%", TownManager.getInstance().getClosestTownName(player));
				html.replace("%LOCATION%", getPictureName(player));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				player.sendPacket(html);
				return;
			}
			else if(actualCommand.equalsIgnoreCase("start"))
			{
				showChatWindow(player, 0);
				return;
			}
		}
		super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename;
		if(validateCondition() == Cond_Busy_Because_Of_Siege)
			filename = "data/html/auction/auction-busy.htm"; // Busy because of siege
		else
			filename = "data/html/auction/auction.htm";
		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	private int validateCondition()
	{
		if(getCastle() != null && getCastle().getId() > 0)
		{
			if(getCastle().getSiege().isInProgress())
				return Cond_Busy_Because_Of_Siege; // Busy because of siege
			return Cond_Regular;
		}

		return Cond_All_False;
	}

	private String getPictureName(L2Player player)
	{
		int nearestTownId = MapRegion.getInstance().getMapRegion(player.getX(), player.getY());
		String nearestTown;

		switch(nearestTownId)
		{
			case 6:
				nearestTown = "GLUDIO";
				break;
			case 7:
				nearestTown = "GLUDIN";
				break;
			case 8:
				nearestTown = "DION";
				break;
			case 9:
				nearestTown = "GIRAN";
				break;
			case 14:
				nearestTown = "RUNE";
				break;
			case 15:
				nearestTown = "GODARD";
				break;
			case 16:
				nearestTown = "SCHUTTGART";
				break;
			default:
				nearestTown = "ADEN";
				break;
		}
		return nearestTown;
	}
}