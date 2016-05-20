package l2d.game.model;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import l2d.Config;
import l2d.game.instancemanager.CatacombSpawnManager;
import l2d.game.instancemanager.DayNightSpawnManager;
import l2d.game.instancemanager.RaidBossSpawnManager;
import l2d.game.instancemanager.ServerVariables;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.GArray;
import l2d.util.GCSArray;
import l2d.util.Location;

/**
 * @author Diamond
 * @Date: 15/5/2007
 * @Time: 10:06:34
 */
public class L2World
{
	private static final Logger _log = Logger.getLogger(L2World.class.getName());

	/** Map dimensions */
	public static final int MAP_MIN_X = -163840;
	public static final int MAP_MAX_X = 229375;
	public static final int MAP_MIN_Y = -262144;
	public static final int MAP_MAX_Y = 294911;
	public static final int MAP_MIN_Z = -32768;
	public static final int MAP_MAX_Z = 32767;

	public static final int WORLD_SIZE_X = L2World.MAP_MAX_X - L2World.MAP_MIN_X + 1 >> 15;
	public static final int WORLD_SIZE_Y = L2World.MAP_MAX_Y - L2World.MAP_MIN_Y + 1 >> 15;

	private static final int SHIFT_BY = Config.SHIFT_BY;
	private static final int SHIFT_BY_FOR_Z = Config.SHIFT_BY_FOR_Z;

	/** calculated offset used so top left region is 0,0 */
	private static final int OFFSET_X = Math.abs(MAP_MIN_X >> SHIFT_BY);
	private static final int OFFSET_Y = Math.abs(MAP_MIN_Y >> SHIFT_BY);
	private static final int OFFSET_Z = Math.abs(MAP_MIN_Z >> SHIFT_BY_FOR_Z);

	/** Размерность массива регионов */
	private static final int REGIONS_X = (MAP_MAX_X >> SHIFT_BY) + OFFSET_X;
	private static final int REGIONS_Y = (MAP_MAX_Y >> SHIFT_BY) + OFFSET_Y;
	private static final int REGIONS_Z = (MAP_MAX_Z >> SHIFT_BY_FOR_Z) + OFFSET_Z;

	public static final int LINEAR_TERRITORY_CELL_SIZE = Config.LINEAR_TERRITORY_CELL_SIZE;
	public static final int LINEAR_TERRITORY_CELLS = (MAP_MAX_Y - MAP_MIN_Y) / LINEAR_TERRITORY_CELL_SIZE + 1;

	@SuppressWarnings("unchecked")
	public static final GCSArray<L2Territory>[] _linearTerritories = new GCSArray[LINEAR_TERRITORY_CELLS];

	/* database statistics */
	private static long _insertItemCounter = 0;
	private static long _deleteItemCounter = 0;
	private static long _updateItemCounter = 0;
	private static long _lazyUpdateItem = 0;
	private static long _updatePlayerBase = 0;

	private static ConcurrentHashMap<Integer, L2Object> _allObjects = new ConcurrentHashMap<Integer, L2Object>();
	private static ConcurrentHashMap<Integer, L2Character> _allCharacters = new ConcurrentHashMap<Integer, L2Character>();
	private static ConcurrentHashMap<Integer, L2Player> _allPlayers = new ConcurrentHashMap<Integer, L2Player>();

	private static L2WorldRegion[][][] _worldRegions = new L2WorldRegion[REGIONS_X + 1][REGIONS_Y + 1][REGIONS_Z + 1];
	private static GCSArray<L2WorldRegion> _activeRegions = new GCSArray<L2WorldRegion>();

	private static int _taxSum;

	/**
	 * Выдает массив регионов, примыкающих к текущему, плюс текущий регион (куб 3х3х3)
	 * На входе - координаты региона (уже преобразованные)
	 */
	public static ArrayList<L2WorldRegion> getNeighbors(int x, int y, int z)
	{
		ArrayList<L2WorldRegion> neighbors = new ArrayList<L2WorldRegion>();
		for(int a = -1; a <= 1; a++)
			for(int b = -1; b <= 1; b++)
				for(int c = -1; c <= 1; c++)
					if(validRegion(x + a, y + b, z + c) && _worldRegions[x + a][y + b][z + c] != null)
						neighbors.add(_worldRegions[x + a][y + b][z + c]);
		return neighbors;
	}

	/**
	 * Выдает список соседей, с заданным разбросом по Z (параллилепипед 3х3хN)
	 * На входе - координаты обьекта (не региона)
	 */
	public static ArrayList<L2WorldRegion> getNeighbors(int x, int y, int z1, int z2)
	{
		ArrayList<L2WorldRegion> neighbors = new ArrayList<L2WorldRegion>();

		int _x = (x >> SHIFT_BY) + OFFSET_X;
		int _y = (y >> SHIFT_BY) + OFFSET_Y;
		int _z1 = (z1 >> SHIFT_BY_FOR_Z) + OFFSET_Z;
		int _z2 = (z2 >> SHIFT_BY_FOR_Z) + OFFSET_Z;

		for(int a = -1; a <= 1; a++)
			for(int b = -1; b <= 1; b++)
				for(int c = _z1; c <= _z2; c++)
					if(validRegion(_x + a, _y + b, c) && _worldRegions[_x + a][_y + b][c] != null)
						neighbors.add(_worldRegions[_x + a][_y + b][c]);

		return neighbors;
	}

	/**
	 * @param x координата
	 * @param y координата
	 * @param z координата
	 * @return Правильные ли координаты для региона
	 */
	private static boolean validRegion(int x, int y, int z)
	{
		return x >= 0 && x < REGIONS_X && y >= 0 && y < REGIONS_Y && z >= 0 && z < REGIONS_Z;
	}

	/**
	 * @param obj обьект для посика региона
	 * @return Регион, соответствующий координатам обьекта (не путать с L2Object.getCurrentRegion())
	 */
	public static L2WorldRegion getRegion(L2Object obj)
	{
		return getRegion(obj.getX(), obj.getY(), obj.getZ());
	}

	/**
	 * @param x координата
	 * @param y координата
	 * @param z координата
	 * @return Регион, соответствующий координатам
	 */
	public static L2WorldRegion getRegion(int x, int y, int z)
	{
		int _x = (x >> SHIFT_BY) + OFFSET_X;
		int _y = (y >> SHIFT_BY) + OFFSET_Y;
		int _z = (z >> SHIFT_BY_FOR_Z) + OFFSET_Z;
		if(validRegion(_x, _y, _z))
		{
			if(_worldRegions[_x][_y][_z] == null)
				_worldRegions[_x][_y][_z] = new L2WorldRegion(_x, _y, _z);
			return _worldRegions[_x][_y][_z];
		}
		return null;
	}

	public static void addActiveRegion(L2WorldRegion region)
	{
		_activeRegions.add(region);
	}

	public static void removeActiveRegion(L2WorldRegion region)
	{
		_activeRegions.remove(region);
	}

	public static GCSArray<L2WorldRegion> getActiveRegions()
	{
		return _activeRegions;
	}

	/**
	 * Добавляет обьект в _allObjects (а так же в _allCharacters и _allPlayers для частных случаев)
	 * Здесь находятся все обьекты, не только видимые.
	 * @param object обьект для добавления
	 */
	public static void addObject(L2Object object)
	{
		if(object == null)
			return;

		_allObjects.put(object.getObjectId(), object);

		if(object.isCharacter())
		{
			_allCharacters.put(object.getObjectId(), (L2Character) object);
			if(object.isPlayer())
				addPlayer(object);
		}
	}

	/**
	 * Удаляет обьект из _allObjects (а так же из _allCharacters и _allPlayers для частных случаев)
	 * Также делается попытка удалить игков из всех территорий.
	 * @param object Обьект для удаления
	 */
	public static void removeObject(L2Object object)
	{
		if(object == null)
			return;

		// Удаление обьекта из всех территорий, также очистка территорий у удаленного обьекта
		L2Territory[] territories = object.getTerritories();
		if(territories != null)
		{
			for(L2Territory t : territories)
				if(t != null)
					t.doLeave(object, false);
			object.clearTerritories();
		}

		_allObjects.remove(object.getObjectId());

		if(object.isCharacter())
		{
			_allCharacters.remove(object.getObjectId());
			if(object.isPlayer())
				removePlayer(object);
		}
	}

	/**
	 * @param oID objectId
	 * @return обьект по objectId или null
	 */
	public static L2Object findObject(int oID)
	{
		return _allObjects.get(oID);
	}

	/**
	 * Находит npc по npc-id
	 * @param npc_id id непися
	 * @return L2NpcInstance или null
	 */
	public static L2NpcInstance findNpcByNpcId(int npc_id)
	{
		L2NpcInstance result = null;
		for(L2Character cha : _allCharacters.values())
			if(cha.isNpc() && npc_id == cha.getNpcId())
			{
				if(!cha.isDead())
					return (L2NpcInstance) cha;
				result = (L2NpcInstance) cha;
			}
		return result;
	}

	/**
	 * Находит npc по имени
	 * @param name имя непися
	 * @return L2NpcInstance или null
	 */
	public static L2NpcInstance findNpcByName(String name)
	{
		L2NpcInstance result = null;
		for(L2Character cha : _allCharacters.values())
			if(cha.isNpc() && name.equalsIgnoreCase(cha.getName()))
			{
				if(!cha.isDead())
					return (L2NpcInstance) cha;
				result = (L2NpcInstance) cha;
			}
		return result;
	}

	public static L2NpcInstance findNpcByObjId(int objId)
	{
		L2Object obj = _allCharacters.get(objId);
		return obj != null && obj.isNpc() ? (L2NpcInstance) obj : null;
	}

	public static L2NpcInstance findNpcByObjId(int objId, int npc_id)
	{
		L2NpcInstance obj = findNpcByObjId(objId);
		return obj != null && obj.getNpcId() == npc_id ? obj : null;
	}

	/**
	 * @return _allObjects
	 */
	public static ConcurrentHashMap<Integer, L2Object> getAllObjects()
	{
		return _allObjects;
	}

	/**
	 * @return Число всех обьектов в мире
	 */
	public static Integer getAllObjectsCount()
	{
		return _allObjects.size();
	}

	public static void addPlayer(L2Object object)
	{
		if(_allPlayers.put(object.getObjectId(), (L2Player) object) == null)
			_online++;
	}

	public static void removePlayer(L2Object object)
	{
		if(_allPlayers.remove(object.getObjectId()) != null)
			_online--;
	}

	/**
	 * @return Список всех игроков в мире
	 */
	public static L2Player[] getAllPlayers()
	{
		return _allPlayers.values().toArray(new L2Player[0]);
	}

	/**
	 * @return Список всех персонажей в мире
	 */
	public static ArrayList<L2Character> getAllCharacters()
	{
		ArrayList<L2Character> chars = new ArrayList<L2Character>();
		for(L2Character cha : _allCharacters.values())
			chars.add(cha);
		return chars;
	}

	private static int _online = 0;

	/**
	 * @return Число игроков в мире
	 */
	public static int getAllPlayersCount()
	{
		return _online;
	}

	private static long _timestamp_offline = 0;
	private static int _offline = 0;

	/**
	 * @return количество ботов
	 */
	public static int getAllOfflineCount()
	{
		if(!Config.SERVICES_OFFLINE_TRADE_ALLOW)
			return 0;

		long ctime = System.currentTimeMillis();

		if(_timestamp_offline < ctime)
		{
			_timestamp_offline = ctime + 10000;
			_offline = 0;
			for(L2Player player : _allPlayers.values())
				if(player.isInOfflineMode())
					_offline++;
		}

		return _offline;
	}

	/**
	 * Находит игрока по имени
	 * Регистр символов любой.
	 * @param name имя
	 * @return найденый игрок или null если игрока нет
	 */
	public static L2Player getPlayer(String name)
	{
		for(L2Player player : _allPlayers.values())
			if(name.equalsIgnoreCase(player.getName()))
				return player;
		return null;
	}

	/**
	 * Находит игрока по ObjId
	 * @return найденый игрок или null если игрока нет
	 */
	public static L2Player getPlayer(int id)
	{
		return _allPlayers.get(id);
	}

	public static boolean containsPlayer(int id)
	{
		return _allPlayers.get(id) != null;
	}

	public static boolean containsPlayer(L2Player player)
	{
		if(player == null)
			return false;
		return _allPlayers.get(player.getObjectId()) != null;
	}

	/**
	 * Находит питомца по ObjId
	 * @return найденый игрок или null если игрока нет
	 */
	public static L2Summon getPet(int id)
	{
		L2Character result = _allCharacters.get(id);
		return result != null && (result.isPet() || result.isSummon()) ? (L2Summon) result : null;
	}

	/**
	 * Находит Character по ObjId
	 * @return найденый игрок или null если игрока нет
	 */
	public static L2Character getCharacter(int id)
	{
		return _allCharacters.get(id);
	}

	/**
	 * Проверяет, сменился ли регион в котором находится обьект
	 * Если сменился - удаляет обьект из старого региона и добавляет в новый.
	 * @param object обьект для проверки
	 * @param dropper - если это L2ItemInstance, то будет анимация дропа с перса
	 */
	public static void addVisibleObject(L2Object object, L2Character dropper)
	{
		if(object == null || !object.isVisible() || object.inObserverMode())
			return;

		if(object.isPet() || object.isSummon())
		{
			L2Player owner = object.getPlayer();
			if(owner != null && object.getReflection() != owner.getReflection())
				object.setReflection(owner.getReflection());
		}

		L2WorldRegion region = getRegion(object);
		L2WorldRegion currentRegion = object.getCurrentRegion();

		if(region == null || currentRegion != null && currentRegion.equals(region))
			return;

		if(currentRegion == null) // Новый обьект (пример - игрок вошел в мир, заспаунился моб, дропнули вещь)
		{
			// Показываем обьект в текущем и соседних регионах
			// Если обьект игрок, показываем ему все обьекты в текущем и соседних регионах
			for(L2WorldRegion neighbor : region.getNeighbors())
				neighbor.addToPlayers(object, dropper);

			// Добавляем обьект в список видимых
			region.addObject(object);
			object.setCurrentRegion(region);
		}
		else
		// Обьект уже существует, перешел из одного региона в другой
		{
			// Показываем обьект, но в отличие от первого случая - только для новых соседей.
			// Убираем обьект из старых соседей.
			ArrayList<L2WorldRegion> oldNeighbors = currentRegion.getNeighbors();
			ArrayList<L2WorldRegion> newNeighbors = region.getNeighbors();
			for(L2WorldRegion neighbor : oldNeighbors)
			{
				boolean flag = true;
				for(L2WorldRegion newneighbor : newNeighbors)
					if(newneighbor != null && newneighbor.equals(neighbor))
					{
						flag = false;
						break;
					}
				if(flag)
					neighbor.removeFromPlayers(object);
			}
			for(L2WorldRegion neighbor : newNeighbors)
			{
				boolean flag = true;
				for(L2WorldRegion oldneighbor : oldNeighbors)
					if(oldneighbor != null && oldneighbor.equals(neighbor))
					{
						flag = false;
						break;
					}
				if(flag)
					neighbor.addToPlayers(object, dropper);
			}
			// Добавляем обьект в список видимых
			region.addObject(object);
			object.setCurrentRegion(region);

			// Удаляем обьект из старого региона
			currentRegion.removeObject(object, true);
		}
	}

	/**
	 * Удаляет обьект из текущего региона
	 * @param object обьект для удаления
	 */
	public static void removeVisibleObject(L2Object object)
	{
		if(object == null || object.isVisible() || object.inObserverMode())
			return;
		if(object.getCurrentRegion() != null)
		{
			object.getCurrentRegion().removeObject(object, false);
			if(object.getCurrentRegion() != null)
				for(L2WorldRegion neighbor : object.getCurrentRegion().getNeighbors())
					neighbor.removeFromPlayers(object);
			object.setCurrentRegion(null);
		}
	}

	/**
	 * Проверяет координаты на корректность
	 * @param x координата x
	 * @param y координата y
	 * @return Корректные ли координаты
	 */
	public static boolean validCoords(int x, int y)
	{
		return x > MAP_MIN_X && x < MAP_MAX_X && y > MAP_MIN_Y && y < MAP_MAX_Y;
	}

	/**
	 * Удаляет весь спаун
	 */
	public static synchronized void deleteVisibleNpcSpawns()
	{
		RaidBossSpawnManager.getInstance().cleanUp();
		DayNightSpawnManager.getInstance().cleanUp();
		CatacombSpawnManager.getInstance().cleanUp();
		_log.info("Deleting all visible NPC's...");
		for(int i = 0; i < REGIONS_X; i++)
			for(int j = 0; j < REGIONS_Y; j++)
				for(int k = 0; k < REGIONS_Z; k++)
					if(_worldRegions[i][j][k] != null)
						_worldRegions[i][j][k].deleteVisibleNpcSpawns();
		_log.info("All visible NPC's deleted.");
	}

	public static L2Object getAroundObjectById(L2Object object, Integer id)
	{
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return null;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			for(L2Object o : region.getObjectsList(new GArray<L2Object>(), object.getObjectId(), object.getReflection()))
				if(o != null && o.getObjectId() == id)
					return o;
		return null;
	}

	public static GArray<L2Object> getAroundObjects(L2Object object)
	{
		int oid = object.getObjectId();
		GArray<L2Object> result = new GArray<L2Object>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			region.getObjectsList(result, oid, object.getReflection());
		return result;
	}

	public static GArray<L2Object> getAroundObjects(L2Object object, int radius, int height)
	{
		int oid = object.getObjectId();
		GArray<L2Object> result = new GArray<L2Object>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			region.getObjectsList(result, oid, object.getReflection(), object.getX(), object.getY(), object.getZ(), radius * radius, height);
		return result;
	}

	public static GArray<L2Character> getAroundCharacters(L2Object object)
	{
		int oid = object.getObjectId();
		GArray<L2Character> result = new GArray<L2Character>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			region.getCharactersList(result, oid, object.getReflection().getId());
		return result;
	}

	public static GArray<L2Character> getAroundCharacters(L2Object object, int radius, int height)
	{
		int oid = object.getObjectId();
		GArray<L2Character> result = new GArray<L2Character>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			region.getCharactersList(result, oid, object.getReflection().getId(), object.getX(), object.getY(), object.getZ(), radius * radius, height);
		return result;
	}

	public static GArray<L2NpcInstance> getAroundNpc(L2Object object)
	{
		int oid = object.getObjectId();
		GArray<L2NpcInstance> result = new GArray<L2NpcInstance>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			region.getNpcsList(result, oid, object.getReflection());
		return result;
	}

	public static GArray<L2NpcInstance> getAroundNpc(L2Object object, int radius, int height)
	{
		int oid = object.getObjectId();
		GArray<L2NpcInstance> result = new GArray<L2NpcInstance>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			region.getNpcsList(result, oid, object.getReflection(), object.getX(), object.getY(), object.getZ(), radius * radius, height);
		return result;
	}

	public static GArray<L2Playable> getAroundPlayables(L2Object object)
	{
		int oid = object.getObjectId();
		GArray<L2Playable> result = new GArray<L2Playable>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			region.getPlayablesList(result, oid, object.getReflection());
		return result;
	}

	public static GArray<L2Playable> getAroundPlayables(L2Object object, int radius, int height)
	{
		int oid = object.getObjectId();
		GArray<L2Playable> result = new GArray<L2Playable>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			region.getPlayablesList(result, oid, object.getReflection(), object.getX(), object.getY(), object.getZ(), radius * radius, height);
		return result;
	}

	public static GArray<L2Player> getAroundPlayers(L2Object object)
	{
		int oid = object.getObjectId();
		GArray<L2Player> result = new GArray<L2Player>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			region.getPlayersList(result, oid, object.getReflection());
		return result;
	}

	public static GArray<L2Player> getAroundPlayers(L2Object object, int radius, int height)
	{
		int oid = object.getObjectId();
		GArray<L2Player> result = new GArray<L2Player>();
		if(object.getCurrentRegion() == null || !object.getCurrentRegion().isActive())
			return result;
		for(L2WorldRegion region : object.getCurrentRegion().getNeighbors())
			region.getPlayersList(result, oid, object.getReflection(), object.getX(), object.getY(), object.getZ(), radius * radius, height);
		return result;
	}

	public static boolean isAroundPlayers(L2Object object)
	{
		return object.getCurrentRegion() != null && object.getCurrentRegion().isActive() && !object.getCurrentRegion().areNeighborsEmpty();
	}

	public static void addTerritory(L2Territory territory)
	{
		int pos_start = (territory.getYmin() - MAP_MIN_Y) / LINEAR_TERRITORY_CELL_SIZE;
		int pos_stop = (territory.getYmax() - MAP_MIN_Y) / LINEAR_TERRITORY_CELL_SIZE;
		if(pos_start < 0)
			pos_start = 0;
		if(pos_stop >= LINEAR_TERRITORY_CELLS)
			pos_stop = LINEAR_TERRITORY_CELLS - 1;
		int id = territory.getId();
		for(int pos = pos_start; pos <= pos_stop; pos++)
		{
			GCSArray<L2Territory> territories = _linearTerritories[pos];
			if(territories == null)
				_linearTerritories[pos] = territories = new GCSArray<L2Territory>();
			else
				for(L2Territory terr : territories.toArray(new L2Territory[territories.size()]))
					if(terr != null && terr.getId() == id)
						territories.remove(terr);
			territories.add(territory);
		}
	}

	public static void removeTerritory(L2Territory territory)
	{
		int pos_start = (territory.getYmin() - MAP_MIN_Y) / LINEAR_TERRITORY_CELL_SIZE;
		int pos_stop = (territory.getYmax() - MAP_MIN_Y) / LINEAR_TERRITORY_CELL_SIZE;
		if(pos_start < 0)
			pos_start = 0;
		if(pos_stop >= LINEAR_TERRITORY_CELLS)
			pos_stop = LINEAR_TERRITORY_CELLS - 1;
		int id = territory.getId();
		for(int pos = pos_start; pos <= pos_stop; pos++)
		{
			GCSArray<L2Territory> territories = _linearTerritories[pos];
			if(territories == null)
				_linearTerritories[pos] = territories = new GCSArray<L2Territory>();
			else
				for(L2Territory terr : territories.toArray(new L2Territory[territories.size()]))
					if(terr != null && terr.getId() == id)
						territories.remove(terr);
		}
	}

	/**
	 * Создает и возвращает список территорий для точек x и y
	 * @param x точка x
	 * @param y точка y
	 * @return список территорий в зависимости от параметра onlyActive
	 */
	public static GCSArray<L2Territory> getTerritory(int x, int y)
	{
		GCSArray<L2Territory> result = new GCSArray<L2Territory>();
		int pos = (y - MAP_MIN_Y) / LINEAR_TERRITORY_CELL_SIZE;
		if(pos < 0)
			pos = 0;
		if(pos >= LINEAR_TERRITORY_CELLS)
			pos = LINEAR_TERRITORY_CELLS - 1;
		GCSArray<L2Territory> territories = _linearTerritories[pos];
		if(territories != null)
			for(L2Territory terr : territories.toArray(new L2Territory[territories.size()]))
				if(terr != null && terr.isInside(x, y))
					result.add(terr);
		return result;
	}

	public static L2Territory getWater(L2Object obj)
	{
		return getWater(obj.getX(), obj.getY(), obj.getZ());
	}

	public static L2Territory getWater(Location loc)
	{
		return getWater(loc.x, loc.y, loc.z);
	}

	public static L2Territory getWater(int x, int y, int z)
	{
		for(L2Territory terr : getTerritory(x, y))
			if(terr != null && terr.getZone() != null && terr.getZone().getType() == ZoneType.water && terr.isInside(x, y, z))
				return terr;
		return null;
	}

	public static boolean isWater(L2Object obj)
	{
		return isWater(obj.getX(), obj.getY(), obj.getZ());
	}

	public static boolean isWater(Location loc)
	{
		return isWater(loc.x, loc.y, loc.z);
	}

	public static boolean isWater(int x, int y, int z)
	{
		return getWater(x, y, z) != null;
	}

	// database statistic methods
	// items
	public static void increaseInsertItemCount()
	{
		_insertItemCounter++;
	}

	public static long getInsertItemCount()
	{
		return _insertItemCounter;
	}

	public static void increaseDeleteItemCount()
	{
		_deleteItemCounter++;
	}

	public static long getDeleteItemCount()
	{
		return _deleteItemCounter;
	}

	public static void increaseUpdateItemCount()
	{
		_updateItemCounter++;
	}

	public static long getUpdateItemCount()
	{
		return _updateItemCounter;
	}

	public static void increaseLazyUpdateItem()
	{
		_lazyUpdateItem++;
	}

	public static long getLazyUpdateItem()
	{
		return _lazyUpdateItem;
	}

	// players
	public static void increaseUpdatePlayerBase()
	{
		_updatePlayerBase++;
	}

	public static long getUpdatePlayerBase()
	{
		return _updatePlayerBase;
	}

	public static void loadTaxSum()
	{
		_taxSum = ServerVariables.getInt("taxsum", 0);
	}

	public static void addTax(int sum)
	{
		_taxSum += sum;
		ServerVariables.set("taxsum", _taxSum);
	}

	public static int getTaxSum()
	{
		return _taxSum;
	}
}