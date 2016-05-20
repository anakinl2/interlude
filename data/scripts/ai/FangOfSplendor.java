package ai;

import l2d.game.ai.CtrlEvent;
import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.L2Spawn;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.NpcTable;
import com.lineage.util.Rnd;

public class FangOfSplendor extends Fighter
{
	public FangOfSplendor(L2Character actor)
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
			if(!(((L2MonsterInstance) actor).getChampion() > 0) && actor.getCurrentHpPercents() > 50 && Rnd.chance(5))
			{
				L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(21538));
				spawn.setLoc(actor.getLoc());
				L2NpcInstance npc = spawn.doSpawn(true);
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, 100);
				actor.decayMe();
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