package l2d.game.clientpackets;

import l2d.game.cache.Msg;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Player.TransactionType;
import l2d.game.model.L2World;
import l2d.game.serverpackets.SendTradeRequest;
import l2d.game.serverpackets.SystemMessage;
import com.lineage.util.Util;

public class TradeRequest extends L2GameClientPacket
{
	//Format: cd
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

		if(!activeChar.getPlayerAccess().UseTrade)
		{
			activeChar.sendPacket(Msg.THIS_ACCOUNT_CANOT_TRADE_ITEMS);
			activeChar.sendActionFailed();
			return;
		}

		String tradeBan = activeChar.getVar("tradeBan");
		if(tradeBan != null && (tradeBan.equals("-1") || Long.parseLong(tradeBan) >= System.currentTimeMillis()))
		{
			activeChar.sendMessage("Your trade is banned! Expires: " + (tradeBan.equals("-1") ? "never" : Util.formatTime((Long.parseLong(tradeBan) - System.currentTimeMillis()) / 1000)) + ".");
			return;
		}

		if(activeChar.isDead())
		{
			activeChar.sendActionFailed();
			return;
		}

		L2Object target = L2World.getAroundObjectById(activeChar, _objectId);

		if(target == null || !target.isPlayer() || target.getObjectId() == activeChar.getObjectId())
		{
			activeChar.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return;
		}

		L2Player pcTarget = (L2Player) target;

		if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || pcTarget.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(Msg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}

		if(!pcTarget.getPlayerAccess().UseTrade)
		{
			activeChar.sendPacket(Msg.THIS_ACCOUNT_CANOT_TRADE_ITEMS);
			activeChar.sendActionFailed();
			return;
		}

		tradeBan = pcTarget.getVar("tradeBan");
		if(tradeBan != null && (tradeBan.equals("-1") || Long.parseLong(tradeBan) >= System.currentTimeMillis()))
		{
			pcTarget.sendMessage("Your trade is banned! Expires: " + (tradeBan.equals("-1") ? "never" : Util.formatTime((Long.parseLong(tradeBan) - System.currentTimeMillis()) / 1000)) + ".");
			return;
		}

		if(!activeChar.knowsObject(target))
		{
			activeChar.sendPacket(Msg.CANNOT_SEE_TARGET);
			return;
		}

		if(pcTarget.isInOlympiadMode() || activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return;
		}

		if(pcTarget.getTradeRefusal() || pcTarget.isInBlockList(activeChar) || pcTarget.isBlockAll())
		{
			activeChar.sendPacket(Msg.YOU_HAVE_BEEN_BLOCKED_FROM_THE_CONTACT_YOU_SELECTED);
			return;
		}

		if(activeChar.isTransactionInProgress())
		{
			activeChar.sendPacket(Msg.YOU_ARE_ALREADY_TRADING_WITH_SOMEONE);
			return;
		}

		if(pcTarget.isTransactionInProgress())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addString(pcTarget.getName()));
			return;
		}

		if(activeChar.isFishing())
		{
			activeChar.sendPacket(Msg.YOU_CANNOT_DO_THAT_WHILE_FISHING);
			return;
		}

		pcTarget.setTransactionRequester(activeChar, System.currentTimeMillis() + 10000);
		pcTarget.setTransactionType(TransactionType.TRADE);
		activeChar.setTransactionRequester(pcTarget, System.currentTimeMillis() + 10000);
		activeChar.setTransactionType(TransactionType.TRADE);
		pcTarget.sendPacket(new SendTradeRequest(activeChar.getObjectId()));
		activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_REQUESTED_A_TRADE_WITH_C1).addString(pcTarget.getName()));
	}
}