package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.skills.Env;
import l2d.game.skills.skillclasses.NegateStats;

public class EffectBlockStat extends L2Effect
{
	public EffectBlockStat(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		_effected.addBlockStats(((NegateStats) _skill).getNegateStats());
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.removeBlockStats(((NegateStats) _skill).getNegateStats());
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}