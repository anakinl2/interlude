package com.lineage.game.clientpackets;

import java.util.Collection;
import java.util.logging.Logger;

import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;

public class RequestBlock extends L2GameClientPacket
{
	// format: cd(S)
	private static Logger _log = Logger.getLogger(L2Player.class.getName());

	private final static int BLOCK = 0;
	private final static int UNBLOCK = 1;
	private final static int BLOCKLIST = 2;
	private final static int ALLBLOCK = 3;
	private final static int ALLUNBLOCK = 4;

	private Integer _type;
	private String targetName = null;

	@Override
	public void readImpl()
	{
		_type = readD(); //0x00 - block, 0x01 - unblock, 0x03 - allblock, 0x04 - allunblock

		if(_type == BLOCK || _type == UNBLOCK)
			targetName = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		switch(_type)
		{
			case BLOCK:
				activeChar.addToBlockList(targetName);
				break;
			case UNBLOCK:
				activeChar.removeFromBlockList(targetName);
				break;
			case BLOCKLIST:
				Collection<String> blockList = activeChar.getBlockList();

				if(blockList != null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage._IGNORE_LIST_));

					for(String name : blockList)
						activeChar.sendMessage(name);

					activeChar.sendPacket(new SystemMessage(SystemMessage.__EQUALS__));
				}
				break;
			case ALLBLOCK:
				activeChar.setBlockAll(true);
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOW_BLOCKING_EVERYTHING));
				break;
			case ALLUNBLOCK:
				activeChar.setBlockAll(false);
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NO_LONGER_BLOCKING_EVERYTHING));
				break;
			default:
				_log.info("Unknown 0x0a block type: " + _type);
		}
	}
}