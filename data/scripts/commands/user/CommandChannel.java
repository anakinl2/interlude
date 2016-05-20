package commands.user;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IUserCommandHandler;
import com.lineage.game.handler.UserCommandHandler;
import com.lineage.game.model.L2CommandChannel;
import com.lineage.game.model.L2Party;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.ExMultiPartyCommandChannelInfo;
import com.lineage.game.serverpackets.SystemMessage;

/**
 * Support for CommandChannel commands:<br>
 * 92	/channelcreate<br>
 * 93	/channeldelete<br>
 * 94	/channelinvite [party leader] отправляет пакет RequestExMPCCAskJoin<br>
 * 95	/channelkick [party leader] отправляет пакет RequestExMPCCExit<br>
 * 96	/channelleave<br>
 * 97	/channelinfo<br>
 */
public class CommandChannel implements IUserCommandHandler, ScriptFile
{
	private static final int[] COMMAND_IDS = { 92, 93, 96, 97 };
	private static final int STRATEGY_GUIDE_ID = 8871;
	private static final int CLAN_IMPERIUM_ID = 391;

	public boolean useUserCommand(int id, L2Player activeChar)
	{
		if(id != COMMAND_IDS[0] && id != COMMAND_IDS[1] && id != COMMAND_IDS[2] && id != COMMAND_IDS[3])
			return false;

		switch(id)
		{
			case 92: //channelcreate
				// CC могут создавать только лидеры партий, состоящие в клане ранком не ниже барона, так же не состоящие еще в СС
				if(activeChar.getClan() == null || !activeChar.isInParty() || !activeChar.getParty().isLeader(activeChar) || !activeChar.isClanLeader() || activeChar.getParty().isInCommandChannel())
					return false;

				boolean haveSkill = false;
				// CC моно создать, если есть клановый скилл Clan Imperium
				for(L2Skill skill : activeChar.getAllSkills())
					if(skill.getId() == CLAN_IMPERIUM_ID)
					{
						haveSkill = true;
						break;
					}

				boolean haveItem = false;
				// Ищем Strategy Guide в инвентаре
				if(activeChar.getInventory().getItemByItemId(STRATEGY_GUIDE_ID) != null)
					haveItem = true;

				if(!haveSkill && !haveItem)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_USE_THE_COMMAND_CHANNEL));
					return false;
				}

				// Скила нету, придется расходовать предмет
				if(!haveSkill && haveItem)
				{
					activeChar.getInventory().destroyItemByItemId(STRATEGY_GUIDE_ID, 1, false);
					activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(STRATEGY_GUIDE_ID));
				}

				new L2CommandChannel(activeChar); // Создаём Command Channel
				activeChar.sendPacket(new SystemMessage(SystemMessage.THE_COMMAND_CHANNEL_HAS_BEEN_FORMED));
				break;
			case 93: //channeldelete
				if(!activeChar.isInParty() || !activeChar.getParty().isInCommandChannel())
					return true;
				if(activeChar.getParty().getCommandChannel().getChannelLeader() == activeChar)
				{
					L2CommandChannel channel = activeChar.getParty().getCommandChannel();
					channel.disbandChannel();
				}
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CREATOR_OF_A_CHANNEL_CAN_USE_THE_CHANNEL_DISMISS_COMMAND));
				break;
			case 96: //channelleave
				//FIXME создатель канала вылетел, надо автоматом передать кому-то права
				if(!activeChar.isInParty() || !activeChar.getParty().isInCommandChannel())
					return true;
				if(!activeChar.getParty().isLeader(activeChar))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.ONLY_A_PARTY_LEADER_CAN_CHOOSE_THE_OPTION_TO_LEAVE_A_CHANNEL));
					return true;
				}
				L2CommandChannel channel = activeChar.getParty().getCommandChannel();

				//Лидер СС не может покинуть СС, можно только распустить СС
				//FIXME по идее может, права автоматом должны передаться другой партии
				if(channel.getChannelLeader() == activeChar)
				{
					if(channel.getParties().size() > 1)
						return false;

					// Закрываем СС, если в СС 1 партия и лидер нажал Quit
					channel.disbandChannel();
					return true;
				}

				L2Party party = activeChar.getParty();
				channel.removeParty(party);
				party.broadcastToPartyMembers(new SystemMessage(SystemMessage.YOU_HAVE_QUIT_THE_COMMAND_CHANNEL));
				channel.broadcastToChannelMembers(new SystemMessage(SystemMessage.S1_PARTY_HAS_LEFT_THE_COMMAND_CHANNEL).addString(activeChar.getName()));
				break;
			case 97: //channelinfo
				if(!activeChar.isInParty() || !activeChar.getParty().isInCommandChannel())
					return false;
				activeChar.sendPacket(new ExMultiPartyCommandChannelInfo(activeChar.getParty().getCommandChannel()));
				break;
		}
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