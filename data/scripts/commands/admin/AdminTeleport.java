package commands.admin;

import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.ext.scripts.ScriptFile;
import l2d.game.ai.CtrlIntention;
import l2d.game.cache.Msg;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2Territory;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.tables.NpcTable;
import l2d.game.tables.SpawnTable;
import l2d.game.templates.L2NpcTemplate;
import l2d.util.Location;
import l2d.util.Log;
import l2d.util.Rnd;
import l2d.util.Util;

public class AdminTeleport implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_show_moves,
		admin_show_moves_other,
		admin_show_teleport,
		admin_teleport_to_character,
		admin_teleportto,
		admin_teleport_to,
		admin_move_to,
		admin_moveto,
		admin_teleport_character,
		admin_recall,
		admin_walk,
		admin_recall_npc,
		admin_gonorth,
		admin_gosouth,
		admin_goeast,
		admin_gowest,
		admin_goup,
		admin_godown,
		admin_tele,
		admin_teleto,
		admin_tele_to,
		admin_failed,
		admin_tonpc,
		admin_to_npc,
		admin_correct_merchants,
		admin_correct_gh,
		admin_correct_gh2,
		admin_teletotown,
		admin_correct_tvt
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanTeleport)
			return false;

		switch(command)
		{
			case admin_show_moves:
				AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
				break;
			case admin_show_moves_other:
				AdminHelpPage.showHelpPage(activeChar, "tele/other.htm");
				break;
			case admin_show_teleport:
				showTeleportCharWindow(activeChar);
				break;
			case admin_teleport_to_character:
				teleportToCharacter(activeChar, activeChar.getTarget());
				break;
			case admin_teleport_to:
			case admin_teleportto:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //teleportto charName");
					return false;
				}
				String chaName = Util.joinStrings(" ", wordList, 1);
				L2Player cha = L2World.getPlayer(chaName);
				if(cha == null)
				{
					activeChar.sendMessage("Player '" + chaName + "' not found in world");
					return false;
				}
				teleportToCharacter(activeChar, cha);
				break;
			case admin_move_to:
			case admin_moveto:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //move_to x y z");
					return false;
				}
				teleportTo(activeChar, activeChar, Util.joinStrings(" ", wordList, 1));
				break;
			case admin_teleport_character:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //teleport_character x y z");
					return false;
				}
				teleportCharacter(activeChar, Util.joinStrings(" ", wordList, 1));
				showTeleportCharWindow(activeChar);
				break;
			case admin_recall:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //recall charName");
					return false;
				}
				String targetName = Util.joinStrings(" ", wordList, 1);
				L2Player recall_player = L2World.getPlayer(targetName);
				if(recall_player != null)
				{
					teleportTo(activeChar, recall_player, activeChar.getLoc());
					return true;
				}
				int obj_id = Util.GetCharIDbyName(targetName);
				if(obj_id > 0)
				{
					teleportCharacter_offline(obj_id, activeChar.getLoc());
					activeChar.sendMessage(targetName + " is offline. Offline teleport used...");
					Log.add("teleport player " + targetName + " to " + activeChar.getLoc(), "gm_ext_actions", activeChar);
				}
				else
					activeChar.sendMessage("->" + targetName + "<- is incorrect.");
				break;
			case admin_walk:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //walk x y z");
					return false;
				}
				try
				{
					activeChar.moveToLocation(new Location(Util.joinStrings(" ", wordList, 1)), 0, true);
				}
				catch(IllegalArgumentException e)
				{
					activeChar.sendMessage("USAGE: //walk x y z");
					return false;
				}
				break;
			case admin_recall_npc:
				recallNPC(activeChar);
				break;
			case admin_gonorth:
			case admin_gosouth:
			case admin_goeast:
			case admin_gowest:
			case admin_goup:
			case admin_godown:
				int val = wordList.length < 2 ? 150 : Integer.parseInt(wordList[1]);
				int x = activeChar.getX();
				int y = activeChar.getY();
				int z = activeChar.getZ();
				if(command == Commands.admin_goup)
					z += val;
				else if(command == Commands.admin_godown)
					z -= val;
				else if(command == Commands.admin_goeast)
					x += val;
				else if(command == Commands.admin_gowest)
					x -= val;
				else if(command == Commands.admin_gosouth)
					y += val;
				else if(command == Commands.admin_gonorth)
					y -= val;
				
				activeChar.teleToLocation(x, y, z);
				showTeleportWindow(activeChar);
				break;
			case admin_tele:
				showTeleportWindow(activeChar);
				break;
			case admin_teletotown:
				try
				{
						teleToTown(wordList[1], activeChar);
				}
				catch(StringIndexOutOfBoundsException e)
				{
					activeChar.sendMessage("Usage: //teletotown <town name>");
				}
				AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
				break;
			case admin_teleto:
			case admin_tele_to:
				if(wordList.length > 1 && wordList[1].equalsIgnoreCase("r"))
					activeChar.setTeleMode(2);
				else if(wordList.length > 1 && wordList[1].equalsIgnoreCase("end"))
					activeChar.setTeleMode(0);
				else
					activeChar.setTeleMode(1);
				break;
			case admin_failed:
				activeChar.sendMessage("Trying ActionFailed...");
				activeChar.sendActionFailed();
				break;
			case admin_tonpc:
			case admin_to_npc:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //tonpc npcId|npcName");
					return false;
				}
				String npcName = Util.joinStrings(" ", wordList, 1);
				L2NpcInstance npc;
				try
				{
					if((npc = L2World.findNpcByNpcId(Integer.parseInt(npcName))) != null)
					{
						teleportToCharacter(activeChar, npc);
						return true;
					}
				}
				catch(Exception e)
				{}
				if((npc = L2World.findNpcByName(npcName)) != null)
				{
					teleportToCharacter(activeChar, npc);
					return true;
				}
				activeChar.sendMessage("Npc " + npcName + " not found");
				break;
			case admin_correct_merchants:
				Location[] list = new Location[] {
						new Location(82545, 148604, -3495),
						new Location(81930, 149193, -3495),
						new Location(81375, 149103, -3495),
						new Location(81290, 148618, -3495),
						new Location(81413, 148125, -3495),
						new Location(81923, 148013, -3495),
						new Location(82471, 148124, -3495),
						new Location(82477, 149107, -3495),
						new Location(83496, 148624, -3431),
						new Location(84300, 147409, -3431)
				};

				for(L2Player player : L2World.getAllPlayers())
					if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE && !activeChar.isInOfflineMode())
					{
						Location loc = list[Rnd.get(list.length)];
						loc = Rnd.coordsRandomize(loc.x, loc.y, loc.z, player.getHeading(), 100, 400);
						player.teleToLocation(loc);
					}

				for(L2Player player : L2World.getAllPlayers())
					if(player.isInOfflineMode())
					{
						Location loc = list[Rnd.get(list.length)];
						loc = Rnd.coordsRandomize(loc.x, loc.y, loc.z, player.getHeading(), 100, 400);
						player.decayMe();
						player.setXYZ(loc.x, loc.y, loc.z);
						player.spawnMe();
						System.out.println(player.getName());
					}
				break;
			case admin_correct_gh:
				L2Territory gh_spawn_loc = null;

				// Зона крафта
				gh_spawn_loc = new L2Territory(10000001);

				gh_spawn_loc.add(45704, 186617, -3480, -3380);
				gh_spawn_loc.add(46086, 186419, -3488, -3388);
				gh_spawn_loc.add(46733, 187506, -3480, -3380);
				gh_spawn_loc.add(46294, 187709, -3480, -3380);

				for(L2Player player : L2World.getAllPlayers())
					if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_MANUFACTURE && player.getReflection().getId() == -2)
					{
						int[] point = gh_spawn_loc.getRandomPoint();
						player.decayMe();
						player.setXYZ(point[0], point[1], point[2]);
						player.spawnMe();
					}

				// Зона покупки
				gh_spawn_loc = new L2Territory(10000002);

				gh_spawn_loc.add(46091, 186412, -3488, -3388);
				gh_spawn_loc.add(47218, 185902, -3488, -3388);
				gh_spawn_loc.add(47761, 186929, -3480, -3380);
				gh_spawn_loc.add(46742, 187511, -3480, -3380);

				for(L2Player player : L2World.getAllPlayers())
					if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_BUY && player.getReflection().getId() == -2)
					{
						int[] point = gh_spawn_loc.getRandomPoint();
						player.decayMe();
						player.setXYZ(point[0], point[1], point[2]);
						player.spawnMe();
					}

				// Зона продажи
				gh_spawn_loc = new L2Territory(10000003);

				gh_spawn_loc.add(47665, 186755, -3480, -3380);
				gh_spawn_loc.add(48167, 186488, -3480, -3380);
				gh_spawn_loc.add(48397, 186625, -3480, -3380);
				gh_spawn_loc.add(50156, 184674, -3488, -3388);
				gh_spawn_loc.add(49292, 183916, -3488, -3388);
				gh_spawn_loc.add(47758, 185654, -3488, -3388);
				gh_spawn_loc.add(47244, 185894, -3488, -3388);

				for(L2Player player : L2World.getAllPlayers())
					if((player.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL || player.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE) && player.getReflection().getId() == -2)
					{
						int[] point = gh_spawn_loc.getRandomPoint();
						player.decayMe();
						player.setXYZ(point[0], point[1], point[2]);
						player.spawnMe();
					}
				break;
			case admin_correct_gh2:
				L2Territory gh_spawn_loc2 = null;

				// Зона покупки
				gh_spawn_loc2 = new L2Territory(10000004);

				gh_spawn_loc2.add(46091, 186412, -3488, -3388);
				gh_spawn_loc2.add(47218, 185902, -3488, -3388);
				gh_spawn_loc2.add(47761, 186929, -3480, -3380);
				gh_spawn_loc2.add(46742, 187511, -3480, -3380);

				for(L2Player player : L2World.getAllPlayers())
					if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_BUY)
					{
						int[] point = gh_spawn_loc2.getRandomPoint();

						player.decayMe();

						if(player.getReflection().getId() != -2)
						{
							player.setVar("backCoords", player.getX() + " " + player.getY() + " " + player.getZ());
							player.setReflection(-2);
						}

						player.setXYZ(point[0], point[1], point[2]);
						player.spawnMe();
					}

				// Зона крафта
				gh_spawn_loc2 = new L2Territory(10000005);

				gh_spawn_loc2.add(45704, 186617, -3480, -3380);
				gh_spawn_loc2.add(46086, 186419, -3488, -3388);
				gh_spawn_loc2.add(46733, 187506, -3480, -3380);
				gh_spawn_loc2.add(46294, 187709, -3480, -3380);

				for(L2Player player : L2World.getAllPlayers())
					if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_MANUFACTURE)
					{
						int[] point = gh_spawn_loc2.getRandomPoint();
						player.decayMe();

						if(player.getReflection().getId() != -2)
						{
							player.setVar("backCoords", player.getX() + " " + player.getY() + " " + player.getZ());
							player.setReflection(-2);
						}

						player.setXYZ(point[0], point[1], point[2]);
						player.spawnMe();
					}

				// Зона продажи
				gh_spawn_loc2 = new L2Territory(10000006);

				gh_spawn_loc2.add(47665, 186755, -3480, -3380);
				gh_spawn_loc2.add(48167, 186488, -3480, -3380);
				gh_spawn_loc2.add(48397, 186625, -3480, -3380);
				gh_spawn_loc2.add(50156, 184674, -3488, -3388);
				gh_spawn_loc2.add(49292, 183916, -3488, -3388);
				gh_spawn_loc2.add(47758, 185654, -3488, -3388);
				gh_spawn_loc2.add(47244, 185894, -3488, -3388);

				for(L2Player player : L2World.getAllPlayers())
					if((player.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL || player.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE))
					{
						int[] point = gh_spawn_loc2.getRandomPoint();
						player.decayMe();

						if(player.getReflection().getId() != -2)
						{
							player.setVar("backCoords", player.getX() + " " + player.getY() + " " + player.getZ());
							player.setReflection(-2);
						}

						player.setXYZ(point[0], point[1], point[2]);
						player.spawnMe();
					}
				break;
			case admin_correct_tvt:
				for(L2Player player : L2World.getAllPlayers())
					if(player != null)
					{
						try
						{
							String var = player.getVar("TvT_backCoords");
							if(var == null || var.equals(""))
								continue;
							String[] coords = var.split(" ");
							if(coords.length != 4)
								continue;
							player.teleToLocation(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]), 0);
							player.unsetVar("TvT_backCoords");
						}
						catch(Exception e)
						{}
					}

				for(L2Player player : L2World.getAllPlayers())
					if(player != null)
					{
						try
						{
							String var = player.getVar("LastHero_backCoords");
							if(var == null || var.equals(""))
								continue;
							String[] coords = var.split(" ");
							if(coords.length != 4)
								continue;
							player.teleToLocation(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]), 0);
							player.unsetVar("LastHero_backCoords");
						}
						catch(Exception e)
						{}
					}
				break;
		}

		return true;
	}

	private void teleToTown(String name, L2Player player)
	{
		if(name.equalsIgnoreCase("Aden"))
			player.teleToLocation(147450, 27120, -2208);
		else if(name.equalsIgnoreCase("Giran"))
			player.teleToLocation(82698, 148638, -3473);
		else if(name.equalsIgnoreCase("Oren"))
			player.teleToLocation(82321, 55139, -1529);
		else if(name.equalsIgnoreCase("Dion"))
			player.teleToLocation(18748, 145437, -3132);
		else if(name.equalsIgnoreCase("Heine"))
			player.teleToLocation(111115, 219017, -3547);
		else if(name.equalsIgnoreCase("Gludio"))
			player.teleToLocation(-14225, 123540, -3121);
		else if(name.equalsIgnoreCase("Goddard"))
			player.teleToLocation(147725, -56517, -2780);
		else if(name.equalsIgnoreCase("Schuttgart"))
			player.teleToLocation(87360, -142585, -1340);
		else if(name.equalsIgnoreCase("Gludin"))
			player.teleToLocation(-83063, 150791, -3133);
		else if(name.equalsIgnoreCase("Hunter"))
			player.teleToLocation(116589,76268, -2734);
		else if(name.equalsIgnoreCase("Floran"))
			player.teleToLocation(17144, 170156, -3502);
		else if(name.equalsIgnoreCase("Rune"))
			player.teleToLocation(44070, -50243, -796);
		else if(name.equalsIgnoreCase("ElvenVillage"))
			player.teleToLocation(45873, 49288, -3064);
		else if(name.equalsIgnoreCase("DarkElvenVillage"))
			player.teleToLocation(12428, 16551, -4588);
		else if(name.equalsIgnoreCase("TalkingIslandVillage"))
			player.teleToLocation(-82687, 243157, -3734);
		else if(name.equalsIgnoreCase("DwarvenVillage"))
			player.teleToLocation(116551, -182493, -1525);
		else if(name.equalsIgnoreCase("OrcVillage"))
			player.teleToLocation(-44133, -113911, -244);
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void showTeleportWindow(L2Player activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		StringBuffer replyMSG = new StringBuffer("<html><title>Teleport Menu</title>");
		replyMSG.append("<body>");

		replyMSG.append("<br>");
		replyMSG.append("<center><table>");

		replyMSG.append("<tr><td><button value=\"  \" action=\"bypass -h admin_tele\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"North\" action=\"bypass -h admin_gonorth\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"Up\" action=\"bypass -h admin_goup\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"West\" action=\"bypass -h admin_gowest\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"  \" action=\"bypass -h admin_tele\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"East\" action=\"bypass -h admin_goeast\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"  \" action=\"bypass -h admin_tele\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"South\" action=\"bypass -h admin_gosouth\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"Down\" action=\"bypass -h admin_godown\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");

		replyMSG.append("</table></center>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void showTeleportCharWindow(L2Player activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2Player player = null;
		if(target.isPlayer())
			player = (L2Player) target;
		else
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return;
		}
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		StringBuffer replyMSG = new StringBuffer("<html><title>Teleport Character</title>");
		replyMSG.append("<body>");
		replyMSG.append("The character you will teleport is " + player.getName() + ".");
		replyMSG.append("<br>");

		replyMSG.append("Co-ordinate x");
		replyMSG.append("<edit var=\"char_cord_x\" width=110>");
		replyMSG.append("Co-ordinate y");
		replyMSG.append("<edit var=\"char_cord_y\" width=110>");
		replyMSG.append("Co-ordinate z");
		replyMSG.append("<edit var=\"char_cord_z\" width=110>");
		replyMSG.append("<button value=\"Teleport\" action=\"bypass -h admin_teleport_character $char_cord_x $char_cord_y $char_cord_z\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<button value=\"Teleport near you\" action=\"bypass -h admin_teleport_character " + activeChar.getX() + " " + activeChar.getY() + " " + activeChar.getZ() + "\" width=115 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void teleportTo(L2Player activeChar, L2Player target, String Cords)
	{
		try
		{
			teleportTo(activeChar, target, new Location(Cords));
		}
		catch(IllegalArgumentException e)
		{
			activeChar.sendMessage("You must define 3 coordinates required to teleport");
			return;
		}
	}

	private void teleportTo(L2Player activeChar, L2Player target, Location loc)
	{
		if(!target.equals(activeChar))
			target.sendMessage("Admin is teleporting you.");
			
		target.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		target.teleToLocation(loc);

		if(target.equals(activeChar))
		{
			activeChar.sendMessage("You have been teleported to " + loc);
			Log.add("teleported to " + loc, "gm_ext_actions", activeChar);
		}
		else
			Log.add("teleported player " + target + " to " + loc, "gm_ext_actions", activeChar);
	}

	private void teleportCharacter(L2Player activeChar, String Cords)
	{
		L2Object target = activeChar.getTarget();
		if(target == null || !target.isPlayer())
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return;
		}
		if(target.getObjectId() == activeChar.getObjectId())
		{
			activeChar.sendMessage("You cannot teleport yourself.");
			return;
		}
		teleportTo(activeChar, (L2Player) target, Cords);
	}

	private void teleportCharacter_offline(int obj_id, Location loc)
	{
		if(obj_id == 0)
			return;

		ThreadConnection con = null;
		FiltredPreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("UPDATE characters SET x=?,y=?,z=? WHERE obj_Id=? LIMIT 1");
			st.setInt(1, loc.x);
			st.setInt(2, loc.y);
			st.setInt(3, loc.z);
			st.setInt(4, obj_id);
			st.executeUpdate();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, st);
		}
	}

	private void teleportToCharacter(L2Player activeChar, L2Object target)
	{
		if(target == null)
			return;

		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		activeChar.teleToLocation(target.getLoc().changeZ(25));

		activeChar.sendMessage("You have teleported to " + target);
	}

	private void recallNPC(L2Player activeChar)
	{
		L2Object obj = activeChar.getTarget();
		if(obj != null && obj.isNpc())
		{
			L2NpcInstance target = (L2NpcInstance) obj;
			L2Spawn spawn = target.getSpawn();

			int monsterTemplate = target.getTemplate().npcId;

			L2NpcTemplate template1 = NpcTable.getTemplate(monsterTemplate);

			if(template1 == null)
			{
				activeChar.sendMessage("Incorrect monster template.");
				return;
			}

			int respawnTime = spawn.getRespawnDelay();

			target.deleteMe();
			spawn.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(spawn, true);

			try
			{
				// L2MonsterInstance mob = new L2MonsterInstance(monsterTemplate,
				// template1);

				spawn = new L2Spawn(template1);
				spawn.setLoc(activeChar.getLoc());
				spawn.setAmount(1);
				spawn.setRespawnDelay(respawnTime);
				SpawnTable.getInstance().addNewSpawn(spawn, true);
				spawn.init();

				activeChar.sendMessage("Created " + template1.name + " on " + target.getObjectId() + ".");

				Log.add("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") moved NPC" + target.getObjectId(), "gm_ext_actions", activeChar);
			}
			catch(Exception e)
			{
				activeChar.sendMessage("Target is not in game.");
			}

		}
		else
			activeChar.sendPacket(Msg.INVALID_TARGET);
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