package l2d.game.clientpackets;

import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2ShortCut;
import l2d.game.model.L2Skill;
import l2d.game.model.base.L2EnchantSkillLearn;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.MagicSkillUse;
import l2d.game.serverpackets.ShortCutRegister;
import l2d.game.serverpackets.SkillList;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SkillTreeTable;
import l2d.util.Rnd;

/**
 * Format chdd
 * c: (id) 0xD0
 * h: (subid) 0x0F
 * d: skill id
 * d: skill lvl
 */
public class RequestExEnchantSkill extends L2GameClientPacket
{
	private int _skillID;
	private int _skillLvl;

	@Override
	public void readImpl()
	{
		_skillID = readD();
		_skillLvl = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.getLevel() < 76 || activeChar.getClassId().getLevel() < 4)
		{
			activeChar.sendMessage("You must have 3rd class change quest completed.");
			return;
		}

		L2NpcInstance trainer = activeChar.getLastNpc();
		if(trainer == null || activeChar.getDistance(trainer) > L2Character.INTERACTION_DISTANCE && !activeChar.isGM())
		{
			activeChar.sendActionFailed();
			return;
		}

		L2EnchantSkillLearn sl = SkillTreeTable.getSkillEnchant(_skillID, (short) _skillLvl);
		if(sl == null)
			return;
	
		short slevel = activeChar.getSkillLevel(_skillID);
		if(slevel == -1)
			return;

		int enchantLevel = getEnchantLevel();
		if(slevel >= enchantLevel)
			return;
		
		// Можем ли мы перейти с текущего уровня скилла на данную заточку
		/*if(slevel == sl.getBaseLevel() ? _skillLvl % 100 != 1 : slevel != enchantLevel - 1)
		{
			activeChar.sendMessage("Incorrect enchant level.");
			return;
		}*/

		L2Skill skill = SkillTable.getInstance().getInfo(_skillID, enchantLevel);
		if(skill == null)
		{
			activeChar.sendMessage("Internal error: not found skill level");
			return;
		}

		if(!(trainer.getTemplate().canTeach(activeChar.getClassId()) || trainer.getTemplate().canTeach(activeChar.getClassId().getParent(activeChar.getSex()))))
		{
			activeChar.sendMessage("Wrong teacher");
			return;
		}

		int requiredSp = sl.getSpCost() * SkillTreeTable.NORMAL_ENCHANT_COST_MULTIPLIER;
		long requiredExp = sl.getExpCost() * SkillTreeTable.NORMAL_ENCHANT_COST_MULTIPLIER;
		int rate = sl.getRate(activeChar);

		if(activeChar.getSp() >= requiredSp)
			if(activeChar.getExp() >= requiredExp)
			{
				if(_skillLvl == 101 || _skillLvl == 141) // only first lvl requires book (101, 201, 301 ...)
				{
					L2ItemInstance spb = activeChar.getInventory().getItemByItemId(SkillTreeTable.NORMAL_ENCHANT_BOOK);
					if(spb == null)
					{
						sendPacket(new SystemMessage(SystemMessage.ITEMS_REQUIRED_FOR_SKILL_ENCHANT_ARE_INSUFFICIENT));
						return;
					}
					activeChar.getInventory().destroyItem(spb, 1, true);
				}
			}
			else
			{
				sendPacket(new SystemMessage(SystemMessage.EXP_REQUIRED_FOR_SKILL_ENCHANT_IS_INSUFFICIENT));
				return;
			}
		else
		{
			sendPacket(new SystemMessage(SystemMessage.SP_REQUIRED_FOR_SKILL_ENCHANT_IS_INSUFFICIENT));
			return;
		}

		if(Rnd.chance(rate))
		{
			activeChar.addSkill(skill, true);
			activeChar.addExpAndSp(-1 * requiredExp, -1 * requiredSp, false, false);
			activeChar.sendPacket(new SystemMessage(SystemMessage.EXPERIENCE_HAS_DECREASED_BY_S1).addNumber((int) requiredExp));
			activeChar.sendPacket(new SystemMessage(SystemMessage.SP_HAS_DECREASED_BY_S1).addNumber(requiredSp));
			activeChar.sendPacket(new SystemMessage(SystemMessage.SUCCEEDED_IN_ENCHANTING_SKILL_S1).addSkillName(_skillID, (short) _skillLvl));
			activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 4326, 1, 0, 0));
		}
		else
		{
			activeChar.addSkill(SkillTable.getInstance().getInfo(_skillID, sl.getBaseLevel()), true);
			activeChar.sendPacket(new SystemMessage(SystemMessage.FAILED_IN_ENCHANTING_SKILL_S1).addSkillName(_skillID, (short) _skillLvl));
			activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 4320, 1, 0, 0));
		}

		trainer.showEnchantSkillList(activeChar);
		sendPacket(new SkillList(activeChar));
		updateSkillShortcuts(activeChar);
	}

	private int getEnchantLevel()
	{
		L2EnchantSkillLearn sl = SkillTreeTable.getSkillEnchant(_skillID, (short) _skillLvl);
		return SkillTreeTable.convertEnchantLevel(sl.getBaseLevel(), _skillLvl);
	}

	private void updateSkillShortcuts(L2Player player)
	{
		// update all the shortcuts to this skill
		for(L2ShortCut sc : player.getAllShortCuts())
			if(sc.id == _skillID && sc.type == L2ShortCut.TYPE_SKILL)
			{
				L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, _skillLvl);
				player.sendPacket(new ShortCutRegister(newsc));
				player.registerShortCut(newsc);
			}
	}
}