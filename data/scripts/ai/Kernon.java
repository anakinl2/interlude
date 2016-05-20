package ai;

import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;

/**
 * @author PaInKiLlEr
 *         АИ для РБ Kernon.
 *         Если зет координаты меньше или больше предназначеных, телепортируется обратно и ресает хп.
 *         Выполнено специально для L2Dream.su
 */
public class Kernon extends Fighter
{
	private static final int z1 = 3900;
	private static final int z2 = 4300;

	public Kernon(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		int z = actor.getZ();
		if(z > z2 || z < z1)
		{
			actor.teleToLocation(113420, 16424, 3969);
			actor.setCurrentHp(actor.getMaxHp(), false);
		}
		super.onEvtAttacked(attacker, damage);
	}
}