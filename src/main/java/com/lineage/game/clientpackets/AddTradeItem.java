package com.lineage.game.clientpackets;

import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2TradeList;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.SendTradeDone;
import com.lineage.game.serverpackets.TradeOtherAdd;
import com.lineage.game.serverpackets.TradeOwnAdd;

/**
 * [C] 16 AddTradeItem <p>
 * <b>Format:</b> cddd
 * 
 * @author Felixx
 */
public class AddTradeItem extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _tradeId, _objectId;
	private int _amount;

	@Override
	public void readImpl()
	{
		_tradeId = readD(); // 1 ?
		_objectId = readD();
		_amount = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || _amount < 1)
			return;
		L2Player requestor = activeChar.getTransactionRequester();

		L2TradeList playerItemList = activeChar.getTradeList();

		if(requestor == null || requestor.getTransactionRequester() == null)
		{
			// Партнер по передачи вышел из игры, трейд отменяется.
			activeChar.sendPacket(new SendTradeDone(0));
			activeChar.sendPacket(Msg.THAT_PLAYER_IS_NOT_ONLINE);
			activeChar.setTransactionRequester(null);
			if(playerItemList != null && playerItemList.getItems() != null)
				playerItemList.getItems().clear();
			return;
		}

		if(requestor.getTradeList() == null || playerItemList == null)
		{
			activeChar.sendPacket(Msg.SYSTEM_ERROR);
			activeChar.sendActionFailed();
			return;
		}

		if(requestor.getTradeList().hasConfirmed() || playerItemList.hasConfirmed())
		{
			activeChar.sendPacket(Msg.YOU_CANNOT_MOVE_ADDITIONAL_ITEMS_BECAUSE_TRADE_HAS_BEEN_CONFIRMED);
			activeChar.sendActionFailed();
			return;
		}

		L2ItemInstance InvItem = activeChar.getInventory().getItemByObjectId(_objectId);

		if(InvItem == null || !InvItem.canBeTraded(activeChar))
		{
			activeChar.sendPacket(Msg.THIS_ITEM_CANNOT_BE_TRADED_OR_SOLD);
			return;
		}

		int InvItemCount = InvItem.getIntegerLimitedCount();

		L2ItemInstance TradeItem;

		int realCount = _amount;

		if(_amount > InvItemCount)
			realCount = InvItemCount;
		long leaveCount = InvItemCount - realCount;
		if(playerItemList.getItems().isEmpty() || !playerItemList.contains(_objectId))
		{
			TradeItem = new L2ItemInstance(_objectId, InvItem.getItem());
			TradeItem.setCount(realCount);
			TradeItem.setEnchantLevel(InvItem.getEnchantLevel());
			playerItemList.addItem(TradeItem);
		}
		else
		{
			TradeItem = playerItemList.getItem(_objectId);
			if(TradeItem == null || !TradeItem.canBeTraded(activeChar))
				return;
			int TradeItemCount = TradeItem.getIntegerLimitedCount();
			if(InvItemCount == TradeItemCount)
				return;

			if(_amount + TradeItemCount > Integer.MAX_VALUE)
			{
				activeChar.sendPacket(Msg.SYSTEM_ERROR);
				activeChar.sendActionFailed();
				return;
			}

			if(_amount + TradeItemCount >= InvItemCount)
				realCount = InvItemCount - TradeItemCount;

			TradeItem.setCount(realCount + TradeItemCount);
			leaveCount = InvItemCount - realCount - TradeItemCount;
		}

		activeChar.sendPacket(new TradeOwnAdd(InvItem, realCount));
		requestor.sendPacket(new TradeOtherAdd(InvItem, realCount));
	}
}