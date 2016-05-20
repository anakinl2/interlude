package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Stats;
import com.lineage.game.templates.StatsSet;

public class CombatPointHeal extends L2Skill
{
	public CombatPointHeal(StatsSet set)
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
			double maxNewCp = _power * target.calcStat(Stats.CPHEAL_EFFECTIVNESS, 100, null, null) / 100;
			double addToCp = Math.min(Math.max(0.0, target.getMaxCp() - target.getCurrentCp()), maxNewCp);
			target.setCurrentCp(addToCp + target.getCurrentCp());
			target.sendPacket(new SystemMessage(SystemMessage.S1_CPS_WILL_BE_RESTORED).addNumber((int) addToCp));
			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}
