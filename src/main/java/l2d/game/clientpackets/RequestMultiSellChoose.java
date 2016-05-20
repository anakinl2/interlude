package l2d.game.clientpackets;

import java.nio.BufferUnderflowException;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.Config;
import l2d.game.cache.Msg;
import l2d.game.model.L2Multisell;
import l2d.game.model.L2Multisell.MultiSellListContainer;
import l2d.game.model.L2Player;
import l2d.game.model.PcInventory;
import l2d.game.model.base.L2Augmentation;
import l2d.game.model.base.MultiSellEntry;
import l2d.game.model.base.MultiSellIngredient;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.ExPCCafePointInfo;
import l2d.game.serverpackets.PledgeShowInfoUpdate;
import l2d.game.serverpackets.PledgeStatusChanged;
import l2d.game.serverpackets.StatusUpdate;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.game.tables.PetDataTable;
import l2d.game.templates.L2Item;
import com.lineage.util.Log;
import com.lineage.util.Util;

public class RequestMultiSellChoose extends L2GameClientPacket
{
	// format: cdddhdddddddddd
	private static Logger _log = Logger.getLogger(RequestMultiSellChoose.class.getName());
	private int _listId;
	private int _entryId;
	private int _amount;
	private int _enchant = 0;
	private boolean _keepenchant = false;
	private boolean _notax = false;
	private MultiSellListContainer _list = null;
	private FastList<ItemData> _items = new FastList<ItemData>();

	private class ItemData
	{
		private final int _id;
		private final int _count;
		private final L2ItemInstance _item;

		public ItemData(final int id, final int count, final L2ItemInstance item)
		{
			_id = id;
			_count = count;
			_item = item;
		}

		public int getId()
		{
			return _id;
		}

		public int getCount()
		{
			return _count;
		}

		public L2ItemInstance getItem()
		{
			return _item;
		}

		@Override
		public boolean equals(final Object obj)
		{
			if(!(obj instanceof ItemData))
				return false;

			final ItemData i = (ItemData) obj;

			return _id == i._id && _count == i._count && _item == i._item;
		}
	}

	@Override
	public void readImpl()
	{
		try
		{
			_listId = readD();
			_entryId = readD();
			_amount = readD();
		}
		catch(final BufferUnderflowException e)
		{
			_log.warning(getClient().getLoginName() + " maybe packet cheater!");
		}
	}

	@Override
	public void runImpl()
	{
		final L2Player activeChar = getClient().getActiveChar();

		if(activeChar.getKarma() > 0 && !activeChar.isGM())
		{
			activeChar.sendActionFailed();
			return;
		}

		_list = activeChar.getMultisell();

		// На всякий случай...
		if(_list == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		// Проверяем, не подменили ли id
		if(activeChar.getMultisell().getListId() != _listId)
		{
			Util.handleIllegalPlayerAction(activeChar, "RequestMultiSellChoose[110]", "Tried to buy from multisell: " + _listId, 1);
			return;
		}

		if(_amount < 1 || _amount > Integer.MAX_VALUE)
		{
			activeChar.sendActionFailed();
			return;
		}

		_keepenchant = _list.getKeepEnchant();
		_notax = _list.getNoTax();

		for(FastList.Node<MultiSellEntry> n = _list.getEntries().head(), end = _list.getEntries().tail(); (n = n.getNext()) != end;)
		{
			final MultiSellEntry entry = n.getValue();
			if(entry.getEntryId() == _entryId)
			{
				doExchange(activeChar, entry);
				break;
			}
		}
	}

	private void doExchange(final L2Player activeChar, final MultiSellEntry entry)
	{
		final PcInventory inv = activeChar.getInventory();

		int totalAdenaCost = 0;
		double tax = 0.;
		double taxRate = 0.;
		final L2NpcInstance merchant = activeChar.getLastNpc();

		if(merchant != null && merchant.getCastle() != null)
			taxRate = merchant.getCastle().getTaxRate();
		if(_notax)
			taxRate = 0.;

		final FastList<MultiSellIngredient> productId = entry.getProduction();
		if(_keepenchant)
			for(final MultiSellIngredient p : productId)
				_enchant = Math.max(_enchant, p.getItemEnchant());
		final boolean logExchange = Config.LOG_MULTISELL_ID_LIST.contains(_listId);
		StringBuffer msgb = new StringBuffer();

		if(logExchange)
			msgb.append("<multisell id=").append(_listId).append(" player=\"").append(activeChar.getName()).append("\" oid=").append(activeChar.getObjectId()).append(">\n");

		synchronized (inv)
		{
			final int slots = inv.slotsLeft();
			if(slots == 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.THE_WEIGHT_AND_VOLUME_LIMIT_OF_INVENTORY_MUST_NOT_BE_EXCEEDED));
				return;
			}

			int req = 0;
			long totalLoad = 0;
			for(final MultiSellIngredient i : productId)
				if(i.getItemId() != -100 && i.getItemId() != -200 && i.getItemId() != -300)
				{
					totalLoad += ItemTable.getInstance().getTemplate(i.getItemId()).getWeight() * _amount;
					if(!ItemTable.getInstance().getTemplate(i.getItemId()).isStackable())
						req += _amount;
					else
						req++;
				}
			if(req > slots || !inv.validateWeight(totalLoad))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.THE_WEIGHT_AND_VOLUME_LIMIT_OF_INVENTORY_MUST_NOT_BE_EXCEEDED));
				return;
			}

			boolean can = false;
			for(L2NpcInstance npc : activeChar.getAroundNpc(200, 200))
			{
				if(npc.getNpcId() == merchant.getNpcId())
					can = true;
			}

			if(!can)
			{
				activeChar.sendMessage("To far from npc.");
				return;
			}

			if(entry.getIngredients().size() == 0)
			{
				System.out.println("WARNING Ingredients list = 0 multisell id=:" + _listId + " player:" + activeChar.getName());
				activeChar.sendActionFailed();
				return;
			}

			L2Augmentation augmentation = null;

			// Перебор всех ингридиентов, проверка наличия и создание списка забираемого
			for(FastList.Node<MultiSellIngredient> sn = entry.getIngredients().head(), send = entry.getIngredients().tail(); (sn = sn.getNext()) != send;)
			{
				final MultiSellIngredient ingridient = sn.getValue();
				final int ingridientItemId = ingridient.getItemId();
				final long ingridientItemCount = ingridient.getItemCount();
				final double total = ingridientItemCount * _amount;

				if(total <= 0 || total > Integer.MAX_VALUE) // Проверка на переполнение
				{
					activeChar.sendActionFailed();
					return;
				}

				if(ingridientItemId > 0 && !ItemTable.getInstance().getTemplate(ingridientItemId).isStackable())
					for(int i = 0; i < ingridientItemCount * _amount; i++)
					{
						final L2ItemInstance[] list = inv.getAllItemsById(ingridientItemId);
						// Если энчант имеет значение - то ищем вещи с точно таким энчантом
						if(_keepenchant)
						{
							L2ItemInstance itemToTake = null;
							for(final L2ItemInstance itm : list)
								if((itm.getEnchantLevel() == _enchant || itm.getItem().getType2() > 2) && !_items.contains(new ItemData(itm.getItemId(), itm.getIntegerLimitedCount(), itm)) && !itm.isShadowItem() && !itm.isTemporalItem() && (itm.getCustomFlags() & L2ItemInstance.FLAG_NO_TRADE) != L2ItemInstance.FLAG_NO_TRADE)
								{
									itemToTake = itm;
									break;
								}

							if(itemToTake == null)
							{
								activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
								return;
							}

							if(!checkItem(itemToTake, activeChar))
							{
								activeChar.sendActionFailed();
								return;
							}

							if(itemToTake.getAugmentation() != null)
							{
								itemToTake.setWhFlag(true);
								augmentation = itemToTake.getAugmentation();
							}
							_items.add(new ItemData(itemToTake.getItemId(), 1, itemToTake));
						}
						// Если энчант не обрабатывается берется вещь с наименьшим энчантом
						else
						{
							L2ItemInstance itemToTake = null;
							for(final L2ItemInstance itm : list)
								if(!_items.contains(new ItemData(itm.getItemId(), itm.getIntegerLimitedCount(), itm)) && (itemToTake == null || itm.getEnchantLevel() < itemToTake.getEnchantLevel()) && !itm.isShadowItem() && !itm.isTemporalItem() && (itm.getCustomFlags() & L2ItemInstance.FLAG_NO_TRADE) != L2ItemInstance.FLAG_NO_TRADE && checkItem(itm, activeChar))
								{
									itemToTake = itm;
									if(itemToTake.getEnchantLevel() == 0)
										break;
								}

							if(itemToTake == null)
							{
								activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
								return;
							}
							if(itemToTake.getAugmentation() != null)
							{
								itemToTake.setWhFlag(true);
								augmentation = itemToTake.getAugmentation();
							}
							_items.add(new ItemData(itemToTake.getItemId(), 1, itemToTake));
						}
					}
				else if(ingridientItemId == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE)
				{
					if(activeChar.getClan() == null)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_A_CLAN_MEMBER));
						return;
					}

					if(activeChar.getClan().getReputationScore() < total)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW));
						return;
					}

					if(activeChar.getClan().getLeaderId() != activeChar.getObjectId())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_NOT_A_CLAN_LEADER).addString(activeChar.getName()));
						return;
					}
					_items.add(new ItemData(ingridientItemId, (int) total, null));
				}
				else if(ingridientItemId == L2Item.ITEM_ID_PC_BANG_POINTS)
				{
					if(activeChar.getPcBangPoints() < total)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_SHORT_OF_ACCUMULATED_POINTS));
						return;
					}
					_items.add(new ItemData(ingridientItemId, (int) total, null));
				}
				else
				{
					if(ingridientItemId == 57)
					{
						long priceProduct = 0;
						for(MultiSellIngredient mi : entry.getProduction())
						{
							L2Item i = ItemTable.getInstance().getTemplate(mi.getItemId());
							priceProduct = i.getReferencePrice() * _amount;
						}
						totalAdenaCost += ingridientItemCount * _amount;
						tax += total;
						// Если кол-во аден потраченных на покупку меньше
						// чем стоимость всех купленных предметов-то мы имеем кривую мультесельку
						// и баг на деньги.
						if(totalAdenaCost < priceProduct && entry.getIngredients().size() == 1)
						{
							if(Config.DEBUG_MULTISELL)
								_log.warning("bugged price in multsel id: " + _listId + " product id: " + entry.getEntryId());
							if(Config.FORCE_MULTISELL_SELL_PRICE)
								priceProduct = totalAdenaCost;// Нсильное присвоение стандртной цены.
						}
					}
					final L2ItemInstance item = inv.getItemByItemId(ingridientItemId);

					if(item == null || item.getIntegerLimitedCount() < total)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
						return;
					}

					_items.add(new ItemData(item.getItemId(), (int) total, item));
				}

				if(activeChar.getAdena() < totalAdenaCost)
				{
					activeChar.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
					return;
				}
			}

			for(final ItemData id : _items)
			{
				final int count = id.getCount();
				if(count > 0)
				{
					final L2ItemInstance item = id.getItem();

					if(item != null)
					{
						activeChar.sendPacket(item.isStackable() ? new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addNumber(id.getCount()).addItemName(item.getItemId()) : new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(item.getItemId()));
						if(logExchange)
							msgb.append("\t<destroy id=").append(item.getItemId()).append(" oid=").append(item.getObjectId()).append(" count=").append(id.getCount()).append(">\n");

						if(item.isEquipped())
							inv.unEquipItemInSlot(item.getEquipSlot());
						inv.destroyItem(item, count, true);
					}
					else if(id.getId() == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE)
					{
						activeChar.getClan().incReputation(-count, false, "MultiSell");
						activeChar.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(activeChar.getClan()));
						activeChar.getClan().broadcastToOnlineMembers(new PledgeStatusChanged(activeChar.getClan()));
						activeChar.sendPacket(new SystemMessage(SystemMessage.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_REPUTATION_SCORE).addNumber(count));
					}
					else if(id.getId() == L2Item.ITEM_ID_PC_BANG_POINTS)
					{
						activeChar.setPcBangPoints(activeChar.getPcBangPoints() - count);
						activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_USING_S1_POINT).addNumber(count));
						activeChar.sendPacket(new ExPCCafePointInfo(activeChar));
					}
				}
			}

			tax = totalAdenaCost - Math.round(tax / (taxRate + 1));
			if(taxRate > 0 && tax > 0)
			{
				activeChar.sendMessage("Tax: " + (int) tax);
				if(merchant != null && merchant.getReflection().getId() == 0)
				{
					merchant.getCastle().addToTreasury((int) tax, true, false);
					Log.add(merchant.getCastle().getName() + "|" + (int) tax + "|Multisell", "treasury");
				}
			}

			for(final MultiSellIngredient in : productId)
			{
				int total = (int) (in.getItemCount() * _amount);
				if(in.getItemId() != -300)
				{
					if(ItemTable.getInstance().getTemplate(in.getItemId()).isStackable())
					{
						final L2ItemInstance product = ItemTable.getInstance().createItem(in.getItemId());

						if(total < 0 || total > Integer.MAX_VALUE)
						{
							activeChar.sendActionFailed();
							return;
						}

						product.setCount(total);
						activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S).addItemName(product.getItemId()).addNumber(product.getIntegerLimitedCount()));
						if(logExchange)
							msgb.append("\t<add id=").append(product.getItemId()).append(" count=").append(product.getIntegerLimitedCount()).append(">\n");
						inv.addItem(product);
					}
					else
						for(int i = 0; i < _amount; i++)
						{
							final L2ItemInstance product = inv.addItem(ItemTable.getInstance().createItem((short) in.getItemId()));
							product.setCount(1);
							if(_keepenchant)
								product.setEnchantLevel(_enchant);
							if(augmentation != null && product.isEquipable())
								product.setAugmentation(augmentation);
							if(logExchange)
								msgb.append("\t<add id=").append(product.getItemId()).append(" oid=").append(product.getObjectId()).append(" count=").append(product.getIntegerLimitedCount()).append(">\n");
							activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addItemName(product.getItemId()));
						}
				}
				else if(total < 0 || total > Long.MAX_VALUE)
				{
					activeChar.sendActionFailed();
					return;
				}
			}
		}

		activeChar.sendPacket(new StatusUpdate(activeChar.getObjectId()).addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad()));
		if(logExchange)
		{
			msgb.append("</multisell>\n");
			Log.add(msgb.toString(), "multisell", "");
			msgb = null;
		}

		if(_list == null || !_list.getShowAll()) // Если показывается только то, на что хватает материалов обновить окно у игрока
			L2Multisell.getInstance().SeparateAndSend(_listId, activeChar, taxRate);
	}

	private boolean checkItem(final L2ItemInstance temp, final L2Player activeChar)
	{
		if(temp == null)
			return false;

		if(temp.isHeroItem())
			return false;

		if(temp.isShadowItem())
			return false;

		if(temp.isTemporalItem())
			return false;

		if(PetDataTable.isPetControlItem(temp) && activeChar.isMounted())
			return false;

		if(activeChar.getPet() != null && temp.getObjectId() == activeChar.getPet().getControlItemObjId())
			return false;

		if(temp.isEquipped())
			return false;

		if(temp.isWear())
			return false;

		if(activeChar.getEnchantScroll() == temp)
			return false;

		return true;
	}
}