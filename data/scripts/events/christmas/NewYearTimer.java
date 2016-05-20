package events.christmas;

import java.util.Calendar;

import l2d.ext.scripts.ScriptFile;
import l2d.game.Announcements;
import l2d.game.ThreadPoolManager;
import l2d.game.instancemanager.ServerVariables;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2World;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.tables.SkillTable;

public class NewYearTimer implements ScriptFile
{
	private static NewYearTimer instance;

	public static NewYearTimer getInstance()
	{
		if(instance == null)
			new NewYearTimer();
		return instance;
	}

	public NewYearTimer()
	{
		if(instance != null)
			return;

		instance = this;

		if(!isActive())
			return;

		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, 2008);
		c.set(Calendar.MONTH, Calendar.JANUARY);
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		while(getDelay(c) < 0)
			c.set(Calendar.YEAR, c.get(Calendar.YEAR) + 1);

		ThreadPoolManager.getInstance().scheduleGeneral(new NewYearAnnouncer("С Новым, " + c.get(Calendar.YEAR) + ", Годом!!!"), getDelay(c));
		c.add(Calendar.SECOND, -1);
		ThreadPoolManager.getInstance().scheduleGeneral(new NewYearAnnouncer("1"), getDelay(c));
		c.add(Calendar.SECOND, -1);
		ThreadPoolManager.getInstance().scheduleGeneral(new NewYearAnnouncer("2"), getDelay(c));
		c.add(Calendar.SECOND, -1);
		ThreadPoolManager.getInstance().scheduleGeneral(new NewYearAnnouncer("3"), getDelay(c));
		c.add(Calendar.SECOND, -1);
		ThreadPoolManager.getInstance().scheduleGeneral(new NewYearAnnouncer("4"), getDelay(c));
		c.add(Calendar.SECOND, -1);
		ThreadPoolManager.getInstance().scheduleGeneral(new NewYearAnnouncer("5"), getDelay(c));
	}

	private long getDelay(Calendar c)
	{
		return c.getTime().getTime() - System.currentTimeMillis();
	}

	/**
	 * Вызывается при загрузке классов скриптов
	 */
	public void onLoad()
	{}

	/**
	 * Вызывается при перезагрузке
	 * После перезагрузки onLoad() вызывается автоматически
	 */
	public void onReload()
	{}

	/**
	 * Читает статус эвента из базы.
	 * @return
	 */
	private static boolean isActive()
	{
		return ServerVariables.getString("Christmas", "off").equalsIgnoreCase("on");
	}

	/**
	 * Вызывается при выключении сервера
	 */
	public void onShutdown()
	{}

	private class NewYearAnnouncer implements Runnable
	{
		private final String message;

		private NewYearAnnouncer(String message)
		{
			this.message = message;
		}

		public void run()
		{
			Announcements.getInstance().announceToAll(message);

			// Через жопу сделано, но не суть важно :)
			if(message.length() == 1)
				return;

			for(L2Player player : L2World.getAllPlayers())
			{
				L2Skill skill = SkillTable.getInstance().getInfo(3266, 1);
				MagicSkillUse msu = new MagicSkillUse(player, player, 3266, 1, skill.getHitTime(), 0);
				player.broadcastPacket(msu);
			}

			instance = null;
			new NewYearTimer();
		}
	}
}