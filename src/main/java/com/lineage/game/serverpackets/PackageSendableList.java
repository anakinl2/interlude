package com.lineage.game.serverpackets;

import java.util.ArrayList;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.templates.L2Item;

/**
 * Format: dQd[hddQhhdhhhddddddddd]
 */
public class PackageSendableList extends L2GameServerPacket
{
	private int player_obj_id, char_adena;
	private ArrayList<L2ItemInstance> _itemslist = new ArrayList<L2ItemInstance>();

	public PackageSendableList(L2Player cha, int playerObjId)
	{
		player_obj_id = playerObjId;
		char_adena = cha.getAdena();
		for(L2ItemInstance item : cha.getInventory().getItems())
			if(item != null && item.canBeStored(cha, false))
				_itemslist.add(item);
	}

	@Override
	protected final void writeImpl()
	{
		if(player_obj_id == 0)
			return;

		writeC(0xC3);
		writeD(player_obj_id);
		writeD(char_adena);
		writeD(_itemslist.size());
		for(L2ItemInstance temp : _itemslist)
		{
			L2Item item = temp.getItem();
			writeH(item.getType1());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getIntegerLimitedCount());
			writeH(item.getType2());
			writeH(temp.getCustomType1());
			writeD(temp.getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
			writeH(0x00);
			writeD(temp.getObjectId()); // some item identifier later used by client to answer (see RequestPackageSend) not item id nor object id maybe some freight system id??
		}
	}
}