package l2d.game.skills.funcs;

import l2d.game.model.instances.L2ItemInstance;
import l2d.game.skills.Env;
import l2d.game.skills.Stats;
import l2d.game.templates.L2Item;
import l2d.game.templates.L2Item.Grade;
import l2d.game.templates.L2Weapon.WeaponType;

public class FuncEnchant extends Func
{
	public FuncEnchant(final Stats stat, final int order, final Object owner, final double value)
	{
		super(stat, order, owner);
	}

	@Override
	public void calc(final Env env)
	{
		if(_cond != null && !_cond.test(env))
			return;
		final L2ItemInstance item = (L2ItemInstance) _funcOwner;

		if(item.getItem().getCrystalType() == Grade.NONE)
			return;

		final int enchant = item.getEnchantLevel();
		final int overenchant = Math.max(0, enchant - 3);

		if(_stat == Stats.MAGIC_DEFENCE || _stat == Stats.POWER_DEFENCE)
		{
			env.value += enchant + overenchant * 3;
			return;
		}

		if(_stat == Stats.MAX_HP)
		{
			// D - 4,25x2 - 21,05x + 25,15
			// C - 5,75x2 - 27,95x + 31,85
			// B - 7x2 - 35,4x + 43,8
			// A - 8x2 - 41x + 52
			// S - 8,5x2 - 42,5x + 51

			final int enchant_level = Math.min(20, enchant);
			if(enchant_level > 3)
				switch(item.getItem().getCrystalType().cry)
				{
					case L2Item.CRYSTAL_D:
						env.value += 4.25 * Math.pow(enchant_level, 2) - 21.05 * enchant_level + 25.15;
						break;
					case L2Item.CRYSTAL_C:
						env.value += 5.75 * Math.pow(enchant_level, 2) - 27.95 * enchant_level + 31.85;
						break;
					case L2Item.CRYSTAL_B:
						env.value += 7 * Math.pow(enchant_level, 2) - 35.4 * enchant_level + 43.8;
						break;
					case L2Item.CRYSTAL_A:
						env.value += 8 * Math.pow(enchant_level, 2) - 41 * enchant_level + 52;
						break;
					case L2Item.CRYSTAL_S:
						env.value += 8.5 * Math.pow(enchant_level, 2) - 42.5 * enchant_level + 51;
						break;
				}
			return;
		}

		if(_stat == Stats.MAGIC_ATTACK)
		{
			switch(item.getItem().getCrystalType().cry)
			{
				case L2Item.CRYSTAL_S:
					env.value += 4 * (enchant + overenchant);
					break;
				case L2Item.CRYSTAL_A:
					env.value += 3 * (enchant + overenchant);
					break;
				case L2Item.CRYSTAL_B:
					env.value += 3 * (enchant + overenchant);
					break;
				case L2Item.CRYSTAL_C:
					env.value += 3 * (enchant + overenchant);
					break;
				case L2Item.CRYSTAL_D:
					env.value += 2 * (enchant + overenchant);
					break;
			}
			return;
		}

		final Enum itemType = item.getItemType();
		switch(item.getItem().getCrystalType().cry)
		{
			case L2Item.CRYSTAL_S:
				if(itemType == WeaponType.BOW)
					env.value += 10 * (enchant + overenchant);
				else if((itemType == WeaponType.DUALFIST || itemType == WeaponType.DUAL || itemType == WeaponType.BIGSWORD || itemType == WeaponType.SWORD) && item.getItem().getBodyPart() == L2Item.SLOT_LR_HAND)
					env.value += 6 * (enchant + overenchant);
				else
					env.value += 5 * (enchant + overenchant);
				break;
			case L2Item.CRYSTAL_A:
				if(itemType == WeaponType.BOW)
					env.value += 8 * (enchant + overenchant);
				else if((itemType == WeaponType.DUALFIST || itemType == WeaponType.DUAL || itemType == WeaponType.BIGSWORD || itemType == WeaponType.SWORD) && item.getBodyPart() == L2Item.SLOT_LR_HAND)
					env.value += 5 * (enchant + overenchant);
				else
					env.value += 4 * (enchant + overenchant);
				break;
			case L2Item.CRYSTAL_B:
				if(itemType == WeaponType.BOW)
					env.value += 6 * (enchant + overenchant);
				else if((itemType == WeaponType.DUALFIST || itemType == WeaponType.DUAL || itemType == WeaponType.BIGSWORD || itemType == WeaponType.SWORD) && item.getItem().getBodyPart() == L2Item.SLOT_LR_HAND)
					env.value += 4 * (enchant + overenchant);
				else
					env.value += 3 * (enchant + overenchant);
				break;
			case L2Item.CRYSTAL_C:
				if(itemType == WeaponType.BOW)
					env.value += 6 * (enchant + overenchant);
				else if((itemType == WeaponType.DUALFIST || itemType == WeaponType.DUAL || itemType == WeaponType.BIGSWORD || itemType == WeaponType.SWORD) && item.getItem().getBodyPart() == L2Item.SLOT_LR_HAND)
					env.value += 4 * (enchant + overenchant);
				else
					env.value += 3 * (enchant + overenchant);
				break;
			case L2Item.CRYSTAL_D:
				if(itemType == WeaponType.BOW)
					env.value += 4 * (enchant + overenchant);
				else
					env.value += 2 * (enchant + overenchant);
				break;
		}
		return;
	}
}