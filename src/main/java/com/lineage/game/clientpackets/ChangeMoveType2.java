package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;

public final class ChangeMoveType2 extends L2GameClientPacket
{
	private boolean _typeRun;

	@Override
	protected void readImpl()
	{
		_typeRun = readD() == 1;
	}

	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		if(_typeRun)
			activeChar.setRunning();
		else
			activeChar.setWalking();
	}
}
