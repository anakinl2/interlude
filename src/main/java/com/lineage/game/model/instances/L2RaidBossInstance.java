package com.lineage.game.model.instances;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.game.model.quest.QuestState;
import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.instancemanager.QuestManager;
import com.lineage.game.instancemanager.RaidBossSpawnManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.quest.Quest;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Log;
import com.lineage.util.MinionList;
import com.lineage.util.PrintfFormat;
import com.lineage.util.Rnd;
import com.lineage.util.Util;

/**
 * This class manages all RaidBoss.
 * In a group mob, there are one master called RaidBoss and several slaves called Minions.
 */
public class L2RaidBossInstance extends L2MonsterInstance
{
	protected static Logger _log = Logger.getLogger(L2RaidBossInstance.class.getName());

	private ScheduledFuture<?> minionUnspawnTask;

	private static final int RAIDBOSS_MAINTENANCE_INTERVAL = 60000;
	private static final int MINION_UNSPAWN_INTERVAL = 5000; // time to unspawn minions when boss is dead, msec

	private RaidBossSpawnManager.StatusEnum _raidStatus;

	/**
	 * Constructor of L2RaidBossInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2RaidBossInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
	 * <li>Set the name of the L2RaidBossInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 * 
	 * @param objectId
	 *            Identifier of the object to initialized

	 *            Template to apply to the NPC
	 */
	public L2RaidBossInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	protected int getMaintenanceInterval()
	{
		return RAIDBOSS_MAINTENANCE_INTERVAL;
	}

	protected int getMinionUnspawnInterval()
	{
		return MINION_UNSPAWN_INTERVAL;
	}

	protected int getKilledInterval(final L2MinionInstance minion)
	{
		return Rnd.get(240000, 360000); // 4-6 minutes to respawn
	}

	@Override
	public void notifyMinionDied(final L2MinionInstance minion)
	{
		ThreadPoolManager.getInstance().scheduleAi(new maintainKilledMinion(this, minion), getKilledInterval(minion), false);
		super.notifyMinionDied(minion);
	}

	private static class maintainKilledMinion implements Runnable
	{
		private L2RaidBossInstance _boss;
		private L2MinionInstance _minion;

		public maintainKilledMinion(final L2RaidBossInstance boss, final L2MinionInstance minion)
		{
			_boss = boss;
			_minion = minion;
		}

		@Override
		public void run()
		{
			try
			{
				if(!_boss.isDead())
				{
					final MinionList list = _boss.getMinionList();
					if(list != null)
						list.spawnSingleMinionSync(_minion.getNpcId());
				}
			}
			catch(final Throwable e)
			{
				e.printStackTrace();
			}
		}
	}

	static class DamagerInfo
	{
		double damage;
		FastList<String> skills = new FastList<String>();

		public DamagerInfo(final double _damage)
		{
			damage = _damage;
		}

		public DamagerInfo()
		{
			this(0);
		}

		@Override
		public String toString()
		{
			String result = String.valueOf((int) damage);
			if(skills.size() > 0)
			{
				result += " | Skills: " + skills.removeFirst();
				for(final String skill : skills)
					result += ", " + skill;
			}
			return result;
		}

		public String toTime()
		{
			return Util.formatTime((int) ((System.currentTimeMillis() - damage) / 1000));
		}
	}

	private final FastMap<String, DamagerInfo> lastDamagers = new FastMap<String, DamagerInfo>();

	@Override
	public void reduceCurrentHp(final double i, final L2Character attacker, final L2Skill skill, final boolean awake, final boolean standUp, final boolean directHp, final boolean canReflect)
	{
		if(attacker == null || attacker.getPlayer() == null || (attacker == this || i > getMaxHp() / 10) && !attacker.getPlayer().isGM())
			return;
		final String attackerName = attacker.getPlayer().getName();
		DamagerInfo di;
		synchronized (lastDamagers)
		{
			di = lastDamagers.get(attackerName);
			if(di == null)
			{
				di = new DamagerInfo();
				lastDamagers.put(attackerName, di);
			}
			di.damage += i;
			if(skill != null && !di.skills.contains(skill.getName()))
				di.skills.add(skill.getName());
			if(!lastDamagers.containsKey("@"))
				lastDamagers.put("@", new DamagerInfo(System.currentTimeMillis()));
		}
		super.reduceCurrentHp(i, attacker, skill, awake, standUp, directHp, canReflect);
	}

	@Override
	public void doRegen()
	{
		super.doRegen();
		if(isInCombat() || !isCurrentHpFull() || lastDamagers.size() == 0)
			return;
		lastDamagers.clear();
	}

	@Override
	public void doDie(final L2Character killer)
	{
		if(this instanceof L2ReflectionBossInstance)
		{
			super.doDie(killer);
			return;
		}

		synchronized (lastDamagers)
		{
			final String killTime = lastDamagers.containsKey("@") ? lastDamagers.remove("@").toTime() : "-";
			Log.add(PrintfFormat.LOG_BOSS_KILLED, new Object[] { getTypeName(), getName(), getNpcId(), killer, getX(), getY(), getZ(), killTime }, "bosses");
			for(final String damagerName : lastDamagers.keySet())
				Log.add("\tDamager [" + damagerName + "] = " + lastDamagers.get(damagerName), "bosses");
			lastDamagers.clear();
		}

		if(killer instanceof L2Playable)
		{
			final L2Player player = killer.getPlayer();

			if(player.getParty() != null)
				player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.CONGRATULATIONS_YOUR_RAID_WAS_SUCCESSFUL));
			else
				player.sendPacket(new SystemMessage(SystemMessage.CONGRATULATIONS_YOUR_RAID_WAS_SUCCESSFUL));

			final Quest q = QuestManager.getQuest(508);
			if(q != null)
			{
				final String qn = q.getName();
				if(player.getClan() != null && player.getClan().getLeader().isOnline() && player.getClan().getLeader().getPlayer().getQuestState(qn) != null)
				{
					final QuestState st = player.getClan().getLeader().getPlayer().getQuestState(qn);
					st.getQuest().onKill(this, st);
				}
			}
		}

		RaidBossSpawnManager.getInstance().updateStatus(this, true);

		unspawnMinions();

		int boxId = 0;
		switch(getNpcId())
		{
			case 25035: // Shilens Messenger Cabrio
				boxId = 31027;
				break;
			case 25054: // Demon Kernon
				boxId = 31028;
				break;
			case 25126: // Golkonda, the Longhorn General
				boxId = 31029;
				break;
			case 25220: // Death Lord Hallate
				boxId = 31030;
				break;
		}

		if(boxId != 0)
		{
			final L2NpcTemplate boxTemplate = NpcTable.getTemplate(boxId);
			if(boxTemplate != null)
			{
				final L2NpcInstance box = new L2NpcInstance(IdFactory.getInstance().getNextId(), boxTemplate);
				box.spawnMe(getLoc());

				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
					@Override
					public void run()
					{
						box.deleteMe();
					}
				}, 60000);
			}
		}

		super.doDie(killer);
	}

	public void unspawnMinions()
	{
		if(hasMinions())
			minionUnspawnTask = ThreadPoolManager.getInstance().scheduleAi(new Runnable(){
				@Override
				public void run()
				{
					try
					{
						removeMinions();
					}
					catch(final Throwable e)
					{
						_log.log(Level.SEVERE, "", e);
						e.printStackTrace();
					}
				}
			}, getMinionUnspawnInterval(), false);
	}

	@Override
	public void onSpawn()
	{
		addSkill(SkillTable.getInstance().getInfo(4045, 1)); // Resist Full Magic Attack
		RaidBossSpawnManager.getInstance().updateStatus(this, false);
		super.onSpawn();
		getSpawn().stopRespawn();
	}

	public void setRaidStatus(final RaidBossSpawnManager.StatusEnum status)
	{
		_raidStatus = status;
	}

	public RaidBossSpawnManager.StatusEnum getRaidStatus()
	{
		return _raidStatus;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isParalyzeImmune()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}

	@Override
	public boolean hasRandomWalk()
	{
		return false;
	}
}