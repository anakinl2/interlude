package ai;

import l2d.ext.scripts.Functions;
import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Rnd;

public class GuardianAngel extends DefaultAI
{
	static final String[] flood = {
		"Waaaah! Step back from the confounded box! I will take it myself!",
		"Grr! Who are you and why have you stopped my?",
		"Grr. I've been hit..."
	};

	public GuardianAngel(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor != null)
			Functions.npcShout(actor, flood[Rnd.get(2)]);

		return super.thinkActive();
	}

	@Override
	protected void onEvtDead()
	{
		L2NpcInstance actor = getActor();
		if(actor != null)
			Functions.npcShout(actor, flood[2]);
		super.onEvtDead();
	}
}