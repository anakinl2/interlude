package commands.admin;

import java.util.StringTokenizer;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.loginservercon.LSConnection;
import com.lineage.game.loginservercon.gspackets.ChangeAccessLevel;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.tables.ReflectionTable;
import com.lineage.util.AutoBan;
import com.lineage.util.GArray;
import com.lineage.util.Location;
import com.lineage.util.HWID;

public class AdminBan implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_ban,
		admin_unban,
		admin_chatban,
		admin_ckarma,
		admin_cban,
		admin_chatunban,
		admin_acc_ban,
		admin_acc_unban,
		admin_trade_ban,
		admin_trade_unban,
		admin_jail,
		admin_unjail,
		admin_banhwid,
		admin_ban_hwid,
		admin_unban_hwid,
		admin_unbanhwid
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanBan)
			return false;

		StringTokenizer st = new StringTokenizer(fullString);

		switch(command)
		{
			case admin_ban:
				try
				{
					st.nextToken();

					String player = st.nextToken();

					int time = 0;
					String bmsg = "";
					String msg = "";

					if(st.hasMoreTokens())
						time = Integer.parseInt(st.nextToken());

					if(st.hasMoreTokens())
					{
						bmsg = "admin_ban " + player + " " + time + " ";
						msg = fullString.substring(bmsg.length(), fullString.length());
					}

					L2Player plyr = L2World.getPlayer(player);
					if(plyr != null)
					{
						plyr.sendMessage(new CustomMessage("scripts.commands.admin.AdminBan.YoureBannedByGM", plyr).addString(activeChar.getName()));
						plyr.setAccessLevel(-100);
						AutoBan.Banned(plyr, time, msg, activeChar.getName());
						plyr.logout(false, false, true);
						activeChar.sendMessage("You banned " + plyr.getName());
					}
					else if(AutoBan.Banned(player, -100, time, msg, activeChar.getName()))
						activeChar.sendMessage("You banned " + player);
					else
						activeChar.sendMessage("Can't find char: " + player);
				}
				catch(Exception e)
				{
					activeChar.sendMessage("Command syntax: //ban char_name days reason");
				}
				break;
			case admin_unban:
				if(st.countTokens() > 1)
				{
					st.nextToken();
					String player = st.nextToken();
					if(AutoBan.Banned(player, 0, 0, "", activeChar.getName()))
						activeChar.sendMessage("You unbanned " + player);
					else
						activeChar.sendMessage("Can't find char: " + player);
				}
				break;
			case admin_acc_ban:
				if(st.countTokens() > 1)
				{
					st.nextToken();
					int time = 0;
					String reason = "command by " + activeChar.getName();
					String account = st.nextToken();
					if(account.equals("$target"))
					{
						if(activeChar.getTarget() == null || !activeChar.getTarget().isPlayer())
							return false;
						account = ((L2Player) activeChar.getTarget()).getAccountName();
					}
					if(st.hasMoreTokens())
						time = Integer.parseInt(st.nextToken());
					if(st.hasMoreTokens())
						reason = activeChar.getName() + ": " + st.nextToken();
					LSConnection.getInstance().sendPacket(new ChangeAccessLevel(account, -100, reason, time));
					activeChar.sendMessage("You banned " + account + ", reason: " + reason);
					L2Player tokick = null;
					for(L2Player p : L2World.getAllPlayers())
						if(p.getAccountName().equalsIgnoreCase(account))
						{
							tokick = p;
							break;
						}
					if(tokick != null)
					{
						tokick.logout(false, false, true);
						activeChar.sendMessage("Player " + tokick.getName() + " kicked.");
					}
				}
				break;
			case admin_acc_unban:
				if(st.countTokens() > 1)
				{
					st.nextToken();
					String account = st.nextToken();
					LSConnection.getInstance().sendPacket(new ChangeAccessLevel(account, 0, "command admin_acc_unban", 0));
					activeChar.sendMessage("You unbanned " + account);
				}
				break;
			case admin_trade_ban:
				if(activeChar.getTarget() == null || !activeChar.getTarget().isPlayer())
					return false;
				st.nextToken();
				L2Player targ = (L2Player) activeChar.getTarget();
				long time = -1;
				if(st.hasMoreTokens())
					time = Long.parseLong(st.nextToken()) * 24 * 60 * 60 * 1000 + System.currentTimeMillis();
				targ.setVar("tradeBan", String.valueOf(time));
				if(targ.isInOfflineMode())
				{
					targ.setOfflineMode(false);
					targ.logout(false, false, true);
					if(targ.getNetConnection() != null)
						targ.getNetConnection().disconnectOffline();
				}
				else if(targ.isInStoreMode())
				{
					targ.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
					targ.broadcastUserInfo(true);
					targ.getBuyList().clear();
				}
				break;
			case admin_trade_unban:
				if(activeChar.getTarget() == null || !activeChar.getTarget().isPlayer())
					return false;
				((L2Player) activeChar.getTarget()).unsetVar("tradeBan");
				break;
			case admin_chatban:
				try
				{
					st.nextToken();
					String player = st.nextToken();
					String srok = st.nextToken();
					String bmsg = "admin_chatban " + player + " " + srok + " ";
					String msg = fullString.substring(bmsg.length(), fullString.length());

					if(AutoBan.ChatBan(player, Integer.parseInt(srok), msg, activeChar.getName()))
						activeChar.sendMessage("You ban chat for " + player + ".");
					else
						activeChar.sendMessage("Can't find char " + player + ".");
				}
				catch(Exception e)
				{
					activeChar.sendMessage("Command syntax: //chatban char_name period reason");
				}
				break;
			case admin_chatunban:
				try
				{
					st.nextToken();
					String player = st.nextToken();

					if(AutoBan.ChatUnBan(player, activeChar.getName()))
						activeChar.sendMessage("You unban chat for " + player + ".");
					else
						activeChar.sendMessage("Can't find char " + player + ".");
				}
				catch(Exception e)
				{
					activeChar.sendMessage("Command syntax: //chatunban char_name");
					e.printStackTrace();
				}
				break;
			case admin_jail:
				try
				{
					st.nextToken();
					String player = st.nextToken();
					String srok = st.nextToken();

					L2Player target = L2World.getPlayer(player);

					if(target != null)
	                {
	                    target.setVar("jailedFrom", target.getX() + ";" + target.getY() + ";" + target.getZ());
	                    target.setVar("jailed", srok); // "jailed" - Срок на нарах в минутах
	                    target._unjailTask = ThreadPoolManager.getInstance().scheduleGeneral(target.new UnJailTask(target.getLoc()), Integer.parseInt(srok) * 60000);
	                    if(target.getParty() != null)
                        {
                            if(target.getParty().isLeader(target))
                            {
                            	GArray<L2Player> members = target.getParty().getPartyMembers();
                                for(L2Player cha : members)
                                    cha.leaveParty();
                            }
                            else
                                target.leaveParty();
                        }
						target.teleToLocation(-114648, -249384, -2984);
	                    target.setReflection(0);
	                    activeChar.sendMessage("Вы посадили в тюрьму: " + player + " на " + srok + " минут.");
	                    target.sendMessage("GM " + activeChar.getName() + " посадил вас в тюрьму на " + srok + " минут.");
	                }
	                else
	                    activeChar.sendMessage("Игрок не найден: " + player + ".");
				}
				catch(Exception e)
				{
					activeChar.sendMessage("Command syntax: //jail char_name period");
					e.printStackTrace();
				}
				break;
			case admin_unjail:
				try
				{
					st.nextToken();
					String player = st.nextToken();

					L2Player target = L2World.getPlayer(player);

					if(target != null && target.getVar("jailed") != null)
					{
						String[] re = target.getVar("jailedFrom").split(";");
						target.teleToLocation(Integer.parseInt(re[0]), Integer.parseInt(re[1]), Integer.parseInt(re[2]));
						target.setReflection(re.length > 3 ? Integer.parseInt(re[3]) : 0);
						target._unjailTask.cancel(true);
						target.unsetVar("jailedFrom");
						target.unsetVar("jailed");
						activeChar.sendMessage("You unjailed " + player + ".");
					}
					else
						activeChar.sendMessage("Can't find char " + player + ".");
				}
				catch(Exception e)
				{
					activeChar.sendMessage("Command syntax: //unjail char_name");
					e.printStackTrace();
				}
				break;
			case admin_ckarma:
				try
				{
					st.nextToken();
					String player = st.nextToken();
					String srok = st.nextToken();
					String bmsg = "admin_ckarma " + player + " " + srok + " ";
					String msg = fullString.substring(bmsg.length(), fullString.length());

					L2Player plyr = L2World.getPlayer(player);
					if(plyr != null)
					{
						int newKarma = Integer.parseInt(srok) + plyr.getKarma();

						// update karma
						plyr.setKarma(newKarma);

						plyr.sendMessage("You get karma(" + srok + ") by GM " + activeChar.getName());
						AutoBan.Karma(plyr, Integer.parseInt(srok), msg, activeChar.getName());
						activeChar.sendMessage("You set karma(" + srok + ") " + plyr.getName());
					}
					else if(AutoBan.Karma(player, Integer.parseInt(srok), msg, activeChar.getName()))
						activeChar.sendMessage("You set karma(" + srok + ") " + player);
					else
						activeChar.sendMessage("Can't find char: " + player);
				}
				catch(Exception e)
				{
					activeChar.sendMessage("Command syntax: //ckarma char_name karma reason");
				}
				break;
			case admin_cban:
				AdminHelpPage.showHelpPage(activeChar, "cban.htm");
				break;
			case admin_banhwid:
			case admin_ban_hwid:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //banhwid char_name|hwid [kick:true|false] [reason]");
					return false;
				}
				try
				{
					activeChar.sendMessage(HWID.handleBanHWID(wordList));
				}
				catch(Exception e)
				{
					activeChar.sendMessage("USAGE: //banhwid char_name|hwid [kick:true|false] [reason]");
					e.printStackTrace();
				}
				break;
			case admin_unbanhwid:
			case admin_unban_hwid:
				if(!Config.PROTECT_ENABLE || !Config.PROTECT_GS_ENABLE_HWID_BANS)
				{
					activeChar.sendMessage("HWID bans feature disabled");
					return false;
				}
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //unbanhwid hwid");
					return false;
				}
				if(wordList[1].length() != 32)
				{
					activeChar.sendMessage(wordList[1] + " is not like HWID");
					return false;
				}
				HWID.UnbanHWID(wordList[1]);
				activeChar.sendMessage("HWID " + wordList[1] + " unbanned");
				break;
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
		// init jail reflection
		ReflectionTable.getInstance().get(-3, true).setCoreLoc(new Location(-114648, -249384, -2984));
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}