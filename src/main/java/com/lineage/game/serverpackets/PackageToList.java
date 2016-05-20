package com.lineage.game.serverpackets;

import java.util.HashMap;

import com.lineage.game.model.L2Player;

/**
 * Format: (c) d[dS]
 * d: list size
 * [
 *   d: char ID
 *   S: char Name
 * ]
 *
 * Пример с оффа:
 * C2 02 00 00 00 D0 33 08 00 43 00 4B 00 4A 00 49 00 41 00 44 00 75 00 4B 00 00 00 D0 A7 09 00 53 00 65 00 6B 00 61 00 73 00 00 00
 */
public class PackageToList extends L2GameServerPacket
{
	private HashMap<Integer, String> characters = null;

	@Override
	final public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		characters = activeChar.getAccountChars();
		// No other chars in the account of this player
		if(characters.size() < 1)
		{
			characters = null;
			activeChar.sendPacket(new SystemMessage(SystemMessage.THAT_CHARACTER_DOES_NOT_EXIST));
			return;
		}
	}

	@Override
	protected final void writeImpl()
	{
		if(characters == null)
			return;

		writeC(0xC2);
		writeD(characters.size());
		for(Integer char_id : characters.keySet())
		{
			writeD(char_id); // Character object id
			writeS(characters.get(char_id)); // Character name
		}
	}
}