package com.lineage.game.clientpackets;

import com.lineage.game.instancemanager.SiegeManager;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Effect.EffectType;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.residence.Castle;
import com.lineage.game.model.entity.residence.ClanHall;
import com.lineage.game.model.entity.residence.ResidenceFunction;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.entity.siege.SiegeClan;
import com.lineage.game.serverpackets.Die;
import com.lineage.game.serverpackets.Revive;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.MapRegion;
import com.lineage.util.Location;

public class RequestRestartPoint extends L2GameClientPacket
{
	protected int _requestedPointType;
	protected boolean _continuation;

	private static final int TO_VILLAGE = 0;
	private static final int TO_CLANHALL = 1;
	private static final int TO_CASTLE = 2;
	private static final int TO_SIEGEHQ = 3;
	private static final int FIXED = 4;

	/**
	 * packet type id 0x7D
	 * format:    cd
	 */
	@Override
	public void readImpl()
	{
		_requestedPointType = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		if(activeChar.getTeam() > 0)
		{
			activeChar.sendMessage("Please wait till event ends!");
			return;
		}

		if(activeChar.isFakeDeath())
		{
			activeChar.getEffectList().stopEffects(EffectType.FakeDeath);
			activeChar.broadcastPacket(new Revive(activeChar));
			return;
		}

		if(!activeChar.isDead() && !activeChar.getPlayerAccess().IsGM)
		{
			activeChar.sendActionFailed();
			return;
		}

		try
		{
			if(activeChar.isFestivalParticipant())
			{
				activeChar.setIsPendingRevive(true);
				activeChar.teleToLocation(activeChar.getLoc());
				return;
			}

			Location loc = null;

			boolean isInDefense = false;
			L2Clan clan = activeChar.getClan();
			Siege siege = SiegeManager.getSiege(activeChar, true);

			switch(_requestedPointType)
			{
				case TO_CLANHALL:
					if(clan == null || clan.getHasHideout() == 0)
						loc = MapRegion.getTeleToClosestTown(activeChar);
					else
					{
						ClanHall clanHall = activeChar.getClanHall();
						loc = MapRegion.getTeleToClanHall(activeChar);
						if(clanHall.getFunction(ResidenceFunction.RESTORE_EXP) != null)
							activeChar.restoreExp(clanHall.getFunction(ResidenceFunction.RESTORE_EXP).getLevel());
					}
					break;
				case TO_CASTLE:
					isInDefense = false;
					if(siege != null && siege.isInProgress() && siege.checkIsDefender(activeChar.getClan()))
						isInDefense = true;
					if((clan == null || clan.getHasCastle() == 0) && !isInDefense)
					{
						activeChar.sendActionFailed();
						return;
					}
					Castle castle = activeChar.getCastle();
					loc = MapRegion.getTeleToCastle(activeChar);
					if(castle.getFunction(ResidenceFunction.RESTORE_EXP) != null)
						activeChar.restoreExp(castle.getFunction(ResidenceFunction.RESTORE_EXP).getLevel());
					break;
				case TO_SIEGEHQ:
					SiegeClan siegeClan = null;
					if(siege != null && siege.isInProgress())
						siegeClan = siege.getAttackerClan(activeChar.getClan());
					if(siegeClan == null || siegeClan.getHeadquarter() == null)
					{
						sendPacket(new SystemMessage(SystemMessage.IF_A_BASE_CAMP_DOES_NOT_EXIST_RESURRECTION_IS_NOT_POSSIBLE));
						sendPacket(new Die(activeChar));
						return;
					}
					loc = MapRegion.getTeleToHeadquarter(activeChar);
					break;
				case FIXED:
					if(!activeChar.getPlayerAccess().ResurectFixed)
					{
						activeChar.sendActionFailed();
						return;
					}
					loc = activeChar.getLoc();
					break;
				case TO_VILLAGE:
				default:
					loc = MapRegion.getTeleToClosestTown(activeChar);
					break;
			}

			activeChar.setIsPendingRevive(true);
			activeChar.teleToLocation(loc);
		}
		catch(Throwable e)
		{}
	}
}