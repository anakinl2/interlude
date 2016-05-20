package com.lineage.auth.packet.server;

import com.lineage.auth.L2LoginClient;
import com.lineage.mmo.SendablePacket;

/**
 * @author KenM
 */
public abstract class L2LoginServerPacket extends SendablePacket<L2LoginClient>
{
	@Override
	protected int getHeaderSize()
	{
		return 2;
	}

	@Override
	protected void writeHeader(int dataSize)
	{
		writeH(dataSize + getHeaderSize());
	}
}
