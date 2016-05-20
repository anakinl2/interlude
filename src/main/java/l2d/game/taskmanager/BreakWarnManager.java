package l2d.game.taskmanager;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;

import l2d.game.ThreadPoolManager;
import l2d.game.cache.Msg;
import l2d.game.model.L2Player;

public class BreakWarnManager
{
	private PlayerContainer[] _tasks = new PlayerContainer[360];
	private int _currentCell = 0;

	private static BreakWarnManager _instance;

	private BreakWarnManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new DispelScheduler(), 60000, 60000);
	}

	public static BreakWarnManager getInstance()
	{
		if(_instance == null)
			_instance = new BreakWarnManager();

		return _instance;
	}

	public PlayerContainer addWarnTask(L2Player p)
	{
		int cell = _currentCell + 120;
		if(_tasks.length <= cell)
			cell -= _tasks.length;
		if(_tasks[cell] == null)
			_tasks[cell] = new PlayerContainer();
		_tasks[cell].addPlayer(p);
		return _tasks[cell];
	}

	private class DispelScheduler implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if(_tasks[_currentCell] != null && !_tasks[_currentCell].getList().isEmpty())
					for(WeakReference<L2Player> we : _tasks[_currentCell].getList())
						try
						{
							L2Player player = we.get();
							if(player == null || !player.isConnected() || player.isDeleting())
								continue;
							player.sendPacket(Msg.YOU_HAVE_BEEN_PLAYING_FOR_AN_EXTENDED_PERIOD_OF_TIME_PLEASE_CONSIDER_TAKING_A_BREAK);
							addWarnTask(player);
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if(_tasks[_currentCell] != null)
					_tasks[_currentCell].clear();

				_currentCell++;
				if(_currentCell >= _tasks.length)
					_currentCell = 0;
			}
		}
	}

	private class PlayerContainer
	{
		private HashMap<Integer, WeakReference<L2Player>> list = new HashMap<Integer, WeakReference<L2Player>>();

		public void addPlayer(L2Player e)
		{
			if(!list.containsKey(e.getObjectId()))
				list.put(e.getObjectId(), new WeakReference<L2Player>(e));
		}

		public Collection<WeakReference<L2Player>> getList()
		{
			return list.values();
		}

		public void clear()
		{
			synchronized (list)
			{
				list.clear();
			}
		}
	}
}