package ai;

import static l2d.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import l2d.game.ai.CtrlIntention;
import l2d.game.ai.Mystic;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.L2Playable;

/**
 * AI Triols Revelation в Pagan Temple.
 */
public class TriolsRevelation extends Mystic
{
	public TriolsRevelation(L2Character actor)
	{
		super(actor);
		_actor.setImobilised(true);
	}

	@Override
	public void checkAggression(L2Character target)
	{
		if( !(target instanceof L2Playable))
			return;

		if(_intention != AI_INTENTION_ACTIVE)
			return;
		if(_globalAggro < 0)
			return;
		if( !this.getActor().isInRange(target, this.getActor().getAggroRange()))
			return;
		if(Math.abs(target.getZ() - _actor.getZ()) > 400)
			return;
		if( !GeoEngine.canSeeTarget(_actor, target,false))
			return;
		target.addDamageHate(this.getActor(), 0, 1);
		setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
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
}