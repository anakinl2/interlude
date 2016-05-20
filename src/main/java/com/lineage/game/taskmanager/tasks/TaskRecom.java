package com.lineage.game.taskmanager.tasks;

import java.util.logging.Logger;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.taskmanager.Task;
import com.lineage.game.taskmanager.TaskManager;
import com.lineage.game.taskmanager.TaskManager.ExecutedTask;
import com.lineage.game.taskmanager.TaskTypes;

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