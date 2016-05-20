package ai;

import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.SkillTable;
import l2d.util.Rnd;

public class AndreasRoyalGuard extends Fighter
{
	public AndreasRoyalGuard(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();

		if((actor.getCurrentHpPercents() < 70) & (Rnd.chance(100)))
		{
			actor.doCast(SkillTable.getInstance().getInfo(4612, 9), attacker, true); // NPC Wide Wild Sweep
			actor.doDie(actor);
		}
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead()
	{
		super.onEvtDead();
	}
}