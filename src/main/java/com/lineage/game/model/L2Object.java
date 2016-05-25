package com.lineage.game.model;

import java.util.logging.Logger;

import com.lineage.ext.listeners.MethodCollection;
import com.lineage.ext.listeners.MethodInvokeListener;
import com.lineage.ext.listeners.PropertyChangeListener;
import com.lineage.ext.listeners.PropertyCollection;
import com.lineage.ext.listeners.engine.DefaultListenerEngine;
import com.lineage.ext.listeners.engine.ListenerEngine;
import com.lineage.ext.listeners.events.MethodEvent;
import com.lineage.ext.listeners.events.PropertyEvent;
import com.lineage.ext.listeners.events.L2Object.TerritoryChangeEvent;
import com.lineage.ext.scripts.Events;
import com.lineage.game.ai.L2CharacterAI;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.instancemanager.MercTicketManager;
import com.lineage.game.instancemanager.QuestManager;
import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.game.model.instances.L2BossInstance;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2MonsterInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.model.instances.L2PetInstance;
import com.lineage.game.model.instances.L2RaidBossInstance;
import com.lineage.game.model.instances.L2ReflectionBossInstance;
import com.lineage.game.model.instances.L2SummonInstance;
import com.lineage.game.model.quest.Quest;
import com.lineage.game.tables.ReflectionTable;
import com.lineage.game.tables.TerritoryTable;
import com.lineage.util.GCSArray;
import com.lineage.util.Location;

public abstract class L2Object
{
	public static final int POLY_NONE = 0;
	public static final int POLY_NPC = 1;
	public static final int POLY_ITEM = 2;
	private static final Logger _log = Logger.getLogger(L2Object.class.getName());

	L2WorldRegion _currentRegion;

	/** Object identifier */
	protected int _objectId;

	private int _reflection = 0;

	/** Object location : Used for items/chars that are seen in the world */
	private int _x;
	private int _y;
	private int _z;

	private int _poly_id;

	private GCSArray<L2Territory> _territories = null;

	/** Object visibility */
	private boolean _hidden;

	// --------------------------- Listeners system test -----------------------------
	private DefaultListenerEngine<L2Object> listenerEngine;

	/**
	 * Constructor of L2Object.<BR><BR>
	 * @param objectId этого объекта
	 */
	public L2Object(Integer objectId)
	{
		_objectId = objectId;
	}

	private ListenerEngine<L2Object> getListenerEngine()
	{
		if(listenerEngine == null)
			listenerEngine = new DefaultListenerEngine<L2Object>(this);
		return listenerEngine;
	}

	// ------------------------- Listeners system test end ---------------------------

	@Override
	protected void finalize()
	{
		getReflection().removeObject(_objectId);
	}

	/**
	 * Return the identifier of the L2Object.<BR><BR>
	 *
	 * @ - deprecated?
	 */
	@Override
	public final int hashCode()
	{
		return _objectId;
	}

	public void addMethodInvokeListener(MethodInvokeListener listener)
	{
		getListenerEngine().addMethodInvokedListener(listener);
	}

	public void addMethodInvokeListener(MethodCollection methodName, MethodInvokeListener listener)
	{
		getListenerEngine().addMethodInvokedListener(methodName, listener);
	}

	public void addProperty(PropertyCollection property, Object value)
	{
		getListenerEngine().addProperty(property, value);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		getListenerEngine().addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(PropertyCollection value, PropertyChangeListener listener)
	{
		getListenerEngine().addPropertyChangeListener(value, listener);
	}

	public void clearTerritories()
	{
		_territories = null;
	}

	/**
	 * Init a dropped L2ItemInstance and add it in the world as a visible object.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion </li>
	 * <li>Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion</li>
	 * <li>Add the L2ItemInstance dropped in the world as a <B>visible</B> object</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects of L2World </B></FONT><BR><BR>
	 *
	 * <B><U> Assert </U> :</B><BR><BR>
	 * <li> this instanceof L2ItemInstance</li>
	 * <li> _worldRegion == null <I>(L2Object is invisible at the beginning)</I></li><BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Drop item</li>
	 * <li> Call Pet</li><BR>
	 *
	 * @param dropper Char that dropped item
	 * @param loc drop coordinates
	 */
	public void dropMe(L2Character dropper, Location loc)
	{
		if(dropper != null)
			setReflection(dropper.getReflection());

		// Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion
		_hidden = false;

		_x = loc.x;
		_y = loc.y;
		_z = getGeoZ(loc);

		L2World.addVisibleObject(this, dropper);
	}

	public void setReflection(Reflection i)
	{
		if(getReflection() == i)
			return;

		boolean blink = false;
		if(!_hidden)
		{
			decayMe();
			blink = true;
		}

		getReflection().removeObject(_objectId);
		_reflection = i.getId();
		getReflection().addObject(this);

		if(blink)
			spawnMe();
	}

	/**
	 * Удаляет обьект из текущего региона, делая его невидимым.
	 */
	public void decayMe()
	{
		_hidden = true;
		L2World.removeVisibleObject(this);
	}

	/**
	 * Добавляет обьект в мир, добавляет в текущий регион. Делает обьект видимым.
	 */
	public void spawnMe()
	{
		// Set the x,y,z position of the L2Object spawn and update its _worldregion
		_hidden = false;

		// Add the L2Oject spawn in the _allobjects of L2World
		L2World.addObject(this);
		L2World.addVisibleObject(this, null);
		updateTerritories();
	}

	public void updateTerritories()
	{
		GCSArray<L2Territory> current_territories = L2World.getTerritory(getX(), getY());
		GCSArray<L2Territory> new_territories = new GCSArray<L2Territory>();
		GCSArray<L2Territory> old_territories = new GCSArray<L2Territory>();

		if(_territories == null)
			new_territories = current_territories;
		else
		{
			for(L2Territory terr : current_territories)
				if(!_territories.contains(terr))
					new_territories.add(terr);

			if(_territories.size() > 0)
				for(L2Territory terr : getTerritories())
					if(!current_territories.contains(terr))
						old_territories.add(terr);
		}

		if(current_territories.size() > 0)
			_territories = current_territories;
		else
			_territories = null;

		for(L2Territory terr : old_territories)
			if(terr != null)
				terr.doLeave(this, true);

		for(L2Territory terr : new_territories)
			if(terr != null)
				terr.doEnter(this);

		firePropertyChanged(new TerritoryChangeEvent(old_territories, new_territories, this));
	}

	private void firePropertyChanged(PropertyEvent event)
	{
		getListenerEngine().firePropertyChanged(event);
	}

	public Reflection getReflection()
	{
		Reflection result = ReflectionTable.getInstance().get(_reflection);
		if(result == null)
		{
			result = ReflectionTable.getInstance().getDefault();
			_reflection = result.getId();
		}
		return result;
	}

	private int getGeoZ(Location loc)
	{
		if(isPlayer())
		{
			L2Player activechar = (L2Player) this;
			if(activechar.isFlying() || activechar.isInWater() || activechar.isInBoat())
				return loc.z;
			return GeoEngine.getHeight(loc);
		}
		else if(isNpc())
		{
			L2Spawn spawn = ((L2NpcInstance) this).getSpawn();
			if(spawn != null && spawn.getLocx() == 0 && spawn.getLocy() == 0)
				return GeoEngine.getHeight(loc);
			return loc.z;
		}
		else if(this instanceof L2BoatInstance || this instanceof L2DoorInstance)
			return loc.z;
		return GeoEngine.getHeight(loc);
	}

	public boolean isPlayer()
	{
		return this instanceof L2Player;
	}

	public boolean isInWater()
	{
		return isPlayer() && ((L2Player) this).getWaterTask() != null;
	}

	public boolean isNpc()
	{
		return this instanceof L2NpcInstance;
	}

	public void fireMethodInvoked(MethodEvent event)
	{
		getListenerEngine().fireMethodInvoked(event);
	}

	void fireMethodInvoked(MethodCollection methodName, Object[] args)
	{
		getListenerEngine().fireMethodInvoked(methodName, this, args);
	}

	void firePropertyChanged(PropertyCollection value, Object oldValue, Object newValue)
	{
		getListenerEngine().firePropertyChanged(value, this, oldValue, newValue);
	}

	public L2CharacterAI getAI()
	{
		return null;
	}

	public float getColHeight()
	{
		_log.warning("getColHeight called directly from L2Object");
		Thread.dumpStack();
		return 0;
	}

	public float getColRadius()
	{
		_log.warning("getColRadius called directly from L2Object");
		Thread.dumpStack();
		return 0;
	}

	public L2WorldRegion getCurrentRegion()
	{
		return _currentRegion;
	}

	public final double getDistance(int x, int y)
	{
		return Math.sqrt(getXYDeltaSq(x, y));
	}

	private final long getXYDeltaSq(int x, int y)
	{
		int dx = x - getX();
		int dy = y - getY();
		return dx * dx + dy * dy;
	}

	/**
	 * @return the x position of the L2Object.<BR><BR>
	 */
	public int getX()
	{
		return _x;
	}

	/**
	 * @return the y position of the L2Object.<BR><BR>
	 */
	public int getY()
	{
		return _y;
	}

	public final double getDistance(int x, int y, int z)
	{
		return Math.sqrt(getXYZDeltaSq(x, y, z));
	}

	private long getXYZDeltaSq(int x, int y, int z)
	{
		return getXYDeltaSq(x, y) + getZDeltaSq(z);
	}

	private long getZDeltaSq(int z)
	{
		int dz = z - getZ();
		return dz * dz;
	}

	/**
	 * @return the z position of the L2Object.<BR><BR>
	 */
	public int getZ()
	{
		return _z;
	}

	public String getL2ClassShortName()
	{
		return getClass().getName().replaceAll("^.*\\.(.*?)$", "$1");
	}

	public float getMoveSpeed()
	{
		return 0;
	}

	/**
	 * Возвращает L2Player управляющий даным обьектом.<BR>
	 * <li>Для L2Player это сам игрок.</li>
	 * <li>Для L2Summon это его хозяин.</li><BR><BR>
	 * @return L2Player управляющий даным обьектом.
	 */
	public L2Player getPlayer()
	{
		return null;
	}

	public int getPolyid()
	{
		return _poly_id & 0xFFFFFF;
	}

	int getPolytype()
	{
		return _poly_id >> 24;
	}

	public Object getProperty(PropertyCollection property)
	{
		return getListenerEngine().getProperty(property);
	}

	public final double getRealDistance(L2Object obj)
	{
		return getRealDistance3D(obj, true);
	}

	private double getRealDistance3D(L2Object obj, boolean ignoreZ)
	{
		double distance = ignoreZ ? getDistance(obj) : getDistance3D(obj);
		if(isCharacter())
			distance -= ((L2Character) this).getTemplate().collisionRadius;
		if(obj.isCharacter())
			distance -= ((L2Character) obj).getTemplate().collisionRadius;
		return distance > 0 ? distance : 0;
	}

	public final double getDistance(L2Object obj)
	{
		if(obj == null)
			return 0;
		return Math.sqrt(getXYDeltaSq(obj.getX(), obj.getY()));
	}

	public final double getDistance3D(L2Object obj)
	{
		if(obj == null)
			return 0;
		return Math.sqrt(getXYZDeltaSq(obj.getX(), obj.getY(), obj.getZ()));
	}

	public boolean isCharacter()
	{
		return this instanceof L2Character;
	}

	public final double getRealDistance3D(L2Object obj)
	{
		return getRealDistance3D(obj, false);
	}

	public final long getSqDistance(L2Object obj)
	{
		if(obj == null)
			return 0;
		return getXYDeltaSq(obj.getLoc());
	}

	private long getXYDeltaSq(Location loc)
	{
		return getXYDeltaSq(loc.x, loc.y);
	}

	/**
	 * Возвращает позицию (x, y, z, heading)
	 * @return Location
	 */
	public Location getLoc()
	{
		return new Location(_x, _y, _z, getHeading());
	}

	public int getHeading()
	{
		return 0;
	}

	public final long getSqDistance(int x, int y)
	{
		return getXYDeltaSq(x, y);
	}

	public final long getZDeltaSq(Location loc)
	{
		return getZDeltaSq(loc.z);
	}

	public L2Zone getZone(L2Zone.ZoneType type)
	{
		//FastList<L2Territory> territories = L2World.getActiveTerritory(getX(), getY());
		if(_territories != null)
			for(L2Territory terr : getTerritories())
				if(terr.getZone() != null && terr.getZone().isActive() && terr.getZone().getType() == type)
					return terr.getZone();
		return null;
	}

	public L2Territory[] getTerritories()
	{
		if(_territories == null)
			return null;
		return _territories.toArray(new L2Territory[_territories.size()]);
	}

	public boolean hasAI()
	{
		return false;
	}

	public boolean inObserverMode()
	{
		return false;
	}

	public boolean isActionBlocked(String action)
	{
		//FastList<L2Territory> territories = L2World.getActiveTerritory(getX(), getY());
		if(_territories != null)
		{
			for(L2Territory terr : getTerritories())
				if(terr.getZone() != null && terr.getZone().isActive() && terr.getZone().getType() == L2Zone.ZoneType.unblock_actions && terr.getZone().isActionBlocked(action))
					return false;
			for(L2Territory terr : getTerritories())
				if(terr.getZone() != null && terr.getZone().isActive() && terr.getZone().getType() != L2Zone.ZoneType.unblock_actions && terr.getZone().isActionBlocked(action))
					return true;
		}
		return false;
	}

	public boolean isAttackable()
	{
		return false;
	}

	public abstract boolean isAutoAttackable(L2Character attacker);

	public boolean isBoss()
	{
		return this instanceof L2BossInstance;
	}

	/**
	 * Проверяет в досягаемости расстояния ли объект
	 * @param obj проверяемый объект
	 * @param range расстояние
	 * @return true, если объект досягаем
	 */
	public final boolean isInRange(L2Object obj, int range)
	{
		if(obj == null)
			return false;
		long dx = Math.abs(obj.getX() - getX());
		if(dx > range)
			return false;
		long dy = Math.abs(obj.getY() - getY());
		if(dy > range)
			return false;
		long dz = Math.abs(obj.getZ() - getZ());
		return dz <= 1500 && dx * dx + dy * dy <= range * range;
	}

	public final boolean isInRange(Location loc, long range)
	{
		return isInRangeSq(loc, range * range);
	}

	public final boolean isInRangeSq(Location loc, long range)
	{
		return getXYDeltaSq(loc) <= range;
	}

	public final boolean isInRangeZ(L2Object obj, int range)
	{
		if(obj == null)
			return false;
		long dx = Math.abs(obj.getX() - getX());
		if(dx > range)
			return false;
		long dy = Math.abs(obj.getY() - getY());
		if(dy > range)
			return false;
		long dz = Math.abs(obj.getZ() - getZ());
		return dz <= range && dx * dx + dy * dy + dz * dz <= range * range;
	}

	public final boolean isInRangeZ(Location loc, long range)
	{
		return isInRangeZSq(loc, range * range);
	}

	private boolean isInRangeZSq(Location loc, long range)
	{
		return getXYZDeltaSq(loc) <= range;
	}

	private long getXYZDeltaSq(Location loc)
	{
		return getXYDeltaSq(loc.x, loc.y) + getZDeltaSq(loc.z);
	}

	/**
	 * Проверяет наличие объекта в мире
	 * @return true если оъект есть в мире
	 */
	boolean isInWorld()
	{
		return L2World.findObject(_objectId) != null;
	}

	public boolean isInZone(L2Zone zone)
	{
		//FastList<L2Territory> territories = L2World.getActiveTerritory(getX(), getY());
		if(_territories != null)
			for(L2Territory terr : getTerritories())
				if(terr.getZone() != null && terr.getZone().getId() == zone.getId())
					return true;
		return false;
	}

	public boolean isInZoneOlympiad()
	{
		return isInZone(L2Zone.ZoneType.OlympiadStadia);
	}

	public boolean isInZone(L2Zone.ZoneType type)
	{
		//FastList<L2Territory> territories = L2World.getActiveTerritory(getX(), getY());
		if(_territories != null)
			for(L2Territory terr : getTerritories())
				if(terr != null && terr.getZone() != null && terr.getZone().isActive() && terr.getZone().getType() == type)
					return true;
		return false;
	}

	public boolean isInZonePeace()
	{
		return isInZone(L2Zone.ZoneType.peace_zone) && !isInZoneBattle();
	}

	public boolean isInZoneBattle()
	{
		return isInZone(L2Zone.ZoneType.battle_zone) || isInZone(L2Zone.ZoneType.OlympiadStadia);
	}

	boolean isInZoneWater()
	{
		return isInZoneIncludeZ(L2Zone.ZoneType.water) && !isInZone(L2Zone.ZoneType.no_water) && !isInBoat();
	}

	public boolean isInZoneIncludeZ(L2Zone.ZoneType type)
	{
		//FastList<L2Territory> territories = L2World.getActiveTerritory(getX(), getY());
		if(_territories != null)
			for(L2Territory terr : getTerritories())
				if(terr.getZone() != null && terr.getZone().isActive() && terr.getZone().getType() == type && getZ() > terr.getZmin() && getZ() < terr.getZmax())
					return true;
		return false;
	}

	public boolean isInBoat()
	{
		return false;
	}

	public boolean isItem()
	{
		return this instanceof L2ItemInstance;
	}

	public boolean isMarker()
	{
		return false;
	}

	public boolean isMonster()
	{
		return this instanceof L2MonsterInstance;
	}

	public boolean isPet()
	{
		return this instanceof L2PetInstance;
	}

	public boolean isPlayable()
	{
		return this instanceof L2Playable;
	}

	public boolean isPolymorphed()
	{
		return _poly_id != 0;
	}

	public boolean isRaid()
	{
		return this instanceof L2RaidBossInstance && !(this instanceof L2ReflectionBossInstance);
	}

	public boolean isSummon()
	{
		return this instanceof L2SummonInstance;
	}

	boolean isSwimming()
	{
		return getWaterZ() != -1;
	}

	/**
	 * Возвращает координаты поверхности воды, если мы находимся в ней, или над ней.
	 */
	public int getWaterZ()
	{
		if(!isPlayer() || isInBoat() || _territories == null)
			return -1;
		for(L2Territory terr : getTerritories())
			if(terr != null && terr.getZone() != null && terr.getZone().getType() == L2Zone.ZoneType.no_water && terr.isInside(getX(), getY()))
				return -1;
		int z = GeoEngine.getHeight(getLoc());
		for(L2Territory terr : getTerritories())
			if(terr != null && terr.getZone() != null && terr.getZone().getType() == L2Zone.ZoneType.water && terr.isInside(getX(), getY(), z))
				return terr.getZmax();
		return -1;
	}

	public void onAction(L2Player player)
	{
		if(Events.onAction(player, this))
			return;

		player.sendActionFailed();
	}

	public void onActionShift(L2Player player)
	{
		if(Events.onActionShift(player, this))
			return;

		player.sendActionFailed();
	}

	public void onForcedAttack(L2Player player)
	{
		player.sendActionFailed();
	}

	/**
	 * Do Nothing.<BR><BR>
	 *
	 * <B><U> Overriden in </U> :</B><BR><BR>
	 * <li> L2Summon :  Reset isShowSpawnAnimation flag</li>
	 * <li> L2NpcInstance    :  Reset some flags</li><BR><BR>
	 *
	 */
	public void onSpawn()
	{}

	public void pickupMe(L2Character target)
	{
		// Create a server->client GetItem packet to pick up the L2ItemInstance
		//player.broadcastPacket(new GetItem((L2ItemInstance) this, player.getObjectId()));

		// if this item is a mercenary ticket, remove the spawns!
		if(this instanceof L2ItemInstance)
		{
			int itemId = ((L2ItemInstance) this).getItemId();
			/*if(itemId >= 3960 && itemId <= 3972 // Gludio
			 || itemId >= 3973 && itemId <= 3985 // Dion
			 || itemId >= 3986 && itemId <= 3998 // Giran
			 || itemId >= 3999 && itemId <= 4011 // Oren
			 || itemId >= 4012 && itemId <= 4026 // Aden
			 || itemId >= 5205 && itemId <= 5215 // Innadril
			 || itemId >= 6779 && itemId <= 6833 // Goddard
			 || itemId >= 7973 && itemId <= 8029 // Rune
			 || itemId >= 7918 && itemId <= 7972 // Schuttgart
			 )*/
			if(itemId >= 3960 && itemId <= 4026 || itemId >= 5205 && itemId <= 5214 || itemId >= 6038 && itemId <= 6306 || itemId >= 6779 && itemId <= 6833 || itemId >= 7918 && itemId <= 8029)
				MercTicketManager.getInstance().removeTicket((L2ItemInstance) this);

			if(target != null && target.isPlayer() && (itemId == 57 || itemId == 6353))
			{
				Quest q = QuestManager.getQuest(255);
				if(q != null)
					((L2Player) target).processQuestEvent(q.getName(), "CE" + itemId);
			}
		}

		// Remove the L2ItemInstance from the world
		_hidden = true;
		L2World.removeVisibleObject(this);
	}

	protected void refreshID()
	{
		int newObjectId = IdFactory.getInstance().getNextId();
		int oldObjectId = getObjectId();
		L2World.removeObject(this);
		_objectId = newObjectId;
		IdFactory.getInstance().releaseId(oldObjectId);
	}

	/**
	 * @return the identifier of the L2Object.<BR><BR>
	 */
	public final int getObjectId()
	{
		return _objectId;
	}

	public void removeMethodInvokeListener(MethodInvokeListener listener)
	{
		getListenerEngine().removeMethodInvokedListener(listener);
	}

	public void removeMethodInvokeListener(MethodCollection methodName, MethodInvokeListener listener)
	{
		getListenerEngine().removeMethodInvokedListener(methodName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		getListenerEngine().removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyCollection value, PropertyChangeListener listener)
	{
		getListenerEngine().removePropertyChangeListener(value, listener);
	}

	void setCurrentRegion(L2WorldRegion region)
	{
		_currentRegion = region;
	}

	/**
	 * Устанавливает позицию (x, y, z) L2Object
	 * @param loc Location
	 */
	public void setLoc(Location loc)
	{
		setXYZ(loc.x, loc.y, loc.z);
	}

	/**
	 * Set the x,y,z position of the L2Object and if necessary modify its _worldRegion.<BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Update position during and after movement, or after teleport </li><BR>
	 *
	 * @param x new x coord
	 * @param y new y coord
	 * @param z new z coord
	 */
	public void setXYZ(int x, int y, int z)
	{
		if(!L2World.validCoords(x, y))
			if(isPlayer())
			{
				_log.warning("Player " + this + " (" + _objectId + ") at bad coords: (" + getX() + ", " + getY() + ").");
				L2Player player = (L2Player) this;
				player.abortAttack();
				player.abortCast();
				player.sendActionFailed();
				player.teleToClosestTown();
				return;
			}
			else if(this instanceof L2NpcInstance)
			{
				L2Spawn spawn = ((L2NpcInstance) this).getSpawn();
				if(spawn == null)
					return;
				if(spawn.getLocx() != 0)
				{
					x = spawn.getLocx();
					y = spawn.getLocy();
					z = spawn.getLocz();
				}
				else
				{
					int p[] = TerritoryTable.getInstance().getRandomPoint(spawn.getLocation());
					x = p[0];
					y = p[1];
					z = p[2];
				}
			}
			else if(isCharacter())
			{
				decayMe();
				return;
			}

		_x = x;
		_y = y;
		_z = z;

		L2World.addVisibleObject(this, null);
	}

	public void setPolyInfo(int polytype, int polyid)
	{
		_poly_id = (polytype << 24) + polyid;
		if(isPlayer())
		{
			L2Player cha = (L2Player) this;
			cha.teleToLocation(getLoc());
			cha.broadcastUserInfo(true);
		}
		else
		{
			decayMe();
			spawnMe(getLoc());
		}
	}

	public final void spawnMe(Location loc)
	{
		if(loc.x > L2World.MAP_MAX_X)
			loc.x = L2World.MAP_MAX_X - 5000;
		if(loc.x < L2World.MAP_MIN_X)
			loc.x = L2World.MAP_MIN_X + 5000;
		if(loc.y > L2World.MAP_MAX_Y)
			loc.y = L2World.MAP_MAX_Y - 5000;
		if(loc.y < L2World.MAP_MIN_Y)
			loc.y = L2World.MAP_MIN_Y + 5000;

		_x = loc.x;
		_y = loc.y;
		_z = getGeoZ(loc);

		spawnMe();
	}

	public void setReflection(int i)
	{
		if(_reflection == i)
			return;

		boolean blink = false;
		if(!_hidden)
		{
			decayMe();
			blink = true;
		}

		getReflection().removeObject(_objectId);
		_reflection = i;
		getReflection().addObject(this);

		if(blink)
			spawnMe();
	}

	/**
	 * Set the x,y,z position of the L2Object and make it invisible.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * A L2Object is invisble if <B>_hidden</B> = true<BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Create a Door</li>
	 * <li> Restore L2Player</li><BR>
	 *
	 * @param x new x coord
	 * @param y new y coord
	 * @param z new z coord
	 */
	public void setXYZInvisible(int x, int y, int z)
	{
		if(x > L2World.MAP_MAX_X)
			x = L2World.MAP_MAX_X - 5000;
		if(x < L2World.MAP_MIN_X)
			x = L2World.MAP_MIN_X + 5000;
		if(y > L2World.MAP_MAX_Y)
			y = L2World.MAP_MAX_Y - 5000;
		if(y < L2World.MAP_MIN_Y)
			y = L2World.MAP_MIN_Y + 5000;

		if(z < -16000 || z > 16000)
			z = 16000;

		_x = x;
		_y = y;
		_z = z;

		_hidden = true;
	}

	public void startAttackStanceTask()
	{}

	public void toggleVisible()
	{
		if(isVisible())
			decayMe();
		else
			spawnMe();
	}

	/**
	 * Return the visibility state of the L2Object. <BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * A L2Object is invisible if <B>_hidden</B>=true or <B>_worldregion</B>==null <BR><BR>
	 *
	 * @return true if visible
	 */
	public final boolean isVisible()
	{
		return !_hidden;
	}
}