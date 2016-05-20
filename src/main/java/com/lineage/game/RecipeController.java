package com.lineage.game;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.cache.Msg;
import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2ManufactureItem;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Recipe;
import com.lineage.game.model.L2RecipeComponent;
import com.lineage.game.model.L2World;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.RecipeBookItemList;
import com.lineage.game.serverpackets.RecipeItemMakeInfo;
import com.lineage.game.serverpackets.RecipeShopItemInfo;
import com.lineage.game.serverpackets.StatusUpdate;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ItemTable;
import com.lineage.util.Rnd;

public class RecipeController
{
	protected static Logger _log = Logger.getLogger(RecipeController.class.getName());
	private static RecipeController _instance;

	private HashMap<Integer, L2Recipe> _listByRecipeId;
	private HashMap<Integer, L2Recipe> _listByRecipeItem;

	public static RecipeController getInstance()
	{
		if(_instance == null)
			_instance = new RecipeController();
		return _instance;
	}

	public RecipeController()
	{
		_listByRecipeId = new HashMap<Integer, L2Recipe>();
		_listByRecipeItem = new HashMap<Integer, L2Recipe>();
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		FiltredPreparedStatement st2 = null;
		ResultSet list = null, rset2 = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM recipes");
			st2 = con.prepareStatement("SELECT * FROM `recitems` WHERE `rid`=?");
			list = statement.executeQuery();

			while(list.next())
			{
				Vector<L2RecipeComponent> recipePartList = new Vector<L2RecipeComponent>();

				boolean isDvarvenCraft = list.getBoolean("dwarven");
				String recipeName = list.getString("name");
				int id = list.getInt("id");
				int recipeId = list.getShort("recid");
				int level = list.getInt("lvl");
				short itemId = list.getShort("item");
				short foundation = list.getShort("foundation");
				short count = list.getShort("q");
				int mpCost = list.getInt("mp");
				int successRate = list.getInt("success");
				long exp = list.getLong("exp");
				long sp = list.getLong("sp");

				// material
				st2.setInt(1, id);
				rset2 = st2.executeQuery();
				while(rset2.next())
				{
					int rpItemId = rset2.getInt("item");
					int quantity = rset2.getInt("q");
					L2RecipeComponent rp = new L2RecipeComponent(rpItemId, quantity);
					recipePartList.add(rp);
				}

				L2Recipe recipeList = new L2Recipe(id, level, recipeId, recipeName, successRate, mpCost, itemId, foundation, count, exp, sp, isDvarvenCraft);
				for(L2RecipeComponent recipePart : recipePartList)
					recipeList.addRecipe(recipePart);
				_listByRecipeId.put(id, recipeList);
				_listByRecipeItem.put(recipeId, recipeList);
			}
			_log.info("[ Recipe Controller ]");
			_log.info(" ~ Loaded: " + _listByRecipeId.size() + " Recipes.");
			_log.info("[ Recipe Controller ]\n");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseSR(st2, rset2);
			DatabaseUtils.closeDatabaseCSR(con, statement, list);
		}
	}

	public Collection<L2Recipe> getRecipes()
	{
		return _listByRecipeId.values();
	}

	public L2Recipe getRecipeByRecipeId(int listId)
	{
		return _listByRecipeId.get(listId);
	}

	public L2Recipe getRecipeByRecipeItem(int itemId)
	{
		return _listByRecipeItem.get(itemId);
	}

	public void requestBookOpen(L2Player player, boolean isDwarvenCraft)
	{
		RecipeBookItemList response = new RecipeBookItemList(isDwarvenCraft, (int) player.getCurrentMp());
		if(isDwarvenCraft)
			response.setRecipes(player.getDwarvenRecipeBook());
		else
			response.setRecipes(player.getCommonRecipeBook());
		player.sendPacket(response);
	}

	public void requestMakeItem(L2Player player, int recipeListId)
	{
		L2Recipe recipeList = getRecipeByRecipeId(recipeListId);
		player.resetWaitSitTime();

		if(recipeList == null || recipeList.getRecipes().length == 0)
		{
			player.sendPacket(Msg.THE_RECIPE_IS_INCORRECT);
			return;
		}

		synchronized (player)
		{
			if(player.getCurrentMp() < recipeList.getMpCost())
			{
				player.sendPacket(Msg.NOT_ENOUGH_MP);
				player.sendPacket(new RecipeItemMakeInfo(recipeList.getId(), player, 0));
				return;
			}

			if(!player.findRecipe(recipeListId))
			{
				player.sendPacket(Msg.PLEASE_REGISTER_A_RECIPE);
				player.sendActionFailed();
				return;
			}
		}

		synchronized (player.getInventory())
		{
			L2RecipeComponent[] recipes = recipeList.getRecipes();
			Inventory inventory = player.getInventory();
			for(L2RecipeComponent recipe : recipes)
			{
				if(recipe.getQuantity() == 0)
					continue;

				L2ItemInstance invItem = inventory.getItemByItemId(recipe.getItemId());

				if(invItem == null || recipe.getQuantity() > invItem.getIntegerLimitedCount())
				{
					player.sendPacket(Msg.NOT_ENOUGH_MATERIALS);
					player.sendPacket(new RecipeItemMakeInfo(recipeList.getId(), player, 0));
					return;
				}
			}

			player.reduceCurrentMp(recipeList.getMpCost(), null);

			for(L2RecipeComponent recipe : recipes)
				if(recipe.getQuantity() != 0)
				{
					L2ItemInstance invItem = inventory.getItemByItemId(recipe.getItemId());
					inventory.destroyItem(invItem, recipe.getQuantity(), false);
					player.sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(invItem.getItemId()).addNumber(recipe.getQuantity()));

				}
		}

		boolean doubl = false;
		if(recipeList.getFoundation() > 0 && Rnd.chance(3))
			doubl = true;

		int success = 0;
		if(!Rnd.chance(recipeList.getSuccessRate()))
			player.sendPacket(new SystemMessage(SystemMessage.S1_MANUFACTURING_FAILURE).addItemName(recipeList.getItemId()));
		else
		{
			for(int i = 0; i < (doubl ? 2 : 1); i++)
			{
				L2ItemInstance createdItem = ItemTable.getInstance().createItem(recipeList.getFoundation() > 0 && Rnd.chance(5) ? recipeList.getFoundation() : recipeList.getItemId());
				createdItem.setCount(recipeList.getCount());

				player.getInventory().addItem(createdItem);
				SystemMessage sm;
				if(recipeList.getCount() > 1)
				{
					sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S);
					sm.addItemName(createdItem.getItemId());
					sm.addNumber(recipeList.getCount());
				}
				else
				{
					sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1);
					sm.addItemName(createdItem.getItemId());
				}
				player.sendPacket(sm);
			}
			success++;
		}

		if(Config.ALT_GAME_EXP_FOR_CRAFT)
			player.addExpAndSp((long) (recipeList.getExp() * Config.RATE_XP), (long) (recipeList.getSp() * Config.RATE_SP), true, false);
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		su.addAttribute(StatusUpdate.CUR_MP, (int) player.getCurrentMp());
		player.sendPacket(su);
		player.sendPacket(new RecipeItemMakeInfo(recipeList.getId(), player, success));
	}

	/***************************************************************************/

	public void requestManufactureItem(L2Player player, L2Player employer, int recipeListId)
	{
		L2Recipe recipeList = getRecipeByRecipeId(recipeListId);
		if(recipeList == null)
			return;

		player.resetWaitSitTime();
		int success = 0;

		player.sendMessage(new CustomMessage("com.lineage.game.RecipeController.GotOrder", player).addString(recipeList.getRecipeName()));

		if(recipeList.getRecipes().length == 0)
		{
			player.sendMessage(new CustomMessage("com.lineage.game.RecipeController.NoRecipe", player).addString(recipeList.getRecipeName()));
			employer.sendMessage(new CustomMessage("com.lineage.game.RecipeController.NoRecipe", player).addString(recipeList.getRecipeName()));
			return;
		}

		synchronized (player)
		{
			if(player.getCurrentMp() < recipeList.getMpCost())
			{
				player.sendPacket(Msg.NOT_ENOUGH_MP);
				employer.sendPacket(Msg.NOT_ENOUGH_MP);
				employer.sendPacket(new RecipeShopItemInfo(player.getObjectId(), recipeListId, success));
				return;
			}

			if(!player.findRecipe(recipeListId))
			{
				player.sendPacket(Msg.PLEASE_REGISTER_A_RECIPE);
				player.sendActionFailed();
				return;
			}
		}

		int price = 0;
		for(L2ManufactureItem temp : player.getCreateList().getList())
			if(temp.getRecipeId() == recipeList.getId())
			{
				price = temp.getCost();
				break;
			}

		if(employer.getAdena() < price)
		{
			employer.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			employer.sendPacket(new RecipeShopItemInfo(player.getObjectId(), recipeListId, success));
			return;
		}

		synchronized (employer.getInventory())
		{
			L2RecipeComponent[] recipes = recipeList.getRecipes();
			Inventory inventory = employer.getInventory();
			for(L2RecipeComponent recipe : recipes)
			{
				if(recipe.getQuantity() == 0)
					continue;

				L2ItemInstance invItem = inventory.getItemByItemId(recipe.getItemId());

				if(invItem == null || recipe.getQuantity() > invItem.getIntegerLimitedCount())
				{
					employer.sendPacket(Msg.NOT_ENOUGH_MATERIALS);
					employer.sendPacket(new RecipeShopItemInfo(player.getObjectId(), recipeListId, success));
					return;
				}
			}

			player.reduceCurrentMp(recipeList.getMpCost(), null);

			for(L2RecipeComponent recipe : recipes)
				if(recipe.getQuantity() != 0)
				{
					L2ItemInstance invItem = inventory.getItemByItemId(recipe.getItemId());
					inventory.destroyItem(invItem, recipe.getQuantity(), false);
					employer.sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(invItem.getItemId()).addNumber(recipe.getQuantity()));
				}
		}

		if(price > 0)
		{
			employer.reduceAdena(price);
			player.addAdena(price);

			int tax = (int) (price * Config.SERVICES_TRADE_TAX / 100);
			if(ZoneManager.getInstance().checkIfInZone(L2Zone.ZoneType.offshore, player.getX(), player.getY()))
				tax = (int) (price * Config.SERVICES_OFFSHORE_TRADE_TAX / 100);
			if(Config.SERVICES_TRADE_TAX_ONLY_OFFLINE && !player.isInOfflineMode())
				tax = 0;
			if(tax > 0)
			{
				player.reduceAdena(tax);
				L2World.addTax(tax);
				player.sendMessage(new CustomMessage("trade.HavePaidTax", player).addNumber(tax));
			}
		}

		boolean doubl = false;
		if(recipeList.getFoundation() > 0 && Rnd.chance(3))
			doubl = true;

		SystemMessage msgtoemployer;
		SystemMessage msgtomaster;
		if(!Rnd.chance(recipeList.getSuccessRate()))
		{
			msgtoemployer = new SystemMessage(SystemMessage.S1_HAS_FAILED_TO_CREATE_S2_AT_THE_PRICE_OF_S3_ADENA);
			msgtoemployer.addString(player.getName());
			msgtoemployer.addItemName(recipeList.getItemId());
			msgtoemployer.addNumber(price);
			msgtomaster = new SystemMessage(SystemMessage.THE_ATTEMPT_TO_CREATE_S2_FOR_S1_AT_THE_PRICE_OF_S3_ADENA_HAS_FAILED);
			msgtomaster.addString(employer.getName());
			msgtomaster.addItemName(recipeList.getItemId());
			msgtomaster.addNumber(price);
			player.sendPacket(msgtomaster);
			employer.sendPacket(msgtoemployer);
		}
		else
		{
			L2ItemInstance createdItem = ItemTable.getInstance().createItem(recipeList.getFoundation() > 0 && Rnd.chance(5) ? recipeList.getFoundation() : recipeList.getItemId());
			createdItem.setCount(recipeList.getCount());

			employer.getInventory().addItem(createdItem);
			if(doubl)
				employer.getInventory().addItem(ItemTable.getInstance().createItem(recipeList.getFoundation() > 0 && Rnd.chance(5) ? recipeList.getFoundation() : recipeList.getItemId()));

			if(recipeList.getCount() > 1 || doubl)
			{
				msgtoemployer = new SystemMessage(SystemMessage.S1_CREATED_S2_S3_AT_THE_PRICE_OF_S4_ADENA);
				msgtoemployer.addString(player.getName());
				msgtoemployer.addNumber(recipeList.getCount() + (doubl ? 1 : 0));
				msgtoemployer.addItemName(recipeList.getItemId());
				msgtoemployer.addNumber(price);
				msgtomaster = new SystemMessage(SystemMessage.S2_S3_HAVE_BEEN_SOLD_TO_S1_FOR_S4_ADENA);
				msgtomaster.addString(employer.getName());
				msgtomaster.addNumber(recipeList.getCount() + (doubl ? 1 : 0));
				msgtomaster.addItemName(recipeList.getItemId());
				msgtomaster.addNumber(price);
				player.sendPacket(msgtomaster);
				employer.sendPacket(msgtoemployer);
			}
			else
			{
				msgtoemployer = new SystemMessage(SystemMessage.S1_CREATED_S2_AFTER_RECEIVING_S3_ADENA);
				msgtoemployer.addString(player.getName());
				msgtoemployer.addItemName(recipeList.getItemId());
				msgtoemployer.addNumber(price);
				msgtomaster = new SystemMessage(SystemMessage.S2_IS_SOLD_TO_S1_AT_THE_PRICE_OF_S3_ADENA);
				msgtomaster.addString(employer.getName());
				msgtomaster.addItemName(recipeList.getItemId());
				msgtomaster.addNumber(price);
				player.sendPacket(msgtomaster);
				employer.sendPacket(msgtoemployer);
			}

			success++;
		}

		if(Config.ALT_GAME_EXP_FOR_CRAFT)
			player.addExpAndSp((long) (recipeList.getExp() * Config.RATE_XP), (long) (recipeList.getSp() * Config.RATE_SP), true, false);
		player.sendPacket(new StatusUpdate(player.getObjectId()).addAttribute(StatusUpdate.CUR_MP, (int) player.getCurrentMp()));
		employer.sendChanges();
		employer.sendPacket(new StatusUpdate(player.getObjectId()).addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad()));
		employer.sendPacket(new RecipeShopItemInfo(player.getObjectId(), recipeListId, success));
	}
}
