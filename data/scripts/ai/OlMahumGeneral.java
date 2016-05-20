package ai;

import com.lineage.ext.scripts.Functions;
import l2d.game.ai.Ranger;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import com.lineage.util.Rnd;

/**
 * AI для Karul Bugbear ID: 20438
 */
public class OlMahumGeneral extends Ranger
{
	private boolean _firstTimeAttacked = true;

	public OlMahumGeneral(L2Character actor)
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
				Functions.npcShout(actor, "We shall see about that!");
		}
		else if(Rnd.chance(10))
			Functions.npcShout(actor, "I will definitely repay this humiliation!");
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead()
	{
		_firstTimeAttacked = true;
		super.onEvtDead();
	}
}