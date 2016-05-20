package com.lineage.auth.packet.client;

import com.lineage.auth.packet.server.LoginFail;
import com.lineage.Config;
import com.lineage.auth.L2LoginClient.LoginClientState;
import com.lineage.auth.packet.server.GGAuth;
import com.lineage.mmo.ReceivablePacket;

/**
 * @author -Wooden-
 *         Format: ddddd
 */
public class AuthGameGuard extends L2LoginClientPacket
{
	private int _sessionId;
	private int _data1;
	private int _data2;
	private int _data3;
	private int _data4;

	public int getSessionId()
	{
		return _sessionId;
	}

	public int getData1()
	{
		return _data1;
	}

	public int getData2()
	{
		return _data2;
	}

	public int getData3()
	{
		return _data3;
	}

	public int getData4()
	{
		return _data4;
	}

	/**
	 * @see L2LoginClientPacket#readImpl()
	 */
	@Override
	protected boolean readImpl()
	{
		if(getAvaliableBytes() >= 20)
		{
			_sessionId = readD();
			_data1 = readD();
			_data2 = readD();
			_data3 = readD();
			_data4 = readD();
			return true;
		}
		return false;
	}

	/**
	 * @see ReceivablePacket#run()
	 */
	@Override
	public void runImpl()
	{
		if(!Config.LOGIN_GG_CHECK || _sessionId == getClient().getSessionId())
		{
			getClient().setState(LoginClientState.AUTHED_GG);
			getClient().sendPacket(new GGAuth(getClient().getSessionId()));
		}
		else
			getClient().close(LoginFail.LoginFailReason.REASON_ACCESS_FAILED);
	}
}
