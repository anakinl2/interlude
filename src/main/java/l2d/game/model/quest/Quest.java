package l2d.game.model.quest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2d.Config;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.game.ThreadPoolManager;
import l2d.game.instancemanager.QuestManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Spawn;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.tables.ItemTable;
import l2d.game.tables.NpcTable;
import l2d.game.tables.ReflectionTable;
import l2d.game.templates.L2Item;
import l2d.game.templates.L2NpcTemplate;
import l2d.util.Files;
import l2d.util.Location;
import l2d.util.Rnd;

public class Quest
{
	public static String SOUND_ITEMGET = "ItemSound.quest_itemget";
	public static String SOUND_ACCEPT = "ItemSound.quest_accept";
	public static String SOUND_MIDDLE = "ItemSound.quest_middle";
	public static String SOUND_FINISH = "ItemSound.quest_finish";
	public static String SOUND_GIVEUP = "ItemSound.quest_giveup";
	public static String SOUND_TUTORIAL = "ItemSound.quest_tutorial";
	public static String SOUND_JACKPOT = "ItemSound.quest_jackpot";
	public static String SOUND_HORROR2 = "SkillSound5.horror_02";
	public static String SOUND_BEFORE_BATTLE = "Itemsound.quest_before_battle";
	public static String SOUND_FANFARE_MIDDLE = "ItemSound.quest_fanfare_middle";
	public static String SOUND_FANFARE2 = "ItemSound.quest_fanfare_2";
	public static String SOUND_BROKEN_KEY = "ItemSound2.broken_key";
	public static String SOUND_ENCHANT_SUCESS = "ItemSound3.sys_enchant_sucess";
	public static String SOUND_ENCHANT_FAILED = "ItemSound3.sys_enchant_failed";
	public static String SOUND_ED_CHIMES05 = "AmdSound.ed_chimes_05";
	public static String SOUND_ARMOR_WOOD_3 = "ItemSound.armor_wood_3";
	public static String SOUND_ITEM_DROP_EQUIP_ARMOR_CLOTH = "ItemSound.item_drop_equip_armor_cloth";
	public static String SOUND_WARRIOR_SONG = "EtcSound.elcroki_song_full";

	protected static Logger _log = Logger.getLogger(Quest.class.getName());

	/** HashMap containing events from String value of the event */
	private static Map<String, Quest> _allEventsS = new FastMap<String, Quest>();

	/** HashMap containing lists of timers from the name of the timer */
	private static Map<String, FastList<QuestTimer>> _allEventTimers = new FastMap<String, FastList<QuestTimer>>();

	private ArrayList<Integer> _questitems = new ArrayList<Integer>();

	/**
	 * Этот метод для регистрации квестовых вещей, которые будут удалены
	 * при прекращении квеста, независимо от того, был он закончен или
	 * прерван. <strong>Добавлять сюда награды нельзя</strong>.
	 */
	public void addQuestItem(int[] ids)
	{
		for(int id : ids)
			if(id != 0)
				addQuestItem(id);
	}

	public void addQuestItem(short[] ids)
	{
		for(int id : ids)
			if(id != 0)
				addQuestItem(id);
	}

	/**
	 * Этот метод для регистрации квестовых вещей, которые будут удалены
	 * при прекращении квеста, независимо от того, был он закончен или
	 * прерван. <strong>Добавлять сюда награды нельзя</strong>.
	 */
	public void addQuestItem(int id)
	{
		L2Item i = null;
		try
		{
			i = ItemTable.getInstance().getTemplate(id);
		}
		catch(Exception e)
		{
			System.out.println("Warning: unknown item " + i + " (" + id + ") in quest drop in " + getName());
		}

		if(i == null || i.getType2() != L2Item.TYPE2_QUEST)
			if(Config.DEBUG)
				System.out.println("Warning: non-quest item " + i + " (" + id + ") in quest drop in " + getName());

		if(_questitems.contains(id))
			if(Config.DEBUG)
				System.out.println("Warning: " + i + " (" + id + ") multiple times in quest drop in " + getName());

		_questitems.add(id);
	}

	public ArrayList<Integer> getItems()
	{
		return _questitems;
	}

	/**
	 * Update informations regarding quest in database.<BR>
	 * <U><I>Actions :</I></U><BR>
	 * <LI>Get ID state of the quest recorded in object qs</LI>
	 * <LI>Save in database the ID state (with or without the star) for the variable called "&lt;state&gt;" of the quest</LI>
	 * 
	 * @param qs
	 *            : QuestState
	 */
	public static void updateQuestInDb(QuestState qs)
	{
		updateQuestVarInDb(qs, "<state>", qs.getStateId());
	}

	/**
	 * Insert in the database the quest for the player.
	 * 
	 * @param qs
	 *            : QuestState pointing out the state of the quest
	 * @param var
	 *            : String designating the name of the variable for the quest
	 * @param value
	 *            : String designating the value of the variable for the quest
	 */
	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO character_quests (char_id,name,var,value) VALUES (?,?,?,?)");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuest().getName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * Delete the player's quest from database.
	 * 
	 * @param qs
	 *            : QuestState pointing out the player's quest
	 */
	public static void deleteQuestInDb(QuestState qs)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuest().getName());
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not delete char quest:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * Delete a variable of player's quest from the database.
	 * 
	 * @param qs
	 *            : object QuestState pointing out the player's quest
	 * @param var
	 *            : String designating the variable characterizing the quest
	 */
	public static void deleteQuestVarInDb(QuestState qs, String var)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=? AND var=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuest().getName());
			statement.setString(3, var);
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not delete char quest:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * Return collection view of the values contains in the allEventS
	 * 
	 * @return Collection<Quest>
	 */
	public static Collection<Quest> findAllEvents()
	{
		return _allEventsS.values();
	}

	/**
	 * Add quests to the L2PCInstance of the player.<BR><BR>
	 * <U><I>Action : </U></I><BR>
	 * Add state of quests, drops and variables for quests in the HashMap _quest of L2Player
	 * 
	 * @param player
	 *            : Player who is entering the world
	 */
	public static void playerEnter(L2Player player)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		FiltredPreparedStatement invalidQuestData = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? and name=?");
			statement = con.prepareStatement("SELECT name,value FROM character_quests WHERE char_id=? AND var=?");
			statement.setInt(1, player.getObjectId());
			statement.setString(2, "<state>");
			rset = statement.executeQuery();
			while(rset.next())
			{
				String questId = rset.getString("name");
				String stateId = rset.getString("value");

				if(stateId.equalsIgnoreCase("Start")) // невзятый квест
				{
					invalidQuestData.setInt(1, player.getObjectId());
					invalidQuestData.setString(2, questId);
					invalidQuestData.executeUpdate();
					continue;
				}

				// Search quest associated with the ID
				Quest q = QuestManager.getQuest(questId);
				if(q == null)
				{
					if(!Config.DONTLOADQUEST)
						_log.warning("Unknown quest " + questId + " for player " + player.getName());
					continue;
				}

				// Create an object State containing the state of the quest
				State state = q._states.get(stateId);
				if(state == null)
				{
					if(Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestData.setInt(1, player.getObjectId());
						invalidQuestData.setString(2, questId);
						invalidQuestData.executeUpdate();
					}
					else
						_log.warning("Unknown state " + state + " in quest " + questId + " for player " + player.getName());
					continue;
				}
				// Create a new QuestState for the player that will be added to the player's list of quests
				new QuestState(q, player, state);
			}
			invalidQuestData.close();
			DatabaseUtils.closeDatabaseSR(statement, rset);

			// Get list of quests owned by the player from the DB in order to add variables used in the quest.
			statement = con.prepareStatement("SELECT name,var,value FROM character_quests WHERE char_id=?");
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			while(rset.next())
			{
				String questId = rset.getString("name");
				String var = rset.getString("var");
				String value = rset.getString("value");
				// Get the QuestState saved in the loop before
				QuestState qs = player.getQuestState(questId);
				if(qs == null)
					continue;
				// Add parameter to the quest
				qs.setInternal(var, value);
			}
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
		}
		finally
		{
			DatabaseUtils.closeStatement(invalidQuestData);
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		// events
		for(String name : _allEventsS.keySet())
			player.processQuestEvent(name, "enter");
	}

	protected final String _descr;

	protected final String _name;

	protected final boolean _party;

	protected final int _questId;

	protected State CREATED;
	protected State STARTED;
	protected State COMPLETED;

	protected Map<String, State> _states = new FastMap<String, State>();

	/**
	 * (Constructor)Add values to class variables and put the quest in HashMaps.
	 * 
	 * @param questId
	 *            : int pointing out the ID of the quest
	 * @param descr
	 *            : String for the description of the quest
	 * @param party
	 *            : boolean for the party quest drop
	 */
	public Quest(int questId, String descr, boolean party)
	{
		_questId = questId;
		_name = getClass().getSimpleName();
		_descr = descr;
		_party = party;

		CREATED = new State("Start", this);
		STARTED = new State("Started", this);
		COMPLETED = new State("Completed", this);

		if(questId != 0)
			QuestManager.addQuest(this);
		else
			_allEventsS.put(_name, this);
	}

	/**
	 * Add this quest to the list of quests that the passed mob will respond to
	 * for Attack Events.<BR>
	 * <BR>
	 * 
	 * @param attackId
	 * @return int : attackId
	 */
	public L2NpcTemplate addAttackId(int attackId)
	{
		return addEventId(attackId, QuestEventType.MOBGOTATTACKED);
	}

	public void addAttackId(int[] attackIds)
	{
		for(int attackId : attackIds)
			addAttackId(attackId);
	}

	/**
	 * Add this quest to the list of quests that the passed mob will respond to
	 * for the specified Event type.<BR>
	 * <BR>
	 * 
	 * @param npcId
	 *            : id of the NPC to register
	 * @param eventType
	 *            : type of event being registered
	 * @return int : npcId
	 */
	public L2NpcTemplate addEventId(int npcId, QuestEventType eventType)
	{
		try
		{
			L2NpcTemplate t = NpcTable.getTemplate(npcId);
			if(t != null)
				t.addQuestEvent(eventType, this);
			return t;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public void addKillId(int[] killIds)
	{
		for(int killid : killIds)
			addKillId(killid);
	}

	public void addKillId(Collection<Integer> killIds)
	{
		for(int killid : killIds)
			addKillId(killid);
	}

	/**
	 * Add this quest to the list of quests that the passed mob will respond to
	 * for Kill Events.<BR>
	 * <BR>
	 * 
	 * @param killId
	 * @return int : killId
	 */
	public L2NpcTemplate addKillId(int killId)
	{
		return addEventId(killId, QuestEventType.MOBKILLED);
	}

	/**
	 * Add this quest to the list of quests that the passed npc will respond to
	 * for Skill-Use Events.<BR>
	 * <BR>
	 * 
	 * @param npcId
	 *            : ID of the NPC
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate addSkillUseId(int npcId)
	{
		return addEventId(npcId, QuestEventType.MOB_TARGETED_BY_SKILL);
	}

	/**
	 * Add the quest to the NPC's startQuest
	 * Вызывает addTalkId
	 * 
	 * @param npcId
	 * @return L2NpcTemplate : Start NPC
	 */
	public L2NpcTemplate addStartNpc(int npcId)
	{
		addTalkId(npcId);
		return addEventId(npcId, QuestEventType.QUEST_START);
	}

	/**
	 * Add a state to the quest
	 * 
	 * @param state
	 * @return state added
	 */
	public State addState(State state)
	{
		_states.put(state.getName(), state);
		return state;
	}

	/**
	 * Add the quest to the NPC's first-talk (default action dialog)
	 * 
	 * @param npcId
	 * @return L2NpcTemplate : Start NPC
	 */
	public L2NpcTemplate addFirstTalkId(int npcId)
	{
		return addEventId(npcId, QuestEventType.NPC_FIRST_TALK);
	}

	public void addFirstTalkId(int[] npcIds)
	{
		for(int npcId : npcIds)
			addFirstTalkId(npcId);
	}

	public void addFirstTalkId(Collection<Integer> npcIds)
	{
		for(int npcId : npcIds)
			addFirstTalkId(npcId);
	}

	/**
	 * Add this quest to the list of quests that the passed npc will respond to
	 * for Talk Events.<BR>
	 * <BR>
	 * 
	 * @param talkId
	 *            : ID of the NPC
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate addTalkId(int talkId)
	{
		return addEventId(talkId, QuestEventType.QUEST_TALK);
	}

	public void addTalkId(int[] talkIds)
	{
		for(int talkId : talkIds)
			addTalkId(talkId);
	}

	public void addTalkId(Collection<Integer> talkIds)
	{
		for(int talkId : talkIds)
			addTalkId(talkId);
	}

	public void cancelQuestTimer(String name, L2NpcInstance npc, L2Player player)
	{
		QuestTimer timer = getQuestTimer(name, npc, player);
		if(timer != null)
			timer.cancel();
	}

	/**
	 * Return description of the quest
	 * 
	 * @return String
	 */
	public String getDescr()
	{
		return _descr;
	}

	/**
	 * Return name of the quest
	 * 
	 * @return String
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * Return ID of the quest
	 * 
	 * @return int
	 */
	public int getQuestIntId()
	{
		return _questId;
	}

	public QuestTimer getQuestTimer(String name, L2NpcInstance npc, L2Player player)
	{
		if(_allEventTimers.get(name) == null)
			return null;
		for(QuestTimer timer : _allEventTimers.get(name))
			if(timer.isMatch(this, name, npc, player))
				return timer;
		return null;
	}

	public FastList<QuestTimer> getQuestTimers(String name)
	{
		return _allEventTimers.get(name);
	}

	/**
	 * Return party state of quest
	 * 
	 * @return String
	 */
	public boolean isParty()
	{
		return _party;
	}

	/**
	 * Add a new QuestState to the database and return it.
	 * 
	 * @param player
	 * @return QuestState : QuestState created
	 */
	public QuestState newQuestState(L2Player player)
	{
		QuestState qs = new QuestState(this, player, CREATED);
		Quest.updateQuestInDb(qs);
		return qs;
	}

	public State getStateByID(String state_id)
	{
		if(state_id.equalsIgnoreCase("CREATED"))
			return CREATED;
		if(state_id.equalsIgnoreCase("STARTED"))
			return STARTED;
		if(state_id.equalsIgnoreCase("COMPLETED"))
			return COMPLETED;
		return null;
	}

	public void notifyAttack(L2NpcInstance npc, QuestState qs)
	{
		String res = null;
		try
		{
			res = onAttack(npc, qs);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
			return;
		}
		showResult(qs.getPlayer(), res);
	}

	public void notifyDeath(L2NpcInstance killer, L2Character victim, QuestState qs)
	{
		String res = null;
		try
		{
			res = onDeath(killer, victim, qs);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
			return;
		}
		showResult(qs.getPlayer(), res);
	}

	public void notifyEvent(String event, QuestState qs)
	{
		String res = null;
		try
		{
			res = onEvent(event, qs);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
			return;
		}
		showResult(qs.getPlayer(), res);
	}

	public void notifyKill(L2NpcInstance npc, QuestState qs)
	{
		String res = null;
		try
		{
			res = onKill(npc, qs);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
			return;
		}
		showResult(qs.getPlayer(), res);
	}

	/**
	 * Override the default NPC dialogs when a quest defines this for the given NPC
	 */
	public final boolean notifyFirstTalk(L2NpcInstance npc, L2Player player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch(Exception e)
		{
			showError(player, e);
			return true;
		}
		// if the quest returns text to display, display it. Otherwise, use the default npc text.
		return showResult(player, res);
	}

	public boolean notifyTalk(L2NpcInstance npc, QuestState qs)
	{
		String res = null;
		try
		{
			res = onTalk(npc, qs);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
			return true;
		}
		return showResult(qs.getPlayer(), res);
	}

	public boolean notifySkillUse(L2NpcInstance npc, L2Skill skill, QuestState qs)
	{
		String res = null;
		try
		{
			res = onSkillUse(npc, skill, qs);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
			return true;
		}
		return showResult(qs.getPlayer(), res);
	}

	public String onAttack(L2NpcInstance npc, QuestState qs)
	{
		return null;
	}

	public String onDeath(L2NpcInstance killer, L2Character victim, QuestState qs)
	{
		return null;
	}

	public String onEvent(String event, QuestState qs)
	{
		return null;
	}

	public String onKill(L2NpcInstance npc, QuestState qs)
	{
		return null;
	}

	public String onFirstTalk(L2NpcInstance npc, L2Player player)
	{
		return null;
	}

	public String onTalk(L2NpcInstance npc, QuestState qs)
	{
		return null;
	}

	public String onSkillUse(L2NpcInstance npc, L2Skill skill, QuestState qs)
	{
		return null;
	}

	public void onAbort(QuestState qs)
	{}

	public void removeQuestTimer(QuestTimer timer)
	{
		if(timer == null)
			return;
		FastList<QuestTimer> timers = getQuestTimers(timer.getName());
		if(timers == null)
			return;
		timers.remove(timer);
	}

	/**
	 * Show message error to player who has an access level greater than 0
	 * 
	 * @param player
	 *            : L2PcInstance
	 * @param t
	 *            : Throwable
	 */
	private void showError(L2Player player, Throwable t)
	{
		_log.log(Level.WARNING, "", t);
		if(player.getPlayerAccess().IsGM)
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.close();
			String res = "<html><body><title>Script error</title>" + sw.toString() + "</body></html>";
			showResult(player, res);
		}
	}

	/**
	 * Show HTML file to client
	 * 
	 * @param fileName
	 * @return String : message sent to client
	 */
	public String showHtmlFile(L2Player player, String fileName)
	{
		return showHtmlFile(player, fileName, null, (String[]) null); // (String[]) - затык для вызова корректного метода.
	}

	public String showHtmlFile(L2Player player, String fileName, String toReplace, String replaceWith)
	{
		return showHtmlFile(player, fileName, new String[] { toReplace }, new String[] { replaceWith });
	}

	public String showHtmlFile(L2Player player, String fileName, String toReplace[], String replaceWith[])
	{
		String content = null;

		// for scripts
		if(fileName.contains("/"))
			content = Files.read(fileName, player);
		else
		{
			String _path = getClass().toString();
			_path = _path.substring(6, _path.lastIndexOf(".")) + ".";
			content = Files.read("data/scripts/" + _path.replace(".", "/") + fileName, player);
		}

		if(content == null)
			content = "Can't find file '" + fileName + "'";

		if(player != null && player.getTarget() != null)
			content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));

		// Make a replacement inside before sending html to client
		if(toReplace != null && replaceWith != null && toReplace.length == replaceWith.length)
			for(int i = 0; i < toReplace.length; i++)
				content = content.replaceAll(toReplace[i], replaceWith[i]);

		// Send message to client if message not empty
		if(content != null && player != null)
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			player.sendPacket(npcReply);
		}

		return content;
	}

	/**
	 * Show a message to player.<BR><BR>
	 * <U><I>Concept : </I></U><BR>
	 * 3 cases are managed according to the value of the parameter "res" :<BR>
	 * <LI><U>"res" ends with string ".html" :</U> an HTML is opened in order to be shown in a dialog box</LI>
	 * <LI><U>"res" starts with tag "html" :</U> the message hold in "res" is shown in a dialog box</LI>
	 * <LI><U>"res" is null :</U> do not show any message</LI>
	 * <LI><U>"res" is empty string :</U> show default message</LI>
	 * <LI><U>otherwise :</U> the message hold in "res" is shown in chat box</LI>
	 * 
	 * @param qs
	 *            : QuestState
	 * @param res
	 *            : String pointing out the message to show at the player
	 * @return boolean, if false onFirstTalk show default npc message, if true - quest message
	 */
	private boolean showResult(L2Player player, String res)
	{
		if(res == null) // do not show message
			return true;
		if(res.isEmpty()) // show default npc message
			return false;
		if(res.startsWith("no_quest") || res.equalsIgnoreCase("noquest") || res.equalsIgnoreCase("no-quest"))
			showHtmlFile(player, "data/html/no-quest.htm");
		else if(res.equalsIgnoreCase("completed"))
			showHtmlFile(player, "data/html/completed-quest.htm");
		else if(res.endsWith(".htm"))
			showHtmlFile(player, res);
		else
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(res);
			player.sendPacket(npcReply);
		}
		return true;
	}

	/**
	 * Add a timer to the quest, if it doesn't exist already
	 * 
	 * @param name
	 *            : name of the timer (also passed back as "event" in onAdvEvent)
	 * @param time
	 *            : time in ms for when to fire the timer
	 * @param npc
	 *            : npc associated with this timer (can be null)
	 * @param player
	 *            : player associated with this timer (can be null)
	 */
	public void startQuestTimer(String name, long time, L2NpcInstance npc, L2Player player)
	{
		// Add quest timer if timer doesn't already exist
		FastList<QuestTimer> timers = getQuestTimers(name);
		if(timers == null)
		{
			timers = new FastList<QuestTimer>();
			timers.add(new QuestTimer(this, name, time, npc, player));
			_allEventTimers.put(name, timers);
		}
		// a timer with this name exists, but may not be for the same set of npc and player
		else // if there exists a timer with this name, allow the timer only if the [npc, player] set is unique
		// nulls act as wildcards
		if(getQuestTimer(name, npc, player) == null)
			timers.add(new QuestTimer(this, name, time, npc, player));
	}

	protected String str(long i)
	{
		return String.valueOf(i);
	}

	public static boolean isdigit(String s)
	{
		try
		{
			Double.parseDouble(s);
		}
		catch(NumberFormatException e)
		{
			return false;
		}
		return true;
	}

	// =========================================================
	// QUEST SPAWNS
	// =========================================================

	public class DeSpawnScheduleTimerTask implements Runnable
	{
		L2NpcInstance _npc = null;

		public DeSpawnScheduleTimerTask(L2NpcInstance npc)
		{
			_npc = npc;
		}

		@Override
		public void run()
		{
			if(_npc != null && _npc.getSpawn() != null)
				_npc.getSpawn().despawnAll();
		}
	}

	public L2NpcInstance addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay)
	{
		L2NpcInstance result = null;
		try
		{
			L2NpcTemplate template = NpcTable.getTemplate(npcId);
			if(template != null)
			{
				// Sometimes, even if the quest script specifies some xyz (for example npc.getX() etc) by the time the code
				// reaches here, xyz have become 0! Also, a questdev might have purposely set xy to 0,0...however,
				// the spawn code is coded such that if x=y=0, it looks into location for the spawn loc! This will NOT work
				// with quest spawns! For both of the above cases, we need a fail-safe spawn. For this, we use the
				// default spawn location, which is at the player's loc.
				if(x == 0 && y == 0)
				{
					_log.log(Level.SEVERE, "Failed to adjust bad locks for quest spawn!  Spawn aborted!");
					return null;
				}
				if(randomOffset)
				{
					int offset;

					offset = Rnd.get(2); // Get the direction of the offset
					if(offset == 0)
						offset = -1; // make offset negative
					offset *= Rnd.get(50, 100);
					x += offset;

					offset = Rnd.get(2); // Get the direction of the offset
					if(offset == 0)
						offset = -1; // make offset negative
					offset *= Rnd.get(50, 100);
					y += offset;
				}
				L2Spawn spawn = new L2Spawn(template);
				spawn.setHeading(heading);
				spawn.setLocx(x);
				spawn.setLocy(y);
				spawn.setLocz(z + 20);
				spawn.setAmount(1);
				spawn.stopRespawn();
				result = spawn.doSpawn(true);

				if(despawnDelay > 0)
					ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnScheduleTimerTask(result), despawnDelay);

				return result;
			}
		}
		catch(Exception e1)
		{
			_log.warning("Could not spawn Npc " + npcId);
		}

		return null;
	}

	public static L2NpcInstance addSpawnToInstance(int npcId, Location loc, boolean randomOffset, int refId)
	{
		L2NpcInstance result = null;
		try
		{
			L2NpcTemplate template = NpcTable.getTemplate(npcId);
			if(template != null)
			{
				if(randomOffset)
					loc.rnd(50, 100);
				L2Spawn spawn = new L2Spawn(template);
				spawn.setLoc(loc);
				spawn.setAmount(1);
				spawn.stopRespawn();
				spawn.setReflection(refId);
				ReflectionTable.getInstance().get(refId).addSpawn(spawn);
				result = spawn.doSpawn(true);
				return result;
			}
		}
		catch(Exception e1)
		{
			_log.warning("Could not spawn Npc " + npcId);
		}
		return null;
	}
}