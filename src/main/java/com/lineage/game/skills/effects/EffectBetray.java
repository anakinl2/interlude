package com.lineage.game.skills.effects;

import static com.lineage.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Summon;
import com.lineage.game.skills.Env;

public class EffectBetray extends L2Effect
{
	public EffectBetray(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(_effected instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) _effected;
			summon.setPossessed(true);
			summon.getAI().Attack(summon.getPlayer(), true);
		}
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(_effected instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) _effected;
			summon.setPossessed(false);
			summon.getAI().setIntention(AI_INTENTION_ACTIVE);
		}
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}