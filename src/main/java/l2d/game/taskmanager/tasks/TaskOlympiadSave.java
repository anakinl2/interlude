package l2d.game.taskmanager.tasks;

import java.util.logging.Logger;

import com.lineage.Config;
import l2d.game.model.entity.olympiad.OlympiadDatabase;
import l2d.game.taskmanager.Task;
import l2d.game.taskmanager.TaskManager;
import l2d.game.taskmanager.TaskManager.ExecutedTask;
import l2d.game.taskmanager.TaskTypes;

/**
 * Updates all data of Olympiad nobles in db
 *
 * @author godson
 */
public class TaskOlympiadSave extends Task
{
	private static final Logger _log = Logger.getLogger(TaskOlympiadSave.class.getName());
	public static final String NAME = "OlympiadSave";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		if(!Config.ENABLE_OLYMPIAD)
			return;
		try
		{
			OlympiadDatabase.save();
			_log.info("Olympiad System: Data updated successfully.");
		}
		catch(Exception e)
		{
			_log.warning("Olympiad System: Failed to save Olympiad configuration: " + e);
		}
	}

	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "0", "600000", "");
	}
}
