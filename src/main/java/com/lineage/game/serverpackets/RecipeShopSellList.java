package com.lineage.game.serverpackets;

import com.lineage.game.model.L2ManufactureItem;
import com.lineage.game.model.L2ManufactureList;
import com.lineage.game.model.L2Player;

/**
 * dddd d(ddd)
 */
public class RecipeShopSellList extends L2GameServerPacket
{
	public int obj_id, curMp, maxMp, buyer_adena;
	private L2ManufactureList createList;

	public RecipeShopSellList(L2Player buyer, L2Player manufacturer)
	{
		obj_id = manufacturer.getObjectId();
		curMp = (int) manufacturer.getCurrentMp();
		maxMp = manufacturer.getMaxMp();
		buyer_adena = buyer.getAdena();
		createList = manufacturer.getCreateList();
	}

	@Override
	protected final void writeImpl()
	{
		if(createList == null)
			return;

		writeC(0xd9);
		writeD(obj_id);
		writeD(curMp);//Creator's MP
		writeD(maxMp);//Creator's MP
		writeD(buyer_adena);
		writeD(createList.size());
		for(L2ManufactureItem temp : createList.getList())
		{
			writeD(temp.getRecipeId());
			writeD(0x00); //unknown
			writeD(temp.getCost());
		}
	}
}