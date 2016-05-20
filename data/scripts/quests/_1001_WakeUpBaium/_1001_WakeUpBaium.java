package quests._1001_WakeUpBaium;

import bosses.BaiumManager;
import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.quest.Quest;
import l2d.game.model.quest.QuestState;
import l2d.util.Location;

public class _1001_WakeUpBaium extends Quest implements ScriptFile
{
	private static final int Baium = 29020;
	private static final int BaiumNpc = 29025;
	private static final int Angel = 29021;
	private static final int AngelicVortex = 31862;
	private static final Location TELEPORT_POSITION = new Location(113100, 14500, 10077);

	public void onLoad()
	{
		System.out.println("Loaded Quest: 1001: Wake Up Baium");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public _1001_WakeUpBaium()
	{
		super(1001, "Wake Up Baium", true);

		addStartNpc(BaiumNpc);
		addStartNpc(AngelicVortex);

		addKillId(Baium);
		addKillId(Angel);
		
		addAttackId(Baium);
	}

	@Override
	public String onTalk(L2NpcInstance npc, QuestState st)
	{
		int npcId = npc.getNpcId();
		if(npcId == AngelicVortex)
		{
			st.getPlayer().teleToLocation(TELEPORT_POSITION);
			return null;
		}
		if(npcId == BaiumNpc)
		{
			L2NpcInstance baiumBoss = L2World.findNpcByNpcId(Baium);
			if(baiumBoss != null)
				return "<html><head><body>Angelic Vortex:<br>Baium is already woken up! You can't enter!</body></html>";
			if(npc.isBusy())
				return "Baium is busy!";
			npc.setBusy(true);
			npc.setBusyMessage("Attending another player's request");
			Functions.npcShout(npc, "You call my name! Now you gonna die!");
			BaiumManager.spawnBaium(npc, st.getPlayer());
			return "You call my name! Now you gonna die!";
		}
		return null;
	}

	@Override
	public String onKill(L2NpcInstance npc, QuestState st)
	{
		int npcId = npc.getNpcId();
		if(npcId == Baium)
			st.exitCurrentQuest(true);
		return null;
	}

	@Override
	public String onAttack(L2NpcInstance npc, QuestState st)
	{
		int npcId = npc.getNpcId();
		if(npcId == Baium)
			BaiumManager.setLastAttackTime();
		return null;
	}
}