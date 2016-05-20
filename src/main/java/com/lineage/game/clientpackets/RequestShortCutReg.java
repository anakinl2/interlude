package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2ShortCut;
import com.lineage.game.serverpackets.ShortCutRegister;

/**
 * [C] 33 RequestShortCutReg
 * @author Felixx
 */
public class RequestShortCutReg extends L2GameClientPacket
{
	private int _type;
	private int _id;
	private int _slot;
	private int _page;
	private int _lvl;
	private int _characterType; // 1 - player, 2 - pet

	@Override
	public void readImpl()
	{
		_type = readD();
		int slot = readD();
		_id = readD();
		readD(); // unknown

		_slot = slot % 12;
		_page = slot / 12;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(_page > 10 || _page < 0)
		{
			activeChar.sendActionFailed();
			return;
		}

		switch(_type)
		{
			case 0x01: // item
			case 0x03: // action
			case 0x04: // macro
			case 0x05: // recipe
			{
				L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, -1);
				sendPacket(new ShortCutRegister(sc));
				activeChar.registerShortCut(sc);
				break;
			}
			case 0x02: // skill
			{
				int level = activeChar.getSkillDisplayLevel(_id);
				if(level > 0)
				{
					L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, level);
					sendPacket(new ShortCutRegister(sc));
					activeChar.registerShortCut(sc);
				}
				break;
			}
		}
	}
}