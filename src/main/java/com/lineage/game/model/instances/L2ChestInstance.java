package com.lineage.game.model.instances;

import javolution.util.FastList;
import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.ai.CtrlEvent;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.base.ItemToDrop;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Rnd;

public class L2ChestInstance extends L2MonsterInstance
{
	private boolean _fake;

	public L2ChestInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onSpawn()
	{
		_fake = !Rnd.chance(Config.ALT_TRUE_CHESTS);
		super.onSpawn();
	}

	public void onOpen(L2Player opener)
	{
		if(_fake)
		{
			opener.sendMessage(new CustomMessage("l2d.game.model.instances.L2ChestInstance.Fake", opener));
			getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, opener, 100);
		}
		else
		{
			setSpoiled(false, null);
			int trueId = getTrueId();
			if(NpcTable.getTemplate(trueId) != null && NpcTable.getTemplate(trueId).getDropData() != null)
			{
				final FastList<ItemToDrop> drops = NpcTable.getTemplate(getTrueId()).getDropData().rollDrop(0, this, opener, 1.);
				for(final ItemToDrop drop : drops)
					dropItem(opener, drop.itemId, drop.count);
			}
			doDie(opener);
		}
	}

	private int getTrueId()
	{
		switch(getNpcId())
		{
			case 21671: // Otherworldly Invader Food
				return getNpcId() - Rnd.get(3383, 3384);
			case 21694: // Dimension Invader Food
				return getNpcId() - Rnd.get(3404, 3405);
			case 21717: // Purgatory Invader Food
				return getNpcId() - Rnd.get(3425, 3426);
			case 21740: // Forbidden Path Invader Food
				return getNpcId() - Rnd.get(3446, 3445);
			case 21763: // Dark Omen Invader Food
				return getNpcId() - Rnd.get(3467, 3468);
			case 21786: // Messenger Invader Food
				return getNpcId() - Rnd.get(3488, 3489);
			default:
				return getNpcId() - 3536;
		}
	}

	@Override
	public void reduceCurrentHp(final double damage, final L2Character attacker, L2Skill skill, final boolean awake, final boolean standUp, boolean directHp, boolean canReflect)
	{
		if(_fake)
			super.reduceCurrentHp(damage, attacker, skill, awake, standUp, directHp, canReflect);
		else
			doDie(attacker);
	}

	public boolean isFake()
	{
		return _fake;
	}
}