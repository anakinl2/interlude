package services;

import com.lineage.Config;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SkillList;
import l2d.game.serverpackets.SocialAction;
import l2d.game.tables.ItemTable;
import l2d.game.tables.SkillTable;
import l2d.game.templates.L2Item;

public class NoblessSell extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public void get()
	{
		L2Player player = (L2Player) self;

		if(player.isNoble())
			return;

		if(player.getSubLevel() < 75)
		{
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
				player.sendMessage("У Вас нету саб-класса 75лвла или выше.");
			else
				player.sendMessage("You do not have a sub-class 75 lvl or higher.");
			return;
		}

		L2Item item = ItemTable.getInstance().getTemplate(Config.SERVICES_NOBLESS_SELL_ITEM);
		L2ItemInstance pay = player.getInventory().getItemByItemId(item.getItemId());
		if(pay != null && pay.getCount() >= Config.SERVICES_NOBLESS_SELL_PRICE)
		{
			player.getInventory().destroyItem(pay, Config.SERVICES_NOBLESS_SELL_PRICE, true);
			player.setNoble(true);
			player.addSkill(SkillTable.getInstance().getInfo(1323, 1));
			player.addSkill(SkillTable.getInstance().getInfo(325, 1));
			player.addSkill(SkillTable.getInstance().getInfo(326, 1));
			player.addSkill(SkillTable.getInstance().getInfo(327, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1324, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1325, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1326, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1327, 1));
			addItem(player, 7694, 1);
			player.sendPacket(new SkillList(player));
			player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
			player.broadcastUserInfo(true);
		}
		else if(Config.SERVICES_NOBLESS_SELL_ITEM == 57)
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
		else
			player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
	}

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}
}