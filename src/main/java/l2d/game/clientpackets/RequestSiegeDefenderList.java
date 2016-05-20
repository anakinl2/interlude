package l2d.game.clientpackets;

import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.ClanHallManager;
import l2d.game.model.entity.residence.Residence;
import l2d.game.serverpackets.SiegeDefenderList;

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