package commands.voiced;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.IVoicedCommandHandler;
import l2d.game.handler.VoicedCommandHandler;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;

public class PvpPoints extends Functions implements IVoicedCommandHandler, ScriptFile
{
	public static L2Object self;
	public static L2NpcInstance npc;

	private String[] _commandList = new String[] { "pvp"};

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
		if(command.equalsIgnoreCase("pvp"))
			activeChar.sendMessage("Your current class pvp points - "+activeChar.getActiveClass().getPvPCount());
		return false;
	}

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}