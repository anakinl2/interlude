package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;

public class ExStorageMaxCount extends L2GameServerPacket
{
	private int _inventory;
	private int _warehouse;
	private int _freight;
	private int _privateSell;
	private int _privateBuy;
	private int _recipeDwarven;
	private int _recipeCommon;

	public ExStorageMaxCount(L2Player player)
	{
		_inventory = player.getInventoryLimit();
		_warehouse = player.getWarehouseLimit();
		_freight = player.getFreightLimit();
		_privateBuy = _privateSell = player.getTradeLimit();
		_recipeDwarven = player.getDwarvenRecipeLimit();
		_recipeCommon = player.getCommonRecipeLimit();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x2e);

		writeD(_inventory);
		writeD(_warehouse);
		writeD(_freight);
		writeD(_privateSell);
		writeD(_privateBuy);
		writeD(_recipeDwarven);
		writeD(_recipeCommon);
	}
}