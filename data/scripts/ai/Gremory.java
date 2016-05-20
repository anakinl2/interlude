package ai;

import l2d.game.ThreadPoolManager;
import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.MagicSkillUse;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

/**
 * @author PaInKiLlEr
 * AI для Gremory в Monastery of Silence .
 * Телепортируется раз в 30 минут по двум точкам (мос 2 и 3 этаж).
 */
public class Gremory extends DefaultAI
{
	static final Location[] points =
		{
			new Location(114629, -70818, -544),
			new Location(110456, -82232, -1615)
		};

	private static final long TELEPORT_PERIOD = 30 * 60 * 1000; // 15 min
	private long _lastTeleport = System.currentTimeMillis();

	public Gremory(L2Character actor)
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