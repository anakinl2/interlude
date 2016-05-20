package com.lineage.game.handler;

import com.lineage.game.model.L2Player;

public interface IUserCommandHandler
{
	/**
	 * this is the worker method that is called when someone uses an admin command.
	 * @param activeChar
	 * @param command
	 * @return command success
	 */
	public boolean useUserCommand(int id, L2Player activeChar);

	/**
	 * this method is called at initialization to register all the item ids automatically
	 * @return all known itemIds
	 */
	public int[] getUserCommandList();
}
