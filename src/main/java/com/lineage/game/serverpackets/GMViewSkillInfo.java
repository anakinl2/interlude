package com.lineage.game.serverpackets;

import java.util.Collection;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;

public class GMViewSkillInfo extends L2GameServerPacket
{
	private String char_name;
	private Collection<L2Skill> _skills;

	public GMViewSkillInfo(L2Player cha)
	{
		char_name = cha.getName();
		_skills = cha.getAllSkills();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x91);
		writeS(char_name);
		writeD(_skills.size());
		for(L2Skill skill : _skills)
		{
			if(skill.getId() > 9000)
				continue; // fake skills to change base stats

			writeD(skill.isLikePassive() ? 1 : 0);
			writeD(skill.getDisplayLevel());
			writeD(skill.getId());
			writeC(0x00); //c5
		}
	}
}