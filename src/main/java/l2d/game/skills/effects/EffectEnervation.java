package l2d.game.skills.effects;

import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Effect;
import l2d.game.skills.Env;

public class EffectEnervation extends L2Effect
{

	public EffectEnervation(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(_effected.isMonster())
			((DefaultAI) _effected.getAI()).set("DebuffIntention", 0.5D);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(_effected.isMonster())
			((DefaultAI) _effected.getAI()).set("DebuffIntention", 1.0D);
	}
}
