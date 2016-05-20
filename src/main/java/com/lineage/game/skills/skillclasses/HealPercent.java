package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.model.instances.L2SiegeHeadquarterquarterInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.templates.StatsSet;

public class HealPercent extends L2Skill
{
	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(target instanceof L2DoorInstance || target instanceof L2SiegeHeadquarterquarterInstance)
			return false;

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	public HealPercent(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target.isDead() || target.isHealBlocked())
				continue;

			getEffects(activeChar, target, getActivateRate() > 0, false);

			double addToHp = Math.min(Math.max(0, target.getMaxHp() - target.getCurrentHp()), target.getMaxHp() * _power / 100);
			if(addToHp > 0)
				target.setCurrentHp(addToHp + target.getCurrentHp(), false);
			if(target.isPlayer())
				if(activeChar != target)
					target.sendPacket(new SystemMessage(SystemMessage.XS2S_HP_HAS_BEEN_RESTORED_BY_S1).addString(activeChar.getName()).addNumber(Math.round(addToHp)));
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HPS_HAVE_BEEN_RESTORED).addNumber(Math.round(addToHp)));
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}
