package com.lineage.game.instancemanager;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.model.entity.Town;
import com.lineage.game.tables.MapRegion;

public final class TownManager
{
	private static TownManager _instance;
	private List<Town> _towns = new FastList<Town>();

	private final static Logger _log = Logger.getLogger(TownManager.class.getName());

	private TownManager()
	{
		FastList<L2Zone> zones = ZoneManager.getInstance().getZoneByType(ZoneType.Town);
		if(zones.size() == 0)
			_log.warning("Not found zones for Towns!!!");
		else
			for(L2Zone zone : zones)
				_towns.add(new Town(zone.getIndex()));
	}

	public static TownManager getInstance()
	{
		if(_instance == null)
			_instance = new TownManager();
		return _instance;
	}

	public Town getClosestTown(int x, int y)
	{
		return getTown(MapRegion.getInstance().getMapRegion(x, y));
	}

	public Town getClosestTown(L2Object activeObject)
	{
		return getTown(MapRegion.getInstance().getMapRegion(activeObject.getX(), activeObject.getY()));
	}

	public int getClosestTownNumber(L2Character activeChar)
	{
		return MapRegion.getInstance().getMapRegion(activeChar.getX(), activeChar.getY());
	}

	public String getClosestTownName(L2Character activeChar)
	{
		return getClosestTown(activeChar).getName();
	}

	public Town getTown(int townId)
	{
		for(Town town : _towns)
			if(town.getTownId() == townId)
				return town;

		return null;
	}
}