package ai;

import l2d.ext.scripts.Functions;
import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Location;
import l2d.util.Rnd;

public class Tate extends DefaultAI
{
	static final Location[] points = {
		new Location(115824, -181564, -1352),
		new Location(116048, -181575, -1352),
		new Location(116521, -181476, -1400),
		new Location(116632, -180022, -1168),
		new Location(115355, -178617, -928),
		new Location(115763, -177585, -896),
		new Location(115795, -177361, -880),
		new Location(115877, -177338, -880),
		new Location(115783, -177493, -880),
		new Location(115112, -179836, -880),
		new Location(115102, -180026, -872),
		new Location(114876, -180045, -872),
		new Location(114840, -179694, -872),
		new Location(116322, -179602, -1096),
		new Location(116792, -180386, -1240),
		new Location(116319, -181573, -1376),
		new Location(115824, -181564, -1352)
	};

	private int current_point = -1;
	private long wait_timeout = 0;
	private boolean wait = false;

	public Tate(L2Character actor)
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
					case 0:
						wait_timeout = System.currentTimeMillis() + 20000;
						Functions.npcShout(actor, "Care to go a round?");
						wait = true;
						return true;
					case 7:
						wait_timeout = System.currentTimeMillis() + 15000;
						Functions.npcShout(actor, "Have a nice day, Mr. Garita and Mion!");
						wait = true;
						return true;
					case 11:
						wait_timeout = System.currentTimeMillis() + 30000;
						Functions.npcShout(actor, "Mr. Lid, Murdoc, and Airy! How are you doing?");
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