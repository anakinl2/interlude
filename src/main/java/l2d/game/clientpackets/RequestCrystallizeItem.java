package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import com.lineage.util.Log;

public class RequestCrystallizeItem extends L2GameClientPacket
{
	//Format: cdd

	private int _objectId;
	@SuppressWarnings("unused")
	private long unk;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		unk = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM));
			activeChar.sendActionFailed();
			return;
		}

		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);

		if(item == null || !item.canBeCrystallized(activeChar, true))
		{
			activeChar.sendActionFailed();
			return;
		}

		crystallize(activeChar, item);
	}

	public static void crystallize(L2Player activeChar, L2ItemInstance item)
	{
		activeChar.getInventory().destroyItem(item, 1, true);

		// add crystals
		int crystalAmount = item.getItem().getCrystalCount();
		int crystalId = item.getItem().getCrystalType().cry;

		L2ItemInstance createditem = ItemTable.getInstance().createItem(crystalId);
		createditem.setCount(crystalAmount);
		L2ItemInstance addedItem = activeChar.getInventory().addItem(createditem);
		activeChar.sendPacket(new SystemMessage(SystemMessage.THE_ITEM_HAS_BEEN_SUCCESSFULLY_CRYSTALLIZED));
		activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S2_S1).addItemName(crystalId).addNumber(crystalAmount));

		Log.LogItem(activeChar, Log.CrystalizeItem, item);
		Log.LogItem(activeChar, Log.Sys_GetItem, addedItem);

		activeChar.updateStats();

		L2World.removeObject(item);
	}
}