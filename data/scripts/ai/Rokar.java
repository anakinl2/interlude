package ai;

import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Location;
import l2d.util.Rnd;

public class Rokar extends DefaultAI
{
	static final Location[] points = {
		new Location(-46516, -117700, -264),
		new Location(-45550, -115420, -256),
		new Location(-44052, -114575, -256),
		new Location(-44024, -112688, -256),
		new Location(-45748, -111665, -256),
		new Location(-46512, -109390, -232),
		new Location(-45748, -111665, -256),
		new Location(-44024, -112688, -256),
		new Location(-44052, -114575, -256),
		new Location(-45550, -115420, -256)
	};

	private int current_point = -1;
	private long wait_timeout = 0;
	private boolean wait = false;

	public Rokar(L2Character actor)
	{
		super(actor);
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead())
			return true;

		if(_def_think)
		{
			doTask();
			return true;
		}

		if(System.currentTimeMillis() > wait_timeout && (current_point > -1 || Rnd.chance(5)))
		{
			if(!wait)
				switch(current_point)
				{
					case 5:
						wait_timeout = System.currentTimeMillis() + 30000;
						wait = true;
						return true;
				}

			wait_timeout = 0;
			wait = false;
			current_point++;

			if(current_point >= points.length)
				current_point = 0;

			addTaskMove(points[current_point]);
			return true;
		}

		if(randomAnimation())
			return true;

		return false;
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}
}