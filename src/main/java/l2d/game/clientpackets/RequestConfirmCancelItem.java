package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.ExPutItemResultForVariationCancel;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.L2Item.Grade;

/**
 * [C] D0:45 RequestConfirmCancelItem
 * <b>Format:</b>(ch)d
 * @author Felixx
 */
public class RequestConfirmCancelItem extends L2GameClientPacket
{
	int _itemId;

	@Override
	public void readImpl()
	{
		_itemId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_itemId);
		Grade itemGrade = item.getItem().getItemGrade();

		if(item == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(!item.isAugmented())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM));
			return;
		}

		int price = 0;
		switch(itemGrade)
		{
			case C:
			{
				if(item.getItem().getCrystalCount() < 1177)
					price = 95000;
				else if(item.getItem().getCrystalCount() < 1627)
					price = 150000;
				else
					price = 210000;
				break;
			}
			case B:
			{
				if(item.getItem().getCrystalCount() < 1345)
					price = 240000;
				else
					price = 270000;
				break;
			}
			case A:
			{
				if(item.getItem().getCrystalCount() < 1664)
					price = 330000;
				else if(item.getItem().getCrystalCount() < 2175)
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
			default:
				return;
		}
		activeChar.sendPacket(new ExPutItemResultForVariationCancel(_itemId, price));
	}
}