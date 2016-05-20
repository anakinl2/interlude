package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.PcInventory;
import l2d.game.model.PetInventory;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2PetInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.PetDataTable;
import com.lineage.util.Log;

public class RequestGiveItemToPet extends L2GameClientPacket
{
	private int _objectId;
	private int _amount;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_amount = readD();
	}

	@Override
	public void runImpl()
	{
		if(_amount < 1)
			return;
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2PetInstance pet = (L2PetInstance) activeChar.getPet();
		if(pet == null || pet.isDead())
		{
			sendPacket(new SystemMessage(SystemMessage.CANNOT_GIVE_ITEMS_TO_A_DEAD_PET));
			return;
		}

		if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			sendPacket(new SystemMessage(SystemMessage.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM));
			return;
		}

		if(_objectId == pet.getControlItemObjId())
		{
			activeChar.sendActionFailed();
			return;
		}

		PetInventory petInventory = pet.getInventory();
		PcInventory playerInventory = activeChar.getInventory();

		L2ItemInstance playerItem = playerInventory.getItemByObjectId(_objectId);
		if(playerItem == null || playerItem.getObjectId() == pet.getControlItemObjId() || PetDataTable.isPetControlItem(playerItem))
		{
			activeChar.sendActionFailed();
			return;
		}

		if(pet.getInventory().getTotalWeight() + playerItem.getItem().getWeight() * _amount >= pet.getMaxLoad())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.EXCEEDED_PET_INVENTORYS_WEIGHT_LIMIT));
			return;
		}

		if(!playerItem.canBeDropped(activeChar))
		{
			activeChar.sendActionFailed();
			return;
		}

		if(_amount >= playerItem.getIntegerLimitedCount())
		{
			playerInventory.dropItem(_objectId, playerItem.getIntegerLimitedCount());
			playerItem.setCustomFlags(playerItem.getCustomFlags() | L2ItemInstance.FLAG_PET_EQUIPPED);
			petInventory.addItem(playerItem);
		}
		else
		{
			L2ItemInstance newPetItem = playerInventory.dropItem(_objectId, _amount);
			petInventory.addItem(newPetItem);
		}

		pet.sendItemList();
		pet.broadcastPetInfo();

		Log.LogItem(activeChar, pet, Log.GiveItemToPet, playerItem);
	}
}