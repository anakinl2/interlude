package l2d.game.clientpackets;

import com.lineage.Config;
import l2d.game.model.L2Character;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2ShortCut;
import l2d.game.model.L2Skill;
import l2d.game.model.L2SkillLearn;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.instances.L2VillageMasterInstance;
import l2d.game.serverpackets.AcquireSkillList;
import l2d.game.serverpackets.ExStorageMaxCount;
import l2d.game.serverpackets.PledgeShowInfoUpdate;
import l2d.game.serverpackets.PledgeStatusChanged;
import l2d.game.serverpackets.ShortCutRegister;
import l2d.game.serverpackets.SkillList;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.SkillSpellbookTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SkillTreeTable;
import com.lineage.util.Util;

public class RequestAquireSkill extends L2GameClientPacket
{
	// format: cddd(d)
	private int _id, _level, _skillType, _unk;

	@Override
	public void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = readD();
		if(_skillType == 3)
			_unk = readD();
	}

	@Override
	public void runImpl()
	{
		final L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		final L2NpcInstance trainer = activeChar.getLastNpc();
		if((trainer == null || activeChar.getDistance(trainer.getX(), trainer.getY()) > L2Character.INTERACTION_DISTANCE) && !activeChar.isGM())
			return;

		if(_skillType == 3)
			System.out.println(getType() + " :: skillType == 3 :: " + _unk + " // " + activeChar.toFullString());

		activeChar.setSkillLearningClassId(activeChar.getClassId());

		final L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);

		if(activeChar.getSkillLevel(_id) >= _level)
			return; // already knows the skill with this level

		if(_level > 1 && activeChar.getSkillLevel(_id) != _level - 1)
		{
			Util.handleIllegalPlayerAction(activeChar, "RequestAquireSkill[58]", "tried to increase skill " + _id + " level to " + _level + " while having it's level " + activeChar.getSkillLevel(_id), 1);
			return;
		}

		if(!(skill.isCommon() || SkillTreeTable.getInstance().isSkillPossible(activeChar, _id, _level)))
		{
			Util.handleIllegalPlayerAction(activeChar, "RequestAquireSkill[64]", "tried to learn skill " + _id + " while on class " + activeChar.getActiveClass(), 1);
			return;
		}

		final L2SkillLearn SkillLearn = SkillTreeTable.getSkillLearn(_id, _level, activeChar.getClassId(), _skillType == AcquireSkillList.CLAN ? activeChar.getClan() : null);

		if(SkillLearn.getItemCount() == -1)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(_skillType == AcquireSkillList.CLAN)
			learnClanSkill(skill, activeChar.getClan());
		else
		{
			final int _requiredSp = SkillTreeTable.getInstance().getSkillCost(activeChar, skill);

			if(activeChar.getSp() >= _requiredSp || SkillLearn.common || SkillLearn.transformation)
			{
				final Integer spb_id = SkillSpellbookTable.getSkillSpellbooks().get(SkillSpellbookTable.hashCode(new int[] { skill.getId(), skill.getLevel() }));

				if(spb_id != null)
				{
					final L2ItemInstance spb = activeChar.getInventory().getItemByItemId(spb_id);
					if(spb == null || spb.getIntegerLimitedCount() < SkillLearn.itemCount)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_SKILLS));
						return;
					}
					final L2ItemInstance ri = activeChar.getInventory().destroyItem(spb, SkillLearn.itemCount, true);

					if(ri.getItemId() != 57)
						activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(ri.getItemId()));
					else
						activeChar.sendPacket(new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED).addNumber(SkillLearn.itemCount));
				}
				activeChar.addSkill(skill, true);
				if(!SkillLearn.common && !SkillLearn.transformation)
					activeChar.setSp(activeChar.getSp() - _requiredSp);

				activeChar.updateStats();
				activeChar.sendUserInfo(true);

				// update all the shortcuts to this skill
				if(_level > 1)
					for(final L2ShortCut sc : activeChar.getAllShortCuts())
						if(sc.id == _id && sc.type == L2ShortCut.TYPE_SKILL)
						{
							final L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, _level);
							activeChar.sendPacket(new ShortCutRegister(newsc));
							activeChar.registerShortCut(newsc);
						}
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_LEARN_SKILLS));
				return;
			}
		}

		if(SkillLearn.common)
			activeChar.sendPacket(new ExStorageMaxCount(activeChar));
		activeChar.sendPacket(new SkillList(activeChar));

		if(trainer != null)
			if(_skillType == AcquireSkillList.USUAL)
				trainer.showSkillList(activeChar);
			else if(_skillType == AcquireSkillList.FISHING)
				trainer.showFishingSkillList(activeChar);
			else if(_skillType == AcquireSkillList.CLAN)
				trainer.showClanSkillList(activeChar);
	}

	private void learnClanSkill(final L2Skill skill, final L2Clan clan)
	{
		final L2Player player = getClient().getActiveChar();
		if(player == null || skill == null || clan == null)
			return;
		final L2NpcInstance trainer = player.getLastNpc();
		if(trainer == null)
			return;
		if(!(trainer instanceof L2VillageMasterInstance))
		{
			System.out.println("RequestAquireSkill.learnClanSkill, trainer isn't L2VillageMasterInstance");
			System.out.println(trainer.getName() + "[" + trainer.getNpcId() + "] Loc: " + trainer.getLoc());
			return;
		}
		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_IS_ENABLED));
			return;
		}
		final L2SkillLearn SkillLearn = SkillTreeTable.getSkillLearn(_id, _level, null, clan);
		final int requiredRep = SkillTreeTable.getInstance().getSkillRepCost(clan, skill);
		short itemId = 0;
		if(!Config.ALT_DISABLE_SPELLBOOKS)
			itemId = SkillLearn.itemId;
		if(skill.getMinPledgeClass() <= clan.getLevel() && clan.getReputationScore() >= requiredRep)
		{
			if(itemId > 0)
			{
				final L2ItemInstance spb = player.getInventory().getItemByItemId(itemId);
				if(spb == null)
				{
					// Haven't spellbook
					player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_SKILLS));
					return;
				}
				final L2ItemInstance ri = player.getInventory().destroyItem(spb, SkillLearn.itemCount, true);
				player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(ri.getItemId()));
			}
			clan.incReputation(-requiredRep, false, "AquireSkill");
			clan.addNewSkill(skill, true);
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addSkillName(_id, _level));
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
			clan.broadcastToOnlineMembers(new PledgeStatusChanged(clan));

			((L2VillageMasterInstance) trainer).showClanSkillWindow(player); // Maybe we shoud add a check here...
		}
		else
		{
			player.sendMessage("Your clan doesn't have enough reputation points to learn this skill");
			// sm = null;
			return;
		}

		// update all the shortcuts to this skill
		if(_level > 1)
			for(final L2ShortCut sc : player.getAllShortCuts())
				if(sc.id == _id && sc.type == L2ShortCut.TYPE_SKILL)
				{
					final L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, _level);
					player.sendPacket(new ShortCutRegister(newsc));
					player.registerShortCut(newsc);
				}

		clan.addAndShowSkillsToPlayer(player);
	}
}