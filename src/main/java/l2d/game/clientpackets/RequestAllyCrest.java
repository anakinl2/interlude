package l2d.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import l2d.game.cache.CrestCache;
import l2d.game.serverpackets.AllianceCrest;

public class RequestAllyCrest extends L2GameClientPacket
{
	// format: cd
	private static Logger _log = Logger.getLogger(RequestAllyCrest.class.getName());

	private int _crestId;

	@Override
	public void readImpl()
	{
		_crestId = readD();
	}

	@Override
	public void runImpl()
	{
		if(_crestId == 0)
			return;
		if(Config.DEBUG)
			_log.fine("allycrestid " + _crestId + " requested");
		byte[] data = CrestCache.getAllyCrest(_crestId);
		if(data != null)
		{
			AllianceCrest ac = new AllianceCrest(_crestId, data);
			sendPacket(ac);
		}
		else if(Config.DEBUG)
			_log.fine("allycrest is missing:" + _crestId);
	}
}