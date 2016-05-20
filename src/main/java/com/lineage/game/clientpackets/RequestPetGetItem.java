package com.lineage.game.clientpackets;

import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2PetInstance;
import com.lineage.game.serverpackets.SystemMessage;

public class RequestPetGetItem extends L2GameClientPacket
{
	// format: cd
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
		L2ItemInstance item = (L2ItemInstance) L2World.findObject(_objectId);
		if(item == null || (item.getCustomFlags() & L2ItemInstance.FLAG_EQUIP_ON_PICKUP) == L2ItemInstance.FLAG_EQUIP_ON_PICKUP)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getPet() instanceof L2PetInstance)
		{
			if(item.getDropTimeOwner() != 0 && item.getItemDropOwner() != null && item.getDropTimeOwner() > System.currentTimeMillis() && activeChar != item.getItemDropOwner() && (!activeChar.isInParty() || activeChar.isInParty() && item.getItemDropOwner().isInParty() && activeChar.getParty() != item.getItemDropOwner().getParty()))
			{
				SystemMessage sm;
				if(item.getItemId() == 57)
				{
					sm = new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
					sm.addNumber(item.getCount());
				}
				else
				{
					sm = new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_PICK_UP_S1);
					sm.addItemName(item.getItemId());
				}
				sendPacket(sm);
				activeChar.sendActionFailed();
				return;
			}

			L2PetInstance pet = (L2PetInstance) activeChar.getPet();
			if(pet == null || pet.isDead() || pet.isOutOfControl())
			{
				activeChar.sendActionFailed();
				return;
			}
			pet.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, item, null);
		}
		else
		{
			activeChar.sendActionFailed();
			return;
		}
	}
}