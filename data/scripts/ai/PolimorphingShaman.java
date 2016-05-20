package ai;

import com.lineage.game.ai.CtrlEvent;
import com.lineage.game.ai.Fighter;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.tables.NpcTable;

/**
 * AI полиморфного шамана ID: 21258, превращающегося в тигра ID: 21259 при ударе
 */
public class PolimorphingShaman extends Fighter
{
	private static final int TIGER_ID = 21259;

	public PolimorphingShaman(L2Character actor)
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
			L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(TIGER_ID));
			spawn.setLoc(actor.getLoc());
			L2NpcInstance npc = spawn.doSpawn(true);
			npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		actor.decayMe();
		actor.doDie(actor);
	}
}