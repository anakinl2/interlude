package com.lineage.game.skills.effects;

import com.lineage.game.cache.Msg;
import com.lineage.game.skills.Stats;
import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Env;

public final class EffectSilentMove extends L2Effect
{
	public EffectSilentMove(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(_effected.isPlayer())
			((L2Player) _effected).setSilentMoving(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(_effected.isPlayer())
			((L2Player) _effected).setSilentMoving(false);
	}

	@Override
	public boolean onActionTime()
	{
		if(_effected.isDead())
			return false;

		if(!getSkill().isToggle())
			return false;

		double manaDam = calc();
		if(getSkill().isMagic())
			manaDam = _effected.calcStat(Stats.MP_MAGIC_SKILL_CONSUME, manaDam, null, getSkill());
		else
			manaDam = _effected.calcStat(Stats.MP_PHYSICAL_SKILL_CONSUME, manaDam, null, getSkill());

		if(manaDam > _effected.getCurrentMp())
		{
			_effected.sendPacket(Msg.NOT_ENOUGH_MP);
			_effected.sendPacket(new SystemMessage(SystemMessage.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED).addSkillName(getSkill().getId(), getSkill().getDisplayLevel()));
			return false;
		}

		_effected.reduceCurrentMp(manaDam, null);
		return true;
	}
}