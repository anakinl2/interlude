package l2d.game.model.entity.olympiad;

import java.util.concurrent.ScheduledFuture;

import l2d.game.ThreadPoolManager;
import com.lineage.util.Log;

public class OlympiadGameTask implements Runnable
{
	private OlympiadGame _game;
	private BattleStatus _status;
	private int _count;

	private boolean _terminated = false;

	public boolean isTerminated()
	{
		return _terminated;
	}

	public BattleStatus getStatus()
	{
		return _status;
	}

	public int getCount()
	{
		return _count;
	}

	public OlympiadGame getGame()
	{
		return _game;
	}

	public OlympiadGameTask(OlympiadGame game, BattleStatus status)
	{
		_game = game;
		_status = status;
		_count = 0;
	}

	public OlympiadGameTask(OlympiadGame game, BattleStatus status, int count)
	{
		_game = game;
		_status = status;
		_count = count;
	}

	@Override
	public void run()
	{
		OlympiadGameTask task = null;
		ScheduledFuture sf = null;
		try
		{
			//_started = true;
			if(!Olympiad.inCompPeriod())
				return;
			if(_game != null)
				if(_game._aborted || _game.checkPlayersOnline())
				{
					switch(_status)
					{
						case Begining:
						{
							_game.sendMessageToPlayers(false, 120);
							task = new OlympiadGameTask(_game, BattleStatus.Begin_Countdown, 60);
							sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 60000);
							break;
						}
						case Begin_Countdown:
						{
							_game.sendMessageToPlayers(false, _count);
							if(_count == 60)
							{
								task = new OlympiadGameTask(_game, BattleStatus.Begin_Countdown, 30);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 30000);
							}
							else if(_count == 30)
							{
								task = new OlympiadGameTask(_game, BattleStatus.Begin_Countdown, 15);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 10000);
							}
							else if(_count == 15)
							{
								task = new OlympiadGameTask(_game, BattleStatus.Begin_Countdown, 5);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 5000);
							}
							else if(_count < 6 && _count > 1)
							{
								task = new OlympiadGameTask(_game, BattleStatus.Begin_Countdown, (_count - 1));
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 1000);
							}
							else if(_count == 1)
							{
								task = new OlympiadGameTask(_game, BattleStatus.PortPlayers);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 1000);
							}
							break;
						}
						case PortPlayers:
						{
							if(!_game.checkPlayersOnline())
							{
								Log.add("Error before players teleport for game " + _game.getId(), "olympiad");
								task = new OlympiadGameTask(_game, BattleStatus.ValidateWinner, 0);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 1000);
								break;
							}
							if(!_game.portPlayersToArena())
							{
								Log.add("Error on players teleport for game " + _game.getId(), "olympiad");
								task = new OlympiadGameTask(_game, BattleStatus.ValidateWinner, 1);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 10);
							}
							else
							{
								_game.InvisPlayers();
								_game.preparePlayers();
								task = new OlympiadGameTask(_game, BattleStatus.Started, 60);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 20000);
							}
							break;
						}
						case Started:
						{
							if(!_game.checkPlayersOnline())
							{
								task = new OlympiadGameTask(_game, BattleStatus.ValidateWinner, 0);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 1000);
								break;
							}

							if(_count == 60)
							{
								_game.additions();
								_game.setStarted((byte) 1);
							}
							_game.sendMessageToPlayers(true, _count);
							_count -= 10;
							if(_count > 10)
							{
								task = new OlympiadGameTask(_game, BattleStatus.Started, _count);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 10000);
								break;
							}
							synchronized (this)
							{
								//first battle of the perios set the battles started
								if(!Olympiad._battleStarted)
									Olympiad._battleStarted = true;
							}

							_game.InvisPlayers();
							task = new OlympiadGameTask(_game, BattleStatus.CountDown, 10);
							sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 10 * 1000);
							break;
						}
						case CountDown:
						{
							_game.sendMessageToPlayers(true, _count);
							_count--;
							if(_count == 0)
								task = new OlympiadGameTask(_game, BattleStatus.StartComp, 36);
							else
								task = new OlympiadGameTask(_game, BattleStatus.CountDown, _count);
							sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 1000);
							break;
						}
						case StartComp:
						{
							if(_count == 36)
							{
								_game.setStarted((byte) 2);
								_game.makeCompetitionStart();
							}
							//Wait 3 mins (Battle)
							if(_game._aborted || !_game.checkPlayersOnline())
							{
								task = new OlympiadGameTask(_game, BattleStatus.ValidateWinner, 0);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 1000);
							}
							else
							{
								_count--;
								if(_count == 0)
									task = new OlympiadGameTask(_game, BattleStatus.ValidateWinner, 0);
								else
									task = new OlympiadGameTask(_game, BattleStatus.StartComp, _count);
								sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 10000);
							}
							break;
						}
						case ValidateWinner:
						{
							try
							{
								_game.setStarted((byte) 0);
								_game.validateWinner(_count);
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
							task = new OlympiadGameTask(_game, BattleStatus.Ending);
							sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 20000);
							break;
						}
						case Ending:
						{
							_game.portPlayersBack();
							_game.clearSpectators();
							_game.deleteBuffers();
							_terminated = true;
							task = this;
							break;
						}
					}
					if(Olympiad.getGamesQueue().get(_game.getId()).getStatus() != BattleStatus.ValidateWinner || task != null && task.getStatus() == BattleStatus.Ending)
					{
						Olympiad.getGamesQueue().put(_game.getId(), task);
						Olympiad.getGamesQueueScheduled().put(_game.getId(), sf);
					}
					else if(sf != null)
						sf.cancel(true);
				}
				else
				{
					Log.add("Players is null for game " + _game.getId(), "olympiad");
					_game._aborted = true;
					task = new OlympiadGameTask(_game, BattleStatus.ValidateWinner);
					sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 1000);
					Olympiad.getGamesQueue().put(_game.getId(), task);
					Olympiad.getGamesQueueScheduled().put(_game.getId(), sf);
				}
		}
		catch(Exception e)
		{
			Log.add("Error for game " + _game.getId() + " :" + e.getMessage(), "olympiad");
			e.printStackTrace();
			_game._aborted = true;
			task = new OlympiadGameTask(_game, BattleStatus.ValidateWinner);
			sf = ThreadPoolManager.getInstance().scheduleGeneral(task, 1000);
			Olympiad.getGamesQueue().put(_game.getId(), task);
			Olympiad.getGamesQueueScheduled().put(_game.getId(), sf);
		}
	}
}
