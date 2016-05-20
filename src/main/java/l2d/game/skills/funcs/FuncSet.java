package l2d.game.skills.funcs;

import l2d.game.skills.Env;
import l2d.game.skills.Stats;

public class FuncSet extends Func
{
	public FuncSet(final Stats stat, final int order, final Object owner, final double value)
	{
		super(stat, order, owner, value);
	}

	@Override
	public void calc(final Env env)
	{
		if(_cond == null || _cond.test(env))
			env.value = _value;
	}
}