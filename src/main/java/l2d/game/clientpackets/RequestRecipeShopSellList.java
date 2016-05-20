package l2d.game.clientpackets;

import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.serverpackets.RecipeShopSellList;

/**
 * Возврат к списку из информации о рецепте
 */
public class RequestRecipeShopSellList extends L2GameClientPacket
{
	int _objectId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2Object trader = L2World.findObject(_objectId);
		if(trader != null && trader.isPlayer())
			activeChar.sendPacket(new RecipeShopSellList(activeChar, (L2Player) trader));
		else
			activeChar.sendActionFailed();
	}
}