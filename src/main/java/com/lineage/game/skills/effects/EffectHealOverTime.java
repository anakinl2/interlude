package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.serverpackets.ExRegenMax;
import com.lineage.game.skills.Env;

public class EffectHealOverTime extends L2Effect
{
	public EffectHealOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();

		if(getEffected().isPlayer())
			getEffected().sendPacket(new ExRegenMax(calc(), (int) (getCount() * getPeriod() / 1000), Math.round(getPeriod() / 1000)));

		/**
			switch(getSkill().getId().intValue())
			{
				case 2031: // Lesser Healing Potion
					getEffected().sendPacket(new ExRegenMax(ExRegenMax.POTION_HEALING_LESSER));
					break;
				case 2032: // Healing Potion
					getEffected().sendPacket(new ExRegenMax(ExRegenMax.POTION_HEALING_MEDIUM));
					break;
				case 2037: // Greater Healing Potion
					getEffected().sendPacket(new ExRegenMax(ExRegenMax.POTION_HEALING_GREATER));
					break;
			}
		*/
	}

	@Override
	public boolean onActionTime()
	{
		if(getEffected().isDead())
			return false;

		getEffected().setCurrentHp(getEffected().getCurrentHp() + calc(), false);
		return true;
	}
}