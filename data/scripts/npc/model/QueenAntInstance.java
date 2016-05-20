package npc.model;

import java.util.ArrayList;

import events.Helper;
import l2d.game.model.L2Character;
import l2d.game.model.L2Spawn;
import l2d.game.model.instances.L2BossInstance;
import l2d.game.model.instances.L2MinionInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.PlaySound;
import l2d.game.tables.NpcTable;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

public class QueenAntInstance extends L2BossInstance
{
	private static final int Queen_Ant_Larva = 29002;

	private ArrayList<L2Spawn> _spawns = new ArrayList<L2Spawn>();
	private L2NpcInstance Larva = null;

	public QueenAntInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}

	public L2NpcInstance getLarva()
	{
		if(Larva == null)
			Larva = SpawnNPC(Queen_Ant_Larva, new Location( -21600, 179482, -5846, Rnd.get(0, 0xFFFF)));
		return Larva;
	}

	@Override
	protected int getKilledInterval(final L2MinionInstance minion)
	{
		return minion.getNpcId() == 29003 ? 40000 : 280000 + Rnd.get(40000);
	}

	@Override
	public void doDie(final L2Character killer)
	{
		broadcastPacketToOthers(new PlaySound(1, "BS02_D", 1, 0, getLoc()));
		Helper.deSpawnNPCs(_spawns);
		Larva = null;
		super.doDie(killer);
	}

	@Override
	public void spawnMe()
	{
		super.spawnMe();
		getLarva();
		broadcastPacketToOthers(new PlaySound(1, "BS01_A", 1, 0, getLoc()));
	}

	private L2NpcInstance SpawnNPC(final int npcId, final Location loc)
	{
		final L2NpcTemplate template = NpcTable.getTemplate(npcId);
		if(template == null)
		{
			System.out.println("WARNING! template is null for npc: " + npcId);
			Thread.dumpStack();
			return null;
		}
		try
		{
			final L2Spawn sp = new L2Spawn(template);
			sp.setLoc(loc);
			sp.setAmount(1);
			sp.setRespawnDelay(0);
			_spawns.add(sp);
			return sp.spawnOne();
		}
		catch(final Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
}