package com.lineage.game.skills.conditions;

import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.skills.Env;

final class ConditionSlotItemType extends ConditionInventory
{
	private final int _mask;

	ConditionSlotItemType(short slot, int mask)
	{
		super(slot);
		_mask = mask;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;
		Inventory inv = ((L2Player) env.character).getInventory();
		L2ItemInstance item = inv.getPaperdollItem(_slot);
		if(item == null)
			return false;
		return (item.getItem().getItemMask() & _mask) != 0;
	}
}