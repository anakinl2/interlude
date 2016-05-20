package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.quest.Quest;
import com.lineage.game.model.quest.QuestState;

public class GMViewQuestList extends L2GameServerPacket
{
	private L2Player _activeChar;

	public GMViewQuestList(L2Player cha)
	{
		_activeChar = cha;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x93);
		writeS(_activeChar.getName());

		Quest[] questList = _activeChar.getAllActiveQuests();

		if(questList.length == 0)
		{
			writeC(0);
			writeH(0);
			writeH(0);
			return;
		}

		writeH(questList.length); // quest count

		for(Quest q : questList)
		{
			writeD(q.getQuestIntId());

			QuestState qs = _activeChar.getQuestState(q.getName());

			if(qs == null)
			{
				writeD(0);
				continue;
			}

			writeD(qs.getInt("cond")); // stage of quest progress
		}
	}
}