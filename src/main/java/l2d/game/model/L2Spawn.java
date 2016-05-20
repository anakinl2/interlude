package l2d.game.model;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import l2d.Config;
import l2d.game.idfactory.IdFactory;
import l2d.game.model.instances.L2MinionInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.instances.L2PetInstance;
import l2d.game.tables.TerritoryTable;
import l2d.game.taskmanager.SpawnTaskManager;
import l2d.game.templates.L2NpcTemplate;
import l2d.util.Location;
import l2d.util.Rnd;

/**
 * This class manages the spawn and respawn of a group of L2NpcInstance that are in the same are and have the same type.
 * <B><U> Concept</U> :</B><BR><BR>
 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
 * The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR><BR>
 */
@SuppressWarnings({ "nls", "unqualified-field-access", "boxing" })
public class L2Spawn implements Cloneable
{
	private static Logger _log = Logger.getLogger(L2Spawn.class.getName());

	/** Минимальное время респа */
	private static final int MIN_RESPAWN_DELAY = 30;

	private final L2SpawnGroup _group;
	/** The link on the L2NpcTemplate object containing generic and static properties of this spawn (ex : RewardExp, RewardSP, AggroRange...) */
	private L2NpcTemplate _template;

	/** The Identifier of this spawn in the spawn table */
	private int _id;

	/** Position of the spawn point */
	private int _locx, _locy, _locz, _heading, _location;

	/** The maximum number of L2NpcInstance that can manage this L2Spawn */
	private int _maximumCount;

	/** То количество что установлено в базе (текущий максимум может изменяться) */
	private int _referenceCount;

	/** The current number of L2NpcInstance managed by this L2Spawn */
	private int _currentCount;

	/** The current number of SpawnTask in progress or stand by of this L2Spawn */
	private int _scheduledCount;

	/** The delay between a L2NpcInstance remove and its re-spawn */
	private int _respawnDelay, _respawnDelayRandom;

	/** Время респауна, unixtime в секундах */
	private int _respawnTime;

	/** The generic constructor of L2NpcInstance managed by this L2Spawn */
	private Constructor<?> _constructor;

	/** If True a L2NpcInstance is respawned each time that another is killed */
	boolean _doRespawn;

	public boolean isDoRespawn()
	{
		return _doRespawn;
	}

	private L2NpcInstance _lastSpawn;
	private static final List<SpawnListener> _spawnListeners = new ArrayList<SpawnListener>();

	private HashSet<L2NpcInstance> _spawned;

	private int _siegeId;

	private int _reflection;

	public int getReflection()
	{
		return _reflection;
	}

	public void setReflection(final int reflection)
	{
		_reflection = reflection;
	}

	public void decreaseScheduledCount()
	{
		if(_scheduledCount > 0)
			_scheduledCount--;
	}

	/**
	 * Constructor of L2Spawn.<BR><BR>
	 * <B><U> Concept</U> :</B><BR><BR>
	 * Each L2Spawn owns generic and static properties (ex : RewardExp, RewardSP, AggroRange...).
	 * All of those properties are stored in a different L2NpcTemplate for each type of L2Spawn.
	 * Each template is loaded once in the server cache memory (reduce memory use).
	 * When a new instance of L2Spawn is created, server just create a link between the instance and the template.
	 * This link is stored in <B>_template</B><BR><BR>
	 * Each L2NpcInstance is linked to a L2Spawn that manages its spawn and respawn (delay, location...).
	 * This link is stored in <B>_spawn</B> of the L2NpcInstance<BR><BR>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the _template of the L2Spawn </li>
	 * <li>Calculate the implementationName used to generate the generic constructor of L2NpcInstance managed by this L2Spawn</li>
	 * <li>Create the generic constructor of L2NpcInstance managed by this L2Spawn</li><BR><BR>
	 * 
	 * @param mobTemplate
	 *            The L2NpcTemplate to link to this L2Spawn
	 */
	public L2Spawn(final L2NpcTemplate mobTemplate, final L2SpawnGroup group) throws SecurityException, ClassNotFoundException
	{
		// Set the _template of the L2Spawn
		_template = mobTemplate;
		_group = group;

		_constructor = _template.getInstanceConstructor();

		if(_constructor == null)
			throw new ClassNotFoundException();

		_spawned = new HashSet<L2NpcInstance>(1);
	}

	public L2Spawn(final L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException
	{
		this(mobTemplate, null);
	}

	public void setConstructor(final Constructor<?> constr)
	{
		_constructor = constr;
	}

	/**
	 * Return the maximum number of L2NpcInstance that this L2Spawn can manage.<BR><BR>
	 */
	public int getAmount()
	{
		return _maximumCount;
	}

	/**
	 * Return the number of L2NpcInstance that this L2Spawn spawned.<BR><BR>
	 */
	public int getSpawnedCount()
	{
		return _currentCount;
	}

	/**
	 * Return the number of L2NpcInstance that this L2Spawn sheduled.<BR><BR>
	 */
	public int getSheduledCount()
	{
		return _scheduledCount;
	}

	/**
	 * Return the Identifier of this L2Spwan (used as key in the SpawnTable).<BR><BR>
	 */
	public int getId()
	{
		return _id;
	}

	/**
	 * Return the Identifier of the location area where L2NpcInstance can be spwaned.<BR><BR>
	 */
	public int getLocation()
	{
		return _location;
	}

	/**
	 * Return the position of the spawn point.<BR><BR>
	 */
	public Location getLoc()
	{
		return new Location(_locx, _locy, _locz);
	}

	/**
	 * Return the X position of the spawn point.<BR><BR>
	 */
	public int getLocx()
	{
		return _locx;
	}

	/**
	 * Return the Y position of the spawn point.<BR><BR>
	 */
	public int getLocy()
	{
		return _locy;
	}

	/**
	 * Return the Z position of the spawn point.<BR><BR>
	 */
	public int getLocz()
	{
		return _locz;
	}

	/**
	 * Return the Identifier of the L2NpcInstance manage by this L2Spwan contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getNpcId()
	{
		return _template.npcId;
	}

	/**
	 * Return the heading of L2NpcInstance when they are spawned.<BR><BR>
	 */
	public int getHeading()
	{
		return _heading;
	}

	/**
	 * Return the delay between a L2NpcInstance remove and its re-spawn.<BR><BR>
	 */
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}

	public int getRespawnDelayWithRnd()
	{
		return _respawnDelayRandom == 0 ? _respawnDelay : Rnd.get(_respawnDelay - _respawnDelayRandom, _respawnDelay + _respawnDelayRandom);
	}

	public int getRespawnTime()
	{
		return _respawnTime;
	}

	/**
	 * Set the maximum number of L2NpcInstance that this L2Spawn can manage.<BR><BR>
	 */
	public void setAmount(final int amount)
	{
		if(_referenceCount == 0)
			_referenceCount = amount;
		_maximumCount = amount;
	}

	/**
	 * Восстанавливает измененное количество
	 */
	public void restoreAmount()
	{
		_maximumCount = _referenceCount;
	}

	/**
	 * Set the Identifier of this L2Spwan (used as key in the SpawnTable).<BR><BR>
	 */
	public void setId(final int id)
	{
		_id = id;
	}

	/**
	 * Set the Identifier of the location area where L2NpcInstance can be spawned.<BR><BR>
	 */
	public void setLocation(final int location)
	{
		_location = location;
	}

	/**
	 * Set the position(x, y, z, heading) of the spawn point.
	 * 
	 * @param loc
	 *            Location
	 */
	public void setLoc(final Location loc)
	{
		_locx = loc.x;
		_locy = loc.y;
		_locz = loc.z;
		_heading = loc.h;
	}

	/**
	 * Set the X position of the spawn point.<BR><BR>
	 */
	public void setLocx(final int locx)
	{
		_locx = locx;
	}

	/**
	 * Set the Y position of the spawn point.<BR><BR>
	 */
	public void setLocy(final int locy)
	{
		_locy = locy;
	}

	/**
	 * Set the Z position of the spawn point.<BR><BR>
	 */
	public void setLocz(final int locz)
	{
		_locz = locz;
	}

	/**
	 * Set the heading of L2NpcInstance when they are spawned.<BR><BR>
	 */
	public void setHeading(final int heading)
	{
		_heading = heading;
	}

	/**
	 * Decrease the current number of L2NpcInstance of this L2Spawn and if necessary create a SpawnTask to launch after the respawn Delay.<BR><BR>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Decrease the current number of L2NpcInstance of this L2Spawn </li>
	 * <li>Check if respawn is possible to prevent multiple respawning caused by lag </li>
	 * <li>Update the current number of SpawnTask in progress or stand by of this L2Spawn </li>
	 * <li>Create a new SpawnTask to launch after the respawn Delay </li><BR><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : A respawn is possible ONLY if _doRespawn=True and _scheduledCount + _currentCount < _maximumCount</B></FONT><BR><BR>
	 */
	public void decreaseCount(final L2NpcInstance oldNpc)
	{
		// Decrease the current number of L2NpcInstance of this L2Spawn
		_currentCount--;

		if(_currentCount < 0)
			_currentCount = 0;

		if(_group != null)
			_group.OnNpcDeleted(this, oldNpc);

		// Check if respawn is possible to prevent multiple respawning caused by lag
		if(_doRespawn && _scheduledCount + _currentCount < _maximumCount)
		{
			// Update the current number of SpawnTask in progress or stand by of this L2Spawn
			_scheduledCount++;

			// Create a new SpawnTask to launch after the respawn Delay
			SpawnTaskManager.getInstance().addSpawnTask(oldNpc, Math.max(1000, _respawnDelay * 1000L - oldNpc.getDeadTime()));
		}
	}

	/**
	 * Create the initial spawning and set _doRespawn to True.<BR><BR>
	 * 
	 * @return The number of L2NpcInstance that were spawned
	 */
	public int init()
	{
		while(_currentCount + _scheduledCount < _maximumCount)
			doSpawn(false);

		_doRespawn = true;

		return _currentCount;
	}

	/**
	 * Create a L2NpcInstance in this L2Spawn.<BR><BR>
	 */
	public L2NpcInstance spawnOne()
	{
		return doSpawn(false);
	}

	public void despawnAll()
	{
		stopRespawn();
		for(final L2NpcInstance npc : getAllSpawned())
			if(npc != null)
				npc.deleteMe();
		_currentCount = 0;
	}

	/**
	 * Set _doRespawn to False to stop respawn in this L2Spawn.<BR><BR>
	 */
	public void stopRespawn()
	{
		_doRespawn = false;
	}

	/**
	 * Set _doRespawn to True to start or restart respawn in this L2Spawn.<BR><BR>
	 */
	public void startRespawn()
	{
		_doRespawn = true;
	}

	/**
	 * Create the L2NpcInstance, add it to the world and lauch its onSpawn action.<BR><BR>
	 * <B><U> Concept</U> :</B><BR><BR>
	 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
	 * The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR><BR>
	 * <B><U> Actions for an random spawn into location area</U> : <I>(if Locx=0 and Locy=0)</I></B><BR><BR>
	 * <li>Get L2NpcInstance Init parameters and its generate an Identifier </li>
	 * <li>Call the constructor of the L2NpcInstance </li>
	 * <li>Calculate the random position in the location area (if Locx=0 and Locy=0) or get its exact position from the L2Spawn </li>
	 * <li>Set the position of the L2NpcInstance </li>
	 * <li>Set the HP and MP of the L2NpcInstance to the max </li>
	 * <li>Set the heading of the L2NpcInstance (random heading if not defined : value=-1) </li>
	 * <li>Link the L2NpcInstance to this L2Spawn </li>
	 * <li>Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world </li>
	 * <li>Lauch the action onSpawn fo the L2NpcInstance </li><BR><BR>
	 * <li>Increase the current number of L2NpcInstance managed by this L2Spawn </li><BR><BR>
	 */
	public L2NpcInstance doSpawn(boolean spawn)
	{
		try
		{
			// Check if the L2Spawn is not a L2Pet or L2Minion spawn
			if(_template.isInstanceOf(L2PetInstance.class) || _template.isInstanceOf(L2MinionInstance.class))
			{
				_currentCount++;
				return null;
			}

			// Get L2NpcInstance Init parameters and its generate an Identifier
			final Object[] parameters = { IdFactory.getInstance().getNextId(), _template };

			// Call the constructor of the L2NpcInstance
			// (can be a L2ArtefactInstance, L2FriendlyMobInstance, L2GuardInstance, L2MonsterInstance, L2SiegeGuardInstance, L2BoxInstance or L2NpcInstance)
			final Object tmp = _constructor.newInstance(parameters);

			// Check if the Instance is a L2NpcInstance
			if(!(tmp instanceof L2NpcInstance))
				return null;

			if(!spawn)
				spawn = _respawnTime <= System.currentTimeMillis() / 1000 + MIN_RESPAWN_DELAY;

			_spawned.add((L2NpcInstance) tmp);

			return intializeNpc((L2NpcInstance) tmp, spawn);
		}
		catch(final Exception e)
		{
			_log.log(Level.WARNING, "NPC " + _template.npcId + " class not found", e);
		}

		return null;
	}

	public HashSet<L2NpcInstance> getAllSpawned()
	{
		return _spawned;
	}

	private L2NpcInstance intializeNpc(final L2NpcInstance mob, final boolean spawn)
	{
		Location newLoc;
		int newHeading;

		// If Locx=0 and Locy=0, the L2NpcInstance must be spawned in an area defined by location
		if(getLocation() != 0)
		{
			// Calculate the random position in the location area
			final int p[] = TerritoryTable.getInstance().getRandomPoint(getLocation());

			// Set the calculated position of the L2NpcInstance
			newLoc = new Location(p[0], p[1], p[2]);
			newHeading = Rnd.get(0xFFFF);
		}
		else
		{
			// The L2NpcInstance is spawned at the exact position (Lox, Locy, Locz)
			newLoc = getLoc();

			// random heading if not defined
			newHeading = getHeading() == -1 ? Rnd.get(0xFFFF) : getHeading();
		}

		// Set the HP and MP of the L2NpcInstance to the max
		mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp(), true);

		mob.getEffectList().stopAllEffects();

		// Link the L2NpcInstance to this L2Spawn
		mob.setSpawn(this);

		// Set the heading of the L2NpcInstance (random heading if not defined)
		mob.setHeading(newHeading);

		// save spawned points
		mob.setSpawnedLoc(newLoc);

		// Спавнится в указанном отражении
		mob.setReflection(getReflection());

		if(spawn)
		{
			// Launch the action onSpawn for the L2NpcInstance
			mob.onSpawn();

			// Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world as a visible object
			mob.spawnMe(newLoc);

			L2Spawn.notifyNpcSpawned(mob);

			// Increase the current number of L2NpcInstance managed by this L2Spawn
			_currentCount++;

			if(_group != null)
				_group.OnNpcCreated(this, mob);
		}
		else
		{
			mob.setXYZInvisible(newLoc.x, newLoc.y, newLoc.z);

			// Update the current number of SpawnTask in progress or stand by of this L2Spawn
			_scheduledCount++;

			// Create a new SpawnTask to launch after the respawn Delay
			SpawnTaskManager.getInstance().addSpawnTask(mob, _respawnTime * 1000L - System.currentTimeMillis());
		}

		_lastSpawn = mob;

		if(Config.DEBUG)
			_log.finest("spawned Mob ID: " + _template.npcId + " ,at: " + mob.getX() + " x, " + mob.getY() + " y, " + mob.getZ() + " z");

		return mob;
	}

	public static void addSpawnListener(final SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.add(listener);
		}
	}

	public static void removeSpawnListener(final SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.remove(listener);
		}
	}

	public static void notifyNpcSpawned(final L2NpcInstance npc)
	{
		synchronized (_spawnListeners)
		{
			for(final SpawnListener listener : _spawnListeners)
				listener.npcSpawned(npc);
		}
	}

	/**
	 * @param respawnDelay
	 *            delay in seconds
	 */
	public void setRespawnDelay(final int respawnDelay, final int respawnDelayRandom)
	{
		if(respawnDelay < 0)
			_log.warning("respawn delay is negative for spawnId:" + _id);

		_respawnDelay = respawnDelay > MIN_RESPAWN_DELAY ? respawnDelay : MIN_RESPAWN_DELAY;
		_respawnDelayRandom = respawnDelayRandom > 0 ? respawnDelayRandom : 0;
	}

	public void setRespawnDelay(final int respawnDelay)
	{
		setRespawnDelay(respawnDelay, 0);
	}

	/**
	 * Устанавливает время следующего респауна
	 * 
	 * @param respawnTime
	 *            в unixtime
	 */
	public void setRespawnTime(final int respawnTime)
	{
		_respawnTime = respawnTime;
	}

	public L2NpcInstance getLastSpawn()
	{
		return _lastSpawn;
	}

	/**
	 * @param oldNpc
	 */
	public void respawnNpc(final L2NpcInstance oldNpc)
	{
		oldNpc.refreshID();
		intializeNpc(oldNpc, true);
	}

	public void setSiegeId(final int id)
	{
		_siegeId = id;
	}

	public int getSiegeId()
	{
		return _siegeId;
	}

	@Override
	public L2Spawn clone()
	{
		L2Spawn spawnDat = null;
		try
		{
			spawnDat = new L2Spawn(_template);
			spawnDat.setLocation(_location);
			spawnDat.setLocx(_locx);
			spawnDat.setLocy(_locy);
			spawnDat.setLocz(_locz);
			spawnDat.setHeading(_heading);
			spawnDat.setAmount(_maximumCount);
			spawnDat.setRespawnDelay(_respawnDelay, _respawnDelayRandom);
		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}
		return spawnDat;
	}
}