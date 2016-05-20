package l2d.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import l2d.game.cache.CrestCache;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.ExPledgeCrestLarge;

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