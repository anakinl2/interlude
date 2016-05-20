package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.RecipeShopItemInfo;

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