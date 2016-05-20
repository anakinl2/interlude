package ai;

import l2d.game.ThreadPoolManager;
import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.util.Location;
import l2d.util.Rnd;

/**
 * Blacksmith of Wind Rooney, телепортируется раз в 15 минут по 5м разным точкам FoG.
 *
 */
public class Rooney extends DefaultAI
{
	static final Location[] points = {
		new Location(184022, -117083, -3342),
		new Location(183516, -118815, -3093),
		new Location(185007, -115651, -1587),
		new Location(186191, -116465, -1587),
		new Location(189630, -115611, -1587)
	};

	private static final long TELEPORT_PERIOD = 15 * 60 * 1000; // 15 min
	private long _lastTeleport = System.currentTimeMillis();

	public Rooney(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null || System.currentTimeMillis() - _lastTeleport < TELEPORT_PERIOD)
			return false;

		for(int i = 0; i < points.length; i++)
		{
			Location loc = points[Rnd.get(points.length)];
			if(actor.getLoc().equals(loc))
				continue;

			actor.broadcastPacketToOthers(new MagicSkillUse(actor, actor, 4671, 1, 500, 0));
			ThreadPoolManager.getInstance().scheduleAi(new Teleport(loc), 500, false);
			_lastTeleport = System.currentTimeMillis();
			break;
		}
		return true;
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}
}