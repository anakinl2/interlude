package l2d.game.clientpackets;

import java.util.logging.Logger;

import l2d.game.cache.Msg;
import l2d.game.model.L2Player;
import l2d.game.model.PcInventory;
import l2d.game.model.PetInventory;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2PetInstance;
import com.lineage.util.Log;

public class RequestGetItemFromPet extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestGetItemFromPet.class.getName());

	private int _objectId;
	private int _amount;
	@SuppressWarnings("unused")
	private int _unknown;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_amount = readD();
		_unknown = readD();// = 0 for most trades
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
		if(pet == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		PetInventory petInventory = pet.getInventory();
		PcInventory playerInventory = activeChar.getInventory();

		L2ItemInstance petItem = petInventory.getItemByObjectId(_objectId);

		if(petItem == null)
		{
			_log.warning(activeChar.getName() + " requested item obj_id: " + _objectId + " from pet, but its not there.");
			return;
		}

		if(petItem.isEquipped())
		{
			activeChar.sendActionFailed();
			return;
		}

		long finalLoad = petItem.getItem().getWeight() * _amount;

		if(!activeChar.getInventory().validateWeight(finalLoad))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return;
		}

		L2ItemInstance item = petInventory.dropItem(_objectId, _amount);
		item.setCustomFlags(item.getCustomFlags() ^ L2ItemInstance.FLAG_PET_EQUIPPED);
		playerInventory.addItem(item);

		pet.sendItemList();
		pet.broadcastPetInfo();

		Log.LogItem(activeChar, activeChar.getPet(), Log.GetItemFromPet, petItem);
	}
}