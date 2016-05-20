package commands.voiced;

import static com.lineage.game.model.L2Zone.ZoneType.OlympiadStadia;
import static com.lineage.game.model.L2Zone.ZoneType.Siege;
import static com.lineage.game.model.L2Zone.ZoneType.no_restart;
import static com.lineage.game.model.L2Zone.ZoneType.no_summon;
import static com.lineage.game.model.L2Zone.ZoneType.offshore;

import java.sql.ResultSet;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.cache.Msg;
import com.lineage.game.handler.IVoicedCommandHandler;
import com.lineage.game.handler.VoicedCommandHandler;
import com.lineage.game.instancemanager.CoupleManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2World;
import com.lineage.game.model.entity.Couple;
import com.lineage.game.serverpackets.ConfirmDlg;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.SetupGauge;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.SkillTable;
import com.lineage.util.Location;

public class Wedding implements IVoicedCommandHandler, ScriptFile
{
	private static String[] _voicedCommands = {"divorce", "engage", "gotolove"};

	public boolean useVoicedCommand(String command, L2Player activeChar, String target)
	{
		if(command.startsWith("engage"))
			return engage(activeChar);
		else if(command.startsWith("divorce"))
			return divorce(activeChar);
		else if(command.startsWith("gotolove"))
			return goToLove(activeChar);
		return false;
	}

	public boolean divorce(L2Player activeChar)
	{
		if(activeChar.getPartnerId() == 0)
			return false;

		int _partnerId = activeChar.getPartnerId();
		int AdenaAmount = 0;

		if(activeChar.isMaried())
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.Divorced", activeChar));
			AdenaAmount = Math.abs(activeChar.getAdena() / 100 * Config.WEDDING_DIVORCE_COSTS - 10);
			activeChar.reduceAdena(AdenaAmount);
		}
		else
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.Disengaged", activeChar));

		activeChar.setMaried(false);
		activeChar.setPartnerId(0);
		Couple couple = CoupleManager.getInstance().getCouple(activeChar.getCoupleId());
		couple.divorce();
		couple = null;

		L2Player partner;
		partner = (L2Player) L2World.findObject(_partnerId);

		if(partner != null)
		{
			partner.setPartnerId(0);
			if(partner.isMaried())
				partner.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.PartnerDivorce", partner));
			else
				partner.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.PartnerDisengage", partner));
			partner.setMaried(false);

			// give adena
			if(AdenaAmount > 0)
				partner.addAdena(AdenaAmount);
		}
		return true;
	}

	public boolean engage(L2Player activeChar)
	{
		// check target
		if(activeChar.getTarget() == null)
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.NoneTargeted", activeChar));
			return false;
		}
		// check if target is a L2Player
		if( !activeChar.getTarget().isPlayer())
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.OnlyAnotherPlayer", activeChar));
			return false;
		}
		// check if player is already engaged
		if(activeChar.getPartnerId() != 0)
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.AlreadyEngaged", activeChar));
			if(Config.WEDDING_PUNISH_INFIDELITY)
			{
				activeChar.startAbnormalEffect(0x2000); // give player a Big
				// Head
				// lets recycle the sevensigns debuffs
				int skillId;

				int skillLevel = 1;

				if(activeChar.getLevel() > 40)
					skillLevel = 2;

				if(activeChar.isMageClass())
					skillId = 4361;
				else
					skillId = 4362;

				L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);

				if(activeChar.getEffectList().getEffectsBySkill(skill) == null)
				{
					skill.getEffects(activeChar, activeChar, false, false);
					SystemMessage sm = new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT);
					sm.addSkillName(skillId, (short) skillLevel);
					activeChar.sendPacket(sm);
				}
			}
			return false;
		}

		L2Player ptarget = (L2Player) activeChar.getTarget();

		// check if player target himself
		if(ptarget.getObjectId() == activeChar.getObjectId())
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.EngagingYourself", activeChar));
			return false;
		}

		if(ptarget.isMaried())
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.PlayerAlreadyMarried", activeChar));
			return false;
		}

		if(ptarget.getPartnerId() != 0)
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.PlayerAlreadyEngaged", activeChar));
			return false;
		}

		if(ptarget.isEngageRequest())
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.PlayerAlreadyAsked", activeChar));
			return false;
		}

		if(ptarget.getPartnerId() != 0)
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.PlayerAlreadyEngaged", activeChar));
			return false;
		}

		if(ptarget.getSex() == activeChar.getSex() && !Config.WEDDING_SAMESEX)
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.SameSex", activeChar));
			return false;
		}

		// check if target has player on friendlist
		boolean FoundOnFriendList = false;
		int objectId;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id=?");
			statement.setInt(1, ptarget.getObjectId());
			rset = statement.executeQuery();

			while(rset.next())
			{
				objectId = rset.getInt("friend_id");
				if(objectId == activeChar.getObjectId())
				{
					FoundOnFriendList = true;
					break;
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		if( !FoundOnFriendList)
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.NotInFriendlist", activeChar));
			return false;
		}

		ptarget.setEngageRequest(true, activeChar.getObjectId());
		// ptarget.sendMessage("Player "+activeChar.getName()+" wants to engage with you.");
		ptarget.sendPacket(new ConfirmDlg(SystemMessage.S1, 60000, 4).addString("Player " + activeChar.getName() + " asking you to engage. Do you want to start new relationship?"));
		return true;
	}

	public boolean goToLove(L2Player activeChar)
	{
		if( !activeChar.isMaried())
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.YoureNotMarried", activeChar));
			return false;
		}

		if(activeChar.isInZone(offshore) && activeChar.getReflection().getId() != 0)
		{
			activeChar.sendMessage(new CustomMessage("common.TryLater", activeChar));
			return false;
		}

		if(activeChar.getPartnerId() == 0)
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.PartnerNotInDB", activeChar));
			return false;
		}

		L2Player partner;
		partner = (L2Player) L2World.findObject(activeChar.getPartnerId());
		if(partner == null)
		{
			activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.PartnerOffline", activeChar));
			return false;
		}

		if(partner.isInZone(offshore) && partner.getReflection().getId() != 0)
		{
			activeChar.sendMessage(new CustomMessage("common.TryLater", activeChar));
			return false;
		}

		if(partner.isInOlympiadMode() || partner.isFestivalParticipant() || activeChar.isMovementDisabled() || activeChar.isMuted() || activeChar.isInOlympiadMode() || activeChar.getDuel() != null || activeChar.isFestivalParticipant() || partner.isInZone(no_summon))
		{
			activeChar.sendMessage(new CustomMessage("common.TryLater", activeChar));
			return false;
		}

		if(activeChar.isInParty() && activeChar.getParty().isInDimensionalRift() || partner.isInParty() && partner.getParty().isInDimensionalRift())
		{
			activeChar.sendMessage(new CustomMessage("common.TryLater", activeChar));
			return false;
		}

		if(activeChar.getTeleMode() != 0 || activeChar.getUnstuck() != 0 || activeChar.getReflection().getId() != 0)
		{
			activeChar.sendMessage(new CustomMessage("common.TryLater", activeChar));
			return false;
		}

		// "Нельзя вызывать персонажей в/из зоны свободного PvP"
		// "в зоны осад"
		// "на Олимпийский стадион"
		// "в зоны определенных рейд-боссов и эпик-боссов"
		if(partner.isInZoneBattle() || partner.isInZone(Siege) || partner.isInZoneIncludeZ(no_restart) || partner.isInZone(OlympiadStadia) || activeChar.isInZoneBattle() || activeChar.isInZone(Siege) || activeChar.isInZoneIncludeZ(no_restart) || activeChar.isInZone(OlympiadStadia) || partner.getReflection().getId() != 0 || partner.isInZone(no_summon))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}

		activeChar.abortCast();
		activeChar.abortAttack();
		activeChar.sendActionFailed();
		activeChar.stopMove();
		activeChar.block();
		activeChar.setUnstuck(1);

		int teleportTimer = Config.WEDDING_TELEPORT_INTERVAL * 1000;

		if(activeChar.getInventory().getAdena() < Config.WEDDING_TELEPORT_PRICE)
		{
			activeChar.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return false;
		}

		activeChar.reduceAdena(Config.WEDDING_TELEPORT_PRICE);

		activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Wedding.Teleport", activeChar).addNumber(teleportTimer / 60000));
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

		// SoE Animation section
		activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 1050, 1, teleportTimer, 0));
		activeChar.sendPacket(new SetupGauge(0, teleportTimer));
		// End SoE Animation section

		// continue execution later
		ThreadPoolManager.getInstance().scheduleAi(new EscapeFinalizer(activeChar, partner.getLoc()), teleportTimer, true);
		return true;
	}

	static class EscapeFinalizer implements Runnable
	{
		private L2Player _activeChar;
		private Location _loc;

		EscapeFinalizer(L2Player activeChar, Location loc)
		{
			_activeChar = activeChar;
			_loc = loc;
		}

		public void run()
		{
			if(_activeChar.isDead() || _activeChar.getUnstuck() == 0)
				return;
			_activeChar.unblock();
			_activeChar.setUnstuck(0);
			_activeChar.teleToLocation(_loc);
		}
	}

	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}

	public void onLoad()
	{
		if(Config.WEDDING_ALLOW_WEDDING)
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}