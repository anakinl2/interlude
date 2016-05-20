package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.skills.effects.EffectSeed;
import com.lineage.game.templates.StatsSet;

public class L2SkillSeed extends L2Skill
{

	public L2SkillSeed(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		if(activeChar.isAlikeDead())
			return;

		// Update Seeds Effects
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target.isAlikeDead() && getTargetType() != SkillTargetType.TARGET_CORPSE)
				continue;

			EffectSeed oldEffect = (EffectSeed) target.getEffectList().getEffectBySkillId(getId());

			if(oldEffect == null)
				getEffects(activeChar, target, false, false);
			else
				oldEffect.increasePower();

			//	if(target.getEffectList().getEffectByType(EffectType.SEED) != null)
			//		target.getEffectList().getEffectByType(EffectType.SEED).rescheduleEffect();
		}
	}
}
