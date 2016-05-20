package com.lineage.game.serverpackets;

import com.lineage.util.Location;

/**
 * [S] d3 Earthquake
 * 
 * @author Felixx
 *         format dddddd
 */
public class Earthquake extends L2GameServerPacket
{
	private Location _loc;
	private int _intensity;
	private int _duration;

	public Earthquake(final Location loc, final int intensity, final int duration)
	{
		_loc = loc;
		_intensity = intensity;
		_duration = duration;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xc4);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
		writeD(_intensity);
		writeD(_duration);
		writeD(0x00); // Unknown
	}
}