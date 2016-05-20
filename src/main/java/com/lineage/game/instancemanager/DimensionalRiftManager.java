package com.lineage.game.instancemanager;

import java.util.logging.Logger;

import com.lineage.game.idfactory.IdFactory;
import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.L2Territory;
import com.lineage.game.model.L2World;
import com.lineage.game.model.Reflection;
import com.lineage.game.model.entity.DimensionalRift;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.serverpackets.TeleportToLocation;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.tables.ReflectionTable;
import com.lineage.game.tables.TerritoryTable;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Location;
import com.lineage.util.Rnd;
import com.lineage.util.parsers.AbstractFileParser;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DimensionalRiftManager extends AbstractFileParser
{
	private static Logger _log = Logger.getLogger(DimensionalRiftManager.class.getName());
	private static DimensionalRiftManager _instance;

	private int countGood = 0, countBad = 0, typeSize = 0, roomSize = 0;
	private FastMap<Integer, FastMap<Integer, DimensionalRiftRoom>> _rooms = new FastMap<Integer, FastMap<Integer, DimensionalRiftRoom>>();
	private final static int DIMENSIONAL_FRAGMENT_ITEM_ID = 7079;

	public static DimensionalRiftManager getInstance()
	{
		if(_instance == null)
			_instance = new DimensionalRiftManager();

		return _instance;
	}

	public DimensionalRiftManager()
	{
		super("./data/dimensionalRift.xml");
		super.parse();

		typeSize = _rooms.keySet().size();

		for(int b : _rooms.keySet())
			roomSize += _rooms.get(b).keySet().size();

		_log.info("[ Dimensional Rift Manager ]");
		_log.info(" ~ Loaded: " + typeSize + " room types with " + roomSize + " rooms.");
		_log.info(" ~ Loaded: " + countGood + " dimensional rift spawns. ");
		_log.info(" ~ Loaded: " + countBad + " spawn errors.");
		_log.info("[ Dimensional Rift Manager ]\n");
	}

	public DimensionalRiftRoom getRoom(int type, int room)
	{
		return _rooms.get(type).get(room);
	}

	public void reload()
	{
		if(_instance != null)
		{
			for(int b : _rooms.keySet())
				_rooms.get(b).clear();
			_rooms.clear();

			new DimensionalRiftManager();
		}
	}

	public boolean checkIfInRiftZone(Location loc, boolean ignorePeaceZone)
	{
		if(ignorePeaceZone)
			return _rooms.get(0).get(1).checkIfInZone(loc);
		return _rooms.get(0).get(1).checkIfInZone(loc) && !_rooms.get(0).get(0).checkIfInZone(loc);
	}

	public boolean checkIfInPeaceZone(Location loc)
	{
		return _rooms.get(0).get(0).checkIfInZone(loc);
	}

	public void teleportToWaitingRoom(L2Player player)
	{
		teleToLocation(player, getRoom(0, 0).getTeleportCoords(), null);
	}

	public void start(L2Player player, int type, L2NpcInstance npc)
	{
		if(!player.isInParty())
		{
			showHtmlFile(player, "data/html/rift/NoParty.htm", npc);
			return;
		}

		if(!player.isGM())
		{
			if(player.getParty().getPartyLeaderOID() != player.getObjectId())
			{
				showHtmlFile(player, "data/html/rift/NotPartyLeader.htm", npc);
				return;
			}

			if(player.getParty().isInDimensionalRift())
			{
				showHtmlFile(player, "data/html/rift/Cheater.htm", npc);

				if(!player.isGM())
					_log.warning("Player " + player.getName() + "(" + player.getObjectId() + ") was cheating in dimension rift area!");

				return;
			}

			if(player.getParty().getMemberCount() < Config.RIFT_MIN_PARTY_SIZE)
			{
				showHtmlFile(player, "data/html/rift/SmallParty.htm", npc);
				return;
			}

			for(L2Player p : player.getParty().getPartyMembers())
				if(!checkIfInPeaceZone(p.getLoc()))
				{
					showHtmlFile(player, "data/html/rift/NotInWaitingRoom.htm", npc);
					return;
				}

			L2ItemInstance i;
			for(L2Player p : player.getParty().getPartyMembers())
			{
				i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);
				if(i == null || i.getCount() < getNeededItems(type))
				{
					showHtmlFile(player, "data/html/rift/NoFragments.htm", npc);
					return;
				}
			}

			for(L2Player p : player.getParty().getPartyMembers())
				p.getInventory().destroyItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID, getNeededItems(type), true);
		}

		new DimensionalRift(player.getParty(), type, Rnd.get(1, 9));
	}

	public void killRift(DimensionalRift d)
	{
		d.collapse();
		if(d.getTeleportTimerTask() != null)
			d.getTeleportTimerTask().cancel();
		d.setTeleportTimerTask(null);

		if(d.getTeleportTimer() != null)
			d.getTeleportTimer().cancel();
		d.setTeleportTimer(null);

		if(d.getSpawnTimerTask() != null)
			d.getSpawnTimerTask().cancel();
		d.setSpawnTimerTask(null);

		if(d.getSpawnTimer() != null)
			d.getSpawnTimer().cancel();
		d.setSpawnTimer(null);

		if(d.getKillRiftTimerTask() != null)
			d.getKillRiftTimerTask().cancel();
		d.setKillRiftTimerTask(null);

		if(d.getKillRiftTimer() != null)
			d.getKillRiftTimer().cancel();
		d.setKillRiftTimer(null);
	}

	public class DimensionalRiftRoom
	{
		private final L2Territory _territory;
		private final Location _teleportCoords;
		private final boolean _isBossRoom;
		private final FastList<L2Spawn> _roomSpawns;

		public DimensionalRiftRoom(L2Territory territory, int xT, int yT, int zT, boolean isBossRoom)
		{
			_territory = territory;
			_teleportCoords = new Location(xT, yT, zT);
			_isBossRoom = isBossRoom;
			_roomSpawns = new FastList<L2Spawn>();
		}

		public Location getTeleportCoords()
		{
			return _teleportCoords;
		}

		public boolean checkIfInZone(Location loc)
		{
			return checkIfInZone(loc.x, loc.y, loc.z);
		}

		public boolean checkIfInZone(int x, int y, int z)
		{
			return _territory.isInside(x, y, z);
		}

		public boolean isBossRoom()
		{
			return _isBossRoom;
		}

		public FastList<L2Spawn> getSpawns()
		{
			return _roomSpawns;
		}
	}

	private int getNeededItems(int type)
	{
		switch(type)
		{
			case 1:
				return Config.RIFT_ENTER_COST_RECRUIT;
			case 2:
				return Config.RIFT_ENTER_COST_SOLDIER;
			case 3:
				return Config.RIFT_ENTER_COST_OFFICER;
			case 4:
				return Config.RIFT_ENTER_COST_CAPTAIN;
			case 5:
				return Config.RIFT_ENTER_COST_COMMANDER;
			case 6:
				return Config.RIFT_ENTER_COST_HERO;
			default:
				return 999999;
		}
	}

	public void showHtmlFile(L2Player player, String file, L2NpcInstance npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player, npc);
		html.setFile(file);
		html.replace("%t_name%", npc.getName());
		player.sendPacket(html);
	}

	public static void teleToLocation(L2Player player, Location loc, Reflection ref)
	{
		teleToLocation(player, loc.x, loc.y, loc.z, ref);
	}

	public static void teleToLocation(L2Player player, int x, int y, int z, Reflection ref)
	{
		player.setTarget(null);

		if(player.isInBoat())
			player.setBoat(null);

		player.decayMe();

		player.setXYZInvisible(x, y, z);
		player.setIsTeleporting(true);
		if(ref == null)
			ref = ReflectionTable.getInstance().getDefault();
		player.setReflection(ref);

		// Нужно при телепорте с более высокой точки на более низкую, иначе наносится вред от "падения"
		player.setLastClientPosition(null);
		player.setLastServerPosition(null);
		player.sendPacket(new TeleportToLocation(player, x, y, z));
	}

	@Override
	protected void readData(Node rift)
	{
		try
		{
			NamedNodeMap attrs;
			int type;
			int roomId;
			int mobId, delay, count;
			L2Spawn spawnDat;
			L2NpcTemplate template;
			int xMin = 0, xMax = 0, yMin = 0, yMax = 0, zMin = 0, zMax = 0, xT = 0, yT = 0, zT = 0;
			boolean isBossRoom;

			if("rift".equalsIgnoreCase(rift.getNodeName()))
				for(Node area = rift.getFirstChild(); area != null; area = area.getNextSibling())
					if("area".equalsIgnoreCase(area.getNodeName()))
					{
						attrs = area.getAttributes();
						type = Integer.parseInt(attrs.getNamedItem("type").getNodeValue());

						for(Node room = area.getFirstChild(); room != null; room = room.getNextSibling())
							if("room".equalsIgnoreCase(room.getNodeName()))
							{
								attrs = room.getAttributes();
								roomId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
								Node boss = attrs.getNamedItem("isBossRoom");
								isBossRoom = boss != null ? Boolean.parseBoolean(boss.getNodeValue()) : false;

								for(Node coord = room.getFirstChild(); coord != null; coord = coord.getNextSibling())
									if("teleport".equalsIgnoreCase(coord.getNodeName()))
									{
										attrs = coord.getAttributes();
										xT = Integer.parseInt(attrs.getNamedItem("x").getNodeValue());
										yT = Integer.parseInt(attrs.getNamedItem("y").getNodeValue());
										zT = Integer.parseInt(attrs.getNamedItem("z").getNodeValue());
									}
									else if("zone".equalsIgnoreCase(coord.getNodeName()))
									{
										attrs = coord.getAttributes();
										xMin = Integer.parseInt(attrs.getNamedItem("xMin").getNodeValue());
										xMax = Integer.parseInt(attrs.getNamedItem("xMax").getNodeValue());
										yMin = Integer.parseInt(attrs.getNamedItem("yMin").getNodeValue());
										yMax = Integer.parseInt(attrs.getNamedItem("yMax").getNodeValue());
										zMin = Integer.parseInt(attrs.getNamedItem("zMin").getNodeValue());
										zMax = Integer.parseInt(attrs.getNamedItem("zMax").getNodeValue());
									}

								int loc_id = IdFactory.getInstance().getNextId();
								L2Territory territory = new L2Territory(loc_id);
								territory.add(xMin, yMin, zMin, zMax);
								territory.add(xMax, yMin, zMin, zMax);
								territory.add(xMax, yMax, zMin, zMax);
								territory.add(xMin, yMax, zMin, zMax);
								TerritoryTable.getInstance().getLocations().put(loc_id, territory);
								L2World.addTerritory(territory);

								if(!_rooms.containsKey(type))
									_rooms.put(type, new FastMap<Integer, DimensionalRiftRoom>());

								_rooms.get(type).put(roomId, new DimensionalRiftRoom(territory, xT, yT, zT, isBossRoom));

								for(Node spawn = room.getFirstChild(); spawn != null; spawn = spawn.getNextSibling())
									if("spawn".equalsIgnoreCase(spawn.getNodeName()))
									{
										attrs = spawn.getAttributes();
										mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());
										delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
										count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());

										template = NpcTable.getTemplate(mobId);
										if(template == null)
											_log.warning("Template " + mobId + " not found!");
										if(!_rooms.containsKey(type))
											_log.warning("Type " + type + " not found!");
										else if(!_rooms.get(type).containsKey(roomId))
											_log.warning("Room " + roomId + " in Type " + type + " not found!");

										if(template != null && _rooms.containsKey(type) && _rooms.get(type).containsKey(roomId))
										{
											spawnDat = new L2Spawn(template);
											spawnDat.setLocation(loc_id);
											spawnDat.setHeading(-1);
											spawnDat.setRespawnDelay(delay);
											spawnDat.setAmount(count);
											_rooms.get(type).get(roomId).getSpawns().add(spawnDat);
											countGood++;
										}
										else
											countBad++;
									}
							}
					}
		}
		catch(Exception e)
		{
			_log.warning(" ~ ERROR: on loading dimensional rift spawns: " + e);
		}
	}
}