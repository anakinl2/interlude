package com.lineage.game.model.base;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

public enum PlayerClass
{
	HumanFighter(Race.human, ClassType.Fighter, ClassLevel.First),
	//0
	Warrior(Race.human, ClassType.Fighter, ClassLevel.Second),
	//1
	Gladiator(Race.human, ClassType.Fighter, ClassLevel.Third),
	//2
	Warlord(Race.human, ClassType.Fighter, ClassLevel.Third),
	//3
	HumanKnight(Race.human, ClassType.Fighter, ClassLevel.Second),
	//4
	Paladin(Race.human, ClassType.Fighter, ClassLevel.Third),
	//5
	DarkAvenger(Race.human, ClassType.Fighter, ClassLevel.Third),
	//6
	Rogue(Race.human, ClassType.Fighter, ClassLevel.Second),
	//7
	TreasureHunter(Race.human, ClassType.Fighter, ClassLevel.Third),
	//8
	Hawkeye(Race.human, ClassType.Fighter, ClassLevel.Third),
	//9
	HumanMystic(Race.human, ClassType.Mystic, ClassLevel.First),
	//10
	HumanWizard(Race.human, ClassType.Mystic, ClassLevel.Second),
	//11
	Sorceror(Race.human, ClassType.Mystic, ClassLevel.Third),
	//12
	Necromancer(Race.human, ClassType.Mystic, ClassLevel.Third),
	//13
	Warlock(Race.human, ClassType.Mystic, ClassLevel.Third),
	//14
	Cleric(Race.human, ClassType.Priest, ClassLevel.Second),
	//15
	Bishop(Race.human, ClassType.Priest, ClassLevel.Third),
	//16
	Prophet(Race.human, ClassType.Priest, ClassLevel.Third),
	//17
	ElvenFighter(Race.elf, ClassType.Fighter, ClassLevel.First),
	//18
	ElvenKnight(Race.elf, ClassType.Fighter, ClassLevel.Second),
	//19
	TempleKnight(Race.elf, ClassType.Fighter, ClassLevel.Third),
	//20
	Swordsinger(Race.elf, ClassType.Fighter, ClassLevel.Third),
	//21
	ElvenScout(Race.elf, ClassType.Fighter, ClassLevel.Second),
	//22
	Plainswalker(Race.elf, ClassType.Fighter, ClassLevel.Third),
	//23
	SilverRanger(Race.elf, ClassType.Fighter, ClassLevel.Third),
	//24
	ElvenMystic(Race.elf, ClassType.Mystic, ClassLevel.First),
	//25
	ElvenWizard(Race.elf, ClassType.Mystic, ClassLevel.Second),
	//26
	Spellsinger(Race.elf, ClassType.Mystic, ClassLevel.Third),
	//27
	ElementalSummoner(Race.elf, ClassType.Mystic, ClassLevel.Third),
	//28
	ElvenOracle(Race.elf, ClassType.Priest, ClassLevel.Second),
	//29
	ElvenElder(Race.elf, ClassType.Priest, ClassLevel.Third),
	//30
	DarkElvenFighter(Race.darkelf, ClassType.Fighter, ClassLevel.First),
	//31
	PalusKnight(Race.darkelf, ClassType.Fighter, ClassLevel.Second),
	//32
	ShillienKnight(Race.darkelf, ClassType.Fighter, ClassLevel.Third),
	//33
	Bladedancer(Race.darkelf, ClassType.Fighter, ClassLevel.Third),
	//34
	Assassin(Race.darkelf, ClassType.Fighter, ClassLevel.Second),
	//35
	AbyssWalker(Race.darkelf, ClassType.Fighter, ClassLevel.Third),
	//36
	PhantomRanger(Race.darkelf, ClassType.Fighter, ClassLevel.Third),
	//37
	DarkElvenMystic(Race.darkelf, ClassType.Mystic, ClassLevel.First),
	//38
	DarkElvenWizard(Race.darkelf, ClassType.Mystic, ClassLevel.Second),
	//39
	Spellhowler(Race.darkelf, ClassType.Mystic, ClassLevel.Third),
	//40
	PhantomSummoner(Race.darkelf, ClassType.Mystic, ClassLevel.Third),
	//41
	ShillienOracle(Race.darkelf, ClassType.Priest, ClassLevel.Second),
	//42
	ShillienElder(Race.darkelf, ClassType.Priest, ClassLevel.Third),
	//43
	OrcFighter(Race.orc, ClassType.Fighter, ClassLevel.First),
	//44
	orcRaider(Race.orc, ClassType.Fighter, ClassLevel.Second),
	//45
	Destroyer(Race.orc, ClassType.Fighter, ClassLevel.Third),
	//46
	orcMonk(Race.orc, ClassType.Fighter, ClassLevel.Second),
	//47
	Tyrant(Race.orc, ClassType.Fighter, ClassLevel.Third),
	//48
	orcMystic(Race.orc, ClassType.Mystic, ClassLevel.First),
	//49
	orcShaman(Race.orc, ClassType.Mystic, ClassLevel.Second),
	//50
	Overlord(Race.orc, ClassType.Mystic, ClassLevel.Third),
	//51
	Warcryer(Race.orc, ClassType.Mystic, ClassLevel.Third),
	//52
	DwarvenFighter(Race.dwarf, ClassType.Fighter, ClassLevel.First),
	//53
	DwarvenScavenger(Race.dwarf, ClassType.Fighter, ClassLevel.Second),
	//54
	BountyHunter(Race.dwarf, ClassType.Fighter, ClassLevel.Third),
	//55
	DwarvenArtisan(Race.dwarf, ClassType.Fighter, ClassLevel.Second),
	//56
	Warsmith(Race.dwarf, ClassType.Fighter, ClassLevel.Third),
	//57
	DummyEntry1(null, null, null),
	//58
	DummyEntry2(null, null, null),
	//59
	DummyEntry3(null, null, null),
	//60
	DummyEntry4(null, null, null),
	//61
	DummyEntry5(null, null, null),
	//62
	DummyEntry6(null, null, null),
	//63
	DummyEntry7(null, null, null),
	//64
	DummyEntry8(null, null, null),
	//65
	DummyEntry9(null, null, null),
	//66
	DummyEntry10(null, null, null),
	//67
	DummyEntry11(null, null, null),
	//68
	DummyEntry12(null, null, null),
	//69
	DummyEntry13(null, null, null),
	//70
	DummyEntry14(null, null, null),
	//71
	DummyEntry15(null, null, null),
	//72
	DummyEntry16(null, null, null),
	//73
	DummyEntry17(null, null, null),
	//74
	DummyEntry18(null, null, null),
	//75
	DummyEntry19(null, null, null),
	//76
	DummyEntry20(null, null, null),
	//77
	DummyEntry21(null, null, null),
	//78
	DummyEntry22(null, null, null),
	//79
	DummyEntry23(null, null, null),
	//80
	DummyEntry24(null, null, null),
	//81
	DummyEntry25(null, null, null),
	//82
	DummyEntry26(null, null, null),
	//83
	DummyEntry27(null, null, null),
	//84
	DummyEntry28(null, null, null),
	//85
	DummyEntry29(null, null, null),
	//86
	DummyEntry30(null, null, null),
	//87
	Duelist(Race.human, ClassType.Fighter, ClassLevel.Fourth, ClassType.ForceMaster),
	//88
	Dreadnought(Race.human, ClassType.Fighter, ClassLevel.Fourth, ClassType.WeaponMaster),
	//89
	PhoenixKnight(Race.human, ClassType.Fighter, ClassLevel.Fourth, ClassType.ShieldMaster),
	//90
	HellKnight(Race.human, ClassType.Fighter, ClassLevel.Fourth, ClassType.ShieldMaster),
	//91
	Sagittarius(Race.human, ClassType.Fighter, ClassLevel.Fourth, ClassType.BowMaster),
	//92
	Adventurer(Race.human, ClassType.Fighter, ClassLevel.Fourth, ClassType.DaggerMaster),
	//93
	Archmage(Race.human, ClassType.Mystic, ClassLevel.Fourth, ClassType.Wizard),
	//94
	Soultaker(Race.human, ClassType.Mystic, ClassLevel.Fourth, ClassType.Wizard),
	//95
	ArcanaLord(Race.human, ClassType.Mystic, ClassLevel.Fourth, ClassType.Summoner),
	//96
	Cardinal(Race.human, ClassType.Priest, ClassLevel.Fourth, ClassType.Healer),
	//97
	Hierophant(Race.human, ClassType.Priest, ClassLevel.Fourth, ClassType.Enchanter),
	//98
	EvaTemplar(Race.elf, ClassType.Fighter, ClassLevel.Fourth, ClassType.ShieldMaster),
	//99
	SwordMuse(Race.elf, ClassType.Fighter, ClassLevel.Fourth, ClassType.Bard),
	//100
	WindRider(Race.elf, ClassType.Fighter, ClassLevel.Fourth, ClassType.DaggerMaster),
	//101
	MoonlightSentinel(Race.elf, ClassType.Fighter, ClassLevel.Fourth, ClassType.BowMaster),
	//102
	MysticMuse(Race.elf, ClassType.Mystic, ClassLevel.Fourth, ClassType.Wizard),
	//103
	ElementalMaster(Race.elf, ClassType.Mystic, ClassLevel.Fourth, ClassType.Summoner),
	//104
	EvaSaint(Race.elf, ClassType.Priest, ClassLevel.Fourth, ClassType.Healer),
	//105
	ShillienTemplar(Race.darkelf, ClassType.Fighter, ClassLevel.Fourth, ClassType.ShieldMaster),
	//106
	SpectralDancer(Race.darkelf, ClassType.Fighter, ClassLevel.Fourth, ClassType.Bard),
	//107
	GhostHunter(Race.darkelf, ClassType.Fighter, ClassLevel.Fourth, ClassType.DaggerMaster),
	//108
	GhostSentinel(Race.darkelf, ClassType.Fighter, ClassLevel.Fourth, ClassType.BowMaster),
	//109
	StormScreamer(Race.darkelf, ClassType.Mystic, ClassLevel.Fourth, ClassType.Wizard),
	//110
	SpectralMaster(Race.darkelf, ClassType.Mystic, ClassLevel.Fourth, ClassType.Summoner),
	//111
	ShillienSaint(Race.darkelf, ClassType.Priest, ClassLevel.Fourth, ClassType.Healer),
	//112
	Titan(Race.orc, ClassType.Fighter, ClassLevel.Fourth, ClassType.WeaponMaster),
	//113
	GrandKhauatari(Race.orc, ClassType.Fighter, ClassLevel.Fourth, ClassType.ForceMaster),
	//114
	Dominator(Race.orc, ClassType.Mystic, ClassLevel.Fourth, ClassType.Enchanter),
	//115
	Doomcryer(Race.orc, ClassType.Mystic, ClassLevel.Fourth, ClassType.Enchanter),
	//116
	FortuneSeeker(Race.dwarf, ClassType.Fighter, ClassLevel.Fourth, ClassType.WeaponMaster),
	//117
	Maestro(Race.dwarf, ClassType.Fighter, ClassLevel.Fourth, ClassType.WeaponMaster);

	private Race _race;
	private ClassLevel _level;
	private ClassType _type;
	private ClassType _typeExtended;

	private static final Set<PlayerClass> mainSubclassSet;
	private static final Set<PlayerClass> neverSubclassed = EnumSet.of(Overlord, Warsmith);

	private static final Set<PlayerClass> subclasseSet1 = EnumSet.of(DarkAvenger, Paladin, TempleKnight, ShillienKnight);
	private static final Set<PlayerClass> subclasseSet2 = EnumSet.of(TreasureHunter, AbyssWalker, Plainswalker);
	private static final Set<PlayerClass> subclasseSet3 = EnumSet.of(Hawkeye, SilverRanger, PhantomRanger);
	private static final Set<PlayerClass> subclasseSet4 = EnumSet.of(Warlock, ElementalSummoner, PhantomSummoner);
	private static final Set<PlayerClass> subclasseSet5 = EnumSet.of(Sorceror, Spellsinger, Spellhowler);

	private static final EnumMap<PlayerClass, Set<PlayerClass>> subclassSetMap = new EnumMap<PlayerClass, Set<PlayerClass>>(PlayerClass.class);

	static
	{
		Set<PlayerClass> subclasses = getSet(null, ClassLevel.Third);
		subclasses.removeAll(neverSubclassed);

		mainSubclassSet = subclasses;

		subclassSetMap.put(DarkAvenger, subclasseSet1);
		subclassSetMap.put(HellKnight, subclasseSet1);
		subclassSetMap.put(Paladin, subclasseSet1);
		subclassSetMap.put(PhoenixKnight, subclasseSet1);
		subclassSetMap.put(TempleKnight, subclasseSet1);
		subclassSetMap.put(EvaTemplar, subclasseSet1);
		subclassSetMap.put(ShillienKnight, subclasseSet1);
		subclassSetMap.put(ShillienTemplar, subclasseSet1);

		subclassSetMap.put(TreasureHunter, subclasseSet2);
		subclassSetMap.put(Adventurer, subclasseSet2);
		subclassSetMap.put(AbyssWalker, subclasseSet2);
		subclassSetMap.put(GhostHunter, subclasseSet2);
		subclassSetMap.put(Plainswalker, subclasseSet2);
		subclassSetMap.put(WindRider, subclasseSet2);

		subclassSetMap.put(Hawkeye, subclasseSet3);
		subclassSetMap.put(Sagittarius, subclasseSet3);
		subclassSetMap.put(SilverRanger, subclasseSet3);
		subclassSetMap.put(MoonlightSentinel, subclasseSet3);
		subclassSetMap.put(PhantomRanger, subclasseSet3);
		subclassSetMap.put(GhostSentinel, subclasseSet3);

		subclassSetMap.put(Warlock, subclasseSet4);
		subclassSetMap.put(ArcanaLord, subclasseSet4);
		subclassSetMap.put(ElementalSummoner, subclasseSet4);
		subclassSetMap.put(ElementalMaster, subclasseSet4);
		subclassSetMap.put(PhantomSummoner, subclasseSet4);
		subclassSetMap.put(SpectralMaster, subclasseSet4);

		subclassSetMap.put(Sorceror, subclasseSet5);
		subclassSetMap.put(Archmage, subclasseSet5);
		subclassSetMap.put(Spellsinger, subclasseSet5);
		subclassSetMap.put(MysticMuse, subclasseSet5);
		subclassSetMap.put(Spellhowler, subclasseSet5);
		subclassSetMap.put(StormScreamer, subclasseSet5);

		subclassSetMap.put(Duelist, EnumSet.of(Gladiator));
		subclassSetMap.put(Dreadnought, EnumSet.of(Warlord));
		subclassSetMap.put(Soultaker, EnumSet.of(Necromancer));
		subclassSetMap.put(Cardinal, EnumSet.of(Bishop));
		subclassSetMap.put(Hierophant, EnumSet.of(Prophet));
		subclassSetMap.put(SwordMuse, EnumSet.of(Swordsinger));
		subclassSetMap.put(EvaSaint, EnumSet.of(ElvenElder));
		subclassSetMap.put(SpectralDancer, EnumSet.of(Bladedancer));
		subclassSetMap.put(Titan, EnumSet.of(Destroyer));
		subclassSetMap.put(GrandKhauatari, EnumSet.of(Tyrant));
		subclassSetMap.put(Dominator, EnumSet.of(Overlord));
		subclassSetMap.put(Doomcryer, EnumSet.of(Warcryer));
	}

	PlayerClass(Race race, ClassType type, ClassLevel level)
	{
		this(race, type, level, null);
	}
	
	PlayerClass(Race race, ClassType type, ClassLevel level, ClassType extended)
	{
		_race = race;
		_level = level;
		_type = type;
		_typeExtended = extended;
	}

	public final Set<PlayerClass> getAvailableSubclasses()
	{
		Set<PlayerClass> subclasses = null;

		if(_level == ClassLevel.Third || _level == ClassLevel.Fourth)
		{
			subclasses = EnumSet.copyOf(mainSubclassSet);

			subclasses.removeAll(neverSubclassed);
			subclasses.remove(this);

			switch(_race)
			{
				case elf:
					subclasses.removeAll(getSet(Race.darkelf, ClassLevel.Third));
					break;
				case darkelf:
					subclasses.removeAll(getSet(Race.elf, ClassLevel.Third));
					break;
			}

			Set<PlayerClass> unavailableClasses = subclassSetMap.get(this);

			if(unavailableClasses != null)
				subclasses.removeAll(unavailableClasses);
		}

		return subclasses;
	}

	public static EnumSet<PlayerClass> getSet(Race race, ClassLevel level)
	{
		EnumSet<PlayerClass> allOf = EnumSet.noneOf(PlayerClass.class);

		for(PlayerClass playerClass : EnumSet.allOf(PlayerClass.class))
			if(race == null || playerClass.isOfRace(race))
				if(level == null || playerClass.isOfLevel(level))
					allOf.add(playerClass);

		return allOf;
	}

	public final boolean isOfRace(Race race)
	{
		return _race == race;
	}

	public final boolean isOfType(ClassType type)
	{
		return _type == type;
	}

	public final boolean isOfLevel(ClassLevel level)
	{
		return _level == level;
	}

	public final Integer getTypeExtended()
	{
		return _typeExtended.ordinal();
	}
}