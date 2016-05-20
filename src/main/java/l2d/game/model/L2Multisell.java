package l2d.game.model;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2d.Config;
import l2d.game.model.base.MultiSellEntry;
import l2d.game.model.base.MultiSellIngredient;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.MultiSellList;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2Item;
import l2d.game.templates.L2Item.Grade;
import l2d.game.templates.L2Weapon;
import l2d.game.templates.L2Weapon.WeaponType;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Multisell list manager
 */
public class L2Multisell
{
	private static Logger _log = Logger.getLogger(L2Multisell.class.getName());
	private FastMap<Integer, MultiSellListContainer> entries = new FastMap<Integer, MultiSellListContainer>();
	private static L2Multisell _instance = new L2Multisell();

	public MultiSellListContainer getList(int id)
	{
		return entries.get(id);
	}

	public L2Multisell()
	{
		parseData();
	}

	public void reload()
	{
		parseData();
	}

	public static L2Multisell getInstance()
	{
		return _instance;
	}

	private void parseData()
	{
		entries.clear();
		parse();
	}

	public static class MultiSellListContainer
	{
		private int _listId;
		private boolean _showall = true;
		private boolean keep_enchanted = false;
		private boolean is_dutyfree = false;
		public boolean nokey = false;
		FastList<MultiSellEntry> entries = new FastList<MultiSellEntry>();

		public void setListId(int listId)
		{
			_listId = listId;
		}

		public int getListId()
		{
			return _listId;
		}

		public void setShowAll(boolean bool)
		{
			_showall = bool;
		}

		public boolean getShowAll()
		{
			return _showall;
		}

		public void setNoTax(boolean bool)
		{
			is_dutyfree = bool;
		}

		public boolean getNoTax()
		{
			return is_dutyfree;
		}

		public void setKeepEnchant(boolean bool)
		{
			keep_enchanted = bool;
		}

		public boolean getKeepEnchant()
		{
			return keep_enchanted;
		}

		public void addEntry(MultiSellEntry e)
		{
			entries.add(e);
		}

		public FastList<MultiSellEntry> getEntries()
		{
			return entries;
		}
	}

	private void hashFiles(String dirname, List<File> hash)
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/" + dirname);
		if(!dir.exists())
		{
			_log.config("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		File[] files = dir.listFiles();
		for(File f : files)
			if(f.getName().endsWith(".xml"))
				hash.add(f);
			else if(f.isDirectory() && !f.getName().equals(".svn"))
				hashFiles(dirname + "/" + f.getName(), hash);
	}

	public void addMultiSellListContainer(int id, MultiSellListContainer list)
	{
		if(entries.containsKey(id))
		{
			_log.warning("MultiSell redefined: " + id);
			Thread.dumpStack();
		}

		list.setListId(id);
		entries.put(id, list);
	}

	public MultiSellListContainer remove(String s)
	{
		return remove(new File(s));
	}

	public MultiSellListContainer remove(File f)
	{
		return remove(Integer.parseInt(f.getName().replaceAll(".xml", "")));
	}

	public MultiSellListContainer remove(int id)
	{
		return entries.remove(id);
	}

	public void parseFile(File f)
	{
		int id = 0;
		try
		{
			id = Integer.parseInt(f.getName().replaceAll(".xml", ""));
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Error loading file " + f, e);
			return;
		}
		Document doc = null;
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(f);
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Error loading file " + f, e);
			return;
		}
		try
		{
			addMultiSellListContainer(id, parseDocument(doc));
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Error in file " + f, e);
		}
	}

	private void parse()
	{
		List<File> files = new FastList<File>();
		hashFiles("multisell", files);
		for(File f : files)
			parseFile(f);
		files.clear();
	}

	protected MultiSellListContainer parseDocument(Document doc)
	{
		MultiSellListContainer list = new MultiSellListContainer();
		int entId = 1;

		for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			if("list".equalsIgnoreCase(n.getNodeName()))
				for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					if("item".equalsIgnoreCase(d.getNodeName()))
					{
						MultiSellEntry e = parseEntry(d);
						e.setEntryId(entId++);
						list.addEntry(e);
					}
					else if("config".equalsIgnoreCase(d.getNodeName()))
					{
						list.setShowAll(Boolean.parseBoolean(getSubNode(d, "showall")));
						list.setNoTax(Boolean.parseBoolean(getSubNode(d, "notax")));
						list.setKeepEnchant(Boolean.parseBoolean(getSubNode(d, "keepenchanted")));
						list.nokey = Boolean.parseBoolean(getSubNode(d, "nokey"));
					}

		return list;
	}

	private String getSubNode(Node n, String item)
	{
		try
		{
			return n.getAttributes().getNamedItem(item).getNodeValue();
		}
		catch(NullPointerException e)
		{
			return null;
		}
	}

	protected MultiSellEntry parseEntry(Node n)
	{
		Node first = n.getFirstChild();
		MultiSellEntry entry = new MultiSellEntry();

		for(n = first; n != null; n = n.getNextSibling())
			if("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());

				entry.addIngredient(new MultiSellIngredient(id, count));
			}
			else if("production".equalsIgnoreCase(n.getNodeName()))
			{
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());

				entry.addProduct(new MultiSellIngredient(id, count));
			}

		return entry;
	}

	private static int[] parseItemIdAndCount(String s)
	{
		if(s == null || s.isEmpty())
			return null;
		String[] a = s.split(":");
		try
		{
			int id = Integer.parseInt(a[0]);
			int count = a.length > 1 ? Integer.parseInt(a[1]) : 1;
			return new int[] { id, count };
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static MultiSellEntry parseEntryFromStr(String s)
	{
		if(s == null || s.isEmpty())
			return null;

		String[] a = s.split("->");
		if(a.length != 2)
			return null;

		int[] ingredient = parseItemIdAndCount(a[0]);
		int[] production = parseItemIdAndCount(a[1]);
		if(ingredient == null || production == null)
			return null;

		MultiSellEntry entry = new MultiSellEntry();
		entry.addIngredient(new MultiSellIngredient(ingredient[0], ingredient[1]));
		entry.addProduct(new MultiSellIngredient(production[0], production[1]));
		return entry;
	}

	public void SeparateAndSend(int listId, L2Player player, double taxRate)
	{
		MultiSellListContainer list = generateMultiSell(listId, player, taxRate);
		MultiSellListContainer temp = new MultiSellListContainer();
		int page = 1;

		temp.setListId(list.getListId());

		// Запоминаем отсылаемый лист, чтобы не подменили
		player.setMultisell(list);

		for(MultiSellEntry e : list.getEntries())
		{
			if(temp.getEntries().size() == Config.MULTISELL_SIZE)
			{
				player.sendPacket(new MultiSellList(temp, page, 0));
				page++;
				temp = new MultiSellListContainer();
				temp.setListId(list.getListId());
			}
			temp.addEntry(e);
		}

		player.sendPacket(new MultiSellList(temp, page, 1));
	}

	private MultiSellListContainer generateMultiSell(int listId, L2Player player, double taxRate)
	{
		MultiSellListContainer list = new MultiSellListContainer();
		list._listId = listId;

		// Hardcoded  - обмен вещей на равноценные
		HashSet<L2ItemInstance> _items;
		if(listId == 9999)
		{
			list.setShowAll(false);
			list.setKeepEnchant(true);
			list.setNoTax(true);
			final Inventory inv = player.getInventory();
			_items = new HashSet<L2ItemInstance>();
			for(final L2ItemInstance itm : inv.getItems())
				if(!itm.isStackable() && itm.getItem().getType2() == 0 && itm.getItem().getCrystalType() != Grade.NONE && itm.getReferencePrice() <= Config.ALT_MAMMON_EXCHANGE && itm.getItem().getCrystalCount() > 0 && !itm.isShadowItem() && !itm.isTemporalItem() && !itm.isEquipped() && (itm.getCustomFlags() & L2ItemInstance.FLAG_NO_TRADE) != L2ItemInstance.FLAG_NO_TRADE)
					_items.add(itm);

			for(final L2ItemInstance itm : _items)
				for(L2Weapon i : ItemTable.getInstance().getAllWeapons())
					if(!i.isSa() && i.getItemId() != itm.getItemId() && i.getItemType() == WeaponType.DUAL == (itm.getItem().getItemType() == WeaponType.DUAL) && itm.getItem().getCrystalType() == i.getCrystalType() && itm.getItem().getCrystalCount() == i.getCrystalCount() && !i.isShadowItem())
					{
						final int entry = new int[] { itm.getItemId(), i.getItemId(), itm.getEnchantLevel() }.hashCode();
						MultiSellEntry possibleEntry = new MultiSellEntry(entry, i.getItemId(), 1, itm.getEnchantLevel());
						possibleEntry.addIngredient(new MultiSellIngredient(itm.getItemId(), 1, itm.getEnchantLevel()));
						list.entries.add(possibleEntry);
					}
		}

		// Hardcoded  - обмен вещей с доплатой за AA
		else if(listId == 9998)
		{
			list.setShowAll(false);
			list.setKeepEnchant(false);
			list.setNoTax(true);
			final Inventory inv = player.getInventory();
			_items = new HashSet<L2ItemInstance>();
			for(final L2ItemInstance itm : inv.getItems())
				if(!itm.isStackable() && itm.getItem().getType2() == L2Item.TYPE2_WEAPON && itm.getItem().getCrystalType() != Grade.NONE && itm.getReferencePrice() <= Config.ALT_MAMMON_UPGRADE && itm.getItem().getCrystalCount() > 0 && !itm.isShadowItem() && !itm.isTemporalItem() && !itm.isEquipped() && (itm.getCustomFlags() & L2ItemInstance.FLAG_NO_TRADE) != L2ItemInstance.FLAG_NO_TRADE)
					_items.add(itm);

			for(final L2ItemInstance itemtosell : _items)
				for(final L2Weapon itemtobuy : ItemTable.getInstance().getAllWeapons())
					if(!itemtobuy.isSa() && itemtobuy.getType2() == L2Item.TYPE2_WEAPON && itemtobuy.getItemType() == WeaponType.DUAL == (itemtosell.getItem().getItemType() == WeaponType.DUAL) && itemtobuy.getCrystalType().ordinal() >= itemtosell.getItem().getCrystalType().ordinal() && itemtobuy.getReferencePrice() <= Config.ALT_MAMMON_UPGRADE && itemtosell.getItem().getReferencePrice() < itemtobuy.getReferencePrice() && itemtosell.getReferencePrice() * 1.7 > itemtobuy.getReferencePrice() && !itemtobuy.isShadowItem())
					{
						final int entry = new int[] { itemtosell.getItemId(), itemtobuy.getItemId(), itemtosell.getEnchantLevel() }.hashCode();
						MultiSellEntry possibleEntry = new MultiSellEntry(entry, itemtobuy.getItemId(), 1, 0);
						possibleEntry.addIngredient(new MultiSellIngredient(itemtosell.getItemId(), 1, itemtosell.getEnchantLevel()));
						possibleEntry.addIngredient(new MultiSellIngredient((short) 5575, (int) ((itemtobuy.getReferencePrice() - itemtosell.getReferencePrice()) * 1.2), 0));
						list.entries.add(possibleEntry);
					}
		}

		// Hardcoded  - обмен вещей на кристаллы
		else if(listId == 9997)
		{
			list.setShowAll(false);
			list.setKeepEnchant(true);
			list.setNoTax(false);
			final Inventory inv = player.getInventory();
			for(final L2ItemInstance itm : inv.getItems())
				if(!itm.isStackable() && itm.getItem().isCrystallizable() && itm.getItem().getCrystalType() != Grade.NONE && itm.getItem().getCrystalCount() > 0 && !itm.isShadowItem() && !itm.isTemporalItem() && !itm.isEquipped() && (itm.getCustomFlags() & L2ItemInstance.FLAG_NO_CRYSTALLIZE) != L2ItemInstance.FLAG_NO_CRYSTALLIZE)
				{
					final L2Item crystal = ItemTable.getInstance().getTemplate(itm.getItem().getCrystalType().cry);
					final int entry = new int[] { itm.getItemId(), itm.getEnchantLevel() }.hashCode();
					MultiSellEntry possibleEntry = new MultiSellEntry(entry, crystal.getItemId(), itm.getItem().getCrystalCount(), 0);
					possibleEntry.addIngredient(new MultiSellIngredient(itm.getItemId(), 1, itm.getEnchantLevel()));
					possibleEntry.addIngredient(new MultiSellIngredient((short) 57, (int) (itm.getItem().getCrystalCount() * crystal.getReferencePrice() * 0.05), 0));
					list.entries.add(possibleEntry);
				}
		}

		// Все мультиселлы из датапака
		else
		{
			MultiSellListContainer container = L2Multisell.getInstance().getList(listId);
			if(container == null)
				return null;
			FastList<MultiSellEntry> _fulllist = container.getEntries();
			boolean enchant = container.getKeepEnchant();
			final Inventory inv = player.getInventory();

			list.setShowAll(container.getShowAll());
			list.setKeepEnchant(enchant);
			list.setNoTax(container.getNoTax());
			list.nokey = container.nokey;

			for(FastList.Node<MultiSellEntry> n = _fulllist.head(), end = _fulllist.tail(); (n = n.getNext()) != end;)
			{
				MultiSellEntry ent = n.getValue();

				// Обработка налога, если лист не безналоговый
				// Адены добавляются в лист если отсутствуют или прибавляются к существующим
				FastList<MultiSellIngredient> ingridients;
				if(!container.getNoTax() && taxRate > 0.)
				{
					double tax = 0;
					ingridients = new FastList<MultiSellIngredient>();
					for(FastList.Node<MultiSellIngredient> sn = ent.getIngredients().head(), send = ent.getIngredients().tail(); (sn = sn.getNext()) != send;)
					{
						MultiSellIngredient i = sn.getValue();

						if(i.getItemId() == 57)
						{
							tax += i.getItemCount() * (taxRate + 1);
							continue;
						}
						ingridients.add(i);
						if(i.getItemId() == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE)
							// hardcoded. Налог на клановую репутацию. Формула проверена на с6 и соответсвует на 100%.
							//TODO: Проверить на корейском(?) оффе налог на банг поинты и fame
							tax += i.getItemCount() / 120 * 1000 * taxRate * 100;
						if(i.getItemId() < 1)
							continue;

						final L2Item item = ItemTable.getInstance().getTemplate(i.getItemId());
						if(item == null)
							System.out.println("Not found template for itemId: " + i.getItemId());
						else if(item.isStackable())
							tax += item.getReferencePrice() * i.getItemCount() * taxRate;
					}

					tax = Math.round(tax);
					if(tax >= 1)
						ingridients.add(new MultiSellIngredient(57, (long) tax));
				}
				else
					ingridients = ent.getIngredients();

				// Если стоит флаг "показывать все" не проверять наличие ингридиентов
				if(container.getShowAll())
				{
					MultiSellEntry possibleEntry = new MultiSellEntry(ent.getEntryId());
					for(MultiSellIngredient p : ent.getProduction())
						possibleEntry.addProduct(p);
					for(FastList.Node<MultiSellIngredient> sn = ingridients.head(), send = ingridients.tail(); (sn = sn.getNext()) != send;)
						possibleEntry.addIngredient(sn.getValue());
					list.entries.add(possibleEntry);
				}
				else
				{
					HashSet<Integer> _itm = new HashSet<Integer>();
					// Проверка наличия у игрока ингридиентов
					for(FastList.Node<MultiSellIngredient> sn = ingridients.head(), send = ingridients.tail(); (sn = sn.getNext()) != send;)
					{
						MultiSellIngredient i = sn.getValue();
						L2Item template = i.getItemId() <= 0 ? null : ItemTable.getInstance().getTemplate(i.getItemId());
						if(i.getItemId() <= 0 || template.getType2() <= L2Item.TYPE2_ACCESSORY || template.getType2() >= (container.nokey ? L2Item.TYPE2_OTHER : L2Item.TYPE2_PET_WOLF)) // Экипировка
						{
							//if(i.getItemId() == 12374) // Mammon's Varnish Enhancer
							//	continue;

							//TODO: а мы должны тут сверять count?
							if(i.getItemId() == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE)
							{
								if(!_itm.contains(i.getItemId()) && player.getClan() != null && player.getClan().getReputationScore() >= i.getItemCount())
									_itm.add(i.getItemId());
								continue;
							}
							else if(i.getItemId() == L2Item.ITEM_ID_PC_BANG_POINTS)
							{
								if(!_itm.contains(i.getItemId()) && player.getPcBangPoints() >= i.getItemCount())
									_itm.add(i.getItemId());
								continue;
							}

							for(final L2ItemInstance item : inv.getItems())
								if(item.getItemId() == i.getItemId() && !item.isEquipped() && (item.getCustomFlags() & L2ItemInstance.FLAG_NO_TRADE) != L2ItemInstance.FLAG_NO_TRADE)
								{
									if(_itm.contains(enchant ? i.getItemId() + i.getItemEnchant() * 100000 : i.getItemId())) // Не проверять одинаковые вещи
										continue;

									if(item.isStackable() && item.getCount() < i.getItemCount())
										break;

									_itm.add(enchant ? i.getItemId() + i.getItemEnchant() * 100000 : i.getItemId());
									MultiSellEntry possibleEntry = new MultiSellEntry(enchant ? ent.getEntryId() + item.getEnchantLevel() * 100000 : ent.getEntryId());

									for(MultiSellIngredient p : ent.getProduction())
									{
										p.setItemEnchant(item.getEnchantLevel());
										possibleEntry.addProduct(p);
									}

									for(FastList.Node<MultiSellIngredient> ssn = ingridients.head(), ssend = ingridients.tail(); (ssn = ssn.getNext()) != ssend;)
									{
										MultiSellIngredient ig = ssn.getValue();

										if(template != null && template.getType2() <= L2Item.TYPE2_ACCESSORY)
											ig.setItemEnchant(item.getEnchantLevel());

										possibleEntry.addIngredient(ig);
									}
									list.entries.add(possibleEntry);
									break;
								}
						}
					}
				}
			}
		}

		return list;
	}

	public static void unload()
	{
		if(_instance != null)
		{
			_instance.entries.clear();
			_instance = null;
		}
	}
}