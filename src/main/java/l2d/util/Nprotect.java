package l2d.util;

public class Nprotect
{
	private static Nprotect _instance;
	public static boolean _ok = false;

	public static Nprotect getInstance()
	{
		if(_instance == null)
			_instance = new Nprotect();
		return _instance;
	}

	public static void validatekey(int _nprotectkey, int _ekvipor)
	{
		if(_nprotectkey == Runtime.getRuntime().totalMemory() * Runtime.getRuntime().availableProcessors() * _ekvipor)
			_ok = true;
		else
			_ok = false;
	}
}