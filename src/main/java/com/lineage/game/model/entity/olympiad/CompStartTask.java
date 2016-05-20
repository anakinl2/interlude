package com.lineage.game.model.entity.olympiad;

import com.lineage.game.Announcements;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.serverpackets.SystemMessage;

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