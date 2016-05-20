package l2d.game.serverpackets;

import java.util.List;

import javolution.util.FastList;
import l2d.game.model.L2TradeList;
import l2d.game.model.instances.L2ItemInstance;

/**
 * Format: c ddh[hdddhhd]
 * c - id (0xE8)
 *
 * d - money
 * d - manor id
 * h - size
 * [
 * h - item type 1
 * d - object id
 * d - item id
 * d - count
 * h - item type 2
 * h
 * d - price
 * ]
 */
public final class BuyListSeed extends L2GameServerPacket
{
	private int _manorId;
	private List<L2ItemInstance> _list = new FastList<L2ItemInstance>();
	private int _money;

	public BuyListSeed(L2TradeList list, int manorId, int currentMoney)
	{
		_money = currentMoney;
		_manorId = manorId;
		_list = list.getItems();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xE8);

		writeD(_money); // current money
		writeD(_manorId); // manor id

		writeH(_list.size()); // list length

		for(L2ItemInstance item : _list)
		{
			writeH(0x04); // item->type1
			writeD(0x00); // objectId
			writeD(item.getItemId()); // item id
			writeD(item.getIntegerLimitedCount()); // item count
			writeH(0x04); // item->type2
			writeH(0x00); // size of [dhhh]
			writeD(item.getPriceToSell()); // price
		}
	}
}