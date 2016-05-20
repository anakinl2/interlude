package com.lineage.game.clientpackets;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.model.L2Party;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.SevenSignsFestival.SevenSignsFestival;
import com.lineage.game.model.entity.olympiad.Olympiad;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.SkillTable;

/**
 * [C] 09 Logout
 * @author Felixx
 */
public class Logout extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		// Dont allow leaving if player is fighting
		if(activeChar.isInCombat())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_EXIT_WHILE_IN_COMBAT));
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isFishing())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DO_ANYTHING_ELSE_WHILE_FISHING));
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.getTeam() > 0)
		{
			activeChar.sendMessage("Can't logout while in event.");
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isFlying())
			activeChar.removeSkill(SkillTable.getInstance().getInfo(4289, 1));

		if(activeChar.isBlocked())
		{
			activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.Logout.OutOfControl", activeChar));
			activeChar.sendActionFailed();
			return;
		}

		// Prevent player from logging out if they are a festival participant
		// and it is in progress, otherwise notify party members that the player
		// is not longer a participant.
		if(activeChar.isFestivalParticipant())
		{
			if(SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				activeChar.sendMessage("You cannot log out while you are a participant in a festival.");
				return;
			}
			L2Party playerParty = activeChar.getParty();
			if(playerParty != null)
				playerParty.broadcastMessageToPartyMembers(activeChar.getName() + " has been removed from the upcoming festival.");
		}

		if(activeChar.isInOlympiadMode() || Olympiad.isRegistered(activeChar))
		{
			activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.Logout.Olympiad", activeChar));
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.inObserverMode())
		{
			activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.Logout.Observer", activeChar));
			activeChar.sendActionFailed();
			return;
		}

		activeChar.logout(false, false, false);
	}
}