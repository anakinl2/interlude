package l2d.game.instancemanager;

import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2d.game.idfactory.IdFactory;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2Territory;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.tables.DoorTable;
import l2d.game.tables.NpcTable;
import l2d.game.tables.TerritoryTable;
import l2d.game.templates.L2NpcTemplate;
import l2d.util.Location;
import l2d.util.parsers.AbstractDirParser;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Менеджер Инстанц-зон.
 * 
 * @author Felixx
 */
public class InstancedLocationManager extends AbstractDirParser
{
	private static InstancedLocationManager _instance;
	private int countGood = 0, countBad = 0, locSize = 0, roomSize = 0;
	private static FastMap<Integer, FastMap<Integer, InstancedLocation>> _locations = new FastMap<Integer, FastMap<Integer, InstancedLocation>>();
	private static Logger _log = Logger.getLogger(InstancedLocationManager.class.getName());

	public static InstancedLocationManager getInstance()
	{
		if(_instance == null)
			_instance = new InstancedLocationManager();
		return _instance;
	}

	InstancedLocationManager()
	{
		super("./data/instances", "template.xml");
		super.parse();

		locSize = _locations.keySet().size();

		for(Integer b : _locations.keySet())
			roomSize += _locations.get(b).keySet().size();

		_log.info("[ Instanced Location Manager ]");
		_log.info(" ~ Loaded: " + locSize + " locations with " + roomSize + " rooms.");
		_log.info(" ~ Loaded: " + countGood + " instanced location spawns. ");
		_log.info(" ~ Loaded: " + countBad + " spawn errors.");
		_log.info("[ Instanced Location Manager ]\n");
	}

	public FastMap<Integer, InstancedLocation> getById(Integer id)
	{
		return _locations.get(id);
	}

	public void reload()
	{
		if(_instance != null)
		{
			for(Integer b : _locations.keySet())
				_locations.get(b).clear();
			_locations.clear();

			new InstancedLocationManager();
		}
	}

	public static class InstancedLocation
	{
		private final L2Territory _territory;
		private final int _minLevel;
		private final int _maxLevel;
		private final int _minParty;
		private final int _maxParty;
		private final int _live;
		private final int _reuse;
		private final boolean _summon;
		private final Location _teleportCoords;
		private final Location _returnCoords;
		private final boolean _isBossRoom;
		private final FastList<L2Spawn> _roomSpawns;
		private final FastList<L2DoorInstance> _roomDoors;

		public InstancedLocation(int minLevel, int maxLevel, int minParty, int maxParty, L2Territory territory, int xT, int yT, int zT, int xRet, int yRet, int zRet, boolean isBossRoom, int live, int reuse, boolean summon)
		{
			_minLevel = minLevel;
			_maxLevel = maxLevel;
			_territory = territory;
			_teleportCoords = new Location(xT, yT, zT);
			_returnCoords = new Location(xRet, yRet, zRet);
			_isBossRoom = isBossRoom;
			_roomSpawns = new FastList<L2Spawn>();
			_roomDoors = new FastList<L2DoorInstance>();
			_minParty = minParty;
			_maxParty = maxParty;
			_live = live;
			_reuse = reuse;
			_summon = summon;
		}

		/**
		 * @return
		 *         Можно ли призывать саммонов?
		 */
		public boolean canUseSummon()
		{
			return _summon;
		}

		/**
		 * @return
		 *         Возвращает минимальный уровень чара для входа в ИнстантЗону.
		 */
		public int getMinLevel()
		{
			return _minLevel;
		}

		/**
		 * @return
		 *         Возвращает максимальный уровень чара для входа в ИнстантЗону.
		 */
		public int getMaxLevel()
		{
			return _maxLevel;
		}

		/**
		 * @return
		 *         Возвращает минимальное количество народа в Группе.
		 */
		public int getMinParty()
		{
			return _minParty;
		}

		/**
		 * @return
		 *         Возвращает максимальное количество народа в Группе.
		 */
		public int getMaxParty()
		{
			return _maxParty;
		}

		/**
		 * @return
		 *         Возвращает время, жижни комнаты в милисекундах.
		 */
		public int getRoomLiveTime()
		{
			return _live * 60000;
		}

		/**
		 * @return
		 *         Возвращает время, повторного входа в инстант в милисекундах.
		 */
		public int getRoomReuseDelay()
		{
			return _reuse * 60000;
		}

		/**
		 * @return
		 *         Возвращает координаты, для входа в ИнстантЗону.
		 */
		public Location getTeleportCoords()
		{
			return _teleportCoords;
		}

		/**
		 * @return
		 *         Возвращает координаты, для возврата из ИнстантЗоны.
		 */
		public Location getReturnCoords()
		{
			return _returnCoords;
		}

		/**
		 * @return
		 *         true если зона совподает.
		 */
		public boolean checkIfInZone(Location loc)
		{
			return checkIfInZone(loc.x, loc.y, loc.z);
		}

		/**
		 * @return
		 *         true если зона совподает.
		 */
		public boolean checkIfInZone(int x, int y, int z)
		{
			return _territory.isInside(x, y, z);
		}

		public boolean isBossRoom()
		{
			return _isBossRoom;
		}

		/**
		 * @return
		 *         Возвращает список спавна мобов в ИнстантЗоне.
		 */
		public FastList<L2Spawn> getSpawns()
		{
			return _roomSpawns;
		}

		/**
		 * @return
		 *         Возвращает список дверей принадлежащих к ИнстантЗоне.
		 */
		public FastList<L2DoorInstance> getDoors()
		{
			return _roomDoors;
		}
	}

	/**
	 * Парсит папку и ее подпапки с *.xml инстанцев.
	 */
	@Override
	protected void readData(Node node)
	{
		NamedNodeMap attrs;
		Integer instanceId;
		Integer roomId;
		int mobId, doorId, respawn, count;
		L2Spawn spawnDat;
		L2NpcTemplate template;
		L2DoorInstance door;
		int minLevel = 0, maxLevel = 0, minParty = 1, maxParty = 9, xMin = 0, xMax = 0, yMin = 0, yMax = 0, zMin = 0, zMax = 0, xT = 0, yT = 0, zT = 0, xRet = 0, yRet = 0, zRet = 0, xM = 0, yM = 0, zM = 0, hM = 0, live = 30, reuse = 1440;
		boolean isBossRoom, summon;

		if("list".equalsIgnoreCase(node.getNodeName()))
			for(Node area = node.getFirstChild(); area != null; area = area.getNextSibling())
				if("instance".equalsIgnoreCase(area.getNodeName()))
				{
					attrs = area.getAttributes();
					instanceId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());

					for(Node room = area.getFirstChild(); room != null; room = room.getNextSibling())
						if("location".equalsIgnoreCase(room.getNodeName()))
						{
							attrs = room.getAttributes();
							roomId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							summon = Boolean.parseBoolean(attrs.getNamedItem("summon").getNodeValue());
							Node boss = attrs.getNamedItem("isBossRoom");
							isBossRoom = boss != null ? Boolean.parseBoolean(boss.getNodeValue()) : false;

							for(Node coord = room.getFirstChild(); coord != null; coord = coord.getNextSibling())
								if("level".equalsIgnoreCase(coord.getNodeName()))
								{
									attrs = coord.getAttributes();
									minLevel = Integer.parseInt(attrs.getNamedItem("min").getNodeValue());
									maxLevel = Integer.parseInt(attrs.getNamedItem("max").getNodeValue());
								}
								else if("party".equalsIgnoreCase(coord.getNodeName()))
								{
									attrs = coord.getAttributes();
									minParty = Integer.parseInt(attrs.getNamedItem("min").getNodeValue());
									maxParty = Integer.parseInt(attrs.getNamedItem("max").getNodeValue());
								}
								else if("teleport".equalsIgnoreCase(coord.getNodeName()))
								{
									attrs = coord.getAttributes();
									xT = Integer.parseInt(attrs.getNamedItem("x").getNodeValue());
									yT = Integer.parseInt(attrs.getNamedItem("y").getNodeValue());
									zT = Integer.parseInt(attrs.getNamedItem("z").getNodeValue());
								}
								else if("return".equalsIgnoreCase(coord.getNodeName()))
								{
									attrs = coord.getAttributes();
									xRet = Integer.parseInt(attrs.getNamedItem("x").getNodeValue());
									yRet = Integer.parseInt(attrs.getNamedItem("y").getNodeValue());
									zRet = Integer.parseInt(attrs.getNamedItem("z").getNodeValue());
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
								else if("time".equalsIgnoreCase(coord.getNodeName()))
								{
									attrs = coord.getAttributes();
									live = Integer.parseInt(attrs.getNamedItem("live").getNodeValue());
									reuse = Integer.parseInt(attrs.getNamedItem("reuse").getNodeValue());
								}

							int loc_id = IdFactory.getInstance().getNextId();
							L2Territory territory = new L2Territory(loc_id);
							territory.add(xMin, yMin, zMin, zMax);
							territory.add(xMax, yMin, zMin, zMax);
							territory.add(xMax, yMax, zMin, zMax);
							territory.add(xMin, yMax, zMin, zMax);
							TerritoryTable.getInstance().getLocations().put(loc_id, territory);
							L2World.addTerritory(territory);

							if(!_locations.containsKey(instanceId))
								_locations.put(instanceId, new FastMap<Integer, InstancedLocation>());

							_locations.get(instanceId).put(roomId, new InstancedLocation(minLevel, maxLevel, minParty, maxParty, territory, xT, yT, zT, xRet, yRet, zRet, isBossRoom, live, reuse, summon));

							for(Node spawn = room.getFirstChild(); spawn != null; spawn = spawn.getNextSibling())
								if("spawn".equalsIgnoreCase(spawn.getNodeName()))
								{
									attrs = spawn.getAttributes();
									mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());
									respawn = Integer.parseInt(attrs.getNamedItem("respawn").getNodeValue());
									count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
									xM = Integer.parseInt(attrs.getNamedItem("x").getNodeValue());
									yM = Integer.parseInt(attrs.getNamedItem("y").getNodeValue());
									zM = Integer.parseInt(attrs.getNamedItem("z").getNodeValue());
									hM = Integer.parseInt(attrs.getNamedItem("heading").getNodeValue());

									template = NpcTable.getTemplate(mobId);
									if(template == null)
										_log.warning("Template " + mobId + " not found!");

									if(template != null && _locations.containsKey(instanceId) && _locations.get(instanceId).containsKey(roomId))
										try
										{
											spawnDat = new L2Spawn(template);

											if(xM == 0 && yM == 0 && zM == 0)
											{
												spawnDat.setLocation(loc_id);
												spawnDat.setHeading(-1);
												spawnDat.setAmount(count);
											}
											else
											{
												Location loc = new Location(xM, yM, zM, hM);
												spawnDat.setLoc(loc);
												spawnDat.setAmount(1);
											}

											spawnDat.setRespawnDelay(respawn);

											if(respawn > 0)
												spawnDat.startRespawn();
											_locations.get(instanceId).get(roomId).getSpawns().add(spawnDat);
											countGood++;
										}
										catch(SecurityException e)
										{
											e.printStackTrace();
										}
										catch(ClassNotFoundException e)
										{
											e.printStackTrace();
										}
									else
										countBad++;
								}

							for(Node doors = room.getFirstChild(); doors != null; doors = doors.getNextSibling())
								if("door".equalsIgnoreCase(doors.getNodeName()))
								{
									attrs = doors.getAttributes();
									doorId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
									door = DoorTable.getInstance().getDoor(doorId);
									if(door == null)
										_log.warning("Door " + doorId + " not found!");
									if(door != null && _locations.containsKey(instanceId) && _locations.get(instanceId).containsKey(roomId))
										_locations.get(instanceId).get(roomId).getDoors().add(door);
								}
						}
				}
	}
}