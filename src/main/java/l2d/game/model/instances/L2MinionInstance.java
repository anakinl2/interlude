package l2d.game.model.instances;

import l2d.game.model.L2Character;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.Location;
import com.lineage.util.Log;
import com.lineage.util.PrintfFormat;

public class L2MinionInstance extends L2MonsterInstance
{
	private L2MonsterInstance _master;

	public L2MinionInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	public L2MonsterInstance getLeader()
	{
		return _master;
	}

	@Override
	public void onSpawn()
	{
		getLeader().notifyMinionSpawned(this);
	}

	public void setLeader(L2MonsterInstance leader)
	{
		_master = leader;
	}

	@Override
	public void doDie(L2Character killer)
	{
		if(_master.isRaid())
			Log.add(PrintfFormat.LOG_BOSS_KILLED, new Object[] {
					getTypeName(),
					getName() + " {" + _master.getName() + "}",
					getNpcId(),
					killer,
					getX(),
					getY(),
					getZ(),
					"-" }, "bosses");
		_master.notifyMinionDied(this);
		super.doDie(killer);
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public Location getSpawnedLoc()
	{
		return _master.getMinionPosition();
	}
}