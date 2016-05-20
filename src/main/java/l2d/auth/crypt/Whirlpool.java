package l2d.auth.crypt;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import jonelo.jacksum.JacksumAPI;
import jonelo.jacksum.algorithm.AbstractChecksum;

public class Whirlpool implements Crypt
{
	protected static Logger _log = Logger.getLogger(Whirlpool.class.getName());
	private static Whirlpool _instance = new Whirlpool();

	public static Whirlpool getInstance()
	{
		return _instance;
	}

	@Override
	public boolean compare(String password, String expected)
	{
		try
		{
			return encrypt(password).equals(expected);
		}
		catch(NoSuchAlgorithmException nsee)
		{
			_log.warning("Could not check password, algorithm Whirlpool not found! Check jacksum library!");
			return false;
		}
		catch(UnsupportedEncodingException uee)
		{
			_log.warning("Could not check password, UTF-8 is not supported!");
			return false;
		}
	}

	@Override
	public String encrypt(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		AbstractChecksum sha1 = JacksumAPI.getChecksumInstance("whirlpool2");
		sha1.setEncoding("BASE64");
		sha1.update(password.getBytes("UTF-8"));
		return sha1.format("#CHECKSUM");
	}
}