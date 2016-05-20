package ai;

import l2d.game.ai.CtrlEvent;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.L2Spawn;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.NpcTable;
import l2d.util.Rnd;

public class WailingOfSplendor extends RndTeleportFighter
{
	private boolean _spawned;

	public WailingOfSplendor(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;
		try
		{
			if(!_spawned && Rnd.chance(25))
			{
				_spawned = true;
				L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(21540));
				spawn.setLoc(GeoEngine.findPointToStay(actor.getX(), actor.getY(), actor.getZ(), 100, 150));
				L2NpcInstance npc = spawn.doSpawn(true);
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead()
	{
		_spawned = false;
		super.onEvtDead();
	}
}