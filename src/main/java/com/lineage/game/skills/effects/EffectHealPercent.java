package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Env;
import com.lineage.game.skills.Stats;

public class EffectHealPercent extends L2Effect
{
	public EffectHealPercent(final Env env, final EffectTemplate template)
	{
		super(env, template);
		final double base = calc() * _effected.getMaxHp() / 100;
		final double newHp = base * _effected.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100;
		double addToHp = Math.min(Math.max(0, _effected.getMaxHp() - _effected.getCurrentHp()), newHp);
		_effected.sendPacket(new SystemMessage(SystemMessage.S1_HPS_HAVE_BEEN_RESTORED).addNumber(Math.round(addToHp)));
		if(addToHp > 0)
			_effected.setCurrentHp(addToHp + _effected.getCurrentHp(), false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}