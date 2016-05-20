package com.lineage.game.skills.conditions;

import com.lineage.game.skills.Env;

/**
 * @author Felixx
 * Возвращает <b>true</b> если ЦП БОЛЬШЕ указанного в скилле.
 */
public class ConditionPlayerMaxPercentCp extends Condition
{
	private final float _cp;

	public ConditionPlayerMaxPercentCp(int cp)
	{
		_cp = cp / 100f;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getCurrentCp() >= _cp * env.character.getMaxCp();
	}
}