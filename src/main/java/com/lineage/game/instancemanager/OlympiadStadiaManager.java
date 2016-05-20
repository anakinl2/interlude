package com.lineage.game.instancemanager;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.model.entity.olympiad.OlympiadStadia;

public class OlympiadStadiaManager
{
	protected static Logger _log = Logger.getLogger(OlympiadStadiaManager.class.getName());

	private static OlympiadStadiaManager _instance;

	public static OlympiadStadiaManager getInstance()
	{
		if(_instance == null)
		{
			System.out.println("Initializing OlympiadStadiaManager");
			_instance = new OlympiadStadiaManager();
			_instance.load();
		}
		return _instance;
	}

	private List<OlympiadStadia> _olympiadStadias;

	public void reload()
	{
		getOlympiadStadias().clear();
		load();
	}

	private void load()
	{
		FastList<L2Zone> zones = ZoneManager.getInstance().getZoneByType(ZoneType.OlympiadStadia);
		if(zones.size() == 0)
			System.out.println("Not found zones for Olympiad!!!");
		else
			for(L2Zone zone : zones)
				getOlympiadStadias().add(new OlympiadStadia(zone.getId()));
	}

	public final List<OlympiadStadia> getOlympiadStadias()
	{
		if(_olympiadStadias == null)
			_olympiadStadias = new FastList<OlympiadStadia>();
		return _olympiadStadias;
	}
}
