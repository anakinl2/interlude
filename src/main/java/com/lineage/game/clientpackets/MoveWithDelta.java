package com.lineage.game.clientpackets;

/**
 * [C] 0x41 MoveWithDelta
 * <b>Format:</b> (c) ddd
 * d: dx
 * d: dy
 * d: dz
 * @author Felixx
 */
public class MoveWithDelta extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _dx, _dy, _dz;

	@Override
	protected void readImpl()
	{
		_dx = readD();
		_dy = readD();
		_dz = readD();
	}

	@Override
	protected void runImpl()
	{
		// TODO this
	}
}