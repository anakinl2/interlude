package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Env;
import com.lineage.game.skills.Stats;

public class EffectManaHeal extends L2Effect
{
	public EffectManaHeal(final Env env, final EffectTemplate template)
	{
		super(env, template);
		if(_effected.isDead() || _effected.isHealBlocked())
			return;
		final double newMp = calc() * _effected.calcStat(Stats.MANAHEAL_EFFECTIVNESS, 100, null, null) / 100;
		double addToMp = Math.min(Math.max(0, _effected.getMaxMp() - _effected.getCurrentMp()), newMp);
		_effected.sendPacket(new SystemMessage(SystemMessage.S1_MPS_HAVE_BEEN_RESTORED).addNumber(Math.round(addToMp)));
		if(addToMp > 0)
			_effected.setCurrentMp(addToMp + _effected.getCurrentMp());
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}