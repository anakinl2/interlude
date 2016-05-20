package l2d.auth;

/**
 * This class ...
 * 
 * @version $Revision: 1.2.4.2 $ $Date: 2005/03/27 15:30:09 $
 */

public class HackingException extends Exception
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 4050762693478463029L;
	String _ip;
	private int _connects;

	public HackingException(String ip, int connects)
	{
		_ip = ip;
		_connects = connects;
	}

	/**
	 * @return
	 */
	public String getIP()
	{
		return _ip;
	}

	public int getConnects()
	{
		return _connects;
	}

}
