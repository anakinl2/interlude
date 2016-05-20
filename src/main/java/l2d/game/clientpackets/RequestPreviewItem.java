package l2d.game.clientpackets;

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import l2d.Config;
import l2d.game.ThreadPoolManager;
import l2d.game.TradeController;
import l2d.game.cache.Msg;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2TradeList;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2CastleChamberlainInstance;
import l2d.game.model.instances.L2ClanHallManagerInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2MercManagerInstance;
import l2d.game.model.instances.L2MerchantInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.ItemList;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2Item;

public class RequestPreviewItem extends L2GameClientPacket
{
	// format: cdddb
	protected static Logger _log = Logger.getLogger(RequestPreviewItem.class.getName());

	protected Future<?> _removeWearItemsTask;

	@SuppressWarnings("unused")
	private int _unknow;

	/** List of ItemID to Wear */
	private int _listId;

	/** Number of Item to Wear */
	private int _count;

	/** Table of ItemId containing all Item to Wear */
	private int[] _items;
	protected L2Player _cha;

	class RemoveWearItemsTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				L2ItemInstance[] items = _cha.getInventory().getItems();
				for(L2ItemInstance i : items)
					if(i.isWear())
					{
						if(i.isEquipped())
						{
							_cha.getInventory().unEquipItemInSlot(i.getEquipSlot());
							_cha.sendPacket(new SystemMessage(SystemMessage.S1__HAS_BEEN_DISARMED).addItemName(i.getItemId()));
						}
						L2World.removeObject(_cha.getInventory().destroyItem(i.getObjectId(), 1, true));
					}
				_cha.broadcastUserInfo(true);
				_cha.sendPacket(new SystemMessage(SystemMessage.TRYING_ON_MODE_HAS_ENDED));
				sendPacket(new ItemList(_cha, false));
			}
			catch(Throwable e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}

	@Override
	public void readImpl()
	{
		_cha = getClient().getActiveChar();
		_unknow = readD();
		_listId = readD();
		_count = readD();
		if(_count < 0)
			_count = 0;
		if(_count > 100)
			_count = 0;

		_items = new int[_count];

		for(int i = 0; i < _count; i++)
		{
			int itemId = readD();
			_items[i] = itemId;
		}
	}

	@Override
	protected void runImpl()
	{

		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null || !Config.WEAR_ENABLED)
			return;

		if(activeChar.getKarma() > 0)
			return;

		L2NpcInstance npc = activeChar.getLastNpc();

		boolean isValidMerchant = npc instanceof L2ClanHallManagerInstance || npc instanceof L2MerchantInstance || npc instanceof L2MercManagerInstance || npc instanceof L2CastleChamberlainInstance;

		if(!activeChar.isGM() && (npc == null || !isValidMerchant || !activeChar.isInRange(npc.getLoc(), L2Character.INTERACTION_DISTANCE)))
		{
			activeChar.sendActionFailed();
			return;
		}

		L2TradeList list = TradeController.getInstance().getBuyList(_listId);

		if(list == null || _count < 1 || _listId >= 1000000)
			return;

		int totalPrice = 0, slots = 0, weight = 0;

		for(int i = 0; i < _count; i++)
		{
			int itemId = _items[i];

			if(list.getItemByItemId(itemId) == null)
				return;

			L2Item template = ItemTable.getInstance().getTemplate(itemId);
			weight += template.getWeight();
			slots++;
			totalPrice += 10;
		}

		if(!activeChar.getInventory().validateWeight(weight))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			activeChar.sendActionFailed();
			return;
		}

		if(!activeChar.getInventory().validateCapacity(slots))
		{
			sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			activeChar.sendActionFailed();
			return;
		}

		if(totalPrice < 0 || activeChar.getAdena() < totalPrice)
		{
			sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			activeChar.sendActionFailed();
			return;
		}

		for(int i = 0; i < _count; i++)
		{
			int itemId = _items[i];
			if(list.getItemByItemId(itemId) == null)
				return;

			L2ItemInstance item = activeChar.getInventory().addWearItem(itemId, activeChar);
			activeChar.getInventory().equipItem(item);
		}

		activeChar.broadcastUserInfo(true);
		if(_removeWearItemsTask == null)
			_removeWearItemsTask = ThreadPoolManager.getInstance().scheduleAi(new RemoveWearItemsTask(), 5000, true);
	}
}