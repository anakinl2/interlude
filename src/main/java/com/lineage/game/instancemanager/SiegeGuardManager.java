package com.lineage.game.instancemanager;

import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.entity.residence.Residence;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Location;

public class SiegeGuardManager
{
	private static Logger _log = Logger.getLogger(SiegeGuardManager.class.getName());

	private Residence _siegeUnit;
	private List<L2Spawn> _siegeGuardSpawn = new FastList<L2Spawn>();

	public SiegeGuardManager(Residence siegeUnit)
	{
		_siegeUnit = siegeUnit;
	}

	/**
	 * Add guard.<BR><BR>
	 */
	public void addSiegeGuard(L2Player activeChar, int npcId)
	{
		if(activeChar == null)
			return;
		addSiegeGuard(activeChar.getLoc(), npcId);
	}

	/**
	 * Add guard.<BR><BR>
	 */
	public void addSiegeGuard(Location loc, int npcId)
	{
		saveSiegeGuard(loc, npcId, 0);
	}

	/**
	 * Hire merc.<BR><BR>
	 */
	public void hireMerc(L2Player activeChar, int npcId)
	{
		if(activeChar == null)
			return;
		hireMerc(activeChar.getLoc(), npcId);
	}

	/**
	 * Hire merc.<BR><BR>
	 */
	public void hireMerc(Location loc, int npcId)
	{
		saveSiegeGuard(loc, npcId, 1);
	}

	/**
	 * Remove a single mercenary, identified by the npcId and location.
	 * Presumably, this is used when a castle lord picks up a previously dropped ticket
	 */
	public void removeMerc(int npcId, Location loc)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Delete From siege_guards Where npcId = ? And x = ? AND y = ? AND z = ? AND isHired = 1");
			statement.setInt(1, npcId);
			statement.setInt(2, loc.x);
			statement.setInt(3, loc.y);
			statement.setInt(4, loc.z);
			statement.execute();
		}
		catch(Exception e1)
		{
			_log.warning("Error deleting hired siege guard at " + loc.toString() + ":" + e1);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * Remove mercs.<BR><BR>
	 */
	public static void removeMercsFromDb(int unitId)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Delete From siege_guards Where unitId = ? And isHired = 1");
			statement.setInt(1, unitId);
			statement.execute();
		}
		catch(Exception e1)
		{
			_log.warning("Error deleting hired siege guard for unit " + unitId + ":" + e1);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * Spawn guards.<BR><BR>
	 */
	public void spawnSiegeGuard()
	{
		unspawnSiegeGuard();
		loadSiegeGuard();
		for(L2Spawn spawn : _siegeGuardSpawn)
			if(spawn != null)
			{
				spawn.init();
				if(spawn.getRespawnDelay() == 0)
					spawn.stopRespawn();
			}
	}

	/**
	 * Unspawn guards.<BR><BR>
	 */
	public void unspawnSiegeGuard()
	{
		for(L2Spawn spawn : _siegeGuardSpawn)
		{
			if(spawn == null)
				continue;

			spawn.stopRespawn();
			if(spawn.getLastSpawn() != null)
				spawn.getLastSpawn().doDie(null);
		}

		getSiegeGuardSpawn().clear();
	}

	/**
	 * Load guards.<BR><BR>
	 */
	private void loadSiegeGuard()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM siege_guards Where unitId = ? And isHired = ?");
			statement.setInt(1, getSiegeUnit().getId());
			if(getSiegeUnit().getOwnerId() > 0) // If castle is owned by a clan, then don't spawn default guards
				statement.setInt(2, 1);
			else
				statement.setInt(2, 0);
			rset = statement.executeQuery();

			L2Spawn spawn1;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getTemplate(rset.getInt("npcId"));
				if(template1 != null)
				{
					spawn1 = new L2Spawn(template1);
					spawn1.setId(rset.getInt("id"));
					spawn1.setAmount(1);
					spawn1.setLocx(rset.getInt("x"));
					spawn1.setLocy(rset.getInt("y"));
					spawn1.setLocz(rset.getInt("z"));
					spawn1.setHeading(rset.getInt("heading"));
					spawn1.setRespawnDelay(rset.getInt("respawnDelay"));
					spawn1.setLocation(0);

					_siegeGuardSpawn.add(spawn1);
				}
				else
					_log.warning("Missing npc data in npc table for id: " + rset.getInt("npcId"));
			}
		}
		catch(Exception e1)
		{
			_log.warning("Error loading siege guard for unit " + getSiegeUnit().getName() + ":" + e1);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	/**
	 * Save guards.<BR><BR>
	 */
	private void saveSiegeGuard(Location loc, int npcId, int isHire)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Insert Into siege_guards (unitId, npcId, x, y, z, heading, respawnDelay, isHired) Values (?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, getSiegeUnit().getId());
			statement.setInt(2, npcId);
			statement.setInt(3, loc.x);
			statement.setInt(4, loc.y);
			statement.setInt(5, loc.z);
			statement.setInt(6, loc.h);
			if(isHire == 1)
				statement.setInt(7, 0);
			else
				statement.setInt(7, 600);
			statement.setInt(8, isHire);
			statement.execute();
		}
		catch(Exception e1)
		{
			_log.warning("Error adding siege guard for unit " + getSiegeUnit().getName() + ":" + e1);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public Residence getSiegeUnit()
	{
		return _siegeUnit;
	}

	public List<L2Spawn> getSiegeGuardSpawn()
	{
		return _siegeGuardSpawn;
	}
}