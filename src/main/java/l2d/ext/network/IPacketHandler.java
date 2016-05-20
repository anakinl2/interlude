package l2d.ext.network;

import java.nio.ByteBuffer;

public interface IPacketHandler<T extends MMOClient<?>>
{
	public ReceivablePacket<T> handlePacket(ByteBuffer buf, T client);
}
