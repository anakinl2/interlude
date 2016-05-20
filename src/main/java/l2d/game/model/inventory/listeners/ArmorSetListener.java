package l2d.game.model.inventory.listeners;

import java.util.logging.Logger;

import l2d.game.model.Inventory;
import l2d.game.model.L2ArmorSet;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.SkillList;
import l2d.game.tables.ArmorSetsTable;
import l2d.game.tables.SkillTable;

public final class ArmorSetListener implements PaperdollListener
{
	private static final L2Skill COMMON_SET_SKILL = SkillTable.getInstance().getInfo(3006, 1);
	protected static final Logger _log = Logger.getLogger(ArmorSetListener.class.getName());

	Inventory _inv;

	public ArmorSetListener(final Inventory inv)
	{
		_inv = inv;
	}

	@Override
	public void notifyEquipped(final int slot, final L2ItemInstance item)
	{
		if(!_inv.getOwner().isPlayer())
			return;

		final L2Player player = (L2Player) _inv.getOwner();

		// checks if player worns chest item
		final L2ItemInstance chestItem = _inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if(chestItem == null)
			return;

		// checks if there is armorset for chest item that player worns
		final L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
		if(armorSet == null)
			return;

		boolean update = false;
		// checks if equipped item is part of set
		if(armorSet.containItem(slot, item.getItemId()))
		{
			if(armorSet.containAll(player))
			{
				final L2Skill skill = armorSet.getSkill();
				if(skill != null)
				{
					player.addSkill(skill, false);
					player.addSkill(COMMON_SET_SKILL, false);
					update = true;
				}

				if(armorSet.containShield(player)) // has shield from set
				{
					final L2Skill skills = armorSet.getShieldSkill();
					if(skills != null)
					{
						player.addSkill(skills, false);
						update = true;
					}
				}
				if(armorSet.isEnchanted6(player)) // has all parts of set enchanted to 6 or more
				{
					final L2Skill skille = armorSet.getEnchant6skill();
					if(skille != null)
					{
						player.addSkill(skille, false);
						update = true;
					}
				}
			}
		}
		else if(armorSet.containShield(item.getItemId()))
			if(armorSet.containAll(player))
			{
				final L2Skill skills = armorSet.getShieldSkill();
				if(skills != null)
				{
					player.addSkill(skills, false);
					update = true;
				}
			}

		if(update)
		{
			player.sendPacket(new SkillList(player));
			player.updateStats();
		}
	}

	@Override
	public void notifyUnequipped(final int slot, final L2ItemInstance item)
	{
		boolean remove = false;
		L2Skill removeSkillId1 = null; // set skill
		L2Skill removeSkillId2 = null; // shield skill
		L2Skill removeSkillId3 = null; // enchant +6 skill

		if(slot == Inventory.PAPERDOLL_CHEST)
		{
			final L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(item.getItemId());
			if(armorSet == null)
				return;

			remove = true;
			removeSkillId1 = armorSet.getSkill();
			removeSkillId2 = armorSet.getShieldSkill();
			removeSkillId3 = armorSet.getEnchant6skill();

		}
		else
		{
			final L2ItemInstance chestItem = _inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if(chestItem == null)
				return;

			final L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
			if(armorSet == null)
				return;

			if(armorSet.containItem(slot, item.getItemId())) // removed part of set
			{
				remove = true;
				removeSkillId1 = armorSet.getSkill();
				removeSkillId2 = armorSet.getShieldSkill();
				removeSkillId3 = armorSet.getEnchant6skill();
			}
			else if(armorSet.containShield(item.getItemId())) // removed shield
			{
				remove = true;
				removeSkillId2 = armorSet.getShieldSkill();
			}
		}

		boolean update = false;
		if(remove)
		{
			if(removeSkillId1 != null)
			{
				((L2Player) _inv.getOwner()).removeSkill(removeSkillId1, false);
				((L2Player) _inv.getOwner()).removeSkill(COMMON_SET_SKILL, false);
				update = true;
			}
			if(removeSkillId2 != null)
			{
				_inv.getOwner().removeSkill(removeSkillId2);
				update = true;
			}
			if(removeSkillId3 != null)
			{
				_inv.getOwner().removeSkill(removeSkillId3);
				update = true;
			}
		}

		if(update)
		{
			_inv.getOwner().sendPacket(new SkillList((L2Player) _inv.getOwner()));
			_inv.getOwner().updateStats();
		}
	}
}