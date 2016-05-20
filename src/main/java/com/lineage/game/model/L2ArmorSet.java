package com.lineage.game.model;

import com.lineage.game.model.instances.L2ItemInstance;

public final class L2ArmorSet
{
	private final int _chest;
	private final int _legs;
	private final int _head;
	private final int _gloves;
	private final int _feet;
	private final int _shield;

	private final L2Skill _skill;
	private final L2Skill _shieldSkill;
	private final L2Skill _enchant6skill;

	public L2ArmorSet(int chest, int legs, int head, int gloves, int feet, L2Skill skill, int shield, L2Skill shield_skill, L2Skill enchant6skill)
	{
		_chest = chest;
		_legs = legs;
		_head = head;
		_gloves = gloves;
		_feet = feet;
		_shield = shield;

		_skill = skill;
		_shieldSkill = shield_skill;
		_enchant6skill = enchant6skill;
	}

	/**
	 * Checks if player have equipped all items from set (not checking shield)
	 * @param player whose inventory is being checked
	 * @return True if player equips whole set
	 */
	public boolean containAll(L2Player player)
	{
		Inventory inv = player.getInventory();

		L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);

		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;

		if(legsItem != null)
			legs = legsItem.getItemId();
		if(headItem != null)
			head = headItem.getItemId();
		if(glovesItem != null)
			gloves = glovesItem.getItemId();
		if(feetItem != null)
			feet = feetItem.getItemId();

		return containAll(_chest, legs, head, gloves, feet);

	}

	public boolean containAll(int chest, int legs, int head, int gloves, int feet)
	{
		if(_chest != 0 && _chest != chest)
			return false;
		if(_legs != 0 && _legs != legs)
			return false;
		if(_head != 0 && _head != head)
			return false;
		if(_gloves != 0 && _gloves != gloves)
			return false;
		if(_feet != 0 && _feet != feet)
			return false;

		return true;
	}

	public boolean containItem(int slot, int itemId)
	{
		switch(slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				return _chest == itemId;
			case Inventory.PAPERDOLL_LEGS:
				return _legs == itemId;
			case Inventory.PAPERDOLL_HEAD:
				return _head == itemId;
			case Inventory.PAPERDOLL_GLOVES:
				return _gloves == itemId;
			case Inventory.PAPERDOLL_FEET:
				return _feet == itemId;
			default:
				return false;
		}
	}

	public L2Skill getSkill()
	{
		return _skill;
	}

	public boolean containShield(L2Player player)
	{
		Inventory inv = player.getInventory();

		L2ItemInstance shieldItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if(shieldItem != null && shieldItem.getItemId() == _shield)
			return true;

		return false;
	}

	public boolean containShield(int shield_id)
	{
		if(_shield == 0)
			return false;

		return _shield == shield_id;
	}

	public L2Skill getShieldSkill()
	{
		return _shieldSkill;
	}

	public L2Skill getEnchant6skill()
	{
		return _enchant6skill;
	}

	/**
	 * Checks if all parts of set are enchanted to +6 or more
	 * @param player
	 * @return
	 */
	public boolean isEnchanted6(L2Player player)
	{
		// Player don't have full set
		if(!containAll(player))
			return false;

		Inventory inv = player.getInventory();

		L2ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);

		if(chestItem.getEnchantLevel() < 6)
			return false;
		if(_legs != 0 && legsItem.getEnchantLevel() < 6)
			return false;
		if(_gloves != 0 && glovesItem.getEnchantLevel() < 6)
			return false;
		if(_head != 0 && headItem.getEnchantLevel() < 6)
			return false;
		if(_feet != 0 && feetItem.getEnchantLevel() < 6)
			return false;

		return true;
	}

	public int[] getAllItems()
	{
		int[] i = { _chest, _legs, _head, _gloves, _feet, 2498, 924,/*neclas*/
		862,/*earning*/
		893, /*ring*/
		862,/*earning*/
		893 /*ring*/
		};
		return i;
	}
}