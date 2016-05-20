package ai;

import java.util.HashMap;

import l2d.game.ai.CtrlEvent;
import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.L2Spawn;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.instances.L2NpcInstance.AggroInfo;
import l2d.game.tables.NpcTable;

/**
 * AI полиморфных ангелов. После смерти заспавненного респится его двойник, за которого дают и экспу и дроп.
 */
public class PolimorphingAngel extends Fighter
{
	// ID для полиморфизма
	private static final HashMap<Integer, Integer> polymorphing = new HashMap<Integer, Integer>();
	static
	{
		polymorphing.put(20830, 20859);
		polymorphing.put(21067, 21068);
		polymorphing.put(21062, 21063);
		polymorphing.put(20831, 20860);
		polymorphing.put(21070, 21071);
	}

	public PolimorphingAngel(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtDead()
	{
		try
		{
			L2NpcInstance actor = getActor();
			if(actor != null)
			{
				L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(polymorphing.get(actor.getNpcId())));
				spawn.setLoc(actor.getLoc());
				L2NpcInstance npc = spawn.doSpawn(true);

				// FIXME в этот момент агролист уже пуст
				// Заагрим свежезаспавненного ангела на всех, кто был в агролисте у убитого
				for(AggroInfo aggroInfo : actor.getAggroList().values())
					if(aggroInfo.attacker != null && aggroInfo.attacker.isPlayer())
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, aggroInfo.attacker, aggroInfo.hate);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		super.onEvtDead();
	}
}