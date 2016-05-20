package commands.user;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.IUserCommandHandler;
import l2d.game.handler.UserCommandHandler;
import l2d.game.model.L2Party;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.SystemMessage;

/**
 * Support for /partyinfo command
 */
public class PartyInfo implements IUserCommandHandler, ScriptFile
{
	private static final int[] COMMAND_IDS = { 81 };

	public boolean useUserCommand(int id, L2Player activeChar)
	{
		if(id != COMMAND_IDS[0])
			return false;

		if(!activeChar.isInParty())
			return false;

		L2Party playerParty = activeChar.getParty();
		int memberCount = playerParty.getMemberCount();
		int lootDistribution = playerParty.getLootDistribution();
		String partyLeader = playerParty.getPartyLeader().getName();

		activeChar.sendPacket(new SystemMessage(SystemMessage._PARTY_INFORMATION_));

		switch(lootDistribution)
		{
			case L2Party.ITEM_LOOTER:
				activeChar.sendPacket(new SystemMessage(SystemMessage.LOOTING_METHOD_FINDERS_KEEPERS));
				break;
			case L2Party.ITEM_ORDER:
				activeChar.sendPacket(new SystemMessage(SystemMessage.LOOTING_METHOD_BY_TURN));
				break;
			case L2Party.ITEM_ORDER_SPOIL:
				activeChar.sendPacket(new SystemMessage(SystemMessage.LOOTING_METHOD_BY_TURN_INCLUDING_SPOIL));
				break;
			case L2Party.ITEM_RANDOM:
				activeChar.sendPacket(new SystemMessage(SystemMessage.LOOTING_METHOD_RANDOM));
				break;
			case L2Party.ITEM_RANDOM_SPOIL:
				activeChar.sendPacket(new SystemMessage(SystemMessage.LOOTING_METHOD_RANDOM_INCLUDING_SPOIL));
				break;
		}

		activeChar.sendPacket(new SystemMessage(SystemMessage.PARTY_LEADER_S1).addString(partyLeader));
		activeChar.sendMessage(new CustomMessage("scripts.commands.user.PartyInfo.Members", activeChar).addNumber(memberCount));
		activeChar.sendPacket(new SystemMessage(SystemMessage.__DASHES__));
		return true;
	}

	public final int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}

	public void onLoad()
	{
		UserCommandHandler.getInstance().registerUserCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}
