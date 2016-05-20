package l2d.game.serverpackets;

import com.lineage.ext.network.MMOConnection;
import l2d.game.clientpackets.L2GameClientPacket;

public class WrappedMessage extends L2GameServerPacket
{
	final byte[] data;

	public WrappedMessage(byte[] data, MMOConnection<?> con)
	{
		this.data = data;
	}

	public int size()
	{
		return data.length + 2;
	}

	public byte[] getData()
	{
		return data;
	}

	public L2GameClientPacket getClientMsg()
	{
		return null;
	}

	@Override
	protected final void writeImpl()
	{
		writeB(data);
	}

	@Override
	public String getType()
	{
		return null;
	}
}