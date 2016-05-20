package ai;

import com.lineage.game.ai.DefaultAI;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.util.GArray;
/**
 * 
 * @author Midnex
 *
 */
public class Monastry_Epic_Entrance extends DefaultAI
{	
	long lastUse = 0;
	
	public Monastry_Epic_Entrance(L2Character actor)
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

		GArray<L2Character> list = getActor().getAroundCharacters(500, 100);

		for(L2Character npc : list)
		{
			if(npc.isDead())
				continue;
			if(npc.getNpcId() > 10 && npc.getNpcId() < 19)
				continue;
			if(npc.isPlayable() && npc.getDistance(actor)>290)
				continue;
			npc.reduceCurrentHp(npc.isPlayable() ? 400 : 5000, actor, null, true, false, true, false);
			npc.broadcastPacket(new MagicSkillUse(actor, npc, 4319, 1, 0, 0));
		}

		if(System.currentTimeMillis() > lastUse + 2000)
			actor.broadcastPacket(new MagicSkillUse(actor, actor, 1049, 1, 0, 0));

		if(System.currentTimeMillis() < lastUse + 3000)
		{
			for(L2Character npc : list)
			{
				if(npc.getNpcId() > 10 && npc.getNpcId() < 19)
					npc.broadcastPacket(new MagicSkillUse(npc, npc, 7051, 1, 0, 0));
			}
			return true;
		}

		for(L2Character npc : list)
		{
			if(npc.getNpcId() > 10 && npc.getNpcId() < 19)
				npc.broadcastPacket(new MagicSkillUse(npc, actor, 289, 1, 0, 0));
		}
		lastUse = System.currentTimeMillis();
		return true;
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}
}