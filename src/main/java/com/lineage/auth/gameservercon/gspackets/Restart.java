package com.lineage.auth.gameservercon.gspackets;

import com.lineage.auth.gameservercon.AttGS;

/**
 * @Author: Death
 * @Date: 15/11/2007
 * @Time: 12:38:39
 */
public class Restart extends ClientBasePacket
{
	public Restart(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		System.exit(2); // Полный рестарт логинсервера.
	}
}
