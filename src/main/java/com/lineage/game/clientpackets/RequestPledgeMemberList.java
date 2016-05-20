package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.PledgeShowMemberListAll;

public class RequestPledgeMemberList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestPledgeMemberList");
		L2Clan clan = activeChar.getClan();
		if(clan != null)
			activeChar.sendPacket(new PledgeShowMemberListAll(clan, activeChar));
	}
}