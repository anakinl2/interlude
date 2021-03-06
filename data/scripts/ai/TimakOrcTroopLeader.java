package ai;

import l2d.ext.scripts.Functions;
import l2d.game.ai.CtrlEvent;
import l2d.game.ai.Fighter;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.L2Spawn;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.NpcTable;
import l2d.util.Location;
import l2d.util.Rnd;

/**
 * AI для Timak Orc Troop Leader ID: 20767, кричащего и призывающего братьев по клану при ударе.
 *
 */
public class TimakOrcTroopLeader extends Fighter
{
	private static final int[] BROTHERS = {
		20768, // Timak Orc Troop Shaman
		20769, // Timak Orc Troop Warrior
		20770 // Timak Orc Troop Archer
	};

	private boolean _firstTimeAttacked = true;

	public TimakOrcTroopLeader(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;
		if(_firstTimeAttacked)
		{
			_firstTimeAttacked = false;
			Functions.npcShout(actor, "Show yourselves!");
			for(int bro : BROTHERS)
				try
				{
					Location pos = GeoEngine.findPointToStay(actor.getX(), actor.getY(), actor.getZ(), 100, 120);
					L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(bro));
					spawn.setLoc(pos);
					L2NpcInstance npc = spawn.doSpawn(true);
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, Rnd.get(1, 100));
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead()
	{
		_firstTimeAttacked = true;
		super.onEvtDead();
	}
}