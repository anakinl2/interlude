package com.lineage.auth.gameservercon.lspackets;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 19:22:50
 */
public class RSAKey extends ServerBasePacket
{
	public RSAKey(byte[] data)
	{
		writeC(0);
		writeB(data);
	}
}
