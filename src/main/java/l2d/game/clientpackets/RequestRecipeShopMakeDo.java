package l2d.game.clientpackets;

import l2d.game.RecipeController;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;

public class RequestRecipeShopMakeDo extends L2GameClientPacket
{
	private int _id;
	private int _recipeId;
	@SuppressWarnings("unused")
	private long _unknow;

	/**
	 * packet type id 0xBF
	 * format:		cddd
	 * format:		cddQ - Gracia Final
	 */
	@Override
	public void readImpl()
	{
		_id = readD();
		_recipeId = readD();
		_unknow = readD();
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

		L2Player manufacturer = (L2Player) activeChar.getVisibleObject(_id);
		if(manufacturer == null || manufacturer.getPrivateStoreType() != L2Player.STORE_PRIVATE_MANUFACTURE || manufacturer.getDistance(activeChar) > L2Character.INTERACTION_DISTANCE)
			return;

		RecipeController.getInstance().requestManufactureItem(manufacturer, activeChar, _recipeId);
	}
}