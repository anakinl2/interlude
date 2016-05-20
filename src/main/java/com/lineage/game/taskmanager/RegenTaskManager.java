package com.lineage.game.taskmanager;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.lineage.game.ThreadPoolManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;

public class RegenTaskManager
{
	private PlayerContainer[] _tasks = new PlayerContainer[2];
	private int _currentCell = 0;
	private long _currentTick = 0;

	private static RegenTaskManager _instance;

	private RegenTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new RegenScheduler(), 3000);
		_tasks[0] = new PlayerContainer();
		_tasks[1] = new PlayerContainer();
	}

	public static RegenTaskManager getInstance()
	{
		if(_instance == null)
			_instance = new RegenTaskManager();

		return _instance;
	}

	public PlayerContainer addRegenTask(L2Character character)
	{
		int cell = _currentCell == 0 ? 1 : 0;
		_tasks[cell].addPlayer(character);
		return _tasks[cell];
	}

	public long getTick()
	{
		return _currentTick;
	}

	private class RegenScheduler implements Runnable
	{
		@Override
		public void run()
		{
			long start = System.currentTimeMillis();
			try
			{
				WeakReference<L2Character> we;
				while((we = _tasks[_currentCell].getList().poll()) != null)
					try
					{
						L2Character cha = we.get();
						if(cha == null)
							continue;
						if(cha.isPlayer())
						{
							L2Player player = (L2Player) cha;
							if(player.isDeleting() || !(player.isConnected() || player.isInOfflineMode()))
								continue;
						}
						cha.doRegen();
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
				_tasks[_currentCell].clear();
				synchronized (_instance)
				{
					_currentCell = _currentCell == 0 ? 1 : 0;
					_currentTick++;
				}
				long end = System.currentTimeMillis();
				if(end > start + 2500)
					System.out.println("Too long regen task: " + (end - start) + " ms");
				ThreadPoolManager.getInstance().scheduleGeneral(this, Math.max((3000 - (end - start)), 500));
			}
		}
	}

	private class PlayerContainer
	{
		private ConcurrentLinkedQueue<WeakReference<L2Character>> list = new ConcurrentLinkedQueue<WeakReference<L2Character>>();

		public void addPlayer(L2Character e)
		{
			list.add(new WeakReference<L2Character>(e));
		}

		public ConcurrentLinkedQueue<WeakReference<L2Character>> getList()
		{
			return list;
		}

		public void clear()
		{
			list.clear();
		}
	}
}