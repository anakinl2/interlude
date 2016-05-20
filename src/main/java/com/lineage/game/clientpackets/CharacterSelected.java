package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.network.L2GameClient;
import com.lineage.game.network.L2GameClient.GameClientState;
import com.lineage.game.serverpackets.CharSelected;
import com.lineage.util.AutoBan;

/**
 * [C] 0D CharacterSelect
 * <b>Format:</b> cdhddd 
 * @author Felixx
 */
public class CharacterSelected extends L2GameClientPacket
{
	private int _charSlot;

	@Override
	public void readImpl()
	{
		_charSlot = readD();
	}

	@Override
	public void runImpl()
	{
		L2GameClient client = getClient();

		if(client.getActiveChar() != null)
			return;

		L2Player activeChar = client.loadCharFromDisk(_charSlot);
		if(activeChar == null)
			return;

		if(AutoBan.isBanned(activeChar.getObjectId()))
		{
			activeChar.setAccessLevel(-100);
			activeChar.logout(false, false, true);
			return;
		}

		if(activeChar.getAccessLevel() < 0)
			activeChar.setAccessLevel(0);

		client.setState(GameClientState.IN_GAME);

		sendPacket(new CharSelected(activeChar, client.getSessionId().playOkID1));
	}
}