package ai;

import npc.model.OrfenInstance;

import com.lineage.ext.scripts.Functions;
import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Skill.SkillType;
import com.lineage.util.Location;
import com.lineage.util.PrintfFormat;
import com.lineage.util.Rnd;

public class Orfen extends Fighter
{
	public static final PrintfFormat[] MsgOnRecall = {
			new PrintfFormat("%s. Stop kidding yourself about your own powerlessness!"),
			new PrintfFormat("%s. I'll make you feel what true fear is!"),
			new PrintfFormat("You're really stupid to have challenged me. %s! Get ready!"),
			new PrintfFormat("%s. Do you think that's going to work?!") };

	public final L2Skill[] _paralyze;

	public Orfen(L2Character actor)
	{
		super(actor);
		_paralyze = getActor().getTemplate().getSkillsByType(SkillType.PARALYZE);
	}

	@Override
	protected boolean thinkActive()
	{
		if(super.thinkActive())
			return true;
		OrfenInstance actor = getActor();
		if(actor == null)
			return true;

		if(actor.isTeleported() && actor.getCurrentHpPercents() > 95)
		{
			actor.setTeleported(false);
			return true;
		}

		return false;
	}

	@Override
	protected boolean createNewTask()
	{
		return defaultNewTask();
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		super.onEvtAttacked(attacker, damage);
		OrfenInstance actor = getActor();
		if(actor == null || actor.isCastingNow())
			return;

		double distance = actor.getDistance(attacker);

		// if(attacker.isMuted() &&)
		if(distance > 300 && distance < 1000 && _dam_skills.length > 0 && Rnd.chance(10))
		{
			Functions.npcShout(actor, MsgOnRecall[Rnd.get(MsgOnRecall.length - 1)].sprintf(attacker.getName()));
			teleToLocation(attacker, Location.getAroundPosition(actor, attacker, 0, 50, 3));
			L2Skill r_skill = _dam_skills[Rnd.get(_dam_skills.length)];
			if(canUseSkill(r_skill, attacker, -1))
				AddUseSkillDesire(attacker, r_skill, 1000000);
		}
		else if(_paralyze.length > 0 && Rnd.chance(20))
		{
			L2Skill r_skill = _paralyze[Rnd.get(_paralyze.length)];
			if(canUseSkill(r_skill, attacker, -1))
				AddUseSkillDesire(attacker, r_skill, 1000000);
		}
	}

	@Override
	protected void onEvtSeeSpell(L2Skill skill, L2Character caster)
	{
		super.onEvtSeeSpell(skill, caster);
		OrfenInstance actor = getActor();
		if(actor == null || actor.isCastingNow())
			return;

		double distance = actor.getDistance(caster);
		if(_dam_skills.length > 0 && skill.getEffectPoint() > 0 && distance < 1000 && Rnd.chance(20))
		{
			Functions.npcShout(actor, MsgOnRecall[Rnd.get(MsgOnRecall.length)].sprintf(caster.getName()));
			teleToLocation(caster, Location.getAroundPosition(actor, caster, 0, 50, 3));
			L2Skill r_skill = _dam_skills[Rnd.get(_dam_skills.length)];
			if(canUseSkill(r_skill, caster, -1))
				AddUseSkillDesire(caster, r_skill, 1000000);
		}
	}

	@Override
	public OrfenInstance getActor()
	{
		return (OrfenInstance) super.getActor();
	}
	
	private void teleToLocation(L2Character attacker, Location loc)
	{
		attacker.setLoc(loc);
		attacker.setLastClientPosition(loc);
		attacker.setLastServerPosition(loc);
		attacker.validateLocation(true);
	}
}