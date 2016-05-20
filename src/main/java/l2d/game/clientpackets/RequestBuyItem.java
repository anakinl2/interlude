package l2d.game.clientpackets;

import java.util.ArrayList;
import java.util.logging.Logger;

import l2d.Config;
import l2d.ext.scripts.Functions;
import l2d.game.TradeController;
import l2d.game.cache.Msg;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2TradeList;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2CastleChamberlainInstance;
import l2d.game.model.instances.L2ClanHallManagerInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2MercManagerInstance;
import l2d.game.model.instances.L2MerchantInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.ItemList;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.util.Files;
import l2d.util.Log;
import l2d.util.Util;

/**
 * format: cddb, b - array of (dd)
 */
public class RequestBuyItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestBuyItem.class.getName());

	private int _listId;
	private int _count;
	private int[] _items; // count*2

	@Override
	public void readImpl()
	{
		_listId = readD();
		_count = readD();
		if(_count * 8 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new int[_count * 2];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 2 + 0] = readD();
			_items[i * 2 + 1] = readD();
			if(_items[i * 2 + 1] < 0)
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

		if(_items == null || _count == 0)
			return;

		if(activeChar.isOutOfControl())
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getKarma() > 0 && !activeChar.isGM())
		{
			activeChar.sendActionFailed();
			return;
		}

		// Проверяем, не подменили ли id
		if(activeChar.getBuyListId() != _listId)
		{
			Util.handleIllegalPlayerAction(activeChar, "RequestBuyItem[100]", "Tried to buy from buylist: " + _listId, 1);
			return;
		}

		L2NpcInstance npc = activeChar.getLastNpc();

		if("buy".equalsIgnoreCase(activeChar.getLastBbsOperaion()))
			activeChar.setLastBbsOperaion(null);
		else
		{
			boolean isValidMerchant = npc instanceof L2ClanHallManagerInstance || npc instanceof L2MerchantInstance || npc instanceof L2MercManagerInstance || npc instanceof L2CastleChamberlainInstance;
			if(!activeChar.isGM() && (npc == null || !isValidMerchant || !activeChar.isInRange(npc.getLoc(), L2Character.INTERACTION_DISTANCE)))
			{
				activeChar.sendActionFailed();
				return;
			}
		}

		boolean can = false;
		for(L2NpcInstance npcs : activeChar.getAroundNpc(200, 200))
		{
			if(npcs.getNpcId() == npc.getNpcId())
				can = true;
		}

		if(!can)
		{
			activeChar.sendMessage("To far from npc.");
			return;
		}
		
		L2NpcInstance merchant = null;
		if(npc != null && (npc instanceof L2MerchantInstance || npc instanceof L2ClanHallManagerInstance)) // TODO расширить список?
			merchant = npc;

		ArrayList<L2ItemInstance> items = new ArrayList<L2ItemInstance>(_count);
		for(int i = 0; i < _count; i++)
		{
			short itemId = (short) _items[i * 2 + 0];
			int cnt = _items[i * 2 + 1];
			if(cnt <= 0)
			{
				activeChar.sendActionFailed();
				return;
			}

			L2ItemInstance inst = ItemTable.getInstance().createItem(itemId);
			if(inst == null)
			{
				activeChar.sendActionFailed();
				return;
			}

			if(!inst.isStackable() && cnt != 1)
			{
				activeChar.sendActionFailed();
				return;
			}

			inst.setCount(cnt);
			items.add(inst);
		}

		long finalLoad = 0;
		int finalCount = activeChar.getInventory().getSize();
		int needsSpace = 2;
		int weight = 0;
		int currentMoney = activeChar.getAdena();

		int itemId;
		long cnt;
		int price;
		long subTotal = 0;
		double tax = 0;
		double taxRate = 0;

		if(merchant != null && merchant.getCastle() != null)
			taxRate = merchant.getCastle().getTaxRate();

		for(int i = 0; i < items.size(); i++)
		{
			itemId = items.get(i).getItemId();
			cnt = items.get(i).getIntegerLimitedCount();
			needsSpace = 2;

			if(cnt < 0 || cnt > Integer.MAX_VALUE)
			{
				sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED));
				Util.handleIllegalPlayerAction(activeChar, "RequestBuyItem[157]", "overflow exploit, count to buy: " + cnt + ", item to buy: " + itemId + ", merchant: " + merchant, 1);

				for(L2ItemInstance item : items)
					L2World.removeObject(item);

				activeChar.sendActionFailed();
				return;
			}
			if(items.get(i).getItem().isStackable())
			{
				needsSpace = 1;
				if(activeChar.getInventory().getItemByItemId(itemId) != null)
					needsSpace = 0;
			}
			L2TradeList list = TradeController.getInstance().getBuyList(_listId);
			if(list == null)
			{
				Log.add("tried to buy from non-exist list " + _listId, "errors", activeChar);
				activeChar.sendActionFailed();
				return;
			}
			price = list.getPriceForItemId(itemId);
			if(itemId >= 3960 && itemId <= 4921)
				price *= Config.RATE_SIEGE_GUARDS_PRICE;

			if(price == 0 && !activeChar.getPlayerAccess().UseGMShop)
			{
				Util.handleIllegalPlayerAction(activeChar, "RequestBuyItem[191]", "Tried to buy zero price item, list " + _listId + " item " + itemId, 0);

				for(L2ItemInstance item : items)
					L2World.removeObject(item);

				activeChar.sendMessage("Error: zero-price item! Please notify GM.");

				activeChar.sendActionFailed();
				return;
			}

			weight = items.get(i).getItem().getWeight();
			if(price < 0)
			{
				_log.warning("ERROR, no price found. Wrong buylist?");

				for(L2ItemInstance item : items)
					L2World.removeObject(item);

				activeChar.sendActionFailed();
				return;
			}

			subTotal += cnt * price; // Before tax
			tax = subTotal * taxRate;
			if(subTotal + tax > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(activeChar, "RequestBuyItem[227]", "overflow exploit in total cost: " + (subTotal + tax) + ", item to buy: " + itemId + ", merchant: " + merchant, 1);

				for(L2ItemInstance item : items)
					L2World.removeObject(item);

				activeChar.sendActionFailed();
				return;
			}
			if(subTotal + tax < 0)
			{
				activeChar.illegalAction("213: Tried to purchase negative " + (subTotal + tax) + " adena worth of goods.", 10000);
				Util.handleIllegalPlayerAction(activeChar, "RequestBuyItem[238]", "Tried to purchase negative " + (subTotal + tax) + " adena, item to buy: " + itemId + ", merchant: " + merchant, 1);
				return;
			}
			finalLoad += cnt * weight;
			if(finalLoad > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(activeChar, "RequestBuyItem[244]", "tried an overflow exploit finalLoad: " + finalLoad + ", item to buy: " + itemId + ", merchant: " + merchant, 0);

				for(L2ItemInstance item : items)
					L2World.removeObject(item);

				activeChar.sendActionFailed();
				return;
			}
			if(finalLoad < 0)
			{
				activeChar.illegalAction("254: Tried to purchase negative " + finalLoad + " adena worth of goods.", 10000);
				Util.handleIllegalPlayerAction(activeChar, "RequestBuyItem[255]", "Tried to purchase negative " + finalLoad + " adena, item to buy: " + itemId + ", merchant: " + merchant, 1);
				return;
			}
			if(needsSpace == 2)
				finalCount += cnt;
			else if(needsSpace == 1)
				finalCount += 1;
		}

		if(subTotal + tax > currentMoney || subTotal < 0 || currentMoney <= 0)
		{
			sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);

			for(L2ItemInstance item : items)
				L2World.removeObject(item);

			activeChar.sendActionFailed();
			return;
		}

		if(!activeChar.getInventory().validateWeight(finalLoad))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);

			for(L2ItemInstance item : items)
				L2World.removeObject(item);

			activeChar.sendActionFailed();
			return;
		}

		if(!activeChar.getInventory().validateCapacity(finalCount))
		{
			sendPacket(Msg.YOUR_INVENTORY_IS_FULL);

			for(L2ItemInstance item : items)
				L2World.removeObject(item);

			activeChar.sendActionFailed();
			return;
		}

		// Для магазинов с ограниченным количеством товара число продаваемых предметов уменьшаем после всех проверок
		for(int i = 0; i < items.size(); i++)
		{
			itemId = items.get(i).getItemId();
			cnt = items.get(i).getIntegerLimitedCount();
			L2TradeList list = TradeController.getInstance().getBuyList(_listId);
			L2ItemInstance ic = list.getItemByItemId(itemId);

			if(ic != null && ic.isCountLimited())
				if(cnt > ic.getCountToSell())
				{
					if(ic.getLastRechargeTime() + ic.getRechargeTime() <= System.currentTimeMillis() / 60000)
					{
						ic.setLastRechargeTime((int) (System.currentTimeMillis() / 60000));
						ic.setCountToSell(ic.getMaxCountToSell());
					}
					else
						continue;
				}
				else
					ic.setCountToSell((int) (ic.getCountToSell() - cnt));
		}

		activeChar.reduceAdena((int) (subTotal + tax));
		for(L2ItemInstance item : items)
		{
			Log.LogItem(activeChar, merchant, Log.BuyItem, item);
			activeChar.getInventory().addItem(item);
		}

		// Add tax to castle treasury if not owned by npc clan
		if(merchant != null && merchant.getCastle() != null && merchant.getCastle().getOwnerId() > 0 && merchant.getReflection().getId() == 0)
		{
			merchant.getCastle().addToTreasury((int) tax, true, false);
			Log.add(merchant.getCastle().getName() + "|" + (int) tax + "|BuyItem", "treasury");
		}

		sendPacket(new ItemList(activeChar, true));
		activeChar.sendChanges();

		if(merchant != null)
			if(Files.read("data/html/merchant/" + merchant.getNpcId() + "-b.htm") != null)
				Functions.show("data/html/merchant/" + merchant.getNpcId() + "-b.htm", activeChar);
			else
				activeChar.doInteract(merchant);
	}
}