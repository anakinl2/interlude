package com.lineage.game.model.listeners;

import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.SkillList;
import com.lineage.game.skills.Formulas;
import com.lineage.game.templates.L2Item;

/**
 * Добавление\удалениe скилов, прописанных предметам в sql или в xml.
 */
public final class ItemSkillsListener implements PaperdollListener
{
	Inventory _inv;

	public ItemSkillsListener(Inventory inv)
	{
		_inv = inv;
	}

	@Override
	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		if(!_inv.isRefreshingListeners() && item.isShadowItem())
			item.notifyEquipped(false);

		L2Player player;

		if(_inv.getOwner().isPlayer())
			player = (L2Player) _inv.getOwner();
		else
			return;

		L2Skill[] itemSkills = null;
		L2Skill enchant4Skill = null;

		L2Item it = item.getItem();

		itemSkills = it.getAttachedSkills();

		enchant4Skill = it.getEnchant4Skill();

		if(itemSkills != null)
			for(L2Skill itemSkill : itemSkills)
			{
				if(!itemSkill.isLikePassive())
					player.removeSkillFromShortCut(itemSkill.getId());
				player.removeSkill(itemSkill, false);
			}

		if(enchant4Skill != null)
			player.removeSkill(enchant4Skill, false);

		if(itemSkills != null || enchant4Skill != null)
		{
			player.sendPacket(new SkillList(player));
			player.updateStats();
		}
	}

	@Override
	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		if(!_inv.isRefreshingListeners() && item.isShadowItem())
			item.notifyEquipped(true);

		L2Player player;
		if(_inv.getOwner().isPlayer())
			player = (L2Player) _inv.getOwner();
		else
			return;

		L2Skill[] itemSkills = null;
		L2Skill enchant4Skill = null;

		L2Item it = item.getItem();

		itemSkills = it.getAttachedSkills();

		if(item.getEnchantLevel() >= 4)
			enchant4Skill = it.getEnchant4Skill();

		if(itemSkills != null)
			for(L2Skill itemSkill : itemSkills)
			{
				player.addSkill(itemSkill, false);
				if(itemSkill.isActive())
				{
					long reuseDelay = Formulas.calcSkillReuseDelay(player, itemSkill);
					reuseDelay = Math.min(reuseDelay, 20000);
					if(reuseDelay > 0 && player.getSkillReuseTimeStamps().get(itemSkill.getId()) == null)
					{
						player.disableSkill(itemSkill.getId(), reuseDelay);
						player.addSkillTimeStamp(itemSkill.getId(), reuseDelay);
					}
				}
			}

		if(enchant4Skill != null)
			player.addSkill(enchant4Skill, false);

		if(itemSkills != null || enchant4Skill != null)
		{
			player.sendPacket(new SkillList(player));
			player.updateStats();
		}
	}
}