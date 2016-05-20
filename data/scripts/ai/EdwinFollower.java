package ai;

import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Rnd;

public class EdwinFollower extends DefaultAI
{
	private static final int EDWIN_ID = 32072;
	private static final int DRIFT_DISTANCE = 350;
	private static final long wait_timeout = 15000;
	private L2NpcInstance _edwin;

	public EdwinFollower(L2Character actor)
	{
		super(actor);
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}

	@Override
	protected boolean randomAnimation()
	{
		return false;
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}

	@Override
	public void removeActor()
	{
		super.removeActor();
		_edwin = null;
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return false;
		if(_edwin == null)
		{
			// Ищем преследуемого не чаще, чем раз в 15 секунд, если по каким-то причинам его нету
			if(System.currentTimeMillis() > wait_timeout)
				for(L2NpcInstance npc : L2World.getAroundNpc(actor))
					if(npc.getNpcId() == EDWIN_ID)
					{
						_edwin = npc;
						return true;
					}
		}
		else if(!actor.isMoving)
		{
			int x = _edwin.getX() + Rnd.get(2 * DRIFT_DISTANCE) - DRIFT_DISTANCE;
			int y = _edwin.getY() + Rnd.get(2 * DRIFT_DISTANCE) - DRIFT_DISTANCE;
			int z = _edwin.getZ();

			actor.setRunning(); // всегда бегают
			actor.moveToLocation(x, y, z, 0, true);
			return true;
		}
		return false;
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}
}