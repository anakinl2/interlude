package l2d.game;

import java.util.logging.Logger;

import com.lineage.Config;

public class GCTask implements Runnable
{
	private static final Logger _log = Logger.getLogger(GCTask.class.getName());
	private Thread t;

	public GCTask()
	{
		t = new Thread(this);
		t.start();
	}

	@Override
	public void run()
	{
		while(true)
			try
			{
				Thread.sleep(Config.GCTaskDelay);
				_log.info("used mem before GC:" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");
				System.gc();
				_log.info("used mem after GC:" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

	}
}
