package l2d.game.tables;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Skill.SkillType;
import l2d.game.model.L2SkillLearn;
import l2d.game.model.base.ClassId;
import l2d.game.model.base.L2EnchantSkillLearn;
import l2d.game.model.base.Race;
import com.lineage.util.Log;

@SuppressWarnings({ "nls", "unqualified-field-access", "boxing" })
public class SkillTreeTable
{
	public static final int NORMAL_ENCHANT_COST_MULTIPLIER = 1;
	public static final int SAFE_ENCHANT_COST_MULTIPLIER = 3;
	public static final int NORMAL_ENCHANT_BOOK = 6622;
	public static final int SAFE_ENCHANT_BOOK = 9627;
	public static final int CHANGE_ENCHANT_BOOK = 9626;
	public static final int UNTRAIN_ENCHANT_BOOK = 9625;

	private static final Logger _log = Logger.getLogger(SkillTreeTable.class.getName());

	private static SkillTreeTable _instance;

	private static FastMap<ClassId, ArrayList<L2SkillLearn>> _skillTrees;
	private static ArrayList<FastMap<Integer, FastMap<Integer, L2SkillLearn>>> _skillCostTable;
	public static FastMap<Integer, ArrayList<L2EnchantSkillLearn>> _enchant;
	private static ArrayList<L2SkillLearn> _fishingSkills;
	private static ArrayList<L2SkillLearn> _clanSkills;
	private static ArrayList<L2SkillLearn> _transformationSkills;

	private static FastMap<Short, String> _unimplemented_skills;

	public static SkillTreeTable getInstance()
	{
		if(_instance == null)
			_instance = new SkillTreeTable();
		return _instance;
	}

	/**
	 * Return the minimum level needed to have this Expertise.<BR><BR>
	 * 
	 * @param grade
	 *            The grade level searched
	 */
	public static short getExpertiseLevel(int grade)
	{
		if(grade <= 0)
			return 0;

		for(L2SkillLearn sl : SkillTreeTable._skillTrees.get(ClassId.fighter))
			// TODO: переписать нафиг
			if(sl.id == 239 && sl.skillLevel == grade)
				return sl.minLevel;

		throw new Error("Expertise not found for grade " + grade);
	}

	public static int getMinSkillLevel(int skillID, ClassId classID, int skillLVL)
	{
		if(skillID > 0 && skillLVL > 0)
			for(L2SkillLearn sl : SkillTreeTable._skillTrees.get(classID))
				if(sl.skillLevel == skillLVL && sl.id == skillID)
					return sl.minLevel;

		return 0;
	}

	private SkillTreeTable()
	{
		new File("log/game/unimplemented_skills.txt").delete();

		_skillTrees = new FastMap<ClassId, ArrayList<L2SkillLearn>>();
		_fishingSkills = new ArrayList<L2SkillLearn>();
		_transformationSkills = new ArrayList<L2SkillLearn>();
		_clanSkills = new ArrayList<L2SkillLearn>();
		_unimplemented_skills = new FastMap<Short, String>();

		int classintid = 0;
		int count = 0;

		ThreadConnection con = null;
		FiltredPreparedStatement classliststatement = null;
		FiltredPreparedStatement skilltreestatement = null;
		ResultSet classlist = null, skilltree = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			classliststatement = con.prepareStatement("SELECT * FROM class_list ORDER BY id");
			skilltreestatement = con.prepareStatement("SELECT class_id, skill_id, level, name, sp, min_level, rep FROM skill_trees where class_id=? AND class_id >= 0 ORDER BY skill_id, level");
			classlist = classliststatement.executeQuery();
			while(classlist.next())
			{
				classintid = classlist.getInt("id");
				ClassId classId = ClassId.values()[classintid];
				ArrayList<L2SkillLearn> list = new ArrayList<L2SkillLearn>();

				skilltreestatement.setInt(1, classintid);
				skilltree = skilltreestatement.executeQuery();
				addSkills(con, skilltree, list);

				_skillTrees.put(ClassId.values()[classintid], list);
				count += list.size();

				ClassId secondparent = classId.getParent((byte) 1);
				if(secondparent == classId.getParent((byte) 0))
					secondparent = null;

				classId = classId.getParent((byte) 0);
				while(classId != null)
				{
					ArrayList<L2SkillLearn> parentList = _skillTrees.get(classId);
					list.addAll(parentList);
					classId = classId.getParent((byte) 0);
					if(classId == null && secondparent != null)
					{
						classId = secondparent;
						secondparent = secondparent.getParent((byte) 1);
					}
				}

				// _log.config("SkillTreeTable: skill tree for class " + classintid + " has " + list.size() + " skills");
			}
			DatabaseUtils.closeDatabaseSR(classliststatement, classlist);
			classliststatement = null;
			classlist = null;
			DatabaseUtils.closeDatabaseSR(skilltreestatement, skilltree);
			loadFishingSkills(con);
			loadTransformationSkills(con);
			loadClanSkills(con);
			_enchant = EnchantTable._enchant;
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "error while creating skill tree for classId " + classintid, e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseSR(classliststatement, classlist);
			DatabaseUtils.closeDatabaseSR(skilltreestatement, skilltree);
			DatabaseUtils.closeConnection(con);
		}

		loadSkillCostTable();

		_log.info("[ Skill Tree Table ]");
		_log.info(" ~ Loaded: " + count + " skills.");
		_log.info(" ~ Loaded: " + _fishingSkills.size() + " fishing skills.");
		_log.info(" ~ Loaded: " + _transformationSkills.size() + " transformation skills.");
		_log.info(" ~ Loaded: " + _clanSkills.size() + " clan skills.");
		_log.info(" ~ Loaded: " + _enchant.size() + " enchanted skills.");
		_log.info("[ Skill Tree Table ]\n");

		if(_unimplemented_skills.size() > 0)
			_log.info("SkillTreeTable: Loaded " + _unimplemented_skills.size() + " not implemented skills!!!");

		for(Short id : _unimplemented_skills.keySet())
			Log.add(_unimplemented_skills.get(id) + " - " + id, "unimplemented_skills", "");
	}

	private void loadFishingSkills(ThreadConnection con) throws SQLException
	{
		FiltredPreparedStatement statement = null;
		ResultSet skilltree = null;
		try
		{
			statement = con.prepareStatement("SELECT class_id, skill_id, level, name, sp, min_level, rep FROM skill_trees WHERE class_id=-1 ORDER BY skill_id, level");
			skilltree = statement.executeQuery();
			addSkills(con, skilltree, _fishingSkills);
		}
		finally
		{
			DatabaseUtils.closeDatabaseSR(statement, skilltree);
		}
	}

	private void loadTransformationSkills(ThreadConnection con) throws SQLException
	{
		FiltredPreparedStatement statement = null;
		ResultSet skilltree = null;
		try
		{
			statement = con.prepareStatement("SELECT class_id, skill_id, level, name, sp, min_level, rep FROM skill_trees WHERE class_id=-4 ORDER BY skill_id, level");
			skilltree = statement.executeQuery();
			addSkills(con, skilltree, _transformationSkills);
		}
		finally
		{
			DatabaseUtils.closeDatabaseSR(statement, skilltree);
		}
	}

	private void loadClanSkills(ThreadConnection con) throws SQLException
	{
		FiltredPreparedStatement statement = null;
		ResultSet skilltree = null;
		try
		{
			statement = con.prepareStatement("SELECT class_id, skill_id, level, name, sp, min_level, rep FROM skill_trees WHERE class_id=-2 ORDER BY skill_id, level");
			skilltree = statement.executeQuery();
			addSkills(con, skilltree, _clanSkills);
		}
		finally
		{
			DatabaseUtils.closeDatabaseSR(statement, skilltree);
		}
	}

	private void addSkills(ThreadConnection con, ResultSet skilltree, ArrayList<L2SkillLearn> dest) throws SQLException
	{
		while(skilltree.next())
		{
			short id = skilltree.getShort("skill_id");
			byte lvl = skilltree.getByte("level");
			String name = skilltree.getString("name");
			if(lvl == 1)
			{
				L2Skill s = SkillTable.getInstance().getInfo(id, 1);
				if(s == null || s.getSkillType() == SkillType.NOTDONE)
					_unimplemented_skills.put(id, name == null ? "" : name);
			}
			byte minLvl = skilltree.getByte("min_level");
			int cost;
			short itemId = 0;
			int itemCount = 0;
			if(skilltree.getInt("class_id") == -2)
				cost = skilltree.getInt("rep");
			else
				cost = skilltree.getInt("sp");
			FiltredPreparedStatement statement2 = con.prepareStatement("SELECT item_id, item_count FROM skill_spellbooks WHERE skill_id=? AND level=?");
			statement2.setInt(1, id);
			statement2.setInt(2, lvl);
			ResultSet itemIdCount = statement2.executeQuery();
			if(itemIdCount.next())
			{
				itemId = itemIdCount.getShort("item_id");
				itemCount = itemIdCount.getInt("item_count");
			}
			statement2.close();
			L2SkillLearn skl = new L2SkillLearn(id, lvl, minLvl, name, cost, itemId, itemCount, skilltree.getInt("class_id") == -1, skilltree.getInt("class_id") == -2, skilltree.getInt("class_id") == -4);
			dest.add(skl);
		}
	}

	private void loadSkillCostTable()
	{
		_skillCostTable = new ArrayList<FastMap<Integer, FastMap<Integer, L2SkillLearn>>>(ClassId.values().length + 1);
		for(ClassId cid : ClassId.values())
			_skillCostTable.add(cid.getId(), new FastMap<Integer, FastMap<Integer, L2SkillLearn>>());

		for(ClassId classId : _skillTrees.keySet())
		{
			FastMap<Integer, FastMap<Integer, L2SkillLearn>> skt = _skillCostTable.get(classId.getId());

			ArrayList<L2SkillLearn> lst = _skillTrees.get(classId);
			for(L2SkillLearn skl : lst)
			{
				FastMap<Integer, L2SkillLearn> skillmap = skt.get((int) skl.getId());
				if(skillmap == null)
				{
					skillmap = new FastMap<Integer, L2SkillLearn>();
					skt.put((int) skl.getId(), skillmap);
				}
				skillmap.put((int) skl.getLevel(), skl);
			}
		}
	}

	public ArrayList<L2SkillLearn> getAvailableSkills(L2Player cha, ClassId classId)
	{
		ArrayList<L2SkillLearn> result = new ArrayList<L2SkillLearn>();
		ArrayList<L2SkillLearn> skills = _skillTrees.get(classId);
		if(skills == null)
		{
			// the skilltree for this class is undefined, so we give an empty list
			_log.warning("Skilltree for class " + classId + " is not defined !");
			return new ArrayList<L2SkillLearn>(0);
		}

		L2Skill[] oldSkills = cha.getAllSkillsArray();
		for(L2SkillLearn temp : skills)
			if(temp.minLevel <= cha.getLevel())
			{
				boolean knownSkill = false;
				for(L2Skill s : oldSkills)
					if(s.getId() == temp.id)
					{
						if(s.getLevel() == temp.skillLevel - 1)
							result.add(temp); // this is the next level of a skill that we know
						knownSkill = true;
						break;
					}
				if(!knownSkill && temp.skillLevel == 1)
					result.add(temp); // this is a new skill
			}
		return result;
	}

	public L2SkillLearn[] getAvailableClanSkills(L2Clan clan)
	{
		ArrayList<L2SkillLearn> result = new ArrayList<L2SkillLearn>();
		ArrayList<L2SkillLearn> skills = _clanSkills;

		if(skills == null)
			return new L2SkillLearn[0];

		L2Skill[] oldSkills = clan.getAllSkills();

		for(L2SkillLearn temp : skills)
			if(temp.minLevel <= clan.getLevel())
			{
				boolean knownSkill = false;

				for(int j = 0; j < oldSkills.length && !knownSkill; j++)
					if(oldSkills[j].getId() == temp.id)
					{
						knownSkill = true;

						if(oldSkills[j].getLevel() == temp.skillLevel - 1)
							// this is the next level of a skill that we know
							result.add(temp);
					}

				if(!knownSkill && temp.skillLevel == 1)
					// this is a new skill
					result.add(temp);
			}

		return result.toArray(new L2SkillLearn[result.size()]);
	}

	public ArrayList<L2Skill> getSkillsToEnchant(L2Player cha)
	{
		ArrayList<L2Skill> result = new ArrayList<L2Skill>();

		L2Skill[] skills = cha.getAllSkillsArray();
		if(skills.length == 0)
			return result;

		for(L2Skill s : skills)
		{
			ArrayList<L2EnchantSkillLearn> al = _enchant.get(s.getId());
			if(al != null && al.get(0).getBaseLevel() <= s.getLevel() && s.getLevel() < SkillTable.getInstance().getMaxLevel(s.getId()))
				result.add(s);
		}

		return result;
	}

	public L2EnchantSkillLearn[] getAvailableEnchantSkills(L2Player cha)
	{
		List<L2EnchantSkillLearn> result = new FastList<L2EnchantSkillLearn>();
		List<L2EnchantSkillLearn> skills = new FastList<L2EnchantSkillLearn>();

		for(ArrayList<L2EnchantSkillLearn> s : _enchant.values())
			skills.addAll(s);

		L2Skill[] oldSkills = cha.getAllSkillsArray();

		for(L2EnchantSkillLearn temp : skills)
			if(76 <= cha.getLevel())
			{
				boolean knownSkill = false;

				for(int j = 0; j < oldSkills.length && !knownSkill; j++)
					if(oldSkills[j].getId() == temp.getId())
					{
						knownSkill = true;

						if(oldSkills[j].getLevel() == temp.getMinSkillLevel())
							// this is the next level of a skill that we know
							result.add(temp);
					}

			}

		oldSkills = null;
		skills = null;

		//cha.sendMessage("loaded "+ result.size()+" enchant skills for this char(You)");
		return result.toArray(new L2EnchantSkillLearn[result.size()]);
	}

	public static ArrayList<L2EnchantSkillLearn> getFirstEnchantsForSkill(int skillid)
	{
		ArrayList<L2EnchantSkillLearn> result = new ArrayList<L2EnchantSkillLearn>();

		ArrayList<L2EnchantSkillLearn> enchants = _enchant.get(skillid);
		if(enchants == null)
			return result;

		for(L2EnchantSkillLearn e : enchants)
			if(e.getLevel() == 101 || e.getLevel() == 141)
				result.add(e);

		return result;
	}

	public static ArrayList<L2EnchantSkillLearn> getEnchantsForChange(int skillid, int level)
	{
		ArrayList<L2EnchantSkillLearn> result = new ArrayList<L2EnchantSkillLearn>();

		ArrayList<L2EnchantSkillLearn> enchants = _enchant.get(skillid);
		if(enchants == null)
			return result;

		for(L2EnchantSkillLearn e : enchants)
			if(e.getLevel() % 100 == level % 100)
				result.add(e);

		return result;
	}

	public static L2EnchantSkillLearn getSkillEnchant(int skillid, int level)
	{
		ArrayList<L2EnchantSkillLearn> enchants = _enchant.get(skillid);
		if(enchants == null)
			return null;

		for(L2EnchantSkillLearn e : enchants)
			if(e.getLevel() == level)
				return e;
		return null;
	}

	/**
	 * Преобразует уровень скила из клиентского представления в серверное
	 * 
	 * @param baseLevel
	 *            базовый уровень скила - максимально возможный без заточки
	 * @param level
	 *            -
	 *            текущий уровень скила
	 * @return уровень скила
	 */
	public static int convertEnchantLevel(int baseLevel, int level)
	{
		if(level < 100)
			return level;
		int enchantLevel = baseLevel + ((level - level % 100) / 100 - 1) * 30 + level % 100;
		return level > 140 ? enchantLevel - 10 : enchantLevel;
	}

	public static L2SkillLearn getSkillLearn(int skillid, int level, ClassId classid, L2Clan clan)
	{
		if(clan != null)
		{
			L2SkillLearn[] clskills = getInstance().getAvailableClanSkills(clan);
			for(L2SkillLearn tmp : clskills)
				if(tmp.id == skillid && tmp.skillLevel == level)
					return tmp;
			return null;
		}

		if(_fishingSkills != null)
			for(L2SkillLearn tmp : _fishingSkills)
				if(tmp.id == skillid && tmp.skillLevel == level)
					return tmp;

		if(_transformationSkills != null)
			for(L2SkillLearn tmp : _transformationSkills)
				if(tmp.id == skillid && tmp.skillLevel == level)
					return tmp;

		for(L2SkillLearn tmp : _skillTrees.get(classid))
			if(tmp.id == skillid && tmp.skillLevel == level)
				return tmp;

		return null;
	}

	public L2SkillLearn[] getAvailableTransformationSkills(L2Player cha)
	{
		ArrayList<L2SkillLearn> result = new ArrayList<L2SkillLearn>();
		if(_transformationSkills == null)
		{
			_log.warning("Transformation skills not defined!");
			return new L2SkillLearn[0];
		}

		L2Skill[] oldSkills = cha.getAllSkillsArray();

		for(L2SkillLearn temp : _transformationSkills)
			if(temp.minLevel <= cha.getLevel())
			{
				boolean knownSkill = false;
				for(L2Skill s : oldSkills)
				{
					if(knownSkill)
						break;
					if(s.getId() == temp.id)
					{
						knownSkill = true;
						if(s.getLevel() == temp.skillLevel - 1)
							if(cha.getInventory().getItemByItemId(temp.getItemId()) != null)
								result.add(temp);

					}
				}

				if(!knownSkill && temp.skillLevel == 1)
					if(cha.getInventory().getItemByItemId(temp.getItemId()) != null)
						result.add(temp);
			}
		return result.toArray(new L2SkillLearn[result.size()]);
	}

	public L2SkillLearn[] getAvailableFishingSkills(L2Player cha)
	{
		ArrayList<L2SkillLearn> result = new ArrayList<L2SkillLearn>();
		if(_fishingSkills == null)
		{
			_log.warning("Fishing skills not defined!");
			return new L2SkillLearn[0];
		}

		L2Skill[] oldSkills = cha.getAllSkillsArray();

		for(L2SkillLearn temp : _fishingSkills)
			if(temp.minLevel <= cha.getLevel())
			{
				if(temp.getId() == 1368 && cha.getRace() != Race.dwarf)
					continue; // Expand Dwarven Craft

				boolean knownSkill = false;
				for(L2Skill s : oldSkills)
				{
					if(knownSkill)
						break;
					if(s.getId() == temp.id)
					{
						knownSkill = true;
						if(s.getLevel() == temp.skillLevel - 1)
							result.add(temp);
					}
				}

				if(!knownSkill && temp.skillLevel == 1)
					result.add(temp);
			}
		return result.toArray(new L2SkillLearn[result.size()]);
	}

	public byte getMinLevelForNewSkill(L2Player cha, ClassId classId)
	{
		byte minlevel = 0;
		// ArrayList<L2SkillLearn> result = new ArrayList<L2SkillLearn>();
		ArrayList<L2SkillLearn> skills = _skillTrees.get(classId);
		if(skills == null)
		{
			// the skilltree for this class is undefined, so we give an empty list
			_log.warning("Skilltree for class " + classId + " is not defined !");
			return minlevel;
		}

		// L2Skill[] oldSkills = cha.getAllSkills();

		for(L2SkillLearn temp : skills)
			if(temp.minLevel > cha.getLevel())
				if(minlevel == 0 || temp.minLevel < minlevel)
					minlevel = temp.minLevel;
		return minlevel;
	}

	public int getSkillCost(L2Player player, L2Skill skill)
	{
		// TODO снести этот костыль
		switch(skill.getId())
		{
			// Рыбацкие скилы
			case 1312:
			case 1313:
			case 1314:
			case 1315:
			case 1368:
			case 1369:
			case 1370:
			case 1371:
			case 1372:
				// Скилы трансформации
			case 617:
			case 618:
			case 541:
			case 544:
			case 547:
			case 550:
			case 553:
			case 556:
				return 0;
		}

		FastMap<Integer, FastMap<Integer, L2SkillLearn>> skt = _skillCostTable.get(player.getActiveClassId());
		if(skt == null)
			return Integer.MAX_VALUE;
		FastMap<Integer, L2SkillLearn> skillmap = skt.get(skill.getId());
		if(skillmap == null)
			return Integer.MAX_VALUE;
		L2SkillLearn skl = skillmap.get(1 + Math.max(player.getSkillLevel(skill.getId()), 0));
		if(skl == null)
			return Integer.MAX_VALUE;
		return skl.getSpCost();
	}

	public int getSkillRepCost(L2Clan clan, L2Skill skill)
	{
		int min = 100000000;
		int lvl = clan.getLeader().getPlayer().getSkillLevel(skill.getId());

		if(lvl > 0)
			lvl += 1;
		else
			lvl = 1;
		if(_clanSkills != null)
			for(L2SkillLearn tmp : _clanSkills)
			{
				if(tmp.id != skill.getId())
					continue;
				if(tmp.skillLevel != lvl)
					continue;
				if(tmp.minLevel > clan.getLevel())
					continue;
				min = Math.min(min, Math.round(tmp._repCost));
			}
		return min;
	}

	/**
	 * Возвращает true если скилл может быть изучен данным классом
	 * 
	 * @param player
	 * @param skill_id
	 * @param skill_level
	 * @return true/false
	 */
	public boolean isSkillPossible(L2Player player, int skillid, int level)
	{
		for(L2SkillLearn tmp : _clanSkills)
			if(tmp.id == skillid && tmp.skillLevel <= level)
				return true;

		ArrayList<L2SkillLearn> skills = _skillTrees.get(ClassId.values()[player.getActiveClassId()]);
		for(L2SkillLearn skilllearn : skills)
			if(skilllearn.id == skillid && skilllearn.skillLevel <= level)
				return true;

		return false;
	}
	
	public ArrayList<L2SkillLearn> getSkills(ClassId classId)
	{
		return _skillTrees.get(classId);
	}
	
	public ArrayList<L2SkillLearn> getTOPSkills(ClassId classId)
	{
		ArrayList<L2SkillLearn> result = new ArrayList<L2SkillLearn>();

		for(L2SkillLearn s : _skillTrees.get(classId))
		{
			boolean add = true;
			for(L2SkillLearn s11 : result)
				if(s11.getId() == s.getId())
					add = false;
			if(add)
				result.add(s);
		}
		return result;
	}

	public static void unload()
	{
		if(_instance != null)
			_instance = null;
		_skillTrees.clear();
		_skillCostTable.clear();
		_enchant.clear();
		_fishingSkills.clear();
		_clanSkills.clear();
		_transformationSkills.clear();
		_unimplemented_skills.clear();
	}
}