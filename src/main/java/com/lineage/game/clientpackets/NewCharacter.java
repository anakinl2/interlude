package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.game.model.base.ClassId;
import com.lineage.game.serverpackets.NewCharacterSuccess;
import com.lineage.game.tables.CharTemplateTable;

public class NewCharacter extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(NewCharacter.class.getName());

	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		if(Config.DEBUG)
			_log.fine("CreateNewChar");

		NewCharacterSuccess ct = new NewCharacterSuccess();

		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.fighter, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.mage, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.elvenFighter, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.elvenMage, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.darkFighter, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.darkMage, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.orcFighter, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.orcMage, false));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.dwarvenFighter, false));

		sendPacket(ct);
	}
}