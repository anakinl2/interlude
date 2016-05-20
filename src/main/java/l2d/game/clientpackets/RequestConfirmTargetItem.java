package l2d.game.clientpackets;

import l2d.Config;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.ExPutItemResultForVariationMake;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.L2Item;
import l2d.game.templates.L2Item.Grade;

/**
 * [C] D0:26 RequestConfirmTargetItem
 * @author Felixx
 */
public class RequestConfirmTargetItem extends L2GameClientPacket
{
	private int _itemObjId;

	@Override
	public void readImpl()
	{
		_itemObjId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_itemObjId);

		if(item == null)
			return;

		if(activeChar.getLevel() < 46)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS));
			return;
		}

		// check if the item is augmentable
		Grade itemGrade = item.getItem().getItemGrade();
		int itemType = item.getItem().getType2();

		if(item.isAugmented())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.ONCE_AN_ITEM_IS_AUGMENTED_IT_CANNOT_BE_AUGMENTED_AGAIN));
			return;
		}

		if(itemGrade.ordinal() < Grade.C.ordinal() || itemType != L2Item.TYPE2_WEAPON && itemType != L2Item.TYPE2_ACCESSORY && !Config.ALT_ALLOW_AUGMENT_ALL || !item.isDestroyable() || item.isShadowItem() || item.getItem().isRaidAccessory())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}

		// check if the player can augment
		if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION));
			return;
		}
		if(activeChar.isDead())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD));
			return;
		}
		if(activeChar.isParalyzed())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED));
			return;
		}
		if(activeChar.isFishing())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING));
			return;
		}
		if(activeChar.isSitting())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING_DOWN));
			return;
		}

		activeChar.sendPacket(new ExPutItemResultForVariationMake(_itemObjId));
		activeChar.sendPacket(new SystemMessage(SystemMessage.SELECT_THE_CATALYST_FOR_AUGMENTATION));
	}
}