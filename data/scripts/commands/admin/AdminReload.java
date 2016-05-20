package commands.admin;

import l2d.Config;
import l2d.ext.scripts.ScriptFile;
import l2d.ext.scripts.Scripts;
import l2d.ext.scripts.Scripts.ScriptClassAndMethod;
import l2d.game.Announcements;
import l2d.game.TradeController;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Multisell;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.quest.Quest;
import l2d.game.model.quest.QuestState;
import l2d.game.tables.DoorTable;
import l2d.game.tables.FishTable;
import l2d.game.tables.GmListTable;
import l2d.game.tables.NpcTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SpawnTable;
import l2d.game.tables.StaticObjectsTable;
import l2d.game.tables.TerritoryTable;
import l2d.util.Files;
import l2d.util.Strings;

public class AdminReload implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_reload, admin_reload_multisell, admin_reload_announcements, admin_reload_gmaccess, admin_reload_htm, admin_reload_qs, admin_reload_qs_help, admin_reload_loc, admin_reload_skills, admin_reload_npc, admin_reload_spawn, admin_reload_fish, admin_reload_abuse, admin_reload_translit, admin_reload_shops, admin_reload_static, admin_reload_doors, admin_reload_pkt_logger, admin_reload_conf_ai, admin_reload_conf_altset, admin_reload_conf_bsfg, admin_reload_conf_champ, admin_reload_conf_chat, admin_reload_conf_clanhall, admin_reload_conf_craftman, admin_reload_conf_dev, admin_reload_conf_events, admin_reload_conf_gameserver, admin_reload_conf_geodata, admin_reload_conf_lottery, admin_reload_conf_olympiad, admin_reload_conf_other, admin_reload_conf_pvp, admin_reload_conf_rates, admin_reload_conf_services, admin_reload_conf_spoil, admin_reload_conf_vitality, admin_reload_conf_wedding, admin_reload_conf_eventbuffer
	}

	@SuppressWarnings("unchecked")
	public boolean useAdminCommand(final Enum comm, final String[] wordList, final String fullString, final L2Player activeChar)
	{
		final Commands command = (Commands) comm;

		if( !activeChar.getPlayerAccess().CanReload)
			return false;

		switch(command)
		{
			case admin_reload:
				break;
			case admin_reload_multisell:
			{
				try
				{
					L2Multisell.getInstance().reload();
				}
				catch(final Exception e)
				{
					return false;
				}
				for(final ScriptClassAndMethod handler : Scripts.onReloadMultiSell)
					Scripts.callScripts(handler.scriptClass, handler.method,activeChar);
				activeChar.sendMessage("Multisell list reloaded!");
				break;
			}
			case admin_reload_gmaccess:
			{
				try
				{
					Config.loadGMAccess();
					for(final L2Player player : L2World.getAllPlayers())
						if( !Config.EVERYBODY_HAS_ADMIN_RIGHTS)
							player.setPlayerAccess(Config.gmlist.get(player.getObjectId()));
						else
							player.setPlayerAccess(Config.gmlist.get(new Integer(0)));
				}
				catch(final Exception e)
				{
					return false;
				}
				activeChar.sendMessage("GMAccess reloaded!");
				break;
			}
			case admin_reload_htm:
			{
				Files.cacheClean();
				activeChar.sendMessage("HTML cache clearned.");
				break;
			}
			case admin_reload_announcements:
			{
				Announcements.getInstance().loadAnnouncements();
				Announcements.getInstance().listAnnouncements(activeChar);
				break;
			}
			case admin_reload_qs:
			{
				if(fullString.endsWith("all"))
					for(final L2Player p : L2World.getAllPlayers())
						reloadQuestStates(p);
				else
				{
					final L2Object t = activeChar.getTarget();

					if(t != null && t.isPlayer())
					{
						final L2Player p = (L2Player) t;
						reloadQuestStates(p);
					}
					else
						reloadQuestStates(activeChar);
				}
				break;
			}
			case admin_reload_qs_help:
			{
				activeChar.sendMessage("");
				activeChar.sendMessage("Quest Help:");
				activeChar.sendMessage("reload_qs_help - This Message.");
				activeChar.sendMessage("reload_qs <selected target> - reload all quest states for target.");
				activeChar.sendMessage("reload_qs <no target or target is not player> - reload quests for self.");
				activeChar.sendMessage("reload_qs all - reload quests for all players in world.");
				activeChar.sendMessage("");
				break;
			}
			case admin_reload_loc:
			{
				TerritoryTable.getInstance().reloadData();
				ZoneManager.getInstance().reload();
				GmListTable.broadcastMessageToGMs("Locations and zones reloaded.");
				break;
			}
			case admin_reload_skills:
			{
				SkillTable.getInstance().reload();
				GmListTable.broadcastMessageToGMs("Skill table reloaded by " + activeChar.getName() + ".");
				System.out.println("Skill table reloaded by " + activeChar.getName() + ".");
				break;
			}
			case admin_reload_npc:
			{
				NpcTable.getInstance().reloadAllNpc();
				GmListTable.broadcastMessageToGMs("Npc table reloaded.");
				break;
			}
			case admin_reload_spawn:
			{
				SpawnTable.getInstance().reloadAll();
				GmListTable.broadcastMessageToGMs("All npc respawned.");
				break;
			}
			case admin_reload_fish:
			{
				FishTable.getInstance().reload();
				GmListTable.broadcastMessageToGMs("Fish table reloaded.");
				break;
			}
			case admin_reload_abuse:
			{
				Config.abuseLoad();
				GmListTable.broadcastMessageToGMs("Abuse reloaded.");
				break;
			}
			case admin_reload_translit:
			{
				Strings.reload();
				GmListTable.broadcastMessageToGMs("Translit reloaded.");
				break;
			}
			case admin_reload_shops:
			{
				TradeController.reload();
				GmListTable.broadcastMessageToGMs("Shops reloaded.");
				break;
			}
			case admin_reload_static:
			{
				StaticObjectsTable.getInstance().reloadStaticObjects();
				GmListTable.broadcastMessageToGMs("Static objects table reloaded.");
				break;
			}
			case admin_reload_doors:
			{
				DoorTable.getInstance().respawn();
				GmListTable.broadcastMessageToGMs("Door table reloaded.");
				break;
			}
			case admin_reload_conf_ai:
			{
				Config.loadAIConfig();
				GmListTable.broadcastMessageToGMs("AI Config reloaded.");
				break;
			}
			case admin_reload_conf_altset:
			{
				Config.loadAlternativeConfig();
				GmListTable.broadcastMessageToGMs("Altsettings Config reloaded.");
				break;
			}
			case admin_reload_conf_bsfg:
			{
				Config.abuseLoad();
				GmListTable.broadcastMessageToGMs("BSFG Config reloaded.");
				break;
			}
			case admin_reload_conf_champ:
			{
				Config.loadChampionConfig();
				GmListTable.broadcastMessageToGMs("Champion Config reloaded.");
				break;
			}
			case admin_reload_conf_chat:
			{
				Config.loadChatConfig();
				GmListTable.broadcastMessageToGMs("Chat Config reloaded.");
				break;
			}
			case admin_reload_conf_clanhall:
			{
				Config.loadClanHallConfig();
				GmListTable.broadcastMessageToGMs("Clanhall Config reloaded.");
				break;
			}
			case admin_reload_conf_dev:
			{
				Config.loadDevelopersConfig();
				GmListTable.broadcastMessageToGMs("Developer Config reloaded.");
				break;
			}
			case admin_reload_conf_events:
			{
				Config.loadEventConfig();
				GmListTable.broadcastMessageToGMs("Events Config reloaded.");
				break;
			}
			case admin_reload_conf_gameserver:
			{
				Config.loadGameServerConfig();
				GmListTable.broadcastMessageToGMs("Gameserver Config reloaded.");
				break;
			}
			case admin_reload_conf_geodata:
			{
				Config.loadGeodataConfig();
				GmListTable.broadcastMessageToGMs("Geodata Config reloaded.");
				break;
			}
			case admin_reload_conf_lottery:
			{
				Config.loadLotteryConfig();
				GmListTable.broadcastMessageToGMs("Loterry Config reloaded.");
				break;
			}
			case admin_reload_conf_olympiad:
			{
				Config.loadOlympConfig();
				GmListTable.broadcastMessageToGMs("Olympiad Config reloaded.");
				break;
			}
			case admin_reload_conf_other:
			{
				Config.loadOtherConfig();
				GmListTable.broadcastMessageToGMs("Other Config reloaded.");
				break;
			}
			case admin_reload_conf_pvp:
			{
				Config.loadPvPConfig();
				GmListTable.broadcastMessageToGMs("PvP Config reloaded.");
				break;
			}
			case admin_reload_conf_rates:
			{
				Config.loadRateConfig();
				GmListTable.broadcastMessageToGMs("Rates Config reloaded.");
				break;
			}
			case admin_reload_conf_services:
			{
				Config.loadServicesConfig();
				GmListTable.broadcastMessageToGMs("Servises Config reloaded.");
				break;
			}
			case admin_reload_conf_spoil:
			{
				Config.loadSpoilConfig();
				GmListTable.broadcastMessageToGMs("Spoil Config reloaded.");
				break;
			}
			case admin_reload_conf_wedding:
			{
				Config.loadWeddingConfig();
				GmListTable.broadcastMessageToGMs("Wedding Config reloaded.");
				break;
			}
			case admin_reload_pkt_logger:
			{
				try
				{
					Config.reloadPacketLoggerConfig();
					activeChar.sendMessage("Packet Logger setting reloaded");
				}
				catch(final Exception e)
				{
					activeChar.sendMessage("Failed reload Packet Logger setting. Check stdout for error!");
				}
			}
		}
		AdminHelpPage.showHelpPage(activeChar, "reload.htm");
		return true;
	}

	private void reloadQuestStates(final L2Player p)
	{
		for(final QuestState qs : p.getAllQuestsStates())
			p.delQuestState(qs.getQuest().getName());
		Quest.playerEnter(p);
	}

	@SuppressWarnings("unchecked")
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