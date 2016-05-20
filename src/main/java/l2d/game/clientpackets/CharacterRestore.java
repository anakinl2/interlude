package l2d.game.clientpackets;

import l2d.game.serverpackets.CharacterSelectionInfo;

/**
 * [C] 62 CharacterRestore
 * <b>Format:</b> cd 
 * @author Felixx
 *
 */
public class CharacterRestore extends L2GameClientPacket
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
		try
		{
			getClient().markRestoredChar(_charSlot);
		}
		catch(Exception e)
		{}

		CharacterSelectionInfo cl = new CharacterSelectionInfo(getClient().getLoginName(), getClient().getSessionId().playOkID1, 0);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}
}