package com.lineage.game.model.entity.SevenSignsFestival;

import javolution.util.FastMap;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.model.entity.SevenSigns;

/**
 * The FestivalManager class is the main runner of all the festivals.
 * It is used for easier integration and management of all running festivals.
 */
public class FestivalManager implements Runnable
{
	private static final SevenSigns _signsInstance = SevenSigns.getInstance();
	private SevenSignsFestival.FestivalStatus _status;
	private long _elapsed;

	public FestivalManager(SevenSignsFestival.FestivalStatus status)
	{
		_status = status;
	}

	public FestivalManager(SevenSignsFestival.FestivalStatus status, long elapsed)
	{
		_status = status;
		_elapsed = elapsed;
	}

	@Override
	public synchronized void run()
	{
		// The manager shouldn't be running if Seal Validation is in effect.
		if(_signsInstance.isSealValidationPeriod())
			return;
		switch(_status)
		{
			case Begining:
				// If the next period is due to start before the end of this festival cycle, then don't run it.
				if(_signsInstance.getMilliToPeriodChange() < SevenSignsFestival.FESTIVAL_CYCLE_LENGTH)
					return;
				SevenSignsFestival.setFestivalInstances(new FastMap<Integer, L2DarknessFestival>());
				// Set the next start timers.
				SevenSignsFestival.setNextCycleStart();
				SevenSignsFestival.setNextFestivalStart(SevenSignsFestival.FESTIVAL_SIGNUP_TIME);
				SevenSignsFestival.sendMessageToAll("Festival Guide", "The main event will start in " + (SevenSignsFestival.getMinsToNextFestival() - 1) + " minutes. Please register now.");
				ThreadPoolManager.getInstance().scheduleGeneral(new FestivalManager(SevenSignsFestival.FestivalStatus.Started), SevenSignsFestival.FESTIVAL_SIGNUP_TIME);
				break;
			case Started:
				// Set the festival timer to 0, as it is just beginning.
				// Create the instances for the festivals in both Oracles,
				// but only if they have participants signed up for them.
				for(int i = 0; i < SevenSignsFestival.FESTIVAL_COUNT; i++)
				{
					if(SevenSignsFestival.getDuskFestivalParticipants().get(i) != null)
						SevenSignsFestival.getFestivalInstances().put(10 + i, new L2DarknessFestival(SevenSigns.CABAL_DUSK, i));
					if(SevenSignsFestival.getDawnFestivalParticipants().get(i) != null)
						SevenSignsFestival.getFestivalInstances().put(20 + i, new L2DarknessFestival(SevenSigns.CABAL_DAWN, i));
				}
				// Prevent future signups while festival is in progress.
				SevenSignsFestival.setFestivalInitialized(true);
				SevenSignsFestival.setNextFestivalStart(SevenSignsFestival.FESTIVAL_CYCLE_LENGTH);
				SevenSignsFestival.sendMessageToAll("Festival Guide", "The main event is now starting.");
				// Clear past participants, they can no longer register their score if not done so already.
				SevenSignsFestival.getDawnPreviousParticipants().clear();
				SevenSignsFestival.getDuskPreviousParticipants().clear();
				ThreadPoolManager.getInstance().scheduleGeneral(new FestivalManager(SevenSignsFestival.FestivalStatus.FirstSpawn), SevenSignsFestival.FESTIVAL_FIRST_SPAWN);
				break;
			case FirstSpawn:
				_elapsed = SevenSignsFestival.FESTIVAL_FIRST_SPAWN;
				// Participants can now opt to increase the challenge, if desired.
				SevenSignsFestival.setFestivalInProgress(true);
				// Sequentially set all festivals to begin, spawn the Festival Witch and notify participants.
				for(L2DarknessFestival festivalInst : SevenSignsFestival.getFestivalInstances().values())
				{
					festivalInst.festivalStart();
					festivalInst.sendMessageToParticipants("The festival is about to begin!");
				}
				ThreadPoolManager.getInstance().scheduleGeneral(new FestivalManager(SevenSignsFestival.FestivalStatus.FirstSwarm, _elapsed), SevenSignsFestival.FESTIVAL_FIRST_SWARM - SevenSignsFestival.FESTIVAL_FIRST_SPAWN);
				break;
			case FirstSwarm:
				_elapsed += SevenSignsFestival.FESTIVAL_FIRST_SWARM - SevenSignsFestival.FESTIVAL_FIRST_SPAWN;
				for(L2DarknessFestival festivalInst : SevenSignsFestival.getFestivalInstances().values())
					festivalInst.moveMonstersToCenter();
				ThreadPoolManager.getInstance().scheduleGeneral(new FestivalManager(SevenSignsFestival.FestivalStatus.SecondSpawn, _elapsed), SevenSignsFestival.FESTIVAL_SECOND_SPAWN - SevenSignsFestival.FESTIVAL_FIRST_SWARM);
				break;
			case SecondSpawn:
				// Spawn an extra set of monsters (archers) on the free platforms with a faster respawn when killed.
				for(L2DarknessFestival festivalInst : SevenSignsFestival.getFestivalInstances().values())
				{
					festivalInst.spawnFestivalMonsters(SevenSignsFestival.FESTIVAL_DEFAULT_RESPAWN / 2, 2);
					festivalInst.sendMessageToParticipants("The festival will end in " + (SevenSignsFestival.FESTIVAL_LENGTH - SevenSignsFestival.FESTIVAL_SECOND_SPAWN) / 1000 / 60 + " minute(s).");
				}
				_elapsed += SevenSignsFestival.FESTIVAL_SECOND_SPAWN - SevenSignsFestival.FESTIVAL_FIRST_SWARM;
				// After another short time period, again move all idle spawns to the center of the arena.
				ThreadPoolManager.getInstance().scheduleGeneral(new FestivalManager(SevenSignsFestival.FestivalStatus.SecondSwarm, _elapsed), SevenSignsFestival.FESTIVAL_SECOND_SWARM - SevenSignsFestival.FESTIVAL_SECOND_SPAWN);
				break;
			case SecondSwarm:
				for(L2DarknessFestival festivalInst : SevenSignsFestival.getFestivalInstances().values())
					festivalInst.moveMonstersToCenter();
				_elapsed += SevenSignsFestival.FESTIVAL_SECOND_SWARM - SevenSignsFestival.FESTIVAL_SECOND_SPAWN;
				// Stand by until the time comes for the chests to be spawned.
				ThreadPoolManager.getInstance().scheduleGeneral(new FestivalManager(SevenSignsFestival.FestivalStatus.ChestSpawn, _elapsed), SevenSignsFestival.FESTIVAL_CHEST_SPAWN - SevenSignsFestival.FESTIVAL_SECOND_SWARM);
				break;
			case ChestSpawn:
				// Spawn the festival chests, which enable the team to gain greater rewards for each chest they kill.
				for(L2DarknessFestival festivalInst : SevenSignsFestival.getFestivalInstances().values())
				{
					festivalInst.spawnFestivalMonsters(SevenSignsFestival.FESTIVAL_DEFAULT_RESPAWN, 3);
					festivalInst.sendMessageToParticipants("The chests have spawned! Be quick, the festival will end soon.");
				}
				_elapsed += SevenSignsFestival.FESTIVAL_CHEST_SPAWN - SevenSignsFestival.FESTIVAL_SECOND_SWARM;
				// Stand by and wait until it's time to end the festival.
				ThreadPoolManager.getInstance().scheduleGeneral(new FestivalManager(SevenSignsFestival.FestivalStatus.Ending, _elapsed), SevenSignsFestival.FESTIVAL_LENGTH - _elapsed);
				break;
			case Ending:
				// Participants can no longer opt to increase the challenge, as the festival will soon close.
				SevenSignsFestival.setFestivalInProgress(false);
				// Sequentially begin the ending sequence for all running festivals.
				for(L2DarknessFestival festivalInst : SevenSignsFestival.getFestivalInstances().values())
					festivalInst.festivalEnd();
				// Clear the participants list for the next round of signups.
				SevenSignsFestival.getDawnFestivalParticipants().clear();
				SevenSignsFestival.getDuskFestivalParticipants().clear();
				// Allow signups for the next festival cycle.
				SevenSignsFestival.setFestivalInitialized(false);
				SevenSignsFestival.getFestivalInstances().clear();
				SevenSignsFestival.sendMessageToAll("Festival Witch", "That will do! I'll move you to the outside soon.");
				SevenSignsFestival.setManagerInstance(null);
				break;
		}
	}
}
