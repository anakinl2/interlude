package com.lineage.game.taskmanager.tasks;

import com.lineage.game.WebStatusUpdate;
import com.lineage.game.taskmanager.Task;
import com.lineage.game.taskmanager.TaskManager;
import com.lineage.game.taskmanager.TaskTypes;

public class TaskUpdateWebStatus extends Task
{
	private static final String NAME = "sp_update_web_status";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(TaskManager.ExecutedTask task)
	{
		WebStatusUpdate.getInstance().updateOnline(false);
	}

	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "0", "10000", "");
	}
}