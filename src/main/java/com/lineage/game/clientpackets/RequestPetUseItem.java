package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2PetInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.PetDataTable;

public class RequestPetUseItem extends L2GameClientPacket
{
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

		L2PetInstance pet = (L2PetInstance) activeChar.getPet();
		if(pet == null)
			return;

		L2ItemInstance item = pet.getInventory().getItemByObjectId(_objectId);

		if(item == null || item.getCount() <= 0)
			return;

		int itemId = item.getItemId();

		if(activeChar.isAlikeDead() || pet.isDead())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addItemName(item.getItemId()));
			return;
		}

		int petId = pet.getNpcId();

		if(pet.tryEquipItem(item, true))
			return;

		// manual pet feeding
		if(PetDataTable.getFoodId(petId) == itemId)
		{
			feed(activeChar, pet, item);
			return;
		}

		L2Skill[] skills = item.getItem().getAttachedSkills();
		if(skills != null && skills.length > 0)
			for(L2Skill skill : skills)
			{
				L2Character aimingTarget = skill.getAimingTarget(pet, pet.getTarget());
				if(skill.checkCondition(pet, aimingTarget, false, false, true))
					pet.getAI().Cast(skill, aimingTarget, false, false);
			}
		else
			activeChar.sendPacket(new SystemMessage(SystemMessage.ITEM_NOT_AVAILABLE_FOR_PETS));
	}

	private void feed(L2Player activeChar, L2PetInstance pet, L2ItemInstance item)
	{
		int newFed = Math.min(pet.getMaxFed(), pet.getCurrentFed() + Math.max(pet.getMaxFed() * pet.getAddFed() / 100, 1));
		if(pet.getCurrentFed() != newFed)
		{
			pet.removeItemFromInventory(item, 1, true);
			pet.setCurrentFed(newFed);
			pet.sendPetInfo();
		}
	}
}