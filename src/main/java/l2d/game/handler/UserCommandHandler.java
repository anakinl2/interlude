package l2d.game.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import l2d.Config;

public class UserCommandHandler
{
	private static Logger _log = Logger.getLogger(UserCommandHandler.class.getName());

	private static UserCommandHandler _instance;

	private Map<Integer, IUserCommandHandler> _datatable;

	public static UserCommandHandler getInstance()
	{
		if(_instance == null)
			_instance = new UserCommandHandler();
		return _instance;
	}

	private UserCommandHandler()
	{
		_datatable = new HashMap<Integer, IUserCommandHandler>();
	}

	public void registerUserCommandHandler(IUserCommandHandler handler)
	{
		int[] ids = handler.getUserCommandList();
		for(int element : ids)
		{
			if(Config.DEBUG)
				_log.fine("Adding handler for user command " + element);
			_datatable.put(element, handler);
		}
	}

	public IUserCommandHandler getUserCommandHandler(int userCommand)
	{
		if(Config.DEBUG)
			_log.fine("getting handler for user command: " + userCommand);
		return _datatable.get(userCommand);
	}

	/**
	 * @return
	 */
	public int size()
	{
		return _datatable.size();
	}

	public void clear()
	{
		_datatable.clear();
	}
}
