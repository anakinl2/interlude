package l2d.auth.crypt;

import java.io.IOException;

public class ConnectionCryptDummy implements ConnectionCrypt
{
	public static final ConnectionCryptDummy instance = new ConnectionCryptDummy();

	@Override
	public byte[] decrypt(byte[] raw) throws IOException
	{
		/*
		byte[] result = new byte[raw.length];
		System.arraycopy(result, 0, raw, 0, raw.length);
		return result;
		*/
		return raw;
	}

	@Override
	public void decrypt(byte[] raw, final int offset, final int size) throws IOException
	{}

	@Override
	public byte[] crypt(byte[] raw) throws IOException
	{
		/*
		byte[] result = new byte[raw.length];
		System.arraycopy(result, 0, raw, 0, raw.length);
		return result;
		*/
		return raw;
	}

	@Override
	public void crypt(byte[] raw, final int offset, final int size) throws IOException
	{}
}