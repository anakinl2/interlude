package l2d.game.serverpackets;

import l2d.game.model.entity.SevenSigns;

/**
 * format: d
 */
public class ShowMiniMap extends L2GameServerPacket
{
	private int _mapId, _period;
	private boolean canWriteImpl = true;

	public ShowMiniMap(int mapId)
	{
		_mapId = mapId;
		_period = SevenSigns.getInstance().getCurrentPeriod();
	}

	@Override
	final public void runImpl()
	{}

	@Override
	protected final void writeImpl()
	{
		if(!canWriteImpl)
			return;

		writeC(0x9d);
		writeD(_mapId);
		writeC(_period);
	}
}