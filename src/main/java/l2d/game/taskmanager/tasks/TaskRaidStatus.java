package l2d.game.taskmanager.tasks;

import l2d.game.tables.EpicRespawnTimesHolder;
import l2d.game.taskmanager.Task;
import l2d.game.taskmanager.TaskManager;
import l2d.game.taskmanager.TaskManager.ExecutedTask;
import l2d.game.taskmanager.TaskTypes;

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