package l2d.game.skills.conditions;

import l2d.game.skills.Env;

/**
 * @author Felixx
 * Возвращает <b>true</b> если ЦП МЕНЬШЕ указанного в скилле.
 */
public class ConditionPlayerMinPercentCp extends Condition
{
	private final float _cp;

	public ConditionPlayerMinPercentCp(int cp)
	{
		_cp = cp / 100f;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.getCurrentCp() <= _cp * env.character.getMaxCp();
	}
}