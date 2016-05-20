package com.lineage.game.clientpackets;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.handler.IUserCommandHandler;
import com.lineage.game.handler.UserCommandHandler;
import com.lineage.game.model.L2Player;

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