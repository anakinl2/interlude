package com.lineage.game.clientpackets;

import com.lineage.game.serverpackets.CharacterSelectionInfo;

public class GotoLobby extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		CharacterSelectionInfo cl = new CharacterSelectionInfo(getClient().getLoginName(), getClient().getSessionId().playOkID1);
		sendPacket(cl);
	}
}