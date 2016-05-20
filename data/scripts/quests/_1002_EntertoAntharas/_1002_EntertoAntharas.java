package quests._1002_EntertoAntharas;

import l2d.ext.scripts.ScriptFile;
import l2d.game.model.L2Party;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.quest.Quest;
import l2d.game.model.quest.QuestState;
import l2d.util.Location;
import bosses.AntharasManager;

public class _1002_EntertoAntharas extends Quest implements ScriptFile
{
	// NPCs
	private static final int ANTHARAS_OLD = 29019;
	private static final int ANTHARAS_WEAK = 29066;
	private static final int ANTHARAS_NORMAL = 29067;
	private static final int ANTHARAS_STRONG = 29068;

	private static final int HEART = 13001;

	// Items
	private static final int ANTHARAS_CIRCLET = 8568;

	private static final Location TELEPORT_POSITION = new Location(179892, 114915, -7704);

	public _1002_EntertoAntharas()
	{
		super(1002, "_1002_EntertoAntharas", false);

		addStartNpc(HEART);

		addKillId(ANTHARAS_OLD);
		addKillId(ANTHARAS_WEAK);
		addKillId(ANTHARAS_NORMAL);
		addKillId(ANTHARAS_STRONG);

		addAttackId(ANTHARAS_OLD);
		addAttackId(ANTHARAS_WEAK);
		addAttackId(ANTHARAS_NORMAL);
		addAttackId(ANTHARAS_STRONG);
	}

	public void onLoad()
	{
		System.out.println("Loaded Quest: 1002 - Enter to Antharas");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	@Override
	public String onTalk(L2NpcInstance npc, QuestState st)
	{
		int npcId = npc.getNpcId();
		if(npcId == HEART)
		{
			if(st.getPlayer().isFlying())
				return "<html><body>Heart of Warding:<br>You may not enter while flying a wyvern</body></html>";
			AntharasManager.setAntharasSpawnTask();
			st.getPlayer().teleToLocation(TELEPORT_POSITION);
			return null;
		}
		return null;
	}

	@Override
	public String onKill(L2NpcInstance npc, QuestState st)
	{
		// give the antharas slayer circlet to ALL PARTY MEMBERS who help kill antharas,
		L2Party party = st.getPlayer().getParty();
		if(party != null)
			for(L2Player partyMember : party.getPartyMembers())
			{
				QuestState pst = partyMember.getQuestState(_1002_EntertoAntharas.class);
				if(pst != null)
					if(pst.getQuestItemsCount(ANTHARAS_CIRCLET) < 1)
					{
						pst.giveItems(ANTHARAS_CIRCLET, 1);
						st.exitCurrentQuest(true);
					}
			}
		else if(st.getQuestItemsCount(ANTHARAS_CIRCLET) < 1)
			st.giveItems(ANTHARAS_CIRCLET, 1);
		st.exitCurrentQuest(true);
		return null;
	}

	@Override
	public String onAttack(L2NpcInstance npc, QuestState st)
	{
		int npcId = npc.getNpcId();
		if(npcId == ANTHARAS_OLD || npcId == ANTHARAS_WEAK || npcId == ANTHARAS_NORMAL || npcId == ANTHARAS_STRONG)
			AntharasManager.setLastAttackTime();
		return null;
	}
}
