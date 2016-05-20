package com.lineage.game.serverpackets;

import com.lineage.ext.network.SendablePacket;
import com.lineage.game.network.L2GameClient;

public abstract class L2GameServerPacket extends SendablePacket<L2GameClient>
{
	@Override
	protected void write()
	{
		try
		{
			writeImpl();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}

	@Override
	public void runImpl()
	{}

	protected abstract void writeImpl();

	protected final static int EXTENDED_PACKET = 0xFE;

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

	public String getType()
	{
		return "[S] " + getClass().getSimpleName();
	}
}