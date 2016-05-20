package l2d.game.skills.conditions;

import l2d.game.skills.Env;

public final class ConditionItemId extends Condition
{
	private final short _itemId;

	public ConditionItemId(short itemId)
	{
		_itemId = itemId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.item == null)
			return false;
		return env.item.getItemId() == _itemId;
	}
}