package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.game.cache.CrestCache;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.ExPledgeCrestLarge;

public class RequestPledgeCrestLarge extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPledgeCrestLarge.class.getName());
	// format: chd
	private int _crestId;

	@Override
	public void readImpl()
	{
		_crestId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestPledgeCrestLarge");
		if(_crestId == 0)
			return;
		if(Config.DEBUG)
			_log.fine("largecrestid " + _crestId + " requested");
		byte[] data = CrestCache.getPledgeCrestLarge(_crestId);
		if(data != null)
		{
			ExPledgeCrestLarge pcl = new ExPledgeCrestLarge(_crestId, data);
			sendPacket(pcl);
		}
		else if(Config.DEBUG)
			_log.fine("largecrest file is missing:" + _crestId);
	}
}