package com.lineage.game.model.base;

import javolution.util.FastList;
import com.lineage.game.instancemanager.AugmentationManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.SkillList;
import com.lineage.game.skills.Formulas;
import com.lineage.game.skills.Stats;
import com.lineage.game.skills.funcs.FuncAdd;
import com.lineage.game.tables.SkillTable;

public final class L2Augmentation
{
	private int _effectsId = 0;
	private AugmentationStatBoni _boni = null;
	private L2Skill _skill = null;

	public L2Augmentation(int effects, L2Skill skill)
	{
		_effectsId = effects;
		_boni = new AugmentationStatBoni(_effectsId);
		_skill = skill;
	}

	public L2Augmentation(int effects, int skill, int skillLevel)
	{
		this(effects, SkillTable.getInstance().getInfo(skill, skillLevel));
	}

	public class AugmentationStatBoni
	{
		private Stats _stats[];
		private float _values[];
		private boolean _active;

		public AugmentationStatBoni(int augmentationId)
		{
			_active = false;
			FastList<AugmentationManager.AugStat> as = AugmentationManager.getInstance().getAugStatsById(augmentationId);

			_stats = new Stats[as.size()];
			_values = new float[as.size()];

			int i = 0;
			for(AugmentationManager.AugStat aStat : as)
			{
				_stats[i] = aStat.getStat();
				_values[i] = aStat.getValue();
				i++;
			}
		}

		public void applyBoni(L2Player player)
		{
			// make sure the boni are not applyed twice..
			if(_active)
				return;

			for(int i = 0; i < _stats.length; i++)
				player.addStatFunc(new FuncAdd(_stats[i], 0x60, this, _values[i]));

			_active = true;
		}

		public void removeBoni(L2Player player)
		{
			// make sure the boni is not removed twice
			if(!_active)
				return;

			player.removeStatsOwner(this);

			_active = false;
		}
	}

	/**
	 * Get the augmentation "id" used in server.
	 * 
	 * @return augmentationId
	 */
	public int getAugmentationId()
	{
		return _effectsId;
	}

	public L2Skill getSkill()
	{
		return _skill;
	}

	/**
	 * Applys the boni to the player.
	 * 
	 * @param player
	 */
	public void applyBoni(L2Player player)
	{
		_boni.applyBoni(player);

		// add the skill if any
		if(_skill != null)
		{
			player.addSkill(_skill);
			if(_skill.isActive())
			{
				long reuseDelay = Formulas.calcSkillReuseDelay(player, _skill);
				reuseDelay = Math.min(reuseDelay, 20000);
				if(reuseDelay > 0 && player.getSkillReuseTimeStamps().get(_skill.getId()) == null)
				{
					player.disableSkill(_skill.getId(), reuseDelay);
					player.addSkillTimeStamp(_skill.getId(), reuseDelay);
				}
			}
			player.sendPacket(new SkillList(player));
		}
	}

	/**
	 * Removes the augmentation boni from the player.
	 * 
	 * @param player
	 */
	public void removeBoni(L2Player player)
	{
		_boni.removeBoni(player);

		// remove the skill if any
		if(_skill != null)
		{
			player.removeSkill(_skill);
			player.sendPacket(new SkillList(player));
		}
	}

	@Override
	public String toString()
	{
		return "Augmentation";
	}
}