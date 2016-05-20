package com.lineage.game.skills.conditions;

import com.lineage.game.skills.Env;
import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;

public final class ConditionSlotItemId extends ConditionInventory
{
	private final int _itemId;

	private final int _enchantLevel;

	public ConditionSlotItemId(short slot, int itemId, int enchantLevel)
	{
		super(slot);
		_itemId = itemId;
		_enchantLevel = enchantLevel;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;
		Inventory inv = ((L2Player) env.character).getInventory();
		L2ItemInstance item = inv.getPaperdollItem(_slot);
		if(item == null)
			return _itemId == 0;
		return item.getItemId() == _itemId && item.getEnchantLevel() >= _enchantLevel;
	}
}
