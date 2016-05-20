package com.lineage.game.model.instances;

import java.util.logging.Logger;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.Functions;
import com.lineage.game.instancemanager.QuestManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.quest.Quest;
import com.lineage.game.model.quest.QuestEventType;
import com.lineage.game.model.quest.QuestState;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.templates.L2NpcTemplate;

public final class L2GuideInstance extends L2NpcInstance
{
	private static Logger _log = Logger.getLogger(L2GuideInstance.class.getName());

	public L2GuideInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		Quest q = QuestManager.getQuest(999);
		showQuestWindow(player, q.getName());
	}

	@Override
	public void showQuestWindow(final L2Player player, final String questId)
	{
		if(!player.isQuestContinuationPossible())
			return;

		try
		{
			// Get the state of the selected quest
			QuestState qs = player.getQuestState(questId);
			if(qs != null)
			{
				if(qs.isCompleted())
				{
					Functions.show(new CustomMessage("quests.QuestAlreadyCompleted", player), player);
					return;
				}
				if(qs.getQuest().notifyTalk(this, qs))
					return;
			}
			else
			{
				final Quest q = QuestManager.getQuest(questId);
				if(q != null)
				{
					// check for start point
					final Quest[] qlst = getTemplate().getEventQuests(QuestEventType.QUEST_START);
					if(qlst != null && qlst.length > 0)
						for(final Quest element : qlst)
							if(element == q)
							{
								qs = q.newQuestState(player);
								if(qs.getQuest().notifyTalk(this, qs))
									return;
								break;
							}
				}
			}

			final String content = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
			final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setHtml(content);
			player.sendPacket(html);
		}
		catch(final Exception e)
		{
			_log.warning("problem with npc text " + e);
			e.printStackTrace();
		}

		player.sendActionFailed();
	}
}