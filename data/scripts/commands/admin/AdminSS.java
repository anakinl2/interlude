package commands.admin;

import java.util.StringTokenizer;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Player;
import l2d.game.model.entity.SevenSigns;

@SuppressWarnings("unused")
public class AdminSS implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_ssq_change,
		admin_ssq_time
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanUseGMCommand)
			return false;

		if(fullString.startsWith("admin_ssq_change"))
		{
			StringTokenizer st = new StringTokenizer(fullString);

			if(st.countTokens() > 2)
			{
				st.nextToken();
				int period = Integer.parseInt(st.nextToken());
				int minutes = Integer.parseInt(st.nextToken());
				SevenSigns.getInstance().changePeriod(period, minutes * 60);
			}
			else if(st.countTokens() > 1)
			{
				st.nextToken();
				int period = Integer.parseInt(st.nextToken());
				SevenSigns.getInstance().changePeriod(period);
			}
			else
				SevenSigns.getInstance().changePeriod();
		}
		if(fullString.startsWith("admin_ssq_time"))
		{
			StringTokenizer st = new StringTokenizer(fullString);

			if(st.countTokens() > 1)
			{
				st.nextToken();
				int time = Integer.parseInt(st.nextToken());
				SevenSigns.getInstance().setTimeToNextPeriodChange(time);
			}
		}
		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}