package l2d.game.model.instances;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import l2d.ext.multilang.CustomMessage;
import l2d.game.TradeController;
import l2d.game.cache.Msg;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.CastleManorManager;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2TradeList;
import l2d.game.model.entity.SevenSigns;
import l2d.game.model.entity.residence.Residence;
import l2d.game.serverpackets.BuyList;
import l2d.game.serverpackets.ExShowCropInfo;
import l2d.game.serverpackets.ExShowCropSetting;
import l2d.game.serverpackets.ExShowManorDefaultInfo;
import l2d.game.serverpackets.ExShowSeedInfo;
import l2d.game.serverpackets.ExShowSeedSetting;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2NpcTemplate;
import l2d.util.Log;

public class L2CastleChamberlainInstance extends L2ResidenceManager
{
	private static Logger _log = Logger.getLogger(L2CastleChamberlainInstance.class.getName());

	private static int Cond_All_False = 0;
	private static int Cond_Busy_Because_Of_Siege = 1;
	private static int Cond_Clan = 2;
	private static int Cond_Clan_wPrivs = 3;
	private static int Cond_Owner = 4;

	public L2CastleChamberlainInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		player.sendActionFailed();
		if(player.isActionsDisabled() || player.isSitting() || player.getLastNpc().getDistance(player) > 300)
			return;

		int condition = validateCondition(player);
		if(condition <= Cond_All_False)
			return;

		if(condition == Cond_Busy_Because_Of_Siege)
			return;

		if(condition < Cond_Clan_wPrivs)
			return;

		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();
		String val = "";
		if(st.countTokens() >= 1)
			val = st.nextToken();

		if(command.equalsIgnoreCase("banish_foreigner"))
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_DISMISS))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			if(!val.equals(""))
			{
				getCastle().banishForeigner(player); // Move non-clan members off castle area
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/castle/chamberlain/chamberlain-afterbanish.htm");
				player.sendPacket(html);
				return;
			}

			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/chamberlain-banish.htm");
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("list_siege_clans"))
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			getCastle().getSiege().listRegisterClan(player);
		}
		else if(actualCommand.equalsIgnoreCase("CastleFunctions"))
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_SET_FUNCTIONS))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/chamberlain-castlefunc.htm");
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("ManageTreasure"))
		{
			if(!player.isClanLeader())
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/chamberlain-castlevault.htm");
			html.replace("%Treasure%", String.valueOf(getCastle().getTreasury()));
			html.replace("%CollectedShops%", String.valueOf(getCastle().getCollectedShops()));
			html.replace("%CollectedSeed%", String.valueOf(getCastle().getCollectedSeed()));
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("TakeTreasure"))
		{
			if(!player.isClanLeader())
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			if(!val.equals(""))
			{
				int treasure = Integer.parseInt(val);
				if(getCastle().getTreasury() < treasure)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile("data/html/castle/chamberlain/chamberlain-havenottreasure.htm");
					html.replace("%Treasure%", String.valueOf(getCastle().getTreasury()));
					html.replace("%Requested%", String.valueOf(treasure));
					player.sendPacket(html);
					return;
				}
				if(treasure > 0)
				{
					getCastle().addToTreasuryNoTax(-treasure, false, false);
					Log.add(getCastle().getName() + "|" + -treasure + "|CastleChamberlain", "treasury");
					player.addAdena(treasure);
				}
			}

			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/chamberlain-castlevault.htm");
			html.replace("%Treasure%", String.valueOf(getCastle().getTreasury()));
			html.replace("%CollectedShops%", String.valueOf(getCastle().getCollectedShops()));
			html.replace("%CollectedSeed%", String.valueOf(getCastle().getCollectedSeed()));
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("PutTreasure"))
		{
			if(!val.equals(""))
			{
				int treasure = Integer.parseInt(val);
				if(treasure > player.getAdena())
				{
					player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
					return;
				}
				if(treasure > 0)
				{
					getCastle().addToTreasuryNoTax(treasure, false, false);
					Log.add(getCastle().getName() + "|" + treasure + "|CastleChamberlain", "treasury");
					player.reduceAdena(treasure);
				}
			}

			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/chamberlain-castlevault.htm");
			html.replace("%Treasure%", String.valueOf(getCastle().getTreasury()));
			html.replace("%CollectedShops%", String.valueOf(getCastle().getCollectedShops()));
			html.replace("%CollectedSeed%", String.valueOf(getCastle().getCollectedSeed()));
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("manor"))
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_MANOR_ADMIN))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			String filename = "";
			if(CastleManorManager.getInstance().isDisabled())
				filename = "data/html/npcdefault.htm";
			else
			{
				int cmd = Integer.parseInt(val);
				switch(cmd)
				{
					case 0:
						filename = "data/html/castle/chamberlain/manor/manor.htm";
						break;
					// TODO: correct in html's to 1
					case 4:
						filename = "data/html/castle/chamberlain/manor/manor_help00" + st.nextToken() + ".htm";
						break;
					default:
						filename = "data/html/castle/chamberlain/chamberlain-no.htm";
						break;
				}
			}

			if(filename.length() > 0)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName());
				player.sendPacket(html);
			}
		}
		else if(actualCommand.startsWith("manor_menu_select"))
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_MANOR_ADMIN))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			// input string format:
			// manor_menu_select?ask=X&state=Y&time=X
			if(CastleManorManager.getInstance().isUnderMaintenance())
			{
				player.sendActionFailed();
				player.sendPacket(new SystemMessage(SystemMessage.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE));
				return;
			}

			String params = actualCommand.substring(actualCommand.indexOf("?") + 1);
			StringTokenizer str = new StringTokenizer(params, "&");
			int ask = Integer.parseInt(str.nextToken().split("=")[1]);
			int state = Integer.parseInt(str.nextToken().split("=")[1]);
			int time = Integer.parseInt(str.nextToken().split("=")[1]);

			int castleId;
			if(state == -1) // info for current manor
				castleId = getCastle().getId();
			else
				// info for requested manor
				castleId = state;

			switch(ask)
			{ // Main action
				case 3: // Current seeds (Manor info)
					if(time == 1 && !CastleManager.getInstance().getCastleByIndex(castleId).isNextPeriodApproved())
						player.sendPacket(new ExShowSeedInfo(castleId, null));
					else
						player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleByIndex(castleId).getSeedProduction(time)));
					break;
				case 4: // Current crops (Manor info)
					if(time == 1 && !CastleManager.getInstance().getCastleByIndex(castleId).isNextPeriodApproved())
						player.sendPacket(new ExShowCropInfo(castleId, null));
					else
						player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleByIndex(castleId).getCropProcure(time)));
					break;
				case 5: // Basic info (Manor info)
					player.sendPacket(new ExShowManorDefaultInfo());
					break;
				case 7: // Edit seed setup
					if(getCastle().isNextPeriodApproved())
						player.sendPacket(new SystemMessage(SystemMessage.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
					else
						player.sendPacket(new ExShowSeedSetting(getCastle().getId()));
					break;
				case 8: // Edit crop setup
					if(getCastle().isNextPeriodApproved())
						player.sendPacket(new SystemMessage(SystemMessage.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
					else
						player.sendPacket(new ExShowCropSetting(getCastle().getId()));
					break;
			}
		}
		else if(actualCommand.equalsIgnoreCase("operate_door")) // door control
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_OPEN_DOOR))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			if(!val.equals(""))
			{
				boolean open = Integer.parseInt(val) == 1;
				while(st.hasMoreTokens())
					getCastle().openCloseDoor(player, Integer.parseInt(st.nextToken()), open);
			}

			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/" + getTemplate().npcId + "-d.htm");
			html.replace("%npcname%", getName());
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("tax_set")) // tax rates control
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_TAXES))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			if(!val.equals(""))
			{
				// По умолчанию налог не более 15%
				Integer maxTax = 15;
				// Если печатью SEAL_STRIFE владеют DUSK то налог можно выставлять не более 5%
				if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
					maxTax = 5;
				// Если печатью SEAL_STRIFE владеют DAWN то налог можно выставлять не более 25%
				else if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
					maxTax = 25;

				if(Integer.parseInt(val) < 0 || Integer.parseInt(val) > maxTax)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setFile("data/html/castle/chamberlain/chamberlain-hightax.htm");
					html.replace("%CurrentTax%", String.valueOf(getCastle().getTaxPercent()));
					player.sendPacket(html);
					return;
				}
				getCastle().setTaxPercent(player, Integer.parseInt(val));
			}

			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/chamberlain-settax.htm");
			html.replace("%CurrentTax%", String.valueOf(getCastle().getTaxPercent()));
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("upgrade_castle"))
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/chamberlain-upgrades.htm");
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("reinforce"))
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/doorStrengthen-" + getCastle().getName() + ".htm");
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("door_manage"))
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_OPEN_DOOR))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/doorManage.htm");
			html.replace("%id%", val);
			html.replace("%type%", st.nextToken());
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("upgrade_door_confirm"))
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			int id = Integer.parseInt(val);
			int type = Integer.parseInt(st.nextToken());
			int level = Integer.parseInt(st.nextToken());
			long price = getDoorCost(type, level);

			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/doorConfirm.htm");
			html.replace("%id%", String.valueOf(id));
			html.replace("%level%", String.valueOf(level));
			html.replace("%type%", String.valueOf(type));
			html.replace("%price%", String.valueOf(price));
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("upgrade_door"))
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			int id = Integer.parseInt(val);
			int type = Integer.parseInt(st.nextToken());
			int level = Integer.parseInt(st.nextToken());
			int price = getDoorCost(type, level);

			L2DoorInstance door = getCastle().getDoor(id);
			int upgradeHp = (door.getMaxHp() - door.getUpgradeHp()) * level - door.getMaxHp();

			if(price == 0 || upgradeHp < 0)
			{
				player.sendMessage(new CustomMessage("common.Error", player));
				return;
			}

			if(door.getUpgradeHp() >= upgradeHp)
			{
				int oldLevel = door.getUpgradeHp() / (door.getMaxHp() - door.getUpgradeHp()) + 1;
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/castle/chamberlain/doorAlready.htm");
				html.replace("%level%", String.valueOf(oldLevel));
				player.sendPacket(html);
				return;
			}

			L2ItemInstance adena = player.getClan().getAdena();
			if(adena == null || adena.getCount() < price)
			{
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				return;
			}

			player.getClan().getWarehouse().destroyItem(57, price);
			getCastle().upgradeDoor(id, upgradeHp, true);
		}
		else if(actualCommand.equalsIgnoreCase("report")) // Report page
		{
			if(!isHaveRigths(player, L2Clan.CP_CS_USE_FUNCTIONS))
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			String ssq_period;
			if(SevenSigns.getInstance().getCurrentPeriod() == 1)
				ssq_period = "Competition";
			else if(SevenSigns.getInstance().getCurrentPeriod() == 3)
				ssq_period = "Effective sealing";
			else
				ssq_period = "Ready";

			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/chamberlain-report.htm");
			html.replace("%FeudName%", getCastle().getName());
			html.replace("%CharClan%", player.getClan().getName());
			html.replace("%CharName%", player.getName());
			html.replace("%SSPeriod%", ssq_period);
			html.replace("%Avarice%", getSealOwner(1));
			html.replace("%Revelation%", getSealOwner(2));
			html.replace("%Strife%", getSealOwner(3));
			player.sendPacket(html);
		}
		else if(command.equalsIgnoreCase("items")) // Main page for items
		{
			if(!player.isClanLeader())
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/" + getTemplate().getNpcId() + "-items.htm");
			html.replace("%npcname%", getName());
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("Crown")) // Give Crown to Castle Owner
		{
			if(!player.isClanLeader())
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2ResidenceManager.NotAuthorizedToDoThis", player));
				return;
			}
			if(player.getInventory().getItemByItemId(6841) == null)
			{
				L2ItemInstance CrownItem = ItemTable.getInstance().createItem(6841);
				player.getInventory().addItem(CrownItem);

				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/castle/chamberlain/gavecrown.htm");
				html.replace("%CharName%", String.valueOf(player.getName()));
				html.replace("%FeudName%", getCastle().getName());
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("data/html/castle/chamberlain/alreadyhavecrown.htm");
				player.sendPacket(html);
			}
		}
		else if(actualCommand.equalsIgnoreCase("default"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			html.setFile("data/html/castle/chamberlain/chamberlain.htm");
			player.sendPacket(html);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = "data/html/castle/chamberlain/chamberlain-notlord.htm";
		int condition = validateCondition(player);
		if(condition > Cond_All_False)
			if(condition == Cond_Busy_Because_Of_Siege)
				filename = "data/html/castle/chamberlain/chamberlain-busy.htm";
			else if(condition == Cond_Owner || condition == Cond_Clan_wPrivs) // Clan owns castle
				filename = "data/html/castle/chamberlain/chamberlain.htm";
		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	protected int validateCondition(L2Player player)
	{
		if(player.isGM())
			return Cond_Owner;
		Residence castle = getCastle();
		if(castle != null && castle.getId() > 0)
			if(player.getClan() != null)
				if(castle.getSiege().isInProgress())
					return Cond_Busy_Because_Of_Siege; // Busy because of siege
				else if(castle.getOwnerId() == player.getClanId())
				{
					if(player.isClanLeader()) // Leader of clan
						return Cond_Owner;
					if(isHaveRigths(player, L2Clan.CP_CS_OPEN_DOOR) || // doors
					isHaveRigths(player, L2Clan.CP_CS_MANOR_ADMIN) || // manor
					isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE) || // siege
					isHaveRigths(player, L2Clan.CP_CS_USE_FUNCTIONS) || // funcs
					isHaveRigths(player, L2Clan.CP_CS_DISMISS) || // banish
					isHaveRigths(player, L2Clan.CP_CS_TAXES) || // tax
					isHaveRigths(player, L2Clan.CP_CS_MERCENARIES) || // merc
					isHaveRigths(player, L2Clan.CP_CS_SET_FUNCTIONS) // funcs
					)
						return Cond_Clan_wPrivs; // Есть какие либо замковые привилегии
					return Cond_Clan;
				}

		return Cond_All_False;
	}

	private String getSealOwner(int seal)
	{
		switch(SevenSigns.getInstance().getSealOwner(seal))
		{
			case SevenSigns.CABAL_DUSK:
				return "Evening";
			case SevenSigns.CABAL_DAWN:
				return "Dawn";
			default:
				return "None belongs";
		}
	}

	@Override
	protected void showBuyWindow(L2Player player, int val)
	{
		if(!player.getPlayerAccess().UseShop)
			return;
		player.tempInvetoryDisable();
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		if(list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
			player.sendPacket(new BuyList(list, player, 0));
		else
		{
			_log.warning("[L2CastleChamberlainInstance] possible client hacker: " + player.getName() + " attempting to buy from GM shop! < Ban him!");
			_log.warning("buylist id:" + val + " / list_npc = " + (list == null ? "nulllist" : list.getNpcId()) + " / npc = " + getNpcId());
		}
	}

	private int getDoorCost(int type, int level)
	{
		int price = 0;

		switch(type)
		{
			case 1: // Главные ворота
				switch(level)
				{
					case 2:
						price = 3000000;
						break;
					case 3:
						price = 4000000;
						break;
					case 5:
						price = 5000000;
						break;
				}
				break;
			case 2: // Внутренние ворота
				switch(level)
				{
					case 2:
						price = 750000;
						break;
					case 3:
						price = 900000;
						break;
					case 5:
						price = 1000000;
						break;
				}
				break;
			case 3: // Стены
				switch(level)
				{
					case 2:
						price = 1600000;
						break;
					case 3:
						price = 1800000;
						break;
					case 5:
						price = 2000000;
						break;
				}
				break;
		}

		int SSQ_DawnFactor_door = 80;
		int SSQ_DrawFactor_door = 100;
		int SSQ_DuskFactor_door = 300;

		switch(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DUSK:
				price = price * SSQ_DuskFactor_door / 100;
				break;
			case SevenSigns.CABAL_DAWN:
				price = price * SSQ_DawnFactor_door / 100;
				break;
			default:
				price = price * SSQ_DrawFactor_door / 100;
				break;
		}

		return price;
	}

	@Override
	protected Residence getResidence()
	{
		return getCastle();
	}

	@Override
	public void broadcastDecoInfo()
	{}

	@Override
	protected int getPrivFunctions()
	{
		return L2Clan.CP_CS_SET_FUNCTIONS;
	}

	@Override
	protected int getPrivDismiss()
	{
		return L2Clan.CP_CS_DISMISS;
	}

	@Override
	protected int getPrivDoors()
	{
		return L2Clan.CP_CS_OPEN_DOOR;
	}
}