package com.lineage.game.clientpackets;

import java.util.List;

import com.lineage.game.cache.Msg;
import com.lineage.game.instancemanager.CastleManorManager;
import javolution.util.FastList;
import com.lineage.game.model.L2Manor;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2ManorManagerInstance;
import com.lineage.game.serverpackets.StatusUpdate;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;

@SuppressWarnings("unused")
public class RequestProcureCrop extends L2GameClientPacket
{
	// format: cddb
	private int _listId;
	private int _count;
	private int[] _items;
	private List<CastleManorManager.CropProcure> _procureList = new FastList<CastleManorManager.CropProcure>();

	@Override
	protected void readImpl()
	{
		_listId = readD();
		_count = readD();
		if(_count * 12 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_count = 0;
			return;
		}
		_items = new int[_count * 2];
		for(int i = 0; i < _count; i++)
		{
			long servise = readD();
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
		L2Player player = getClient().getActiveChar();
		if(player == null)
			return;

		// Alt game - Karma punishment
		if(player.getKarma() > 0)
			return;

		L2Object target = player.getTarget();

		if(_count < 1)
		{
			player.sendActionFailed();
			return;
		}

		long subTotal = 0;
		int tax = 0;

		// Check for buylist validity and calculates summary values
		int slots = 0;
		int weight = 0;
		L2ManorManagerInstance manor = target != null && target instanceof L2ManorManagerInstance ? (L2ManorManagerInstance) target : null;

		for(int i = 0; i < _count; i++)
		{
			int itemId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];
			int price = 0;
			if(count < 0 || count > Integer.MAX_VALUE)
			{
				sendPacket(Msg.INCORRECT_ITEM_COUNT);
				return;
			}

			L2Item template = ItemTable.getInstance().getTemplate(L2Manor.getInstance().getRewardItem(itemId, manor.getCastle().getCrop(itemId, CastleManorManager.PERIOD_CURRENT).getReward()));
			weight += count * template.getWeight();

			if(!template.isStackable())
				slots += count;
			else if(player.getInventory().getItemByItemId(itemId) == null)
				slots++;
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
		_procureList = manor.getCastle().getCropProcure(CastleManorManager.PERIOD_CURRENT);

		for(int i = 0; i < _count; i++)
		{
			int itemId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];
			if(count < 0)
				count = 0;

			int rewradItemId = L2Manor.getInstance().getRewardItem(itemId, manor.getCastle().getCrop(itemId, CastleManorManager.PERIOD_CURRENT).getReward());

			int rewradItemCount = L2Manor.getInstance().getRewardAmountPerCrop(manor.getCastle().getId(), itemId, manor.getCastle().getCropRewardType(itemId));

			rewradItemCount = count * rewradItemCount;

			// Add item to Inventory and adjust update packet
			L2ItemInstance item = player.getInventory().addItem(rewradItemId, rewradItemCount, 0, "Manor");
			L2ItemInstance iteme = player.getInventory().destroyItemByItemId(itemId, count, true);

			if(item == null || iteme == null)
				continue;

			// Send Char Buy Messages
			SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S);
			sm.addItemName(rewradItemId);
			sm.addNumber(rewradItemCount);
			player.sendPacket(sm);
			sm = null;

			//manor.getCastle().setCropAmount(itemId, manor.getCastle().getCrop(itemId, CastleManorManager.PERIOD_CURRENT).getAmount() - count);
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}