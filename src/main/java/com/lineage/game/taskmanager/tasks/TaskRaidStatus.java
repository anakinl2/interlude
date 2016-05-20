package com.lineage.game.taskmanager.tasks;

import com.lineage.game.tables.EpicRespawnTimesHolder;
import com.lineage.game.taskmanager.Task;
import com.lineage.game.taskmanager.TaskManager;
import com.lineage.game.taskmanager.TaskManager.ExecutedTask;
import com.lineage.game.taskmanager.TaskTypes;

/**
 * 
 * @author Midnex
 *
 */
public class TaskRaidStatus extends Task
{
	private static final String NAME = "sp_raidstatus";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		EpicRespawnTimesHolder.getInstance().update();
	}

	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "0", "300000", "");
	}
}