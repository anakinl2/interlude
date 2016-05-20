package com.lineage.util;

public final class GsaTr
{
	public static String Buyler = "Developers"; // = "Developers";
	public static String Type = "Developer Version"; // = "Full version";
	public static int TrialOnline = 1000; // = Config.MAXIMUM_ONLINE_USERS_NOTTRIAL;

	@SuppressWarnings("unused")
	private static String getBuy()
	{
		return Buyler;
	}

	@SuppressWarnings("unused")
	private static String getVersionType()
	{
		return Type;
	}

	@SuppressWarnings("unused")
	private static int getTrialOnline()
	{
		return TrialOnline;
	}
}