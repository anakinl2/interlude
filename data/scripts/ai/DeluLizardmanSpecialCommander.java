package ai;

import com.lineage.ext.scripts.Functions;
import com.lineage.game.ai.Ranger;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.util.Rnd;

public class DeluLizardmanSpecialCommander extends Ranger 
{
	private boolean _firstTimeAttacked = true;

	public DeluLizardmanSpecialCommander(L2Character actor) 
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
			if(Rnd.get(60) < 40) 
				Functions.npcShout(actor, "Come on, Ill take you on!");
		} 
		else if(Rnd.get(30) < 10) 
			Functions.npcShout(actor, "How dare you interrupt a sacred duel! You must be taught a lesson!");
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead()
	{
		_firstTimeAttacked = true;
		super.onEvtDead();
	}
}