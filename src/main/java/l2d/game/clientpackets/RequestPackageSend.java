package l2d.game.clientpackets;

import java.util.HashMap;

import com.lineage.Config;
import l2d.game.cache.Msg;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.PcInventory;
import l2d.game.model.Warehouse;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2ItemInstance.ItemClass;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.SystemMessage;

/**
 * Format: cddb, b - array of (dd)
 * @author SYS
 */
public class RequestPackageSend extends L2GameClientPacket
{
	private int _objectID;
	private HashMap<Integer, Integer> _items;

	private static int _FREIGHT_FEE = 1000;

	@Override
	public void readImpl()
	{
		_objectID = readD();
		int itemsCount = readD();
		if(itemsCount * 8 > _buf.remaining() || itemsCount > Short.MAX_VALUE || itemsCount <= 0)
		{
			_items = null;
			return;
		}
		_items = new HashMap<Integer, Integer>(itemsCount + 1, 0.999f);
		for(int i = 0; i < itemsCount; i++)
		{
			int obj_id = readD(); // this is some id sent in PackageSendableList
			int itemQuantity = readD();
			if(itemQuantity < 0)
			{
				_items = null;
				return;
			}
			_items.put(obj_id, itemQuantity);
		}
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || _items == null || !activeChar.getPlayerAccess().UseWarehouse)
			return;

		PcInventory inventory = activeChar.getInventory();
		int adenaDeposit = 0;
		int adenaObjId;
		L2ItemInstance adena = inventory.getItemByItemId(57);
		if(adena != null)
			adenaObjId = adena.getObjectId();
		else
			adenaObjId = -1;
		for(Integer itemObjectId : _items.keySet())
		{
			L2ItemInstance item = inventory.getItemByObjectId(itemObjectId);
			if(item == null || item.isEquipped())
				return;

			if(_items.get(itemObjectId) < 0)
				return;

			if(itemObjectId == adenaObjId)
				adenaDeposit = _items.get(itemObjectId);
		}

		L2NpcInstance freighter = activeChar.getLastNpc();
		if(freighter == null || !activeChar.isInRange(freighter.getLoc(), L2Character.INTERACTION_DISTANCE))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_FAILED_AT_SENDING_THE_PACKAGE_BECAUSE_YOU_ARE_TOO_FAR_FROM_THE_WAREHOUSE));
			return;
		}

		int fee = _items.size() * _FREIGHT_FEE;

		if(fee + adenaDeposit > activeChar.getAdena())
		{
			activeChar.sendPacket(Msg.YOU_LACK_THE_FUNDS_NEEDED_TO_PAY_FOR_THIS_TRANSACTION);
			return;
		}

		L2Player destChar = null;
		Warehouse warehouse;
		destChar = L2Player.load(_objectID);
		if(destChar == null)
		{
			// Something went wrong!
			if(Config.DEBUG)
				_log.warning("Error retrieving a warehouse object for char " + activeChar.getName());
			return;
		}
		warehouse = destChar.getFreight();

		// Item Max Limit Check
		if(_items.size() + warehouse.listItems(ItemClass.ALL).length > destChar.getFreightLimit())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_CAPACITY_OF_THE_WAREHOUSE_HAS_BEEN_EXCEEDED));
			destChar.deleteMe();
			return;
		}

		// Transfer the items from activeChar's Inventory Instance to destChar's Freight Instance
		for(Integer itemObjectId : _items.keySet())
		{
			L2ItemInstance found = inventory.getItemByObjectId(itemObjectId);
			if(found == null || !found.canBeDropped(activeChar))
				continue;

			L2ItemInstance item = inventory.dropItem(found, _items.get(itemObjectId), true);
			warehouse.addItem(item, null);
		}

		activeChar.reduceAdena(fee);
		activeChar.updateStats();

		// Delete destination L2Player used for freight
		destChar.deleteMe();
		activeChar.sendPacket(new SystemMessage(SystemMessage.THE_TRANSACTION_IS_COMPLETE));
	}
}