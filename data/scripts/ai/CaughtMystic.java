package ai;

import com.lineage.game.ai.Mystic;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.instances.L2NpcInstance;

public class CaughtMystic extends Mystic
{
	private static final int TIME_TO_LIVE = 60000;
	private final long TIME_TO_DIE = System.currentTimeMillis() + TIME_TO_LIVE;

	public CaughtMystic(L2Character actor)
	{
		super(actor);
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor != null && System.currentTimeMillis() >= TIME_TO_DIE)
		{
			 actor.deleteMe();
			return false;
		}
		return super.thinkActive();
	}
}