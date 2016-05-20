package commands.user;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.GameTimeController;
import com.lineage.game.handler.IUserCommandHandler;
import com.lineage.game.handler.UserCommandHandler;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;

/**
 * Support for /time command
 */
public class Time implements IUserCommandHandler, ScriptFile
{
	private static final int[] COMMAND_IDS = { 77 };

	public boolean useUserCommand(int id, L2Player activeChar)
	{
		if(COMMAND_IDS[0] != id)
			return false;

		int t = GameTimeController.getInstance().getGameTime();
		int h = t / 60 % 24;
		int m = t % 60;

		SystemMessage sm;
		if(h >= 0 && h < 6)
			sm = new SystemMessage(SystemMessage.THE_CURRENT_TIME_IS_S1S2_IN_THE_NIGHT);
		else
			sm = new SystemMessage(SystemMessage.THE_CURRENT_TIME_IS_S1S2_IN_THE_DAY);
		sm.addNumber(h);
		sm.addNumber(m);
		activeChar.sendPacket(sm);
		return true;
	}

	public final int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}

	public void onLoad()
	{
		UserCommandHandler.getInstance().registerUserCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}
