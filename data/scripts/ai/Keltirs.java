package ai;

import l2d.ext.scripts.Functions;
import l2d.ext.scripts.ScriptFile;
import l2d.game.ai.Fighter;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.util.Location;
import l2d.util.Rnd;

/**
 * Info: Crazy Keltirs - по желанию можно добавить и другие
 */
public class Keltirs extends Fighter implements ScriptFile
{
	// Радиус на который будут отбегать келтиры.
	private static final int range = 600;
	// Время в мс. через которое будет повторяться Rnd фраза.
	private static final int voicetime = 8000;
	private long _lastAction;
	private static final String[] _retreatText =
		{
		"Do not touch me, I'm afraid!",
		"You're terrible! Brothers, run away!",
		"Stand from under! Hunting season is open !!!",
		"If you hit me again - you will be have trouble!",
		"Poacher, I'll pass the law enforcement!",
		"Happy Feet, for 60 seconds ^-_-^",
		"You are not gonna get us ..."
		};

	private static final String[] _fightText =
		{
		"I'll kill you all!",
		"Rrrrrrrr!",
		"Do you want to bite your ass?",
		"BITE HIM ..."
		};

	public Keltirs(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected boolean createNewTask()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return false;

		if(Rnd.chance(60))
		{
			// clearTasks();
			L2Character target;
			if((target = prepareTarget()) == null)
				return false;

			// Добавить новое задание
			Task task = new Task();
			task.type = TaskType.ATTACK;
			task.target = target;
			_task_list.add(task);
			_def_think = true;

			if(System.currentTimeMillis() - _lastAction > voicetime)
			{
				Functions.npcShout(actor, _fightText[Rnd.get(_fightText.length)]);
				_lastAction = System.currentTimeMillis();
			}
			return true;
		}

		Location sloc = actor.getSpawnedLoc();
		int spawnX = sloc.x;
		int spawnY = sloc.y;
		int spawnZ = sloc.z;

		int x = spawnX + Rnd.get(2 * range) - range;
		int y = spawnY + Rnd.get(2 * range) - range;
		int z = GeoEngine.getHeight(x, y, spawnZ);

		actor.setRunning();

		actor.moveToLocation(x, y, z, 0, true);

		Task task = new Task();
		task.type = TaskType.MOVE;
		task.loc = new Location(spawnX, spawnY, spawnZ);
		_task_list.add(task);
		_def_think = true;

		if(System.currentTimeMillis() - _lastAction > voicetime)
		{
			Functions.npcShout(actor, _retreatText[Rnd.get(_retreatText.length)]);
			_lastAction = System.currentTimeMillis();
		}
		return true;
	}

	@Override
	public void onLoad()
	{
	}

	@Override
	public void onReload()
	{}

	@Override
	public void onShutdown()
	{}
}