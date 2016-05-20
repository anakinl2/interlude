package ai;

import l2d.game.ai.Mystic;
import l2d.game.model.L2Character;

/**
 * Моб Mystic не использует рандом валк
 */
public class NoRndWalkMystic extends Mystic
{
	public NoRndWalkMystic(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}
}