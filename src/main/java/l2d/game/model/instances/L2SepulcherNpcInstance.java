package l2d.game.model.instances;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastMap;
import l2d.game.ThreadPoolManager;
import l2d.game.ai.CtrlIntention;
import l2d.game.instancemanager.FourSepulchersManager;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.quest.Quest;
import l2d.game.model.quest.QuestEventType;
import l2d.game.serverpackets.MyTargetSelected;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.serverpackets.NpcSay;
import l2d.game.serverpackets.SocialAction;
import l2d.game.serverpackets.StatusUpdate;
import l2d.game.serverpackets.ValidateLocation;
import l2d.game.tables.DoorTable;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.GArray;
import com.lineage.util.Rnd;

/**
 * L2SepulcherNpcInstance
 * 
 * @author: Ameron
 */
public class L2SepulcherNpcInstance extends L2NpcInstance
{
	protected static Map<Integer, Integer> _hallGateKeepers = new FastMap<Integer, Integer>();
	protected ScheduledFuture<?> _closeTask;
	protected ScheduledFuture<?> _spawnNextMysteriousBoxTask;
	protected ScheduledFuture<?> _spawnMonsterTask;
	private final static String HTML_FILE_PATH = "data/html/SepulcherNpc/";
	private final static int HALLS_KEY = 7260;

	private static final Logger _log = Logger.getLogger(L2SepulcherNpcInstance.class.getName());

	public L2SepulcherNpcInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);

		if(_closeTask != null)
			_closeTask.cancel(true);
		if(_spawnNextMysteriousBoxTask != null)
			_spawnNextMysteriousBoxTask.cancel(true);
		if(_spawnMonsterTask != null)
			_spawnMonsterTask.cancel(true);
		_closeTask = null;
		_spawnNextMysteriousBoxTask = null;
		_spawnMonsterTask = null;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
	}

	@Override
	public void deleteMe()
	{
		if(_closeTask != null)
		{
			_closeTask.cancel(true);
			_closeTask = null;
		}
		if(_spawnNextMysteriousBoxTask != null)
		{
			_spawnNextMysteriousBoxTask.cancel(true);
			_spawnNextMysteriousBoxTask = null;
		}
		if(_spawnMonsterTask != null)
		{
			_spawnMonsterTask.cancel(true);
			_spawnMonsterTask = null;
		}
		super.deleteMe();
	}

	@Override
	public void onAction(L2Player player)
	{
		if(player == null)
			return;

		// Check if the L2Player already target the L2NpcInstance
		if(this != player.getTarget())
		{
			// Set the target of the L2Player player
			player.setTarget(this);

			// Check if the player is attackable (without a forced attack)
			if(isAutoAttackable(player))
			{
				MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
				player.sendPacket(my);

				// Send a Server->Client packet StatusUpdate of the
				// L2NpcInstance to the L2Player to update its HP bar
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			else
			{
				// Send a Server->Client packet MyTargetSelected to the
				// L2Player player
				MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
				player.sendPacket(my);
			}

			// Send a Server->Client packet ValidateLocation to correct the
			// L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Check if the player is attackable (without a forced attack) and
			// isn't dead
			if(isAutoAttackable(player) && !isAlikeDead())
				// Check the height difference
				if(Math.abs(player.getZ() - getZ()) < 400)
					// Set the L2Player Intention to AI_INTENTION_ATTACK
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				else
					// Send a Server->Client packet ActionFailed (target is out
					// of attack range) to the L2Player player
					player.sendActionFailed();

			if(!isAutoAttackable(player))
				// Calculate the distance between the L2Player and the
				// L2NpcInstance
				if(!isInRange(player, 200))
					// Notify the L2Player AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				else
				{
					// Send a Server->Client packet SocialAction to the all
					// L2Player on the _knownPlayer of the L2NpcInstance
					// to display a social action of the L2NpcInstance on their
					// client
					SocialAction sa = new SocialAction(getObjectId(), Rnd.get(8));
					broadcastPacket(sa);

					doAction(player);
				}
			// Send a Server->Client ActionFailed to the L2Player in order
			// to avoid that the client wait another packet
			player.sendActionFailed();
		}
	}

	private void doAction(L2Player player)
	{
		if(isDead())
		{
			player.sendActionFailed();
			return;
		}

		switch(getNpcId())
		{
			case 31468:
			case 31469:
			case 31470:
			case 31471:
			case 31472:
			case 31473:
			case 31474:
			case 31475:
			case 31476:
			case 31477:
			case 31478:
			case 31479:
			case 31480:
			case 31481:
			case 31482:
			case 31483:
			case 31484:
			case 31485:
			case 31486:
			case 31487:
				setIsInvul(false);
				reduceCurrentHp(getMaxHp() + 1, player, null, false, false, false, false);
				if(_spawnMonsterTask != null)
					_spawnMonsterTask.cancel(true);
				_spawnMonsterTask = ThreadPoolManager.getInstance().scheduleEffect(new SpawnMonster(getNpcId()), 3500);
				break;

			case 31455:
			case 31456:
			case 31457:
			case 31458:
			case 31459:
			case 31460:
			case 31461:
			case 31462:
			case 31463:
			case 31464:
			case 31465:
			case 31466:
			case 31467:
				setIsInvul(false);
				reduceCurrentHp(getMaxHp() + 1, player, null, false, false, false, false);
				if(player.getParty() != null && !player.getParty().isLeader(player))
					player = player.getParty().getPartyLeader();
				player.getInventory().addItem(HALLS_KEY, 1, getObjectId(), "Quest"); // .addItem("Quest", HALLS_KEY, 1, player, true);
				break;

			default:
				Quest[] qlsa = getTemplate().getEventQuests(QuestEventType.QUEST_START);
				if(qlsa != null && qlsa.length > 0)
					player.setLastNpc(this);
				Quest[] qlst = getTemplate().getEventQuests(QuestEventType.NPC_FIRST_TALK);
				if(qlst != null && qlst.length == 1)
					qlst[0].notifyFirstTalk(this, player);
				else
					showChatWindow(player, 0);
		}
		player.sendActionFailed();
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return HTML_FILE_PATH + pom + ".htm";
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = getHtmlPath(getNpcId(), val);
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		player.sendActionFailed();
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(isBusy())
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/npcbusy.htm");
			html.replace("%busymessage%", getBusyMessage());
			html.replace("%npcname%", getName());
			html.replace("%playername%", player.getName());
			player.sendPacket(html);
		}
		else if(command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch(IndexOutOfBoundsException ioobe)
			{}
			catch(NumberFormatException nfe)
			{}
			showChatWindow(player, val);
		}
		else if(command.startsWith("open_gate"))
		{
			L2ItemInstance hallsKey = player.getInventory().getItemByItemId(HALLS_KEY);
			if(hallsKey == null)
				player.sendMessage("You dont have a key to open door....");
			else if(FourSepulchersManager.getInstance().isAttackTime())
			{
				switch(getNpcId())
				{
					case 31929:
					case 31934:
					case 31939:
					case 31944:
						FourSepulchersManager.getInstance().spawnShadow(getNpcId());
				}

				// Moved here from switch-default
				openNextDoor(getNpcId());
				if(player.getParty() != null)
				{
					for(L2Player mem : player.getParty().getPartyMembers())
						if(mem.getInventory().getItemByItemId(HALLS_KEY) != null)
							mem.getInventory().destroyItemByItemId(HALLS_KEY, mem.getInventory().getItemByItemId(HALLS_KEY).getCount(), true);
				}
				else
					player.getInventory().destroyItemByItemId(HALLS_KEY, hallsKey.getCount(), true);
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	public void openNextDoor(int npcId)
	{
		int doorId = FourSepulchersManager.getInstance().getHallGateKeepers().get(npcId);
		DoorTable _doorTable = DoorTable.getInstance();
		_doorTable.getDoor(doorId).openMe();

		if(_closeTask != null)
			_closeTask.cancel(true);
		_closeTask = ThreadPoolManager.getInstance().scheduleEffect(new CloseNextDoor(npcId, doorId), 10000);
	}

	private class CloseNextDoor implements Runnable
	{
		final DoorTable _DoorTable = DoorTable.getInstance();
		private int _NpcId;
		private int _DoorId;

		public CloseNextDoor(int npcId, int doorId)
		{
			_NpcId = npcId;
			_DoorId = doorId;
		}

		@Override
		public void run()
		{
			try
			{
				_DoorTable.getDoor(_DoorId).closeMe();
			}
			catch(Exception e)
			{
				_log.warning(e.getMessage());
			}

			if(_spawnNextMysteriousBoxTask != null)
				_spawnNextMysteriousBoxTask.cancel(true);
			_spawnNextMysteriousBoxTask = ThreadPoolManager.getInstance().scheduleEffect(new SpawnNextMysteriousBox(_NpcId), 10000);
		}
	}

	private class SpawnNextMysteriousBox implements Runnable
	{
		private int _NpcId;

		public SpawnNextMysteriousBox(int npcId)
		{
			_NpcId = npcId;
		}

		@Override
		public void run()
		{
			FourSepulchersManager.getInstance().spawnMysteriousBox(_NpcId);
		}
	}

	private class SpawnMonster implements Runnable
	{
		private int _NpcId;

		public SpawnMonster(int npcId)
		{
			_NpcId = npcId;
		}

		@Override
		public void run()
		{
			FourSepulchersManager.getInstance().spawnMonster(_NpcId);
		}
	}

	public void sayInShout(String msg)
	{
		if(msg == null || msg.isEmpty())
			return;// wrong usage

		GArray<L2Player> knownPlayers = L2World.getAroundPlayers(this, 15000, 15000); // getAllPlayers();
		if(knownPlayers == null || knownPlayers.isEmpty())
			return;
		NpcSay sm = new NpcSay(this, 0, msg);
		for(L2Player player : knownPlayers)
		{
			if(player == null)
				continue;
			player.sendPacket(sm);
		}
	}

	public void showHtmlFile(L2Player player, String file)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/SepulcherNpc/" + file);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}