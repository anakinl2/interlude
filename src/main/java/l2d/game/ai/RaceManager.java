package l2d.game.ai;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2RaceManagerInstance;
import l2d.game.serverpackets.MonRaceInfo;

public class RaceManager extends DefaultAI
{
	private Boolean thinking = false; // to prevent recursive thinking
	FastList<L2Player> _knownPlayers = new FastList<L2Player>();

	public RaceManager(L2Character actor)
	{
		super(actor);
		AI_TASK_DELAY = 5000;
	}

	@Override
	public void run()
	{
		onEvtThink();
	}

	@Override
	protected void onEvtThink()
	{
		L2RaceManagerInstance actor = getActor();
		if(actor == null)
			return;

		MonRaceInfo packet = actor.getPacket();
		if(packet == null)
			return;

		synchronized (thinking)
		{
			if(thinking)
				return;
			thinking = true;
		}

		try
		{
			FastList<L2Player> newPlayers = new FastList<L2Player>();
			for(L2Player player : L2World.getAroundPlayers(actor, 1200, 200))
			{
				if(player == null)
					continue;
				newPlayers.add(player);
				if(!_knownPlayers.contains(player))
					player.sendPacket(packet);
				_knownPlayers.remove(player);
			}

			for(L2Player player : _knownPlayers)
				actor.removeKnownPlayer(player);

			_knownPlayers = newPlayers;

		}
		finally
		{
			// Stop thinking action
			thinking = false;
		}
	}

	@Override
	public L2RaceManagerInstance getActor()
	{
		return (L2RaceManagerInstance) super.getActor();
	}
}