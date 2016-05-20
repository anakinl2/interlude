package l2d.game.clientpackets;

import l2d.ext.multilang.CustomMessage;
import l2d.game.model.L2Player;

public class RequestWithDrawalParty extends L2GameClientPacket
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
		if(activeChar.isInParty())
			if(activeChar.getParty().isInDimensionalRift() && !activeChar.getParty().getDimensionalRift().getRevivedAtWaitingRoom().contains(activeChar.getObjectId()))
				activeChar.sendMessage(new CustomMessage("l2d.game.clientpackets.RequestWithDrawalParty.Rift", activeChar));
			else if(activeChar.getParty().isInReflection() && activeChar.isInCombat())
				activeChar.sendMessage("Вы не можете сейчас выйти из группы.");
			else
				activeChar.getParty().oustPartyMember(activeChar);
	}
}