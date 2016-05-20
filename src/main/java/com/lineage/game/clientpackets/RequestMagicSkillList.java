package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SkillList;

public class RequestMagicSkillList extends L2GameClientPacket
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

		sendPacket(new SkillList(activeChar));
	}
}