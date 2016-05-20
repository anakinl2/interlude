package com.lineage.game.clientpackets;

import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.util.Log;

/**
 * format: cdd
 * format: cdQ - Gracia Final
 */
public class RequestDestroyItem extends L2GameClientPacket
{
	private int _objectId;
	private int _count;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_count = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		int count = _count;

		L2ItemInstance itemToRemove = activeChar.getInventory().getItemByObjectId(_objectId);

		if(itemToRemove == null)
			return;

		if(count < 1)
		{
			activeChar.sendPacket(Msg.YOU_CANNOT_DESTROY_IT_BECAUSE_THE_NUMBER_IS_INCORRECT);
			return;
		}

		if(itemToRemove.isHeroItem())
		{
			activeChar.sendPacket(Msg.HERO_WEAPONS_CANNOT_BE_DESTROYED);
			return;
		}

		if(!itemToRemove.canBeDestroyed(activeChar))
		{
			activeChar.sendPacket(Msg.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}

		if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(Msg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}

		if(activeChar.isFishing())
		{
			activeChar.sendPacket(Msg.YOU_CANNOT_DO_THAT_WHILE_FISHING);
			return;
		}

		if(activeChar.getPet() != null && activeChar.getPet().getControlItemObjId() == itemToRemove.getObjectId())
		{
			activeChar.sendPacket(Msg.THE_PET_HAS_BEEN_SUMMONED_AND_CANNOT_BE_DELETED);
			return;
		}

		if(activeChar.isActionsDisabled())
		{
			activeChar.sendActionFailed();
			return;
		}

		if(_count > itemToRemove.getIntegerLimitedCount())
			count = itemToRemove.getIntegerLimitedCount();

		if(itemToRemove.isEquipped())
		{
			activeChar.getInventory().unEquipItemInSlot(itemToRemove.getEquipSlot());
			activeChar.broadcastUserInfo(true);
		}

		if(itemToRemove.canBeCrystallized(activeChar, false))
		{
			RequestCrystallizeItem.crystallize(activeChar, itemToRemove);
			return;
		}

		L2ItemInstance removedItem = activeChar.getInventory().destroyItem(_objectId, count, true);

		Log.LogItem(activeChar, Log.DeleteItem, removedItem);

		activeChar.sendChanges();

		if(!removedItem.isStackable() || count == 1)
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(removedItem.getItemId()));
		else
			activeChar.sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(removedItem.getItemId()).addNumber(count));

		L2World.removeObject(removedItem);

		activeChar.updateStats();
	}
}