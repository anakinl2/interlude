package l2d.game.serverpackets;

import l2d.game.model.L2Object;
import l2d.game.model.L2Player;

/**
 * dddddQ
 */
public class RecipeShopItemInfo extends L2GameServerPacket
{
	private int _recipeId, _shopId, curMp, maxMp;
	private int _success = 0xFFFFFFFF;
	private boolean can_writeImpl = false;

	public RecipeShopItemInfo(int shopId, int recipeId, int success)
	{
		_recipeId = recipeId;
		_shopId = shopId;
		_success = success;
	}

	@Override
	final public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2Object manufacturer = activeChar.getVisibleObject(_shopId);

		if(manufacturer == null)
			return;

		if(!manufacturer.isPlayer())
			return;

		curMp = (int) ((L2Player) manufacturer).getCurrentMp();
		maxMp = ((L2Player) manufacturer).getMaxMp();
		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0xda);
		writeD(_shopId);
		writeD(_recipeId);
		writeD(curMp);
		writeD(maxMp);
		writeD(_success);
	}
}