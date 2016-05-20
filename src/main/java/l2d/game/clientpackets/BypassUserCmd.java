package l2d.game.clientpackets;

import l2d.ext.multilang.CustomMessage;
import l2d.game.handler.IUserCommandHandler;
import l2d.game.handler.UserCommandHandler;
import l2d.game.model.L2Player;

/**
 * [C] b3 BypassUserCmd
 * <b>Format:</b> cd <p>
 * Пример пакета по команде /loc:
 * AA 00 00 00 00
 * @author Felixx
 */
public class BypassUserCmd extends L2GameClientPacket
{
	private int _command;

	@Override
	public void readImpl()
	{
		_command = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		IUserCommandHandler handler = UserCommandHandler.getInstance().getUserCommandHandler(_command);

		if(handler == null)
			activeChar.sendMessage(new CustomMessage("common.S1NotImplemented", activeChar).addString(String.valueOf(_command)));
		else
			handler.useUserCommand(_command, activeChar);
	}
}