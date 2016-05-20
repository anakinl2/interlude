package com.lineage.game.serverpackets;

import javolution.util.FastList;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.skills.SkillTimeStamp;

/**
 * @author: Death
 * @date: 16/2/2007
 * @time: 21:25:14
 */
public class SkillCoolTime extends L2GameServerPacket
{
	/**
	 * Example (C4, 656)
	 * C1 01 00 00 00 6E 00 00 00 02 00 00 00 9D 05 00 00 83 05 00 00 - Ultimate Defence level 2
	 * possible structure
	 * c - packet number
	 * d - size of skills ???
	 * now cycle?????
	 * d - skill id
	 * d - skill level
	 * d - 1437, total reuse delay
	 * d - 1411, remaining reuse delay
	 */

	FastList<L2Skill> _sList;
	FastList<SkillTimeStamp> _tList;

	public SkillCoolTime(L2Player player)
	{
		_sList = new FastList<L2Skill>();
		_tList = new FastList<SkillTimeStamp>();

		for(L2Skill skill : player.getAllSkillsArray())
		{
			if(skill.isLikePassive())
				continue;
			if(player.getSkillReuseTimeStamps().containsKey(skill.getId()))
			{
				_sList.add(skill);
				_tList.add(player.getSkillReuseTimeStamps().get(skill.getId()));
			}
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xc7); //packet type
		writeD(_sList.size()); // Size of list

		for(int i = 0; i < _sList.size(); i++)
		{
			writeD(_sList.get(i).getId()); // Skill Id
			writeD(_sList.get(i).getLevel()); // Skill Level
			writeD((int) _tList.get(i).getReuseBasic() / 1000); // Total reuse delay, seconds
			writeD((int) _tList.get(i).getReuseCurrent() / 1000); // Time remaining, seconds
		}
	}
}