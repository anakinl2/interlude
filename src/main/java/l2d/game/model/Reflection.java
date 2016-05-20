package l2d.game.model;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javolution.util.FastList;
import l2d.game.idfactory.IdFactory;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.tables.ReflectionTable;
import l2d.game.tables.SpawnTable;
import com.lineage.util.Location;

public class Reflection
{
	protected int _id;
	private Location _coreLoc; // место, к которому кидает при использовании SoE/unstuck, иначе выбрасывает в основной мир
	private Location _returnLoc; // если не прописано core, но прописан return, то телепортит туда, одновременно перемещая в основной мир
	private Location _teleportLoc; // точка входа
	private FastList<L2Spawn> _spawns = new FastList<L2Spawn>();
	private FastList<L2DoorInstance> _doors = new FastList<L2DoorInstance>();
	protected HashMap<Integer, WeakReference<L2Object>> _objects = new HashMap<Integer, WeakReference<L2Object>>();
	protected HashMap<Integer, WeakReference<L2Player>> _players = new HashMap<Integer, WeakReference<L2Player>>();
	private Timer _collapseTimer;
	private TimerTask _collapseTimerTask;
	private L2Party _party;

	public Reflection(int id)
	{
		_id = id;
		ReflectionTable.getInstance().addReflection(this);
	}

	public void addSpawn(L2Spawn spawn)
	{
		_spawns.add(spawn);
	}

	public void addDoor(L2DoorInstance door)
	{
		_doors.add(door);
	}

	public void setParty(L2Party party)
	{
		_party = party;
	}

	public void startCollapseTimer(long time)
	{
		if(_collapseTimerTask != null)
		{
			_collapseTimerTask.cancel();
			_collapseTimerTask = null;
		}

		if(_collapseTimer != null)
		{
			_collapseTimer.cancel();
			_collapseTimer = null;
		}

		_collapseTimer = new Timer();
		_collapseTimerTask = new TimerTask(){
			@Override
			public void run()
			{
				collapse();
			}
		};

		_collapseTimer.schedule(_collapseTimerTask, time);
	}

	public void stopCollapseTimer()
	{
		if(_collapseTimerTask != null)
			_collapseTimerTask.cancel();
		_collapseTimerTask = null;

		if(_collapseTimer != null)
			_collapseTimer.cancel();
		_collapseTimer = null;
	}

	public void collapse() // TODO: добавить уничтожение лежащего на земле
	{
		stopCollapseTimer();
		for(L2Spawn s : _spawns)
		{
			s.despawnAll();
			s.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(s, false);
		}
		for(L2DoorInstance d : _doors)
			d.decayMe();
		FastList<L2Player> teleport_list = new FastList<L2Player>();
		for(WeakReference<L2Object> o : _objects.values())
			if(o.get() != null)
				if(o.get().isPlayer())
					teleport_list.add((L2Player) o.get());
				else if(!o.get().isSummon() && !o.get().isPet())
				{
					o.get().decayMe();
					L2World.removeObject(o.get());
				}

		for(L2Player player : teleport_list)
		{
			if(player.getParty() != null && equals(player.getParty().getReflection()))
				player.getParty().setReflection(null);
			if(equals(player.getReflection()))
				if(getReturnLoc() != null)
					player.teleToLocation(getReturnLoc(), 0);
				else
					player.setReflection(0);
		}

		if(_party != null)
		{
			_party.setReflection(null);
			_party = null;
		}

		ReflectionTable.getInstance().removeReflection(_id);
		if(_id > 100000)
			IdFactory.getInstance().releaseId(_id);
	}

	public int getId()
	{
		return _id;
	}

	public void addObject(L2Object o)
	{
		_objects.put(o._objectId, new WeakReference<L2Object>(o));
		if(o.isPlayer())
			_players.put(o._objectId, new WeakReference<L2Player>((L2Player) o));
	}

	public void removeObject(int i)
	{
		_objects.remove(i);
		_players.remove(i);
		if(i >= 268435456 && _players.isEmpty())
			collapse();
	}

	public void setCoreLoc(Location l)
	{
		_coreLoc = l;
	}

	public Location getCoreLoc()
	{
		return _coreLoc;
	}

	public void setReturnLoc(Location l)
	{
		_returnLoc = l;
	}

	public Location getReturnLoc()
	{
		return _returnLoc;
	}

	public void setTeleportLoc(Location l)
	{
		_teleportLoc = l;
	}

	public Location getTeleportLoc()
	{
		return _teleportLoc;
	}

	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Reflection))
			return false;
		return _id == ((Reflection) o).getId();
	}

	public FastList<L2Spawn> getSpawns()
	{
		return _spawns;
	}

	public FastList<L2DoorInstance> getDoors()
	{
		return _doors;
	}

	@Override
	protected void finalize()
	{
		collapse();
	}

	public boolean canChampions()
	{
		return _id == 0;
	}
}