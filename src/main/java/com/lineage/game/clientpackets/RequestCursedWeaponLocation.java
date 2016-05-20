package com.lineage.game.clientpackets;

import java.util.List;

import com.lineage.game.instancemanager.CursedWeaponsManager;
import javolution.util.FastList;
import com.lineage.game.model.CursedWeapon;
import com.lineage.game.model.L2Character;
import com.lineage.game.serverpackets.ExCursedWeaponLocation;
import com.lineage.game.serverpackets.ExCursedWeaponLocation.CursedWeaponInfo;
import com.lineage.util.Location;

public class RequestCursedWeaponLocation extends L2GameClientPacket
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

		List<CursedWeaponInfo> list = new FastList<CursedWeaponInfo>();
		for(CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons())
		{
			Location pos = cw.getWorldPosition();
			if(pos != null)
				list.add(new CursedWeaponInfo(pos, cw.getItemId(), cw.isActivated() ? 1 : 0));
		}

		activeChar.sendPacket(new ExCursedWeaponLocation(list));
	}
}