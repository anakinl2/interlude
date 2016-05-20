package commands.admin;

import javolution.util.FastList;
import com.lineage.Config;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.L2WorldRegion;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.Earthquake;
import l2d.game.serverpackets.NpcInfo;
import l2d.game.serverpackets.SocialAction;
import l2d.game.tables.SkillTable;
import com.lineage.util.Rnd;
import com.lineage.util.Util;

public class AdminEffects implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_invis, //
		admin_vis, //
		admin_earthquake, //
		admin_bighead, //
		admin_shrinkhead, //
		admin_unpara_all, //
		admin_para_all, //
		admin_unpara, //
		admin_para, //
		admin_changename, //
		admin_gmspeed, //
		admin_invul, //
		admin_setinvul, //
		admin_social, //
		admin_abnormal,
	}

	@SuppressWarnings("unchecked")
	public boolean useAdminCommand(final Enum comm, final String[] wordList, final String fullString, final L2Player activeChar)
	{
		final Commands command = (Commands) comm;

		if( !activeChar.getPlayerAccess().GodMode)
			return false;

		int val;
		L2Object target = activeChar.getTarget();

		switch(command)
		{
			case admin_invis:
			case admin_vis:
				if(activeChar.isInvisible())
				{
					activeChar.setInvisible(false);
					activeChar.broadcastUserInfo(true);
					if(activeChar.getPet() != null)
						activeChar.getPet().broadcastPetInfo();
				}
				else
				{
					activeChar.setInvisible(true);
					activeChar.sendUserInfo(true);
					if(activeChar.getCurrentRegion() != null)
						for(final L2WorldRegion neighbor : activeChar.getCurrentRegion().getNeighbors())
							neighbor.removePlayerFromOtherPlayers(activeChar);
				}
				break;
			case admin_earthquake:
				try
				{
					final int intensity = Integer.parseInt(wordList[1]);
					final int duration = Integer.parseInt(wordList[2]);
					activeChar.broadcastPacket(new Earthquake(activeChar.getLoc(), intensity, duration));
				}
				catch(final Exception e)
				{
					activeChar.sendMessage("USAGE: //earthquake intensity duration");
					return false;
				}
				break;
			case admin_bighead:
			case admin_shrinkhead:
				if(target == null || !target.isCharacter())
				{
					activeChar.sendPacket(Msg.INVALID_TARGET);
					return false;
				}
				((L2Character) target).setBigHead( !((L2Character) target).isBigHead());
				if(target.isPlayer())
					((L2Character) target).sendMessage("Admin changed your head type.");
				break;
			case admin_unpara_all:
				for(final L2Player player : L2World.getAroundPlayers(activeChar, 1250, 200))
				{
					player.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_1);
					player.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_2);
					player.setParalyzed(false);
				}
				break;
			case admin_para_all:
				val = wordList.length > 1 && wordList[1].equalsIgnoreCase("2") ? L2Character.ABNORMAL_EFFECT_HOLD_2 : L2Character.ABNORMAL_EFFECT_HOLD_1;
				for(final L2Player player : L2World.getAroundPlayers(activeChar, 1250, 200))
					if(player != null && !player.isGM())
					{
						player.startAbnormalEffect(val);
						player.setParalyzed(true);
					}
				break;
			case admin_unpara:
				if(target == null || !target.isCharacter())
				{
					activeChar.sendPacket(Msg.INVALID_TARGET);
					return false;
				}
				((L2Character) target).stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_1);
				((L2Character) target).stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_2);
				((L2Character) target).setParalyzed(false);
				break;
			case admin_para:
				val = wordList.length > 1 && wordList[1].equalsIgnoreCase("2") ? L2Character.ABNORMAL_EFFECT_HOLD_2 : L2Character.ABNORMAL_EFFECT_HOLD_1;
				if(target == null || !target.isCharacter())
				{
					activeChar.sendPacket(Msg.INVALID_TARGET);
					return false;
				}
				((L2Character) target).startAbnormalEffect(val);
				((L2Character) target).setParalyzed(true);
				break;
			case admin_changename:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //changename newName");
					return false;
				}
				if(target == null)
					target = activeChar;
				if( !target.isCharacter())
				{
					activeChar.sendPacket(Msg.INVALID_TARGET);
					return false;
				}
				final String oldName = ((L2Character) target).getName();
				final String newName = Util.joinStrings(" ", wordList, 1);
				if(target.isPlayer())
				{
					L2World.removeObject(target);
					target.decayMe();
				}
				((L2Character) target).setName(newName);

				if(target.isPlayer())
				{
					target.spawnMe();
					((L2Character) target).broadcastUserInfo(true);
				}
				else if(target.isNpc())
					((L2Character) target).broadcastPacket(new NpcInfo((L2NpcInstance) target, null));
				activeChar.sendMessage("Changed name from " + oldName + " to " + newName + ".");
				break;
			case admin_gmspeed:
				if(wordList.length < 2)
					val = 0;
				else
				{
					try
					{
						val = Integer.parseInt(wordList[1]);
					}
					catch(final Exception e)
					{
						activeChar.sendMessage("USAGE: //gmspeed value=[0..4]");
						return false;
					}
				}
				final FastList<L2Effect> superhaste = activeChar.getEffectList().getEffectsBySkillId(7029);
				final int sh_level = superhaste == null ? 0 : superhaste.isEmpty() ? 0 : superhaste.get(0).getSkill().getLevel();

				if(val == 0)
				{
					if(sh_level != 0)
						activeChar.doCast(SkillTable.getInstance().getInfo(7029, sh_level), activeChar, true); // снимаем еффект
					activeChar.unsetVar("gm_gmspeed");
				}
				else if(val >= 1 && val <= 4)
				{
					if(Config.SAVE_GM_EFFECTS)
						activeChar.setVar("gm_gmspeed", String.valueOf(val));
					if(val != sh_level)
					{
						if(sh_level != 0)
							activeChar.doCast(SkillTable.getInstance().getInfo(7029, sh_level), activeChar, true); // снимаем еффект
						activeChar.doCast(SkillTable.getInstance().getInfo(7029, val), activeChar, true);
					}
				}
				else
					activeChar.sendMessage("USAGE: //gmspeed value=[0..4]");
				break;
			case admin_invul:
				handleInvul(activeChar, activeChar);
				if(activeChar.isInvul())
				{
					if(Config.SAVE_GM_EFFECTS)
						activeChar.setVar("gm_invul", "true");
				}
				else
					activeChar.unsetVar("gm_invul");
				break;
			case admin_setinvul:
				if(target == null || !target.isPlayer())
				{
					activeChar.sendPacket(Msg.INVALID_TARGET);
					return false;
				}
				handleInvul(activeChar, (L2Player) target);
				break;
			case admin_social:
				if(wordList.length < 2)
					val = Rnd.get(1, 7);
				else
					try
					{
						val = Integer.parseInt(wordList[1]);
					}
					catch(final NumberFormatException nfe)
					{
						activeChar.sendMessage("USAGE: //social value");
						return false;
					}
				if(target == null || target == activeChar)
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), val));
				else if(target.isCharacter())
					((L2Character) target).broadcastPacket(new SocialAction(target.getObjectId(), val));
				break;
			case admin_abnormal:
				try
				{
					val = Integer.parseInt(wordList[1]);
					if(val < 1 || val > 8)
						throw new Exception();
				}
				catch(final Exception e)
				{
					activeChar.sendMessage("USAGE: //abnormal effect");
					return false;
				}
				activeChar.setCustomEffect(0x00800000 << val);
				activeChar.updateAbnormalEffect();
				break;
		}

		return true;
	}

	private void handleInvul(final L2Player activeChar, final L2Player target)
	{
		if(target.isInvul())
		{
			target.setIsInvul(false);
			if(target.getPet() != null)
				target.getPet().setIsInvul(false);
			activeChar.sendMessage(target.getName() + " is now mortal.");
		}
		else
		{
			target.setIsInvul(true);
			if(target.getPet() != null)
				target.getPet().setIsInvul(true);
			activeChar.sendMessage(target.getName() + " is now immortal.");
		}
	}

	@SuppressWarnings("unchecked")
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}