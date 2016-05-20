package com.lineage.game.clientpackets;

import com.lineage.Config;
import com.lineage.game.cache.Msg;
import com.lineage.game.instancemanager.CastleManorManager;
import com.lineage.game.instancemanager.CastleManager;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.residence.Castle;
import com.lineage.game.model.instances.L2ManorManagerInstance;
import com.lineage.game.serverpackets.StatusUpdate;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;
import com.lineage.util.Log;
import com.lineage.util.Util;

/**
 * Format: cdd[dd]
 * c // id (0xC5)
 * d // manor id
 * d // seeds to buy
 * [
 * d // seed id
 * d // count
 * ]
 */
public class RequestBuySeed extends L2GameClientPacket
{
	private int _count, _manorId;

	private int[] _items; // size _count * 2

	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_count = readD();

		if(_count > Short.MAX_VALUE || _count <= 0 || _count * 8 < _buf.remaining())
		{
			_count = 0;
			return;
		}

		_items = new int[_count * 2];

		for(int i = 0; i < _count; i++)
		{
			int itemId = readD();
			_items[i * 2 + 0] = itemId;
			long cnt = readD();
			if(cnt > Integer.MAX_VALUE || cnt < 1)
			{
				_count = 0;
				_items = null;
				return;
			}
			_items[i * 2 + 1] = (int) cnt;
		}
	}

	@Override
	protected void runImpl()
	{
		int totalPrice = 0;
		int slots = 0;
		int totalWeight = 0;

		L2Player player = getClient().getActiveChar();
		if(player == null)
			return;

		if(_count < 1)
		{
			player.sendActionFailed();
			return;
		}

		L2Object target = player.getTarget();

		if(!(target instanceof L2ManorManagerInstance))
			target = player.getLastNpc();

		if(!(target instanceof L2ManorManagerInstance))
			return;

		Castle castle = CastleManager.getInstance().getCastleByIndex(_manorId);

		for(int i = 0; i < _count; i++)
		{
			int seedId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];
			int price = 0;
			int residual = 0;

			CastleManorManager.SeedProduction seed = castle.getSeed(seedId, CastleManorManager.PERIOD_CURRENT);
			price = seed.getPrice();
			residual = seed.getCanProduce();

			if(price <= 0)
				return;

			if(residual < count)
				return;

			totalPrice += count * price;

			L2Item template = ItemTable.getInstance().getTemplate(seedId);
			totalWeight += count * template.getWeight();
			if(!template.isStackable())
				slots += count;
			else if(player.getInventory().getItemByItemId(seedId) == null)
				slots++;
		}

		if(totalPrice > Integer.MAX_VALUE)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.", "", Config.DEFAULT_PUNISH);
			return;
		}

		if(!player.getInventory().validateWeight(totalWeight))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return;
		}

		if(!player.getInventory().validateCapacity(slots))
		{
			sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			return;
		}

		// Charge buyer
		if(totalPrice < 0 || player.getAdena() < totalPrice)
		{
			sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return;
		}

		player.reduceAdena(totalPrice);

		// Adding to treasury for Manor Castle
		castle.addToTreasuryNoTax(totalPrice, false, true);
		Log.add(castle.getName() + "|" + totalPrice + "|BuySeed", "treasury");

		// Proceed the purchase
		for(int i = 0; i < _count; i++)
		{
			int seedId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];
			if(count < 0)
				count = 0;

			// Update Castle Seeds Amount
			CastleManorManager.SeedProduction seed = castle.getSeed(seedId, CastleManorManager.PERIOD_CURRENT);
			seed.setCanProduce(seed.getCanProduce() - count);
			if(Config.MANOR_SAVE_ALL_ACTIONS)
				CastleManager.getInstance().getCastleByIndex(_manorId).updateSeed(seed.getId(), seed.getCanProduce(), CastleManorManager.PERIOD_CURRENT);

			// Add item to Inventory and adjust update packet
			player.getInventory().addItem(seedId, count, 0, "RequestBuySeed");

			// Send Char Buy Messages
			SystemMessage sm = null;
			sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S);
			sm.addItemName(seedId);
			sm.addNumber(count);
			player.sendPacket(sm);
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}