package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.skills.Env;

public final class EffectHealBlock extends L2Effect
{
	public EffectHealBlock(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		_effected.setHealBlocked(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.setHealBlocked(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}