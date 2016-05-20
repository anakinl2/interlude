package com.lineage.game.model.base;

import com.lineage.game.model.L2Player;

public final class L2EnchantSkillLearn
{
	// these two build the primary key
	private final int _id;
	private final int _level;

	// not needed, just for easier debug
	private final String _name;
	private final String _type;

	private final int _spCost;
	private final int _baseLvl;
	private final int _maxLvl;
	private final int _minSkillLevel;
	private final long _exp;

	public L2EnchantSkillLearn(int id, int lvl, String name, String type, int minSkillLvl, int baseLvl, int maxLvl, int cost)
	{
		_id = id;
		_level = lvl;
		_baseLvl = baseLvl;
		_maxLvl = maxLvl;
		_minSkillLevel = minSkillLvl;
		_name = name.intern();
		_type = type.intern();
		_spCost = cost;
		_exp = cost * 10;
	}

	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return _id;
	}

	/**
	 * @return Returns the level.
	 */
	public int getLevel()
	{
		return _level;
	}

	/**
	 * @return Returns the minLevel.
	 */
	public int getBaseLevel()
	{
		return _baseLvl;
	}

	/**
	 * @return Returns the minSkillLevel.
	 */
	public int getMinSkillLevel()
	{
		return _minSkillLevel;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * @return Returns the spCost.
	 */
	public int getSpCost()
	{
		return _spCost;
	}

	public long getExpCost()
	{
		return _exp;
	}

	/** Шанс заточки скилов 2й профы */
	private static final int[][] _chance = { {},
			//76  77  78  79  80  81  82  83  84  85
			{ 82, 92, 97, 98, 99}, // 1
			{ 80, 90, 95, 97, 98}, // 2
			{ 78, 88, 93, 96, 98}, // 3
			{ 40, 82, 92, 96, 98}, // 4
			{ 30, 80, 90, 95, 97}, // 5
			{ 20, 78, 88, 93, 96}, // 6
			{ 14, 40, 82, 92, 96}, // 7
			{ 10, 30, 80, 90, 95}, // 8
			{ 06, 20, 78, 88, 93}, // 9
			{ 06, 14, 40, 82, 92}, // 10
			{ 02, 10, 30, 80, 90}, // 11
			{ 02, 06, 20, 78, 88}, // 12
			{ 02, 02, 14, 40, 82}, // 13
			{ 01, 02, 10, 30, 80}, // 14
			{ 01, 02, 06, 20, 78}, // 15
			{ 01, 01, 02, 10, 30}, // 16
			{ 01, 01, 02, 06, 20}, // 17
			{ 01, 01, 02, 02, 14}, // 18
			{ 01, 01, 01, 02, 10}, // 19
			{ 01, 01, 01, 02, 06}, // 20
			{ 01, 01, 01, 01, 02}, // 21
			{ 01, 01, 01, 01, 02}, // 22
			{ 01, 01, 01, 01, 02}, // 23
			{ 01, 01, 01, 01, 01}, // 24
			{ 01, 01, 01, 01, 01}, // 25
			{ 01, 01, 01, 01, 01}, // 26
			{ 01, 01, 01, 01, 01}, // 27
			{ 01, 01, 01, 01, 01}, // 28
			{ 01, 01, 01, 01, 01}, // 29
			{ 01, 01, 01, 01, 01}, // 30
	};

	/**
	 * Шанс успешной заточки
	 */
	public int getRate(L2Player ply)
	{
		int level = _level > 140 ? (_level - 40) % 100 : _level % 100;
		int chance = Math.min(_chance[level].length - 1, ply.getLevel() - 76);
		return _chance[level][chance];
	}

	public String getType()
	{
		return _type;
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + _id;
		result = PRIME * result + _level;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		if(!(obj instanceof L2EnchantSkillLearn))
			return false;
		L2EnchantSkillLearn other = (L2EnchantSkillLearn) obj;
		return getId() == other.getId() && getLevel() == other.getLevel();
	}
}