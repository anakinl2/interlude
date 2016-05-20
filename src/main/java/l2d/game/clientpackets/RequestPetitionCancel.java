package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import com.lineage.util.Log;

public class RequestPetitionCancel extends L2GameClientPacket
{
	private String _text;

	@Override
	public void readImpl()
	{
		_text = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		Log.LogPetition(activeChar, 0, "Cancel: " + _text);
	}
}