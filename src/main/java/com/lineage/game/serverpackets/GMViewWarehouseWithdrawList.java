package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2ItemInstance.ItemClass;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.L2Weapon;

public class GMViewWarehouseWithdrawList extends L2GameServerPacket
{
	private final L2ItemInstance[] _items;
	private String _charName;
	private int _charAdena;

	public GMViewWarehouseWithdrawList(L2Player cha)
	{
		_charName = cha.getName();
		_charAdena = cha.getAdena();
		_items = cha.getWarehouse().listItems(ItemClass.ALL);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x95);
		writeS(_charName);
		writeD(_charAdena);
		writeH(_items.length);

		for(L2ItemInstance temp : _items)
		{
			writeH(temp.getItem().getType1());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getIntegerLimitedCount());
			writeH(temp.getItem().getType2ForPackets());
			writeH(temp.getCustomType1());

			if(temp.isEquipable())
			{
				writeD(temp.getBodyPart());
				writeH(temp.getEnchantLevel());

				if(temp.getItem().getType2() == L2Item.TYPE2_WEAPON)
				{
					writeH(((L2Weapon) temp.getItem()).getSoulShotCount());
					writeH(((L2Weapon) temp.getItem()).getSpiritShotCount());
				}
				else
				{
					writeH(0);
					writeH(0);
				}

				if(temp.isAugmented())
				{
					writeD(temp.getAugmentationId() & 0x0000FFFF);
					writeD(temp.getAugmentationId() >> 16);
				}
				else
				{
					writeD(0);
					writeD(0);
				}

			}

			writeD(temp.isShadowItem() ? temp.getLifeTimeRemaining() : -1);
			writeD(temp.isTemporalItem() ? temp.getLifeTimeRemaining() : 0x00); // limited time item life remaining
		}
	}
}