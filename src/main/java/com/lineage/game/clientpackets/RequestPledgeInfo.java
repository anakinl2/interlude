package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.PledgeInfo;
import com.lineage.game.tables.ClanTable;

public class RequestPledgeInfo extends L2GameClientPacket
{
	private int _clanId;

	@Override
	public void readImpl()
	{
		_clanId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		//if(activeChar.isGM())
		//activeChar.sendMessage("RequestPledgeInfo");
		if(_clanId < 10000000)
		{
			activeChar.sendActionFailed();
			return;
		}
		L2Clan clan = ClanTable.getInstance().getClan(_clanId);
		if(clan == null)
		{
			//Util.handleIllegalPlayerAction(activeChar, "RequestPledgeInfo[40]", "Clan data for clanId " + _clanId + " is missing", 1);
			//_log.warning("Host " + getClient().getIpAddr() + " possibly sends fake packets. activeChar: " + activeChar);
			activeChar.sendActionFailed();
			return;
		}

		activeChar.sendPacket(new PledgeInfo(clan));
	}
}