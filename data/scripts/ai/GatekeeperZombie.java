package ai;

import static com.lineage.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;

import com.lineage.ext.scripts.Functions;
import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.ai.Mystic;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.instances.L2NpcInstance;

/**
 * AI охраны входа в Pagan Temple.<br>
 * <li>кидаются на всех игроков, у которых в кармане нету предмета 8064 или 8067
 * <li>не умеют ходить
 *
 * @coautor HellSystem
 * @moding by Felixx
 */
public class GatekeeperZombie extends Mystic
{
	public GatekeeperZombie(L2Character actor)
	{
		super(actor);
		actor.setImobilised(true);
	}

	@Override
	public void checkAggression(L2Character target)
	{
		L2NpcInstance actor = getActor();
		if(actor == null || !(target instanceof L2Playable))
			return;
		if(getIntention() != AI_INTENTION_ACTIVE)
			return;
		if(_globalAggro < 0)
			return;
		if(!actor.isInRange(target, actor.getAggroRange()))
			return;
		if(Math.abs(target.getZ() - actor.getZ()) > 400)
			return;
		if(Functions.getItemCount((L2Playable) target, 8067) != 0 || Functions.getItemCount((L2Playable) target, 8064) != 0)
			return;
		if(!GeoEngine.canSeeTarget(actor, target,false))
			return;
		target.addDamageHate(actor, 0, 1);
		setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			Functions.npcSayToAll(actor, "You dont have permision to Enter! Die!");
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}
}