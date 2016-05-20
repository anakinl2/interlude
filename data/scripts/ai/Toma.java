package ai;

import l2d.game.ThreadPoolManager;
import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.util.Location;
import l2d.util.Rnd;

/**
 * Master Toma, телепортируется раз в 30 минут по 3м разным точкам гномьего острова.
 */
public class Toma extends DefaultAI
{
	private Location[]	_points			= new Location[3];
	private static long	TELEPORT_PERIOD	= 30 * 60 * 1000;	// 30 min
	private long		_lastTeleport;

	public Toma(L2Character actor)
	{
		super(actor);
		_points[0] = new Location(151680, -174891, -1807, 41400);
		_points[1] = new Location(154153, -220105, -3402);
		_points[2] = new Location(178834, -184336, -352);
		_lastTeleport = System.currentTimeMillis();
	}

	@Override
	protected boolean thinkActive()
	{
		if(System.currentTimeMillis() - _lastTeleport < TELEPORT_PERIOD)
			return false;

		for(int i = 0; i < _points.length; i++)
		{
			Location loc = _points[Rnd.get(_points.length)];
			if(this.getActor().getLoc().equals(loc))
				continue;

			this.getActor().broadcastPacketToOthers(new MagicSkillUse(this.getActor(), this.getActor(), 4671, 1, 500, 0));
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