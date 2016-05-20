package com.lineage.util;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import l2d.game.idfactory.IdFactory;
import l2d.game.model.L2MinionData;
import l2d.game.model.instances.L2MinionInstance;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.tables.NpcTable;

public class MinionList
{
	/** List containing the current spawned minions for this L2MonsterInstance */
	private final ConcurrentLinkedQueue<L2MinionInstance> minionReferences;
	private final L2MonsterInstance master;

	public MinionList(L2MonsterInstance master)
	{
		minionReferences = new ConcurrentLinkedQueue<L2MinionInstance>();
		this.master = master;
	}

	public int countSpawnedMinions()
	{
		return minionReferences.size();
	}

	public boolean hasMinions()
	{
		return minionReferences.size() > 0;
	}

	public ConcurrentLinkedQueue<L2MinionInstance> getSpawnedMinions()
	{
		return minionReferences;
	}

	public void addSpawnedMinion(L2MinionInstance minion)
	{
		synchronized (minionReferences)
		{
			minionReferences.add(minion);
		}
	}

	public void removeSpawnedMinion(L2MinionInstance minion)
	{
		synchronized (minionReferences)
		{
			minionReferences.remove(minion);
		}
	}

	/**
	 *  Спавнит всех недостающих миньонов
	 */
	public void maintainMinions()
	{
		List<L2MinionData> minions = master.getTemplate().getMinionData();

		synchronized (minionReferences)
		{
			byte minionCount;
			short minionId;
			for(L2MinionData minion : minions)
			{
				minionCount = minion.getAmount();
				minionId = minion.getMinionId();

				for(L2MinionInstance m : minionReferences)
					if(m.getNpcId() == minionId)
						minionCount--;

				for(int i = 0; i < minionCount; i++)
					spawnSingleMinion(minionId);
			}
		}
	}

	/**
	 *	Удаляет тех миньонов, которые еще живы
	 */
	public void maintainLonelyMinions()
	{
		synchronized (minionReferences)
		{
			for(L2MinionInstance minion : getSpawnedMinions())
				if(!minion.isDead())
				{
					removeSpawnedMinion(minion);
					minion.deleteMe();
				}
		}
	}

	private void spawnSingleMinion(int minionid)
	{
		L2MinionInstance monster = new L2MinionInstance(IdFactory.getInstance().getNextId(), NpcTable.getTemplate(minionid));
		monster.setReflection(master.getReflection());
		monster.setCurrentHpMp(monster.getMaxHp(), monster.getMaxMp(), true);
		monster.setHeading(master.getHeading());
		monster.setLeader(master);
		addSpawnedMinion(monster);
		Location pos = master.getMinionPosition();
		monster.spawnMe(pos);
		monster.setSpawnedLoc(pos);
	}

	/**
	 * Same as spawnSingleMinion, but synchronized.<BR><BR>
	 * @param minionid The I2NpcTemplate Identifier of the Minion to spawn
	 */
	public void spawnSingleMinionSync(int minionid)
	{
		synchronized (minionReferences)
		{
			spawnSingleMinion(minionid);
		}
	}
}