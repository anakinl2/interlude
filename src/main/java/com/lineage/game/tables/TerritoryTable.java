package com.lineage.game.tables;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.logging.Logger;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.model.L2RoundTerritory;
import com.lineage.game.model.L2Territory;
import com.lineage.game.model.L2World;

public class TerritoryTable
{
	private static Logger _log = Logger.getLogger(TerritoryTable.class.getName());
	private static final TerritoryTable _instance = new TerritoryTable();
	private static HashMap<Integer, L2Territory> _locations;

	public static TerritoryTable getInstance()
	{
		return _instance;
	}

	private TerritoryTable()
	{
		reloadData();
	}

	public L2Territory getLocation(int terr)
	{
		L2Territory t = _locations.get(terr);
		if(t == null)
			_log.warning("Error territory[49] " + terr);
		return t;
	}

	public int[] getRandomPoint(int terr)
	{
		L2Territory t = _locations.get(terr);
		if(t == null)
		{
			_log.warning("Error territory[49] " + terr);
			return new int[3];
		}
		return t.getRandomPoint();
	}

	public int getMinZ(int terr)
	{
		L2Territory t = _locations.get(terr);
		if(t == null)
		{
			_log.warning("Error territory[61] " + terr);
			return 0;
		}
		return t.getZmin();
	}

	public int getMaxZ(int terr)
	{
		L2Territory t = _locations.get(terr);
		if(t == null)
		{
			_log.warning("Error territory[73] " + terr);
			return 0;
		}
		return t.getZmax();
	}

	public void reloadData()
	{
		_locations = new HashMap<Integer, L2Territory>();
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT loc_id, loc_x, loc_y, loc_zmin, loc_zmax, radius FROM `locations`");
			rset = statement.executeQuery();
			while(rset.next())
			{
				int terr = rset.getInt("loc_id");
				if(rset.getInt("radius") > 0)
				{
					if(_locations.get(terr) == null)
					{
						L2RoundTerritory t = new L2RoundTerritory(terr, rset.getInt("loc_x"), rset.getInt("loc_y"), rset.getInt("radius"), rset.getInt("loc_zmin"), rset.getInt("loc_zmax"));
						_locations.put(terr, t);
					}
				}
				else
				{
					if(_locations.get(terr) == null)
					{
						L2Territory t = new L2Territory(terr);
						_locations.put(terr, t);
					}
					_locations.get(terr).add(rset.getInt("loc_x"), rset.getInt("loc_y"), rset.getInt("loc_zmin"), rset.getInt("loc_zmax"));
				}
			}
		}
		catch(Exception e1)
		{
			//problem with initializing spawn, go to next one
			_log.warning("locations couldnt be initialized:" + e1);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		_log.config("TerritoryTable: Loaded " + _locations.size() + " locations");
	}

	public void registerZones()
	{
		int registered = 0;
		for(L2Territory terr : _locations.values())
			if(terr.isWorldTerritory())
			{
				L2World.addTerritory(terr);
				registered++;
			}

		_log.config("TerritoryTable: Added " + registered + " locations to L2World");
	}

	public HashMap<Integer, L2Territory> getLocations()
	{
		return _locations;
	}

	public static void unload()
	{
		_locations.clear();
	}
}