package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.PcInventory;
import l2d.game.model.instances.L2HennaInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.HennaTable;
import l2d.game.tables.HennaTreeTable;
import l2d.game.templates.L2Henna;

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