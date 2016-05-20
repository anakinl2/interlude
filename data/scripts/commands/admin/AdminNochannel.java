package commands.admin;

import java.util.StringTokenizer;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.Announcements;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.util.AutoBan;
import com.lineage.util.Log;
import com.lineage.util.Util;

public class AdminNochannel implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_nochannel,
		admin_nc
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanBanChat)
			return false;

		int banChatCount = 0;
		int banChatCountPerDay = activeChar.getPlayerAccess().BanChatCountPerDay;
		if(banChatCountPerDay > -1)
		{
			String count = activeChar.getVar("banChatCount");
			if(count != null)
				banChatCount = Integer.parseInt(count);

			long LastBanChatDayTime = 0;
			String time = activeChar.getVar("LastBanChatDayTime");
			if(time != null)
				LastBanChatDayTime = Long.parseLong(time);

			if(LastBanChatDayTime != 0)
			{
				if(System.currentTimeMillis() - LastBanChatDayTime < 1000 * 60 * 60 * 24)
				{
					if(banChatCount >= banChatCountPerDay)
					{
						activeChar.sendMessage("В сутки, вы можете выдать не более " + banChatCount + " банов чата.");
						return false;
					}
				}
				else
				{
					activeChar.setVar("LastBanChatDayTime", "" + System.currentTimeMillis());
					activeChar.setVar("banChatCount", "0");
					banChatCount = 0;
					if(activeChar.getPlayerAccess().BanChatBonusId > 0 && activeChar.getPlayerAccess().BanChatBonusCount > 0)
						Functions.addItem(activeChar, activeChar.getPlayerAccess().BanChatBonusId, activeChar.getPlayerAccess().BanChatBonusCount);
				}
			}
			else
				activeChar.setVar("LastBanChatDayTime", "" + System.currentTimeMillis());
		}

		switch(command)
		{
			case admin_nochannel:
			case admin_nc:
			{
				StringTokenizer st = new StringTokenizer(fullString);
				if(st.countTokens() > 1)
				{
					st.nextToken();
					String char_name = st.nextToken();
					L2Player player = L2World.getPlayer(char_name);
					int obj_id = 0;
					if(player == null)
						obj_id = Util.GetCharIDbyName(char_name);
					else
						char_name = player.getName();

					if(player != null || obj_id > 0)
					{
						int timeval = 30; // if no args, then 30 min default.
						String reason = "не указана"; // if no args, then "не указана" default.
						String admin_name = activeChar.getName();

						if(st.countTokens() >= 1)
						{
							String time = st.nextToken();
							try
							{
								timeval = Integer.parseInt(time);
							}
							catch(Exception E)
							{
								timeval = 30;
							}
						}

						if(st.countTokens() >= 1)
						{
							reason = st.nextToken();
							while(st.hasMoreTokens())
								reason += " " + st.nextToken();
						}

						Announcements sys = new Announcements();
						if(timeval == 0)
						{
							if(!activeChar.getPlayerAccess().CanUnBanChat)
							{
								activeChar.sendMessage("Вы не имеете прав на снятие бана чата.");
								return false;
							}
							if(Config.MAT_ANNOUNCE)
								if(Config.MAT_ANNOUNCE_NICK)
									sys.announceToAll(admin_name + " снял бан чата с игрока " + char_name + ".");
								else
									sys.announceToAll("С игрока " + char_name + " снят бан чата.");
							activeChar.sendMessage("Вы сняли бан чата с игрока " + char_name + ".");
							Log.add(admin_name + " снял бан чата с игрока " + char_name + ".", "banchat", activeChar);
						}
						else if(timeval < 0)
						{
							if(activeChar.getPlayerAccess().BanChatMaxValue > 0)
							{
								activeChar.sendMessage("Вы можете банить не более чем на " + activeChar.getPlayerAccess().BanChatMaxValue + " минут.");
								return false;
							}
							if(Config.MAT_ANNOUNCE)
								if(Config.MAT_ANNOUNCE_NICK)
									sys.announceToAll(admin_name + " забанил чат игроку " + char_name + " на бессрочный период, причина: " + reason + ".");
								else
									sys.announceToAll("Забанен чат игроку " + char_name + " на бессрочный период, причина: " + reason + ".");
							activeChar.sendMessage("Вы забанили чат игроку " + char_name + " на бессрочный период, причина: " + reason + ".");
							Log.add(admin_name + " забанил чат игроку " + char_name + " на бессрочный период, причина: " + reason + ".", "banchat", activeChar);
						}
						else
						{
							if(!activeChar.getPlayerAccess().CanUnBanChat && (player == null || player.getNoChannel() != 0))
							{
								activeChar.sendMessage("Вы не имеете права изменять время бана.");
								return false;
							}
							if(timeval > activeChar.getPlayerAccess().BanChatMaxValue && activeChar.getPlayerAccess().BanChatMaxValue != -1)
							{
								activeChar.sendMessage("Вы можете банить не более чем на " + activeChar.getPlayerAccess().BanChatMaxValue + " минут.");
								return false;
							}
							if(Config.MAT_ANNOUNCE)
								if(Config.MAT_ANNOUNCE_NICK)
									sys.announceToAll(admin_name + " забанил чат игроку " + char_name + " на " + timeval + " минут, причина: " + reason + ".");
								else
									sys.announceToAll("Забанен чат игроку " + char_name + " на " + timeval + " минут, причина: " + reason + ".");
							activeChar.sendMessage("Вы забанили чат игроку " + char_name + " на " + timeval + " минут, причина: " + reason + ".");
							Log.add(admin_name + " забанил чат игроку " + char_name + " на " + timeval + " минут, причина: " + reason + ".", "banchat", activeChar);
						}

						if(player != null)
							updateNoChannel(player, timeval);
						else if(obj_id > 0)
							AutoBan.ChatBan(char_name, timeval, reason, admin_name);

						if(banChatCountPerDay > -1)
						{
							banChatCount++;
							activeChar.setVar("banChatCount", "" + banChatCount);
							activeChar.sendMessage("У вас осталось " + (banChatCountPerDay - banChatCount) + " банов чата.");
						}
					}
					else
						activeChar.sendMessage("Игрок " + char_name + " не найден.");
				}
			}
				break;
		}
		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void updateNoChannel(L2Player player, int time)
	{
		player.updateNoChannel(time * 60000);
		if(time == 0)
			player.sendMessage(new CustomMessage("common.ChatUnBanned", player));
		else if(time > 0)
			player.sendMessage(new CustomMessage("common.ChatBanned", player).addNumber(time));
		else
			player.sendMessage(new CustomMessage("common.ChatBannedPermanently", player));
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