package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Stats;
import l2d.game.templates.StatsSet;

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
