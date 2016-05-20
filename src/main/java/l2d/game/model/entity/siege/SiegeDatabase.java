package l2d.game.model.entity.siege;

import java.sql.ResultSet;
import java.util.logging.Logger;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import l2d.game.model.L2Clan;

public abstract class SiegeDatabase
{
	private static Logger _log = Logger.getLogger(SiegeDatabase.class.getName());
	protected Siege _siege;

	public SiegeDatabase(Siege siege)
	{
		_siege = siege;
	}

	public abstract void saveSiegeDate();

	public void saveLastSiegeDate()
	{}

	/**
	 * Return true if the clan is registered or owner of a castle<BR><BR>
	 * @param clan The L2Clan of the player
	 */
	public static boolean checkIsRegistered(L2Clan clan, int unitid)
	{
		if(clan == null)
			return false;

		if(unitid > 0 && clan.getHasCastle() == unitid)
			return true;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		boolean register = false;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clan_id FROM siege_clans where clan_id=?" + (unitid == 0 ? "" : " and unit_id=?"));
			statement.setInt(1, clan.getClanId());
			if(unitid != 0)
				statement.setInt(2, unitid);
			rset = statement.executeQuery();
			if(rset.next())
				register = true;
		}
		catch(Exception e)
		{
			_log.warning("Exception: checkIsRegistered(): " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		return register;
	}

	public void clearSiegeClan()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE unit_id=?");
			statement.setInt(1, _siege.getSiegeUnit().getId());
			statement.execute();
			//DatabaseUtils.closeStatement(statement);
			//if(_siege.getSiegeUnit().getOwnerId() > 0)
			//{
			//	statement = con.prepareStatement("DELETE FROM siege_clans WHERE clan_id=?");
			//	statement.setInt(1, _siege.getSiegeUnit().getOwnerId());
			//	statement.execute();
			//}
		}
		catch(Exception e)
		{
			_log.warning("Exception: clearSiegeClan(): " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		_siege.getAttackerClans().clear();
		_siege.getDefenderClans().clear();
		_siege.getDefenderWaitingClans().clear();
		_siege.getDefenderRefusedClans().clear();
	}

	public void clearSiegeWaitingClan()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE unit_id=? and type = 2");
			statement.setInt(1, _siege.getSiegeUnit().getId());
			statement.execute();

			_siege.getDefenderWaitingClans().clear();
		}
		catch(Exception e)
		{
			_log.warning("Exception: clearSiegeWaitingClan(): " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void clearSiegeRefusedClan()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE unit_id=? and type = 3");
			statement.setInt(1, _siege.getSiegeUnit().getId());
			statement.execute();

			_siege.getDefenderRefusedClans().clear();
		}
		catch(Exception e)
		{
			_log.warning("Exception: clearSiegeRefusedClan(): " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void removeSiegeClan(int clanId)
	{
		if(clanId <= 0)
			return;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE unit_id=? and clan_id=?");
			statement.setInt(1, _siege.getSiegeUnit().getId());
			statement.setInt(2, clanId);
			statement.execute();
			loadSiegeClan();
		}
		catch(Exception e)
		{
			_log.warning("Exception: removeSiegeClan(): " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void loadSiegeClan()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			_siege.getAttackerClans().clear();
			_siege.getDefenderClans().clear();
			_siege.getDefenderWaitingClans().clear();
			_siege.getDefenderRefusedClans().clear();
			// Add castle owner as defender
			if(_siege.getSiegeUnit().getOwnerId() > 0)
				_siege.addDefender(_siege.getSiegeUnit().getOwnerId(), SiegeClanType.OWNER);
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clan_id, type FROM siege_clans where unit_id = ?");
			statement.setInt(1, _siege.getSiegeUnit().getId());
			rset = statement.executeQuery();
			int typeId;
			while(rset.next())
			{
				typeId = rset.getInt("type");
				if(typeId == 0)
					_siege.addDefender(rset.getInt("clan_id"), SiegeClanType.DEFENDER);
				else if(typeId == 1)
					_siege.addAttacker(rset.getInt("clan_id"));
				else if(typeId == 2)
					_siege.addDefenderWaiting(rset.getInt("clan_id"));
				else if(typeId == 3)
					_siege.addDefenderRefused(rset.getInt("clan_id"));
			}
		}
		catch(Exception e)
		{
			_log.warning("Exception: loadSiegeClan(): " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	public void saveSiegeClan(L2Clan clan, int typeId, boolean isUpdateRegistration)
	{
		if(clan == null || clan.getHasCastle() > 0)
			return;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO siege_clans (clan_id,unit_id,type) VALUES (?,?,?)");
			statement.setInt(1, clan.getClanId());
			statement.setInt(2, _siege.getSiegeUnit().getId());
			statement.setInt(3, typeId);
			statement.execute();

			if(typeId == 0 || typeId == -1)
				_siege.addDefender(clan.getClanId(), SiegeClanType.DEFENDER);
			else if(typeId == 1)
				_siege.addAttacker(clan.getClanId());
			else if(typeId == 2)
				_siege.addDefenderWaiting(clan.getClanId());
			else if(typeId == 3)
				_siege.addDefenderRefused(clan.getClanId());
		}
		catch(Exception e)
		{
			_log.warning("Exception: saveSiegeClan: " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}
}