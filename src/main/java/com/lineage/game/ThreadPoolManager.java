package com.lineage.game;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.ext.network.ReceivablePacket;
import com.lineage.game.network.L2GameClient;

public class ThreadPoolManager
{
	private static final Logger _log = Logger.getLogger(ThreadPoolManager.class.getName());

	private static ThreadPoolManager _instance;

	private ScheduledThreadPoolExecutor _generalScheduledThreadPool;
	private ScheduledThreadPoolExecutor _moveScheduledThreadPool;
	private ScheduledThreadPoolExecutor _effectsScheduledThreadPool;
	private ScheduledThreadPoolExecutor _npcAiScheduledThreadPool;
	private ScheduledThreadPoolExecutor _playerAiScheduledThreadPool;

	private ThreadPoolExecutor _generalPacketsThreadPool;
	private ThreadPoolExecutor _ioPacketsThreadPool;
	private ThreadPoolExecutor _LsGsExecutor;

	private boolean _shutdown;

	public static ThreadPoolManager getInstance()
	{
		if(_instance == null)
			_instance = new ThreadPoolManager();
		return _instance;
	}

	private ThreadPoolManager()
	{
		_generalScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_GENERAL, new PriorityThreadFactory("GerenalSTPool", Thread.NORM_PRIORITY));
		_moveScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_MOVE, new PriorityThreadFactory("MoveSTPool", Thread.MAX_PRIORITY));
		_effectsScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_EFFECTS, new PriorityThreadFactory("EffectsSTPool", Thread.MIN_PRIORITY));
		_npcAiScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.NPC_AI_MAX_THREAD, new PriorityThreadFactory("NpcAISTPool", Thread.MIN_PRIORITY + 2));
		_playerAiScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.PLAYER_AI_MAX_THREAD, new PriorityThreadFactory("PlayerAISTPool", Thread.NORM_PRIORITY + 1));

		//_ioPacketsThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new PriorityThreadFactory("High Packet Pool", Thread.NORM_PRIORITY + 3));
		//_generalPacketsThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 15L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new PriorityThreadFactory("Normal Packet Pool", Thread.NORM_PRIORITY + 2));

		_ioPacketsThreadPool = new ThreadPoolExecutor(Config.URGENT_PACKET_THREAD_CORE_SIZE, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("High Packet Pool", Thread.NORM_PRIORITY + 3));
		_generalPacketsThreadPool = new ThreadPoolExecutor(Config.GENERAL_PACKET_THREAD_CORE_SIZE, Config.GENERAL_PACKET_THREAD_CORE_SIZE + 2, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("Normal Packet Pool", Thread.NORM_PRIORITY + 2));

		_LsGsExecutor = new ThreadPoolExecutor(1, 6, 5L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("LS/GS Communications", Thread.MAX_PRIORITY - 2));
	}

	public ScheduledFuture scheduleEffect(Runnable r, long delay)
	{
		try
		{
			if(delay < 0)
				delay = 0;
			return _effectsScheduledThreadPool.schedule(r, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warning("EffectThreadPool: Failed schedule task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public ScheduledFuture scheduleEffectAtFixedRate(Runnable r, long initial, long delay)
	{
		try
		{
			if(delay < 0)
				delay = 0;
			if(initial < 0)
				initial = 0;
			return _effectsScheduledThreadPool.scheduleAtFixedRate(r, initial, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warning("EffectThreadPool: Failed schedule task at fixed rate!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public ScheduledFuture scheduleGeneral(Runnable r, long delay)
	{
		try
		{
			if(delay < 0)
				delay = 0;
			return _generalScheduledThreadPool.schedule(r, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warning("GeneralThreadPool: Failed schedule task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public ScheduledFuture scheduleMove(Runnable r, long delay)
	{
		try
		{
			return _moveScheduledThreadPool.schedule(r, delay > 0 ? delay : 1, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warning("MoveThreadPool: Failed schedule task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public ScheduledFuture scheduleGeneralAtFixedRate(Runnable r, long initial, long delay)
	{
		try
		{
			if(delay <= 0)
				delay = 1;
			return _generalScheduledThreadPool.scheduleAtFixedRate(r, initial, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warning("GeneralThreadPool: Failed schedule task at fixed rate!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public ScheduledFuture scheduleAi(Runnable r, long delay, boolean isPlayer)
	{
		try
		{
			if(delay < 0)
				delay = 0;
			if(isPlayer)
				return _playerAiScheduledThreadPool.schedule(r, delay, TimeUnit.MILLISECONDS);
			return _npcAiScheduledThreadPool.schedule(r, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warning("AiThreadPool: Failed schedule task!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public ScheduledFuture scheduleAiAtFixedRate(Runnable r, long initial, long delay)
	{
		try
		{
			if(delay < 0)
				delay = 0;
			if(initial < 0)
				initial = 0;
			return _npcAiScheduledThreadPool.scheduleAtFixedRate(r, initial, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			if(!isShutdown())
			{
				_log.warning("AiThreadPool: Failed schedule task at fixed rate!");
				Thread.dumpStack();
			}
			return null; /* shutdown, ignore */
		}
	}

	public void executePacket(ReceivablePacket<L2GameClient> pkt)
	{
		_generalPacketsThreadPool.execute(pkt);
	}

	public void executeIOPacket(ReceivablePacket<L2GameClient> pkt)
	{
		_ioPacketsThreadPool.execute(pkt);
	}

	public void executeLSGSPacket(Runnable r)
	{
		_LsGsExecutor.execute(r);
	}

	public void executeGeneral(Runnable r)
	{
		_generalScheduledThreadPool.execute(r);
	}

	public void executeEffect(Runnable r)
	{
		_effectsScheduledThreadPool.execute(r);
	}

	public String[] getStats()
	{
		return new String[] {
				"Scheduled Thread Pools:",
				" + General:",
				" |- ActiveThreads:   " + _generalScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _generalScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _generalScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _generalScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _generalScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_generalScheduledThreadPool.getTaskCount() - _generalScheduledThreadPool.getCompletedTaskCount()),
				" | -------",
				" + Move:",
				" |- ActiveThreads:   " + _moveScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _moveScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _moveScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _moveScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _moveScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_moveScheduledThreadPool.getTaskCount() - _moveScheduledThreadPool.getCompletedTaskCount()),
				" | -------",
				" + Effects:",
				" |- ActiveThreads:   " + _effectsScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _effectsScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _effectsScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _effectsScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _effectsScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_effectsScheduledThreadPool.getTaskCount() - _effectsScheduledThreadPool.getCompletedTaskCount()),
				" | -------",
				" + Npc AI:",
				" |- ActiveThreads:   " + _npcAiScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _npcAiScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _npcAiScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _npcAiScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _npcAiScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_npcAiScheduledThreadPool.getTaskCount() - _npcAiScheduledThreadPool.getCompletedTaskCount()),
				" | -------",
				" + Player AI:",
				" |- ActiveThreads:   " + _playerAiScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _playerAiScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _playerAiScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _playerAiScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _playerAiScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_playerAiScheduledThreadPool.getTaskCount() - _playerAiScheduledThreadPool.getCompletedTaskCount()),
				" | -------",
				"Thread Pools:",
				" + Packets:",
				" |- ActiveThreads:   " + _generalPacketsThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _generalPacketsThreadPool.getCorePoolSize(),
				" |- MaximumPoolSize: " + _generalPacketsThreadPool.getMaximumPoolSize(),
				" |- LargestPoolSize: " + _generalPacketsThreadPool.getLargestPoolSize(),
				" |- PoolSize:        " + _generalPacketsThreadPool.getPoolSize(),
				" |- CompletedTasks:  " + _generalPacketsThreadPool.getCompletedTaskCount(),
				" |- QueuedTasks:     " + _generalPacketsThreadPool.getQueue().size(),
				" | -------",
				" + Urgent Packets:",
				" |- ActiveThreads:   " + _ioPacketsThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _ioPacketsThreadPool.getCorePoolSize(),
				" |- MaximumPoolSize: " + _ioPacketsThreadPool.getMaximumPoolSize(),
				" |- LargestPoolSize: " + _ioPacketsThreadPool.getLargestPoolSize(),
				" |- PoolSize:        " + _ioPacketsThreadPool.getPoolSize(),
				" |- CompletedTasks:  " + _ioPacketsThreadPool.getCompletedTaskCount(),
				" |- QueuedTasks:     " + _ioPacketsThreadPool.getQueue().size(),
				" | -------",
				" + LS/GS Packets:",
				" |- ActiveThreads:   " + _LsGsExecutor.getActiveCount(),
				" |- getCorePoolSize: " + _LsGsExecutor.getCorePoolSize(),
				" |- MaximumPoolSize: " + _LsGsExecutor.getMaximumPoolSize(),
				" |- LargestPoolSize: " + _LsGsExecutor.getLargestPoolSize(),
				" |- PoolSize:        " + _LsGsExecutor.getPoolSize(),
				" |- CompletedTasks:  " + _LsGsExecutor.getCompletedTaskCount(),
				" |- QueuedTasks:     " + _LsGsExecutor.getQueue().size(),
				" | -------", };
	}

	private class PriorityThreadFactory implements ThreadFactory
	{
		private int _prio;
		private String _name;
		private AtomicInteger _threadNumber = new AtomicInteger(1);
		private ThreadGroup _group;

		public PriorityThreadFactory(String name, int prio)
		{
			_prio = prio;
			_name = name;
			_group = new ThreadGroup(_name);
		}

		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(_group, r);
			t.setName(_name + "-" + _threadNumber.getAndIncrement());
			t.setPriority(_prio);
			return t;
		}

		public ThreadGroup getGroup()
		{
			return _group;
		}
	}

	public void shutdown()
	{
		_shutdown = true;
		try
		{
			// в обратном порядке шатдауним потоки
			_LsGsExecutor.awaitTermination(1, TimeUnit.SECONDS);
			_npcAiScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_playerAiScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_ioPacketsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalPacketsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_effectsScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_moveScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);

			_LsGsExecutor.shutdown();
			_npcAiScheduledThreadPool.shutdown();
			_playerAiScheduledThreadPool.shutdown();
			_ioPacketsThreadPool.shutdown();
			_generalPacketsThreadPool.shutdown();
			_effectsScheduledThreadPool.shutdown();
			_moveScheduledThreadPool.shutdown();
			_generalScheduledThreadPool.shutdown();
			System.out.println("All ThreadPools are now stoped.");
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	public String getGPacketStats()
	{
		return getThreadPoolStats(_generalPacketsThreadPool, "general packets");
	}

	public String getIOPacketStats()
	{
		return getThreadPoolStats(_generalPacketsThreadPool, "IO packets");
	}

	public String getGeneralPoolStats()
	{
		return getThreadPoolStats(_generalScheduledThreadPool, "general");
	}

	public String getMovePoolStats()
	{
		return getThreadPoolStats(_moveScheduledThreadPool, "move");
	}

	public String getNpcAIPoolStats()
	{
		return getThreadPoolStats(_npcAiScheduledThreadPool, "npcAi");
	}

	public String getPlayerAIPoolStats()
	{
		return getThreadPoolStats(_playerAiScheduledThreadPool, "playerAi");
	}

	private String getThreadPoolStats(ThreadPoolExecutor pool, String poolname)
	{
		ThreadFactory tf = pool.getThreadFactory();
		if(!(tf instanceof PriorityThreadFactory))
			return "This should not be seen, pool " + poolname;

		StringBuilder res = new StringBuilder();
		PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
		int count = ptf.getGroup().activeCount();
		Thread[] threads = new Thread[count + 2];
		ptf.getGroup().enumerate(threads);

		res.append("\r\nThread Pool: ").append(poolname);
		res.append("\r\nTasks in the queue: ").append(pool.getQueue().size());
		res.append("\r\nThreads stack trace:");
		res.append("\r\nThere should be ").append(count).append(" threads\r\n");

		for(Thread t : threads)
		{
			if(t == null)
				continue;
			res.append("\r\n").append(t.getName());
			StackTraceElement[] trace = t.getStackTrace();
			if(trace.length == 0 || trace[0] == null || trace[0].toString().contains("sun.misc.Unsafe.park"))
				continue; // Пропускаем пустые
			for(StackTraceElement ste : t.getStackTrace())
				res.append("\r\n").append(ste);
		}

		return res.toString();
	}

	public boolean isShutdown()
	{
		return _shutdown;
	}

	public ScheduledThreadPoolExecutor getEffectsScheduledThreadPool()
	{
		return _effectsScheduledThreadPool;
	}

	public ScheduledThreadPoolExecutor getGeneralScheduledThreadPool()
	{
		return _generalScheduledThreadPool;
	}

	public ScheduledThreadPoolExecutor getMoveScheduledThreadPool()
	{
		return _moveScheduledThreadPool;
	}

	public ScheduledThreadPoolExecutor getNpcAiScheduledThreadPool()
	{
		return _npcAiScheduledThreadPool;
	}

	public ScheduledThreadPoolExecutor getPlayerAiScheduledThreadPool()
	{
		return _playerAiScheduledThreadPool;
	}
}