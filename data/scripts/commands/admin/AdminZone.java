package commands.admin;

import javolution.util.FastList;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.idfactory.IdFactory;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2Territory;
import l2d.game.model.L2World;
import l2d.game.model.L2WorldRegion;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.tables.ReflectionTable;
import l2d.game.tables.SpawnTable;
import l2d.game.tables.TerritoryTable;
import com.lineage.util.GArray;

public class AdminZone implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_zone_check,
		admin_region,
		admin_active_region,
		admin_loc,
		admin_pos,
		admin_showloc,
		admin_location,
		admin_loc_begin,
		admin_loc_add,
		admin_loc_reset,
		admin_loc_end,
		admin_loc_remove,
		admin_setref
	}

	private static FastList<int[]> create_loc;
	private static int create_loc_id;

	private static void locationMenu(L2Player activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		StringBuffer replyMSG = new StringBuffer("<html><body><title>Location Create</title>");

		replyMSG.append("<center><table width=260><tr>");
		replyMSG.append("<td width=70>Location:</td>");
		replyMSG.append("<td width=50><edit var=\"loc\" width=50 height=12></td>");
		replyMSG.append("<td width=50><button value=\"Show\" action=\"bypass -h admin_showloc $loc\" width=50 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=90><button value=\"New Location\" action=\"bypass -h admin_loc_begin $loc\" width=90 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><br></center>");

		if(create_loc != null)
		{
			replyMSG.append("<center><table width=260><tr>");
			replyMSG.append("<td width=80><button value=\"Add Point\" action=\"bypass -h admin_loc_add menu\" width=80 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=90><button value=\"Reset Points\" action=\"bypass -h admin_loc_reset menu\" width=90 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=90><button value=\"End Location\" action=\"bypass -h admin_loc_end menu\" width=90 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table></center>");

			replyMSG.append("<center><button value=\"Show\" action=\"bypass -h admin_loc_showloc " + create_loc_id + " menu\" width=80 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");

			replyMSG.append("<br><br>");

			int i = 0;
			for(int[] loc : create_loc)
			{
				replyMSG.append("<button value=\"Remove\" action=\"bypass -h admin_loc_remove " + i + "\" width=80 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
				replyMSG.append("&nbsp;&nbsp;(" + loc[0] + ", " + loc[1] + ", " + loc[2] + ")<br1>");
				i++;
			}
		}

		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

/*	private static ExShowTrace Points2Trace(FastList<int[]> _points, int _step, boolean auto_compleate)
	{
		ExShowTrace result = new ExShowTrace();

		int[] _prev = null;
		int[] _first = null;
		for(int[] p : _points)
		{
			if(_first == null)
				_first = p;

			if(_prev != null)
				result.addLine(_prev[0], _prev[1], _prev[2], p[0], p[1], p[2], _step, 60000);

			_prev = p;
		}

		if(_prev == null || _first == null)
			return result;

		if(auto_compleate)
			result.addLine(_prev[0], _prev[1], _prev[2], _first[0], _first[1], _first[2], _step, 60000);

		return result;
	}*/

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(activeChar == null || !activeChar.getPlayerAccess().CanTeleport)
			return false;

		switch(command)
		{
			case admin_zone_check:
			{
				activeChar.sendMessage("===== Active Territories =====");
				if(activeChar.getTerritories() != null)
					for(L2Territory terr : activeChar.getTerritories())
					{
						activeChar.sendMessage("Territory: " + terr.getId());
						if(terr.getZone() != null)
							activeChar.sendMessage("Zone: " + terr.getZone().getType().toString() + ", id: " + terr.getZone().getId() + ", state: " + (terr.getZone().isActive() ? "active" : "not active"));
					}
				activeChar.sendMessage("======= Mob Spawns =======");
				for(L2Spawn spawn : SpawnTable.getInstance().getSpawnTable().values())
				{
					int location = spawn.getLocation();
					if(location == 0)
						continue;
					L2Territory terr = TerritoryTable.getInstance().getLocation(location);

					if(terr == null)
						continue;

					if(terr.isInside(activeChar.getX(), activeChar.getY()))
						activeChar.sendMessage("Territory: " + terr.getId());
				}
				break;
			}
			case admin_region:
			{
				activeChar.sendMessage("Current region: " + activeChar.getCurrentRegion().getName());
				activeChar.sendMessage("Objects list:");
				for(L2Object o : activeChar.getCurrentRegion().getObjectsList(new GArray<L2Object>(), 0, activeChar.getReflection()))
					if(o != null)
						activeChar.sendMessage(o.toString());
				break;
			}
			case admin_active_region:
			{
				activeChar.sendMessage("Active regions size: " + L2World.getActiveRegions().size());
				activeChar.sendMessage("Active objects list:");
				for(L2WorldRegion region : L2World.getActiveRegions())
					for(L2Character o : region.getCharactersList(new GArray<L2Character>(), 0, activeChar.getReflection().getId()))
						activeChar.sendMessage(o.toString());
				break;
			}
				/*
				* Пишет в консоль текущую точку для локации, оформляем в виде SQL запроса
				* пример: (8699,'loc_8699',111104,-112528,-1400,-1200),
				* Удобно для рисования локаций под спавн, разброс z +100/-10
				* необязательные параметры: id локации и название локации
				* Бросает бутылку, чтобы не запутаццо :)
				*/
			case admin_loc:
			{
				String loc_id = "0";
				String loc_name;
				if(wordList.length > 1)
					loc_id = wordList[1];
				if(wordList.length > 2)
					loc_name = wordList[2];
				else
					loc_name = "loc_" + loc_id;
				System.out.println("(" + loc_id + ",'" + loc_name + "'," + activeChar.getX() + "," + activeChar.getY() + "," + activeChar.getZ() + "," + (activeChar.getZ() + 100) + "),");
				activeChar.sendMessage("Point saved.");
				L2ItemInstance temp = new L2ItemInstance(IdFactory.getInstance().getNextId(), 1060);
				temp.dropMe(activeChar, activeChar.getLoc());
				break;
			}
			case admin_pos:
				String pos = activeChar.getX() + ", " + activeChar.getY() + ", " + activeChar.getZ() + ", " + activeChar.getHeading() + " Geo [" + (activeChar.getX() - L2World.MAP_MIN_X >> 4) + ", " + (activeChar.getY() - L2World.MAP_MIN_Y >> 4) + "]";
				System.out.println(activeChar.getName() + "'s position: " + pos);
				activeChar.sendMessage("Pos: " + pos);
				break;
			case admin_location:
				locationMenu(activeChar);
				break;
			case admin_loc_begin:
			{
				if(wordList.length < 2)
				{
					activeChar.sendMessage("Usage: //loc_begin <location_id>");
					locationMenu(activeChar);
					return false;
				}
				try
				{
					create_loc_id = Integer.valueOf(wordList[1]);
				}
				catch(Exception E)
				{
					activeChar.sendMessage("location_id should be integer");
					create_loc = null;
					locationMenu(activeChar);
					return false;
				}

				create_loc = new FastList<int[]>();
				create_loc.add(new int[] { activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getZ() + 100 });
				activeChar.sendMessage("Now you can add points...");
				//activeChar.sendPacket(new ExShowTrace());
				locationMenu(activeChar);
				break;
			}
			case admin_loc_add:
			{
				if(create_loc == null)
				{
					activeChar.sendMessage("Location not started");
					locationMenu(activeChar);
					return false;
				}

				create_loc.add(new int[] { activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getZ() + 100 });

			//	if(create_loc.size() > 1)
				//	activeChar.sendPacket(Points2Trace(create_loc, 50, false));
				if(wordList.length > 1 && wordList[1].equals("menu"))
					locationMenu(activeChar);
				break;
			}
			case admin_loc_reset:
			{
				if(create_loc == null)
				{
					activeChar.sendMessage("Location not started");
					locationMenu(activeChar);
					return false;
				}

				create_loc.clear();
				//activeChar.sendPacket(new ExShowTrace());
				locationMenu(activeChar);
				break;
			}
			case admin_loc_end:
			{
				if(create_loc == null)
				{
					activeChar.sendMessage("Location not started");
					locationMenu(activeChar);
					return false;
				}
				if(create_loc.size() < 3)
				{
					activeChar.sendMessage("Minimum location size 3 points");
					locationMenu(activeChar);
					return false;
				}

				String prefix = "(" + create_loc_id + ",'loc_" + create_loc_id + "',";
				for(int[] _p : create_loc)
					System.out.println(prefix + _p[0] + "," + _p[1] + "," + _p[2] + "," + _p[3] + "),");
				System.out.println("");

				//activeChar.sendPacket(Points2Trace(create_loc, 50, true));
				create_loc = null;
				create_loc_id = 0;
				activeChar.sendMessage("Location Created, check stdout");
				if(wordList.length > 1 && wordList[1].equals("menu"))
					locationMenu(activeChar);
				break;
			}
			case admin_showloc:
			{
				if(wordList.length < 2)
				{
					activeChar.sendMessage("Usage: //showloc <location>");
					return false;
				}

				String loc_id = wordList[1];
				L2Territory terr = TerritoryTable.getInstance().getLocations().get(loc_id);
				if(terr == null)
				{
					activeChar.sendMessage("Location <" + loc_id + "> undefined.");
					return false;
				}
				if(!terr.isInside(activeChar.getX(), activeChar.getY()))
				{
					int[] _loc = terr.getRandomPoint();
					activeChar.teleToLocation(_loc[0], _loc[1], _loc[2]);
				}
				//activeChar.sendPacket(Points2Trace(terr.getCoords(), 50, true));

				if(wordList.length > 2 && wordList[2].equals("menu"))
					locationMenu(activeChar);
				break;
			}
			case admin_loc_remove:
			{
				if(wordList.length < 2)
				{
					activeChar.sendMessage("Usage: //showloc <location>");
					return false;
				}

				if(create_loc == null)
				{
					activeChar.sendMessage("Location not started");
					locationMenu(activeChar);
					return false;
				}

				int point_id = Integer.parseInt(wordList[1]);

				create_loc.remove(point_id);

			//if(create_loc.size() > 1)
			//		activeChar.sendPacket(Points2Trace(create_loc, 50, false));

				locationMenu(activeChar);
				break;
			}
			case admin_setref:
			{
				if(wordList.length < 2)
				{
					activeChar.sendMessage("Usage: //set_ref <reflection>");
					return false;
				}

				int ref_id = Integer.parseInt(wordList[1]);
				if(ref_id != 0 && ReflectionTable.getInstance().get(ref_id) == null)
				{
					activeChar.sendMessage("Reflection <" + ref_id + "> not found.");
					return false;
				}

				L2Object target = activeChar;
				L2Object obj = activeChar.getTarget();
				if(obj != null)
					target = obj;

				target.setReflection(ref_id);
				target.decayMe();
				target.spawnMe();
				break;
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