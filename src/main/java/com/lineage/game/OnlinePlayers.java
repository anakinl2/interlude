package com.lineage.game;

import com.lineage.Config;
import com.lineage.game.model.L2World;

public class OnlinePlayers
{
	private static OnlinePlayers _instance;

	class AnnounceOnline implements Runnable
	{
		@Override
		public void run()
		{
			Announcements.getInstance().announceToAll("Total Online: " + L2World.getAllPlayersCount());
			ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceOnline(), Config.ONLINE_PLAYERS_ANNOUNCE_INTERVAL * 1000);
		}
	}

	public static OnlinePlayers getInstance()
	{
		if(_instance == null)
			_instance = new OnlinePlayers();
		return _instance;
	}

	private OnlinePlayers()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceOnline(), Config.ONLINE_PLAYERS_ANNOUNCE_INTERVAL * 1000);
	}
}