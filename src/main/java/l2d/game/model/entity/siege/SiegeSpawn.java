package l2d.game.model.entity.siege;

import com.lineage.util.Location;

public class SiegeSpawn
{
	Location _location;
	private int _npcId;
	private int _heading;
	private int _siegeUnitId;
	private int _hp;

	public SiegeSpawn(int siegeUnitId, int x, int y, int z, int heading, int npc_id)
	{
		_siegeUnitId = siegeUnitId;
		_location = new Location(x, y, z, heading);
		_heading = heading;
		_npcId = npc_id;
	}

	public SiegeSpawn(int siegeUnitId, int x, int y, int z, int heading, int npc_id, int hp)
	{
		_siegeUnitId = siegeUnitId;
		_location = new Location(x, y, z, heading);
		_heading = heading;
		_npcId = npc_id;
		_hp = hp;
	}

	public int getSiegeUnitId()
	{
		return _siegeUnitId;
	}

	public int getNpcId()
	{
		return _npcId;
	}

	public int getHeading()
	{
		return _heading;
	}

	public int getHp()
	{
		return _hp;
	}

	public Location getLoc()
	{
		return _location;
	}
}