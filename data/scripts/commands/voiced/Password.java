package commands.voiced;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.IVoicedCommandHandler;
import l2d.game.handler.VoicedCommandHandler;
import l2d.game.loginservercon.LSConnection;
import l2d.game.loginservercon.gspackets.ChangePassword;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import com.lineage.util.Util;

public class Password extends Functions implements IVoicedCommandHandler, ScriptFile
{
	public static L2Object self;
	public static L2NpcInstance npc;

	private String[] _commandList = new String[] { "password" };

	public void onLoad()
	{
		if(Config.SERVICES_CHANGE_PASSWORD)
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public void check(String[] var)
	{
		if(var.length != 3)
		{
			show(new CustomMessage("scripts.commands.user.password.IncorrectValues", self), (L2Player) self);
			return;
		}
		useVoicedCommand("password", (L2Player) self, var[0] + " " + var[1] + " " + var[2]);
	}

	public boolean useVoicedCommand(String command, L2Player activeChar, String target)
	{
		if(command.equals("password") && (target == null || target.equals("")))
		{
			show("data/scripts/commands/voiced/password.html", activeChar);
			return true;
		}

		String[] parts = target.split(" ");

		if(parts.length != 3)
		{
			show(new CustomMessage("scripts.commands.user.password.IncorrectValues", activeChar), activeChar);
			return false;
		}

		if(!parts[1].equals(parts[2]))
		{
			show(new CustomMessage("scripts.commands.user.password.IncorrectConfirmation", activeChar), activeChar);
			return false;
		}

		if(parts[1].equals(parts[0]))
		{
			show(new CustomMessage("scripts.commands.user.password.NewPassIsOldPass", activeChar), activeChar);
			return false;
		}

		if(parts[1].length() < 5 || parts[1].length() > 20)
		{
			show(new CustomMessage("scripts.commands.user.password.IncorrectSize", activeChar), activeChar);
			return false;
		}

		if(!Util.isMatchingRegexp(parts[1], Config.APASSWD_TEMPLATE))
		{
			show(new CustomMessage("scripts.commands.user.password.IncorrectInput", activeChar), activeChar);
			return false;
		}

		LSConnection.getInstance().sendPacket(new ChangePassword(activeChar.getAccountName(), parts[0], parts[1], activeChar.getNetConnection().HWID));
		return true;
	}

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}
