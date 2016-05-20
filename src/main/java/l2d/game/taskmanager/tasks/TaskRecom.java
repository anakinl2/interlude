package l2d.game.taskmanager.tasks;

import java.util.logging.Logger;

import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.taskmanager.Task;
import l2d.game.taskmanager.TaskManager;
import l2d.game.taskmanager.TaskManager.ExecutedTask;
import l2d.game.taskmanager.TaskTypes;

public class TaskRecom extends Task
{
	private static final Logger _log = Logger.getLogger(TaskRecom.class.getName());
	private static final String NAME = "sp_recommendations";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		_log.config("Recommendation Global Task: launched.");
		for(L2Player player : L2World.getAllPlayers())
			player.restartRecom();
		//player.sendUserInfo(false);
		_log.config("Recommendation Global Task: completed.");
	}

	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "13:00:00", "");
	}
}