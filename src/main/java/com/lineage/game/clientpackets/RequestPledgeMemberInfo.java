package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2ClanMember;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.PledgeReceiveMemberInfo;

public class RequestPledgeMemberInfo extends L2GameClientPacket
{
	// format: (ch)dS
	@SuppressWarnings("unused")
	private int _pledgeType;
	private String _target;

	@Override
	public void readImpl()
	{
		_pledgeType = readD();
		_target = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestPledgeMemberInfo");
		L2Clan clan = activeChar.getClan();
		if(clan != null)
		{
			L2ClanMember cm = clan.getClanMember(_target);
			if(cm != null)
				activeChar.sendPacket(new PledgeReceiveMemberInfo(cm));
		}
	}
}