package l2d.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.ext.multilang.CustomMessage;
import l2d.game.cache.Msg;
import l2d.game.model.ClanWarehousePool;
import l2d.game.model.L2Character;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.Warehouse;
import l2d.game.model.Warehouse.WarehouseType;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2NpcInstance;

public class SendWareHouseWithDrawList extends L2GameClientPacket
{
	//Format: cdb, b - array of (dd)
	private static Logger _log = Logger.getLogger(SendWareHouseWithDrawList.class.getName());

	private int _count;
	private int[] _items;
	private int[] counts;

	@Override
	public void readImpl()
	{
		_count = readD();
		if(_count * 8 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new int[_count * 2];
		counts = new int[_count];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 2 + 0] = readD(); //item object id
			_items[i * 2 + 1] = readD(); //count
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
		if(_items == null)
			return;

		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2NpcInstance whkeeper = activeChar.getLastNpc();
		if(whkeeper == null || !activeChar.isInRange(whkeeper.getLoc(), L2Character.INTERACTION_DISTANCE))
		{
			activeChar.sendPacket(Msg.WAREHOUSE_IS_TOO_FAR);
			return;
		}

		if(!activeChar.getVarB("canWhWithdraw") && activeChar.getUsingWarehouseType() == WarehouseType.CLAN && activeChar.getClan().getLeaderId() != activeChar.getObjectId())
			return;

		if(activeChar.getUsingWarehouseType() == WarehouseType.CLAN && !((activeChar.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE))
			return;

		int weight = 0;
		int finalCount = activeChar.getInventory().getSize();
		L2ItemInstance[] olditems = new L2ItemInstance[_count];

		for(int i = 0; i < _count; i++)
		{
			int itemObjId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];
			L2ItemInstance oldinst = L2ItemInstance.restoreFromDb(itemObjId);

			if(count < 0)
			{
				activeChar.sendPacket(Msg.INCORRECT_ITEM_COUNT);
				return;
			}

			if(oldinst == null)
			{
				activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.SendWareHouseWithDrawList.Changed", activeChar));
				for(int f = 0; f < i; f++)
					L2World.removeObject(olditems[i]); // FIXME don't sure...

				return;
			}

			if(oldinst.getIntegerLimitedCount() < count)
				count = oldinst.getIntegerLimitedCount();

			counts[i] = count;
			olditems[i] = oldinst;
			weight += oldinst.getItem().getWeight() * count;
			finalCount++;

			if(oldinst.getItem().isStackable() && activeChar.getInventory().getItemByItemId(oldinst.getItemId()) != null)
				finalCount--;
		}

		if(!activeChar.getInventory().validateCapacity(finalCount))
		{
			for(L2ItemInstance element : olditems)
				//L2World.removeObject(items[i]);
				L2World.removeObject(element); // FIXME don't sure...
			activeChar.sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			//            items = null;
			return;
		}

		if(!activeChar.getInventory().validateWeight(weight))
		{
			for(L2ItemInstance element : olditems)
				L2World.removeObject(element); // FIXME don't sure...
			activeChar.sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return;
		}

		Warehouse warehouse = null;
		if(activeChar.getUsingWarehouseType() == WarehouseType.PRIVATE)
			warehouse = activeChar.getWarehouse();
		else if(activeChar.getUsingWarehouseType() == WarehouseType.CLAN)
		{
			ClanWarehousePool.getInstance().AddWork(activeChar, olditems, counts);
			return;
		}
		else if(activeChar.getUsingWarehouseType() == WarehouseType.FREIGHT)
			warehouse = activeChar.getFreight();
		else
		{
			// Something went wrong!
			_log.warning("Error retrieving a warehouse object for char " + activeChar.getName() + " - using warehouse type: " + activeChar.getUsingWarehouseType());
			return;
		}

		for(int i = 0; i < olditems.length; i++)
		{
			L2ItemInstance TransferItem = warehouse.takeItemByObj(olditems[i].getObjectId(), counts[i]);
			if(TransferItem == null)
				_log.warning("Error getItem from warhouse player: " + activeChar.getName());
			activeChar.getInventory().addItem(TransferItem);
		}

		activeChar.sendChanges();
	}
}