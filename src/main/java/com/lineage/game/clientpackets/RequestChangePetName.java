package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Summon;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2PetInstance;
import com.lineage.game.serverpackets.SystemMessage;

public class RequestChangePetName extends L2GameClientPacket
{
	// format: cS

	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player cha = getClient().getActiveChar();
		L2Summon pet = cha.getPet();
		if(pet != null && pet.getName() == null)
		{
			if(_name.length() > 8)
			{
				sendPacket(new SystemMessage(SystemMessage.YOUR_PETS_NAME_CAN_BE_UP_TO_8_CHARACTERS));
				return;
			}
			pet.setName(_name);
			pet.broadcastPetInfo();

			if(pet.isPet())
			{
				L2PetInstance _pet = (L2PetInstance) pet;
				L2ItemInstance controlItem = _pet.getControlItem();
				if(controlItem != null)
				{
					controlItem.setCustomType2(1);
					controlItem.setPriceToSell(0); // Костыль, иначе CustomType2 = 1 не пишется в базу
					controlItem.updateDatabase();
					_pet.updateControlItem();
				}
			}

		}
	}
}