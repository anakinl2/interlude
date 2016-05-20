package l2d.game.clientpackets;

import l2d.Config;
import l2d.ext.multilang.CustomMessage;
import l2d.game.cache.Msg;
import l2d.game.model.L2Party;
import l2d.game.model.L2Player;
import l2d.game.model.L2Player.TransactionType;
import l2d.game.model.L2World;
import l2d.game.network.L2GameClient;
import l2d.game.serverpackets.AskJoinParty;
import l2d.game.serverpackets.SystemMessage;

public class RequestJoinParty extends L2GameClientPacket
{
	private String _name;
	private int _itemDistribution;

	@Override
	public void readImpl()
	{
		_name = readS();
		_itemDistribution = readD();
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

		if(target.isCursedWeaponEquipped() || activeChar.isCursedWeaponEquipped())
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		if(!activeChar.getPlayerAccess().CanJoinParty || activeChar.isInOlympiadMode())
		{
			activeChar.sendActionFailed();
			return;
		}

		if(!target.getPlayerAccess().CanJoinParty || target.isInOlympiadMode())
		{
			activeChar.sendActionFailed();
			return;
		}
		
		if(activeChar.getTeam() != target.getTeam() && activeChar.isChecksForTeam())
		{
			activeChar.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return;
		}

		if(target.isBlockAll())
		{
			activeChar.sendPacket(Msg.THE_PERSON_IS_IN_A_MESSAGE_REFUSAL_MODE);
			return;
		}

		if(activeChar.isTransactionInProgress())
		{
			activeChar.sendPacket(Msg.WAITING_FOR_ANOTHER_REPLY);
			return;
		}

		if(target.isInParty())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_A_MEMBER_OF_ANOTHER_PARTY_AND_CANNOT_BE_INVITED).addString(target.getName()));
			return;
		}

		if(!activeChar.isInParty())
			createNewParty(getClient(), _itemDistribution, target, activeChar);
		else
			addTargetToParty(getClient(), _itemDistribution, target, activeChar);
	}

	private void addTargetToParty(L2GameClient client, int itemDistribution, L2Player target, L2Player activeChar)
	{
		if(activeChar.getParty().getMemberCount() >= L2Party.MAX_SIZE)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
			return;
		}
		if(activeChar.getParty().isInReflection() && activeChar.getParty().getMemberCount() >= L2Party.MAX_INSTANCE_SIZE)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
			return;
		}

		// Только Party Leader может приглашать новых членов
		if(Config.PARTY_LEADER_ONLY_CAN_INVITE && !activeChar.getParty().isLeader(activeChar))
		{
			activeChar.sendPacket(Msg.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
			return;
		}

		if(activeChar.getParty().isInDimensionalRift())
		{
			activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestJoinParty.InDimensionalRift", activeChar));
			activeChar.sendActionFailed();
		}

		if(!target.isTransactionInProgress())
		{
			target.setTransactionRequester(activeChar, System.currentTimeMillis() + 10000);
			target.setTransactionType(TransactionType.PARTY);
			activeChar.setTransactionRequester(target, System.currentTimeMillis() + 10000);
			activeChar.setTransactionType(TransactionType.PARTY);

			target.sendPacket(new AskJoinParty(activeChar.getName(), itemDistribution));
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_INVITED_C1_TO_JOIN_YOUR_PARTY).addString(target.getName()));
		}
		else
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addString(target.getName()));

	}

	private void createNewParty(L2GameClient client, int itemDistribution, L2Player target, L2Player requestor)
	{
		if(!target.isTransactionInProgress())
		{
			requestor.setParty(new L2Party(requestor, itemDistribution));
			target.setTransactionRequester(requestor, System.currentTimeMillis() + 10000);
			target.setTransactionType(TransactionType.PARTY);
			requestor.setTransactionRequester(target, System.currentTimeMillis() + 10000);
			requestor.setTransactionType(TransactionType.PARTY);
			target.sendPacket(new AskJoinParty(requestor.getName(), itemDistribution));
			requestor.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_INVITED_C1_TO_JOIN_YOUR_PARTY).addString(target.getName()));
		}
		else
			requestor.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addString(target.getName()));
	}
}