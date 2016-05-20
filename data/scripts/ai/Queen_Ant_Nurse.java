package ai;

import npc.model.QueenAntInstance;

import l2d.game.ThreadPoolManager;
import l2d.game.ai.Priest;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2MinionInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.util.Rnd;
import l2d.util.Location;

public class Queen_Ant_Nurse extends Priest
{
	public Queen_Ant_Nurse(L2Character actor)
	{
		super(actor);
		_globalAggro = 0;
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead())
			return true;

		if(_def_think)
		{
			if(doTask())
				clearTasks();
			return true;
		}

		L2Character top_desire_target = getTopDesireTarget();
		if(top_desire_target == null)
			return false;

		if(actor.getDistance(top_desire_target) - top_desire_target.getColRadius() - actor.getColRadius() > 200)
		{
			moveOrTeleportToLocation(Location.getAroundPosition(top_desire_target, actor, 100, 150, 1));
			return false;
		}

		if(!top_desire_target.isCurrentHpFull() && doTask())
			return createNewTask();

		return false;
	}

	@Override
	protected boolean createNewTask()
	{
		clearTasks();
		L2NpcInstance actor = getActor();
		L2Character top_desire_target = getTopDesireTarget();
		if(actor == null || actor.isDead() || top_desire_target == null)
			return false;

		if(!top_desire_target.isCurrentHpFull())
		{
			L2Skill skill = _heal[Rnd.get(_heal.length)];
			if(skill.getCastRange() < actor.getDistance(top_desire_target))
				moveOrTeleportToLocation(Location.getAroundPosition(top_desire_target, actor, skill.getCastRange() - 30, skill.getCastRange() - 10, 5));
			Task heal_task = new Task();
			heal_task.type = TaskType.BUFF;
			heal_task.target = top_desire_target;
			heal_task.skill = skill;
			_def_think = true;
			_task_list.add(heal_task);
			return true;
		}

		return false;
	}

	@Override
	protected int getMaxPursueRange()
	{
		return 10000;
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}

	private void moveOrTeleportToLocation(Location loc)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;
		actor.setRunning();
		if(actor.moveToLocation(loc, 0, true))
			return;
		clientStopMoving();
		_pathfind_fails = 0;
		actor.broadcastPacketToOthers(new MagicSkillUse(actor, actor, 2036, 1, 500, 600000));
		ThreadPoolManager.getInstance().scheduleAi(new Teleport(loc), 500, false);
	}

	private L2Character getTopDesireTarget()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return null;
		QueenAntInstance queen_ant = (QueenAntInstance) (((L2MinionInstance) actor).getLeader());
		if(queen_ant == null)
			return null;
		L2Character Larva = queen_ant.getLarva();
		if(Larva != null && Larva.getCurrentHpPercents() < 5)
			return Larva;
		return queen_ant;
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{}

	@Override
	protected void onEvtClanAttacked(L2Character attacked_member, L2Character attacker, int damage)
	{
		if(doTask())
			createNewTask();
	}
}