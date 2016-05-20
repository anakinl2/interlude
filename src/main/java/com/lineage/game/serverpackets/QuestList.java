package com.lineage.game.serverpackets;

import javolution.util.FastMap;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.quest.Quest;
import com.lineage.game.model.quest.QuestState;

/**
 * format: h[dd]b
 */
public class QuestList extends L2GameServerPacket
{
	private FastMap<Integer, Integer> questlist = new FastMap<Integer, Integer>();
	private static byte[] unk = new byte[128];

	public QuestList(L2Player player)
	{
		if(player == null || player.getAllActiveQuests() == null)
			return;
		QuestState qs;
		for(Quest quest : player.getAllActiveQuests())
			if(quest != null && quest.getQuestIntId() < 999)
			{
				qs = player.getQuestState(quest.getName());
				questlist.put(quest.getQuestIntId(), qs == null ? 0 : qs.getInt("cond")); // stage of quest progress
			}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x80);
		writeH(questlist.size());
		for(int q_id : questlist.keySet())
		{
			writeD(q_id);
			writeD(questlist.get(q_id));
		}
		writeB(unk);
	}
}