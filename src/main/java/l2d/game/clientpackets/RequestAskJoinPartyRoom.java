package l2d.game.clientpackets;

import l2d.game.cache.Msg;
import l2d.game.instancemanager.PartyRoomManager;
import l2d.game.model.L2Player;
import l2d.game.model.L2Player.TransactionType;
import l2d.game.model.L2World;
import l2d.game.model.PartyRoom;
import l2d.game.serverpackets.ExAskJoinPartyRoom;
import l2d.game.serverpackets.SystemMessage;

/**
 * format: (ch)S
 */
public class RequestAskJoinPartyRoom extends L2GameClientPacket
{
	private String _name; // not tested, just guessed

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		L2Player target = L2World.getPlayer(_name);

		if(target == null || target == activeChar)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getPartyRoom() <= 0)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isTransactionInProgress())
		{
			activeChar.sendPacket(Msg.WAITING_FOR_ANOTHER_REPLY);
			return;
		}

		if(target.isTransactionInProgress())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addString(target.getName()));
			return;
		}

		if(target.getPartyRoom() > 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_A_MEMBER_OF_ANOTHER_PARTY_AND_CANNOT_BE_INVITED).addString(target.getName()));
			return;
		}

		PartyRoom room = PartyRoomManager.getInstance().getRooms().get(activeChar.getPartyRoom());
		if(room == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(room.getMembers().size() >= room.getMaxMembers())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
			return;
		}

		if(!PartyRoomManager.getInstance().isLeader(activeChar))
		{
			activeChar.sendPacket(Msg.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
			return;
		}

		target.setTransactionRequester(activeChar, System.currentTimeMillis() + 10000);
		target.setTransactionType(TransactionType.PARTY_ROOM);
		activeChar.setTransactionRequester(target, System.currentTimeMillis() + 10000);
		activeChar.setTransactionType(TransactionType.PARTY_ROOM);

		target.sendPacket(new ExAskJoinPartyRoom(activeChar.getName()));
		activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_INVITED_YOU_TO_ENTER_THE_PARTY_ROOM).addString(target.getName()));
	}
}