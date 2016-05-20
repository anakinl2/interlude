package com.lineage.auth.gameservercon.gspackets;

import com.lineage.auth.gameservercon.AttGS;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 19:21:30
 */
public class BlowFishKey extends ClientBasePacket
{
	public BlowFishKey(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		int keyLength = readD();
		if(keyLength == 0)
		{
			getGameServer().initBlowfish(null);
			return;
		}

		byte[] data = readB(keyLength);
		try
		{
			data = getGameServer().RSADecrypt(data);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		getGameServer().initBlowfish(data);
	}
}
