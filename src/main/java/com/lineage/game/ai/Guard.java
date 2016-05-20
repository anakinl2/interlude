package com.lineage.game.ai;

import static com.lineage.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2NpcInstance;

public class Guard extends Fighter implements Runnable
{
	public Guard(L2Character actor)
	{
		super(actor);
	}

	@Override
	public void checkAggression(L2Character target)
	{
		L2NpcInstance actor = getActor();
		if(actor == null || target.getKarma() <= 0)
			return;
		if(getIntention() != AI_INTENTION_ACTIVE)
			return;
		if(_globalAggro < 0)
			return;
		if(target.getHateList().get(actor) == null && !actor.isInRange(target, 600))
			return;
		if(Math.abs(target.getZ() - actor.getZ()) > 400)
			return;
		if(target instanceof L2Playable && isSilentMoveNotVisible((L2Playable) target))
			return;
		if(!GeoEngine.canSeeTarget(actor, target, false))
			return;
		if(target.isPlayer())
			if(((L2Player) target).getPlayerAccess().IsGM && ((L2Player) target).isInvisible())
				return;

		if((target.isSummon() || target.isPet()) && target.getPlayer() != null)
			target.getPlayer().addDamageHate(actor, 0, 1);

		target.addDamageHate(actor, 0, 2);

		startRunningTask(2000);
		setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}
}