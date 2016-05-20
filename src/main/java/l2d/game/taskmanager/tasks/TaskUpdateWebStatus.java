package l2d.game.taskmanager.tasks;

import l2d.game.WebStatusUpdate;
import l2d.game.taskmanager.Task;
import l2d.game.taskmanager.TaskManager;
import l2d.game.taskmanager.TaskManager.ExecutedTask;
import l2d.game.taskmanager.TaskTypes;

public class TaskUpdateWebStatus extends Task
{
	private static final String NAME = "sp_update_web_status";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
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