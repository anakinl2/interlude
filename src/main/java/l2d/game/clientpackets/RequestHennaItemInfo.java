package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.instances.L2HennaInstance;
import l2d.game.serverpackets.HennaItemInfo;
import l2d.game.tables.HennaTable;
import l2d.game.templates.L2Henna;

public class RequestHennaItemInfo extends L2GameClientPacket
{
	// format  cd
	private int SymbolId;

	@Override
	public void readImpl()
	{
		SymbolId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		L2Henna template = HennaTable.getInstance().getTemplate(SymbolId);
		if(template != null)
			activeChar.sendPacket(new HennaItemInfo(new L2HennaInstance(template), activeChar));
	}
}