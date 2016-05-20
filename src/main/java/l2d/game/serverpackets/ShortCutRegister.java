package l2d.game.serverpackets;

import l2d.game.model.L2ShortCut;

public class ShortCutRegister extends L2GameServerPacket
{
	private L2ShortCut sc;

	public ShortCutRegister(L2ShortCut _sc)
	{
		sc = _sc;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x44);

		writeD(sc.type);
		writeD(sc.slot + sc.page * 12); // C4 Client
		if(sc.type == L2ShortCut.TYPE_SKILL) // Skill
		{
			writeD(sc.id);
			writeD(sc.level);
			writeC(0x00); // C5
		}
		else
			writeD(sc.id);

		writeD(1);//??
	}
}