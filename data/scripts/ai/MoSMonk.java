package ai;

import static l2d.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import com.lineage.ext.scripts.Functions;
import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import com.lineage.util.Rnd;

/**
 * AI монахов в Monastery of Silence<br>
 * - агрятся на чаров с оружием в руках
 * - перед тем как броситься в атаку кричат
 */
public class MoSMonk extends Fighter
{
	public MoSMonk(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;
		if(getIntention() == AI_INTENTION_ACTIVE && Rnd.chance(50))
			Functions.npcShout(actor, "You cannot carry a weapon without authorization!");
		super.onIntentionAttack(target);
	}

	@Override
	public void checkAggression(L2Character target)
	{
		if(target.getActiveWeaponInstance() == null)
			return;
		super.checkAggression(target);
	}
}