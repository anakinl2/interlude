package events;

import java.util.AbstractList;

import com.lineage.game.instancemanager.ServerVariables;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.templates.L2NpcTemplate;

public class Helper
{
	public static void SpawnNPCs(final int npcId, final int[][] locations, final AbstractList<L2Spawn> list)
	{
		final L2NpcTemplate template = NpcTable.getTemplate(npcId);
		if(template == null)
		{
			System.out.println("WARNING! events.Helper.SpawnNPCs template is null for npc: " + npcId);
			Thread.dumpStack();
			return;
		}
		for(final int[] location : locations)
			try
			{
				final L2Spawn sp = new L2Spawn(template);
				sp.setLocx(location[0]);
				sp.setLocy(location[1]);
				sp.setLocz(location[2]);
				sp.setHeading(location[3]);
				sp.setAmount(1);
				sp.setRespawnDelay(0);
				sp.init();
				if(list != null)
					list.add(sp);
			}
			catch(final ClassNotFoundException e)
			{
				e.printStackTrace();
			}
	}

	public static void deSpawnNPCs(final AbstractList<L2Spawn> list)
	{
		for(final L2Spawn sp : list)
		{
			sp.stopRespawn();
			sp.getLastSpawn().deleteMe();
		}
		list.clear();
	}

	public static boolean IsActive(final String name)
	{
		return ServerVariables.getString(name, "off").equalsIgnoreCase("on");
	}

	public static boolean SetActive(final String name, final boolean active)
	{
		if(active == IsActive(name))
			return false;
		if(active)
			ServerVariables.set(name, "on");
		else
			ServerVariables.unset(name);
		return true;
	}

	public static boolean SimpleCheckDrop(final L2Character mob, final L2Character killer)
	{
		return mob != null && mob.isMonster() && !mob.isRaid() && killer != null && killer.getPlayer() != null && killer.getLevel() - mob.getLevel() < 10;
	}
}