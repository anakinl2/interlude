package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Clan.RankPrivs;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.PledgePowerGradeList;

public class RequestPledgePowerGradeList extends L2GameClientPacket
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
			activeChar.sendMessage("RequestPledgePowerGradeList");
		L2Clan clan = activeChar.getClan();
		if(clan != null)
		{
			RankPrivs[] privs = clan.getAllRankPrivs();
			activeChar.sendPacket(new PledgePowerGradeList(privs));
		}
	}
}