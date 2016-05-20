package com.lineage.game.tables;

import java.io.File;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.scripts.Scripts;
import com.lineage.game.cache.InfoCache;
import com.lineage.game.instancemanager.CatacombSpawnManager;
import com.lineage.game.model.L2DropData;
import com.lineage.game.model.L2MinionData;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Skill.SkillType;
import com.lineage.game.model.base.ClassId;
import com.lineage.game.model.instances.L2MonsterInstance;
import com.lineage.game.model.instances.L2TamedBeastInstance;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.DropList;
import com.lineage.util.Log;

public class NpcTable
{
	private static final Logger _log = Logger.getLogger(NpcTable.class.getName());

	private static int FailId = 0;

	private static NpcTable _instance;

	private static HashMap<Integer, L2NpcTemplate> _npcs;
	private static HashMap<Integer, StatsSet> ai_params;
	private static ArrayList<L2NpcTemplate>[] _npcsByLevel;
	private static HashMap<String, L2NpcTemplate> _npcsNames;
	private static boolean _initialized = false;

	public static NpcTable getInstance()
	{
		if(_instance == null)
			_instance = new NpcTable();

		return _instance;
	}

	@SuppressWarnings("unchecked")
	private NpcTable()
	{
		_npcs = new HashMap<Integer, L2NpcTemplate>();
		_npcsByLevel = new ArrayList[100];
		_npcsNames = new HashMap<String, L2NpcTemplate>();
		ai_params = new HashMap<Integer, StatsSet>();
		RestoreNpcData();
	}

	private final double[] hprateskill = new double[] { 0, 1, 1.2, 1.3, 2, 2, 4, 4, 0.25, 0.5, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

	private void RestoreNpcData()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			try
			{
				statement = con.prepareStatement("SELECT * FROM ai_params");
				rs = statement.executeQuery();
				LoadAIParams(rs);
			}
			catch(final Exception e)
			{}
			finally
			{
				DatabaseUtils.closeDatabaseSR(statement, rs);
			}

			try
			{
				statement = con.prepareStatement("SELECT * FROM npc WHERE ai_type IS NOT NULL");
				rs = statement.executeQuery();
				fillNpcTable(rs);
			}
			catch(final Exception e)
			{
				_log.log(Level.SEVERE, " ~ Error while creating npc table ", e);
			}
			finally
			{
				DatabaseUtils.closeDatabaseSR(statement, rs);
			}

			try
			{
				statement = con.prepareStatement("SELECT npcid, skillid, level FROM npcskills");
				rs = statement.executeQuery();
				L2NpcTemplate npcDat;
				L2Skill npcSkill;

				final HashSet<Integer> unimpl = new HashSet<Integer>();
				int counter = 0;
				while(rs.next())
				{
					final int mobId = rs.getInt("npcid");
					npcDat = _npcs.get(mobId);
					if(npcDat == null)
						continue;
					final short skillId = rs.getShort("skillid");
					int level = rs.getByte("level");

					// Для определения расы используется скилл 4416
					if(skillId == 4416)
						npcDat.setRace(level);

					if(skillId >= 4290 && skillId <= 4302)
					{
						_log.warning("Warning! Skill " + skillId + " not used, use 4416 instead.");
						continue;
					}

					if(skillId == 4408)
						if(CatacombSpawnManager._monsters.contains(mobId))
						{
							level = Config.ALT_CATACOMB_MODIFIER_HP + 8;
							npcDat.setRateHp(hprateskill[level]);
							if(Config.ALT_CATACOMB_MODIFIER_HP != 4)
								npcDat.addSkill(SkillTable.getInstance().getInfo(4417, Config.ALT_CATACOMB_MODIFIER_HP));
						}
						else
							npcDat.setRateHp(hprateskill[level]);

					npcSkill = SkillTable.getInstance().getInfo(skillId, level);

					if(npcSkill == null || npcSkill.getSkillType() == SkillType.NOTDONE)
						unimpl.add(Integer.valueOf(skillId));

					if(npcSkill == null)
						continue;

					npcDat.addSkill(npcSkill);
					counter++;
				}
				new File("log/game/unimplemented_npc_skills.txt").delete();
				for(final Integer i : unimpl)
					Log.add("[" + i + "] " + SkillTable.getInstance().getInfo(i, 1), "unimplemented_npc_skills", "");
				_log.info(" ~ Loaded: " + counter + " npc skills.");
			}
			catch(final Exception e)
			{
				_log.log(Level.SEVERE, " ~ Error while reading npcskills table ", e);
			}
			finally
			{
				DatabaseUtils.closeDatabaseSR(statement, rs);
			}

			try
			{
				statement = con.prepareStatement("SELECT mobId, itemId, min, max, sweep, chance, category FROM droplist ORDER BY mobId, category, chance DESC");
				rs = statement.executeQuery();
				L2DropData dropDat = null;
				L2NpcTemplate npcDat = null;

				while(rs.next())
				{
					final int mobId = rs.getInt("mobId");
					npcDat = _npcs.get(mobId);
					if(npcDat != null)
					{
						dropDat = new L2DropData();

						FailId = rs.getShort("itemId");

						dropDat.setItemId(rs.getShort("itemId"));
						dropDat.setMinDrop(rs.getInt("min"));
						dropDat.setMaxDrop(rs.getInt("max"));
						dropDat.setSweep(rs.getInt("sweep") == 1);
						dropDat.setChance(rs.getInt("chance"));
						dropDat.recalcWorth();
						if(dropDat.getItem().isArrow() || dropDat.getItemId() == 1419)
							dropDat.setGroupId(Byte.MAX_VALUE); // группа для нерейтуемых предметов, сюда же надо всякую фигню
						else
							dropDat.setGroupId(rs.getInt("category"));
						npcDat.addDropData(dropDat);
					}
				}

				for(final L2NpcTemplate temp : _npcs.values())
					if(temp.getDropData() != null)
						if(!temp.getDropData().validate())
							_log.warning("Problems with droplist for " + temp.toString());

				if(Config.ALT_GAME_SHOW_DROPLIST && !Config.ALT_GAME_GEN_DROPLIST_ON_DEMAND)
					FillDropList();
				else
					_log.info(" ~ Players droplist load skipped");

			}
			catch(final Exception e)
			{
				_log.log(Level.SEVERE, " ~ Error reading npc drops " + FailId, e);
			}
			finally
			{
				DatabaseUtils.closeDatabaseSR(statement, rs);
			}

			int r = 0;
			try
			{
				statement = con.prepareStatement("SELECT boss_id, minion_id, amount FROM minions");
				rs = statement.executeQuery();
				L2MinionData minionDat = null;
				L2NpcTemplate npcDat = null;
				int cnt = 0;

				while(rs.next())
				{
					r = rs.getInt("boss_id");
					final int raidId = rs.getInt("boss_id");
					npcDat = _npcs.get(raidId);
					minionDat = new L2MinionData();
					minionDat.setMinionId(rs.getInt("minion_id"));
					minionDat.setAmount(rs.getByte("amount"));
					npcDat.addRaidData(minionDat);
					cnt++;
				}

				_log.info(" ~ Loaded: " + cnt + " Minions.");
			}
			catch(final Exception e)
			{
				_log.log(Level.SEVERE, " ~ Error while loading minions" + r, e);
			}
			finally
			{
				DatabaseUtils.closeDatabaseSR(statement, rs);
			}

			try
			{
				statement = con.prepareStatement("SELECT npc_id, class_id FROM skill_learn");
				rs = statement.executeQuery();
				L2NpcTemplate npcDat = null;
				int cnt = 0;

				while(rs.next())
				{
					npcDat = _npcs.get(rs.getInt(1));
					npcDat.addTeachInfo(ClassId.values()[rs.getInt(2)]);
					cnt++;
				}

				_log.config(" ~ Loaded: " + cnt + " SkillLearn entrys.");
				_log.config("[ Npc Table ]\n");
			}
			catch(final Exception e)
			{
				_log.log(Level.SEVERE, " ~ Error while loading SkillLearn entrys.", e);
			}
			finally
			{
				DatabaseUtils.closeDatabaseSR(statement, rs);
			}
		}
		catch(final Exception e)
		{
			_log.log(Level.SEVERE, " ~ Cannot find connection to database");
		}
		finally
		{
			DatabaseUtils.closeConnection(con);
		}

		_initialized = true;

		Scripts.getInstance();
	}

	private static void LoadAIParams(final ResultSet AIData) throws Exception
	{
		int ai_params_counter = 0;
		StatsSet _set = null;
		int npc_id;
		String param, value;
		while(AIData.next())
		{
			npc_id = AIData.getInt("npc_id");
			param = AIData.getString("param");
			value = AIData.getString("value");
			if(ai_params.containsKey(npc_id))
				_set = ai_params.get(npc_id);
			else
			{
				_set = new StatsSet();
				ai_params.put(npc_id, _set);
			}
			_set.set(param, value);
			ai_params_counter++;
		}
		_log.info("[ Npc Table ]");
		_log.info(" ~ Loaded: " + ai_params_counter + " AI params for " + ai_params.size() + " NPCs.");
	}

	private static StatsSet fillNpcTable(final ResultSet NpcData) throws Exception
	{
		StatsSet npcDat = null;
		while(NpcData.next())
		{
			npcDat = new StatsSet();
			final int id = NpcData.getInt("id");
			final int level = NpcData.getByte("level");

			npcDat.set("npcId", id);
			npcDat.set("displayId", NpcData.getInt("displayId"));
			npcDat.set("level", level);
			npcDat.set("jClass", NpcData.getString("class"));

			npcDat.set("baseShldDef", NpcData.getInt("shield_defense"));
			npcDat.set("baseShldRate", NpcData.getInt("shield_defense_rate"));
			npcDat.set("baseCritRate", Math.max(1, NpcData.getInt("base_critical")) * 10);

			npcDat.set("name", NpcData.getString("name"));
			npcDat.set("title", NpcData.getString("title"));
			npcDat.set("collision_radius", NpcData.getDouble("collision_radius"));
			npcDat.set("collision_height", NpcData.getDouble("collision_height"));
			npcDat.set("sex", NpcData.getString("sex"));
			npcDat.set("type", NpcData.getString("type"));
			npcDat.set("ai_type", NpcData.getString("ai_type"));
			npcDat.set("baseAtkRange", NpcData.getInt("attackrange"));
			npcDat.set("revardExp", NpcData.getInt("exp"));
			npcDat.set("revardSp", NpcData.getInt("sp"));
			npcDat.set("basePAtkSpd", NpcData.getInt("atkspd"));
			npcDat.set("baseMAtkSpd", NpcData.getInt("matkspd"));
			npcDat.set("aggroRange", NpcData.getShort("aggro"));
			npcDat.set("rhand", NpcData.getInt("rhand"));
			npcDat.set("lhand", NpcData.getInt("lhand"));
			npcDat.set("armor", NpcData.getInt("armor"));
			npcDat.set("baseWalkSpd", NpcData.getInt("walkspd"));
			npcDat.set("baseRunSpd", NpcData.getInt("runspd"));

			npcDat.set("baseHpReg", NpcData.getDouble("base_hp_regen"));
			npcDat.set("baseCpReg", 0);
			npcDat.set("baseMpReg", NpcData.getDouble("base_mp_regen"));

			npcDat.set("baseSTR", NpcData.getInt("str"));
			npcDat.set("baseCON", NpcData.getInt("con"));
			npcDat.set("baseDEX", NpcData.getInt("dex"));
			npcDat.set("baseINT", NpcData.getInt("int"));
			npcDat.set("baseWIT", NpcData.getInt("wit"));
			npcDat.set("baseMEN", NpcData.getInt("men"));

			npcDat.set("baseHpMax", NpcData.getInt("hp"));
			npcDat.set("baseCpMax", 0);
			npcDat.set("baseMpMax", NpcData.getInt("mp"));
			npcDat.set("basePAtk", NpcData.getInt("patk"));
			npcDat.set("basePDef", NpcData.getInt("pdef"));
			npcDat.set("baseMAtk", NpcData.getInt("matk"));
			npcDat.set("baseMDef", NpcData.getInt("mdef"));

			final String factionId = NpcData.getString("faction_id");
			if(factionId != null)
				factionId.trim();
			npcDat.set("factionId", factionId);
			npcDat.set("factionRange", factionId == null || factionId.equals("") ? 0 : NpcData.getShort("faction_range"));

			npcDat.set("isDropHerbs", NpcData.getBoolean("isDropHerbs"));

			npcDat.set("shots", NpcData.getString("shots"));

			final L2NpcTemplate template = new L2NpcTemplate(npcDat, ai_params.containsKey(id) ? ai_params.get(id) : null);
			_npcs.put(id, template);
			if(_npcsByLevel[level] == null)
				_npcsByLevel[level] = new ArrayList<L2NpcTemplate>();
			_npcsByLevel[level].add(template);
			_npcsNames.put(NpcData.getString("name").toLowerCase(), template);
		}
		_log.info(" ~ Loaded: " + _npcs.size() + " Npc Templates.");

		return npcDat;
	}

	public static void reloadNpc(final int id)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			// save a copy of the old data
			final L2NpcTemplate old = getTemplate(id);
			final HashMap<Integer, L2Skill> skills = new HashMap<Integer, L2Skill>();
			if(old.getSkills() != null)
				skills.putAll(old.getSkills());
			/*
			 * Contact with Styx to understand this commenting
			 * ArrayList<L2DropData> drops = new ArrayList<L2DropData>();
			 * if(old.getDropData() != null)
			 * drops.addAll(old.getDropData());
			 */
			ClassId[] classIds = null;
			if(old.getTeachInfo() != null)
				classIds = old.getTeachInfo().clone();
			final ArrayList<L2MinionData> minions = new ArrayList<L2MinionData>(old.getMinionData());

			// reload the NPC base data
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("SELECT * FROM npc WHERE id=?");
			st.setInt(1, id);
			rs = st.executeQuery();
			fillNpcTable(rs);

			// restore additional data from saved copy
			final L2NpcTemplate created = getTemplate(id);
			for(final L2Skill skill : skills.values())
				created.addSkill(skill);
			/*
			 * for(L2DropData drop : drops)
			 * created.addDropData(drop);
			 */
			if(classIds != null)
				for(final ClassId classId : classIds)
					created.addTeachInfo(classId);
			for(final L2MinionData minion : minions)
				created.addRaidData(minion);
		}
		catch(final Exception e)
		{
			_log.warning("cannot reload npc " + id + ": " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, st, rs);
		}
	}

	public static StatsSet getNpcStatsSet(final int id)
	{
		StatsSet dat = null;

		ThreadConnection con = null;
		FiltredPreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("SELECT * FROM npc WHERE id=?");
			st.setInt(1, id);
			rs = st.executeQuery();
			dat = fillNpcTable(rs);
		}
		catch(final Exception e)
		{
			_log.warning("cannot load npc stats for " + id + ": " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, st, rs);
		}

		return dat;
	}

	// just wrapper
	public void reloadAllNpc()
	{
		RestoreNpcData();
	}

	public void saveNpc(final StatsSet npc)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		String query = "";
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final HashMap<String, Object> set = npc.getSet();
			String name = "";
			String values = "";
			for(final Object obj : set.keySet())
			{
				name = (String) obj;
				if(!name.equalsIgnoreCase("npcId"))
				{
					if(!values.equals(""))
						values += ", ";
					values += name + " = '" + set.get(name) + "'";
				}
			}
			query = "UPDATE npc SET " + values + " WHERE id = ?";
			statement = con.prepareStatement(query);
			statement.setInt(1, npc.getInteger("npcId"));
			statement.execute();
		}
		catch(final Exception e1)
		{
			// problem with storing spawn
			_log.warning("npc data couldnt be stored in db, query is :" + query + " : " + e1);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public static boolean isInitialized()
	{
		return _initialized;
	}

	public static void replaceTemplate(final L2NpcTemplate npc)
	{
		_npcs.put(npc.npcId, npc);
		_npcsNames.put(npc.name.toLowerCase(), npc);
	}

	public static L2NpcTemplate getTemplate(final int id)
	{
		return _npcs.get(id);
	}

	public static L2NpcTemplate getTemplateByName(final String name)
	{
		return _npcsNames.get(name.toLowerCase());
	}

	public static ArrayList<L2NpcTemplate> getAllOfLevel(final int lvl)
	{
		return _npcsByLevel[lvl];
	}

	public static L2NpcTemplate[] getAll()
	{
		return _npcs.values().toArray(new L2NpcTemplate[_npcs.size()]);
	}

	public void FillDropList()
	{
		for(final L2NpcTemplate npc : _npcs.values())
			InfoCache.addToDroplistCache(npc.npcId, DropList.generateDroplist(npc, null, 1, null));
		_log.info(" ~ INFO: Players droplist was cached");
	}

	public void applyServerSideTitle()
	{
		if(Config.SERVER_SIDE_NPC_TITLE_WITH_LVL)
			for(final L2NpcTemplate npc : _npcs.values())
				if(npc.isInstanceOf(L2MonsterInstance.class) && !npc.isInstanceOf(L2TamedBeastInstance.class))
				{
					String title = "L" + npc.level;
					if(npc.aggroRange != 0 || npc.factionRange != 0)
						title += " " + (npc.aggroRange != 0 ? "A" : "") + (npc.factionRange != 0 ? "S" : "");
					title += " ";
					npc.title = title + npc.title;
				}
	}

	public static void unload()
	{
		if(_npcs != null)
		{
			_npcs.clear();
			_npcs = null;
		}
		if(ai_params != null)
		{
			ai_params.clear();
			ai_params = null;
		}
		if(_npcsByLevel != null)
			_npcsByLevel = null;
		if(_npcsNames != null)
		{
			_npcsNames.clear();
			_npcsNames = null;
		}
		if(_instance != null)
			_instance = null;
	}
}