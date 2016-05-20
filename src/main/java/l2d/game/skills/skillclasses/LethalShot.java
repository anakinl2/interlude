package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.skills.Formulas;
import l2d.game.templates.StatsSet;

public class LethalShot extends L2Skill
{
	public LethalShot(final StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(final L2Character activeChar, final FastList<L2Character> targets)
	{
		final boolean ss = activeChar.getChargedSoulShot() && isSSPossible();
		if(ss)
			activeChar.unChargeShots(false);

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target.isDead())
				continue;

			if(target.checkReflectSkill(activeChar, this))
				target = activeChar;

			if(getPower() > 0) // Если == 0 значит скилл "отключен"
			{
				final double damage = Formulas.calcPhysDam(activeChar, target, this, false, false, ss).damage;
				target.reduceCurrentHp(damage, activeChar, this, true, true, false, true);
			}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}
	}
}