package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SSQStatus;

/**
 * Seven Signs Record Update Request
 * packet type id 0xc8
 * format: cc
 */
public class RequestSSQStatus extends L2GameClientPacket
{
	private int _page;

	@Override
	public void readImpl()
	{
		_page = readC();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		sendPacket(new SSQStatus(activeChar, _page));
	}
}