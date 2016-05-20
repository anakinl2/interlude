package l2d.game.skills.conditions;

import l2d.game.model.L2Player;
import l2d.game.skills.Env;

public class ConditionPlayerInvSize extends Condition
{
	private final int _size;

	public ConditionPlayerInvSize(int size)
	{
		_size = size;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.character instanceof L2Player)
			return ((L2Player) env.character).getInventory().getSize() <= ((L2Player) env.character).getInventoryLimit() - _size;

		return true;
	}
}
