package com.lineage.game.skills.effects;

import java.util.ArrayList;

import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Summon;
import com.lineage.game.skills.Env;
import com.lineage.util.Rnd;

public class EffectDiscord extends L2Effect
{
	private boolean effected_correct_target;

	public EffectDiscord(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();

		effected_correct_target = false;
		int skilldiff = _effected.getLevel() - _skill.getMagicLevel();
		int lvldiff = _effected.getLevel() - _effector.getLevel();
		if(skilldiff > 10 || skilldiff > 5 && Rnd.chance(30) || Rnd.chance(Math.abs(lvldiff) * 2))
		{
			exit();
			return;
		}

		boolean multitargets = _skill.isAoE();

		if(!_effected.isMonster())
		{
			if(!multitargets)
				getEffector().sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			exit();
			return;
		}

		if(_effected.isFearImmune() || _effected.isRaid())
		{
			if(!multitargets)
				getEffector().sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			exit();
			return;
		}

		// Discord нельзя наложить на осадных саммонов
		if(_effected instanceof L2Summon && ((L2Summon) _effected).isSiegeWeapon())
		{
			if(!multitargets)
				getEffector().sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			exit();
			return;
		}

		if(_effected.isInZonePeace())
		{
			if(!multitargets)
				getEffector().sendPacket(Msg.YOU_MAY_NOT_ATTACK_IN_A_PEACEFUL_ZONE);
			exit();
			return;
		}

		effected_correct_target = true;
		_effected.startConfused();
		onActionTime();
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(!effected_correct_target)
			return;
		_effected.stopConfused();
		_effected.setWalking();
		_effected.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}

	@Override
	public boolean onActionTime()
	{
		if(!effected_correct_target)
			return true;

		ArrayList<L2Character> targetList = new ArrayList<L2Character>();

		for(L2Character character : _effected.getAroundCharacters(900, 200))
			if(character.isNpc() && character != getEffected())
				targetList.add(character);

		// if there is no target, exit function
		if(targetList.size() == 0)
			return true;

		// Choosing randomly a new target
		L2Character target = targetList.get(Rnd.get(targetList.size()));

		// Attacking the target
		_effected.setRunning();
		_effected.getAI().Attack(target, true);

		return false;
	}
}