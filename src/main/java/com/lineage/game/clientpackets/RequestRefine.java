package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.game.instancemanager.AugmentationManager;
import com.lineage.game.instancemanager.CursedWeaponsManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.ExVariationResult;
import com.lineage.game.serverpackets.InventoryUpdate;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.serverpackets.UserInfo;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.L2Item.Grade;
import com.lineage.util.Util;

public final class RequestRefine extends L2GameClientPacket
{
	protected static Logger _log = Logger.getLogger(RequestRefine.class.getName());

	private int _targetItemObjId;
	private int _refinerItemObjId;
	private int _gemstoneItemObjId;
	private int _gemstoneCount;

	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
		_gemstoneItemObjId = readD();
		_gemstoneCount = readD();
	}

	@Override
	protected void runImpl()
	{
		if(_gemstoneCount <= 0)
		{
			if(Config.DEBUG)
				_log.info("_gemstoneCount <= 0");
			return;
		}
		final L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
		{
			if(Config.DEBUG)
				_log.info("Char = null");
			return;
		}

		final L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
		final L2ItemInstance refinerItem = activeChar.getInventory().getItemByObjectId(_refinerItemObjId);
		final L2ItemInstance gemstoneItem = activeChar.getInventory().getItemByObjectId(_gemstoneItemObjId);

		if(targetItem.isEquipped())
		{
			if(Config.DEBUG)
				_log.info("UnEnupedItem");
			activeChar.getInventory().unEquipItem(targetItem);
		}

		if(LifeStoneLevel(refinerItem.getItemId()) > activeChar.getLevel())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_LEVEL_OF_THE_HARDENER_IS_TOO_HIGH_TO_BE_USED));
			return;
		}

		if(targetItem == null || refinerItem == null || gemstoneItem == null || targetItem.getOwnerId() != activeChar.getObjectId() || refinerItem.getOwnerId() != activeChar.getObjectId() || gemstoneItem.getOwnerId() != activeChar.getObjectId() || activeChar.getLevel() < 46)
		{
			activeChar.sendPacket(new ExVariationResult(0, 0, 0));
			activeChar.sendPacket(new SystemMessage(SystemMessage.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS));
			return;
		}

		if(TryAugmentItem(activeChar, targetItem, refinerItem, gemstoneItem))
		{
			final int stat12 = 0x0000FFFF & targetItem.getAugmentation().getAugmentationId();
			final int stat34 = targetItem.getAugmentation().getAugmentationId() >> 16;
			activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED));
		}
		else
		{
			activeChar.sendPacket(new ExVariationResult(0, 0, 0));
			activeChar.sendPacket(new SystemMessage(SystemMessage.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS));
		}
	}

	boolean TryAugmentItem(final L2Player player, final L2ItemInstance targetItem, final L2ItemInstance refinerItem, final L2ItemInstance gemstoneItem)
	{
		final L2Player activeChar = getClient().getActiveChar();
		final Grade itemGrade = targetItem.getItem().getItemGrade();
		final int itemType = targetItem.getItem().getType2();
		final int lifeStoneId = refinerItem.getItemId();
		final int gemstoneItemId = gemstoneItem.getItemId();

		if(targetItem.isAugmented() || targetItem.isWear() || targetItem.isHeroItem() || targetItem.isShadowItem() || CursedWeaponsManager.getInstance().isCursed(targetItem.getItemId()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_A_SUITABLE_ITEM));
			return false;
		}
		if(player.isDead())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD));
			return false;
		}
		if(player.isSitting())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING_DOWN));
			return false;
		}
		if(player.isFishing())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING));
			return false;
		}
		if(player.isParalyzed())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED));
			return false;
		}
		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION));
			return false;
		}
		if(player.getInventory().getItemByObjectId(refinerItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong LifeStone-id.", "", Config.DEFAULT_PUNISH);
			return false;
		}
		if(player.getInventory().getItemByObjectId(targetItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong Item-id.", "", Config.DEFAULT_PUNISH);
			return false;
		}
		if(player.getInventory().getItemByObjectId(gemstoneItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong Gemstone-id.", "", Config.DEFAULT_PUNISH);
			return false;
		}
		// is the refiner Item a life stone?
		if((lifeStoneId < 8723 || lifeStoneId > 8762) && (lifeStoneId < 9573 || lifeStoneId > 9576) && (lifeStoneId < 10483 || lifeStoneId > 10486))
		{
			if(Config.DEBUG)
				_log.info("if(!isAccessoryLifeStone && ((lifeStoneId < 8723");
			return false;
		}

		if(itemGrade.ordinal() < Grade.C.ordinal() || itemType != L2Item.TYPE2_WEAPON  && !Config.ALT_ALLOW_AUGMENT_ALL || !targetItem.isDestroyable() || targetItem.isShadowItem() || targetItem.getItem().isRaidAccessory())
		{
			if(Config.DEBUG)
				_log.info("itemGrade < L2Item.CRYSTAL_C)...");
			return false;
		}

		int modifyGemstoneCount = _gemstoneCount;
		int lifeStoneLevel = -1;
		int lifeStoneGrade = -1;
		if(itemType == L2Item.TYPE2_WEAPON)
		{
			lifeStoneLevel = getLifeStoneLevel(lifeStoneId);
			lifeStoneGrade = getLifeStoneGrade(lifeStoneId);
		}
			switch(itemGrade)
			{
				case C:
					if(player.getLevel() < 46 || gemstoneItemId != 2130)
						return false;
					modifyGemstoneCount = 20;
					break;
				case B:
					if(player.getLevel() < 52 || gemstoneItemId != 2130)
						return false;
					modifyGemstoneCount = 30;
					break;
				case A:
					if(player.getLevel() < 61 || gemstoneItemId != 2131)
						return false;
					modifyGemstoneCount = 20;
					break;
				case S:
					if(player.getLevel() < 76 || gemstoneItemId != 2131)
						return false;
					modifyGemstoneCount = 25;
					break;
				case S80:
					if(player.getLevel() < 80 || gemstoneItemId != 2131)
						return false;
					modifyGemstoneCount = 25;
					break;
			}

		// check if the lifestone is appropriate for this player
		switch(lifeStoneLevel)
		{
			case 1:
				if(player.getLevel() < 46)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel1");
					return false;
				}
				break;
			case 2:
				if(player.getLevel() < 49)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel2");
					return false;
				}
				break;
			case 3:
				if(player.getLevel() < 52)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel3");
					return false;
				}
				break;
			case 4:
				if(player.getLevel() < 55)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel4");
					return false;
				}
				break;
			case 5:
				if(player.getLevel() < 58)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel5");
					return false;
				}
				break;
			case 6:
				if(player.getLevel() < 61)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel6");
					return false;
				}
				break;
			case 7:
				if(player.getLevel() < 64)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel7");
					return false;
				}
				break;
			case 8:
				if(player.getLevel() < 67)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel8");
					return false;
				}
				break;
			case 9:
				if(player.getLevel() < 70)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel9");
					return false;
				}
				break;
			case 10:
				if(player.getLevel() < 76)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel10");
					return false;
				}
				break;
			case 11:
				if(player.getLevel() < 80)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel11");
					return false;
				}
				break;
			case 12:
				if(player.getLevel() < 82)
				{
					if(Config.DEBUG)
						_log.info("lifeStoneLevel12");
					return false;
				}
				break;
		}

		if(gemstoneItem.getIntegerLimitedCount() < modifyGemstoneCount)
		{
			if(Config.DEBUG)
				_log.info("getIntegerLimitedCount() < modifyGemstoneCount");
			return false;
		}

		lifeStoneLevel = Math.min(lifeStoneLevel, 10); // инкрустация описана до 10 левела

		// generate augmentation
		if(Config.DEBUG)
			_log.info("Start Argument");
		targetItem.setAugmentation(AugmentationManager.getInstance().generateRandomAugmentation(lifeStoneLevel, lifeStoneGrade, targetItem));
		if(Config.DEBUG)
			_log.info("End Argument");

		// consume items
		player.getInventory().destroyItem(_gemstoneItemObjId, modifyGemstoneCount, true);
		player.getInventory().destroyItem(refinerItem, 1, true);

		// send an inventory update packet
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		player.sendPacket(iu);
		player.sendPacket(new UserInfo(player));
		return true;
	}

	private int getLifeStoneGrade(int itemId)
	{
		itemId -= 8723;
		if(itemId < 10 || itemId == 850 || itemId == 1760)
			return 0; // normal grade
		if(itemId < 20 || itemId == 851 || itemId == 1761)
			return 1; // mid grade
		if(itemId < 30 || itemId == 852 || itemId == 1762)
			return 2; // high grade
		return 3; // top grade
	}

	private int getLifeStoneLevel(int itemId)
	{
		itemId -= 10 * getLifeStoneGrade(itemId);
		itemId -= 8722;
		if(itemId > 823 && itemId < 852)
			return 11;
		if(itemId > 833 && itemId < 1762)
			return 12;
		return itemId;
	}


	private int LifeStoneLevel(final int stoneid)
	{
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