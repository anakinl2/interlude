package com.lineage.game.skills.funcs;

import com.lineage.game.skills.Env;
import com.lineage.game.skills.Stats;

public class FuncMul extends Func
{
	public FuncMul(final Stats stat, final int order, final Object owner, final double value)
	{
		super(stat, order, owner, value);
	}

	@Override
	public void calc(final Env env)
	{
		if(_cond == null || _cond.test(env))
			env.value *= _value;
	}
}