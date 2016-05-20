package com.lineage.game.clientpackets;

import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.ExVariationCancelResult;
import com.lineage.game.serverpackets.InventoryUpdate;
import com.lineage.game.serverpackets.SystemMessage;

/**
 * [C] D0:46 RequestRefineCancel
 * <b>Format:</b> (ch)d
 * @author Felixx
 *
 */
public final class RequestRefineCancel extends L2GameClientPacket
{
	private int _targetItemObjId;

	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);

		// cannot remove augmentation from a not augmented item
		if(!targetItem.isAugmented())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM));
			return;
		}

		if(targetItem.isEquipped())
			activeChar.getInventory().unEquipItem(targetItem);

		// get the price
		int price = 0;
		switch(targetItem.getItem().getItemGrade())
		{
			case C:
			{
				if(targetItem.getItem().getCrystalCount() < 1720)
					price = 95000;
				else if(targetItem.getItem().getCrystalCount() < 2452)
					price = 150000;
				else
					price = 210000;
				break;
			}
			case B:
			{
				if(targetItem.getItem().getCrystalCount() < 1746)
					price = 240000;
				else
					price = 270000;
				break;
			}
			case A:
			{
				if(targetItem.getItem().getCrystalCount() < 2160)
					price = 330000;
				else if(targetItem.getItem().getCrystalCount() < 2824)
					price = 390000;
				else
					price = 420000;
				break;
			}
			case S:
			case S80:
			{
				price = 480000;
				break;
			}
				// any other item type is not augmentable
			default:
				activeChar.sendPacket(new ExVariationCancelResult(price));
				return;
		}

		// try to reduce the players adena
		if(activeChar.getAdena() < price)
		{
			activeChar.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return;
		}
		activeChar.reduceAdena(price);

		// cancel boni
		targetItem.getAugmentation().removeBoni(activeChar);

		// remove the augmentation
		targetItem.removeAugmentation();

		// send ExVariationCancelResult
		activeChar.sendPacket(new ExVariationCancelResult(price));

		// send inventory update
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		activeChar.sendPacket(iu);

		// send system message
		SystemMessage sm = new SystemMessage(SystemMessage.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1);
		sm.addItemName(targetItem.getItemId());
		activeChar.sendPacket(sm);

		activeChar.broadcastUserInfo(true);
	}
}