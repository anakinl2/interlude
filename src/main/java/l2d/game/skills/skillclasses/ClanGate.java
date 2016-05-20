package l2d.game.skills.skillclasses;

import static l2d.game.model.L2Zone.ZoneType.Siege;
import static l2d.game.model.L2Zone.ZoneType.no_restart;
import javolution.util.FastList;
import l2d.game.instancemanager.DimensionalRiftManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Clan;
import l2d.game.model.L2ClanGate;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2World;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.StatsSet;
import l2d.util.GArray;

public class ClanGate extends L2Skill
{

	public ClanGate(StatsSet set)
	{
		super(set);
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(activeChar.isPlayer())
		{
			// "Нельзя вызывать персонажей в/из зоны свободного PvP"
			// "в зоны осад"
			// "на Олимпийский стадион"
			// "в зоны определенных рейд-боссов и эпик-боссов"
			if(activeChar.isInZoneBattle() || activeChar.isInZone(Siege) || activeChar.isInZoneIncludeZ(no_restart))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
				return false;
			}

			// Нельзя саммонить в зону рифта
			if(DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getLoc(), false))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
				return false;
			}

			L2Player activePlayer = (L2Player) activeChar;

			// нельзя сумонить в тюрьму)
			if(activePlayer.isInJail())
			{
				activePlayer.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
				return false;
			}

			// нельзя суммонить в режиме боя
			if(activeChar.isInCombat())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_SUMMON_DURING_COMBAT));
				return false;
			}

			// нельзя суммонить, находясь в режиме торговли или в процессе торговли/обмена
			if(activePlayer.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || activePlayer.isTransactionInProgress())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_SUMMON_DURING_A_TRADE_OR_WHILE_USING_THE_PRIVATE_SHOPS));
				return false;
			}

			// проверить наличие боссов в радиусе 5000
			GArray<L2Object> objects = L2World.getAroundObjects(activePlayer, 5000, 500);
			if(objects != null)
				for(L2Object object : objects)
					if(object.isBoss())
					{
						activePlayer.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
						return false;
					}

			// проверить мб клангейт уже создан для клана?
			L2Clan clan = activePlayer.getClan();
			if(clan.getClanGate() != null)
			{
				activePlayer.sendMessage("Clan gate already exists!");
				return false;
			}

			// таргет незачем проверять
		}
		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		if(isSSPossible())
			activeChar.unChargeShots(isMagic());

		if(!activeChar.isPlayer())
			return;

		L2Player activePlayer = (L2Player) activeChar;
		L2Clan clan = activePlayer.getClan();
		if(clan.getClanGate() != null)
		{
			activePlayer.sendMessage("Clan gate already exists!");
			return;
		}
		new L2ClanGate(activePlayer);

		// apply effects (immobileBuff)
		for(L2Character target : targets)
			getEffects(activeChar, target, getActivateRate() > 0, false);
	}
}
