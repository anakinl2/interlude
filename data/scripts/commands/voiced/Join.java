package commands.voiced;

import events.TeamvsTeam.EventScheduler;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.ext.scripts.Scripts;
import l2d.game.handler.IVoicedCommandHandler;
import l2d.game.handler.VoicedCommandHandler;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;

public class Join extends Functions implements IVoicedCommandHandler, ScriptFile
{
	public static L2Object self;
	public static L2NpcInstance npc;

	private String[] _commandList = new String[] { "join"};

	public void onLoad()
	{
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public boolean useVoicedCommand(String command, L2Player activeChar, String args)
	{
		command = command.intern();
		if(command.equalsIgnoreCase("join"))
		{
			String nextEvent = EventScheduler.nextEventDisplay();
			long nextEventTime = EventScheduler.nextEventTime();
			
			if(nextEventTime == 0)
			{
				if(nextEvent.equals("[ Impulse TeamDeathMatch ]"))
					Scripts.callScripts("events.TeamvsTeam.TeamvsTeamDM", "addPlayer",activeChar);
				else if(nextEvent.equals("[ Impulse TeamFight ]"))
					Scripts.callScripts("events.TeamvsTeam.TeamvsTeam", "addPlayer",activeChar);
			}
			else if(nextEventTime == -1)
			{
				activeChar.sendMessage(nextEvent+" is now taking place. You cant join.");
			}
			else
			{
				activeChar.sendMessage("Next event "+nextEvent+" in "+nextEventTime+" minutes.");
			}
		}
		return false;
	}

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}