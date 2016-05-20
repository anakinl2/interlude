package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Env;
import l2d.game.skills.Stats;

public class EffectHeal extends L2Effect
{
	public EffectHeal(final Env env, final EffectTemplate template)
	{
		super(env, template);
		final double newHp = calc() * _effected.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100;
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