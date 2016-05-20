package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Fishing;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.L2Weapon;
import l2d.game.templates.StatsSet;

public class ReelingPumping extends L2Skill
{
	private static final int PUMPING = 1;
	private static final int REELING = 2;
	private final int _fishSkillType;

	public ReelingPumping(StatsSet set)
	{
		super(set);
		_fishSkillType = _skillType == SkillType.PUMPING ? PUMPING : REELING;
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!((L2Player) activeChar).isFishing())
		{
			if(_fishSkillType == PUMPING)
				activeChar.sendPacket(new SystemMessage(SystemMessage.PUMPING_SKILL_IS_AVAILABLE_ONLY_WHILE_FISHING));
			else
				activeChar.sendPacket(new SystemMessage(SystemMessage.REELING_SKILL_IS_AVAILABLE_ONLY_WHILE_FISHING));
			activeChar.sendActionFailed();
			return false;
		}
		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character caster, FastList<L2Character> targets)
	{
		if(caster == null || !caster.isPlayer())
			return;

		L2Player player = (L2Player) caster;
		L2Fishing fish = player.getFishCombat();
		L2Weapon weaponItem = player.getActiveWeaponItem();
		int SS = player.getChargedFishShot() ? 2 : 1;
		int pen = 0;
		double gradebonus = 1 + weaponItem.getCrystalType().ordinal() * 0.1;
		int dmg = (int) (getPower() * gradebonus * SS);

		if(player.getSkillLevel(1315) <= getLevel() - 2) // 1315 - Fish Expertise
		{
			// Penalty
			player.sendPacket(new SystemMessage(SystemMessage.SINCE_THE_SKILL_LEVEL_OF_REELING_PUMPING_IS_HIGHER_THAN_THE_LEVEL_OF_YOUR_FISHING_MASTERY_A_PENALTY_OF_S1_WILL_BE_APPLIED));
			pen = 50;
			int penatlydmg = dmg - pen;
			if(player.isGM())
				player.sendMessage("Dmg w/o penalty = " + dmg);
			dmg = penatlydmg;
		}

		if(SS == 2)
			player.unChargeFishShot();

		if(fish != null)
			if(getSkillType() == SkillType.REELING)
				fish.UseRealing(dmg, pen);
			else
				fish.UsePomping(dmg, pen);
	}
}