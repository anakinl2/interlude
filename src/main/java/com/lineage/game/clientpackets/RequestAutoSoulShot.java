package com.lineage.game.clientpackets;

import com.lineage.game.cache.Msg;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.ExAutoSoulShot;
import com.lineage.game.serverpackets.SystemMessage;

/**
 * format:		chdd
 * @param decrypt
 */
public class RequestAutoSoulShot extends L2GameClientPacket
{
	private int _itemId;
	private boolean _type; // 1 = on : 0 = off;

	@Override
	public void readImpl()
	{
		_itemId = readD();
		_type = readD() == 1;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || activeChar.getTransactionRequester() != null || activeChar.isDead())
			return;

		L2ItemInstance item = activeChar.getInventory().getItemByItemId(_itemId);

		if(item == null)
			return;

		if(_itemId >= 3947 && _itemId <= 3952 && activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(Msg.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		if(_type)
		{
			activeChar.addAutoSoulShot(_itemId);
			activeChar.sendPacket(new ExAutoSoulShot(_itemId, true));
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_USE_OF_S1_WILL_NOW_BE_AUTOMATED).addString(item.getItem().getName()));
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(_itemId);
			handler.useItem(activeChar, item);
			return;
		}

		activeChar.removeAutoSoulShot(_itemId);
		activeChar.sendPacket(new ExAutoSoulShot(_itemId, false));
		activeChar.sendPacket(new SystemMessage(SystemMessage.THE_AUTOMATIC_USE_OF_S1_WILL_NOW_BE_CANCELLED).addString(item.getItem().getName()));
	}
}