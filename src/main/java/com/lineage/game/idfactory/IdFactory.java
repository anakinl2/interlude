package com.lineage.game.idfactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;

public abstract class IdFactory
{
	private static Logger _log = Logger.getLogger(IdFactory.class.getName());

	protected boolean initialized;

	protected long releasedCount = 0;

	public static final int FIRST_OID = 0x10000000;
	public static final int LAST_OID = 0x7FFFFFFF;
	public static final int FREE_OBJECT_ID_SIZE = LAST_OID - FIRST_OID;

	protected static final IdFactory _instance = new BitSetIDFactory();

	protected IdFactory()
	{
		setAllCharacterOffline();
		cleanUpDB();
	}

	private void setAllCharacterOffline()
	{
		ThreadConnection conn = null;
		FiltredStatement stmt = null;
		try
		{
			conn = L2DatabaseFactory.getInstance().getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("UPDATE characters SET online = 0");
			stmt.executeUpdate("UPDATE characters SET accesslevel = 0 WHERE accesslevel = -1");
			_log.info("Clear characters online status and accesslevel.");
		}
		catch(SQLException e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCS(conn, stmt);
		}
	}

	/**
	 * Cleans up Database
	 */
	private void cleanUpDB()
	{
		ThreadConnection conn = null;
		FiltredStatement stmt = null;
		try
		{
			int cleanCount = 0;
			int curCount;

			conn = L2DatabaseFactory.getInstance().getConnection();
			stmt = conn.createStatement();

			if((curCount = stmt.executeUpdate("DELETE FROM character_friends WHERE character_friends.char_id NOT IN (SELECT obj_Id FROM characters) OR character_friends.friend_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_friends.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM couples WHERE couples.player1Id NOT IN (SELECT obj_Id FROM characters) OR couples.player2Id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table couples.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_blocklist WHERE character_blocklist.obj_Id NOT IN (SELECT obj_Id FROM characters) OR character_blocklist.target_Id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_friends.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_hennas WHERE character_hennas.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_hennas.");
			}

			if((curCount = stmt.executeUpdate("DELETE FROM character_macroses WHERE character_macroses.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_macroses.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_quests WHERE character_quests.char_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_quests.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM character_recipebook WHERE character_recipebook.char_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_recipebook.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_shortcuts WHERE character_shortcuts.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_shortcuts.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM character_skills WHERE character_skills.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_skills.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_effects_save WHERE character_effects_save.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_effects_save.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_skills_save WHERE character_skills_save.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_skills_save.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM character_subclasses WHERE character_subclasses.char_obj_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_subclasses.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM character_variables WHERE character_variables.obj_id = '0';")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table character_variables.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM clan_data WHERE clan_data.leader_id NOT IN (SELECT obj_Id FROM characters);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table clan_data.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM clan_subpledges WHERE clan_subpledges.clan_id NOT IN (SELECT clan_id FROM clan_data);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table clan_subpledges.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM ally_data WHERE ally_data.leader_id NOT IN (SELECT clan_id FROM clan_data);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table ally_data.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM pets WHERE pets.item_obj_id NOT IN (SELECT object_id FROM items);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table pets.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM siege_clans WHERE siege_clans.clan_id NOT IN (SELECT clan_id FROM clan_data);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table siege_clans.");
			}
			if(Config.HARD_DB_CLEANUP_ON_START && (curCount = stmt.executeUpdate("DELETE FROM items WHERE owner_id NOT IN (SELECT obj_Id FROM characters) AND owner_id NOT IN (SELECT clan_id FROM clan_data) AND owner_id NOT IN (SELECT objId FROM pets) AND owner_id NOT IN (SELECT id FROM npc);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table items.");
			}

			if((curCount = stmt.executeUpdate("DELETE FROM clan_wars where clan1 not in (select clan_id FROM clan_data) or clan2 not in (select clan_id FROM clan_data);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table clan_wars.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM item_attributes where itemId not in (select object_id FROM items);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table item_attributes.");
			}
			if((curCount = stmt.executeUpdate("DELETE FROM ally_data WHERE `ally_id` NOT IN (SELECT ally_id FROM `clan_data`);")) > 0)
			{
				cleanCount += curCount;
				_log.info("Cleaned " + curCount + " elements from table ally_data.");
			}

			if((curCount = stmt.executeUpdate("UPDATE characters SET clanid=0,pledge_type=0,pledge_rank=0,lvl_joined_academy=0,apprentice=0 WHERE clanid!=0 AND clanid NOT IN (SELECT clan_id FROM clan_data);")) > 0)
				_log.info("Updated " + curCount + " elements from table characters.");
			if((curCount = stmt.executeUpdate("UPDATE clan_data SET ally_id=0 WHERE ally_id!=0 AND ally_id NOT IN (SELECT ally_id FROM ally_data);")) > 0)
				_log.info("Updated " + curCount + " elements from table clan_data");

			_log.info("Total cleaned " + cleanCount + " elements from database.");
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(conn, stmt);
		}
	}

	protected int[] extractUsedObjectIDTable()
	{
		final String[][] objTables = {
				{ "characters", "obj_id" },
				{ "items", "object_id" },
				{ "clan_data", "clan_id" },
				{ "ally_data", "ally_id" },
				{ "pets", "objId" },
				{ "couples", "id" } };

		ThreadConnection con = null;
		FiltredStatement s = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			s = con.createStatement();

			String query = "SELECT " + objTables[0][1] + ", 0 AS i FROM " + objTables[0][0];
			for(int i = 1; i < objTables.length; i++)
				query += " UNION SELECT " + objTables[i][1] + ", " + i + " FROM " + objTables[i][0];

			rs = s.executeQuery("SELECT COUNT(*), COUNT(DISTINCT " + objTables[0][1] + ") FROM ( " + query + " ) AS all_ids");
			if(!rs.next())
				throw new Exception("IdFactory: can't extract count ids");
			if(rs.getInt(1) != rs.getInt(2))
				throw new Exception("IdFactory: there are duplicates in object ids");

			int[] result = new int[rs.getInt(1)];
			DatabaseUtils.closeResultSet(rs);
			_log.info("IdFactory: Extracting " + result.length + " used id's from data tables...");

			rs = s.executeQuery(query);
			int idx = 0;
			while(rs.next())
				result[idx++] = rs.getInt(1);

			_log.info("IdFactory: Successfully extracted " + idx + " used id's from data tables.");
			return result;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
			return null;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, s, rs);
		}
	}

	public boolean isInitialized()
	{
		return initialized;
	}

	public static IdFactory getInstance()
	{
		return _instance;
	}

	public abstract int getNextId();

	/**
	 * return a used Object ID back to the pool
	 * @param object ID
	 */
	public void releaseId(int id)
	{
		releasedCount++;
	}

	public long getReleasedCount()
	{
		return releasedCount;
	}

	public abstract int size();

	public static void unload()
	{
		if(_instance != null)
			((BitSetIDFactory) _instance)._unload();
	}
}