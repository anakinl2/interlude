package com.lineage.game.clientpackets;

import com.lineage.Config;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.PackageSendableList;

/**
 * Format: cd
 * @author SYS
 */
public class RequestPackageSendableItemList extends L2GameClientPacket
{
	private int _characterObjectId;

	@Override
	public void readImpl()
	{
		_characterObjectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || !activeChar.getPlayerAccess().UseWarehouse)
			return;

		activeChar.tempInvetoryDisable();

		if(Config.DEBUG)
			_log.fine("Showing items to freight");

		activeChar.sendPacket(new PackageSendableList(activeChar, _characterObjectId));
	}
}