package com.lineage.game.clientpackets;

import com.lineage.game.instancemanager.QuestManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.quest.Quest;

public class RequestTutorialQuestionMark extends L2GameClientPacket
{
	// format: cd
	int _number = 0;

	@Override
	public void readImpl()
	{
		_number = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getActiveChar();
		if(player == null)
			return;

		Quest q = QuestManager.getQuest(255);
		if(q != null)
			player.processQuestEvent(q.getName(), "QM" + _number);
	}
}