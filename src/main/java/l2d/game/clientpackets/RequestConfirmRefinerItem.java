package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.ExPutIntensiveResultForVariationMake;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.L2Item;
import l2d.game.templates.L2Item.Grade;
import l2d.game.templates.L2Weapon.WeaponType;

/**
 * [C] D0:27 RequestConfirmRefinerItem
 * 
 * @author Felixx
 */
public class RequestConfirmRefinerItem extends L2GameClientPacket
{
	private static final int GEMSTONE_D = 2130;
	private static final int GEMSTONE_C = 2131;
	private int _targetItemObjId;
	private int _refinerItemObjId;

	@Override
	public void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
	}

	@Override
	public void runImpl()
	{
		final L2Player activeChar = getClient().getActiveChar();
		final L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
		final L2ItemInstance refinerItem = activeChar.getInventory().getItemByObjectId(_refinerItemObjId);
		if(activeChar == null || targetItem == null || refinerItem == null)
			return;

		final Grade itemGrade = targetItem.getItem().getItemGrade();
		final int refinerItemId = refinerItem.getItem().getItemId();

		final boolean isAccessoryLifeStone = isAccessoryLifeStone(refinerItemId);

		// is the item a life stone?
		if(!isAccessoryLifeStone && (refinerItemId < 8723 || refinerItemId > 8762) && (refinerItemId < 9573 || refinerItemId > 9576) && (refinerItemId < 10483 || refinerItemId > 10486))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}

		if(targetItem.getItem().getType2() == L2Item.TYPE2_ACCESSORY && !isAccessoryLifeStone)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}
		else if(targetItem.getItem().getType2() == L2Item.TYPE2_WEAPON && isAccessoryLifeStone)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}

		if(targetItem.getItem().getItemType() == WeaponType.ROD)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}

		if(LifeStoneLevel(refinerItemId) > activeChar.getLevel())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_LEVEL_OF_THE_HARDENER_IS_TOO_HIGH_TO_BE_USED));
			return;
		}

		int gemstoneCount = 0;
		int gemstoneItemId = 0;
		final SystemMessage sm = new SystemMessage(SystemMessage.REQUIRES_S1_S2);

		if(isAccessoryLifeStone)
			switch(itemGrade)
			{
				case C:
				{
					gemstoneCount = 200;
					gemstoneItemId = GEMSTONE_D;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone D");
					break;
				}
				case B:
				{
					gemstoneCount = 300;
					gemstoneItemId = GEMSTONE_D;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone D");
					break;
				}
				case A:
				{
					gemstoneCount = 200;
					gemstoneItemId = GEMSTONE_C;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone C");
					break;
				}
				case S:
				case S80:
				{
					gemstoneCount = 250;
					gemstoneItemId = GEMSTONE_C;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone C");
					break;
				}
			}
		else
			switch(itemGrade)
			{
				case C:
				{
					gemstoneCount = 20;
					gemstoneItemId = GEMSTONE_D;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone D");
					break;
				}
				case B:
				{
					gemstoneCount = 30;
					gemstoneItemId = GEMSTONE_D;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone D");
					break;
				}
				case A:
				{
					gemstoneCount = 20;
					gemstoneItemId = GEMSTONE_C;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone C");
					break;
				}
				case S:
				case S80:
				{
					gemstoneCount = 25;
					gemstoneItemId = GEMSTONE_C;
					sm.addNumber(gemstoneCount);
					sm.addString("Gemstone C");
					break;
				}
			}

		activeChar.sendPacket(new ExPutIntensiveResultForVariationMake(_refinerItemObjId, refinerItemId, gemstoneItemId, gemstoneCount));
		activeChar.sendPacket(sm);
	}

	private boolean isAccessoryLifeStone(final int id)
	{
		return id >= 12754 && id <= 12763 || id >= 12821 && id <= 12822 || id >= 12840 && id <= 12851;
	}

	private int LifeStoneLevel(final int stoneid)
	{
		if(isAccessoryLifeStone(stoneid))
			switch(stoneid)
			{
				case 12754:
				case 12840:
					return 46;
				case 12755:
				case 12841:
					return 49;
				case 12756:
				case 12842:
					return 52;
				case 12757:
				case 12843:
					return 55;
				case 12758:
				case 12844:
					return 58;
				case 12759:
				case 12845:
					return 61;
				case 12760:
				case 12846:
					return 64;
				case 12761:
				case 12847:
					return 67;
				case 12762:
				case 12848:
					return 70;
				case 12763:
				case 12849:
					return 76;
				case 12821:
				case 12850:
					return 80;
				case 12822:
				case 12851:
					return 82;
			}
		else
			switch(stoneid)
			{
				case 8723:
				case 8733:
				case 8743:
				case 8753:
					return 46;
				case 8724:
				case 8734:
				case 8744:
				case 8754:
					return 49;
				case 8725:
				case 8735:
				case 8745:
				case 8755:
					return 52;
				case 8726:
				case 8736:
				case 8746:
				case 8756:
					return 55;
				case 8727:
				case 8737:
				case 8747:
				case 8757:
					return 58;
				case 8728:
				case 8738:
				case 8748:
				case 8758:
					return 61;
				case 8729:
				case 8739:
				case 8749:
				case 8759:
					return 64;
				case 8730:
				case 8740:
				case 8750:
				case 8760:
					return 67;
				case 8731:
				case 8741:
				case 8751:
				case 8761:
					return 70;
				case 8732:
				case 8742:
				case 8752:
				case 8762:
					return 76;
			}
		return stoneid;
	}
}