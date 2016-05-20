package com.lineage.game.serverpackets;

import java.util.logging.Logger;

import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;

/**
 * Пример:
 * 08
 * a5 04 31 48 ObjectId
 * 00 00 00 7c unk
 *
 * format  d
 */
public class DeleteObject extends L2GameServerPacket
{
	private static Logger _log = Logger.getLogger(DeleteObject.class.getName());

	private int _objectId;

	public DeleteObject(L2Object obj)
	{
		_objectId = obj.getObjectId();
	}

	@Override
	final public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar != null && activeChar.getObjectId() == _objectId)
		{
			_log.warning("Try self.DeleteObject for " + getClient().getActiveChar());
			Thread.dumpStack();
			_objectId = 0;
			return;
		}
	}

	@Override
	protected final void writeImpl()
	{
		if(_objectId == 0)
			return;

		writeC(0x12);
		writeD(_objectId);
		writeD(0x00); // unknown
	}

	@Override
	public String getType()
	{
		return super.getType() + " " + L2World.findObject(_objectId) + " (" + _objectId + ")";
	}
}