package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Alliance;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ClanTable;

public class RequestDismissAlly extends L2GameClientPacket
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

		L2Clan clan = activeChar.getClan();
		if(clan == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		L2Alliance alliance = clan.getAlliance();
		if(alliance == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS));
			return;
		}

		if(!activeChar.isAllyLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.FEATURE_AVAILABLE_TO_ALLIANCE_LEADERS_ONLY));
			return;
		}

		if(alliance.getMembersCount() > 1)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_DISSOLVE_THE_ALLIANCE));
			return;
		}

		ClanTable.getInstance().dissolveAlly(activeChar);
	}
}