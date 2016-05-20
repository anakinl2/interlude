package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.templates.StatsSet;

public class SPHeal extends L2Skill
{
	public SPHeal(StatsSet set)
	{
		super(set);
	}

	@Override
	public boolean checkCondition(final L2Character activeChar, final L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!activeChar.isPlayer())
			return false;

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			((L2Player) target).setSp(((L2Player) target).getSp() + (int) _power);
			((L2Player) target).sendChanges();

			getEffects(activeChar, target, getActivateRate() > 0, false);

			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED__S1S2).addNumber((int) _power).addString("SP"));
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}
