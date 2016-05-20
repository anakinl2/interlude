package l2d.game.handler;

public class ScriptHandler
{
	private static ScriptHandler _instance;
	private IScriptHandler _handler;

	public static ScriptHandler getInstance()
	{
		if(_instance == null)
			_instance = new ScriptHandler();
		return _instance;
	}

	public void registerScriptHandler(IScriptHandler handler)
	{
		_handler = handler;
	}

	public IScriptHandler getScriptHandler()
	{
		return _handler;
	}
}
