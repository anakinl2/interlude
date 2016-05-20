package com.lineage.game.skills.effects;

import com.lineage.game.model.L2Effect;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Env;

public class EffectHealCPPercent extends L2Effect
{
	public EffectHealCPPercent(final Env env, final EffectTemplate template)
	{
		super(env, template);
		if(_effected.isDead() || _effected.isHealBlocked())
			return;
		final double newCp = calc() * _effected.getMaxCp() / 100;
		double addToCp = Math.min(Math.max(0, _effected.getMaxCp() - _effected.getCurrentCp()), newCp);
		_effected.sendPacket(new SystemMessage(SystemMessage.S1_WILL_RESTORE_S2S_CP).addNumber((int) addToCp));
		if(addToCp > 0)
			_effected.setCurrentCp(addToCp + _effected.getCurrentCp());
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}