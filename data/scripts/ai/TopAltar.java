package ai;

import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.MagicSkillUse;
/**
 * 
 * @author Midnex
 *
 */
public class TopAltar extends DefaultAI
{	
	long lastUse = 0;
	
	public TopAltar(L2Character actor)
	{
		super(actor);
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead())
			return true;

		if(System.currentTimeMillis() > lastUse + 6000)
		{			
			//5006 -33s - before reg?
			//4515
			//2300
			//3089<--meduda
			actor.broadcastPacket(new MagicSkillUse(actor, actor, 3089, 1, 0, 0));
			lastUse = System.currentTimeMillis();
		}		
		return true;
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}
}