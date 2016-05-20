package l2d.game.model.entity.olympiad;

import l2d.game.Announcements;
import l2d.game.ThreadPoolManager;
import l2d.game.serverpackets.SystemMessage;

class CompEndTask implements Runnable
{
	@Override
	public void run()
	{
		if(Olympiad.isOlympiadEnd())
			return;
		//_scheduledManagerTask.cancel(true);
		Olympiad._inCompPeriod = false;

		try
		{
			if(Olympiad._battleStarted)
			{
				//wait 1 minutes for end of pendings games
				ThreadPoolManager.getInstance().scheduleGeneral(new CompEndTask(), 60000);
				return;
			}
			Announcements.getInstance().announceToAll(new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_HAS_ENDED));
			Olympiad._log.info("Olympiad System: Olympiad Game Ended");
			OlympiadDatabase.save();
		}
		catch(Exception e)
		{
			Olympiad._log.warning("Olympiad System: Failed to save Olympiad configuration:");
			e.printStackTrace();
		}
		Olympiad.init();
	}
}