package com.lineage.game.serverpackets;

import java.util.ArrayList;

import javolution.util.FastList;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2TradeList;
import com.lineage.game.model.instances.L2ItemInstance;

public class BuyList extends L2GameServerPacket
{
	private int _listId; //TODO: long
	private FastList<L2ItemInstance> _list;
	private int _money;
	private double _TaxRate = 0;

	public BuyList(L2TradeList list, L2Player activeChar)
	{
		_listId = list.getListId();
		_list = list.getItems();
		_money = activeChar.getAdena();
		activeChar.setBuyListId(_listId);
	}

	public BuyList(L2TradeList list, L2Player activeChar, double taxRate)
	{
		_listId = list.getListId();
		_list = list.getItems();
		_money = activeChar.getAdena();
		_TaxRate = taxRate;
		activeChar.setBuyListId(_listId);
	}

	public BuyList(ArrayList<L2ItemInstance> lst, int listId, L2Player activeChar)
	{
		_listId = listId;
		_list = new FastList<L2ItemInstance>(lst);
		_money = activeChar.getAdena();
		activeChar.setBuyListId(_listId);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x11);
		writeD(_money); // current money
		writeD(_listId);

		writeH(_list.size());

		for(FastList.Node<L2ItemInstance> n = _list.head(), end = _list.tail(); (n = n.getNext()) != end;)
		{
			L2ItemInstance item = n.getValue();
			if(item.getCountToSell() == 0 && item.getMaxCountToSell() != 0)
				continue;

			writeH(item.getItem().getType1()); // item type1
			writeD(item.getObjectId());
			writeD(item.getItemId());

			// А не пора ли обновить количество лимитированных предметов в трейд листе?
			if(item.getCountToSell() < item.getMaxCountToSell() && item.getLastRechargeTime() + item.getRechargeTime() < System.currentTimeMillis() / 60000)
			{
				item.setLastRechargeTime((int) (System.currentTimeMillis() / 60000));
				item.setCountToSell(item.getMaxCountToSell());
			}

			writeD(item.getCountToSell()); // max amount of items that a player can buy at a time (with this itemid)
			writeH(item.getItem().getType2ForPackets()); // item type2
			writeH(item.getCustomType1()); // ?
			writeD(item.getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
			writeH(item.getEnchantLevel()); // enchant level
			writeH(item.getCustomType2()); // ?
			writeH(0x00); // unknown
			writeD((int) (item.getPriceToSell() * (1 + _TaxRate)));
		}
	}
}