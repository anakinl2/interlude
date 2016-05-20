package com.lineage.ext.scripts;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.clientpackets.Say2C;
import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.L2World;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.serverpackets.NpcSay;
import com.lineage.game.serverpackets.Say2;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.tables.NpcTable;
import com.lineage.util.Location;
import com.lineage.util.Strings;

/**
 * @Author: Diamond
 * @Date: 7/6/2007
 * @Time: 5:22:23
 */
public class Functions
{
	public static L2Object self;
	public static L2NpcInstance npc;

	/**
	 * Вызывает метод с задержкой
	 * 
	 * @param object
	 *            - от чьего имени вызывать
	 * @param sClass
	 *            - вызываемый класс
	 * @param sMethod
	 *            - вызываемый метод
	 * @param args
	 *            - массив аргуметов
	 * @param variables
	 *            - список выставляемых переменных
	 * @param delay
	 *            - задержка в миллисекундах
	 */
	public static ScheduledFuture executeTask(final L2Object object, final String sClass, final String sMethod, final Object[] args, final HashMap<String, Object> variables, Integer delay)
	{
		return ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
			@Override
			public void run()
			{
				if(object != null)
					Scripts.callScripts(sClass, sMethod,object, args, variables);
			}
		}, delay);
	}

	public static ScheduledFuture executeTask(final String sClass, final String sMethod, final Object[] args, final HashMap<String, Object> variables, Integer delay)
	{
		return ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
			@Override
			public void run()
			{
				callScripts(sClass, sMethod, args, variables);
			}
		}, delay);
	}

	public static ScheduledFuture executeTask(final L2Object object, final String sClass, final String sMethod, final Object[] args, Integer delay)
	{
		return executeTask(object, sClass, sMethod, args, null, delay);
	}

	public static ScheduledFuture executeTask(final String sClass, final String sMethod, final Object[] args, Integer delay)
	{
		return executeTask(sClass, sMethod, args, null, delay);
	}

	public static Object callScripts(String _class, String method, Object[] args, HashMap<String, Object> variables)
	{
		if(Scripts.loading)
			return null;

		ScriptObject o;

		Script scriptClass = Scripts.getInstance().getClasses().get(_class);

		if(scriptClass == null)
			return null;

		try
		{
			o = scriptClass.newInstance();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}

		if(variables != null)
			for(Map.Entry<String, Object> obj : variables.entrySet())
				try
				{
					o.setProperty(obj.getKey(), obj.getValue());
				}
				catch(Exception e)
				{}

		return o.invokeMethod(method, args);
	}

	public static void show(String text, L2Player self)
	{
		if(text == null || self == null)
			return;
		NpcHtmlMessage msg = new NpcHtmlMessage(self.getLastNpc() != null ? self.getLastNpc().getObjectId() : 5);

		// Не указываем явно язык
		if(text.endsWith(".html-ru") || text.endsWith(".htm-ru"))
			text = text.substring(0, text.length() - 3);

		// приводим нашу html-ку в нужный вид
		if(text.endsWith(".html") || text.endsWith(".htm"))
			msg.setFile(text);
		else
			msg.setHtml(Strings.bbParse(text));
		self.sendPacket(msg);
	}

	public static void show(CustomMessage message, L2Player self)
	{
		show(message.toString(), self);
	}

	public static void sendMessage(String text, L2Player self)
	{
		self.sendMessage(text);
	}

	public static void sendMessage(CustomMessage message, L2Player self)
	{
		self.sendMessage(message);
	}

	public static void sayInRange(L2NpcInstance npc, String text, int range)
	{
		if(npc == null)
			return;
		Say2 cs = new Say2(npc.getObjectId(), 0, npc.getName(), text);
		for(L2Player player : L2World.getAroundPlayers(npc, range, 200))
			if(player != null)
				player.sendPacket(cs);
	}

	public static void npcSayInRange(L2NpcInstance npc, String text, int range)
	{
		NpcSay cs;
		if(npc == null)
			return;
		cs = new NpcSay(npc, 0, text);
		for(L2Player player : L2World.getAroundPlayers(npc, range, 200))
			if(player != null)
				player.sendPacket(cs);
	}

	public static void npcShoutInRange(String name, String text, int range)
	{
		Say2 cs;
		cs = new Say2(0, Say2C.SHOUT, name, text);
		for(L2Player player : L2World.getAroundPlayers(npc, range, 200))
			if(player != null)
				player.sendPacket(cs);
	}
	

	public static void npcShoutInRange(L2Object obj, String name, String text, int range)
	{
		Say2 cs;
		cs = new Say2(0, Say2C.SHOUT, name, text);
		for(L2Player player : L2World.getAroundPlayers(obj, range, 200))
			if(player != null)
				player.sendPacket(cs);
		obj.getPlayer().sendPacket(cs);
	}

	public static void npcSay(L2NpcInstance npc, String text)
	{
		npcSayInRange(npc, text, 1500);
	}

	public static void npcSayToAll(L2NpcInstance npc, String text)
	{
		NpcSay cs;
		if(npc == null)
			return;
		cs = new NpcSay(npc, 1, text);
		if(Config.GLOBAL_CHAT < Config.ALT_MAX_LEVEL + 1)
		{
			for(L2Player player : L2World.getAllPlayers())
				if(player != null)
					player.sendPacket(cs);
		}
		else
			for(L2Player player : L2World.getAroundPlayers(npc))
				if(player != null)
					player.sendPacket(cs);
	}

	public static void npcSayToPlayer(L2NpcInstance npc, L2Player player, String text)
	{
		NpcSay cs;
		if(npc == null)
			return;
		cs = new NpcSay(npc, 0, text);
		player.sendPacket(cs);
	}

	public static void npcShout(L2NpcInstance npc, String text)
	{
		NpcSay cs;
		if(npc == null)
			return;
		cs = new NpcSay(npc, 0, text);
		for(L2Player player : L2World.getAroundPlayers(npc))
			if(player != null)
				player.sendPacket(cs);
	}

	public static void npcShoutCustomMessage(L2NpcInstance npc, String address, String[] replacements)
	{
		if(npc == null)
			return;

		for(L2Player player : L2World.getAroundPlayers(npc))
			if(player != null && player.isConnected())
			{
				CustomMessage cm = new CustomMessage(address, player);

				if(replacements != null)
					for(String s : replacements)
						cm.addString(s);

				player.sendPacket(new NpcSay(npc, 0, cm.toString()));
			}
	}

	/**
	 * Добавляет предмет в инвентарь чара
	 * 
	 * @param playable
	 *            Владелец инвентаря
	 * @param item_id
	 *            ID предмета
	 * @param count
	 *            количество
	 */
	public static void addItem(L2Playable playable, int item_id, int count)
	{
		if(playable == null || count < 1)
			return;

		L2Playable player;
		if(playable.isSummon())
			player = playable.getPlayer();
		else
			player = playable;

		L2ItemInstance item = ItemTable.getInstance().createItem(item_id);
		if(!item.isStackable())
			for(int i = 0; i < count; i++)
				player.getInventory().addItem(ItemTable.getInstance().createItem(item_id));
		else
		{
			item.setCount(count);
			player.getInventory().addItem(item);
		}

		if(item_id == 57)
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1_ADENA).addNumber(count));
		else if(count > 1)
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S).addItemName(item_id).addNumber(count));
		else
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addItemName(item_id));
	}

	/**
	 * Возвращает количество предметов в инвентаре чара.
	 * 
	 * @param playable
	 *            Владелец инвентаря
	 * @param item_id
	 *            ID предмета
	 * @return количество
	 */
	public static int getItemCount(L2Playable playable, int item_id)
	{
		int count = 0;
		L2Playable player;
		if(playable != null && playable.isSummon())
			player = playable.getPlayer();
		else
			player = playable;
		Inventory inv = player.getInventory();
		if(inv == null)
			return 0;
		L2ItemInstance[] items = inv.getItems();
		for(L2ItemInstance item : items)
			if(item.getItemId() == item_id)
				count += item.getIntegerLimitedCount();
		return count;
	}

	/**
	 * Удаляет предметы из инвентаря чара.
	 * 
	 * @param playable
	 *            Владелец инвентаря
	 * @param item_id
	 *            ID предмета
	 * @param count
	 *            количество
	 */
	public static void removeItem(L2Playable playable, int item_id, int count)
	{
		if(playable == null || count < 1)
			return;

		L2Playable player;
		if(playable.isSummon())
			player = playable.getPlayer();
		else
			player = playable;
		Inventory inv = player.getInventory();
		if(inv == null)
			return;
		int removed = count;
		L2ItemInstance[] items = inv.getItems();
		for(L2ItemInstance item : items)
			if(item.getItemId() == item_id && count > 0)
			{
				int item_count = item.getIntegerLimitedCount();
				int rem = count <= item_count ? count : item_count;
				player.getInventory().destroyItemByItemId(item_id, rem, true);
				count -= rem;
			}
		removed -= count;
		if(item_id == 57)
			player.sendPacket(new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED).addNumber(removed));
		else if(removed > 1)
			player.sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(item_id).addNumber(removed));
		else
			player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(item_id));
	}

	public static void removeItemByObjId(L2Playable playable, int item_obj_id, int count)
	{
		if(playable == null || count < 1)
			return;

		L2Playable player;
		if(playable.isSummon())
			player = playable.getPlayer();
		else
			player = playable;
		Inventory inv = player.getInventory();
		if(inv == null)
			return;
		L2ItemInstance[] items = inv.getItems();
		for(L2ItemInstance item : items)
			if(item.getObjectId() == item_obj_id && count > 0)
			{
				int item_count = item.getIntegerLimitedCount();
				int item_id = item.getItemId();
				int removed = count <= item_count ? count : item_count;
				player.getInventory().destroyItem(item, removed, true);
				if(item_id == 57)
					player.sendPacket(new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED).addNumber(removed));
				else if(removed > 1)
					player.sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(item_id).addNumber(removed));
				else
					player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(item_id));
			}
	}

	public static boolean ride(L2Player player, int pet)
	{
		if(player.isMounted())
			player.setMount(0, 0, 0);

		if(player.getPet() != null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ALREADY_HAVE_A_PET));
			return false;
		}

		player.setMount(pet, 0, 0);
		return true;
	}

	public static void unRide(L2Player player)
	{
		player.setMount(0, 0, 0);
	}

	public static L2NpcInstance spawn(Location loc, int npcId)
	{
		try
		{
			L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(npcId));
			spawn.setLoc(loc);
			return spawn.doSpawn(true);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
}