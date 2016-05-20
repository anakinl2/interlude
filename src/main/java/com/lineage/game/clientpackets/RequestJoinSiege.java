package com.lineage.game.clientpackets;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.instancemanager.CastleManager;
import com.lineage.game.instancemanager.ClanHallManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.residence.Castle;
import com.lineage.game.model.entity.residence.ClanHall;
import com.lineage.game.model.entity.residence.Residence;

public class RequestJoinSiege extends L2GameClientPacket
{
	// format: cddd

	private int _id;
	private boolean _isAttacker;
	private boolean _isJoining;

	@Override
	public void readImpl()
	{
		_id = readD();
		_isAttacker = readD() == 1;
		_isJoining = readD() == 1;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || !activeChar.isClanLeader())
			return;

		Residence siegeUnit = CastleManager.getInstance().getCastleByIndex(_id);

		if(siegeUnit == null)
			siegeUnit = ClanHallManager.getInstance().getClanHall(_id);

		if(siegeUnit != null && siegeUnit.getSiege() != null)
		{
			if(_isJoining)
			{
				if(siegeUnit instanceof Castle)
					for(Castle temp : CastleManager.getInstance().getCastles().values())
						if(temp.getSiege().checkIsClanRegistered(activeChar.getClanId()))
						{
							activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestJoinSiege.AlreadyRegistered", activeChar).addString(temp.getName()));
							return;
						}
				if(siegeUnit instanceof ClanHall)
					for(ClanHall temp : ClanHallManager.getInstance().getClanHalls().values())
						if(temp.getSiege() != null && temp.getSiege().checkIsClanRegistered(activeChar.getClanId()))
						{
							activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestJoinSiege.AlreadyRegistered", activeChar).addString(temp.getName()));
							return;
						}

				if(_isAttacker)
					siegeUnit.getSiege().registerAttacker(activeChar);
				else
					siegeUnit.getSiege().registerDefender(activeChar);
			}
			else
				siegeUnit.getSiege().removeSiegeClan(activeChar);

			siegeUnit.getSiege().listRegisterClan(activeChar);
		}
	}
}