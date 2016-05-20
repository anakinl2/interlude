package l2d.game.tables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.logging.Logger;

import l2d.Config;
import l2d.game.ThreadPoolManager;
import l2d.game.model.L2World;
import l2d.util.GArray;
import l2d.util.Rnd;

public class FakePlayersTable
{
	public static class Task implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if(_activeFakePlayers.size() < L2World.getAllPlayersCount() * Config.FAKE_PLAYERS_PERCENT / 100 && _activeFakePlayers.size() < _fakePlayers.length)
				{
					if(Rnd.chance(10))
					{
						String player = _fakePlayers[Rnd.get(_fakePlayers.length)];
						if(player != null && !_activeFakePlayers.contains(player))
							_activeFakePlayers.add(player);
					}
				}
				else if(_activeFakePlayers.size() > 0)
					_activeFakePlayers.remove(Rnd.get(_activeFakePlayers.size()));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private static final Logger _log = Logger.getLogger(FakePlayersTable.class.getName());

	private static String[] _fakePlayers;
	private static GArray<String> _activeFakePlayers = new GArray<String>();

	private static FakePlayersTable _instance;

	public static FakePlayersTable getInstance()
	{
		if(_instance == null)
			new FakePlayersTable();
		return _instance;
	}

	public FakePlayersTable()
	{
		_instance = this;
		if(Config.ALLOW_FAKE_PLAYERS)
		{
			parseData();
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Task(), 180000, 1000);
		}
	}

	private static void parseData()
	{
		LineNumberReader lnr = null;
		try
		{
			File doorData = new File(FakePlayersTable.class.getClassLoader().getResource(Config.FAKE_PLAYERS_LIST).getFile());
			lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));
			String line;
			ArrayList<String> players_list = new ArrayList<String>();
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				players_list.add(line);
			}
			_fakePlayers = players_list.toArray(new String[players_list.size()]);
			_log.config("FakePlayersTable: Loaded " + _fakePlayers.length + " Fake Players.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e1)
			{}
		}
	}

	public static int getFakePlayersCount()
	{
		return _activeFakePlayers.size();
	}

	public static GArray<String> getActiveFakePlayers()
	{
		return _activeFakePlayers;
	}
}