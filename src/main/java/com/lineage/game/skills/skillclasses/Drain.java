package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.skills.Formulas;
import com.lineage.game.templates.StatsSet;

public class Drain extends L2Skill
{
	private float _absorbPart;
	private int _absorbAbs;

	public Drain(final StatsSet set)
	{
		super(set);

		_absorbPart = set.getFloat("absorbPart", 0.f);
		_absorbAbs = set.getInteger("absorbAbs", 0);
	}

	@Override
	public void useSkill(final L2Character activeChar, final FastList<L2Character> targets)
	{
		final int sps = isSSPossible() ? activeChar.getChargedSpiritShot() : 0;
		final boolean ss = isSSPossible() && activeChar.getChargedSoulShot();

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();

			if(getPower() > 0 || _absorbAbs > 0) // Если == 0 значит скилл "отключен"
			{
				if(target.isDead() && _targetType != SkillTargetType.TARGET_CORPSE)
					continue;

				double hp = 0.;
				final double targetHp = target.getCurrentHp();

				if(_targetType != SkillTargetType.TARGET_CORPSE)
				{
					if(target.checkReflectSkill(activeChar, this))
						target = activeChar;

					final double damage = isMagic() ? Formulas.calcMagicDam(activeChar, target, this, sps) : Formulas.calcPhysDam(activeChar, target, this, false, false, ss).damage;
					final double targetCP = target.getCurrentCp();

					// Нельзя восстанавливать HP из CP
					if(damage > targetCP || !target.isPlayer())
						hp = (damage - targetCP) * _absorbPart;

					target.reduceCurrentHp(damage, activeChar, this, true, true, false, true);
				}

				if(_absorbAbs == 0 && _absorbPart == 0)
					continue;

				hp += _absorbAbs;

				// Нельзя восстановить больше hp, чем есть у цели.
				if(hp > targetHp && _targetType != SkillTargetType.TARGET_CORPSE)
					hp = targetHp;

				hp += activeChar.getCurrentHp();

				if(!(target instanceof L2DoorInstance) && !activeChar.isHealBlocked())
					activeChar.setCurrentHp(hp, false);

				if(target.isDead() && _targetType == SkillTargetType.TARGET_CORPSE && target.isNpc())
				{
					activeChar.getAI().setAttackTarget(null);
					((L2NpcInstance) target).endDecayTask();
				}
			}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isMagic() ? sps != 0 : ss)
			activeChar.unChargeShots(isMagic());
	}
}