package com.lineage.game.serverpackets;

public class ExPutItemResultForVariationMake extends L2GameServerPacket
{
	private int _itemObjId;
	private int _unk1;
	private int _unk2;

	public ExPutItemResultForVariationMake(int itemObjId)
	{
		_itemObjId = itemObjId;
		_unk1 = 1;
		_unk2 = 1;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x52);
		writeD(_itemObjId);
		writeD(_unk1);
		writeD(_unk2);
	}
}