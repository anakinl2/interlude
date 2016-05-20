package l2d.game.serverpackets;

import l2d.game.instancemanager.SiegeManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.entity.siege.Siege;
import l2d.game.model.entity.siege.SiegeClan;
import l2d.game.model.instances.L2MonsterInstance;

/**
 * Пример:
 * 00
 * 8b 22 90 48 objectId
 * 01 00 00 00
 * 00 00 00 00
 * 00 00 00 00
 * 00 00 00 00
 * 00 00 00 00
 * 00 00 00 00
 * 00 00 00 00
 * format  dddddddd   rev 828
 */
public class Die extends L2GameServerPacket
{
	private int _chaId;
	private boolean _fake;
	private boolean _sweepable;
	private int _access;
	private L2Clan _clan;
	private L2Character _cha;
	private int to_hideaway, to_castle, to_siege_HQ;

	/**
	 * @param _characters
	 */
	public Die(L2Character cha)
	{
		_cha = cha;
		if(cha.isPlayer())
		{
			L2Player player = (L2Player) cha;
			_access = player.getPlayerAccess().ResurectFixed ? 0x01 : 0x00;
			_clan = player.getClan();
		}
		_chaId = cha.getObjectId();
		_fake = !cha.isDead();
		if(cha.isMonster())
			_sweepable = ((L2MonsterInstance) cha).isSweepActive();

		if(_clan != null)
		{
			SiegeClan siegeClan = null;
			Siege siege = SiegeManager.getSiege(_cha, true);
			if(siege != null && siege.isInProgress())
				siegeClan = siege.getAttackerClan(_clan);

			to_hideaway = _clan.getHasHideout() > 0 ? 0x01 : 0x00;
			to_castle = _clan.getHasCastle() > 0 ? 0x01 : 0x00;
			to_siege_HQ = siegeClan != null && siegeClan.getHeadquarter() != null ? 0x01 : 0x00;
		}
		else
		{
			to_hideaway = 0;
			to_castle = 0;
			to_siege_HQ = 0;
		}
	}

	@Override
	protected final void writeImpl()
	{
		if(_fake)
			return;

		writeC(0x06);
		writeD(_chaId);
		writeD(0x01); // to nearest village
		writeD(to_hideaway); // to hide away
		writeD(to_castle); // to castle
		writeD(to_siege_HQ); // to siege HQ
		writeD(_sweepable ? 0x01 : 0x00); // sweepable  (blue glow)
		writeD(_access); // FIXED
	}
}