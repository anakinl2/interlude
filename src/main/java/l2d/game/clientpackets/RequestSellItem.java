package l2d.game.clientpackets;

import l2d.ext.scripts.Functions;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2CastleChamberlainInstance;
import l2d.game.model.instances.L2ClanHallManagerInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2MercManagerInstance;
import l2d.game.model.instances.L2MerchantInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.ItemList;
import l2d.game.serverpackets.SystemMessage;
import l2d.util.Files;
import l2d.util.Log;
import l2d.util.Util;

/**
 * packet type id 0x37
 * format:		cddb, b - array if (ddd)
 */
public class RequestSellItem extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _listId;
	private int _count;
	private int[] _items; // count*3

	@Override
	public void readImpl()
	{
		_listId = readD();
		_count = readD();
		if(_count * 12 > _buf.remaining() || _count > Short.MAX_VALUE || _count <= 0)
		{
			_items = null;
			return;
		}
		_items = new int[_count * 3];
		for(int i = 0; i < _count; i++)
		{
			_items[i * 3 + 0] = readD();
			_items[i * 3 + 1] = readD();
			_items[i * 3 + 2] = readD();
			if(_items[i * 3 + 1] < 0)
			{
				_items = null;
				break;
			}
		}
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		if(_items == null || _count <= 0)
			return;

		if(activeChar.getKarma() > 0 && !activeChar.isGM())
		{
			activeChar.sendActionFailed();
			return;
		}

		L2NpcInstance npc = activeChar.getLastNpc();

		if("sell".equalsIgnoreCase(activeChar.getLastBbsOperaion()))
			activeChar.setLastBbsOperaion(null);
		else
		{
			boolean isValidMerchant = npc instanceof L2ClanHallManagerInstance || npc instanceof L2MerchantInstance || npc instanceof L2MercManagerInstance || npc instanceof L2CastleChamberlainInstance;
			if(!activeChar.isGM() && (npc == null || !isValidMerchant || !activeChar.isInRange(npc.getLoc(), L2Character.INTERACTION_DISTANCE)))
			{
				activeChar.sendActionFailed();
				return;
			}
		}

		for(int i = 0; i < _count; i++)
		{
			int objectId = _items[i * 3 + 0];
			int itemId = _items[i * 3 + 1];
			int cnt = _items[i * 3 + 2];

			if(cnt < 0)
			{
				Util.handleIllegalPlayerAction(activeChar, "Integer overflow", "RequestSellItem[100]", 0);
				continue;
			}
			else if(cnt == 0)
				continue;

			L2ItemInstance item = activeChar.getInventory().getItemByObjectId(objectId);
			if(item == null || !item.canBeTraded(activeChar) || !item.getItem().isSellable())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.THE_ATTEMPT_TO_SELL_HAS_FAILED));
				return;
			}

			if(item.getItemId() != itemId)
			{
				Util.handleIllegalPlayerAction(activeChar, "Fake packet", "RequestSellItem[115]", 0);
				continue;
			}

			if(item.getIntegerLimitedCount() < cnt)
			{
				Util.handleIllegalPlayerAction(activeChar, "Incorrect item count", "RequestSellItem[121]", 0);
				continue;
			}

			int price = item.getReferencePrice() * cnt / 2;

			activeChar.addAdena(price);
			Log.LogItem(activeChar, Log.SellItem, item);

			// If player sells the enchant scroll he is using, deactivate it
			if(activeChar.getEnchantScroll() != null && item.getObjectId() == activeChar.getEnchantScroll().getObjectId())
				activeChar.setEnchantScroll(null);

			activeChar.getInventory().destroyItem(item, cnt, true);
		}

		activeChar.updateStats();
		activeChar.sendPacket(new ItemList(activeChar, true));

		if(npc != null)
			if(Files.read("data/html/merchant/" + npc.getNpcId() + "-s.htm") != null)
				Functions.show("data/html/merchant/" + npc.getNpcId() + "-s.htm", activeChar);
			else
				activeChar.doInteract(npc);
	}
}