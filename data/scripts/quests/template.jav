package quests.qqqqq;

import l2ace.extensions.scripts.ScriptFile;
import l2ace.gameserver.model.instances.L2NpcInstance;
import l2ace.gameserver.model.quest.Quest;
import l2ace.gameserver.model.quest.QuestState;
import l2ace.gameserver.model.quest.State;

public class qqqqq extends Quest implements ScriptFile
{
	public void onLoad()
	{
		System.out.println("Loaded Quest: 000: fullname");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public qqqqq()
	{
		super(000, "fullname", false);
	}

	@Override
	public String onEvent(String event, QuestState st)
	{
		String htmltext = event;
		return htmltext;
	}

	@Override
	public String onTalk(L2NpcInstance npc, QuestState st)
	{
		String htmltext = "noquest";
		int npcId = npc.getNpcId();
		State id = st.getState();
		int cond = st.getInt("cond");
		return htmltext;
	}

	@Override
	public String onKill(L2NpcInstance npc, QuestState st)
	{
		return null;
	}
}