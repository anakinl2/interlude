package commands.user;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IUserCommandHandler;
import com.lineage.game.handler.UserCommandHandler;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.olympiad.Olympiad;
import com.lineage.game.serverpackets.SystemMessage;

/**
 * Support for /olympiadstat command
 */
public class OlympiadStat implements IUserCommandHandler, ScriptFile
{
	private static final int[] COMMAND_IDS = { 109 };

	public boolean useUserCommand(int id, L2Player activeChar)
	{
		if(id != COMMAND_IDS[0])
			return false;

		SystemMessage sm;

		if(!activeChar.isNoble())
			sm = new SystemMessage(SystemMessage.THIS_COMMAND_CAN_ONLY_BE_USED_BY_A_NOBLESSE);
		else
		{
			sm = new SystemMessage(SystemMessage.THE_CURRENT_FOR_THIS_OLYMPIAD_IS_S1_WINS_S2_DEFEATS_S3_YOU_HAVE_EARNED_S4_OLYMPIAD_POINTS);
			sm.addNumber(Olympiad.getCompetitionDone(activeChar.getObjectId()));
			sm.addNumber(Olympiad.getCompetitionWin(activeChar.getObjectId()));
			sm.addNumber(Olympiad.getCompetitionLoose(activeChar.getObjectId()));
			sm.addNumber(Olympiad.getNoblePoints(activeChar.getObjectId()));
		}
		activeChar.sendPacket(sm);
		return true;
	}

	public int[] getUserCommandList()
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
