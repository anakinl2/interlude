package ai;

import java.util.Calendar;

import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;

public class RoyalRushNpc extends DefaultAI
{
	public RoyalRushNpc(L2Character actor)
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
		if(actor != null && Calendar.getInstance().get(Calendar.MINUTE) >= 54)
		{
			actor.deleteMe();
			return false;
		}
		return super.thinkActive();
	}
}