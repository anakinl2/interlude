package events.TeamvsTeam;

import java.util.concurrent.ScheduledFuture;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.util.Rnd;

/**
 * 
 * @author Midnex
 *
 */
public class EventScheduler extends Functions implements ScriptFile
{
	static int _nextEvent = 0;
	static long nextEventTime;
	static String[] events = {"TeamvsTeamDM","TeamvsTeam"};
	static ScheduledFuture<?> clock = null;
	
	public static void setEvent(boolean first)
	{
		if(clock != null)
		{
			clock.cancel(false);
			clock = null;
		}
		if(first)
		{
			_nextEvent = Rnd.get(0,1);
			clock = executeTask("events.TeamvsTeam." + events[_nextEvent], "init", new Object[0], 900000);//15min
			nextEventTime = System.currentTimeMillis()+900000;
		}
		else
		{
			_nextEvent = _nextEvent == 0 ? 1 : 0;
			clock = executeTask("events.TeamvsTeam." + events[_nextEvent], "init", new Object[0], 3600000);//15min
			nextEventTime = System.currentTimeMillis()+3600000;
		}
	}
	
	public static String nextEvent()
	{
		return events[_nextEvent];
	}
	
	public static String nextEventDisplay()
	{
		String  e = events[_nextEvent];
		if(e.equals("TeamvsTeamDM"))
			return "[ Impulse TeamDeathMatch ]";
		else if(e.equals("TeamvsTeam"))
			return "[ Impulse TeamFight ]";
		return "ERROR";
	}

	
	public static long nextEventTime()
	{
		return nextEventTime < 1 ? nextEventTime : (nextEventTime - System.currentTimeMillis())/60000;
	}
	
	public static void setNextEventTime(int i)
	{
		if(i == 0)
		{
			if(clock != null)
			{
				clock.cancel(false);
				clock = null;
			}
		}
		nextEventTime = i;
	}
	
	public static void setEventName(int id)
	{
		_nextEvent = id;
	}
	
	@Override
	public void onReload()
	{
		setEvent(true);
	}

	@Override
	public void onShutdown()
	{}

	@Override
	public void onLoad()
	{
		setEvent(true);
	}
}
