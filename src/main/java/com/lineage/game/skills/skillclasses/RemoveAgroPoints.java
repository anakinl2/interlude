package com.lineage.game.skills.skillclasses;

import com.lineage.game.templates.StatsSet;
import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Character.HateInfo;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.SystemMessage;

public class RemoveAgroPoints extends L2Skill
{
	public RemoveAgroPoints(final StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(final L2Character activeChar, final FastList<L2Character> targets)
	{
		if(activeChar == null)
			return;
		if(activeChar.getTarget() == null)
			return;
		if(!activeChar.getTarget().isMonster())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.INVALID_TARGET));
			return;
		}
		L2Character target = (L2Character) activeChar.getTarget();
		if(target.getHateList() == null)
			return;
		HateInfo ai = activeChar.getHateList().get(target);
		if(ai == null)
			return;
		ai.hate -= _power;
		if(ai.hate < 0)
			ai.hate = 0;
		activeChar.getHateList().put((L2NpcInstance) target, ai);
	}
}
