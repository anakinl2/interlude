package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.base.L2EnchantSkillLearn;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.ExEnchantSkillInfo;
import com.lineage.game.serverpackets.PlaySound;
import com.lineage.game.tables.SkillTreeTable;

public class RequestExEnchantSkillInfo extends L2GameClientPacket
{
	private int _skillId;
	private int _skillLvl;

	@Override
	public void readImpl()
	{
		_skillId = readD();
		_skillLvl = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(_skillId == 0 && _skillLvl == 0)
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
		L2EnchantSkillLearn sl = SkillTreeTable.getSkillEnchant(_skillId, (short) _skillLvl);

		if(sl == null || sl.getId() != _skillId)
		{
			//_log.warning("enchant skill id " + _skillID + " level " + _skillLvl
			//    + " is undefined. aquireEnchantSkillInfo failed.");
			activeChar.sendMessage("This skill doesn't yet have enchant info in Datapack");
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

		ExEnchantSkillInfo asi = new ExEnchantSkillInfo(_skillId, _skillLvl, requiredSp, requiredExp, rate);

		if(sl.getLevel() == 101 || sl.getLevel() == 141) // only first lvl requires book
		{
			int spbId = 6622;
			asi.addRequirement(4, spbId, 1, 0);
		}
		sendPacket(asi);
		activeChar.sendPacket(new PlaySound("ItemSound.quest_itemget"));
	}
}