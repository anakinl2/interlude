package commands.voiced;

import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.IVoicedCommandHandler;
import l2d.game.handler.VoicedCommandHandler;
import l2d.game.instancemanager.CastleManager;
import l2d.game.model.L2Player;
import l2d.game.model.entity.residence.Residence;
import l2d.game.model.instances.L2DoorInstance;

public class PlayerCastleDoors implements IVoicedCommandHandler, ScriptFile
{
	private static String[] _voicedCommands = { "open", "close" };

	public boolean useVoicedCommand(String command, L2Player activeChar, String target)
	{
		if(command.startsWith("open") && target.equals("doors") && activeChar.isClanLeader())
		{
			if(activeChar.getTarget() instanceof L2DoorInstance)
			{
				L2DoorInstance door = (L2DoorInstance) activeChar.getTarget();
				Residence castle = CastleManager.getInstance().getCastleByIndex(activeChar.getClan().getHasCastle());
				if(door == null || castle == null)
					return false;
				if(castle.checkIfInZone(door.getX(), door.getY()))
					door.openMe();
			}
			else
				return false;
		}
		else if(command.startsWith("close") && target.equals("doors") && activeChar.isClanLeader())
			if(activeChar.getTarget() instanceof L2DoorInstance)
			{
				L2DoorInstance door = (L2DoorInstance) activeChar.getTarget();
				Residence castle = CastleManager.getInstance().getCastleByIndex(activeChar.getClan().getHasCastle());
				if(door == null || castle == null)
					return false;
				if(castle.checkIfInZone(door.getX(), door.getY()))
					door.closeMe();
			}
			else
				return false;
		return true;
	}

	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}

	public void onLoad()
	{
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}
