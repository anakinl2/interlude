package com.lineage.game.skills.inits;

import com.lineage.game.model.L2Character;
import com.lineage.game.skills.Env;

public final class Init_rShld extends InitFunc
{
	@Override
	public void calc(Env env)
	{
		L2Character cha = env.character;
		if(cha == null || cha.isPlayer())
			env.value = 0.;
		else
			env.value = cha.getTemplate().baseShldRate;
	}
}