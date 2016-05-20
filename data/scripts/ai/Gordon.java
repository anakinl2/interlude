package ai;

import static com.lineage.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import com.lineage.game.ai.Fighter;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

public class Gordon extends Fighter
{
	static final Location[] points = {
		// spawn: 147316,-64797,-3469
		new Location(146268, -64651, -3412),
		new Location(143678, -64045, -3434),
		new Location(141620, -62316, -3210),
		new Location(139466, -60839, -2994),
		new Location(138429, -57679, -3548),
		new Location(139402, -55879, -3334),
		new Location(139660, -52780, -2908),
		new Location(139516, -50343, -2591),
		new Location(140059, -48657, -2271),
		new Location(140319, -46063, -2408),
		new Location(142462, -45540, -2432),
		new Location(144290, -43543, -2380),
		new Location(146494, -43234, -2325),
		new Location(148416, -43186, -2329),
		new Location(151135, -44084, -2746),
		new Location(153040, -42240, -2920),
		new Location(154871, -39193, -3294),
		new Location(156725, -41827, -3569),
		new Location(157788, -45071, -3598),
		new Location(159433, -45943, -3547),
		new Location(160327, -47404, -3681),
		new Location(159106, -48215, -3691),
		new Location(159541, -50908, -3563),
		new Location(159576, -53782, -3226),
		new Location(160918, -56899, -2790),
		new Location(160785, -59505, -2662),
		new Location(158252, -60098, -2680),
		new Location(155962, -59751, -2656),
		new Location(154649, -60214, -2701),
		new Location(153121, -63319, -2969),
		new Location(151511, -64366, -3174),
		new Location(149161, -64576, -3316)
	};

	private int current_point = -1;
	private long wait_timeout = 0;
	private boolean wait = false;

	public Gordon(L2Character actor)
	{
		super(actor);
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}

	@Override
	public void checkAggression(L2Character target)
	{
		if(getIntention() != AI_INTENTION_ACTIVE)
			return;
		// Агрится только на носителей проклятого оружия
		if(!target.isCursedWeaponEquipped())
			return;
		super.checkAggression(target);
		// Продолжит идти с предыдущей точки
		if(getIntention() != AI_INTENTION_ACTIVE && current_point > -1)
			current_point--;
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead())
			return true;

		// Update every 1s the _globalAggro counter to come close to 0
		if(_globalAggro < 0)
			_globalAggro++;
		else if(_globalAggro > 0)
			_globalAggro--;

		if(_def_think)
		{
			doTask();
			return true;
		}

		// BUFF
		if(super.thinkActive())
			return true;

		if(System.currentTimeMillis() > wait_timeout && (current_point > -1 || Rnd.chance(5)))
		{
			if(!wait)
				switch(current_point)
				{
					case 31:
						wait_timeout = System.currentTimeMillis() + 60000;
						wait = true;
						return true;
				}

			wait_timeout = 0;
			wait = false;
			current_point++;

			if(current_point >= points.length)
				current_point = 0;

			actor.setWalking();

			// Добавить новое задание
			Task task = new Task();
			task.type = TaskType.MOVE;
			task.loc = points[current_point];
			_task_list.add(task);
			_def_think = true;
			return true;
		}

		if(randomAnimation())
			return false;

		return false;
	}
}