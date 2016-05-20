package l2d.game.instancemanager;

import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.entity.residence.Castle;
import l2d.game.model.entity.residence.ClanHall;
import l2d.game.model.entity.residence.Residence;
import l2d.game.model.entity.siege.Siege;
import l2d.game.tables.SkillTable;

public abstract class SiegeManager
{
	public static void addSiegeSkills(L2Player character)
	{
		character.addSkill(SkillTable.getInstance().getInfo(246, 1), false);
		character.addSkill(SkillTable.getInstance().getInfo(247, 1), false);
		if(character.isNoble())
			character.addSkill(SkillTable.getInstance().getInfo(326, 1), false);
	}

	public static void removeSiegeSkills(L2Player character)
	{
		character.removeSkill(SkillTable.getInstance().getInfo(246, 1), false);
		character.removeSkill(SkillTable.getInstance().getInfo(247, 1), false);
		character.removeSkill(SkillTable.getInstance().getInfo(326, 1), false);
	}

	public static boolean getCanRide()
	{
		for(Castle castle : CastleManager.getInstance().getCastles().values())
			if(castle != null && castle.getSiege().isInProgress())
				return false;
		for(ClanHall clanhall : ClanHallManager.getInstance().getClanHalls().values())
			if(clanhall != null && clanhall.getSiege() != null && clanhall.getSiege().isInProgress())
				return false;
		return true;
	}

	public static Residence getSiegeUnitByObject(L2Object activeObject)
	{
		return getSiegeUnitByCoord(activeObject.getX(), activeObject.getY());
	}

	public static Residence getSiegeUnitByCoord(int x, int y)
	{
		for(Castle castle : CastleManager.getInstance().getCastles().values())
			if(castle.checkIfInZone(x, y))
				return castle;
		return null;
	}

	public static Siege getSiege(L2Object activeObject, boolean onlyActive)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), onlyActive);
	}

	public static Siege getSiege(int x, int y, boolean onlyActive)
	{
		for(Castle castle : CastleManager.getInstance().getCastles().values())
			if(castle.getSiege().checkIfInZone(x, y, onlyActive))
				return castle.getSiege();
		for(ClanHall clanhall : ClanHallManager.getInstance().getClanHalls().values())
			if(clanhall.getSiege() != null && clanhall.getSiege().checkIfInZone(x, y, onlyActive))
				return clanhall.getSiege();
		return null;
	}
}