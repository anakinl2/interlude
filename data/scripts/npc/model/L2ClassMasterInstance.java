package npc.model;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.base.ClassId;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2MerchantInstance;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Files;
import com.lineage.util.Util;

public final class L2ClassMasterInstance extends L2MerchantInstance implements ScriptFile
{

	private static Logger _log = Logger.getLogger(L2ClassMasterInstance.class.getName());

	public L2ClassMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	private String makeMessage(L2Player player)
	{
		ClassId classId = player.getClassId();
		int jobLevel = classId.getLevel();
		int level = player.getLevel();
		StringBuilder html = new StringBuilder();

		if(player.getLang().equalsIgnoreCase("ru"))
			html.append("====== Менеджер профессий ======<br>");
		else
			html.append("====== Proff Manager ======<br>");

		if(Config.ALLOW_CLASS_MASTERS_LIST.isEmpty() || !Config.ALLOW_CLASS_MASTERS_LIST.contains(jobLevel))
		{
			jobLevel = 4;
		}
		if((level >= 20 && jobLevel == 1 || level >= 40 && jobLevel == 2 || level >= 76 && jobLevel == 3) && Config.ALLOW_CLASS_MASTERS_LIST.contains(jobLevel))
		{
			L2Item item = ItemTable.getInstance().getTemplate(Config.CLASS_MASTERS_PRICE_ITEM);
			if(Config.CLASS_MASTERS_PRICE_LIST[jobLevel] > 0)
			{
				html.append("Стоимость: <font color=\"LEVEL\">");
				html.append(Util.formatAdena(Config.CLASS_MASTERS_PRICE_LIST[jobLevel])).append("</font> ").append(item.getName()).append("<br>");
			}
			for(ClassId cid : ClassId.values())
			{
				// Инспектор является наследником trooper и warder, но сменить его как профессию нельзя,
				// т.к. это сабкласс. Наследуется с целью получения скилов родителей.
				if(cid.childOf(classId) && cid.getLevel() == classId.getLevel() + 1)
					html.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_change_class ").append(cid.getId()).append(" ").append(Config.CLASS_MASTERS_PRICE_LIST[jobLevel]).append("\"><font color=\"LEVEL\">").append(cid.name()).append("</font></a><br>");

			}
			player.sendPacket(new NpcHtmlMessage(player, this).setHtml(html.toString()));
		}
		else
		{
			switch(jobLevel)
			{
				case 1:
					if(player.getLang().equalsIgnoreCase("ru"))
						html.append("Возвращайтесь, когда вы достигнете 20 уровня, чтобы сменить вашу профессию.<br>");
					else
						html.append("Come back here when you reached level 20 to change your class.<br>");
					break;
				case 2:
					if(player.getLang().equalsIgnoreCase("ru"))
						html.append("Возвращайтесь, когда вы достигнете 40 уровня, чтобы сменить вашу профессию.<br>");
					else
						html.append("Come back here when you reached level 40 to change your class.<br>");
					break;
				case 3:
					if(player.getLang().equalsIgnoreCase("ru"))
						html.append("Возвращайтесь, когда вы достигнете 76 уровня, чтобы сменить вашу профессию.<br>");
					else
						html.append("Come back here when you reached level 76 to change your class.<br>");
					break;
				case 4:
					if(player.getLang().equalsIgnoreCase("ru"))
						html.append("Для вас больше нет доступных профессий.<br>");
					else
						html.append("There is no class changes for you any more.<br>");
					break;
			}
		}
		html.append("===========================");
		return html.toString();
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		NpcHtmlMessage msg = new NpcHtmlMessage(player, this, null, 0);
		String html = Files.read("data/html/custom/31860.htm", player);

		if(Config.SERVICES_CLASSMASTERS_BASIC_SHOP)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h npc_%objectId%_Buy 318601\">Купить обычные вещи</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h npc_%objectId%_Buy 318601\">Buy basic items</a>";
			}
		}
		if(Config.SERVICES_CLASSMASTERS_COL_SHOP)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h npc_%objectId%_Multisell 1\">Продвинутый магазин</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h npc_%objectId%_Multisell 1\">Special shop</a>";
			}
		}
		if(Config.SERVICES_CHANGE_NICK_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.Rename:rename_page\">Смена ника</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.Rename:rename_page\">Nick change</a>";
			}
		}
		if(Config.SERVICES_CHANGE_SEX_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.Rename:changesex_page\">Смена пола</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.Rename:changesex_page\">Sex change</a>";
			}
		}
		if(Config.SERVICES_CHANGE_NICK_COLOR_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.NickColor:list\">Смена цвета ника</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.NickColor:list\">Nick color change</a>";
			}
		}
		if(Config.SERVICES_RATE_BONUS_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.RateBonus.RateBonus:list\">Премиум аккаунт</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.RateBonus.RateBonus:list\">Premium Account</a>";
			}
		}
		if(Config.SERVICES_RATE_SPECIAL_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.Bonus.bonus:show\">Премиум рейты</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.Bonus.bonus:show\">Premium rates</a>";
			}
		}
		if(Config.SERVICES_NOBLESS_SELL_ENABLED && !player.isNoble())
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.NoblessSell:get\">Стать дворянином</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.NoblessSell:get\">Become a Nobless</a>";
			}
		}
		if(Config.SERVICES_HOW_TO_GET_COL)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.RateBonus.RateBonus:howtogetcol\">Как получить Coin of Mif</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.RateBonus.RateBonus:howtogetcol\">How to get Coin of Mif</a>";
			}
		}
		if(Config.SERVICES_DONATE)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_events.donate.Shop:Chalange 10\">Donate Услуги</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_events.donate.Shop:Chalange 10\">Donate</a>";
			}
		}
		if(Config.SERVICES_CHANGE_PET_NAME_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.petevolve.exchange:showErasePetName\">Обнулить имя у пета</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.petevolve.exchange:showErasePetName\">Delete Pet's Name</a>";
			}
		}
		if(Config.SERVICES_EXCHANGE_BABY_PET_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.petevolve.exchange:showBabyPetExchange\">Обменять Baby пета</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.petevolve.exchange:showBabyPetExchange\">Change Baby Pet</a>";
			}
		}
		if(Config.SERVICES_EXPAND_INVENTORY_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.ExpandInventory:show\">Сервис расширения инвентаря</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.ExpandInventory:show\">Service of expansion of inventory</a>";
			}
		}
		if(Config.SERVICES_EXPAND_WAREHOUSE_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.ExpandWarhouse:show\">Сервис расширения склада</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.ExpandWarhouse:show\">Service of expansion of WareHouse</a>";
			}
		}
		if(Config.SERVICES_EXPAND_CWH_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.ExpandCWH:show\">Сервис расширения кланового склада</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.ExpandCWH:show\">Service of expansion of Clan WareHouse</a>";
			}
		}
		if(Config.SERVICES_WINDOW_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.Window:show\">Сервис расширения числа окон</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.Window:show\">Service of expansion of number of windows</a>";
			}
		}
		if(Config.SERVICES_CHANGE_CLAN_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_services.ServiceClan:clan_rename_page\">Смена названия клана</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_services.ServiceClan:clan_rename_page\">Change Clan Name</a>";
			}
		}
		if(Config.ENCHANT_MASTER_ENABLED)
		{
			if(player.getLang().equalsIgnoreCase("ru"))
			{
				html += "<br><a action=\"bypass -h scripts_events.enchanter.Main:Chalange\">Мастер заточки</a>";
			}
			else
			{
				html += "<br><a action=\"bypass -h scripts_events.enchanter.Main:Chalange\">Enchant</a>";
			}
		}

		msg.setHtml(html);
		msg.replace("%classmaster%", makeMessage(player));
		player.sendPacket(msg);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command);
		if(st.nextToken().equals("change_class"))
		{
			short val = Short.parseShort(st.nextToken());
			int price = Integer.parseInt(st.nextToken());
			L2Item item = ItemTable.getInstance().getTemplate(Config.CLASS_MASTERS_PRICE_ITEM);
			L2ItemInstance pay = player.getInventory().getItemByItemId(item.getItemId());
			if(pay != null && pay.getCount() >= price)
			{
				player.getInventory().destroyItem(pay, price, true);
				changeClass(player, val);
			}
			else if(Config.CLASS_MASTERS_PRICE_ITEM == 57)
			{
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			}
			else
			{
				player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	private void changeClass(L2Player player, short val)
	{
		if(Config.DEBUG)
		{
			_log.fine("Changing class to ClassId:" + val);
			_log.fine("name:" + player.getName());
			_log.fine("level:" + player.getLevel());
			_log.fine("classId:" + player.getClassId());
		}

		if(player.getClassId().getLevel() == 3)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_COMPLETED_THE_QUEST_FOR_3RD_OCCUPATION_CHANGE_AND_MOVED_TO_ANOTHER_CLASS_CONGRATULATIONS)); // для 3 профы
		}
		else
		{
			player.sendPacket(new SystemMessage(SystemMessage.CONGRATULATIONS_YOU_HAVE_TRANSFERRED_TO_A_NEW_CLASS)); // для 1 и 2 профы
		}
		player.setClassId(val, false);
		player.broadcastUserInfo(true);
		player.rewardSkills();
		if(Config.CLASS_MASTERS_SAY != "")
		{
			String text = Config.CLASS_MASTERS_SAY.replaceFirst("%player%", player.getName()).replaceFirst("%prof%", player.getClassId().name());
			Functions.npcShout(this, text);
		}
	}

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}
}