package l2d.game.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import l2d.game.model.base.ClassId;
import l2d.game.model.base.Experience;
import l2d.game.tables.SkillTable;

/**
 * Character Sub-Class Definition
 * <BR>
 * Used to store key information about a character's sub-class.
 *
 * @author Tempy
 */
public class L2SubClass
{
	private int _class = 0;
	private long _exp = Experience.LEVEL[40], maxExp = Experience.LEVEL[Experience.LEVEL.length - 1];
	private int _sp = 0;
	private int _pvp = 0;
	private byte _level = 40, minLevel = 1, maxLevel = 99;
	private double _Hp = 1, _Mp = 1, _Cp = 1;
	private boolean _active = false, _isBase = false;
	private L2Player _player;
	private DeathPenalty _dp;
	private String _skills = "";
	private ArrayList<L2Skill> _skillsList = null;

	public L2SubClass()
	{}

	public int getClassId()
	{
		return _class;
	}

	public long getExp()
	{
		return _exp;
	}

	public long getMaxExp()
	{
		return maxExp;
	}

	public void addExp(long val)
	{
		setExp(_exp + val);
	}

	public int getSp()
	{
		return _sp;
	}

	public void addSp(long val)
	{
		long new_sp = _sp + val;
		setSp(new_sp < 0 ? 0 : new_sp > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) new_sp);
	}

	public byte getLevel()
	{
		return _level;
	}

	public void setClassId(final int classId)
	{
		_class = classId;
	}

	public void setExp(final long val)
	{
		if(val < 0)
			_exp = 0;
		else if(val <= maxExp)
			_exp = val;
	}

	public void setSp(final int spValue)
	{
		_sp = spValue < 0 ? 0 : spValue;
	}

	public void setHp(final double hpValue)
	{
		_Hp = hpValue;
	}

	public double getHp()
	{
		return _Hp;
	}

	public void setMp(final double mpValue)
	{
		_Mp = mpValue;
	}

	public double getMp()
	{
		return _Mp;
	}

	public void setCp(final double cpValue)
	{
		_Cp = cpValue;
	}

	public double getCp()
	{
		return _Cp;
	}

	public boolean setLevel(byte val)
	{
		if(val > maxLevel)
		{
			_level = maxLevel;
			_exp = maxExp;
		}
		else if(val < minLevel)
			_level = minLevel;
		else
			_level = val;
		return _level == val;
	}

	public boolean incLevel()
	{
		return setLevel((byte) (_level + 1));
	}

	public boolean decLevel()
	{
		return setLevel((byte) (_level - 1));
	}

	public void setActive(final boolean active)
	{
		_active = active;
	}

	public boolean isActive()
	{
		return _active;
	}

	public void setBase(final boolean base)
	{
		_isBase = base;
		minLevel = _isBase ? (byte) 1 : (byte) 40;
		maxLevel = _isBase ? (byte) Experience.getMaxLevel() : (byte) Experience.getMaxSubLevel();
		maxExp = Experience.LEVEL[maxLevel + 1] - 1;
	}

	public boolean isBase()
	{
		return _isBase;
	}

	public DeathPenalty getDeathPenalty()
	{
		if(_dp == null)
			_dp = new DeathPenalty(_player, (byte) 0);
		return _dp;
	}

	public void setDeathPenalty(DeathPenalty dp)
	{
		_dp = dp;
	}

	public void setPlayer(L2Player player)
	{
		_player = player;
	}

	public String getSkills()
	{
		return _skills;
	}

	public void setSkills(String skills)
	{
		_skills = skills;
		_skillsList = createSkillsList();
	}

	private ArrayList<L2Skill> createSkillsList()
	{
		ArrayList<L2Skill> result = new ArrayList<L2Skill>();
		if(_skills.isEmpty())
			return result;
		String[] temp = _skills.split(";");
		HashMap<String, Integer> counts = new HashMap<String, Integer>();
		for(String i : temp)
		{
			Integer count = counts.get(i);
			if(count == null)
				count = 0;
			counts.put(i, count + 1);
		}
		for(Entry<String, Integer> entry : counts.entrySet())
		{
			L2Skill skill = SkillTable.getInstance().getInfo(Integer.parseInt(entry.getKey()), entry.getValue());
			if(skill != null)
				result.add(skill);
			else
				System.out.println("Not found skill id: " + entry.getKey() + ", level: " + entry.getValue());
		}
		return result;
	}

	public ArrayList<L2Skill> getSkillsList()
	{
		if(_skillsList == null)
			return new ArrayList<L2Skill>();
		return _skillsList;
	}

	@Override
	public String toString()
	{
		return ClassId.values()[_class].toString() + " " + _level;
	}

	public void setPvPCount(int count)
	{
		_pvp = count;
	}
	
	public int getPvPCount()
	{
		return _pvp;
	}
}