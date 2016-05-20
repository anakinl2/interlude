package com.lineage.game.serverpackets;

public class ExMailArrived extends L2GameServerPacket
{
	private static final String _S__FE_2E_EXMAILARRIVED = "[S] FE:2e ExMailArrived []";
	public static final ExMailArrived STATIC_PACKET = new ExMailArrived();

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x2d);
	}

	@Override
	public String getType()
	{
		return _S__FE_2E_EXMAILARRIVED;
	}
}