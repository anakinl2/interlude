package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.util.Log;

/**
 * Format: (c) Sd
 */
public class RequestPetition extends L2GameClientPacket
{
	private String _text;
	private int _type;

	@Override
	public void readImpl()
	{
		_text = readS();
		_type = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		Log.LogPetition(activeChar, _type, _text);
	}
}