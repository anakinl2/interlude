package l2d.game.ai;

import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Location;

public class Ranger extends DefaultAI
{
	public Ranger(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		return super.thinkActive() || defaultThinkBuff(10);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		super.onEvtAttacked(attacker, damage);
		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead() || attacker == null || actor.getDistance(attacker) > 200)
			return;

		for(Task task : _task_list)
			if(task != null && task.type == TaskType.MOVE)
				return;

		int posX = actor.getX();
		int posY = actor.getY();
		int posZ = actor.getZ();

		int old_posX = posX;
		int old_posY = posY;
		int old_posZ = posZ;

		int signx = posX < attacker.getX() ? -1 : 1;
		int signy = posY < attacker.getY() ? -1 : 1;

		// int range = (int) ((actor.calculateAttackSpeed()  /1000 * actor.getWalkSpeed() )* 0.71); // was "actor.getPhysicalAttackRange()"    0.71 = sqrt(2) / 2

		int range = (int) (0.71 * actor.calculateAttackDelay() / 1000 * actor.getMoveSpeed());

		posX += signx * range;
		posY += signy * range;
		posZ = GeoEngine.getHeight(posX, posY, posZ);

		if(GeoEngine.canMoveToCoord(old_posX, old_posY, old_posZ, posX, posY, posZ))
		{
			Task task = new Task();
			task.type = TaskType.MOVE;
			task.loc = new Location(posX, posY, posZ);
			_task_list.add(task);

			task = new Task();
			task.type = TaskType.ATTACK;
			task.target = attacker;
			_task_list.add(task);

			_def_think = true;
		}
	}

	@Override
	protected boolean createNewTask()
	{
		return defaultFightTask();
	}

	@Override
	public int getRatePHYS()
	{
		return 25;
	}

	@Override
	public int getRateDOT()
	{
		return 50;
	}

	@Override
	public int getRateDEBUFF()
	{
		return 25;
	}

	@Override
	public int getRateDAM()
	{
		return 50;
	}

	@Override
	public int getRateSTUN()
	{
		return 65;
	}

	@Override
	public int getRateHEAL()
	{
		return 50;
	}
}