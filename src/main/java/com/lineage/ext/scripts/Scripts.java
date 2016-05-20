package com.lineage.ext.scripts;

import java.io.File;
import java.lang.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.ext.scripts.jarloader.JarClassLoader;
import com.lineage.ext.scripts.Compiler.MemoryClassLoader;
import com.lineage.game.GameServer;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.instancemanager.QuestManager;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.quest.Quest;

public class Scripts
{
	public static boolean JAR;
	public static HashMap<Integer, ArrayList<ScriptClassAndMethod>> itemHandlers = new HashMap<>();
	public static HashMap<Integer, ArrayList<ScriptClassAndMethod>> dialogAppends = new HashMap<>();
	public static HashMap<String, ScriptClassAndMethod> onAction = new HashMap<>();
	public static ArrayList<ScriptClassAndMethod> onPlayerExit = new ArrayList<>();
	public static ArrayList<ScriptClassAndMethod> onPlayerEnter = new ArrayList<>();
	public static ArrayList<ScriptClassAndMethod> onReloadMultiSell = new ArrayList<>();
	public static ArrayList<ScriptClassAndMethod> onDie = new ArrayList<>();

	public static boolean loading;
	private static final Logger _log = Logger.getLogger(Scripts.class.getName());

	private static Scripts _instance;
	static HashMap<String, ScriptClassAndMethod> onActionShift = new HashMap<>();
	private HashMap<String, Script> _classes = new HashMap<>();

	public static Scripts getInstance()
	{
		if(_instance == null)
			new Scripts();
		return _instance;
	}

	public Scripts()
	{
		_instance = this;
		load(false);
	}

	public static Object callScriptsNoOwner(Script scriptClass, Method method)
	{
		return callScriptsNoOwner(scriptClass, method, null, null);
	}

	public static Object callScriptsNoOwner(Script scriptClass, Method method, Object[] args)
	{
		return callScriptsNoOwner(scriptClass, method, args, null);
	}

	private static Object callScriptsNoOwner(Script scriptClass, Method method, Object[] args, HashMap<String, Object> variables)
	{
		if(loading)
			return null;

		ScriptObject o;
		try
		{
			o = scriptClass.newInstance();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}

		if(variables != null && variables.size() > 0)
			for(Map.Entry<String, Object> obj : variables.entrySet())
				try
				{
					o.setProperty(obj.getKey(), obj.getValue());
				}
				catch(Exception e)
				{}

		try
		{
			o.setProperty("self", null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return args == null ? o.invokeMethod(method) : o.invokeMethod(method, args);
	}

	public static Object callScripts(String _class, String method, L2Object l2Object)
	{
		return callScripts(_class, method,l2Object, null, null);
	}

	public static Object callScripts(String _class, String method, L2Object l2Object, Object[] args)
	{
		return callScripts(_class, method,l2Object, args, null);
	}

	public static Object callScripts(String _class, String method, L2Object l2Object, Object[] args, HashMap<String, Object> variables)
	{
		if(Scripts.loading)
			return null;

		ScriptObject o;

		Script scriptClass = Scripts.getInstance().getClasses().get(_class);

		if(scriptClass == null)
		{
			_log.info("Script class " + _class + " not found");
			return null;
		}

		try
		{
			o = scriptClass.newInstance();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}

		if(variables != null && variables.size() > 0)
			for(Map.Entry<String, Object> obj : variables.entrySet())
				try
				{
					o.setProperty(obj.getKey(), obj.getValue());
				}
				catch(Exception e)
				{}

		try
		{
			o.setProperty("self", l2Object);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		Object ret = args == null ? o.invokeMethod(method) : o.invokeMethod(method, args);

		try
		{
			o.setProperty("self", null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return ret;
	}

	public static Object  callScripts(Script scriptClass, Method method, L2Object l2Object)
	{
		return callScripts(scriptClass, method,l2Object, null, null);
	}

	public static Object callScripts(Script scriptClass, Method method, L2Object l2Object, Object[] args)
	{
		return callScripts(scriptClass, method,l2Object, args, null);
	}

	public static Object callScripts(Script scriptClass, Method method, L2Object l2Object, Object[] args, HashMap<String, Object> variables)
	{
		if(loading)
			return null;

		ScriptObject o;
		try
		{
			o = scriptClass.newInstance();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}

		if(variables != null && variables.size() > 0)
			for(Map.Entry<String, Object> obj : variables.entrySet())
				try
				{
					o.setProperty(obj.getKey(), obj.getValue());
				}
				catch(Exception e)
				{}

		try
		{
			o.setProperty("self", l2Object);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		Object ret = args == null ? o.invokeMethod(method) : o.invokeMethod(method, args);

		try
		{
			o.setProperty("self", null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return ret;
	}

	private boolean load(boolean reload)
	{
		_log.info(" ~ Loading Scripts...");
		boolean error = false;
		Class<?> c;

		JAR = new File("./scripts.jar").exists();

		if(JAR)
		{
			JarClassLoader jcl;
			try
			{
				jcl = new JarClassLoader("./scripts.jar");
				for(String name : jcl.getClassNames())
				{
					if(!name.contains(".class"))
						continue;
					if(name.contains("$"))
						continue; // пропускаем вложенные классы
					name = name.replace(".class", "").replace("/", ".");
					c = jcl.loadClass(name);
					Script s = new Script(c);
					_classes.put(c.getName(), s);
				}
			}
			catch(Exception e)
			{
				error = true;
				e.printStackTrace();
			}
		}
		else
		{
			ArrayList<File> scriptFiles = new ArrayList<File>();
			parseClasses(new File("./data/scripts"), scriptFiles);
			if(Compiler.getInstance().compile(scriptFiles, System.out))
			{
				MemoryClassLoader classLoader = Compiler.getInstance().classLoader; // TODO
				for(String name : classLoader.byteCodes.keySet())
				{
					if(name.contains("$"))
						continue; // пропускаем вложенные классы
					try
					{
						c = classLoader.loadClass(name);
						Script s = new Script(c);
						_classes.put(name, s);
					}
					catch(ClassNotFoundException e)
					{
						_log.warning("Can't load script class:" + e.getMessage());
						error = true;
					}
				}
				Compiler.getInstance().classLoader = null;
			}
			else
			{
				_log.warning("Can't compile scripts!");
				error = true;
			}
		}

		if(error)
		{
			_log.info(" ~ Scripts loaded with errors. Loaded " + _classes.size() + " classes.");
			if(!reload)
				Runtime.getRuntime().halt(0);
		}
		else
			_log.info(" ~ Scripts successfully loaded. Loaded " + _classes.size() + " classes.");
		return error;
	}

	private void parseClasses(File f, ArrayList<File> list)
	{
		for(File z : f.listFiles())
			if(z.isDirectory())
			{
				if(z.isHidden() || z.getName().equals(".svn"))
					continue;
				if(Config.DONTLOADQUEST && z.getName().equals("quests") && z.getParentFile().getName().equals("scripts"))
					continue;
				parseClasses(z, list);
			}
			else
			{
				if(z.isHidden() || !z.getName().contains(".java"))
					continue;
				list.add(z);
			}
	}

	public HashMap<String, Script> getClasses()
	{
		return _classes;
	}

	public boolean reload()
	{
		loading = true;

		for(ScriptObject go : GameServer.scriptsObjects.values())
			try
			{
				go.invokeMethod("onReload");
			}
			catch(Exception f)
			{
				f.printStackTrace();
			}
		GameServer.scriptsObjects.clear();

		boolean error = load(true);
		callOnLoad();

		loading = false;
		return error;
	}

	public void callOnLoad()
	{
		loadAndInitHandlers();
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminScripts());
	}

	private void loadAndInitHandlers()
	{
		itemHandlers.clear();
		dialogAppends.clear();
		onAction.clear();
		onActionShift.clear();
		onPlayerExit.clear();
		onPlayerEnter.clear();
		onReloadMultiSell.clear();
		onDie.clear();

		for(Script _class : _classes.values())
			try
			{
				if(!GameServer.scriptsObjects.containsKey(_class.getName()))
				{
					ScriptObject go = _class.newInstance();
					GameServer.scriptsObjects.put(_class.getName(), go);
					go.invokeMethod("onLoad");
				}

				for(Method method : _class.getRawClass().getMethods())
					if(method.getName().contains("ItemHandler_"))
					{
						Integer id = Integer.parseInt(method.getName().substring(12));
						ArrayList<ScriptClassAndMethod> handlers = itemHandlers.get(id);
						if(handlers == null)
						{
							handlers = new ArrayList<ScriptClassAndMethod>();
							itemHandlers.put(id, handlers);
						}
						handlers.add(new ScriptClassAndMethod(_class, method));
					}
					else if(method.getName().contains("DialogAppend_"))
					{
						Integer id = Integer.parseInt(method.getName().substring(13));
						ArrayList<ScriptClassAndMethod> handlers = dialogAppends.get(id);
						if(handlers == null)
						{
							handlers = new ArrayList<ScriptClassAndMethod>();
							dialogAppends.put(id, handlers);
						}
						handlers.add(new ScriptClassAndMethod(_class, method));
					}
					else if(method.getName().contains("OnAction_"))
					{
						String name = method.getName().substring(9);
						if(onAction.containsKey(name))
							onAction.remove(name);
						onAction.put(name, new ScriptClassAndMethod(_class, method));
					}
					else if(method.getName().contains("OnActionShift_"))
					{
						String name = method.getName().substring(14);
						if(onActionShift.containsKey(name))
							onActionShift.remove(name);
						onActionShift.put(name, new ScriptClassAndMethod(_class, method));
					}
					else if(method.getName().equals("OnPlayerExit"))
						onPlayerExit.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("OnPlayerEnter"))
						onPlayerEnter.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("OnReloadMultiSell"))
						onReloadMultiSell.add(new ScriptClassAndMethod(_class, method));
					else if(method.getName().equals("OnDie"))
						onDie.add(new ScriptClassAndMethod(_class, method));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}

	public boolean reloadClass(String name)
	{
		File f = new File("./data/scripts/" + name.replace(".", "/") + ".java");
		if(f.exists() && f.isFile())
			return reloadClassByName(name.replace("/", "."));

		f = new File("./data/scripts/" + name.replace(".", "/"));
		if(f.exists() && f.isDirectory())
			return reloadClassByPath(f);

		_log.warning("Con't find class or package by path: " + name);
		return true;
	}

	public boolean reloadClassByName(String name)
	{
		if(Compiler.getInstance().compile(new File("./data/scripts/" + name.replace(".", "/") + ".java"), System.out))
		{
			MemoryClassLoader classLoader = Compiler.getInstance().classLoader;
			try
			{
				Class<?> c = classLoader.loadClass(name);
				Script s = new Script(c);
				ScriptObject so = GameServer.scriptsObjects.remove(name);
				if(so != null)
					so.invokeMethod("onReload");
				_classes.put(name, s);
				loadAndInitHandlers();
				return false;
			}
			catch(ClassNotFoundException e)
			{
				_log.warning("Can't load script class:" + e.getMessage());
			}
			Compiler.getInstance().classLoader = null;
		}
		else
			_log.warning("Can't recompile script: " + name);
		return true;
	}

	public boolean reloadClassByPath(File f)
	{
		ArrayList<File> scriptFiles = new ArrayList<File>();
		parseClasses(f, scriptFiles);
		if(Compiler.getInstance().compile(scriptFiles, System.out))
		{
			MemoryClassLoader classLoader = Compiler.getInstance().classLoader;
			Class<?> c;
			for(String name : classLoader.byteCodes.keySet())
			{
				if(name.contains("$"))
					continue; // пропускаем вложенные классы
				try
				{
					c = classLoader.loadClass(name);
					Script s = new Script(c);
					_classes.put(name, s);
					ScriptObject so = GameServer.scriptsObjects.remove(name);
					if(so != null)
						so.invokeMethod("onReload");
				}
				catch(ClassNotFoundException e)
				{
					_log.warning("Can't load script class:" + e.getMessage());
					return true;
				}
			}
			Compiler.getInstance().classLoader = null;
			loadAndInitHandlers();
		}
		else
		{
			_log.warning("Can't recompile scripts: " + f.getPath());
			return true;
		}
		return false;
	}

	public boolean reloadQuest(String name)
	{
		if(Config.DONTLOADQUEST)
			return true;
		Quest q = QuestManager.getQuest(name);
		File f;
		if(q != null)
		{
			String path = q.getClass().getPackage().getName().replace(".", "/");
			f = new File("./data/scripts/" + path + "/");
			if(f.isDirectory())
				return reloadClassByPath(f);
		}
		q = QuestManager.getQuest(Integer.parseInt(name));
		if(q != null)
		{
			String path = q.getClass().getPackage().getName().replace(".", "/");
			f = new File("./data/scripts/" + path + "/");
			if(f.isDirectory())
				return reloadClassByPath(f);
		}
		return reloadClassByPath(new File("./data/scripts/quests/" + name + "/"));
	}

	public void shutdown()
	{
		for(ScriptObject go : GameServer.scriptsObjects.values())
			try
			{
				go.invokeMethod("onShutdown");
			}
			catch(Exception f)
			{
				f.printStackTrace();
			}
		GameServer.scriptsObjects.clear();
	}

	public static class ScriptClassAndMethod
	{
		public final Script scriptClass;
		public final Method method;

		public ScriptClassAndMethod(Script _scriptClass, Method _method)
		{
			scriptClass = _scriptClass;
			method = _method;
		}
	}
}