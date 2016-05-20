package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.ExPutCommissionResultForVariationMake;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.L2Item.Grade;

/**
 * [C] D0:28 RequestConfirmGemStone
 * <b>Format:</b> (ch)dddd
 * @author Felixx
 */
public class RequestConfirmGemStone extends L2GameClientPacket
{
	private int _targetItemObjId;
	private int _refinerItemObjId;
	private int _gemstoneItemObjId;
	private int _gemstoneCount;

	@Override
	public void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
		_gemstoneItemObjId = readD();
		_gemstoneCount = readD();
	}

	@Override
	public void runImpl()
	{
		if(_gemstoneCount <= 0)
			return;

		L2Player activeChar = getClient().getActiveChar();
		L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
		L2ItemInstance refinerItem = activeChar.getInventory().getItemByObjectId(_refinerItemObjId);
		L2ItemInstance gemstoneItem = activeChar.getInventory().getItemByObjectId(_gemstoneItemObjId);

		if(targetItem == null || refinerItem == null || gemstoneItem == null)
			return;

		// Make sure the item is a gemstone
		int gemstoneItemId = gemstoneItem.getItem().getItemId();
		if(gemstoneItemId != 2130 && gemstoneItemId != 2131)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}

		// Check if the gemstoneCount is sufficant
		Grade itemGrade = targetItem.getItem().getItemGrade();
		if(isAccessoryLifeStone(refinerItem.getItemId()))
			switch(itemGrade)
			{
				case C:
				{
					if(_gemstoneCount != 200 || gemstoneItemId != 2130)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.GEMSTONE_QUANTITY_IS_INCORRECT));
						return;
					}
					break;
				}
				case B:
				{
					if(_gemstoneCount != 300 || gemstoneItemId != 2130)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.GEMSTONE_QUANTITY_IS_INCORRECT));
						return;
					}
					break;
				}
				case A:
				{
					if(_gemstoneCount != 200 || gemstoneItemId != 2131)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.GEMSTONE_QUANTITY_IS_INCORRECT));
						return;
					}
					break;
				}
				case S:
				case S80:
				{
					if(_gemstoneCount != 250 || gemstoneItemId != 2131)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.GEMSTONE_QUANTITY_IS_INCORRECT));
						return;
					}
					break;
				}
			}
		else
			switch(itemGrade)
			{
				case C:
				{
					if(_gemstoneCount != 20 || gemstoneItemId != 2130)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.GEMSTONE_QUANTITY_IS_INCORRECT));
						return;
					}
					break;
				}
				case B:
				{
					if(_gemstoneCount != 30 || gemstoneItemId != 2130)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.GEMSTONE_QUANTITY_IS_INCORRECT));
						return;
					}
					break;
				}
				case A:
				{
					if(_gemstoneCount != 20 || gemstoneItemId != 2131)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.GEMSTONE_QUANTITY_IS_INCORRECT));
						return;
					}
					break;
				}
				case S:
				case S80:
				{
					if(_gemstoneCount != 25 || gemstoneItemId != 2131)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.GEMSTONE_QUANTITY_IS_INCORRECT));
						return;
					}
					break;
				}
			}

		activeChar.sendPacket(new ExPutCommissionResultForVariationMake(_gemstoneItemObjId, _gemstoneCount));
		activeChar.sendPacket(new SystemMessage(SystemMessage.PRESS_THE_AUGMENT_BUTTON_TO_BEGIN));
	}

	private boolean isAccessoryLifeStone(int id)
	{
		return id >= 12754 && id <= 12763 || id >= 12821 && id <= 12822 || id >= 12840 && id <= 12851;
	}
}