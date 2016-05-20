package quests._1003_EntertoValakas;

import com.lineage.ext.scripts.ScriptFile;
import l2d.game.model.L2Party;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.quest.Quest;
import l2d.game.model.quest.QuestState;
import com.lineage.util.Location;
import bosses.ValakasManager;

public class _1003_EntertoValakas extends Quest implements ScriptFile
{
	// NPCs
	private static final int VALAKAS = 29028;
	private static final int HEART = 31385;
	private static final int KLEIN = 31540;
	// Items
	private static final int VALAKAS_CIRCLET = 8567;
	private static final Location TELEPORT_POSITION1 = new Location(204101, -111325, 34);
	private static final Location TELEPORT_POSITION2 = new Location(183831, -115457, -3296);

	public void onLoad()
	{
		System.out.println("Loaded Quest: 1003 - Enter to Valakas");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public _1003_EntertoValakas()
	{
		super(1003, "Enter to Valakas", false);

		addStartNpc(KLEIN);
		addStartNpc(HEART);
		addKillId(VALAKAS);
		addAttackId(VALAKAS);
	}

	@Override
	public String onTalk(L2NpcInstance npc, QuestState st)
	{
		int npcId = npc.getNpcId();
		if(npcId == HEART)
		{
			if(st.getPlayer().isFlying())
				return "<html><body>Heart of Volcano:<br>You may not enter while flying a wyvern</body></html>";
			ValakasManager.setValakasSpawnTask();
			st.getPlayer().teleToLocation(TELEPORT_POSITION1);
			return null;
		}
		if(npcId == KLEIN)
		{
			if(st.getPlayer().isFlying())
				return "<html><body>Klein:<br>You may not enter while flying a wyvern</body></html>";
			st.getPlayer().teleToLocation(TELEPORT_POSITION2);
			return null;
		}
		return null;
	}

	@Override
	public String onKill(L2NpcInstance npc, QuestState st)
	{
		// Даем Valakas slayer circlet всем пати мемберам кто помогал убить Валакаса
		L2Party party = st.getPlayer().getParty();
		if(party != null)
			for(L2Player partyMember : party.getPartyMembers())
			{
				QuestState pst = partyMember.getQuestState(_1003_EntertoValakas.class);
				if(pst != null)
					if(pst.getQuestItemsCount(VALAKAS_CIRCLET) < 1)
					{
						pst.giveItems(VALAKAS_CIRCLET, 1);
						st.exitCurrentQuest(true);
					}
			}
		else if(st.getQuestItemsCount(VALAKAS_CIRCLET) < 1)
			st.giveItems(VALAKAS_CIRCLET, 1);
		st.exitCurrentQuest(true);
		return null;
	}

	@Override
	public String onAttack(L2NpcInstance npc, QuestState st)
	{
		int npcId = npc.getNpcId();
		if(npcId == VALAKAS)
			ValakasManager.setLastAttackTime();
		return null;
	}
}