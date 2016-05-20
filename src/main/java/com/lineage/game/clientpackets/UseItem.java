package com.lineage.game.clientpackets;

import java.nio.BufferUnderflowException;

import com.lineage.Config;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.instancemanager.CursedWeaponsManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.ItemList;
import com.lineage.game.serverpackets.ShowCalc;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;

public class UseItem extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _objectId, _unknown;

	/**
	 * packet type id 0x19
	 * format:		cdd
	 */
	@Override
	public void readImpl()
	{
		try
		{
			_objectId = readD();
			_unknown = readD();
		}
		catch(BufferUnderflowException e)
		{
			e.printStackTrace();
			_log.info("Attention! Possible cheater found! Login:" + getClient().getLoginName());
		}
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(System.currentTimeMillis() - activeChar.getLastEquipmPacket() < Config.EQUIPM_PACKET_DELAY)
		{
			activeChar.sendActionFailed();
			return;
		}

		activeChar.setLastEquipmPacket();

		if(activeChar.isOutOfControl())
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getUnstuck() != 0)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_USE_ITEMS_IN_A_PRIVATE_STORE_OR_PRIVATE_WORK_SHOP));
			activeChar.sendActionFailed();
			return;
		}

		synchronized (activeChar.getInventory())
		{
			L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);

			if(item == null)
				return;

			int itemId = item.getItemId();
			if(itemId == 57)
				return;

			if(activeChar.isFishing() && (itemId < 6535 || itemId > 6540))
			{
				// You cannot do anything else while fishing
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DO_ANYTHING_ELSE_WHILE_FISHING));
				return;
			}

			if(activeChar.isDead())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addItemName(itemId));
				return;
			}

			if(item.getItem().isForPet())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_EQUIP_A_PET_ITEM).addItemName(itemId));
				return;
			}

			// Маги не могут вызывать Baby Buffalo Improved
			if(activeChar.isMageClass() && item.getItemId() == 10311)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addItemName(itemId));
				return;
			}

			// Войны не могут вызывать Improved Baby Kookaburra
			if(!activeChar.isMageClass() && item.getItemId() == 10313)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addItemName(itemId));
				return;
			}

			if(item.isEquipable())
			{
				if(activeChar.isCastingNow())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_EQUIP_ITEMS_WHILE_CASTING_OR_PERFORMING_A_SKILL));
					return;
				}

				// Нельзя снимать/одевать любое снаряжение при этих условиях
				if(activeChar.isStunned() || activeChar.isSleeping() || activeChar.isParalyzed() || activeChar.isAlikeDead())
				{
					activeChar.sendActionFailed();
					return;
				}

				int bodyPart = item.getBodyPart();

				// Нельзя снимать/одевать оружие, сидя на пете
				if(activeChar.isMounted() && (bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND))
					return;

				// Нельзя снимать/одевать проклятое оружие
				if(CursedWeaponsManager.getInstance().isCursed(itemId))
					return;

				// Don't allow weapon/shield hero equipment during Olympiads
				if(activeChar.isInOlympiadMode() && item.isHeroItem())
					return;

				if(item.isEquipped())
				{
					activeChar.getInventory().unEquipItemInBodySlotAndNotify(activeChar, item.getBodyPart(), item);
					return;
				}

				activeChar.getInventory().equipItem(item);
				if(!item.isEquipped())
					return;

				SystemMessage sm;
				if(item.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessage.EQUIPPED__S1_S2);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(itemId);
				}
				else
					sm = new SystemMessage(SystemMessage.YOU_HAVE_EQUIPPED_YOUR_S1).addItemName(itemId);
				activeChar.sendPacket(sm);
				activeChar.refreshExpertisePenalty();

				if(item.getItem().getType2() == L2Item.TYPE2_ACCESSORY)
				{
					activeChar.sendUserInfo(true);
					// TODO убрать, починив предварительно отображение бижы
					activeChar.sendPacket(new ItemList(activeChar, false));
				}
				else
					activeChar.broadcastUserInfo(true);
				return;
			}

			if(itemId == 4393)
			{
				activeChar.sendPacket(new ShowCalc(itemId));
				return;
			}

			if(ItemTable.useHandler(activeChar, item))
				return;

			IItemHandler handler = ItemHandler.getInstance().getItemHandler(itemId);
			if(handler != null)
				handler.useItem(activeChar, item);
		}
	}
}