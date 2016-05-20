package items;

import java.util.Collection;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.RecipeController;
import com.lineage.game.cache.Msg;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Recipe;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.RecipeBookItemList;
import com.lineage.game.serverpackets.SystemMessage;

public class Recipes implements IItemHandler, ScriptFile
{
	private static int[] _itemIds = null;

	public Recipes()
	{
		Collection<L2Recipe> rc = RecipeController.getInstance().getRecipes();
		_itemIds = new int[rc.size()];
		int i = 0;
		for(L2Recipe r : rc)
			_itemIds[i++] = r.getRecipeId();
	}

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		if(item == null || item.getIntegerLimitedCount() < 1)
		{
			player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
			return;
		}

		L2Recipe rp = RecipeController.getInstance().getRecipeByRecipeItem(item.getItemId());
		if(rp.isDwarvenRecipe())
		{
			if(player.getDwarvenRecipeLimit() > 0)
			{
				if(player.getDwarvenRecipeBook().size() >= player.getDwarvenRecipeLimit())
				{
					player.sendPacket(Msg.NO_FURTHER_RECIPES_MAY_BE_REGISTERED);
					return;
				}

				if(rp.getLevel() > player.getSkillLevel(L2Skill.SKILL_CRAFTING))
				{
					player.sendPacket(Msg.CREATE_ITEM_LEVEL_IS_TOO_LOW_TO_REGISTER_THIS_RECIPE);
					return;
				}
				if(player.findRecipe(rp))
				{
					player.sendPacket(Msg.THAT_RECIPE_IS_ALREADY_REGISTERED);
					return;
				}
				// add recipe to recipebook
				player.registerRecipe(rp, true);
				player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_ADDED).addString(item.getItem().getName()));
				player.getInventory().destroyItem(item, 1, true);
				RecipeBookItemList response = new RecipeBookItemList(rp.isDwarvenRecipe(), (int) player.getCurrentMp());
				response.setRecipes(player.getDwarvenRecipeBook());
				player.sendPacket(response);
			}
			else
				player.sendPacket(Msg.YOU_ARE_NOT_AUTHORIZED_TO_REGISTER_A_RECIPE);
			return;
		}

		if(player.getCommonRecipeLimit() > 0)
		{
			if(player.getCommonRecipeBook().size() >= player.getCommonRecipeLimit())
			{
				player.sendPacket(Msg.NO_FURTHER_RECIPES_MAY_BE_REGISTERED);
				return;
			}
			if(player.findRecipe(rp))
			{
				player.sendPacket(Msg.THAT_RECIPE_IS_ALREADY_REGISTERED);
				return;
			}
			player.registerRecipe(rp, true);
			player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_ADDED).addString(item.getItem().getName()));
			player.getInventory().destroyItem(item, 1, true);
			RecipeBookItemList response = new RecipeBookItemList(rp.isDwarvenRecipe(), (int) player.getCurrentMp());
			response.setRecipes(player.getDwarvenRecipeBook());
			player.sendPacket(response);
		}
		else
			player.sendPacket(Msg.YOU_ARE_NOT_AUTHORIZED_TO_REGISTER_A_RECIPE);
	}

	public int[] getItemIds()
	{
		return _itemIds;
	}

	public void onLoad()
	{
		ItemHandler.getInstance().registerItemHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}