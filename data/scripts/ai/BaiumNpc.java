package ai;

import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.L2WorldRegion;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.Earthquake;
import l2d.game.serverpackets.L2GameServerPacket;
import l2d.util.GArray;

/**
 * AI каменной статуи Байума.<br>
 * Раз в 15 минут устраивает замлятрясение а ТОИ.
 */
public class BaiumNpc extends DefaultAI
{
	private long _wait_timeout = 0;
	private static final int BAIUM_EARTHQUAKE_TIMEOUT = 1000 * 60 * 15; // 15 мин

	public BaiumNpc(L2Character actor)
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
		if(actor == null)
			return true;
		// Пора устроить землятрясение
		if(_wait_timeout < System.currentTimeMillis())
		{
			_wait_timeout = System.currentTimeMillis() + BAIUM_EARTHQUAKE_TIMEOUT;
			L2GameServerPacket eq = new Earthquake(actor.getLoc(), 40, 10);
			for(L2WorldRegion region : L2World.getNeighbors(actor.getX(), actor.getY(), -5000, 10000))
				if(region != null && region.getObjectsSize() > 0)
					for(L2Player player : region.getPlayersList(new GArray<L2Player>(), actor.getObjectId(), actor.getReflection()))
						if(player != null)
							player.sendPacket(eq);
		}
		return false;
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}
}