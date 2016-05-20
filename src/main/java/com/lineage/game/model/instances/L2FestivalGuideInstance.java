package com.lineage.game.model.instances;

import java.util.Calendar;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.model.L2Party;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.SevenSigns;
import com.lineage.game.model.entity.SevenSignsFestival.SevenSignsFestival;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.GArray;

/**
 * Festival of Darkness Guide (Seven Signs)
 * 
 * @author Tempy
 */
public final class L2FestivalGuideInstance extends L2NpcInstance
{
	// private static Logger _log = Logger.getLogger(L2FestivalGuideInstance.class.getName());

	protected int _festivalType;
	protected int _festivalOracle;

	/**
	 * @param template
	 */
	public L2FestivalGuideInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);

		switch(getNpcId())
		{
			case 31127:
			case 31132:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_31;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				break;
			case 31128:
			case 31133:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_42;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				break;
			case 31129:
			case 31134:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_53;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				break;
			case 31130:
			case 31135:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_64;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				break;
			case 31131:
			case 31136:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_NONE;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				break;

			case 31137:
			case 31142:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_31;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				break;
			case 31138:
			case 31143:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_42;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				break;
			case 31139:
			case 31144:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_53;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				break;
			case 31140:
			case 31145:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_64;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				break;
			case 31141:
			case 31146:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_NONE;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				break;
		}

		if(getNpcId() == 31127)
			SevenSignsFestival.getInstance().setDawnChat(this);

		if(getNpcId() == 31137)
			SevenSignsFestival.getInstance().setDuskChat(this);
	}

	@Override
	public void onBypassFeedback(final L2Player player, final String command)
	{
		if(SevenSigns.getInstance().getPlayerCabal(player) == SevenSigns.CABAL_NULL)
		{
			player.sendMessage("You must be Seven Signs participant.");
			return;
		}

		if(command.startsWith("FestivalDesc"))
		{
			final int val = Integer.parseInt(command.substring(13));
			showChatWindow(player, val, null, true);
		}
		else if(command.startsWith("Festival"))
		{
			final L2Party playerParty = player.getParty();
			final int val = Integer.parseInt(command.substring(9, 10));

			switch(val)
			{
				case 1: // Become a Participant
					// Check if the festival period is active, if not then don't allow registration.
					if(SevenSigns.getInstance().getCurrentPeriod() != SevenSigns.PERIOD_COMPETITION)
					{
						showChatWindow(player, 2, "a", false);
						return;
					}

					// Check if a festival is in progress, then don't allow registration yet.
					if(SevenSignsFestival.getInstance().isFestivalInitialized())
					{
						player.sendMessage(new CustomMessage("l2d.game.model.instances.L2FestivalGuideInstance.InProgress", player));
						return;
					}

					// Check if the player is in a formed party already.
					if(playerParty == null)
					{
						showChatWindow(player, 2, "b", false);
						return;
					}

					// Check if the player is the party leader.
					if(!playerParty.isLeader(player))
					{
						showChatWindow(player, 2, "c", false);
						return;
					}

					// Check to see if the party has at least 5 members.
					if(playerParty.getMemberCount() < Config.FESTIVAL_MIN_PARTY_SIZE)
					{
						showChatWindow(player, 2, "b", false);
						return;
					}

					// Check if all the party members are in the required level range.
					if(playerParty.getLevel() > SevenSignsFestival.getMaxLevelForFestival(_festivalType))
					{
						showChatWindow(player, 2, "d", false);
						return;
					}

					/*
					 * Check to see if the player has already signed up,
					 * if they are then update the participant list providing all the
					 * required criteria has been met.
					 */
					if(player.isFestivalParticipant())
					{
						SevenSignsFestival.getInstance().setParticipants(_festivalOracle, _festivalType, playerParty);
						showChatWindow(player, 2, "f", false);
						return;
					}

					showChatWindow(player, 1, null, false);
					break;
				case 2: // Festival 2 xxxx
					final int stoneType = Integer.parseInt(command.substring(11));
					int stonesNeeded = 0;

					switch(stoneType)
					{
						case SevenSigns.SEAL_STONE_BLUE_ID:
							stonesNeeded = 4500;
							break;
						case SevenSigns.SEAL_STONE_GREEN_ID:
							stonesNeeded = 2700;
							break;
						case SevenSigns.SEAL_STONE_RED_ID:
							stonesNeeded = 1350;
							break;
					}

					final L2ItemInstance sealStoneInst = player.getInventory().getItemByItemId(stoneType);
					int stoneCount = 0;

					if(sealStoneInst != null)
						stoneCount = sealStoneInst.getIntegerLimitedCount();

					if(stoneCount < stonesNeeded)
					{
						player.sendMessage(new CustomMessage("l2d.game.model.instances.L2FestivalGuideInstance.NotEnoughSSType", player));
						return;
					}

					player.getInventory().destroyItem(sealStoneInst, stonesNeeded, true);

					final SystemMessage sm = new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED);
					sm.addNumber(stonesNeeded);
					sm.addItemName(stoneType);
					player.sendPacket(sm);

					SevenSignsFestival.getInstance().setParticipants(_festivalOracle, _festivalType, playerParty);
					SevenSignsFestival.getInstance().addAccumulatedBonus(_festivalType, stoneType, stonesNeeded);

					showChatWindow(player, 2, "e", false);
					break;
				case 3: // Score Registration
					// Check if the festival period is active, if not then don't register the score.
					if(SevenSigns.getInstance().isSealValidationPeriod())
					{
						showChatWindow(player, 3, "a", false);
						return;
					}

					// Check if a festival is in progress, if it is don't register the score.
					if(SevenSignsFestival.getInstance().isFestivalInProgress())
					{
						player.sendMessage(new CustomMessage("l2d.game.model.instances.L2FestivalGuideInstance.InProgressPoints", player));
						return;
					}

					// Check if the player is in a party.
					if(playerParty == null)
					{
						showChatWindow(player, 3, "b", false);
						return;
					}

					GArray<L2Player> prevParticipants = SevenSignsFestival.getInstance().getPreviousParticipants(_festivalOracle, _festivalType);

					// Check if there are any past participants.
					if(prevParticipants == null)
						return;

					// Check if this player was among the past set of participants for this festival.
					if(!prevParticipants.contains(player))
					{
						showChatWindow(player, 3, "b", false);
						return;
					}

					// Check if this player was the party leader in the festival.
					if(player.getObjectId() != prevParticipants.get(0).getObjectId())
					{
						showChatWindow(player, 3, "b", false);
						return;
					}

					final L2ItemInstance bloodOfferings = player.getInventory().getItemByItemId(SevenSignsFestival.FESTIVAL_OFFERING_ID);
					int offeringCount;

					// Check if the player collected any blood offerings during the festival.
					if(bloodOfferings == null)
					{
						player.sendMessage(new CustomMessage("l2d.game.model.instances.L2FestivalGuideInstance.BloodOfferings", player));
						return;
					}

					offeringCount = bloodOfferings.getIntegerLimitedCount();

					final int offeringScore = offeringCount * SevenSignsFestival.FESTIVAL_OFFERING_VALUE;
					final boolean isHighestScore = SevenSignsFestival.getInstance().setFinalScore(player, _festivalOracle, _festivalType, offeringScore);

					player.getInventory().destroyItem(bloodOfferings, offeringCount, true);

					// Send message that the contribution score has increased.
					player.sendPacket(new SystemMessage(SystemMessage.YOUR_CONTRIBUTION_SCORE_IS_INCREASED_BY_S1).addNumber(offeringScore));

					if(isHighestScore)
						showChatWindow(player, 3, "c", false);
					else
						showChatWindow(player, 3, "d", false);
					break;
				case 4: // Current High Scores
					final StringBuffer strBuffer = new StringBuffer("<html><body>Festival Guide:<br>These are the top scores of the week, for the ");

					final StatsSet dawnData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DAWN, _festivalType);
					final StatsSet duskData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DUSK, _festivalType);
					final StatsSet overallData = SevenSignsFestival.getInstance().getOverallHighestScoreData(_festivalType);

					final int dawnScore = dawnData.getInteger("score");
					final int duskScore = duskData.getInteger("score");
					int overallScore = 0;

					// If no data is returned, assume there is no record, or all scores are 0.
					if(overallData != null)
						overallScore = overallData.getInteger("score");

					strBuffer.append(SevenSignsFestival.getFestivalName(_festivalType) + " festival.<br>");

					if(dawnScore > 0)
						strBuffer.append("Dawn: " + calculateDate(dawnData.getString("date")) + ". Score " + dawnScore + "<br>" + dawnData.getString("members") + "<br>");
					else
						strBuffer.append("Dawn: No record exists. Score 0<br>");

					if(duskScore > 0)
						strBuffer.append("Dusk: " + calculateDate(duskData.getString("date")) + ". Score " + duskScore + "<br>" + duskData.getString("members") + "<br>");
					else
						strBuffer.append("Dusk: No record exists. Score 0<br>");

					if(overallScore > 0 && overallData != null)
					{
						String cabalStr = "Children of Dusk";
						if(overallData.getInteger("cabal") == SevenSigns.CABAL_DAWN)
							cabalStr = "Children of Dawn";
						strBuffer.append("Consecutive top scores: " + calculateDate(overallData.getString("date")) + ". Score " + overallScore + "<br>Affilated side: " + cabalStr + "<br>" + overallData.getString("members") + "<br>");
					}
					else
						strBuffer.append("Consecutive top scores: No record exists. Score 0<br>");

					strBuffer.append("<a action=\"bypass -h npc_" + getObjectId() + "_Chat 0\">Go back.</a></body></html>");

					final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
					html.setHtml(strBuffer.toString());
					player.sendPacket(html);
					break;
				case 8: // Increase the Festival Challenge
					if(playerParty == null)
						return;

					if(!SevenSignsFestival.getInstance().isFestivalInProgress())
						return;

					if(!playerParty.isLeader(player))
					{
						showChatWindow(player, 8, "a", false);
						break;
					}

					if(SevenSignsFestival.getInstance().increaseChallenge(_festivalOracle, _festivalType))
						showChatWindow(player, 8, "b", false);
					else
						showChatWindow(player, 8, "c", false);
					break;
				case 9: // Leave the Festival
					if(playerParty == null)
						return;

					/**
					 * If the player is the party leader, remove all participants from the festival
					 * (i.e. set the party to null, when updating the participant list)
					 * otherwise just remove this player from the "arena", and also remove them from the party.
					 */
					if(playerParty.isLeader(player))
						SevenSignsFestival.getInstance().updateParticipants(player, null);
					else if(playerParty.getMemberCount() > Config.FESTIVAL_MIN_PARTY_SIZE)
					{
						SevenSignsFestival.getInstance().updateParticipants(player, playerParty);
						playerParty.oustPartyMember(player);
					}
					else
						player.sendMessage("Only partyleader can leave festival, if minmum party member is reached.");
					break;
				default:
					showChatWindow(player, val, null, false);
			}
		}
		else
			// this class dont know any other commands, let forward
			// the command to the parent class
			super.onBypassFeedback(player, command);
	}

	private void showChatWindow(final L2Player player, final int val, final String suffix, final boolean isDescription)
	{
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH + "festival/";
		filename += isDescription ? "desc_" : "festival_";
		filename += suffix != null ? val + suffix + ".htm" : val + ".htm";
		final NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setFile(filename);
		html.replace("%festivalType%", SevenSignsFestival.getFestivalName(_festivalType));
		html.replace("%cycleMins%", String.valueOf(SevenSignsFestival.getInstance().getMinsToNextCycle()));
		if(val == 0)
			html.replace("%fame%", String.valueOf(SevenSignsFestival.getInstance().distribAccumulatedBonus(player)));
		// If the stats or bonus table is required, construct them.
		if(val == 5)
			html.replace("%statsTable%", getStatsTable());
		if(val == 6)
			html.replace("%bonusTable%", getBonusTable());
		player.sendPacket(html);
		player.sendActionFailed();
	}

	@Override
	public void showChatWindow(final L2Player player, final int val)
	{
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;

		switch(getNpcId())
		{
			// Dawn Festival Guides
			case 31127:
			case 31128:
			case 31129:
			case 31130:
			case 31131:
				filename += "festival/dawn_guide.htm";
				break;
			// Dusk Festival Guides
			case 31137:
			case 31138:
			case 31139:
			case 31140:
			case 31141:
				filename += "festival/dusk_guide.htm";
				break;
			// Festival Witches
			case 31132:
			case 31133:
			case 31134:
			case 31135:
			case 31136:
			case 31142:
			case 31143:
			case 31144:
			case 31145:
			case 31146:
				filename += "festival/festival_witch.htm";
				break;
			default:
				filename = getHtmlPath(getNpcId(), val);
		}

		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	private String getStatsTable()
	{
		final StringBuffer tableHtml = new StringBuffer();

		// Get the scores for each of the festival level ranges (types).
		for(int i = 0; i < 5; i++)
		{
			final int dawnScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DAWN, i);
			final int duskScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DUSK, i);
			final String festivalName = SevenSignsFestival.getFestivalName(i);
			String winningCabal = "Children of Dusk";

			if(dawnScore > duskScore)
				winningCabal = "Children of Dawn";
			else if(dawnScore == duskScore)
				winningCabal = "None";

			tableHtml.append("<tr><td width=\"100\" align=\"center\">" + festivalName + "</td><td align=\"center\" width=\"35\">" + duskScore + "</td><td align=\"center\" width=\"35\">" + dawnScore + "</td><td align=\"center\" width=\"130\">" + winningCabal + "</td></tr>");
		}

		return tableHtml.toString();
	}

	private String getBonusTable()
	{
		final StringBuffer tableHtml = new StringBuffer();

		// Get the accumulated scores for each of the festival level ranges (types).
		for(int i = 0; i < 5; i++)
		{
			final int accumScore = SevenSignsFestival.getInstance().getAccumulatedBonus(i);
			final String festivalName = SevenSignsFestival.getFestivalName(i);

			tableHtml.append("<tr><td align=\"center\" width=\"150\">" + festivalName + "</td><td align=\"center\" width=\"150\">" + accumScore + "</td></tr>");
		}

		return tableHtml.toString();
	}

	private String calculateDate(final String milliFromEpoch)
	{
		final long numMillis = Long.valueOf(milliFromEpoch);
		final Calendar calCalc = Calendar.getInstance();

		calCalc.setTimeInMillis(numMillis);

		return calCalc.get(Calendar.YEAR) + "/" + calCalc.get(Calendar.MONTH) + "/" + calCalc.get(Calendar.DAY_OF_MONTH);
	}
}
