package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Env;
import l2d.game.skills.Stats;

public class EffectManaHealPercent extends L2Effect
{
	public EffectManaHealPercent(final Env env, final EffectTemplate template)
	{
		super(env, template);
		if(_effected.isDead() || _effected.isHealBlocked())
			return;
		final double base = calc() * _effected.getMaxMp() / 100;
		final double newMp = base * _effected.calcStat(Stats.MANAHEAL_EFFECTIVNESS, 100, null, null) / 100;
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