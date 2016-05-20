package l2d.game.clientpackets;

import java.util.List;

import javolution.util.FastList;
import l2d.game.instancemanager.CursedWeaponsManager;
import l2d.game.model.CursedWeapon;
import l2d.game.model.L2Character;
import l2d.game.serverpackets.ExCursedWeaponLocation;
import l2d.game.serverpackets.ExCursedWeaponLocation.CursedWeaponInfo;
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