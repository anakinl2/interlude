package l2d.game.communitybbs.Manager;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.game.communitybbs.BB.Forum;
import l2d.game.model.L2Player;

public class ForumsBBSManager extends BaseBBSManager
{
	private static Logger _log = Logger.getLogger(ForumsBBSManager.class.getName());
	private Map<Integer, Forum> _root;
	private List<Forum> _table;
	private static ForumsBBSManager _Instance;
	private int lastid = 1;

	public static ForumsBBSManager getInstance()
	{
		if(_Instance == null)
		{
			_Instance = new ForumsBBSManager();
			_Instance.load();
		}
		return _Instance;
	}

	public ForumsBBSManager()
	{
		_root = new FastMap<Integer, Forum>();
		_table = new FastList<Forum>();
	}

	public void addForum(Forum ff)
	{
		_table.add(ff);

		if(ff.getID() > lastid)
			lastid = ff.getID();
	}

	private void load()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT forum_id FROM forums WHERE forum_type=0");
			rset = statement.executeQuery();
			while(rset.next())
			{
				Forum f = new Forum(Integer.parseInt(rset.getString("forum_id")), null);
				_root.put(Integer.parseInt(rset.getString("forum_id")), f);
			}
		}
		catch(Exception e)
		{
			_log.warning("data error on Forum (root): " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	public Forum getForumByName(String Name)
	{
		for(Forum f : _table)
			if(f.getName().equals(Name))
				return f;

		return null;
	}

	public Forum CreateNewForum(String name, Forum parent, int type, int perm, int oid)
	{
		Forum forum;
		forum = new Forum(name, parent, type, perm, oid);
		forum.insertindb();
		return forum;
	}

	public int GetANewID()
	{
		lastid++;
		return lastid;
	}

	public Forum getForumByID(int idf)
	{
		for(Forum f : _table)
			if(f.getID() == idf)
				return f;
		return null;
	}

	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2Player activeChar)
	{}

	@Override
	public void parsecmd(String command, L2Player activeChar)
	{}
}