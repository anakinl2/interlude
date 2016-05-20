package com.lineage.game.serverpackets;

import javolution.util.FastTable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;

/**
 * format d (dddc)
 */
public class SkillList extends L2GameServerPacket
{
	private FastTable<L2Skill> _skills;

	public SkillList(final L2Player p)
	{
		_skills = new FastTable<L2Skill>();
		_skills.addAll(p.getAllSkills());
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x58);
		writeD(_skills.size());

		for(final L2Skill temp : _skills)
		{
			writeD(temp.isActive() || temp.isToggle() ? 0 : 1); // deprecated? клиентом игнорируется
			writeD(temp.getDisplayLevel());
			writeD(temp.getDisplayId());
			writeC(0x00); // иконка скилла серая если не 0
		}
	}
}