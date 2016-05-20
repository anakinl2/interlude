package ai;

import com.lineage.game.ai.CtrlEvent;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.instances.L2MonsterInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.tables.NpcTable;

public class BladeOfSplendor extends RndTeleportFighter
{
	public BladeOfSplendor(L2Character actor)
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
			if( !(((L2MonsterInstance) actor).getChampion() > 0) && actor.getCurrentHp() < 2000)
			{
				L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(21525));
				spawn.setLoc(actor.getLoc());
				L2NpcInstance npc = spawn.doSpawn(true);
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
				actor.doDie(actor);
				return;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		super.onEvtAttacked(attacker, damage);
	}
}