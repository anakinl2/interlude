package com.lineage.game.skills.inits;

import com.lineage.game.skills.Env;

public final class InitConst extends InitFunc
{
	private double _value;

	public InitConst(double value)
	{
		_value = value;
	}

	@Override
	public void calc(Env env)
	{
		env.value = _value;
	}
}