package l2d.game.model.entity.residence;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import l2d.Config;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.game.ThreadPoolManager;
import l2d.game.instancemanager.AuctionManager;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2World;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.entity.Auction;
import l2d.game.model.entity.siege.clanhall.ClanHallSiege;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.tables.SkillTable;
import l2d.util.Log;

public class ClanHall extends Residence
{
	protected static Logger _log = Logger.getLogger(ClanHall.class.getName());

	private int _lease;
	private String _desc;
	private String _location;
	private Calendar _paidUntil;
	private boolean _inDebt;
	private int _grade;
	private int _price;

	private class AutoTask implements Runnable
	{
		@Override
		public void run()
		{
			if(getOwnerId() != 0)
				try
				{
					L2Clan clan = getOwner();
					if(clan == null)
					{
						_log.warning("ClanHall[59]: clan == null");
						return;
					}

					L2ItemInstance adena = clan.getAdena();

					if(getPaidUntil() > System.currentTimeMillis())
						ThreadPoolManager.getInstance().scheduleGeneral(new AutoTask(), getPaidUntil() - System.currentTimeMillis());
					else if(adena != null && adena.getCount() >= getLease())
					{
						clan.getWarehouse().destroyItem(57, getLease());
						setInDebt(false);
						updateRentTime();
						ThreadPoolManager.getInstance().scheduleGeneral(new AutoTask(), getPaidUntil() - System.currentTimeMillis());
						Log.add("clanhall " + getName() + " lease " + getLease() + " adena from clan " + clan.getName() + "(id:" + clan.getClanId() + ") cwh at " + _paidUntil.get(Calendar.DAY_OF_MONTH) + "/" + _paidUntil.get(Calendar.MONTH), "residence");
					}
					else if(!isInDebt())
					{
						setInDebt(true);
						updateRentTime();
						ThreadPoolManager.getInstance().scheduleGeneral(new AutoTask(), getPaidUntil() - System.currentTimeMillis());
						Log.add("clanhall " + getName() + " is in debt for " + getLease() + " adena from clan " + clan.getName() + "(id:" + clan.getClanId() + ") cwh at " + _paidUntil.get(Calendar.DAY_OF_MONTH) + "/" + _paidUntil.get(Calendar.MONTH), "residence");
					}
					else
					{
						Log.add("remove " + getName() + "  clanhall from clan " + clan.getName() + "(id:" + clan.getClanId() + "), because thay have only " + (adena == null ? 0 : adena.getCount()) + " when lease is " + getLease(), "residence");
						changeOwner(null);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
		}
	}

	private class AutoTaskDoors implements Runnable
	{
		private boolean _finded = false;

		@Override
		public void run()
		{
			_finded = false;
			if(getOwnerId() != 0)
			{
				try
				{
					for(L2DoorInstance door : getDoors())
					{
						//Находим чаров вокруг двери
						for(L2Player playerarounddoor : L2World.getAroundPlayers(door, 100, 1000))
							if(playerarounddoor.getClan() != null)
								if(playerarounddoor.getClan() == getOwner())
								{
									//Если игрок найден и имеет привелегии на открытие двери - открываем
									door.openMe();
									_finded = true;
								}

						//Если игрок не найден - закрываем дверь
						if(_finded == false)
							door.closeMe();
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				ThreadPoolManager.getInstance().scheduleGeneral(new AutoTaskDoors(), Config.CH_DOORS_AUTO_OPEN_DELAY);
			}
		}
	}

	private void startAutoTask()
	{
		new AutoTask().run();
		if(Config.CH_DOORS_AUTO_OPEN)
			new AutoTaskDoors().run();
	}

	public ClanHall(int clanHallId)
	{
		super(clanHallId);
	}

	private ClanHallSiege _Siege;
	private int _SiegeDayOfWeek;
	private int _SiegeHourOfDay;

	@Override
	public ClanHallSiege getSiege()
	{
		if(_SiegeDayOfWeek <= 0)
			return null;
		if(_Siege == null)
			_Siege = new ClanHallSiege(this);
		return _Siege;
	}

	@Override
	public int getSiegeDayOfWeek()
	{
		return _SiegeDayOfWeek;
	}

	@Override
	public int getSiegeHourOfDay()
	{
		return _SiegeHourOfDay;
	}

	@Override
	public void changeOwner(L2Clan clan)
	{
		L2Clan oldOwner = getOwner();

		// Remove old owner
		if(oldOwner != null && (clan == null || clan.getClanId() != oldOwner.getClanId()))
		{
			removeSkills(); // Удаляем КХ скилы у старого владельца
			oldOwner.setHasHideout(0); // Unset has hideout flag for old owner
		}

		// Update in database
		updateOwnerInDB(clan);
		rewardSkills(); // Выдаем КХ скилы новому владельцу

		if(clan != null && getLease() > 0)
		{
			updateRentTime();
			startAutoTask();
		}
	}

	@Override
	protected void loadData()
	{
		_SiegeDayOfWeek = 1;
		_SiegeHourOfDay = 12;
		_Siege = null;

		_type = ResidenceType.Clanhall;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rs = null;
		_paidUntil = Calendar.getInstance();

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT * FROM clanhall WHERE id = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();

			if(rs.next())
			{
				_name = rs.getString("name");
				_ownerId = rs.getInt("ownerId");
				_price = rs.getInt("price");
				_lease = rs.getInt("lease");
				_desc = rs.getString("desc");
				_location = rs.getString("location");
				_paidUntil.setTimeInMillis(rs.getLong("paidUntil"));
				_grade = rs.getInt("Grade");
				_inDebt = rs.getInt("inDebt") == 1;

				_SiegeDayOfWeek = rs.getInt("siegeDayOfWeek");
				_SiegeHourOfDay = rs.getInt("siegeHourOfDay");

				if(_SiegeDayOfWeek > 0)
					getSiege().setSiegeDateTime(rs.getLong("siegeDate") * 1000);

				StringTokenizer st = new StringTokenizer(rs.getString("skills"), ";");
				while(st.hasMoreTokens())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(Integer.valueOf(st.nextToken()), Integer.valueOf(st.nextToken()));
					if(skill != null)
						_skills.put(skill.getId(), skill);
				}
			}
			DatabaseUtils.closeDatabaseSR(statement, rs);

			statement = con.prepareStatement("SELECT clan_id FROM clan_data WHERE hasHideout = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();

			if(rs.next())
				_ownerId = rs.getInt("clan_id");

			_zone = ZoneManager.getInstance().getZoneByIndex(ZoneType.ClanHall, getId(), true);
		}
		catch(Exception e)
		{
			_log.warning("Exception: ClanHall.load(): " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rs);
		}

		if(getOwnerId() == 0) // this should never happen, but one never knows ;)
			return;

		if(getLease() > 0)
			startAutoTask();
	}

	private void updateOwnerInDB(L2Clan clan)
	{
		if(clan != null)
			_ownerId = clan.getClanId(); // Update owner id property
		else
			_ownerId = 0; // Remove owner

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clanhall SET ownerId=?, inDebt=0 WHERE id=?");
			statement.setInt(1, getOwnerId());
			statement.setInt(2, getId());
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("UPDATE clan_data SET hasHideout=0 WHERE hasHideout=?");
			statement.setInt(1, getId());
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("UPDATE clan_data SET hasHideout=? WHERE clan_id=?");
			statement.setInt(1, getId());
			statement.setInt(2, getOwnerId());
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("DELETE FROM residence_functions WHERE id=?");
			statement.setInt(1, getId());
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			// Announce to clan memebers
			if(clan != null)
			{
				clan.setHasHideout(getId()); // Set has hideout flag for new owner
				clan.broadcastClanStatus(false, true);
			}
			else if(getPrice() > 0)
			{
				Calendar endDate = Calendar.getInstance();
				endDate.add(Calendar.DAY_OF_MONTH, 7); // Schedule to happen in 7 days
				statement = con.prepareStatement("REPLACE INTO auction (id, sellerId, sellerName, sellerClanName, itemType, itemId, itemObjectId, itemName, itemQuantity, startingBid, currentBid, endDate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, getId());
				statement.setInt(2, 0);
				statement.setString(3, "NPC");
				statement.setString(4, "NPC Clan");
				statement.setString(5, "ClanHall");
				statement.setInt(6, getId());
				statement.setInt(7, 0);
				statement.setString(8, getName());
				statement.setInt(9, 1);
				statement.setInt(10, getPrice());
				statement.setInt(11, 0);
				statement.setLong(12, endDate.getTimeInMillis());
				statement.execute();
				DatabaseUtils.closeStatement(statement);
				//выставляем сразу на аукцион
				AuctionManager.getInstance().getAuctions().add(new Auction(getId()));
			}
		}
		catch(Exception e)
		{
			_log.warning("Exception: updateOwnerInDB(L2Clan clan): " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeConnection(con);
		}
	}

	public int getPrice()
	{
		return _price;
	}

	public int getLease()
	{
		return isInDebt() ? _lease * 2 : _lease;
	}

	public String getDesc()
	{
		return _desc;
	}

	public String getLocation()
	{
		return _location;
	}

	public long getPaidUntil()
	{
		return _paidUntil.getTimeInMillis();
	}

	public Calendar getPaidUntilCalendar()
	{
		return _paidUntil;
	}

	public int getGrade()
	{
		return _grade;
	}

	public void updateRentTime()
	{
		_paidUntil.setTimeInMillis(System.currentTimeMillis() + 604800000);
		_paidUntil.set(Calendar.MINUTE, 0);

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clanhall SET paidUntil=?, inDebt=? WHERE id=?");
			statement.setLong(1, _paidUntil.getTimeInMillis());
			statement.setLong(2, _inDebt ? 1 : 0);
			statement.setInt(3, getId());
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.warning("Exception: ClanHall.updateRentTime(): " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public boolean isInDebt()
	{
		return _inDebt;
	}

	public void setInDebt(boolean val)
	{
		_inDebt = val;
	}
}