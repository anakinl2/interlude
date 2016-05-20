package com.lineage.game.skills.conditions;

import com.lineage.game.skills.Env;

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