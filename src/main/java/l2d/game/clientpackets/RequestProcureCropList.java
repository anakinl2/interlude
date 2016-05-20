package l2d.game.clientpackets;

import l2d.Config;
import l2d.game.cache.Msg;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.CastleManorManager;
import l2d.game.instancemanager.CastleManorManager.CropProcure;
import l2d.game.model.L2Character;
import l2d.game.model.L2Manor;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2ManorManagerInstance;
import l2d.game.serverpackets.StatusUpdate;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2Item;

/**
 * Format: (ch) d [dddd]
 * d: size
 * [
 * d  obj id
 * d  item id
 * d  manor id
 * d  count
 * ]
 */
public class RequestProcureCropList extends L2GameClientPacket
{
	private int _size;
	private int[] _items; // count*4

	@Override
	protected void readImpl()
	{
		_size = readD();
		if(_size * 16 > _buf.remaining() || _size > Short.MAX_VALUE || _size <= 0)
		{
			_size = 0;
			return;
		}
		_items = new int[_size * 4];
		for(int i = 0; i < _size; i++)
		{
			int objId = readD();
			_items[i * 4 + 0] = objId;
			int itemId = readD();
			_items[i * 4 + 1] = itemId;
			int manorId = readD();
			_items[i * 4 + 2] = manorId;
			int count = readD();
			_items[i * 4 + 3] = count;
		}
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getActiveChar();
		if(player == null)
			return;

		if(_size < 1)
		{
			player.sendActionFailed();
			return;
		}

		L2Object target = player.getTarget();

		if(!(target instanceof L2ManorManagerInstance))
			target = player.getLastNpc();

		if(!player.isGM() && (target == null || !(target instanceof L2ManorManagerInstance) || !player.isInRange(target, L2Character.INTERACTION_DISTANCE)))
			return;

		L2ManorManagerInstance manorManager = (L2ManorManagerInstance) target;

		int currentManorId = manorManager.getCastle().getId();

		// Calculate summary values
		int slots = 0;
		int weight = 0;

		for(int i = 0; i < _size; i++)
		{
			int itemId = _items[i * 4 + 1];
			int manorId = _items[i * 4 + 2];
			int count = _items[i * 4 + 3];

			if(itemId == 0 || manorId == 0 || count == 0)
				continue;
			if(count < 1)
				continue;
			if(count > Integer.MAX_VALUE)
			{
				sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED));
				return;
			}

			try
			{
				CropProcure crop = CastleManager.getInstance().getCastleByIndex(manorId).getCrop(itemId, CastleManorManager.PERIOD_CURRENT);
				int rewardItemId = L2Manor.getInstance().getRewardItem(itemId, crop.getReward());
				L2Item template = ItemTable.getInstance().getTemplate(rewardItemId);
				weight += count * template.getWeight();

				if(!template.isStackable())
					slots += count;
				else if(player.getInventory().getItemByItemId(itemId) == null)
					slots++;
			}
			catch(NullPointerException e)
			{
				continue;
			}
		}

		if(!player.getInventory().validateWeight(weight))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return;
		}

		if(!player.getInventory().validateCapacity(slots))
		{
			sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			return;
		}

		// Proceed the purchase
		for(int i = 0; i < _size; i++)
		{
			int objId = _items[i * 4 + 0];
			int cropId = _items[i * 4 + 1];
			int manorId = _items[i * 4 + 2];
			int count = _items[i * 4 + 3];

			if(objId == 0 || cropId == 0 || manorId == 0 || count == 0)
				continue;

			if(count < 1)
				continue;

			CropProcure crop = null;

			try
			{
				crop = CastleManager.getInstance().getCastleByIndex(manorId).getCrop(cropId, CastleManorManager.PERIOD_CURRENT);
			}
			catch(NullPointerException e)
			{
				continue;
			}
			if(crop == null || crop.getId() == 0 || crop.getPrice() == 0)
				continue;

			int fee = 0; // fee for selling to other manors

			int rewardItem = L2Manor.getInstance().getRewardItem(cropId, crop.getReward());

			if(count > crop.getAmount())
				continue;

			int sellPrice = count * crop.getPrice();
			int rewardPrice = ItemTable.getInstance().getTemplate(rewardItem).getReferencePrice();

			if(rewardPrice == 0)
				continue;

			int rewardItemCount = sellPrice / rewardPrice;
			if(rewardItemCount < 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessage.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(cropId);
				sm.addNumber(count);
				player.sendPacket(sm);
				continue;
			}

			if(manorId != currentManorId)
				fee = sellPrice * 5 / 100; // 5% fee for selling to other manor

			if(player.getInventory().getAdena() < fee)
			{
				SystemMessage sm = new SystemMessage(SystemMessage.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(cropId);
				sm.addNumber(count);
				player.sendPacket(sm);
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				continue;
			}

			// Add item to Inventory and adjust update packet
			L2ItemInstance itemDel = null;
			L2ItemInstance itemAdd = null;
			if(player.getInventory().getItemByObjectId(objId) == null)
				continue;

			// check if player have correct items count
			L2ItemInstance item = player.getInventory().getItemByObjectId(objId);
			if(item.getIntegerLimitedCount() < count)
				continue;

			itemDel = player.getInventory().destroyItem(objId, count, true);
			if(itemDel == null)
				continue;

			if(fee > 0)
				player.getInventory().reduceAdena(fee);
			crop.setAmount(crop.getAmount() - count);
			if(Config.MANOR_SAVE_ALL_ACTIONS)
				CastleManager.getInstance().getCastleByIndex(manorId).updateCrop(crop.getId(), crop.getAmount(), CastleManorManager.PERIOD_CURRENT);

			itemAdd = player.getInventory().addItem(rewardItem, rewardItemCount, 0, "Manor: RequestProcureCropList");
			if(itemAdd == null)
				continue;

			// Send System Messages
			SystemMessage sm = new SystemMessage(SystemMessage.TRADED_S2_OF_CROP_S1);
			sm.addItemName(cropId);
			sm.addNumber(count);
			player.sendPacket(sm);

			if(fee > 0)
			{
				sm = new SystemMessage(SystemMessage.S1_ADENA_HAS_BEEN_PAID_FOR_PURCHASING_FEES);
				sm.addNumber(fee);
				player.sendPacket(sm);
			}

			sm = new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED);
			sm.addItemName(cropId);
			sm.addNumber(count);
			player.sendPacket(sm);

			if(fee > 0)
			{
				sm = new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED);
				sm.addNumber(fee);
				player.sendPacket(sm);
			}

			sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S);
			sm.addItemName(rewardItem);
			sm.addNumber(rewardItemCount);
			player.sendPacket(sm);
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}