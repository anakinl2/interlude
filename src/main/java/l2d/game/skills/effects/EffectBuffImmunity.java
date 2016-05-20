package l2d.game.skills.effects;

import l2d.game.model.L2Effect;
import l2d.game.skills.Env;

public final class EffectBuffImmunity extends L2Effect
{
	public EffectBuffImmunity(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		getEffected().setBuffImmunity(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().setBuffImmunity(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}