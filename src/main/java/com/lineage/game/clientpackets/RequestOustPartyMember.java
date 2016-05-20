package com.lineage.game.clientpackets;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.model.L2Player;

public class RequestOustPartyMember extends L2GameClientPacket
{
	//Format: cS
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
			if(activeChar.getParty().isInDimensionalRift() && !activeChar.getParty().getDimensionalRift().getRevivedAtWaitingRoom().contains(activeChar.getObjectId()))
				activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestOustPartyMember.CantOustInRift", activeChar));
			else if(activeChar.getParty().isInReflection())
				activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestOustPartyMember.CantOustInDungeon", activeChar));
			else
				activeChar.getParty().oustPartyMember(_name);
	}
}