package com.lineage.game.skills.conditions;

import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Player;
import com.lineage.game.skills.Env;

public final class ConditionUsingItemType extends Condition
{
	private final int _mask;

	public ConditionUsingItemType(int mask)
	{
		_mask = mask;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;
		Inventory inv = ((L2Player) env.character).getInventory();
		return (_mask & inv.getWearedMask()) != 0;
	}
}
