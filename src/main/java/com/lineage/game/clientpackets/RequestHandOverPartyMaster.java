package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;

public class RequestHandOverPartyMaster extends L2GameClientPacket
{
	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.isInParty() && activeChar.getParty().isLeader(activeChar))
			activeChar.getParty().changePartyLeader(_name);
	}
}