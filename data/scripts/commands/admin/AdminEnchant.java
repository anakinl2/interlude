package commands.admin;

import javolution.text.TextBuilder;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.InventoryUpdate;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.util.Log;

public class AdminEnchant implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_seteh, // 6
		admin_setec, // 10
		admin_seteg, // 9
		admin_setel, // 11
		admin_seteb, // 12
		admin_setew, // 7
		admin_setes, // 8
		admin_setle, // 1
		admin_setre, // 2
		admin_setlf, // 4
		admin_setrf, // 5
		admin_seten, // 3
		admin_setun, // 0
		admin_enchant
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanEditChar)
			return false;

		int armorType = -1;

		switch(command)
		{
			case admin_enchant:
				showMainPage(activeChar);
				return true;
			case admin_seteh:
				armorType = Inventory.PAPERDOLL_HEAD;
				break;
			case admin_setec:
				armorType = Inventory.PAPERDOLL_CHEST;
				break;
			case admin_seteg:
				armorType = Inventory.PAPERDOLL_GLOVES;
				break;
			case admin_seteb:
				armorType = Inventory.PAPERDOLL_FEET;
				break;
			case admin_setel:
				armorType = Inventory.PAPERDOLL_LEGS;
				break;
			case admin_setew:
				armorType = Inventory.PAPERDOLL_RHAND;
				break;
			case admin_setes:
				armorType = Inventory.PAPERDOLL_LHAND;
				break;
			case admin_setle:
				armorType = Inventory.PAPERDOLL_LEAR;
				break;
			case admin_setre:
				armorType = Inventory.PAPERDOLL_REAR;
				break;
			case admin_setlf:
				armorType = Inventory.PAPERDOLL_LFINGER;
				break;
			case admin_setrf:
				armorType = Inventory.PAPERDOLL_RFINGER;
				break;
			case admin_seten:
				armorType = Inventory.PAPERDOLL_NECK;
				break;
			case admin_setun:
				armorType = Inventory.PAPERDOLL_UNDER;
				break;
		}

		if(armorType == -1 || wordList.length < 2)
		{
			showMainPage(activeChar);
			return true;
		}

		try
		{
			int ench = Integer.parseInt(wordList[1]);
			if(ench < 0 || ench > 65535)
				activeChar.sendMessage("You must set the enchant level to be between 0-65535.");
			else
				setEnchant(activeChar, ench, armorType);
		}
		catch(StringIndexOutOfBoundsException e)
		{
			activeChar.sendMessage("Please specify a new enchant value.");
		}
		catch(NumberFormatException e)
		{
			activeChar.sendMessage("Please specify a valid new enchant value.");
		}

		// show the enchant menu after an action
		showMainPage(activeChar);
		return true;
	}

	private void setEnchant(L2Player activeChar, int ench, int armorType)
	{
		// get the target
		L2Object target = activeChar.getTarget();
		if(target == null)
			target = activeChar;
		if(!target.isPlayer())
		{
			activeChar.sendMessage("Wrong target type.");
			return;
		}

		L2Player player = (L2Player) target;

		// now we need to find the equipped weapon of the targeted character...
		int curEnchant = 0; // display purposes only

		// only attempt to enchant if there is a weapon equipped
		L2ItemInstance itemInstance = player.getInventory().getPaperdollItem(armorType);

		if(itemInstance != null)
		{
			curEnchant = itemInstance.getEnchantLevel();

			// set enchant value
			player.getInventory().unEquipItemInSlot((short) armorType);
			itemInstance.setEnchantLevel(ench);
			player.getInventory().equipItem(itemInstance);

			// send packets
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(itemInstance);
			player.sendPacket(iu);
			player.broadcastUserInfo(true);

			// informations
			activeChar.sendMessage("Changed enchantment of " + player.getName() + "'s " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".");
			player.sendMessage("Admin has changed the enchantment of your " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".");

			Log.add(activeChar + " change the enchantment of " + player.getName() + "'s " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".", "gm_ext_actions", activeChar);
		}
	}

	public void showMainPage(L2Player activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<center><table width=260><tr><td width=40>");
		replyMSG.append("<button value=\"Main\" action=\"bypass -h admin_admin\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</td><td width=180>");
		replyMSG.append("<center>Enchant Equip</center>");
		replyMSG.append("</td><td width=40>");
		replyMSG.append("</td></tr></table></center><br>");
		replyMSG.append("<center><table width=270><tr><td>");
		replyMSG.append("<button value=\"Underwear\" action=\"bypass -h admin_setun $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Helmet\" action=\"bypass -h admin_seteh $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		/**replyMSG.append("<button value=\"Cloak\" action=\"bypass -h admin_setba $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");**/
		replyMSG.append("<button value=\"Mask\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Necklace\" action=\"bypass -h admin_seten $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
		replyMSG.append("</center><center><table width=270><tr><td>");
		replyMSG.append("<button value=\"Weapon\" action=\"bypass -h admin_setew $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Chest\" action=\"bypass -h admin_setec $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Shield\" action=\"bypass -h admin_setes $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Earring\" action=\"bypass -h admin_setre $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Earring\" action=\"bypass -h admin_setle $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
		replyMSG.append("</center><center><table width=270><tr><td>");
		replyMSG.append("<button value=\"Gloves\" action=\"bypass -h admin_seteg $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Leggings\" action=\"bypass -h admin_setel $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Boots\" action=\"bypass -h admin_seteb $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Ring\" action=\"bypass -h admin_setrf $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Ring\" action=\"bypass -h admin_setlf $menu_command\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
		replyMSG.append("</center><br>");
		replyMSG.append("<center>[Enchant 0-65535]</center>");
		replyMSG.append("<center><edit var=\"menu_command\" width=100 height=15></center><br>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}