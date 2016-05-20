package l2d.game.skills.conditions;

import l2d.game.model.Inventory;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.skills.Env;

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