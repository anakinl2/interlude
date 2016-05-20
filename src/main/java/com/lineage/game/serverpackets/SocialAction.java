package com.lineage.game.serverpackets;

public class SocialAction extends L2GameServerPacket
{
	private int _playerId;
	private int _actionId;

	public SocialAction(int playerId, int actionId)
	{
		_playerId = playerId;
		_actionId = actionId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x2d);
		writeD(_playerId);
		writeD(_actionId);
	}
}