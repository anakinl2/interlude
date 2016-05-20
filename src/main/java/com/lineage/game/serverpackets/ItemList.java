package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;

public class ItemList extends L2GameServerPacket
{
	private final L2ItemInstance[] _items;
	private final boolean _showWindow;

	public ItemList(L2Player cha, boolean showWindow)
	{
		_items = cha.getInventory().getItems();
		_showWindow = showWindow;
	}

	public ItemList(L2ItemInstance[] items, boolean showWindow)
	{
		_items = items;
		_showWindow = showWindow;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x1b);
		writeH(_showWindow ? 1 : 0);
		writeH(_items.length);

		for(L2ItemInstance temp : _items)
		{
			writeH(temp.getItem().getType1()); // item type1
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getIntegerLimitedCount());
			writeH(temp.getItem().getType2()); // item type2
			writeH(temp.getCustomType1()); // item type3
			writeH(temp.isEquipped() ? 0x01 : 0x00);
			writeD(temp.getItem().getBodyPart());
			writeH(temp.getEnchantLevel()); // enchant level
			writeH(temp.getCustomType2()); // item type3
			writeD(temp.getAugmentationId());
			writeD(temp.isShadowItem() ? temp.getLifeTimeRemaining() : -1);
		}
	}
}