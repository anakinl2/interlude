package commands.admin;

import java.lang.reflect.Constructor;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import l2d.db.mysql;
import l2d.ext.scripts.ScriptFile;
import l2d.ext.scripts.Scripts;
import l2d.game.ai.L2CharacterAI;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.idfactory.IdFactory;
import l2d.game.instancemanager.BoatManager;
import l2d.game.instancemanager.RaidBossSpawnManager;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.GmListTable;
import l2d.game.tables.NpcTable;
import l2d.game.tables.SpawnTable;
import l2d.game.templates.L2CharTemplate;
import l2d.game.templates.L2NpcTemplate;
import l2d.game.templates.StatsSet;
import l2d.util.Log;

@SuppressWarnings("unused")
public class AdminSpawn implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_show_spawns,
		admin_spawn,
		admin_spawn_monster,
		admin_spawn_index,
		admin_unspawnall,
		admin_spawn1,
		admin_setheading,
		admin_setai
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanEditNPC)
			return false;

		if(fullString.equals("admin_show_spawns"))
			AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
		else if(fullString.startsWith("admin_spawn_index"))
			try
			{
				String val = fullString.substring(18);
				AdminHelpPage.showHelpPage(activeChar, "spawns/" + val + ".htm");
			}
			catch(StringIndexOutOfBoundsException e)
			{}
		else if(fullString.startsWith("admin_spawn1"))
		{
			StringTokenizer st = new StringTokenizer(fullString, " ");
			try
			{
				st.nextToken();
				String id = st.nextToken();
				int mobCount = 1;
				if(st.hasMoreTokens())
					mobCount = Integer.parseInt(st.nextToken());
				spawnMonster(activeChar, id, 0, mobCount);
			}
			catch(Exception e)
			{
				// Case of wrong monster data
			}
		}
		else if(fullString.startsWith("admin_spawn") || fullString.startsWith("admin_spawn_monster"))
		{
			StringTokenizer st = new StringTokenizer(fullString, " ");
			try
			{
				st.nextToken();
				String id = st.nextToken();
				int respawnTime = 30;
				int mobCount = 1;
				if(st.hasMoreTokens())
					mobCount = Integer.parseInt(st.nextToken());
				if(st.hasMoreTokens())
					respawnTime = Integer.parseInt(st.nextToken());
				spawnMonster(activeChar, id, respawnTime, mobCount);
			}
			catch(Exception e)
			{
				// Case of wrong monster data
			}
		}
		else if(fullString.startsWith("admin_unspawnall"))
		{
			for(L2Player player : L2World.getAllPlayers())
				player.sendPacket(new SystemMessage(SystemMessage.THE_NPC_SERVER_IS_NOT_OPERATING));
			L2World.deleteVisibleNpcSpawns();
			GmListTable.broadcastMessageToGMs("NPC Unspawn completed!");
		}
		else if(fullString.startsWith("admin_setai"))
		{
			if(activeChar.getTarget() == null || !activeChar.getTarget().isNpc())
			{
				activeChar.sendMessage("Please select target NPC or mob.");
				return false;
			}

			StringTokenizer st = new StringTokenizer(fullString, " ");
			st.nextToken();
			if(!st.hasMoreTokens())
			{
				activeChar.sendMessage("Please specify AI name.");
				return false;
			}
			String aiName = st.nextToken();
			L2NpcInstance target = (L2NpcInstance) activeChar.getTarget();

			Constructor<?> aiConstructor = null;
			try
			{
				if(!aiName.equalsIgnoreCase("npc"))
					aiConstructor = Class.forName("l2d.game.ai." + aiName).getConstructors()[0];
			}
			catch(Exception e)
			{
				try
				{
					aiConstructor = Scripts.getInstance().getClasses().get("ai." + aiName).getRawClass().getConstructors()[0];
				}
				catch(Exception e1)
				{
					activeChar.sendMessage("This type AI not found.");
					return false;
				}
			}

			target.detachAI();

			if(aiConstructor != null)
			{
				try
				{
					target.setAI((L2CharacterAI) aiConstructor.newInstance(new Object[] { target }));
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				target.getAI().startAITask();
			}
		}
		else if(fullString.startsWith("admin_setheading"))
		{
			L2Object obj = activeChar.getTarget();
			if(!obj.isNpc())
			{
				activeChar.sendMessage("Target is incorrect!");
				return false;
			}

			L2NpcInstance npc = (L2NpcInstance) obj;

			L2Spawn spawn = npc.getSpawn();
			if(spawn == null)
			{
				activeChar.sendMessage("Spawn for this npc == null!");
				return false;
			}

			if(!mysql.set("update spawnlist set heading = " + activeChar.getHeading() //
					+ " where npc_templateid = " + npc.getNpcId() //
					+ " and locx = " + spawn.getLocx() //
					+ " and locy = " + spawn.getLocy() //
					+ " and locz = " + spawn.getLocz() //
					+ " and loc_id = " + spawn.getLocation()))
			{
				activeChar.sendMessage("Error in mysql query!");
				return false;
			}

			npc.setHeading(activeChar.getHeading());
			npc.decayMe();
			npc.spawnMe();
			activeChar.sendMessage("New heading : " + activeChar.getHeading());
		}
		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void spawnMonster(L2Player activeChar, String monsterId, int respawnTime, int mobCount)
	{
		L2Object target = activeChar.getTarget();
		if(target == null)
			target = activeChar;

		Pattern pattern = Pattern.compile("[0-9]*");
		Matcher regexp = pattern.matcher(monsterId);
		L2NpcTemplate template;
		if(regexp.matches())
		{
			// First parameter was an ID number
			int monsterTemplate = Integer.parseInt(monsterId);
			template = NpcTable.getTemplate(monsterTemplate);
		}
		else
		{
			// First parameter wasn't just numbers so go by name not ID
			monsterId = monsterId.replace('_', ' ');
			template = NpcTable.getTemplateByName(monsterId);
		}

		if(template == null)
		{
			activeChar.sendMessage("Incorrect monster template.");
			return;
		}

		try
		{
			L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(target.getLoc());
			spawn.setLocation(0);
			spawn.setAmount(mobCount);
			spawn.setHeading(activeChar.getHeading());
			spawn.setRespawnDelay(respawnTime);

			if(RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcId()))
				activeChar.sendMessage("Raid Boss " + template.name + " already spawned.");
			else
			{
				if(template.isRaid)
				{
					if(respawnTime != 0)
						RaidBossSpawnManager.getInstance().addNewSpawn(spawn, true);
				}
				else
					SpawnTable.getInstance().addNewSpawn(spawn, respawnTime != 0);

				spawn.init();
				if(respawnTime == 0)
					spawn.stopRespawn();

				activeChar.sendMessage("Created " + template.name + " on " + target.getObjectId() + ".");

				Log.add("Created " + template.name + " on " + target.getObjectId(), "gm_ext_actions", activeChar);
			}
		}
		catch(Exception e)
		{
			activeChar.sendMessage("Target is not ingame.");
		}
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