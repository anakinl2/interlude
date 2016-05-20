package l2d.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import l2d.game.cache.CrestCache;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.PledgeCrest;

public class RequestPledgeCrest extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPledgeCrest.class.getName());
	// format: cd

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
		//if(activeChar.isGM())
		//activeChar.sendMessage("RequestPledgeCrest");
		if(_crestId == 0)
			return;
		if(Config.DEBUG)
			_log.fine("crestid " + _crestId + " requested");
		byte[] data = CrestCache.getPledgeCrest(_crestId);
		if(data != null)
		{
			PledgeCrest pc = new PledgeCrest(_crestId, data);
			sendPacket(pc);
		}
		else if(Config.DEBUG)
			_log.fine("crest is missing:" + _crestId);
	}
}