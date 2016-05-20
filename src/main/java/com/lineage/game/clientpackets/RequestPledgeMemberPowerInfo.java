package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2ClanMember;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.PledgeReceivePowerInfo;

public class RequestPledgeMemberPowerInfo extends L2GameClientPacket
{
	// format: chdS
	@SuppressWarnings("unused")
	private int _not_known;
	private String _target;

	@Override
	public void readImpl()
	{
		_not_known = readD();
		_target = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestPledgeMemberPowerInfo");
		L2Clan clan = activeChar.getClan();
		if(clan != null)
		{
			L2ClanMember cm = clan.getClanMember(_target);
			if(cm != null)
				activeChar.sendPacket(new PledgeReceivePowerInfo(cm));
		}
	}
}