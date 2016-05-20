package com.lineage.game.taskmanager;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Stack;
import java.util.concurrent.ScheduledFuture;

import com.lineage.game.ThreadPoolManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Player;
import com.lineage.util.GArray;

public class EffectTaskManager
{
	private EffectContainer[] _dispelTasks = new EffectContainer[172800];
	private Stack<EffectContainer> _pool = new Stack<EffectContainer>();
	private Object lock = new Object();
	private int _currentDispelCell = 0;
	@SuppressWarnings({ "unused", "unchecked" })
	private ScheduledFuture _task;

	private static EffectTaskManager _instance;

	private EffectTaskManager()
	{
		_task = ThreadPoolManager.getInstance().scheduleGeneral(new DispelScheduler(), 1000);
	}

	public static EffectTaskManager getInstance()
	{
		if(_instance == null)
			_instance = new EffectTaskManager();

		return _instance;
	}

	/**
	 * интервал в секундах!
	 */
	public EffectContainer addDispelTask(final L2Effect e, int interval)
	{
		synchronized (lock)
		{
			if(interval < 1)
				interval = 1;
			if(interval > _dispelTasks.length / 2)
			{
				System.out.println("[ERROR]: Effect " + e.getSkill().getName() + " Has waning Interval: " + interval);
				interval = _dispelTasks.length - 1;
			}
			int cell = _currentDispelCell + interval;
			if(_dispelTasks.length <= cell)
				cell -= _dispelTasks.length;
			if(_dispelTasks[cell] == null)
				if(!_pool.isEmpty())
					_dispelTasks[cell] = _pool.pop();
				else
					_dispelTasks[cell] = new EffectContainer();
			_dispelTasks[cell].addEffect(e);
			return _dispelTasks[cell];
		}
	}

	private class DispelScheduler implements Runnable
	{
		@Override
		public void run()
		{
			long start;
			final GArray<L2Effect> works = new GArray<L2Effect>();

			synchronized (lock)
			{
				start = System.currentTimeMillis();
				try
				{
					if(_dispelTasks[_currentDispelCell] != null && !_dispelTasks[_currentDispelCell].getList().isEmpty())
						for(final WeakReference<L2Effect> we : _dispelTasks[_currentDispelCell].getList())
							try
							{
								final L2Effect eff = we.get();
								if(eff == null || eff.isFinished())
									continue;

								works.add(eff);

								if(!eff.isEnded())
									addDispelTask(eff, (int) (eff.getPeriod() / 1000));
							}
							catch(final Exception e)
							{
								e.printStackTrace();
							}
				}
				catch(final Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					if(_dispelTasks[_currentDispelCell] != null)
					{
						_dispelTasks[_currentDispelCell].clear();
						_pool.push(_dispelTasks[_currentDispelCell]);
						_dispelTasks[_currentDispelCell] = null;
					}
					_currentDispelCell++;
					if(_currentDispelCell >= _dispelTasks.length)
						_currentDispelCell = 0;
				}
			}

			for(final L2Effect work : works)
			{
				final L2Character effected = work.getEffected();
				if(effected == null || effected.isPlayer() && ((L2Player) effected).isDeleting())
					continue;
				work.scheduleEffect();
			}

			_task = ThreadPoolManager.getInstance().scheduleGeneral(this, 1000 + start - System.currentTimeMillis());
		}
	}

	private class EffectContainer
	{
		private LinkedList<WeakReference<L2Effect>> list = new LinkedList<WeakReference<L2Effect>>();

		public void addEffect(final L2Effect e)
		{
			list.add(new WeakReference<L2Effect>(e));
		}

		public LinkedList<WeakReference<L2Effect>> getList()
		{
			return list;
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