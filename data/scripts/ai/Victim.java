package ai;

import l2d.game.ai.Fighter;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;

public class Victim extends Fighter
{
	public Victim(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;
		if(attacker != null)
		{
			int posX = actor.getX();
			int posY = actor.getY();
			int posZ = actor.getZ();

			int signx = posX < attacker.getX() ? -1 : 1;
			int signy = posY < attacker.getY() ? -1 : 1;

			int range = 40;

			posX += Math.round(signx * range);
			posY += Math.round(signy * range);
			posZ = GeoEngine.getHeight(posX, posY, posZ);

			if(GeoEngine.canMoveToCoord(attacker.getX(), attacker.getY(), attacker.getZ(), posX, posY, posZ))
				addTaskMove(posX, posY, posZ);
		}
	}

	@Override
	public void checkAggression(L2Character target)
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}
}