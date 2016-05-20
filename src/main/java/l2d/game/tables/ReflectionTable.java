package l2d.game.tables;

import java.util.HashMap;

import javolution.util.FastMap;
import l2d.game.model.Reflection;

public class ReflectionTable
{
	private static ReflectionTable _instance;
	private static Reflection _default = new Reflection(0);

	public static ReflectionTable getInstance()
	{
		if(_instance == null)
			_instance = new ReflectionTable();
		return _instance;
	}

	private HashMap<Integer, Reflection> _list = new HashMap<Integer, Reflection>();
	private FastMap<Integer, Integer> _soloKamalokaList = new FastMap<Integer, Integer>();

	public void addReflection(Reflection r)
	{
		_list.put(r.getId(), r);
	}

	public void removeSoloKamaloka(Integer player)
	{
		_soloKamalokaList.remove(player);
	}

	public void removeReflection(int id)
	{
		_list.remove(id);
	}

	public Reflection get(int id, boolean CreateIfNonExist)
	{
		Reflection r = _list.get(id);
		if(CreateIfNonExist && r == null)
			r = new Reflection(id);
		return r;
	}

	public Reflection get(int id)
	{
		return _list.get(id);
	}

	public Reflection getDefault()
	{
		return _default;
	}
}