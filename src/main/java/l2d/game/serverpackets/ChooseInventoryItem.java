package l2d.game.serverpackets;

public class ChooseInventoryItem extends L2GameServerPacket
{
	private int ItemID;

	public ChooseInventoryItem(int id)
	{
		ItemID = id;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x6f);
		writeD(ItemID);
	}
}