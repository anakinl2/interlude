package commands.admin;

import com.lineage.Config;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.PlaySound;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.util.Util;
import events.LastManStanding.Lms;
import events.TeamvsTeam.TeamvsTeam;
import events.korean.KoreanEvent;

public class AdminAdmin implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_admin, //
		admin_play_sounds, //
		admin_play_sound, //
		admin_silence, //
		admin_tradeoff, //
		admin_cfg, //
		admin_config, //
		admin_show_html, //
		admin_test
	}

	@SuppressWarnings("unchecked")
	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if( !activeChar.getPlayerAccess().Menu)
			return false;

		switch(command)
		{
			case admin_admin:
				AdminHelpPage.showHelpPage(activeChar, "admin.htm");
				break;
			case admin_play_sounds:
				if(wordList.length == 1)
					AdminHelpPage.showHelpPage(activeChar, "songs/songs.htm");
				else
					try
					{
						AdminHelpPage.showHelpPage(activeChar, "songs/songs" + wordList[1] + ".htm");
					}
					catch(StringIndexOutOfBoundsException e)
					{}
				break;
			case admin_play_sound:
				try
				{
					playAdminSound(activeChar, wordList[1]);
				}
				catch(StringIndexOutOfBoundsException e)
				{}
				break;
			case admin_silence:
				if(activeChar.getMessageRefusal()) // already in message refusal
				// mode
				{
					activeChar.unsetVar("gm_silence");
					activeChar.setMessageRefusal(false);
					activeChar.sendPacket(new SystemMessage(SystemMessage.MESSAGE_ACCEPTANCE_MODE));
				}
				else
				{
					if(Config.SAVE_GM_EFFECTS)
						activeChar.setVar("gm_silence", "true");
					activeChar.setMessageRefusal(true);
					activeChar.sendPacket(new SystemMessage(SystemMessage.MESSAGE_REFUSAL_MODE));
				}
				break;
			case admin_tradeoff:
				try
				{
					if(wordList[1].equalsIgnoreCase("on"))
					{
						activeChar.setTradeRefusal(true);
						activeChar.sendMessage("tradeoff enabled");
					}
					else if(wordList[1].equalsIgnoreCase("off"))
					{
						activeChar.setTradeRefusal(false);
						activeChar.sendMessage("tradeoff disabled");
					}
				}
				catch(Exception ex)
				{
					if(activeChar.getTradeRefusal())
						activeChar.sendMessage("tradeoff currently enabled");
					else
						activeChar.sendMessage("tradeoff currently disabled");
				}
				break;
			case admin_cfg:
			case admin_config:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //config parameter[=value]");
					return false;
				}
				activeChar.sendMessage(Config.HandleConfig(activeChar, Util.joinStrings(" ", wordList, 1)));
				break;
			case admin_show_html:
				String html = wordList[1];
				try
				{
					if(html != null)
						AdminHelpPage.showHelpPage(activeChar, html);
					else
						activeChar.sendMessage("Html page not found");
				}
				catch(Exception npe)
				{
					activeChar.sendMessage("Html page not found");
				}
				break;
			case admin_test:
				if(wordList.length < 2)
				{
					activeChar.sendMessage("USAGE: //test [eventid]  (tvt,lms,korean)");
					return false;
				}

				if(wordList[1].equals("tvt"))
					TeamvsTeam.init();

				if(wordList[1].equals("lms"))
					Lms.init();

				if(wordList[1].equals("korean"))
					KoreanEvent.init();

				if(wordList[1].equals("flame"))
				{					
					L2Character ct = (L2Character) activeChar.getTarget();
					ct.broadcastPacket(new MagicSkillUse(ct, ct, 347, 1, 0, 0));
				}
				
				//	activeChar.imanuub();
				//activeChar.scriptRequest("Do you want join a korean style pvp event?", "events.korean.KoreanEvent:addPlayer", new Object[0]);
				//activeChar.scriptRequest(new CustomMessage("scripts.events.lasthero.LastHero.AskPlayer", activeChar).toString(), "events.lasthero.LastHero:addPlayer", new Object[0]);

				//Lms.init();
				//activeChar.wtfpeacesiht();
				
				//KoreanEvent.init();
			//	System.out.println("Height: " + activeChar.getColHeight());
			//	System.out.println("Radius: " + activeChar.getColRadius());
			//	System.out.println("PAtk Distance: " + activeChar.getPhysicalAttackRange());
				break;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	public void playAdminSound(L2Player activeChar, String sound)
	{
		activeChar.broadcastPacket(new PlaySound(sound));
		AdminHelpPage.showHelpPage(activeChar, "admin.htm");
		activeChar.sendMessage("Playing " + sound + ".");
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