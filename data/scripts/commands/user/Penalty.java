package commands.user;

import java.text.SimpleDateFormat;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IUserCommandHandler;
import com.lineage.game.handler.UserCommandHandler;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.util.Files;

/**
 * Support for commands:
 * /clanpenalty
 * /instancezone 
 */
public class Penalty extends Functions implements IUserCommandHandler, ScriptFile
{
	private static final int[] COMMAND_IDS = { 100, 114 };

	public boolean useUserCommand(int id, L2Player activeChar)
	{
		if(COMMAND_IDS[0] == id)
		{
			long _leaveclan = 0;
			if(activeChar.getLeaveClanTime() != 0)
				_leaveclan = activeChar.getLeaveClanTime() + 1 * 24 * 60 * 60 * 1000;
			long _deleteclan = 0;
			if(activeChar.getDeleteClanTime() != 0)
				_deleteclan = activeChar.getDeleteClanTime() + 10 * 24 * 60 * 60 * 1000;
			SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
			String html = Files.read("data/scripts/commands/user/penalty.htm", activeChar);

			if(activeChar.getClanId() == 0)
			{
				if(_leaveclan == 0 && _deleteclan == 0)
				{
					html = html.replaceFirst("%reason%", "No penalty is imposed.");
					html = html.replaceFirst("%expiration%", " ");
				}
				else if(_leaveclan > 0 && _deleteclan == 0)
				{
					html = html.replaceFirst("%reason%", "Penalty for leaving clan.");
					html = html.replaceFirst("%expiration%", format.format(_leaveclan));
				}
				else if(_deleteclan > 0)
				{
					html = html.replaceFirst("%reason%", "Penalty for dissolving clan.");
					html = html.replaceFirst("%expiration%", format.format(_deleteclan));
				}
			}
			else if(activeChar.getClan().canInvite())
			{
				html = html.replaceFirst("%reason%", "No penalty is imposed.");
				html = html.replaceFirst("%expiration%", " ");
			}
			else
			{
				html = html.replaceFirst("%reason%", "Penalty for expelling clan member.");
				html = html.replaceFirst("%expiration%", format.format(activeChar.getClan().getExpelledMemberTime()));
			}
			show(html, activeChar);
		}
		else if(COMMAND_IDS[1] == id)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.INSTANCE_ZONE_TIME_LIMIT));
			int kamaloka_hall = activeChar.getTimeToNextEnterInstance("KamalokaHall");
			int kamaloka_nm = activeChar.getTimeToNextEnterInstance("KamalokaNightmare");
			if(kamaloka_hall == 0 && kamaloka_nm == 0)
				activeChar.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NO_INSTANCE_ZONE_UNDER_A_TIME_LIMIT));
			else
			{
				if(kamaloka_hall > 0)
					activeChar.sendPacket(new SystemMessage(SystemMessage.S1_WILL_BE_AVAILABLE_FOR_RE_USE_AFTER_S2_HOURS_S3_MINUTES).addString("Kamaloka, the Hall of Abyss").addNumber(kamaloka_hall / 60).addNumber(kamaloka_hall % 60));
				if(kamaloka_nm > 0)
					activeChar.sendPacket(new SystemMessage(SystemMessage.S1_WILL_BE_AVAILABLE_FOR_RE_USE_AFTER_S2_HOURS_S3_MINUTES).addString("Kamaloka, the Hall of Nightmares").addNumber(kamaloka_nm / 60).addNumber(kamaloka_nm % 60));
			}
		}
		return false;
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
