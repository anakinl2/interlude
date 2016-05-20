package com.lineage.ext.scripts;

import java.util.logging.Logger;

import com.lineage.Config;

public class Script
{
	private static final Logger _log = Logger.getLogger(Script.class.getName());
	private Class<?> _class;

	public Script(Class<?> c)
	{
		_class = c;
	}

	public ScriptObject newInstance()
	{
		ScriptObject o = null;
		Object instance = null;
		try
		{
			instance = _class.newInstance();
		}
		catch(InstantiationException e)
		{
			if(Config.DEBUG)
				_log.info("Class " + getName() + " hasn't default constructor.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		o = new ScriptObject(_class, instance);

		return o;
	}

	@SuppressWarnings("unchecked")
	public ScriptObject newInstance(Object[] args)
	{
		ScriptObject o = null;
		Object instance = null;
		try
		{
			Class[] types = new Class[args.length];
			boolean arg = false;
			for(int i = 0; i < args.length; i++)
				if(args[i] != null)
				{
					types[i] = args[i].getClass();
					arg = true;
				}
			if(!arg)
				return newInstance();
			instance = _class.getConstructor(types).newInstance(args);
		}
		catch(InstantiationException e)
		{
			if(Config.DEBUG)
				_log.info("Class " + getName() + " hasn't constructor with such arguments.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		o = new ScriptObject(_class, instance);

		return o;
	}

	public Class<?> getRawClass()
	{
		return _class;
	}

	public String getName()
	{
		return _class.getName();
	}
}
