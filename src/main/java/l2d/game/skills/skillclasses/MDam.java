package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.skills.Formulas;
import l2d.game.templates.StatsSet;

public class MDam extends L2Skill
{
	private final boolean _basedOnTargetDebuff;

	public MDam(final StatsSet set)
	{
		super(set);
		_deathlink = set.getBool("deathlink", false);
		_basedOnTargetDebuff = set.getBool("basedOnTargetDebuff", false);
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

				double damage = Formulas.calcMagicDam(activeChar, target, this, sps);

				if(_basedOnTargetDebuff)
					damage *= 1 + 0.05 * target.getEffectList().getAllEffects().size();

				if(damage < 1)
					damage = 1;

				target.reduceCurrentHp(damage, activeChar, this, true, true, false, true);
			}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());

		if(isSuicideAttack())
			activeChar.doDie(null);
	}
}