package com.lineage.util.parsers;

public class L2Other
{

	int _skillId = 0;
	int _skilllvl = 0;
	int _mp_consume = 0;
	int _cast_range = 0;
	int _is_magic = 0;
	int _hp_consume = 0;

	
	public L2Other(int skillId, int skilllvl, int mp_consume, int cast_range, int is_magic, int hp_consume)
	{
		_skillId = skillId;
		_skilllvl = skilllvl;
		_mp_consume = mp_consume;
		_cast_range = cast_range;
		_is_magic = is_magic;
		_hp_consume = hp_consume;	
	}
}
