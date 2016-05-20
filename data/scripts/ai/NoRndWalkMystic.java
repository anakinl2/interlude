package ai;

import com.lineage.game.ai.Mystic;
import com.lineage.game.model.L2Character;

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