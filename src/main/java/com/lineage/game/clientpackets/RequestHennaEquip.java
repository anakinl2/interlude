package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.model.PcInventory;
import com.lineage.game.model.instances.L2HennaInstance;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.HennaTable;
import com.lineage.game.tables.HennaTreeTable;
import com.lineage.game.templates.L2Henna;

public class RequestHennaEquip extends L2GameClientPacket
{
	private int _symbolId;

	/**
	 * packet type id 0x6F
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		_symbolId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2Henna template = HennaTable.getInstance().getTemplate(_symbolId);
		if(template == null)
			return;

		L2HennaInstance temp = new L2HennaInstance(template);

		boolean cheater = true;
		for(L2HennaInstance h : HennaTreeTable.getInstance().getAvailableHenna(activeChar.getClassId(), activeChar.getSex()))
			if(h.getSymbolId() == temp.getSymbolId())
			{
				cheater = false;
				break;
			}

		if(cheater)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
			return;
		}

		PcInventory inventory = activeChar.getInventory();
		L2ItemInstance item = inventory.getItemByItemId(temp.getItemIdDye());
		if(item != null && item.getIntegerLimitedCount() >= temp.getAmountDyeRequire() && activeChar.getAdena() >= temp.getPrice() && activeChar.addHenna(temp))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addString(temp.getName()));
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_SYMBOL_HAS_BEEN_ADDED));
			inventory.reduceAdena(temp.getPrice());
			if(inventory.destroyItemByItemId(temp.getItemIdDye(), temp.getAmountDyeRequire(), true) == null)
				_log.info("RequestHennaEquip[50]: Item not found!!!");
		}
		else
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
	}
}