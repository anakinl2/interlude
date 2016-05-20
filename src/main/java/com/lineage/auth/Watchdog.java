package com.lineage.auth;

import java.util.logging.Logger;

import com.lineage.auth.gameservercon.lspackets.TestConnection;
import com.lineage.Config;
import com.lineage.auth.gameservercon.GameServerInfo;

public class Watchdog extends Thread
{
	private static long _lastRequest;
	private static volatile boolean _inited = false;
	private static Logger _log = Logger.getLogger(Watchdog.class.getName());

	public static void init()
	{
		if(!_inited && !Config.COMBO_MODE)
		{
			_inited = true;
			new Watchdog().start();
			_log.fine("Login watchdog thread started");
		}
	}

	public static long getLastTime()
	{
		return _lastRequest;
	}

	@Override
	public void run()
	{
		try
		{
			Thread.sleep(30000); // начинаем не сразу
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}

		while(true)
		{
			_lastRequest = System.currentTimeMillis();

			for(GameServerInfo gs : GameServerTable.getInstance().getRegisteredGameServers().values())
				if(gs.getGameServer() != null && gs.getGameServer().isAuthed())
				{
					if(gs.getGameServer().getLastResponse() == 0) // первый пинг
						gs.getGameServer().notifyResponse();
					if(gs.getGameServer().getLastResponse() < _lastRequest - 10000) // не отвечал 10 секунд
					{
						_log.fine("Watchdog: server " + gs.getId() + " not responding, issuing shutdown...");
						L2LoginServer.getInstance().shutdown(true); // ребутаем логин
						return;
					}
					gs.getGameServer().sendPacket(new TestConnection()); // посылаем пинг
				}

			try
			{
				Thread.sleep(1000); // секунду спим перед следующим пингом
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
}