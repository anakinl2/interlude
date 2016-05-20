package l2d.game.model.entity.olympiad;

import l2d.game.Announcements;
import l2d.game.ThreadPoolManager;
import l2d.game.serverpackets.SystemMessage;

class CompStartTask implements Runnable
{
	@Override
	public void run()
	{
		if(Olympiad.isOlympiadEnd())
			return;

		Olympiad._inCompPeriod = true;
		OlympiadManager om = new OlympiadManager();

		Announcements.getInstance().announceToAll(new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_HAS_STARTED));
		Olympiad._log.info("Olympiad System: Olympiad Game Started");

		Thread olyCycle = new Thread(om);
		olyCycle.start();

		ThreadPoolManager.getInstance().scheduleGeneral(new CompEndTask(), Olympiad.getMillisToCompEnd());
	}
}