package com.lineage.game.model.entity.olympiad;

import java.util.Calendar;

public class WeeklyTask implements Runnable
{
	@Override
	public void run()
	{
		Olympiad.addWeeklyPoints();
		Olympiad._log.info("Olympiad System: Added weekly points to nobles");

		Calendar nextChange = Calendar.getInstance();
		Olympiad._nextWeeklyChange = nextChange.getTimeInMillis() + Olympiad.WEEKLY_PERIOD;
	}
}