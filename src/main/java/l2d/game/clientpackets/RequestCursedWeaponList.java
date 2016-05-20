package l2d.game.clientpackets;

import java.util.List;

import javolution.util.FastList;
import l2d.game.instancemanager.CursedWeaponsManager;
import l2d.game.model.L2Character;
import l2d.game.serverpackets.ExCursedWeaponList;

public class RequestCursedWeaponList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Character activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		List<Integer> list = new FastList<Integer>();
		for(int id : CursedWeaponsManager.getInstance().getCursedWeaponsIds())
			list.add(id);

		activeChar.sendPacket(new ExCursedWeaponList(list));
	}
}