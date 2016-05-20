package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.util.Location;
import com.lineage.util.Log;

/**
 * format: cdd ddd
 * cdQ ddd - gracia final
 */
public class RequestDropItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestDropItem.class.getName());

	private int _objectId;
	private int _count;
	private Location _loc = new Location(0, 0, 0);

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_count = readD();
		_loc.x = readD();
		_loc.y = readD();
		_loc.z = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || activeChar.isDead())
			return;

		if(_count < 1 || _loc.x == 0 || _loc.y == 0 || _loc.z == 0)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(!Config.ALLOW_DISCARDITEM)
		{
			activeChar.sendMessage(new CustomMessage("com.lineage.game.clientpackets.RequestDropItem.Disallowed", activeChar));
			return;
		}

		if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(Msg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}

		if(activeChar.isTransactionInProgress())
		{
			sendPacket(Msg.NOTHING_HAPPENED);
			return;
		}

		if(activeChar.isFishing())
		{
			activeChar.sendPacket(Msg.YOU_CANNOT_DO_THAT_WHILE_FISHING);
			return;
		}

		if(activeChar.isActionsDisabled() || activeChar.isSitting() || activeChar.isDropDisabled())
		{
			activeChar.sendActionFailed();
			return;
		}

		if(!activeChar.isInRangeSq(_loc, 22500) || Math.abs(_loc.z - activeChar.getZ()) > 50)
		{
			if(Config.DEBUG)
				_log.finest(activeChar.getObjectId() + ": trying to drop too far away");
			activeChar.sendPacket(Msg.TOO_FAR_TO_DISCARD);
			return;
		}

		L2ItemInstance oldItem = activeChar.getInventory().getItemByObjectId(_objectId);
		if(oldItem == null)
		{
			_log.warning(activeChar.getName() + ":tried to drop an item that is not in the inventory ?!?:" + _objectId);
			return;
		}

		if(!oldItem.canBeDropped(activeChar))
		{
			activeChar.sendPacket(Msg.THAT_ITEM_CANNOT_BE_DISCARDED);
			return;
		}

		int oldCount = oldItem.getIntegerLimitedCount();
		if(oldCount < _count)
		{
			activeChar.sendActionFailed();
			return;
		}

		oldItem.setWhFlag(true);
		L2ItemInstance dropedItem = activeChar.getInventory().dropItem(_objectId, _count);
		oldItem.setWhFlag(false);

		if(dropedItem == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		dropedItem.dropToTheGround(activeChar, _loc);
		activeChar.disableDrop(1000);
		Log.LogItem(activeChar, Log.Drop, dropedItem);
		activeChar.updateStats();
	}
}