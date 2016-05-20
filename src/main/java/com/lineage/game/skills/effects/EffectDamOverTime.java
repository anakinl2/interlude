package com.lineage.game.skills.effects;

import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Effect;
import com.lineage.game.skills.Env;

public class EffectDamOverTime extends L2Effect
{
	// TODO выставить более точные значения
	private static int[] bleed = new int[] { 18, 27, 38, 50, 62, 81, 96, 114, 132, 153, 174, 200 };
	private static int[] poison = new int[] { 24, 54, 72, 93, 114, 132, 144, 160, 175, 200, 215, 230 };

	public EffectDamOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public boolean onActionTime()
	{
		if(_effected.isDead())
			return false;

		double damage = calc();

		if(getSkill().isOffensive())
			damage *= 2;

		if(damage < 2)
			switch(getEffectType())
			{
				case Poison:
					damage = poison[getStackOrder() - 1];
					break;
				case Bleed:
					damage = bleed[getStackOrder() - 1];
					break;
			}

		if(damage > _effected.getCurrentHp() - 1 && !_effected.isNpc())
		{
			if(!getSkill().isOffensive())
				_effected.sendPacket(Msg.NOT_ENOUGH_HP);
			return false;
		}

		_effected.reduceCurrentHp(damage, _effected.isPlayer() ? _effected : _effector, getSkill(), !_effected.isNpc(), !getSkill().isToggle(), _effector.isNpc() || getSkill().isToggle() || _effected == _effector, false);

		return true;
	}
}