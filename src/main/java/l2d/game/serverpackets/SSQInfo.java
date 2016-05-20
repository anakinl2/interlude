package l2d.game.serverpackets;

import l2d.game.model.entity.SevenSigns;

/**
 * [S] 73 SSQInfo
 * @author Felixx
 */
public class SSQInfo extends L2GameServerPacket
{
	private int _state = 0;

	public SSQInfo()
	{
		int compWinner = SevenSigns.getInstance().getCabalHighestScore();
		if(SevenSigns.getInstance().isSealValidationPeriod())
			if(compWinner == SevenSigns.CABAL_DAWN)
				_state = 2;
			else if(compWinner == SevenSigns.CABAL_DUSK)
				_state = 1;
	}

	public SSQInfo(int state)
	{
		_state = state;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xf8);
		if(_state == 2)
			writeH(258);
		else if(_state == 1)
			writeH(257);
		else
			writeH(256);
	}
}