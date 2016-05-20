package com.lineage.game.clientpackets;

import java.util.List;

import com.lineage.game.instancemanager.CursedWeaponsManager;
import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.serverpackets.ExCursedWeaponList;

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