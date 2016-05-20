package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.templates.L2Item;

public class RequestUnEquipItem extends L2GameClientPacket
{
	private int _slot;

	/**
	 * packet type id 0x16
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		_slot = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		// Нельзя снимать проклятое оружие
		if(_slot == L2Item.SLOT_LR_HAND && activeChar.isCursedWeaponEquipped())
			return;

		activeChar.getInventory().unEquipItemInBodySlotAndNotify(activeChar, _slot, null);
	}
}