package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.model.L2Player;

/**
 * This class handles all GM commands triggered by //command
 */
public class SendBypassBuildCmd extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(SendBypassBuildCmd.class.getName());
	// format: cS
	public static int GM_MESSAGE = 9;
	public static int ANNOUNCEMENT = 10;

	private String _command;

	@Override
	public void readImpl()
	{
		_command = readS();

		if(_command != null)
			_command = _command.trim();

	}

	@Override
	public void runImpl()
	{
		if(Config.DEBUG)
			_log.info("Got command '" + _command + "'");

		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		String cmd = _command;

		if(!cmd.contains("admin_"))
			cmd = "admin_" + cmd;

		AdminCommandHandler.getInstance().useAdminCommandHandler(activeChar, cmd);
	}
}