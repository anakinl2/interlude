package ai;

import com.lineage.ext.scripts.Functions;
import l2d.game.ai.Ranger;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import com.lineage.util.Rnd;

/**
 * AI для Karul Bugbear ID: 20600
 */
public class KarulBugbear extends Ranger
{
	private boolean _firstTimeAttacked = true;

	public KarulBugbear(L2Character actor)
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
			if(Rnd.chance(25))
				Functions.npcShout(actor, "Your rear is practically unguarded!");
		}
		else if(Rnd.chance(10))
			Functions.npcShout(actor, "Watch your back!");
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead()
	{
		_firstTimeAttacked = true;
		super.onEvtDead();
	}
}