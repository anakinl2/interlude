package l2d.game.clientpackets;

import l2d.Config;
import l2d.game.model.L2Player;

/**
 * [C] C5 ConfirmDlg
 * <b>Format:</b> cddd
 * @author Felixx
 *
 */
public class ConfirmDlg extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _messageId;
	private int _answer;
	private int _requestId;

	@Override
	public void readImpl()
	{
		_messageId = readD();
		_answer = readD();
		_requestId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		switch(_requestId)
		{
			case 1:
				activeChar.summonCharacterAnswer(_answer);
				break;
			case 2:
				activeChar.reviveAnswer(_answer);
				break;
			case 3:
				activeChar.scriptAnswer(_answer);
				break;
			case 4:
				if(Config.WEDDING_ALLOW_WEDDING && activeChar.isEngageRequest())
					activeChar.engageAnswer(_answer);
				break;
		}
	}
}