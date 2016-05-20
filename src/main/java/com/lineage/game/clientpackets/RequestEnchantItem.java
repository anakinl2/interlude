package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.PcInventory;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.EnchantResult;
import com.lineage.game.serverpackets.InventoryUpdate;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.L2Item.Grade;
import com.lineage.game.templates.L2Weapon;
import com.lineage.game.templates.L2Weapon.WeaponType;
import com.lineage.util.Log;
import com.lineage.util.Rnd;

public class RequestEnchantItem extends L2GameClientPacket
{
	protected static Logger _log = Logger.getLogger(RequestEnchantItem.class.getName());

	// Format: cd
	private int _objectId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.isOutOfControl() || activeChar.isActionsDisabled())
		{
			activeChar.sendActionFailed();
			return;
		}

		PcInventory inventory = activeChar.getInventory();
		L2ItemInstance itemToEnchant = inventory.getItemByObjectId(_objectId);
		L2ItemInstance scroll = activeChar.getEnchantScroll();
		activeChar.setEnchantScroll(null);

		if(itemToEnchant == null || scroll == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		Log.add(activeChar.getName() + "|Trying to enchant|" + itemToEnchant.getItemId() + "|+" + itemToEnchant.getEnchantLevel() + "|" + itemToEnchant.getObjectId(), "enchants");

		if(!itemToEnchant.canBeEnchanted())
		{
			activeChar.sendPacket(Msg.INAPPROPRIATE_ENCHANT_CONDITIONS);
			activeChar.sendActionFailed();
			return;
		}

		if(itemToEnchant.getLocation() != L2ItemInstance.ItemLocation.INVENTORY && itemToEnchant.getLocation() != L2ItemInstance.ItemLocation.PAPERDOLL)
		{
			activeChar.sendPacket(Msg.INAPPROPRIATE_ENCHANT_CONDITIONS);
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(Msg.YOU_CANNOT_PRACTICE_ENCHANTING_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_MANUFACTURING_WORKSHOP);
			activeChar.sendActionFailed();
			return;
		}

		if(itemToEnchant.isStackable() || (scroll = inventory.getItemByObjectId(scroll.getObjectId())) == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		int crystalId = itemToEnchant.getEnchantCrystalId(scroll);

		if(crystalId == 0)
		{
			activeChar.sendPacket(Msg.INAPPROPRIATE_ENCHANT_CONDITIONS);
			activeChar.sendActionFailed();
			return;
		}

		if(itemToEnchant.getEnchantLevel() >= Config.ENCHANT_MAX)
		{
			activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestEnchantItem.MaxLevel", activeChar));
			activeChar.sendActionFailed();
			return;
		}

		// Запрет на заточку чужих вещей, баг может вылезти на серверных лагах
		if(itemToEnchant.getOwnerId() != activeChar.getObjectId())
		{
			activeChar.sendPacket(Msg.INAPPROPRIATE_ENCHANT_CONDITIONS);
			activeChar.sendActionFailed();
			return;
		}

		L2ItemInstance removedScroll;
		synchronized (inventory)
		{
			removedScroll = inventory.destroyItem(scroll.getObjectId(), 1, true);
		}

		// tries enchant without scrolls
		if(removedScroll == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		int itemType = itemToEnchant.getItem().getType2();
		int safeEnchantLevel = itemToEnchant.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR ? Config.SAFE_ENCHANT_FULL_BODY : Config.SAFE_ENCHANT_COMMON;

		double chance;
		if(itemToEnchant.getEnchantLevel() < safeEnchantLevel)
			chance = 100;
		else if(itemType == L2Item.TYPE2_WEAPON)
			chance = removedScroll.isCrystallEnchantScroll() ? Config.ENCHANT_CHANCE_CRYSTAL_WEAPON : Config.ENCHANT_CHANCE_WEAPON;
		else if(itemType == L2Item.TYPE2_SHIELD_ARMOR)
			chance = removedScroll.isCrystallEnchantScroll() ? Config.ENCHANT_CHANCE_CRYSTAL_ARMOR : Config.ENCHANT_CHANCE_ARMOR;
		else if(itemType == L2Item.TYPE2_ACCESSORY)
			chance = removedScroll.isCrystallEnchantScroll() ? Config.ENCHANT_CHANCE_CRYSTAL_ACCESSORY : Config.ENCHANT_CHANCE_ACCESSORY;
		else
		{
			System.out.println("WTF? Request to enchant " + itemToEnchant.getItemId());
			activeChar.sendActionFailed();
			activeChar.sendPacket(Msg.SYSTEM_ERROR);
			activeChar.getInventory().addItem(removedScroll);
			return;
		}

		if(Config.ALT_ENCHANT_FORMULA)
		{
			int enchlvl = itemToEnchant.getEnchantLevel();
			Grade crystaltype = itemToEnchant.getItem().getCrystalType();

			// для уровнения шансов дуальщиков и остальных на победу в PvP вставка SA в дули халявная
			if(itemType == L2Item.TYPE2_WEAPON && itemToEnchant.getItemType() == WeaponType.DUAL)
				safeEnchantLevel += 1;

			if(enchlvl < safeEnchantLevel)
				chance = 100;
			else if(enchlvl > 11)
				chance = 1;
			else
			{
				// Выборка базового шанса
				if(itemType == L2Item.TYPE2_WEAPON)
				{
					L2Weapon wepToEnchant = (L2Weapon) itemToEnchant.getItem();
					boolean magewep = itemType == L2Item.TYPE2_WEAPON && crystaltype.cry >= L2Item.CRYSTAL_C && wepToEnchant.getPDamage() - wepToEnchant.getMDamage() <= wepToEnchant.getPDamage() * 0.4;
					chance = !magewep ? Config.ALT_ENCHANT_CHANCE_W : Config.ALT_ENCHANT_CHANCE_MAGE_W;

					// Штраф на двуручное оружие(немагическое)
					if(itemToEnchant.getItem().getBodyPart() == L2Item.SLOT_LR_HAND && itemToEnchant.getItem().getItemType() == WeaponType.BLUNT && !magewep)
						chance -= Config.PENALTY_TO_TWOHANDED_BLUNTS;
				}
				else
					chance = Config.ALT_ENCHANT_CHANCE_ARMOR;

				int DeltaChance = 15;

				// Основная прогрессия
				for(int i = safeEnchantLevel; i < enchlvl; i++)
				{
					if(i == safeEnchantLevel + 2)
						DeltaChance -= 5;
					if(i == safeEnchantLevel + 6)
						DeltaChance -= 5;
					chance -= DeltaChance;
				}

				// Учёт грейда
				int Delta2 = 5;
				for(int in = 0x00; in < crystaltype.ordinal(); in++)
				{
					if(in == L2Item.CRYSTAL_C)
						Delta2 -= 5;
					if(in == L2Item.CRYSTAL_B)
						Delta2 -= 5;
					if(in == L2Item.CRYSTAL_A)
						Delta2 -= 2;
					if(in == L2Item.CRYSTAL_S)
						Delta2 -= 1;
				}
				chance += Delta2;

				if(scroll.isBlessedEnchantScroll())
					chance += 2;
				if(chance < 1)
					chance = 1;
			}

		}

		if(Rnd.chance(chance))
		{

			if(itemToEnchant.getEnchantLevel() == 0)
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_SUCCESSFULLY_ENCHANTED).addItemName(itemToEnchant.getItemId()));
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessage._S1_S2_HAS_BEEN_SUCCESSFULLY_ENCHANTED);
				sm.addNumber(itemToEnchant.getEnchantLevel());
				sm.addItemName(itemToEnchant.getItemId());
				activeChar.sendPacket(sm);
			}

			itemToEnchant.setEnchantLevel(itemToEnchant.getEnchantLevel() + 1);
			itemToEnchant.updateDatabase();

			activeChar.sendPacket(new InventoryUpdate().addModifiedItem(itemToEnchant));

			Log.add(activeChar.getName() + "|Successfully enchanted|" + itemToEnchant.getItemId() + "|to+" + itemToEnchant.getEnchantLevel() + "|" + chance, "enchants");
			Log.LogItem(activeChar, Log.EnchantItem, itemToEnchant);
		}
		else
		{
			Log.add(activeChar.getName() + "|Failed to enchant|" + itemToEnchant.getItemId() + "|+" + itemToEnchant.getEnchantLevel() + "|" + chance, "enchants");

			if(scroll.isBlessedEnchantScroll() || Config.EnchantCrystalSafe && scroll.isCrystallEnchantScroll())
			{
				// Сброс заточки до указанного значения, по умолчанию 0.
				itemToEnchant.setEnchantLevel(Config.ENCHANT_BLESSED_FAIL);
				activeChar.sendPacket(new InventoryUpdate().addModifiedItem(itemToEnchant));
				activeChar.sendPacket(Msg.FAILED_IN_BLESSED_ENCHANT_THE_ENCHANT_VALUE_OF_THE_ITEM_BECAME_0);
			}
			else
			{
				if(itemToEnchant.isEquipped())
					activeChar.getInventory().unEquipItemInSlot(itemToEnchant.getEquipSlot());

				if(itemToEnchant.getEnchantLevel() == 0)
					activeChar.sendPacket(new SystemMessage(SystemMessage.THE_ENCHANTMENT_HAS_FAILED_YOUR_S1_HAS_BEEN_CRYSTALLIZED).addItemName(itemToEnchant.getItemId()));
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.THE_ENCHANTMENT_HAS_FAILED_YOUR__S1_S2_HAS_BEEN_CRYSTALLIZED).addNumber(itemToEnchant.getEnchantLevel()).addItemName(itemToEnchant.getItemId()));

				L2ItemInstance destroyedItem = inventory.destroyItem(itemToEnchant.getObjectId(), 1, true);
				if(destroyedItem == null)
				{
					_log.warning("failed to destroy " + itemToEnchant.getObjectId() + " after unsuccessful enchant attempt by char " + activeChar.getName());
					activeChar.sendActionFailed();
					return;
				}

				L2ItemInstance crystalsToAdd = ItemTable.getInstance().createItem(crystalId);

				int count = (int) (itemToEnchant.getItem().getCrystalCount() * 0.87);
				if(destroyedItem.getEnchantLevel() > 3)
					count += itemToEnchant.getItem().getCrystalCount() * 0.25 * (destroyedItem.getEnchantLevel() - 3);
				if(count < 1)
					count = 1;
				crystalsToAdd.setCount(count);

				activeChar.getInventory().addItem(crystalsToAdd);

				Log.LogItem(activeChar, Log.EnchantItemFail, itemToEnchant);
				Log.LogItem(activeChar, Log.Sys_GetItem, crystalsToAdd);

				//activeChar.sendPacket(new EnchantResult(1));
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S).addItemName(crystalsToAdd.getItemId()).addNumber(count));
				activeChar.refreshExpertisePenalty();
				activeChar.refreshOverloaded();
			}
		}

		activeChar.setEnchantScroll(null);
		activeChar.sendChanges();
		activeChar.sendPacket(new EnchantResult(itemToEnchant.getEnchantLevel()));

	}
}