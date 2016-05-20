package commands.voiced;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IVoicedCommandHandler;
import com.lineage.game.handler.VoicedCommandHandler;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2NpcInstance;

public class Score extends Functions implements IVoicedCommandHandler, ScriptFile
{
	public static L2Object self;
	public static L2NpcInstance npc;

	private String[] _commandList = new String[] { "score"};

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
		if(command.equalsIgnoreCase("score"))
			activeChar.sendMessage("Current Event Points - "+activeChar.getEventPoints());
		return false;
	}

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}