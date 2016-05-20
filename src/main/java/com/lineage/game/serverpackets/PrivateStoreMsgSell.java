package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;

public class PrivateStoreMsgSell extends L2GameServerPacket
{
	private final int char_obj_id;
	private final String store_name;
	private boolean WholeMsg;

	/**
	 * Название личного магазина продажи
	 * @param player
	 */
	public PrivateStoreMsgSell(L2Player player)
	{
		WholeMsg = player.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE;
		char_obj_id = player.getObjectId();
		store_name = player.getTradeList() == null ? "" : player.getTradeList().getSellStoreName();
	}

	public PrivateStoreMsgSell(L2Player player, boolean _WholeMsg)
	{
		this(player);
		WholeMsg = _WholeMsg;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9c);
		writeD(char_obj_id);
		writeS(store_name);
	}

	@Override
	public String getType()
	{
		return WholeMsg ? "[S] FE:80 PrivateStoreWholeMsgSell" : "[S] A2 PrivateStoreMsgSell";
	}
}