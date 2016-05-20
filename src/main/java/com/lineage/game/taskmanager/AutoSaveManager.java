package com.lineage.game.taskmanager;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;

import com.lineage.Config;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.model.L2Player;
import com.lineage.util.Rnd;

public class AutoSaveManager
{
	private PlayerContainer[] _tasks = new PlayerContainer[3200];
	private int _currentCell = 0;

	private static AutoSaveManager _instance;

	private AutoSaveManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new SaveScheduler(), 3000, 2000);
	}

	public static AutoSaveManager getInstance()
	{
		if(_instance == null)
			_instance = new AutoSaveManager();

		return _instance;
	}

	public void addPlayerTask(L2Player p)
	{
		int cell = _currentCell + Rnd.get(2, 3);
		if(_tasks.length <= cell)
			cell -= _tasks.length;
		if(_tasks[cell] == null)
			_tasks[cell] = new PlayerContainer();
		_tasks[cell].addPlayer(p);
	}

	private class SaveScheduler implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if(_tasks[_currentCell] != null && !_tasks[_currentCell].getList().isEmpty())
					for(WeakReference<L2Player> pw : _tasks[_currentCell].getList())
						try
						{
							L2Player p = pw.get();
							if(p == null || !p.isConnected() || p.isLogoutStarted() || p.getNetConnection() == null)
								continue;
							if(Config.AUTOSAVE)
								p.store(true);
							addPlayerTask(p);
						}
						catch(Throwable e)
						{
							e.printStackTrace();
						}
			}
			catch(Throwable e)
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