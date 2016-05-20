package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.mmo.ReceivablePacket;
import com.lineage.game.model.L2Player;
import com.lineage.game.network.L2GameClient;
import com.lineage.game.serverpackets.L2GameServerPacket;

/**
 * Packets received by the game server from clients
 */
public abstract class L2GameClientPacket extends ReceivablePacket<L2GameClient>
{
	protected static Logger _log = Logger.getLogger(L2GameClientPacket.class.getName());

	@Override
	protected boolean read()
	{
		try
		{
			readImpl();
			return true;
		}
		catch(Exception e)
		{
			_log.severe("Client: " + getClient().toString() + " from IP: " + getClient().getIpAddr() + " - Failed reading: " + getType() + "(" + getClass().getName() + ") - L2P Server Version: " + Config.SERVER_VERSION);
			e.printStackTrace();

			handleIncompletePacket();
		}
		return false;
	}

	protected abstract void readImpl() throws Exception;

	@Override
	public void run()
	{
		L2GameClient client = getClient();
		try
		{
			runImpl();
		}
		catch(Exception e)
		{
			_log.severe("Client: " + client.toString() + " from IP: " + client.getIpAddr() + " - Failed running: " + getType() + " - L2P Server Version: " + Config.SERVER_VERSION);
			e.printStackTrace();
			handleIncompletePacket();
		}
		client.can_runImpl = true;
	}

	protected abstract void runImpl() throws Exception;

	protected void sendPacket(L2GameServerPacket gsp)
	{
		getClient().sendPacket(gsp);
	}

	public boolean checkReadArray(int expected_elements, int element_size, boolean _debug)
	{
		int expected_size = expected_elements * element_size;
		boolean result = expected_size < 0 ? false : _buf.remaining() >= expected_size;
		if(!result && _debug)
			_log.severe("Buffer Underflow Risk in [" + getType() + "], Client: " + getClient().toString() + " from IP: " + getClient().getIpAddr() + " - Buffer Size: " + _buf.remaining() + " / Expected Size: " + expected_size);
		return result;
	}

	public boolean checkReadArray(int expected_elements, int element_size)
	{
		return checkReadArray(expected_elements, element_size, true);
	}

	public void handleIncompletePacket()
	{
		L2GameClient client = getClient();

		L2Player activeChar = client.getActiveChar();
		if(activeChar == null)
			_log.warning("Packet not completed. Maybe cheater. IP:" + client.getIpAddr() + ", account:" + client.getLoginName());
		else
			_log.warning("Packet not completed. Maybe cheater. IP:" + client.getIpAddr() + ", account:" + client.getLoginName() + ", character:" + activeChar.getName());

		client.onClientPacketFail();
	}

	public String getType()
	{
		return "[C] " + getClass().getSimpleName();
	}
}