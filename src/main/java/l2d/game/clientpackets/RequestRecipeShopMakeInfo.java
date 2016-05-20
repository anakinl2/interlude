package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.serverpackets.RecipeShopItemInfo;

/**
 * cdd
 */
public class RequestRecipeShopMakeInfo extends L2GameClientPacket
{
	private int _playerObjectId;
	private int _recipeId;

	@Override
	public void readImpl()
	{
		_playerObjectId = readD();
		_recipeId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.getDuel() != null)
		{
			activeChar.sendActionFailed();
			return;
		}

		activeChar.sendPacket(new RecipeShopItemInfo(_playerObjectId, _recipeId, 0xFFFFFFFF));
	}
}