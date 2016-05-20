/**
 * 
 */
package l2d.game.model.entity.olympiad;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.Config;
import l2d.game.ThreadPoolManager;
import l2d.game.model.L2Player;
import com.lineage.util.Rnd;

public class OlympiadManager implements Runnable
{
	private Map<Integer, OlympiadGame> _olympiadInstances;

	public OlympiadManager()
	{
		_olympiadInstances = new FastMap<Integer, OlympiadGame>();
		Olympiad._manager = this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void run()
	{
		Olympiad._cycleTerminated = false;
		if(Olympiad.isOlympiadEnd())
		{
			Olympiad._cycleTerminated = true;
			return;
		}

		while(Olympiad.inCompPeriod())
		{
			if(Olympiad._nobles.size() == 0)
			{
				try
				{
					wait(60000);
				}
				catch(InterruptedException ex)
				{}
				continue;
			}

			int classBasedPgCount = 0;
			for(List<Integer> classList : Olympiad._classBasedRegisters.values())
				classBasedPgCount += classList.size();
			// _compStarted = true;
			while(Olympiad.inCompPeriod() && (Olympiad._gamesQueue.size() > 0 || classBasedPgCount + getClassGamesCount() * 2 >= Config.CLASS_GAME_MIN || Olympiad._nonClassBasedRegisters.size() + getNonClassGamesCount() * 2 > Config.NONCLASS_GAME_MIN))
			{
				int class_games = getClassGamesCount();
				int nonclass_games = getNonClassGamesCount();
				// first cycle do nothing
				for(OlympiadGameTask ogt : Olympiad._gamesQueue.values())
				{
					if(ogt == null || ogt.getGame() == null)
						continue;
					int i = ogt.getGame().getId();
					if(ogt.isTerminated())
					{
						// removes terminated games from the queue
						_olympiadInstances.remove(i);
						Olympiad._gamesQueue.remove(i);
						Olympiad._gamesQueueScheduled.remove(i);
						Olympiad.STADIUMS[i].setStadiaFree();
					}
					else if(ogt.getStatus() == BattleStatus.Begining)
					{
						if(ogt.getGame().getType() == CompType.CLASSED && class_games * 2 < Config.CLASS_GAME_MIN)
							continue;
						else if(ogt.getGame().getType() == CompType.NON_CLASSED && nonclass_games * 2 < Config.NONCLASS_GAME_MIN)
							continue;
						ScheduledFuture sf = ThreadPoolManager.getInstance().scheduleGeneral(ogt, 10);
						Olympiad._gamesQueueScheduled.put(i, sf);
					}
				}
				// set up the games queue
				for(int i = 0; i < Olympiad.STADIUMS.length; i++)
				{
					if(!existNextOpponents(Olympiad._nonClassBasedRegisters) && !existNextOpponents(getRandomClassList(Olympiad._classBasedRegisters)))
						break;
					if(Olympiad.STADIUMS[i] == null)
					{// FIXME
						System.out.println("[WARNING, OlympiadManager]: STADIUMS is NULL");
						System.out.println("[WARNING, OlympiadManager]: STADIUMS.length=" + Olympiad.STADIUMS.length);
						// System.out.println("[WARNING, OlympiadManager]: STADIUMS is FreetoUse?=" + Olympiad.STADIUMS[i].isFreeToUse());
						System.out.println("[WARNING, OlympiadManager]: STADIUMS ID=" + i);
						return;
					}
					if(Olympiad.STADIUMS[i].isFreeToUse())
						if(Olympiad._nonClassBasedRegisters.size() + getNonClassGamesCount() * 2 > Config.NONCLASS_GAME_MIN && existNextOpponents(Olympiad._nonClassBasedRegisters) && i % 2 == 0)
							try
							{
								_olympiadInstances.put(i, new OlympiadGame(i, CompType.NON_CLASSED, nextOpponents(Olympiad._nonClassBasedRegisters)));
								Olympiad._gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i), BattleStatus.Begining));
								Olympiad.STADIUMS[i].setStadiaBusy();
							}
							catch(Exception ex)
							{
								ex.printStackTrace();
								if(_olympiadInstances.get(i) != null)
								{
									for(L2Player player : _olympiadInstances.get(i).getPlayers())
									{
										player.sendMessage("Your olympiad registration was canceled due to an error");
										player.setIsInOlympiadMode(false);
										player.setOlympiadSide(-1);
										player.setOlympiadGameId(-1);
									}
									_olympiadInstances.remove(i);
								}
								if(Olympiad._gamesQueue.get(i) != null)
									Olympiad._gamesQueue.remove(i);
								if(Olympiad._gamesQueueScheduled.get(i) != null)
									Olympiad._gamesQueueScheduled.remove(i);
								Olympiad.STADIUMS[i].setStadiaFree();
								// try to reuse this stadia next time
								i--;
							}
						else if(classBasedPgCount + getClassGamesCount() * 2 >= Config.CLASS_GAME_MIN && existNextOpponents(getRandomClassList(Olympiad._classBasedRegisters)))
							try
							{
								_olympiadInstances.put(i, new OlympiadGame(i, CompType.CLASSED, nextOpponents(getRandomClassList(Olympiad._classBasedRegisters))));
								Olympiad._gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i), BattleStatus.Begining));
								Olympiad.STADIUMS[i].setStadiaBusy();
							}
							catch(Exception ex)
							{
								ex.printStackTrace();
								if(_olympiadInstances.get(i) != null)
								{
									for(L2Player player : _olympiadInstances.get(i).getPlayers())
									{
										player.sendMessage("Your olympiad registration was canceled due to an error");
										player.setIsInOlympiadMode(false);
										player.setOlympiadSide(-1);
										player.setOlympiadGameId(-1);
									}
									_olympiadInstances.remove(i);
								}
								if(Olympiad._gamesQueue.get(i) != null)
									Olympiad._gamesQueue.remove(i);
								if(Olympiad._gamesQueueScheduled.get(i) != null)
									Olympiad._gamesQueueScheduled.remove(i);
								Olympiad.STADIUMS[i].setStadiaFree();
								// try to reuse this stadia next time
								i--;
							}
				}
				// wait 30 sec for !stress the server
				try
				{
					wait(30000);
				}
				catch(InterruptedException e)
				{}
			}
			// wait 30 sec for !stress the server
			try
			{
				wait(30000);
			}
			catch(InterruptedException e)
			{}
		}

		// when comp time finish wait for all games terminated before execute the cleanup code
		boolean allGamesTerminated = false;
		// wait for all games terminated
		while(!allGamesTerminated)
		{
			try
			{
				wait(30000);
			}
			catch(InterruptedException e)
			{}

			if(Olympiad._gamesQueue.size() == 0)
				allGamesTerminated = true;
			else
				for(OlympiadGameTask game : Olympiad._gamesQueue.values())
					allGamesTerminated = allGamesTerminated || game.isTerminated();
		}
		Olympiad._cycleTerminated = true;
		// when all games terminated clear all
		Olympiad._gamesQueue.clear();
		Olympiad._gamesQueueScheduled.clear();

		_olympiadInstances.clear();
		Olympiad._classBasedRegisters.clear();
		Olympiad._nonClassBasedRegisters.clear();
		Olympiad._battleStarted = false;
	}

	protected OlympiadGame getOlympiadInstance(int index)
	{
		if(_olympiadInstances != null && _olympiadInstances.size() > 0 /* || _compStarted */)
			return _olympiadInstances.get(index);
		return null;
	}

	protected Map<Integer, OlympiadGame> getOlympiadGames()
	{
		return _olympiadInstances == null ? null : _olympiadInstances;
	}

	private FastList<Integer> getRandomClassList(Map<Integer, FastList<Integer>> list)
	{
		if(list.size() == 0)
			return null;

		Map<Integer, FastList<Integer>> tmp = new FastMap<Integer, FastList<Integer>>();
		int tmpIndex = 0;
		for(FastList<Integer> l : list.values())
		{
			tmp.put(tmpIndex, l);
			tmpIndex++;
		}

		FastList<Integer> rndList;
		int classIndex;
		if(tmp.size() == 1)
			classIndex = 0;
		else
			classIndex = Rnd.get(tmp.size());
		rndList = tmp.get(classIndex);
		return rndList;
	}

	private FastList<Integer> nextOpponents(FastList<Integer> list)
	{
		FastList<Integer> opponents = new FastList<Integer>();
		if(list.size() == 0)
			return opponents;
		int loopCount = list.size() / 2;

		int first;
		int second;

		if(loopCount < 1)
			return opponents;

		first = Rnd.get(list.size());
		opponents.add(list.get(first));
		removeOpponent(list.get(first));

		second = Rnd.get(list.size());
		opponents.add(list.get(second));
		removeOpponent(list.get(second));

		return opponents;

	}

	private void removeOpponent(Integer noble)
	{
		Olympiad._nonClassBasedRegisters.remove(noble);
		for(FastList<Integer> classed : Olympiad._classBasedRegisters.values())
			if(classed != null)
				classed.remove(noble);
	}

	private boolean existNextOpponents(FastList<Integer> list)
	{
		if(list == null)
			return false;
		if(list.size() == 0)
			return false;
		int loopCount = list.size() / 2;
		return loopCount >= 1;
	}

	public int getClassGamesCount()
	{
		int res = 0;
		for(OlympiadGameTask game : Olympiad._gamesQueue.values())
		{
			if(!Config.ADD_ALREADY_STARTED_GAMES && game.getStatus() != BattleStatus.Begining)
				continue;
			if(game.getGame().getType() == CompType.CLASSED)
				res++;
		}
		return res;
	}

	public int getNonClassGamesCount()
	{
		int res = 0;
		for(OlympiadGameTask game : Olympiad._gamesQueue.values())
		{
			if(!Config.ADD_ALREADY_STARTED_GAMES && game.getStatus() != BattleStatus.Begining)
				continue;
			if(game.getGame().getType() == CompType.NON_CLASSED)
				res++;
		}
		return res;
	}
}