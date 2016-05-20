package l2d.game.model.entity.olympiad;

import l2d.game.Announcements;
import l2d.game.ThreadPoolManager;
import l2d.game.model.entity.Hero;
import l2d.game.serverpackets.SystemMessage;

public class OlympiadEndTask implements Runnable
{
	@Override
	public void run()
	{
		SystemMessage sm = new SystemMessage(SystemMessage.OLYMPIAD_PERIOD_S1_HAS_ENDED);
		sm.addNumber(Olympiad._currentCycle);

		Announcements.getInstance().announceToAll(sm);
		Announcements.getInstance().announceToAll("Olympiad Validation Period has began");

		Olympiad._isOlympiadEnd = true;
		if(Olympiad._scheduledManagerTask != null)
			Olympiad._scheduledManagerTask.cancel(true);
		if(Olympiad._scheduledWeeklyTask != null)
			Olympiad._scheduledWeeklyTask.cancel(true);

		Olympiad._validationEnd = Olympiad._olympiadEnd + Olympiad.VALIDATION_PERIOD;

		OlympiadDatabase.saveNobleData();
		Olympiad._period = 1;
		Hero.getInstance().clearHeroes();

		try
		{
			OlympiadDatabase.save();
		}
		catch(Exception e)
		{
			Olympiad._log.warning("Olympiad System: Failed to save Olympiad configuration: " + e);
		}

		Olympiad._log.warning("Olympiad System: Starting Validation period. Time to end validation:" + Olympiad.getMillisToValidationEnd() / (60 * 1000));
		if(Olympiad._scheduledValdationTask != null)
			Olympiad._scheduledValdationTask.cancel(true);
		Olympiad._scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValidationTask(), Olympiad.getMillisToValidationEnd());
	}
}