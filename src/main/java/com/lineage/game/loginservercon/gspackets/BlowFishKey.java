package com.lineage.game.loginservercon.gspackets;

import com.lineage.game.loginservercon.AttLS;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 21:39:04
 */
public class BlowFishKey extends GameServerBasePacket
{
	public BlowFishKey(byte[] data, AttLS loginServer)
	{
		writeC(0x00);
		if(data == null || data.length == 0)
		{
			writeD(0);
			return;
		}

		try
		{
			data = loginServer.getRsa().encryptRSA(data);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		writeD(data.length);
		writeB(data);
	}
}