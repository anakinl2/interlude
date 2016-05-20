package ai;

import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;

/**
 * Моб Fighter не использует рандом валк
 */
public class NoRndWalkFighter extends Fighter
{
	public NoRndWalkFighter(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}
}