package com.lineage.game.model.entity.olympiad;

import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.L2Zone.ZoneType;

public class OlympiadStadia
{
	private int _olympiadStadiaId = 0;
	private String _Name = "";
	private L2Zone _Zone;

	public OlympiadStadia(int olympiadStadiaId)
	{
		_olympiadStadiaId = olympiadStadiaId;
		loadData();
	}

	private void loadData()
	{
		L2Zone zone = ZoneManager.getInstance().getZoneByIndex(ZoneType.OlympiadStadia, getOlympiadStadiaId(), true);
		if(zone != null)
			_Name = zone.getName();
	}

	public final int getOlympiadStadiaId()
	{
		return _olympiadStadiaId;
	}

	public final String getName()
	{
		return _Name;
	}

	public final L2Zone getZone()
	{
		if(_Zone == null)
			_Zone = ZoneManager.getInstance().getZoneByIndex(ZoneType.OlympiadStadia, getOlympiadStadiaId(), true);
		return _Zone;
	}
}