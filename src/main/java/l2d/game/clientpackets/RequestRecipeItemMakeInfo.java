package l2d.game.clientpackets;

import l2d.game.serverpackets.RecipeItemMakeInfo;

public class RequestRecipeItemMakeInfo extends L2GameClientPacket
{
	private int _id;

	/**
	 * packet type id 0xB7
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		_id = readD();
	}

	@Override
	public void runImpl()
	{
		sendPacket(new RecipeItemMakeInfo(_id, getClient().getActiveChar(), 0xffffffff));
	}
}