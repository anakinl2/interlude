package l2d.game.clientpackets;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.lineage.Config;
import l2d.game.model.L2Player;
import l2d.game.model.TradeItem;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.PrivateStoreManageList;
import l2d.game.serverpackets.PrivateStoreMsgSell;
import l2d.game.serverpackets.SystemMessage;

/**
 * Это список вещей которые игрок хочет продать в создаваемом им приватном магазине
 * Старое название SetPrivateStoreListSell
 * Format: cddb, b = array of (ddd)
 */
public class SetPrivateStoreList extends L2GameClientPacket
{
	private int _count;
	private boolean _package;
	private int[] _items; // count * 3

	@Override
	public void readImpl()
	{
		_package = readD() == 1;
		_count = readD();
		// Иначе нехватит памяти при создании массива.
		if(_count * 12 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new int[_count * 3];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 3 + 0] = readD(); //objectId
			_items[i * 3 + 1] = readD(); //count
			_items[i * 3 + 2] = readD(); //price
			if(_items[i * 3 + 1] < 0)
			{
				_items = null;
				break;
			}
		}
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(_items == null || _count <= 0 || !activeChar.checksForShop(false))
		{
			cancelStore(activeChar);
			return;
		}

		TradeItem temp;
		ConcurrentLinkedQueue<TradeItem> listsell = new ConcurrentLinkedQueue<TradeItem>();

		int maxSlots = activeChar.getTradeLimit();

		if(_count > maxSlots)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED));
			cancelStore(activeChar);
			activeChar.sendPacket(new PrivateStoreManageList(activeChar, _package));
			return;
		}

		int count = _count;
		for(int x = 0; x < _count; x++)
		{
			int objectId = _items[x * 3 + 0];
			int cnt = _items[x * 3 + 1];
			int price = _items[x * 3 + 2];

			L2ItemInstance itemToSell = activeChar.getInventory().getItemByObjectId(objectId);

			if(cnt < 1 || itemToSell == null || !itemToSell.canBeTraded(activeChar))
			{
				count--;
				continue;
			}

			// If player sells the enchant scroll he is using, deactivate it
			if(activeChar.getEnchantScroll() != null && itemToSell.getObjectId() == activeChar.getEnchantScroll().getObjectId())
				activeChar.setEnchantScroll(null);

			if(cnt > itemToSell.getIntegerLimitedCount())
				cnt = itemToSell.getIntegerLimitedCount();

			temp = new TradeItem();
			temp.setObjectId(objectId);
			temp.setCount(cnt);
			temp.setOwnersPrice(price);
			temp.setItemId(itemToSell.getItemId());
			temp.setEnchantLevel(itemToSell.getEnchantLevel());

			listsell.add(temp);
		}

		if(count != 0)
		{
			activeChar.setSellList(listsell);
			activeChar.setPrivateStoreType(_package ? L2Player.STORE_PRIVATE_SELL_PACKAGE : L2Player.STORE_PRIVATE_SELL);
			activeChar.broadcastUserInfo(true);
			activeChar.broadcastPacket(new PrivateStoreMsgSell(activeChar));
			activeChar.sitDown();
		}
		else
			cancelStore(activeChar);
	}

	private void cancelStore(L2Player activeChar)
	{
		activeChar.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		activeChar.broadcastUserInfo(true);
		activeChar.getBuyList().clear();
		if(activeChar.isInOfflineMode() && Config.SERVICES_OFFLINE_TRADE_KICK_NOT_TRADING)
		{
			activeChar.setOfflineMode(false);
			activeChar.logout(false, false, true);
			activeChar.getNetConnection().disconnectOffline();
		}
	}
}