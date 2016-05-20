package com.lineage.game.tables;

import java.io.File;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.model.L2Skill;
import com.lineage.game.skills.SkillsEngine;
import com.lineage.util.Log;

public class SkillTable
{
	private static SkillTable _instance;

	private L2Skill[][] skills;
	private HashMap<Integer, Integer> _baseLevels = new HashMap<Integer, Integer>();

	public static SkillTable getInstance()
	{
		if(_instance == null)
			_instance = new SkillTable();
		return _instance;
	}

	private SkillTable()
	{
		skills = SkillsEngine.getInstance().loadAllSkills(9099, 232); //TODO если происходит ArrayIndexOutOfBounds то поднять лимит(ы)
		loadBaseLevels();
		loadSqlSkills();
	}

	public void reload()
	{
		_instance = new SkillTable();
	}

	public L2Skill getInfo(int magicId, int level)
	{
		magicId--;
		level--;
		return magicId < 0 || level < 0 || magicId >= skills.length || skills[magicId] == null || level >= skills[magicId].length ? null : skills[magicId][level];
	}

	public int getMaxLevel(int magicId)
	{
		magicId--;
		return skills[magicId] == null ? 0 : skills[magicId].length;
	}

	public int getBaseLevel(int magicId)
	{
		return _baseLevels.get(magicId);
	}

	private void loadBaseLevels()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM skills WHERE level < 100 ORDER BY id, level");
			rset = statement.executeQuery();
			while(rset.next())
			{
				int id = rset.getInt("id");
				int level = rset.getInt("level");
				// Последним запишется наибольший уровень. Некрасиво, но так проще :)
				_baseLevels.put(id, level);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	private void loadSqlSkills()
	{
		new File("log/game/sql_skill_levels.txt").delete();
		new File("log/game/sql_skill_enchant_levels.txt").delete();
		new File("log/game/sql_skill_display_levels.txt").delete();

		ArrayList<Integer> _incorrectSkills = new ArrayList<Integer>();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM skills ORDER BY id, level DESC");
			rset = statement.executeQuery();
			int lastid = 0;
			int lastlevel = 0;
			while(rset.next())
			{
				int id = rset.getInt("id");
				int display_level = rset.getInt("level");
				String name = rset.getString("name");
				boolean is_magic = rset.getInt("is_magic") == 1;
				int mp_consume = rset.getInt("mp_consume");
				int hp_consume = rset.getInt("hp_consume");
				int cast_range = rset.getInt("cast_range");
				int hit_time = rset.getInt("hit_time");
				int power = rset.getInt("power");
				int learn = rset.getInt("learn");

				int baseLevel = _baseLevels.get(id);
				int level = SkillTreeTable.convertEnchantLevel(baseLevel, display_level);
				L2Skill skill = getInfo(id, level);
				
				if(lastid != id)
				{
					lastlevel = level;
					lastid = id;
				}

				if(skill == null)
				{
					if(!_incorrectSkills.contains(id))
					{
						_incorrectSkills.add(id);
						if(display_level < 100)
							Log.add("Incorrect skill levels for id: " + id + ", level = " + level + ", display_level = " + display_level, "sql_skill_levels", "");
						else
							Log.add("Not found enchant for skill id: " + id + ", level = " + level + ", display_level = " + display_level, "sql_skill_enchant_levels", "");
					}
					continue;
				}

				if(skill.getDisplayLevel() != display_level)
					skill.setDisplayLevel((short) display_level);

				// Корректируем уровни скиллов, в основном для энчантов
				if(skill.getDisplayLevel() != display_level)
					Log.add("Incorrect display level: id = " + id + ", level = " + level, "sql_skill_display_levels", "");

				// для некоторых скиллов количество уровней в сервере больше чем в клиенте
				// по хорошему это надо делать только для последнего определенного в sql уровня
				for(int i = 0; (skill = getInfo(id, level + i)) != null; i++)
				{
					if(power > 0)
						skill.setPower(power);

					skill.setBaseLevel((short) baseLevel);

					if(skill.getMagicLevel() == 0)
						skill.setMagicLevel(learn);

					skill.setCastRange(cast_range);

					skill.setName(name);

					skill.setHitTime(hit_time);

					if(skill.getSkillInterruptTime() == 0)
						skill.setSkillInterruptTime(skill.getHitTime() * 3 / 4);

					skill.setIsMagic(is_magic);
					skill.setOverhit(skill.isOverhit() || !is_magic && Config.ALT_ALL_PHYS_SKILLS_OVERHIT);

					skill.setHpConsume(hp_consume);
					if(mp_consume > 0)
						if(mp_consume / 4 >= 1 && is_magic)
						{
							skill.setMpConsume1(mp_consume * 1. / 4);
							skill.setMpConsume2(mp_consume * 3. / 4);
						}
						else
							skill.setMpConsume2(mp_consume);

					if(!(lastlevel == level && lastid == id))
						break;
				}

				/*
				operate_type
				0 - в основном физ.
				1 - в основном маг.
				2 - в основном селфы, иногда бафы.
				3 - дебафы.
				4 - герои, нублесы, скиллы захвата замка.
				5 - рыбные, предметные, аугментация, трансформация, SA.
				6 - ауры
				7 - трансформации
				11 - пассивки игроков
				12 - еще пассивки игроков, и мобов
				13 - спец. пассивки игроков, характерные только для конкретной расы. Расы мобов.
				14 - аналогично, только еще Divine Inspiration, пассивные Final скиллы, и всякая фигня намешана
				15 - клановые скиллы, скиллы фортов
				16 - сеты, эпики, SA, аугментация, все пассивное
				
				OP_PASSIVE: 11, 12, 13, 14, 15, 16
				OP_ACTIVE: 0, 1, 2, 3, 4, 5, 7
				OP_TOGGLE: 6
				OP_ON_ATTACK: 5
				OP_ON_CRIT: 5
				OP_ON_MAGIC_ATTACK: 5
				OP_ON_UNDER_ATTACK: 5
				OP_ON_MAGIC_SUPPORT: 5
				*/
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	public static void unload()
	{
		if(_instance != null)
			_instance = null;
	}

	public L2Skill[] getAllLevels(int magicId)
	{
		magicId--;
		return skills[magicId];
	}

	public int getMaxChance(int magicId)
	{
		int chance = 0;
		for(L2Skill skilas : SkillTable.getInstance().getAllLevels(magicId))
			if(chance < skilas.getActivateRate())
				chance = skilas.getActivateRate();
		return chance;
	}

	public long getMaxReuse(int magicId)
	{
		long reuse = 0;
		for(L2Skill skilas : SkillTable.getInstance().getAllLevels(magicId))
			if(reuse < skilas.getReuseDelay())
				reuse = skilas.getReuseDelay();
		return reuse;
	}

	public long getMaxHitTime(int magicId)
	{
		long hittime = 0;
		for(L2Skill skilas : SkillTable.getInstance().getAllLevels(magicId))
			if(hittime < skilas.getHitTime())
				hittime = skilas.getHitTime();
		return hittime;
	}
}