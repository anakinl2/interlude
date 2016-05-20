package com.lineage.game.serverpackets;

import java.util.Vector;

public class ExEnchantSkillList extends L2GameServerPacket
{

	private final Vector<Skill> _skills;

	class Skill
	{
		public int id;
		public int level;

		Skill(int id, int nextLevel)
		{
			this.id = id;
			level = nextLevel;
		}
	}

	public void addSkill(int id, int level)
	{
		_skills.add(new Skill(id, level));
	}

	public ExEnchantSkillList()
	{
		_skills = new Vector<Skill>();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x17);

		writeD(_skills.size());
		for(Skill sk : _skills)
		{
			writeD(sk.id);
			writeD(sk.level);
			writeD(123);
			writeQ(123);
		}
	}
}