package ai;

import l2d.ext.scripts.Functions;
import l2d.game.ai.CtrlEvent;
import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2MinionInstance;
import l2d.game.model.instances.L2ReflectionBossInstance;
import l2d.util.GArray;
import l2d.util.Location;
import l2d.util.MinionList;
import l2d.util.Rnd;

public class HorsesAI extends DefaultAI
{
	private Location[]	points			= new Location[4];
	private int			current_point	= -1;
	private long		wait_timeout	= 0;
	private boolean		wait			= false;
	private long		_lastOrder		= 0;				// doesn't load without this fix T_T

	public HorsesAI(L2Character actor)
	{
		super(actor);
		points[0] = new Location(82712, 148809, -3495);
		points[1] = new Location(82392, 148798, -3493);
		points[2] = new Location(82381, 148435, -3493);
		points[3] = new Location(82708, 148434, -3495);
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}

	@Override
	protected boolean thinkActive()
	{
		if(this.getActor().isDead())
			return true;

		if(_def_think)
		{
			doTask();
			return true;
		}

		if(System.currentTimeMillis() > wait_timeout && (current_point > -1 || Rnd.chance(5)))
		{
			if(!wait)
			{
				switch (current_point)
				{
					case 0:
						wait_timeout = System.currentTimeMillis() + 1000;
						wait = true;
						return true;
					case 1:
						wait_timeout = System.currentTimeMillis() + 1000;
						wait = true;
						return true;
					case 2:
						wait_timeout = System.currentTimeMillis() + 1000;
						wait = true;
						return true;
					case 3:
						wait_timeout = System.currentTimeMillis() + 1000;
						wait = true;
						return true;
				}
			}

			wait_timeout = 0;
			wait = false;

			if(current_point >= points.length - 1)
				current_point = -1;

			current_point++;

			Task task = new Task();
			task.type = TaskType.MOVE;
			task.loc = points[current_point];
			_task_list.add(task);
			_def_think = true;
			return true;
		}

		if(randomAnimation())
			return true;

		return false;
	}

	@Override
	protected void thinkAttack()
	{
		if(_lastOrder < System.currentTimeMillis() && this.getActor().isInCombat())
		{
			_lastOrder = System.currentTimeMillis() + 30000;
			MinionList ml = ((L2ReflectionBossInstance) this.getActor()).getMinionList();
			if(ml == null || !ml.hasMinions())
			{
				super.thinkAttack();
				return;
			}
			GArray<L2Player> pl = L2World.getAroundPlayers(this.getActor());
			if(pl.isEmpty())
			{
				super.thinkAttack();
				return;
			}
			L2Player target = pl.get(Rnd.get(pl.size()));
			Functions.npcShoutCustomMessage(this.getActor(), "OMFG! I kill you!", new String[] { target.getName() });
			for(L2MinionInstance m : ml.getSpawnedMinions())
			{
				m.clearAggroList(true);
				m.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, target, 10000000);
			}
		}
		super.thinkAttack();
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}
}