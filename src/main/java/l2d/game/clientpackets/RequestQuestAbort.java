package l2d.game.clientpackets;

import l2d.game.instancemanager.QuestManager;
import l2d.game.model.L2Player;
import l2d.game.model.quest.QuestState;
import l2d.game.serverpackets.SystemMessage;

public class RequestQuestAbort extends L2GameClientPacket
{
	private int _QuestID;

	/**
	 * packet type id 0x63
	 * format: cd
	 */
	@Override
	public void readImpl()
	{
		_QuestID = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || QuestManager.getQuest(_QuestID) == null)
			return;
		QuestState qs = activeChar.getQuestState(QuestManager.getQuest(_QuestID).getName());
		if(qs != null)
		{
			qs.abortQuest();
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_ABORTED).addString(QuestManager.getQuest(_QuestID).getDescr()));
		}
	}
}