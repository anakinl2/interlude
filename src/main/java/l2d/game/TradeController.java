package l2d.game;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import l2d.Config;
import l2d.game.model.L2TradeList;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2Item;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class TradeController
{
	private static Logger _log = Logger.getLogger(TradeController.class.getName());
	private static TradeController _instance;

	private HashMap<Integer, L2TradeList> _lists;

	public static TradeController getInstance()
	{
		if(_instance == null)
			_instance = new TradeController();
		return _instance;
	}

	public static void reload()
	{
		_instance = new TradeController();
	}

	private TradeController()
	{
		_log.config("[ Trade Controller ]");

		_lists = new HashMap<Integer, L2TradeList>();

		try
		{
			File filelists = new File(Config.DATAPACK_ROOT + "/data/merchant_filelists.xml");
			DocumentBuilderFactory factory1 = DocumentBuilderFactory.newInstance();
			factory1.setValidating(false);
			factory1.setIgnoringComments(true);
			Document doc1 = factory1.newDocumentBuilder().parse(filelists);

			int counterFiles = 0;
			int counterItems = 0;
			for(Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
				if("list".equalsIgnoreCase(n1.getNodeName()))
					for(Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
						if("file".equalsIgnoreCase(d1.getNodeName()))
						{
							final String filename = d1.getAttributes().getNamedItem("name").getNodeValue();

							File file = new File(Config.DATAPACK_ROOT + "/data/" + filename);
							DocumentBuilderFactory factory2 = DocumentBuilderFactory.newInstance();
							factory2.setValidating(false);
							factory2.setIgnoringComments(true);
							Document doc2 = factory2.newDocumentBuilder().parse(file);
							counterFiles++;

							for(Node n2 = doc2.getFirstChild(); n2 != null; n2 = n2.getNextSibling())
								if("list".equalsIgnoreCase(n2.getNodeName()))
									for(Node d2 = n2.getFirstChild(); d2 != null; d2 = d2.getNextSibling())
										if("tradelist".equalsIgnoreCase(d2.getNodeName()))
										{
											final int shop_id = Integer.parseInt(d2.getAttributes().getNamedItem("shop").getNodeValue());
											final int npc_id = Integer.parseInt(d2.getAttributes().getNamedItem("npc").getNodeValue());
											final float markup = npc_id > 0 ? 1 + Float.parseFloat(d2.getAttributes().getNamedItem("markup").getNodeValue()) / 100f : 0f;
											L2TradeList tl = new L2TradeList(shop_id);
											tl.setNpcId(String.valueOf(npc_id));
											for(Node i = d2.getFirstChild(); i != null; i = i.getNextSibling())
												if("item".equalsIgnoreCase(i.getNodeName()))
												{
													counterItems++;
													final int itemId = Integer.parseInt(i.getAttributes().getNamedItem("id").getNodeValue());
													final L2Item template = ItemTable.getInstance().getTemplate(itemId);
													if(template == null)
														throw new NullPointerException("Template not found for itemId: " + itemId);
													final int price = i.getAttributes().getNamedItem("price") != null ? Integer.parseInt(i.getAttributes().getNamedItem("price").getNodeValue()) : Math.round(template.getReferencePrice() * markup);
													L2ItemInstance item = ItemTable.getInstance().createDummyItem(itemId);
													final int itemCount = i.getAttributes().getNamedItem("count") != null ? Integer.parseInt(i.getAttributes().getNamedItem("count").getNodeValue()) : 0;
													// Время респауна задается минутах
													final int itemRechargeTime = i.getAttributes().getNamedItem("time") != null ? Integer.parseInt(i.getAttributes().getNamedItem("time").getNodeValue()) : 0;
													if(item != null)
													{
														item.setPriceToSell(price);
														item.setCountToSell(itemCount);
														item.setMaxCountToSell(itemCount);
														item.setLastRechargeTime((int) (System.currentTimeMillis() / 60000));
														item.setRechargeTime(itemRechargeTime);
														tl.addItem(item);
													}
													else
														_log.config("TradeController: UNKNOWN ITEM '" + itemId + "' IN SHOP '" + shop_id + "' AT NPC '" + npc_id + "' IN file '" + filename + "'");
												}
											_lists.put(shop_id, tl);
										}
						}

			_log.config(" ~ Loaded: " + counterFiles + " file(s).");
			_log.config(" ~ Loaded: " + counterItems + " Items.");
			_log.config(" ~ Loaded: " + _lists.size() + " Buylists.");
			_log.config("[ Trade Controller ]\n");
		}
		catch(Exception e)
		{
			_log.warning("TradeController: Buylists could not be initialized.");
			e.printStackTrace();
		}
	}

	public L2TradeList getBuyList(int listId)
	{
		return _lists.get(listId);
	}

	public void addToBuyList(int listId, L2TradeList list)
	{
		_lists.put(listId, list);
	}
}