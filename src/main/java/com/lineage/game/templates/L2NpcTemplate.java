package com.lineage.game.templates;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;
import com.lineage.ext.scripts.Script;
import com.lineage.ext.scripts.Scripts;
import com.lineage.game.model.L2Drop;
import com.lineage.game.model.L2DropData;
import com.lineage.game.model.L2MinionData;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Skill.SkillType;
import com.lineage.game.model.base.ClassId;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.model.instances.L2RaidBossInstance;
import com.lineage.game.model.instances.L2ReflectionBossInstance;
import com.lineage.game.model.quest.Quest;
import com.lineage.game.model.quest.QuestEventType;

/**
 * This cl contains all generic data of a L2Spawn object.<BR><BR>
 * <B><U> Data</U> :</B><BR><BR>
 * <li>npcId, type, name, sex</li>
 * <li>revardExp, revardSp</li>
 * <li>aggroRange, factionId, factionRange</li>
 * <li>rhand, lhand, armor</li>
 * <li>_drops</li>
 * <li>_minions</li>
 * <li>_teachInfo</li>
 * <li>_skills</li>
 * <li>_questsStart</li><BR><BR>
 */
public final class L2NpcTemplate extends L2CharTemplate
{
	private static final Logger _log = Logger.getLogger(L2NpcTemplate.class.getName());

	public static enum ShotsType
	{
		NONE,
		SOUL,
		SPIRIT,
		BSPIRIT,
		SOUL_SPIRIT,
		SOUL_BSPIRIT
	}

	private final static HashMap<Integer, L2Skill> _emptySkills = new HashMap<Integer, L2Skill>(0);
	private final static L2Skill[] _emptySkillArray = new L2Skill[0];

	public final int npcId;
	public String type;
	public String ai_type;
	public final String name;
	public String title;
	// не используется - public final String sex;
	public final byte level;
	public final int revardExp;
	public final int revardSp;
	public final short aggroRange;
	public int _rhand;
	public final int _lhand;
	// не используется - public final int armor;
	public final String factionId;
	public final short factionRange;
	public final String jClass;
	public int displayId = 0;
	public boolean isDropHerbs = false;
	public final ShotsType shots;
	public boolean isRaid;
	private StatsSet _AIParams = null;

	/** fixed skills */
	private int race = 0;
	public double rateHp = 1;

	/** The object containing all Item that can be dropped by L2NpcInstance using this L2NpcTemplate */
	private L2Drop _drop = null;
	public int killscount = 0;

	/** The table containing all Minions that must be spawn with the L2NpcInstance using this L2NpcTemplate */
	private final List<L2MinionData> _minions = new ArrayList<L2MinionData>(0);

	private List<ClassId> _teachInfo = null;
	private Map<QuestEventType, Quest[]> _questEvents;
	private Class<L2NpcInstance> this_class;

	private HashMap<Integer, L2Skill> _skills;
	private HashMap<SkillType, L2Skill[]> _skillsByType;
	private L2Skill[] _dam_skills, _dot_skills, _debuff_skills, _buff_skills;

	/**
	 * Constructor of L2Character.<BR><BR>
	 * 
	 * @param set
	 *            The StatsSet object to transfer data to the method
	 */
	public L2NpcTemplate(final StatsSet set, final StatsSet AIParams)
	{
		super(set);
		npcId = set.getInteger("npcId");
		displayId = set.getInteger("displayId");
		type = set.getString("type");
		ai_type = set.getString("ai_type");
		name = set.getString("name");
		title = set.getString("title");
		// sex = set.getString("sex");
		level = set.getByte("level");
		revardExp = set.getInteger("revardExp");
		revardSp = set.getInteger("revardSp");
		aggroRange = set.getShort("aggroRange");
		_rhand = set.getInteger("rhand");
		_lhand = set.getInteger("lhand");
		// armor = set.getInteger("armor");
		jClass = set.getString("jClass");
		final String f = set.getString("factionId", null);
		factionId = f == null ? "" : f.intern();
		factionRange = set.getShort("factionRange");
		isDropHerbs = set.getBool("isDropHerbs");
		shots = set.getEnum("shots", ShotsType.class, ShotsType.NONE);
		_AIParams = AIParams;
		setInstance(type);
	}

	public Class<L2NpcInstance> getInstanceClass()
	{
		return this_class;
	}

	@SuppressWarnings("unchecked")
	public Constructor getInstanceConstructor()
	{
		return this_class == null ? null : this_class.getConstructors()[0];
	}

	@SuppressWarnings("unchecked")
	public boolean isInstanceOf(final Class _class)
	{
		return this_class != null && _class.isAssignableFrom(this_class);
	}

	@SuppressWarnings("unchecked")
	public void setInstance(final String type)
	{
		Class _this_class = null;
		try
		{
			_this_class = Class.forName("com.lineage.game.model.instances." + type + "Instance");
		}
		catch(final ClassNotFoundException e)
		{
			final Script sc = Scripts.getInstance().getClasses().get("npc.model." + type + "Instance");
			if(sc != null)
				_this_class = sc.getRawClass();
		}
		if(_this_class == null)
			System.out.println("Not found type: " + type);
		this_class = _this_class;
		isRaid = isInstanceOf(L2RaidBossInstance.class) && !isInstanceOf(L2ReflectionBossInstance.class);
	}

	public L2NpcTemplate(final StatsSet set)
	{
		this(set, null);
	}

	public void addTeachInfo(final ClassId classId)
	{
		if(_teachInfo == null)
			_teachInfo = new ArrayList<ClassId>();
		_teachInfo.add(classId);
	}

	public ClassId[] getTeachInfo()
	{
		if(_teachInfo == null)
			return null;
		return _teachInfo.toArray(new ClassId[_teachInfo.size()]);
	}

	public boolean canTeach(final ClassId classId)
	{
		if(_teachInfo == null)
			return false;
		return _teachInfo.contains(classId);
	}

	public void addDropData(final L2DropData drop)
	{
		if(_drop == null)
			_drop = new L2Drop();
		_drop.addData(drop);
	}

	public void addRaidData(final L2MinionData minion)
	{
		_minions.add(minion);
	}

	public void addSkill(final L2Skill skill)
	{
		if(_skills == null)
			_skills = new HashMap<Integer, L2Skill>();
		if(_skillsByType == null)
			_skillsByType = new HashMap<SkillType, L2Skill[]>();

		_skills.put(skill.getId(), skill);

		L2Skill[] skilllist;
		if(_skillsByType.get(skill.getSkillType()) != null)
		{
			skilllist = new L2Skill[_skillsByType.get(skill.getSkillType()).length + 1];
			System.arraycopy(_skillsByType.get(skill.getSkillType()), 0, skilllist, 0, _skillsByType.get(skill.getSkillType()).length);
		}
		else
			skilllist = new L2Skill[1];

		skilllist[skilllist.length - 1] = skill;

		if(skill.getTargetType() != L2Skill.SkillTargetType.TARGET_NONE && skill.getSkillType() != L2Skill.SkillType.NOTDONE && skill.isActive())
			_skillsByType.put(skill.getSkillType(), skilllist);
	}

	public L2Skill[] getSkillsByType(final SkillType type)
	{
		if(_skillsByType == null)
			return _emptySkillArray;
		return _skillsByType.containsKey(type) ? _skillsByType.get(type) : _emptySkillArray;
	}

	public synchronized L2Skill[] getDamageSkills()
	{
		if(_dam_skills == null)
			_dam_skills = summ(new L2Skill[][] {
					getSkillsByType(SkillType.PDAM),
					getSkillsByType(SkillType.MANADAM),
					getSkillsByType(SkillType.MDAM),
					getSkillsByType(SkillType.DRAIN),
					getSkillsByType(SkillType.DRAIN_SOUL) });
		return _dam_skills;
	}

	public synchronized L2Skill[] getDotSkills()
	{
		if(_dot_skills == null)
			_dot_skills = summ(new L2Skill[][] {
					getSkillsByType(SkillType.DOT),
					getSkillsByType(SkillType.MDOT),
					getSkillsByType(SkillType.POISON),
					getSkillsByType(SkillType.BLEED) });
		return _dot_skills;
	}

	public synchronized L2Skill[] getDebuffSkills()
	{
		if(_debuff_skills == null)
			_debuff_skills = summ(new L2Skill[][] {
					getSkillsByType(SkillType.DEBUFF),
					getSkillsByType(SkillType.CANCEL),
					getSkillsByType(SkillType.SLEEP),
					getSkillsByType(SkillType.ROOT),
					getSkillsByType(SkillType.PARALYZE),
					getSkillsByType(SkillType.MUTE),
					getSkillsByType(SkillType.TELEPORT_NPC) });
		return _debuff_skills;
	}

	public synchronized L2Skill[] getBuffSkills()
	{
		if(_buff_skills == null)
			_buff_skills = summ(new L2Skill[][] {
					getSkillsByType(SkillType.DEBUFF),
					getSkillsByType(SkillType.CANCEL),
					getSkillsByType(SkillType.SLEEP),
					getSkillsByType(SkillType.ROOT),
					getSkillsByType(SkillType.PARALYZE),
					getSkillsByType(SkillType.MUTE),
					getSkillsByType(SkillType.TELEPORT_NPC) });
		return _buff_skills;
	}

	private static final L2Skill[] summ(final L2Skill[][] skills2d)
	{
		int i = 0;
		for(final L2Skill[] skills : skills2d)
			i += skills.length;
		if(i == 0)
			return _emptySkillArray;
		final L2Skill[] result = new L2Skill[i];
		i = 0;
		for(final L2Skill[] skills : skills2d)
		{
			System.arraycopy(skills, 0, result, i, skills.length);
			i += skills.length;
		}
		return result;
	}

	/**
	 * Return the list of all possible drops of this L2NpcTemplate.<BR><BR>
	 */
	public L2Drop getDropData()
	{
		return _drop;
	}

	/**
	 * Обнуляет дроплист моба
	 */
	public void clearDropData()
	{
		_drop = null;
	}

	/**
	 * Return the list of all Minions that must be spawn with the L2NpcInstance using this L2NpcTemplate.<BR><BR>
	 */
	public List<L2MinionData> getMinionData()
	{
		return _minions;
	}

	public HashMap<Integer, L2Skill> getSkills()
	{
		return _skills == null ? _emptySkills : _skills;
	}

	public void addQuestEvent(final QuestEventType EventType, final Quest q)
	{
		if(_questEvents == null)
			_questEvents = new FastMap<QuestEventType, Quest[]>();

		if(_questEvents.get(EventType) == null)
			_questEvents.put(EventType, new Quest[] { q });
		else
		{
			final Quest[] _quests = _questEvents.get(EventType);
			final int len = _quests.length;

			// if only one registration per npc is allowed for this event type
			// then only register this NPC if not already registered for the specified event.
			// if a quest allows multiple registrations, then register regardless of count
			if(EventType.isMultipleRegistrationAllowed() || len < 1)
			{
				final Quest[] tmp = new Quest[len + 1];
				for(int i = 0; i < len; i++)
				{
					if(_quests[i].getName().equals(q.getName()))
					{
						_quests[i] = q;
						return;
					}
					tmp[i] = _quests[i];
				}
				tmp[len] = q;
				_questEvents.put(EventType, tmp);
			}
			else
			{
				_quests[0] = q;
				_log.warning("Quest event not allowed in multiple quests. Replace addition of Event Type \"" + EventType + "\" for NPC \"" + name + "\" and quest \"" + q.getName() + "\".");
			}
		}
	}

	public Quest[] getEventQuests(final QuestEventType EventType)
	{
		if(_questEvents == null)
			return null;
		return _questEvents.get(EventType);
	}

	public boolean hasQuestEvents()
	{
		return _questEvents != null && !_questEvents.isEmpty();
	}

	public int getRace()
	{
		return race;
	}

	public void setRace(final int newrace)
	{
		race = newrace;
	}

	public boolean isUndead()
	{
		return race == 1;
	}

	public void setRateHp(final double newrate)
	{
		rateHp = newrate;
	}

	@Override
	public String toString()
	{
		return "Npc template " + name + "[" + npcId + "]";
	}

	@Override
	public int getNpcId()
	{
		return npcId;
	}

	public final String getJClass()
	{
		return jClass;
	}

	public final StatsSet getAIParams()
	{
		return _AIParams;
	}

	/**
	 * @return the rhand
	 */
	public int getRhand()
	{
		return _rhand;
	}

	/**
	 * @param rhand
	 *            the rhand to set
	 */
	public void setRhand(final int rhand)
	{
		_rhand = rhand;
	}
}