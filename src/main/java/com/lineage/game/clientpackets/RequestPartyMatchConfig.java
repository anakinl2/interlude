package com.lineage.game.clientpackets;

import com.lineage.game.instancemanager.PartyRoomManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.PartyMatchDetail;

public class RequestPartyMatchConfig extends L2GameClientPacket
{
	private int _page;
	private int _region;
	private int _allLevels;

	/**
	 * Format: ddd
	 */
	@Override
	public void readImpl()
	{
		_page = readD();
		_region = readD(); // 0 to 15, or -1
		_allLevels = readD(); // 1 -> all levels, 0 -> only levels matching my level
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		activeChar.setPartyMatchingLevels(_allLevels);
		activeChar.setPartyMatchingRegion(_region);

		PartyRoomManager.getInstance().addToWaitingList(activeChar);

		activeChar.sendPacket(new PartyMatchDetail(_region, _allLevels, _page, activeChar));
	}
}