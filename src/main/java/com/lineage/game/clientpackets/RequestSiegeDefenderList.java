package com.lineage.game.clientpackets;

import com.lineage.game.instancemanager.CastleManager;
import com.lineage.game.instancemanager.ClanHallManager;
import com.lineage.game.model.entity.residence.Residence;
import com.lineage.game.serverpackets.SiegeDefenderList;

public class RequestSiegeDefenderList extends L2GameClientPacket
{
	private int _unitId;

	@Override
	public void readImpl()
	{
		_unitId = readD();
	}

	@Override
	public void runImpl()
	{
		Residence unit = CastleManager.getInstance().getCastleByIndex(_unitId);
		if(unit == null)
			unit = ClanHallManager.getInstance().getClanHall(_unitId);
		if(unit != null)
			sendPacket(new SiegeDefenderList(unit));
	}
}