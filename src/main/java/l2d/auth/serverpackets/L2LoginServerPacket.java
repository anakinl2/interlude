package l2d.auth.serverpackets;

import l2d.auth.L2LoginClient;
import l2d.ext.network.SendablePacket;

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
