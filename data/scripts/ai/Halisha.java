package ai;

import com.lineage.game.ai.CtrlEvent;
import com.lineage.game.ai.Fighter;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.tables.NpcTable;

public class Halisha extends Fighter
{
	public Halisha(L2Character actor)
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
			if(actor.getCurrentHp() < 50000)
			{
				L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(29047));
				spawn.setLoc(actor.getLoc());
				L2NpcInstance npc = spawn.doSpawn(true);
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
				actor.decayMe();
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