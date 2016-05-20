package com.lineage.game.serverpackets;

import java.util.ArrayList;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;

public class SellList extends L2GameServerPacket
{
	private int _money; //TODO: long
	private ArrayList<L2ItemInstance> _selllist = new ArrayList<L2ItemInstance>();

	/**
	 * Список вещей для продажи в обычный магазин
	 * @param player
	 */
	public SellList(L2Player player)
	{
		_money = player.getAdena();

		for(L2ItemInstance item : player.getInventory().getItems())
			if(item.getItem().isSellable() && item.canBeTraded(player))
				_selllist.add(item);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x10);
		writeD(_money);
		writeD(0); //_listId etc?
		writeH(_selllist.size());

		for(L2ItemInstance item : _selllist)
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getIntegerLimitedCount());
			writeH(item.getItem().getType2ForPackets());
			writeH(item.getCustomType1());
			writeD(item.getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(item.getCustomType2());
			writeH(0x00); // unknown
			writeD(item.getItem().getReferencePrice() / 2);
		}
	}
}