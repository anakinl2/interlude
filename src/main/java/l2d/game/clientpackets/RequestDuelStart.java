package l2d.game.clientpackets;

import java.util.logging.Logger;

import l2d.Config;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.entity.Duel;
import l2d.game.serverpackets.ExDuelAskStart;
import l2d.game.serverpackets.SystemMessage;

public class RequestDuelStart extends L2GameClientPacket
{
	// format: (ch)Sd
	private static Logger _log = Logger.getLogger(RequestDuelStart.class.getName());
	private String _name;
	private int _duelType;

	@Override
	public void readImpl()
	{
		_name = readS();
		_duelType = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		L2Player target = L2World.getPlayer(_name);
		if(activeChar == null)
			return;

		if(target == null || target == activeChar)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL));
			return;
		}

		// Check if duel is possible
		if(!Duel.checkIfCanDuel(activeChar, activeChar, true) || !Duel.checkIfCanDuel(activeChar, target, true))
			return;

		// Duel is a party duel
		if(_duelType == 1)
		{
			/* Заглушка, нам не известны координаты стадионов для парти,
			 * а сваливать всех в одно место - бред.
			 * Собственно говоря нужно найти координаты, сделать обработку стадионов, убрать заглушку.
			 */
			if(true)
			{
				activeChar.sendMessage("Sorry, but party duels are currently disabled. If you know coords of duel stadium pleace contact developers.");
				return;
			}

			// Player must be in a party & the party leader
			if(!activeChar.isInParty() || !(activeChar.isInParty() && activeChar.getParty().isLeader(activeChar)))
			{
				activeChar.sendMessage("You have to be the leader of a party in order to request a party duel.");
				return;
			}
			// Target must be in a party
			else if(!target.isInParty())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.SINCE_THE_PERSON_YOU_CHALLENGED_IS_NOT_CURRENTLY_IN_A_PARTY_THEY_CANNOT_DUEL_AGAINST_YOUR_PARTY));
				return;
			}
			// Target may not be of the same party
			else if(activeChar.getParty().getPartyMembers().contains(target))
			{
				activeChar.sendMessage("This player is a member of your own party.");
				return;
			}

			// Check if every player is ready for a duel
			for(L2Player temp : activeChar.getParty().getPartyMembers())
				if(!Duel.checkIfCanDuel(activeChar, temp, false))
				{
					activeChar.sendMessage("Not all the members of your party are ready for a duel.");
					return;
				}
			L2Player partyLeader = null; // snatch party leader of target's party
			for(L2Player temp : target.getParty().getPartyMembers())
			{
				if(target.getParty().getPartyLeaderOID() == temp.getObjectId())
					partyLeader = temp;

				if(!Duel.checkIfCanDuel(activeChar, temp, false))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL));
					return;
				}
			}

			//Никогда не должно случатся, если случилось то кто-то сломал L2Party
			if(partyLeader == null)
			{
				_log.warning("Some asshole has broken L2Party. Can't get party leader.");
				return;
			}

			// Send request to target's party leader
			if(!partyLeader.isTransactionInProgress())
			{
				activeChar.setTransactionRequester(partyLeader);
				partyLeader.setTransactionRequester(activeChar);
				partyLeader.sendPacket(new ExDuelAskStart(activeChar.getName(), _duelType));

				if(Config.DEBUG)
					_log.fine(activeChar.getName() + " requested a duel with " + partyLeader.getName());

				SystemMessage msg = new SystemMessage(SystemMessage.S1S_PARTY_HAS_BEEN_CHALLENGED_TO_A_DUEL);
				msg.addString(partyLeader.getName());
				activeChar.sendPacket(msg);

				msg = new SystemMessage(SystemMessage.S1S_PARTY_HAS_CHALLENGED_YOUR_PARTY_TO_A_DUEL);
				msg.addString(activeChar.getName());
				target.sendPacket(msg);
			}
			else
			{
				SystemMessage msg = new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER);
				msg.addString(partyLeader.getName());
				activeChar.sendPacket(msg);
			}
		}
		else if(!target.isTransactionInProgress())
		{
			activeChar.setTransactionRequester(target);
			target.setTransactionRequester(activeChar);
			target.sendPacket(new ExDuelAskStart(activeChar.getName(), _duelType));

			if(Config.DEBUG)
				_log.fine(activeChar.getName() + " requested a duel with " + target.getName());

			SystemMessage msg = new SystemMessage(SystemMessage.S1_HAS_BEEN_CHALLENGED_TO_A_DUEL);
			msg.addString(target.getName());
			activeChar.sendPacket(msg);

			msg = new SystemMessage(SystemMessage.S1_HAS_CHALLENGED_YOU_TO_A_DUEL);
			msg.addString(activeChar.getName());
			target.sendPacket(msg);
		}
		else
		{
			SystemMessage msg = new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER);
			msg.addString(target.getName());
			activeChar.sendPacket(msg);
		}
	}
}