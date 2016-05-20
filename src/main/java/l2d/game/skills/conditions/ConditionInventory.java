package l2d.game.skills.conditions;

import l2d.game.skills.Env;

public abstract class ConditionInventory extends Condition implements ConditionListener
{
	protected final short _slot;

	public ConditionInventory(short slot)
	{
		_slot = slot;
	}

	@Override
	public abstract boolean testImpl(Env env);
}