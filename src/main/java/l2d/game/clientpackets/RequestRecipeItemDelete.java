package l2d.game.clientpackets;

import l2d.game.RecipeController;
import l2d.game.model.L2Player;
import l2d.game.model.L2Recipe;
import l2d.game.serverpackets.RecipeBookItemList;

public class RequestRecipeItemDelete extends L2GameClientPacket
{
	// Format: cd

	private int _RecipeID;

	@Override
	public void readImpl()
	{
		_RecipeID = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2Recipe rp = RecipeController.getInstance().getRecipeByRecipeId(_RecipeID);

		if(rp == null)
		{
			activeChar.sendActionFailed();
			return;
		}

		activeChar.unregisterRecipe(_RecipeID);

		RecipeBookItemList response = new RecipeBookItemList(rp.isDwarvenRecipe(), (int) activeChar.getCurrentMp());

		response.setRecipes(activeChar.getDwarvenRecipeBook());

		activeChar.sendPacket(response);
	}
}