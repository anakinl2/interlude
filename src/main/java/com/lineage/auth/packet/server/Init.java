package com.lineage.auth.packet.server;

import com.lineage.auth.L2LoginClient;
import com.lineage.ext.network.SendablePacket;

/**
 * Format: dd b dddd s
 * d: session id
 * d: protocol revision
 * b: 0x90 bytes : 0x80 bytes for the scrambled RSA public key
 * 0x10 bytes at 0x00
 * d: unknow
 * d: unknow
 * d: unknow
 * d: unknow
 * s: blowfish key
 */
public final class Init extends L2LoginServerPacket
{
	private int _sessionId;

	private byte[] _publicKey;
	private byte[] _blowfishKey;

	public Init(L2LoginClient client)
	{
		this(client.getScrambledModulus(), client.getBlowfishKey(), client.getSessionId());
	}

	public Init(byte[] publickey, byte[] blowfishkey, int sessionId)
	{
		_sessionId = sessionId;
		_publicKey = publickey;
		_blowfishKey = blowfishkey;
	}

	/**
	 * @see SendablePacket#write()
	 */
	@Override
	protected void write()
	{
		writeC(0x00); // init packet id

		writeD(_sessionId); // session id
		writeD(0x0000c621); // protocol revision

		writeB(_publicKey); // RSA Public Key

		// unk GG related?
		writeD(0x29DD954E);
		writeD(0x77C39CFC);
		writeD(0x97ADB620);
		writeD(0x07BDE0F7);

		writeB(_blowfishKey); // BlowFish key
		writeC(0x00); // null termination ;)
	}
}
