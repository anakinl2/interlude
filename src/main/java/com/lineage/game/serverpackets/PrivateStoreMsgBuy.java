package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;

public class PrivateStoreMsgBuy extends L2GameServerPacket
{
	private int char_obj_id;
	private String store_name;

	/**
	 * Название личного магазина покупки
	 * @param player
	 */
	public PrivateStoreMsgBuy(L2Player player)
	{
		char_obj_id = player.getObjectId();
		store_name = player.getTradeList() == null ? "" : player.getTradeList().getBuyStoreName();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xB9);
		writeD(char_obj_id);
		writeS(store_name);
	}
}