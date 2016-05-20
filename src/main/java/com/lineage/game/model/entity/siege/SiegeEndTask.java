package com.lineage.game.model.entity.siege;

import java.util.Calendar;

import com.lineage.game.ThreadPoolManager;
import com.lineage.game.serverpackets.SystemMessage;

public class SiegeEndTask implements Runnable
{
	private Siege _siege;

	public SiegeEndTask(Siege siege)
	{
		_siege = siege;
	}

	@Override
	public void run()
	{
		if(!_siege.isInProgress())
			return;

		try
		{
			long timeRemaining = _siege.getSiegeEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
			if(timeRemaining > 3600000)
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining - 3600000); // Prepare task for 1 hr left.
			else if(timeRemaining <= 3600000 && timeRemaining > 600000)
			{
				Siege.announceToPlayer(new SystemMessage(SystemMessage.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION).addNumber(Math.round(timeRemaining / 60000) + 1), false);
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining - 600000); // Prepare task for 10 minute left.
			}
			else if(timeRemaining <= 600000 && timeRemaining > 300000)
			{
				Siege.announceToPlayer(new SystemMessage(SystemMessage.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION).addNumber(Math.round(timeRemaining / 60000) + 1), false);
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining - 300000); // Prepare task for 5 minute left.
			}
			else if(timeRemaining <= 300000 && timeRemaining > 10000)
			{
				Siege.announceToPlayer(new SystemMessage(SystemMessage.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION).addNumber(Math.round(timeRemaining / 60000) + 1), false);
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining - 10000); // Prepare task for 10 seconds count down
			}
			else if(timeRemaining <= 10000 && timeRemaining > 0)
			{
				Siege.announceToPlayer(new SystemMessage(SystemMessage.CASTLE_SIEGE_S1_SECOND_S_LEFT).addNumber(Math.round(timeRemaining / 1000) + 1), false);
				ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(_siege), timeRemaining); // Prepare task for second count down
			}
			else
				_siege.endSiege();
		}
		catch(Throwable t)
		{}
	}
}
