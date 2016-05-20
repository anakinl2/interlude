package ai;

import java.util.HashMap;

import javolution.util.FastMap;

import com.lineage.game.ai.DefaultAI;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.util.Rnd;
import bosses.BaiumManager;

/**
 * AI боса Байума.<br>
 * - Мгновенно убивает первого ударившего<br>
 * - Для атаки использует только скилы по следующей схеме:
 * <li>Стандартный набор: 80% - 4127, 10% - 4128, 10% - 4129
 * <li>если хп < 50%: 70% - 4127, 10% - 4128, 10% - 4129, 10% - 4131
 * <li>если хп < 25%: 60% - 4127, 10% - 4128, 10% - 4129, 10% - 4131, 10% - 4130
 */
public class Baium extends DefaultAI
{
	private boolean _firstTimeAttacked = true;

	// Боевые скилы байума
	final L2Skill baium_normal_attack, energy_wave, earth_quake, thunderbolt, group_hold;

	public Baium(L2Character actor)
	{
		super(actor);
		HashMap<Integer, L2Skill> skills = getActor().getTemplate().getSkills();
		baium_normal_attack = skills.get(4127);
		energy_wave = skills.get(4128);
		earth_quake = skills.get(4129);
		thunderbolt = skills.get(4130);
		group_hold = skills.get(4131);
	}

	@Override
	public boolean isGlobalAI()
	{
		return true;
	}

	//We dont need this.
/*	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		if(_firstTimeAttacked)
		{
			_firstTimeAttacked = false;
			L2NpcInstance actor = getActor();
			if(actor == null || attacker == null)
				return;
			if(attacker.isPlayer() && attacker.getPet() != null)
				attacker.getPet().doDie(actor);
			else if((attacker.isSummon() || attacker.isPet()) && attacker.getPlayer() != null)
				attacker.getPlayer().doDie(actor);
			attacker.doDie(actor);
		}

		super.onEvtAttacked(attacker, damage);
	}*/

	@Override
	protected boolean createNewTask()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return true;

		if(!BaiumManager.getZone().checkIfInZone(actor))
		{
			teleportHome();
			return false;
		}

		clearTasks();

		L2Character target;
		if((target = prepareTarget()) == null)
			return false;

		// Шансы использования скилов
		int s_energy_wave = 10;
		int s_earth_quake = 10;
		int s_group_hold = actor.getCurrentHpPercents() > 50 ? 0 : 10;
		int s_thunderbolt = actor.getCurrentHpPercents() > 25 ? 0 : 10;

		L2Skill r_skill = null;

		if(actor.isRooted()) // Если в руте, то использовать массовый скилл дальнего боя
			r_skill = thunderbolt;
		else if(!Rnd.chance(100 - s_thunderbolt - s_group_hold - s_energy_wave - s_earth_quake)) // Выбираем скилл атаки
		{
			FastMap<L2Skill, Integer> d_skill = new FastMap<L2Skill, Integer>(); //TODO class field ?
			double distance = actor.getDistance(target);

			addDesiredSkill(d_skill, target, distance, energy_wave);
			addDesiredSkill(d_skill, target, distance, earth_quake);
			if(s_group_hold > 0)
				addDesiredSkill(d_skill, target, distance, group_hold);
			if(s_thunderbolt > 0)
				addDesiredSkill(d_skill, target, distance, thunderbolt);
			r_skill = selectTopSkill(d_skill);
		}

		// Использовать скилл если можно, иначе атаковать скилом baium_normal_attack
		if(r_skill == null)
			r_skill = baium_normal_attack;
		else if(r_skill.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF)
			target = actor;

		// Добавить новое задание
		Task task = new Task();
		task.type = TaskType.CAST;
		task.target = target;
		task.skill = r_skill;
		_task_list.add(task);
		_def_think = true;
		r_skill = null;
		return true;
	}

	@Override
	protected boolean maybeMoveToHome()
	{
		L2NpcInstance actor = getActor();
		if(actor != null && !BaiumManager.getZone().checkIfInZone(actor))
			teleportHome();
		return false;
	}

	@Override
	protected void onEvtDead()
	{
		_firstTimeAttacked = true;
		super.onEvtDead();
	}
}