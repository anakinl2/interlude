package l2d.game.model.instances;

import java.util.concurrent.ScheduledFuture;

import l2d.game.ThreadPoolManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2World;
import l2d.game.model.Reflection;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.SpawnTable;
import l2d.game.templates.L2NpcTemplate;

public class L2ReflectionBossInstance extends L2RaidBossInstance
{
	private ScheduledFuture _manaRegen;

	public L2ReflectionBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		_manaRegen = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ManaRegen(), 20000, 20000);
	}

	@Override
	public void doDie(L2Character killer)
	{
		_manaRegen.cancel(true);
		super.doDie(killer);
		final Reflection r = getReflection();
		if(r.getId() > 0)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
				@Override
				public void run()
				{
					for(L2Spawn s : r.getSpawns())
					{
						s.despawnAll();
						s.stopRespawn();
						SpawnTable.getInstance().deleteSpawn(s, false);
					}
				}
			}, 4000);

			r.startCollapseTimer(300000);
			broadcastPacketToOthers(new SystemMessage(SystemMessage.THIS_DUNGEON_WILL_EXPIRE_IN_S1_MINUTES).addNumber(5));
		}
	}

	@Override
	public void unspawnMinions()
	{
		removeMinions();
	}

	private class ManaRegen implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				for(L2Player p : L2World.getAroundPlayers(L2ReflectionBossInstance.this))
				{
					if(p == null || p.isDead() || p.isHealBlocked())
						continue;
					int addMp = getAddMp();
					if(addMp <= 0)
						return;
					double newMp = Math.min(Math.max(0, p.getMaxMp() - p.getCurrentMp()), addMp);
					if(newMp > 0)
						p.setCurrentMp(newMp + p.getCurrentMp());
					p.sendPacket(new SystemMessage(SystemMessage.S1_MPS_HAVE_BEEN_RESTORED).addNumber(Math.round(newMp)));
				}
			}
			catch(Throwable e)
			{
				e.printStackTrace();
			}
		}

		private int getAddMp()
		{
			switch(getLevel())
			{
				case 23:
				case 26:
					return 6;
				case 33:
				case 36:
					return 10;
				default:
					return 0;
			}
		}
	}
}