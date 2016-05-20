package com.lineage.auth.serverpackets;

import com.lineage.ext.network.SendablePacket;

import java.util.logging.Logger;

/**
 * Fromat: d
 * d: response
 */
public final class GGAuth extends L2LoginServerPacket
{
	static Logger _log = Logger.getLogger(GGAuth.class.getName());
	public static int SKIP_GG_AUTH_REQUEST = 0x0b;

	private int _response;

	public GGAuth(int response)
	{
		_response = response;
		// if(Config.LOGIN_DEBUG)_log.warning("Reason Hex: " + Integer.toHexString(response));
	}

	/**
	 * @see SendablePacket#write()
	 */
	@Override
	protected void write()
	{
		writeC(0x0b);
		writeD(_response);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
	}
}
