package ai;

import l2d.ext.scripts.Functions;
import l2d.game.ai.CtrlEvent;
import l2d.game.ai.DefaultAI;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.NpcTable;
import l2d.game.tables.SkillTable;
import l2d.util.Location;
import l2d.util.Rnd;

/**
 * AI для яиц на острове динозавров.
 * - Если был атакован, кастует скилами на игрока.
 * - Завёт на помощь динозавров.
 * - Не использует анимации, не использует рандом валк
 * - Не атакует игроков
 */
public class AncientEgg extends DefaultAI
{
	private L2Player self;
	L2Player player = (L2Player) self;
	private boolean _firstTimeAttacked = true;
	private static final int BROTHERS[] = {22196, 22199, 22200, 22203};

	public AncientEgg(L2Character actor)
	{
		super(actor);
	}

	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(_firstTimeAttacked)
		{
			_firstTimeAttacked = false;
			Functions.npcShout(actor, ":(");
			for(int bro : BROTHERS)
				try
				{
					Location pos = GeoEngine.findPointToStay(actor.getX(), actor.getY(), actor.getZ(), 100, 120);
					L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(bro));
					spawn.setLoc(pos);
					L2NpcInstance npc = spawn.doSpawn(true);
					npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, Rnd.get(1, 100));
					actor.doCast(SkillTable.getInstance().getInfo(5088, 1), attacker, true); // NPC Wide Wild Sweep
					actor.setTarget(player);

				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}

	@Override
	protected boolean randomAnimation()
	{
		return false;
	}

	@Override
	protected void onEvtDead()
	{
		_firstTimeAttacked = true;
		super.onEvtDead();
	}
}