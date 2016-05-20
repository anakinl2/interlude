package l2d.game.skills.conditions;

import l2d.game.model.Inventory;
import l2d.game.model.L2Player;
import l2d.game.skills.Env;

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
