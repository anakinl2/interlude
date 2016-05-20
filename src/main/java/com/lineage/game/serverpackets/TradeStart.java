package com.lineage.game.serverpackets;

import java.util.ArrayList;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;

//0x2e TradeStart   d h (h dddhh dhhh)
public class TradeStart extends L2GameServerPacket
{
	private ArrayList<L2ItemInstance> _tradelist = new ArrayList<L2ItemInstance>();
	private boolean can_writeImpl = false;
	private int requester_obj_id;

	public TradeStart(L2Player me)
	{
		if(me == null)
			return;

		if(me.getTransactionRequester() == null)
			return;

		requester_obj_id = me.getTransactionRequester().getObjectId();

		L2ItemInstance[] inventory = me.getInventory().getItems();
		for(L2ItemInstance item : inventory)
			if(item != null && item.canBeTraded(me))
				_tradelist.add(item);

		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0x1E);
		writeD(requester_obj_id);
		int count = _tradelist.size();
		writeH(count);//count??

		for(L2ItemInstance temp : _tradelist)
		{
			writeH(temp.getItem().getType1()); // item type1
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getIntegerLimitedCount());
			writeH(temp.getItem().getType2()); // item type2
			writeH(0x00); // ?

			writeD(temp.getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
			writeH(temp.getEnchantLevel()); // enchant level
			writeH(0x00); // ?
			writeH(0x00);
		}
	}
}