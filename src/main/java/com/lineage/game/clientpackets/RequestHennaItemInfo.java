package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2HennaInstance;
import com.lineage.game.serverpackets.HennaItemInfo;
import com.lineage.game.tables.HennaTable;
import com.lineage.game.templates.L2Henna;

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