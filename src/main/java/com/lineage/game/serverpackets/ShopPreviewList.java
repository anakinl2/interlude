package com.lineage.game.serverpackets;

import java.util.ArrayList;

import javolution.util.FastList;
import com.lineage.game.model.L2TradeList;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.templates.L2Item;

public class ShopPreviewList extends L2GameServerPacket
{
	private int _listId;
	private L2ItemInstance[] _list;
	private int _money;
	private int _expertise;

	public ShopPreviewList(L2TradeList list, int currentMoney, int expertiseIndex)
	{
		_listId = list.getListId();
		FastList<L2ItemInstance> lst = list.getItems();
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
		_expertise = expertiseIndex;
	}

	public ShopPreviewList(ArrayList<L2ItemInstance> lst, int listId, int currentMoney)
	{
		_listId = listId;
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xef);

		writeD(0x13c0); //?
		writeD(_money);
		writeD(_listId);

		int newlength = 0;
		for(L2ItemInstance item : _list)
			if(item.getItem().getCrystalType().ordinal() <= _expertise && item.isEquipable())
				newlength++;
		writeH(newlength);

		for(L2ItemInstance item : _list)
			if(item.getItem().getCrystalType().ordinal() <= _expertise && item.isEquipable())
			{
				writeD(item.getItemId());
				writeH(item.getItem().getType2ForPackets()); // item type2
				writeH(item.getItem().getType1() == L2Item.TYPE1_ITEM_QUESTITEM_ADENA ? 0x00 : item.getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
				writeD(10);
			}
	}
}