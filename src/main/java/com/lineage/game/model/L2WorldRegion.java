package com.lineage.game.model;

import java.util.ArrayList;

import com.lineage.game.ai.DefaultAI;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.tables.SpawnTable;
import com.lineage.util.GArray;

/**
 * @author Diamond
 * @Date: 15/5/2007
 * @Time: 10:06:34
 */
public final class L2WorldRegion
{
	private L2Object[] _objects = null;
	private int tileX, tileY, tileZ, _objectsSize = 0, _playersSize = 0;
	private boolean _active = false;
	private final Object objects_lock = new Object();
	private final Object active_lock = new Object();

	public L2WorldRegion(int pTileX, int pTileY, int pTileZ)
	{
		tileX = pTileX;
		tileY = pTileY;
		tileZ = pTileZ;
	}

	private void switchAI(Boolean isOn)
	{
		for(L2Character cha : getCharactersList(new GArray<L2Character>(), 0, -1))
			if(isOn)
			{
				if(cha.getAI() instanceof DefaultAI)
					cha.getAI().startAITask();
			}
			else if(cha.hasAI() && !cha.getAI().isGlobalAI() && cha.getAI() instanceof DefaultAI)
			{
				cha.setTarget(null);
				cha.stopMove();
				cha.getEffectList().stopAllEffects();
				cha.getAI().stopAITask();
			}
	}

	private void setActive(boolean value)
	{
		synchronized (active_lock)
		{
			if(_active == value)
				return;
			_active = value;

			switchAI(value);

			if(value)
				L2World.addActiveRegion(this);
			else
				L2World.removeActiveRegion(this);
		}
	}

	private void changeStatus(boolean status)
	{
		for(L2WorldRegion neighbor : getNeighbors())
			if(status || neighbor.areNeighborsEmpty())
				neighbor.setActive(status);
	}

	public void addToPlayers(L2Object object, L2Character dropper)
	{
		if(_objects == null)
		{
			_objectsSize = 0;
			_playersSize = 0;
			return;
		}

		L2Player player = null;
		if(object.isPlayer())
			player = (L2Player) object;

		// Если object - игрок, показать ему все видимые обьекты в регионе
		if(player != null)
			for(L2Object obj : getObjectsList(new GArray<L2Object>(), object.getObjectId(), object.getReflection()))
			{
				if(obj == null)
					continue;
				// Если это фэйк обсервера - не показывать.
				if(obj.inObserverMode() && (obj.getCurrentRegion() == null || !obj.getCurrentRegion().equals(this)))
					continue;
				player.addVisibleObject(obj, dropper);
			}

		// Показать обьект всем игрокам в регионе
		for(L2Object obj : getObjectsList(new GArray<L2Object>(), object.getObjectId(), object.getReflection()))
			if(obj != null && obj.isPlayer())
				((L2Player) obj).addVisibleObject(object, dropper);
	}

	public void removeFromPlayers(L2Object object)
	{
		if(_objects == null)
		{
			_objectsSize = 0;
			_playersSize = 0;
			return;
		}

		L2Player player = null;
		if(object.isPlayer())
			player = (L2Player) object;

		// Если object - игрок, убрать у него все видимые обьекты в регионе
		if(player != null)
			for(L2Object obj : getObjectsList(new GArray<L2Object>(), object.getObjectId(), object.getReflection()))
				if(obj != null)
					player.removeVisibleObject(obj);

		// Убрать обьект у всех игроков в регионе
		for(L2Object obj : getObjectsList(new GArray<L2Object>(), object.getObjectId(), object.getReflection()))
			if(obj != null && obj.isPlayer())
				((L2Player) obj).removeVisibleObject(object);
	}

	public L2Object[] getObjects()
	{
		synchronized (objects_lock)
		{
			if(_objects == null)
			{
				_objects = new L2Object[50];
				_objectsSize = 0;
				_playersSize = 0;
			}
			return _objects;
		}
	}

	public void addObject(L2Object obj)
	{
		if(obj == null)
			return;
		boolean changeStatus = false;
		synchronized (objects_lock)
		{
			if(_objects == null)
			{
				_objects = new L2Object[50];
				_objectsSize = 0;
			}
			else if(_objectsSize >= _objects.length)
			{
				L2Object[] temp = new L2Object[_objects.length * 2];
				for(int i = 0; i < _objectsSize; i++)
					temp[i] = _objects[i];
				_objects = temp;
			}

			_objects[_objectsSize] = obj;
			_objectsSize++;

			if(obj.isPlayer())
			{
				if(_playersSize == 0)
					changeStatus = true;
				_playersSize++;
			}
		}
		if(changeStatus)
			changeStatus(true);
		else if(obj.isNpc() && obj.getAI() instanceof DefaultAI && (obj.getAI().isGlobalAI() || !areNeighborsEmpty()))
			obj.getAI().startAITask();
	}

	public void removeObject(L2Object obj, boolean move)
	{
		if(obj == null)
			return;
		boolean changeStatus = false;
		synchronized (objects_lock)
		{
			if(_objects == null)
			{
				_objectsSize = 0;
				_playersSize = 0;
				return;
			}

			if(_objectsSize > 1)
			{
				int k = -1;
				for(int i = 0; i < _objectsSize; i++)
					if(_objects[i] == obj)
					{
						k = i;
						break;
					}
				if(k > -1)
				{
					_objects[k] = _objects[_objectsSize - 1];
					_objects[_objectsSize - 1] = null;
					_objectsSize--;
				}
			}
			else if(_objectsSize == 1 && _objects[0] == obj)
			{
				_objects[0] = null;
				_objects = null;
				_objectsSize = 0;
				_playersSize = 0;
			}

			if(obj.isPlayer())
			{
				_playersSize--;
				if(_playersSize <= 0)
				{
					_playersSize = 0;
					changeStatus = true;
				}
			}
		}
		if(changeStatus)
			changeStatus(false);
		else if(obj.isNpc())
			if(!move && obj.getAI() instanceof DefaultAI && !obj.getAI().isGlobalAI())
				obj.getAI().stopAITask();
	}

	public GArray<L2Object> getObjectsList(GArray<L2Object> result, int exclude, Reflection reflection)
	{
		synchronized (objects_lock)
		{
			if(_objects == null || _objectsSize == 0)
				return result;
			for(int i = 0; i < _objectsSize; i++)
			{
				L2Object obj = _objects[i];
				if(obj != null && obj.getObjectId() != exclude && (reflection.getId() == -1 || obj.getReflection() == reflection))
					result.add(obj);
			}
		}
		return result;
	}

	public GArray<L2Object> getObjectsList(GArray<L2Object> result, int exclude, Reflection reflection, int x, int y, int z, long sqrad, int height)
	{
		synchronized (objects_lock)
		{
			if(_objects == null || _objectsSize == 0)
				return result;
			for(int i = 0; i < _objectsSize; i++)
			{
				L2Object obj = _objects[i];
				if(obj == null || obj.getObjectId() == exclude || reflection.getId() != -1 && obj.getReflection() != reflection)
					continue;
				if(Math.abs(obj.getZ() - z) > height)
					continue;
				long dx = obj.getX() - x;
				dx *= dx;
				if(dx > sqrad)
					continue;
				long dy = obj.getY() - y;
				dy *= dy;
				if(dx + dy < sqrad)
					result.add(obj);
			}
		}
		return result;
	}

	public GArray<L2Character> getCharactersList(GArray<L2Character> result, int exclude, int reflection)
	{
		synchronized (objects_lock)
		{
			if(_objects == null || _objectsSize == 0)
				return result;
			for(int i = 0; i < _objectsSize; i++)
			{
				L2Object obj = _objects[i];
				if(obj != null && obj.isCharacter() && obj.getObjectId() != exclude && (reflection == -1 || obj.getReflection().getId() == reflection))
					result.add((L2Character) obj);
			}
		}
		return result;
	}

	public GArray<L2Character> getCharactersList(GArray<L2Character> result, int exclude, int reflection, int x, int y, int z, long sqrad, int height)
	{
		synchronized (objects_lock)
		{
			if(_objects == null || _objectsSize == 0)
				return result;
			for(int i = 0; i < _objectsSize; i++)
			{
				L2Object obj = _objects[i];
				if(obj == null || !obj.isCharacter() || obj.getObjectId() == exclude || reflection != -1 && obj.getReflection().getId() != reflection)
					continue;
				if(Math.abs(obj.getZ() - z) > height)
					continue;
				long dx = obj.getX() - x;
				dx *= dx;
				if(dx > sqrad)
					continue;
				long dy = obj.getY() - y;
				dy *= dy;
				if(dx + dy < sqrad)
					result.add((L2Character) obj);
			}
		}
		return result;
	}

	public GArray<L2NpcInstance> getNpcsList(GArray<L2NpcInstance> result, int exclude, Reflection reflection)
	{
		synchronized (objects_lock)
		{
			if(_objects == null || _objectsSize == 0)
				return result;
			for(int i = 0; i < _objectsSize; i++)
			{
				L2Object obj = _objects[i];
				if(obj != null && obj.isNpc() && obj.getObjectId() != exclude && (reflection.getId() == -1 || obj.getReflection() == reflection))
					result.add((L2NpcInstance) obj);
			}
		}
		return result;
	}

	public GArray<L2NpcInstance> getNpcsList(GArray<L2NpcInstance> result, int exclude, Reflection reflection, int x, int y, int z, long sqrad, int height)
	{
		synchronized (objects_lock)
		{
			if(_objects == null || _objectsSize == 0)
				return result;
			for(int i = 0; i < _objectsSize; i++)
			{
				L2Object obj = _objects[i];
				if(obj == null || !obj.isNpc() || obj.getObjectId() == exclude || reflection.getId() != -1 && obj.getReflection().getId() != reflection.getId())
					continue;
				if(Math.abs(obj.getZ() - z) > height)
					continue;
				long dx = obj.getX() - x;
				dx *= dx;
				if(dx > sqrad)
					continue;
				long dy = obj.getY() - y;
				dy *= dy;
				if(dx + dy < sqrad)
					result.add((L2NpcInstance) obj);
			}
		}
		return result;
	}

	public GArray<L2Player> getPlayersList(GArray<L2Player> result, int exclude, Reflection reflection)
	{
		synchronized (objects_lock)
		{
			if(_objects == null || _objectsSize == 0)
				return result;
			for(int i = 0; i < _objectsSize; i++)
			{
				L2Object obj = _objects[i];
				if(obj != null && obj.isPlayer() && obj.getObjectId() != exclude && (reflection.getId() == -1 || obj.getReflection() == reflection))
					result.add((L2Player) obj);
			}
		}
		return result;
	}

	public GArray<L2Player> getPlayersList(GArray<L2Player> result, int exclude, Reflection reflection, int x, int y, int z, long sqrad, int height)
	{
		synchronized (objects_lock)
		{
			if(_objects == null || _objectsSize == 0)
				return result;
			for(int i = 0; i < _objectsSize; i++)
			{
				L2Object obj = _objects[i];
				if(obj == null || !obj.isPlayer() || obj.getObjectId() == exclude || reflection.getId() != -1 && obj.getReflection().getId() != reflection.getId())
					continue;
				if(Math.abs(obj.getZ() - z) > height)
					continue;
				long dx = obj.getX() - x;
				dx *= dx;
				if(dx > sqrad)
					continue;
				long dy = obj.getY() - y;
				dy *= dy;
				if(dx + dy < sqrad)
					result.add((L2Player) obj);
			}
		}
		return result;
	}

	public GArray<L2Playable> getPlayablesList(GArray<L2Playable> result, int exclude, Reflection reflection)
	{
		synchronized (objects_lock)
		{
			if(_objects == null || _objectsSize == 0)
				return result;
			for(int i = 0; i < _objectsSize; i++)
			{
				L2Object obj = _objects[i];
				if(obj != null && (obj.isPlayer() || obj.isPet() || obj.isSummon()) && obj.getObjectId() != exclude && (reflection.getId() == -1 || obj.getReflection() == reflection))
					result.add((L2Playable) obj);
			}
		}
		return result;
	}

	public GArray<L2Playable> getPlayablesList(GArray<L2Playable> result, int exclude, Reflection reflection, int x, int y, int z, long sqrad, int height)
	{
		synchronized (objects_lock)
		{
			if(_objects == null || _objectsSize == 0)
				return result;
			for(int i = 0; i < _objectsSize; i++)
			{
				L2Object obj = _objects[i];
				if(obj == null || !obj.isPlayer() && !obj.isPet() && !obj.isSummon() || obj.getObjectId() == exclude || reflection.getId() != -1 && obj.getReflection().getId() != reflection.getId())
					continue;
				if(Math.abs(obj.getZ() - z) > height)
					continue;
				long dx = obj.getX() - x;
				dx *= dx;
				if(dx > sqrad)
					continue;
				long dy = obj.getY() - y;
				dy *= dy;
				if(dx + dy < sqrad)
					result.add((L2Playable) obj);
			}
		}
		return result;
	}

	public void deleteVisibleNpcSpawns()
	{
		synchronized (objects_lock)
		{
			if(_objects != null)
			{
				ArrayList<L2NpcInstance> toRemove = new ArrayList<L2NpcInstance>();
				for(int i = 0; i < _objectsSize; i++)
				{
					L2Object obj = _objects[i];
					if(obj != null && obj.isNpc())
						toRemove.add((L2NpcInstance) obj);
				}
				for(L2NpcInstance npc : toRemove)
				{
					L2Spawn spawn = npc.getSpawn();
					if(spawn != null)
					{
						npc.deleteMe();
						spawn.stopRespawn();
						SpawnTable.getInstance().deleteSpawn(spawn, false);
					}
				}
			}
		}
	}

	/**
	 * Показывает игроку все видимые обьекты в регионе
	 */
	public void showObjectsToPlayer(L2Player player)
	{
		if(player != null && _objects != null)
			for(L2Object obj : getObjectsList(new GArray<L2Object>(), player.getObjectId(), player.getReflection()))
				if(obj != null)
					player.addVisibleObject(obj, null);
	}

	/**
	 * Убирает у игрока все видимые обьекты в регионе
	 */
	public void removeObjectsFromPlayer(L2Player player)
	{
		if(player != null && _objects != null)
			for(L2Object obj : getObjectsList(new GArray<L2Object>(), player.getObjectId(), player.getReflection()))
				if(obj != null)
					player.removeVisibleObject(obj);
	}

	/**
	 * Убирает обьект у всех игроков в регионе
	 */
	public void removePlayerFromOtherPlayers(L2Object object)
	{
		if(object != null && _objects != null)
			for(L2Object obj : getObjectsList(new GArray<L2Object>(), object.getObjectId(), object.getReflection()))
				if(obj != null && obj.isPlayer())
					((L2Player) obj).removeVisibleObject(object);
	}

	public boolean areNeighborsEmpty()
	{
		if(!isEmpty())
			return false;
		for(L2WorldRegion neighbor : getNeighbors())
			if(!neighbor.isEmpty())
				return false;
		return true;
	}

	public ArrayList<L2WorldRegion> getNeighbors()
	{
		return L2World.getNeighbors(tileX, tileY, tileZ);
	}

	public int getObjectsSize()
	{
		return _objectsSize;
	}

	public boolean isEmpty()
	{
		return _playersSize <= 0;
	}

	public boolean isActive()
	{
		return _active;
	}

	public String getName()
	{
		return "(" + tileX + ", " + tileY + ", " + tileZ + ")";
	}
}