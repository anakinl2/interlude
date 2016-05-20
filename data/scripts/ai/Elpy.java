package ai;

import com.lineage.game.ai.Fighter;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.util.Rnd;

public class Elpy extends Fighter
{
	public Elpy(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;
		if(attacker != null && Rnd.chance(50))
		{
			int posX = actor.getX();
			int posY = actor.getY();
			int posZ = actor.getZ();

			int signx = posX < attacker.getX() ? -1 : 1;
			int signy = posY < attacker.getY() ? -1 : 1;

			int range = 200;

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