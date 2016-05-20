package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.quest.Quest;
import com.lineage.game.model.quest.QuestState;

public class GMViewQuestInfo extends L2GameServerPacket
{
	private final L2Player _cha;

	public GMViewQuestInfo(L2Player cha)
	{
		_cha = cha;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x93);
		writeS(_cha.getName());

		Quest[] quests = _cha.getAllActiveQuests();

		if(quests.length == 0)
		{
			writeH(0);
			writeH(0);
			return;
		}

		writeH(quests.length);

		for(Quest q : quests)
		{
			writeD(q.getQuestIntId());

			QuestState qs = _cha.getQuestState(q.getName());

			if(qs == null)
			{
				writeD(0);
				continue;
			}

			writeD(qs.getInt("cond"));
		}
	}
}