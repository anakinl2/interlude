package l2d.status;

import l2d.game.model.L2Character;
import l2d.game.model.L2Object;

public class DummyL2Object extends L2Object
{
	public DummyL2Object()
	{
		super(-1);
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
}