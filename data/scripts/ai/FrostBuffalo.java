package ai;

import l2d.game.ai.CtrlEvent;
import l2d.game.ai.Fighter;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Spawn;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.NpcTable;
import l2d.util.Location;
import l2d.util.Rnd;

/**
 * AI моба Frost Buffalo для Frozen Labyrinth.<br>
 * - Если был атакован физическим скилом, спавнится миньон-мобы Lost Buffalo 22093 в количестве 4 штук.<br>
 * - Не используют функцию Random Walk, если были заспавнены "миньоны"<br>
 */
public class FrostBuffalo extends Fighter
{
	private boolean _mobsNotSpawned = true;
	private static final int MOBS = 22093;
	private static final int MOBS_COUNT = 4;

	public FrostBuffalo(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtSeeSpell(L2Skill skill, L2Character caster)
	{
		L2NpcInstance actor = getActor();
		if(actor == null || skill.isMagic())
			return;
		if(_mobsNotSpawned)
		{
			_mobsNotSpawned = false;
			for(int i = 0; i < MOBS_COUNT; i++)
				try
				{
					Location pos = GeoEngine.findPointToStay(actor.getX(), actor.getY(), actor.getZ(), 100, 120);
					L2Spawn sp = new L2Spawn(NpcTable.getTemplate(MOBS));
					sp.setLoc(pos);
					L2NpcInstance npc = sp.doSpawn(true);
					if(caster.isPet() || caster.isSummon())
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, caster, Rnd.get(2, 100));
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, caster.getPlayer(), Rnd.get(1, 100));
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
		}
	}

	@Override
	protected void onEvtDead()
	{
		_mobsNotSpawned = true;
		super.onEvtDead();
	}

	@Override
	protected boolean randomWalk()
	{
		return _mobsNotSpawned;
	}
}