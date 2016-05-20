package events.thefallharvest;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2MonsterInstance;
import com.lineage.game.templates.L2NpcTemplate;

public class SquashInstance extends L2MonsterInstance
{
	public final static int Young_Squash = 12774;
	public final static int High_Quality_Squash = 12775;
	public final static int Low_Quality_Squash = 12776;
	public final static int Large_Young_Squash = 12777;
	public final static int High_Quality_Large_Squash = 12778;
	public final static int Low_Quality_Large_Squash = 12779;
	public final static int King_Squash = 13016;
	public final static int Emperor_Squash = 13017;

	private L2Player _spawner;

	public SquashInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		_spawner = null;
	}

	public void setSpawner(L2Player spawner)
	{
		_spawner = spawner;
	}

	public L2Player getSpawner()
	{
		return _spawner;
	}

	@Override
	public void reduceCurrentHp(double i, L2Character attacker, L2Skill skill, boolean awake, boolean standUp, boolean directHp, boolean canReflect)
	{
		if(attacker.getActiveWeaponInstance() == null)
			return;

		int weaponId = attacker.getActiveWeaponInstance().getItemId();

		if(getNpcId() == Low_Quality_Large_Squash || getNpcId() == High_Quality_Large_Squash || getNpcId() == Emperor_Squash)
			// Разрешенное оружие для больших тыкв:
			// 4202 Chrono Cithara
			// 5133 Chrono Unitus
			// 5817 Chrono Campana
			// 7058 Chrono Darbuka
			// 8350 Chrono Maracas
			if(weaponId != 4202 && weaponId != 5133 && weaponId != 5817 && weaponId != 7058 && weaponId != 8350)
				return;

		i = 1;

		super.reduceCurrentHp(i, attacker, skill, awake, standUp, directHp, canReflect);
	}

	@Override
	public synchronized void startRegeneration()
	{}
}