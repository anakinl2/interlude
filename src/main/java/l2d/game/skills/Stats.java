package l2d.game.skills;

import java.util.NoSuchElementException;

import l2d.Config;
import l2d.game.skills.inits.InitConst;
import l2d.game.skills.inits.InitFunc;
import l2d.game.skills.inits.Init_rShld;

public enum Stats
{
	MAX_HP("maxHp", 1, Config.MAX_HP, true), //
	MAX_MP("maxMp", 1, Config.MAX_MP, true), //
	MAX_CP("maxCp", 1, Config.MAX_CP, true), //
	REGENERATE_HP_RATE("regHp", null, null, false), //
	REGENERATE_CP_RATE("regCp", null, null, false), //
	REGENERATE_MP_RATE("regMp", null, null, false), //

	RUN_SPEED("runSpd", 0, Config.MAX_RUNSPD, true), //

	POWER_DEFENCE("pDef", 0, Config.MAX_PDEF, true), //
	MAGIC_DEFENCE("mDef", 0, Config.MAX_MDEF, true), //
	POWER_ATTACK("pAtk", 0, Config.MAX_PATK, true), //
	MAGIC_ATTACK("mAtk", 0, Config.MAX_MATK, true), //
	POWER_ATTACK_SPEED("pAtkSpd", 0, Config.MAX_PATKSPD, false), //
	MAGIC_ATTACK_SPEED("mAtkSpd", 0, Config.MAX_MATKSPD, false), //

	MAGIC_REUSE_RATE("mReuse", null, null, false), //
	PHYSIC_REUSE_RATE("pReuse", null, null, false), //
	ATK_REUSE("atkReuse", null, null, false), //
	ATK_BASE("atkBaseSpeed", null, null, false), //

	CRITICAL_DAMAGE("cAtk", 0, 500, false, new InitConst(100)), //
	CRITICAL_DAMAGE_STATIC("cAtkStatic", null, null, false, new InitConst(0)), //
	EVASION_RATE("rEvas", 0, Config.MAX_EVAS_RATE, false), //
	ACCURACY_COMBAT("accCombat", 0, Config.MAX_ACC_COM, false), //
	CRITICAL_BASE("baseCrit", 0, Config.MAX_CRIT_BASE, false, new InitConst(100)), //
	CRITICAL_RATE("rCrit", 0, null, false, new InitConst(100)), //
	MCRITICAL_RATE("mCritRate", 0, Config.MAX_MCRIT_RATE, false, new InitConst(10.)), //

	PHYSICAL_DAMAGE("physDamage", null, null, false, null), //
	MAGIC_DAMAGE("magicDamage", null, null, false, null), //

	CAST_INTERRUPT("concentration", 0, 100, false, null), //

	SHIELD_DEFENCE("sDef", null, null, false), //
	SHIELD_RATE("rShld", 0, 90, false, new Init_rShld()), //
	SHIELD_ANGLE("shldAngle", null, null, false, new InitConst(60)), //

	POWER_ATTACK_RANGE("pAtkRange", 0, 1500, false), //
	MAGIC_ATTACK_RANGE("mAtkRange", 0, 1500, false), //
	POLE_ATTACK_ANGLE("poleAngle", 0, 180, false), //
	POLE_TARGERT_COUNT("poleTargetCount", null, null, false), //

	STAT_STR("STR", 1, 99, false), //
	STAT_CON("CON", 1, 99, false), //
	STAT_DEX("DEX", 1, 99, false), //
	STAT_INT("INT", 1, 99, false), //
	STAT_WIT("WIT", 1, 99, false), //
	STAT_MEN("MEN", 1, 99, false), //

	BREATH("breath", null, null, false), //
	FALL("fall", null, null, false), //
	EXP_LOST("expLost", null, null, false), //

	BLEED_RECEPTIVE("bleedRcpt", 10, 190, false), //
	POISON_RECEPTIVE("poisonRcpt", 10, 190, false), //
	STUN_RECEPTIVE("stunRcpt", 10, 190, false), //
	ROOT_RECEPTIVE("rootRcpt", 10, 190, false), //
	MENTAL_RECEPTIVE("mentalRcpt", 10, 190, false), //
	SLEEP_RECEPTIVE("sleepRcpt", 10, 190, false), //
	PARALYZE_RECEPTIVE("paralyzeRcpt", 10, 190, false), //
	SILENCE_RECEPTIVE("silenceRcpt", 10, 190, false), //
	CANCEL_RECEPTIVE("cancelRcpt", 10, 190, false), //
	DEBUFF_RECEPTIVE("debuffRcpt", 10, 190, false), //
	DEATH_RECEPTIVE("deathRcpt", 10, 190, false), //

	BLEED_POWER("bleedPower", 10, 190, false), //
	POISON_POWER("poisonPower", 10, 190, false), //
	STUN_POWER("stunPower", 10, 190, false), //
	ROOT_POWER("rootPower", 10, 190, false), //
	MENTAL_POWER("mentalPower", 10, 190, false), //
	SLEEP_POWER("sleepPower", 10, 190, false), //
	PARALYZE_POWER("paralyzePower", 10, 190, false), //
	SILENCE_POWER("silencePower", 10, 190, false), //
	CANCEL_POWER("cancelPower", 10, 190, false), //
	DEBUFF_POWER("debuffPower", 10, 190, false), //
	FATALBLOW_RATE("blowRate", 10, 190, false), //

	FIRE_RECEPTIVE("fireRcpt", null, null, false), //
	WIND_RECEPTIVE("windRcpt", null, null, false), //
	WATER_RECEPTIVE("waterRcpt", null, null, false), //
	EARTH_RECEPTIVE("earthRcpt", null, null, false), //
	UNHOLY_RECEPTIVE("unholyRcpt", null, null, false), //
	SACRED_RECEPTIVE("sacredRcpt", null, null, false), //

	CRIT_DAMAGE_RECEPTIVE("critDamRcpt", 10, 190, false, new InitConst(100)), //
	CRIT_CHANCE_RECEPTIVE("critChanceRcpt", 10, 190, false, new InitConst(100)), //

	ATTACK_ELEMENT_FIRE("attackFire", null, null, false), //
	ATTACK_ELEMENT_WATER("attackWater", null, null, false), //
	ATTACK_ELEMENT_WIND("attackWind", null, null, false), //
	ATTACK_ELEMENT_EARTH("attackEarth", null, null, false), //
	ATTACK_ELEMENT_SACRED("attackSacred", null, null, false), //
	ATTACK_ELEMENT_UNHOLY("attackUnholy", null, null, false), //

	SWORD_WPN_RECEPTIVE("swordWpnRcpt", 10, 190, false), //
	DUAL_WPN_RECEPTIVE("dualWpnRcpt", 10, 190, false), //
	BLUNT_WPN_RECEPTIVE("bluntWpnRcpt", 10, 190, false), //
	DAGGER_WPN_RECEPTIVE("daggerWpnRcpt", 10, 190, false), //
	BOW_WPN_RECEPTIVE("bowWpnRcpt", 10, 190, false), //
	POLE_WPN_RECEPTIVE("poleWpnRcpt", 10, 190, false), //
	FIST_WPN_RECEPTIVE("fistWpnRcpt", 10, 190, false), //

	ABSORB_DAMAGE_PERCENT("absorbDam", 0, 100, false), //
	ABSORB_DAMAGE_ENEMY_PERCENT("absorbEnemyDam", 0, 100, false), //
	ABSORB_DAMAGEMP_PERCENT("absorbDamMp", 0, 100, false), //

	TRANSFER_DAMAGE_PERCENT("transferDam", 0, 100, false), //

	REFLECT_DAMAGE_PERCENT("reflectDam", 0, 100, false), //

	REFLECT_PHYSIC_SKILL("reflectPhysicSkill", 0, 100, false), //
	REVENGE_ON_PHYSIC("revengeOnPhysic", 0, 100, false), // Allseron: Modification fixing Shield of Revenge.
	REFLECT_MAGIC_SKILL("reflectMagicSkill", 0, 100, false), //

	REFLECT_PHYSIC_DEBUFF("reflectPhysicDebuff", 0, 100, false), //
	REFLECT_MAGIC_DEBUFF("reflectMagicDebuff", 0, 100, false), //

	PSKILL_EVASION("pSkillEvasion", 0, 100, false), //
	MSKILL_EVASION("mSkillEvasion", 0, 100, false), //

	COUNTER_ATTACK("counterAttack", 0, 100, false), //

	CANCEL_TARGET("cancelTarget", 0, 100, false), //

	HEAL_EFFECTIVNESS("hpEff", 0, 1000, false), //
	HEAL_POWER("healPower", 0, 1000, false), //
	HEAL_POWER_ADD_STATIC("healPowerAdd", 0, 1000, false), //
	MANAHEAL_EFFECTIVNESS("mpEff", 0, 1000, false), //
	MP_MAGIC_SKILL_CONSUME("mpConsum", null, null, false), //
	MP_PHYSICAL_SKILL_CONSUME("mpConsumePhysical", null, null, false), //
	MP_DANCE_SKILL_CONSUME("mpDanceConsume", null, null, false), //
	MP_USE_BOW("cheapShot", null, null, false), //
	MP_USE_BOW_CHANCE("cheapShotChance", null, null, false), //
	SS_USE_BOW("miser", null, null, false), //
	SS_USE_BOW_CHANCE("miserChance", null, null, false), //
	ACTIVATE_RATE("activateRate", null, null, false), //

	SKILL_MASTERY("skillMastery", 0, null, false), //
	EXP("ExpMultiplier", 0, null, false), //
	SP("SpMultiplier", 0, null, false), //
	CPHEAL_EFFECTIVNESS("cpEff", 0, 1000, false), //
	GRADE_EXPERTISE_LEVEL("gradeExpertiseLevel", null, null, false), //

	MAX_LOAD("maxLoad", null, null, false), //
	MAX_NO_PENALTY_LOAD("maxNoPenaltyLoad", null, null, false), //
	INVENTORY_LIMIT("inventoryLimit", null, Config.SERVICES_EXPAND_INVENTORY_MAX, false), //
	STORAGE_LIMIT("storageLimit", null, null, false), //
	TRADE_LIMIT("tradeLimit", null, null, false), //
	COMMON_RECIPE_LIMIT("CommonRecipeLimit", null, null, false), //
	DWARVEN_RECIPE_LIMIT("DwarvenRecipeLimit", null, null, false), //
	BUFF_LIMIT("buffLimit", null, null, false), //
	DANCE_SONG_LIMIT("DanceSongsLimit", null, null, false), //
	SOULS_LIMIT("soulsLimit", null, null, false), //
	SOULS_CONSUME_EXP("soulsExp", null, null, false), //
	TALISMANS_LIMIT("talismansLimit", 0, 6, false), //

	EXP_SP("ExpSpMultiplier", null, null, false), //
	DROP("DropMultiplier", null, null, false); //

	public static final int NUM_STATS = values().length;

	private String _value;
	public final Integer _min;
	public final Integer _max;
	private boolean _limitOnlyPlayable;
	private InitFunc _init;

	public String getValue()
	{
		return _value;
	}

	public boolean isLimitOnlyPlayable()
	{
		return _limitOnlyPlayable;
	}

	public InitFunc getInit()
	{
		return _init;
	}

	private Stats(final String s, final Integer min, final Integer max, final boolean limitOnlyPlayable)
	{
		_value = s;
		_min = min;
		_max = max;
		_limitOnlyPlayable = limitOnlyPlayable;
		_init = null;
	}

	private Stats(final String s, final Integer min, final Integer max, final boolean limitOnlyPlayable, final InitFunc init)
	{
		_value = s;
		_min = min;
		_max = max;
		_limitOnlyPlayable = limitOnlyPlayable;
		_init = init;
	}

	public static Stats valueOfXml(final String name)
	{
		for(final Stats s : values())
			if(s.getValue().equals(name))
				return s;

		throw new NoSuchElementException("Unknown name '" + name + "' for enum BaseStats");
	}

	@Override
	public String toString()
	{
		return _value;
	}
}