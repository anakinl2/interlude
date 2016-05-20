package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.skills.Formulas;
import com.lineage.game.templates.StatsSet;

public class ManaDam extends L2Skill
{
	public ManaDam(final StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(final L2Character activeChar, final FastList<L2Character> targets)
	{
		final int sps = isSSPossible() ? activeChar.getChargedSpiritShot() : 0;

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target == null || target.isDead())
				continue;

			if(getPower() > 0) // Если == 0 значит скилл "отключен"
			{
				if(target.checkReflectSkill(activeChar, this))
					target = activeChar;

				final double damage = Formulas.calcMagicDam(activeChar, target, this, sps) / 4;

				target.reduceCurrentMp(damage, activeChar);
			}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}