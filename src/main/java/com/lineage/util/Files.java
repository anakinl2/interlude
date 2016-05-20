package com.lineage.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import com.lineage.Config;
import l2d.game.model.L2Player;

public class Files
{
	private static Logger _log = Logger.getLogger(Strings.class.getName());

	private static HashMap<String, String> cache = new HashMap<String, String>();

	public static String read(String name)
	{
		if(name == null)
			return null;

		if(Config.USE_FILE_CACHE && cache.containsKey(name))
			return cache.get(name);

		File file = new File("./" + name);

		//		_log.info("Get file "+file.getPath());

		if(!file.exists())
			return null;

		String content = null;

		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new UnicodeReader(new FileInputStream(file), "UTF-8"));
			StringBuffer sb = new StringBuffer();
			String s = "";
			while((s = br.readLine()) != null)
				sb.append(s).append("\n");
			content = sb.toString();
			sb = null;
		}
		catch(Exception e)
		{ /* problem are ignored */}
		finally
		{
			try
			{
				if(br != null)
					br.close();
			}
			catch(Exception e1)
			{ /* problems ignored */}
		}

		if(Config.USE_FILE_CACHE)
			cache.put(name, content);

		return content;
	}

	public static void cacheClean()
	{
		cache = new HashMap<String, String>();
	}

	public static long lastModified(String name)
	{
		if(name == null)
			return 0;

		return new File(name).lastModified();
	}

	public static String read(String name, L2Player player)
	{
		if(player == null)
			return "";
		return read(name, player.getVar("lang@"));
	}

	public static String langFileName(String name, String lang)
	{
		if(lang == null || lang.equalsIgnoreCase("en"))
			lang = "";

		String tmp;

		tmp = name.replaceAll("(.+)(\\.htm)", "$1-" + lang + "$2");
		if(Config.DEBUG)
			_log.info("Try load file " + tmp);
		if(!tmp.equals(name) && lastModified(tmp) > 0)
			return tmp;

		tmp = name.replaceAll("(.+)(/[^/].+\\.htm)", "$1/" + lang + "$2");
		if(Config.DEBUG)
			_log.info("Try load file " + tmp);
		if(!tmp.equals(name) && lastModified(tmp) > 0)
			return tmp;

		tmp = name.replaceAll("(.+?/html)/", "$1-" + lang + "/");
		if(Config.DEBUG)
			_log.info("Try load file " + tmp);
		if(!tmp.equals(name) && lastModified(tmp) > 0)
			return tmp;

		if(lastModified(name) > 0)
			return name;

		return null;
	}

	public static String read(String name, String lang)
	{
		String tmp = langFileName(name, lang);

		long last_modif = lastModified(tmp); // время модификации локализованного файла
		if(last_modif > 0) // если он существует
		{
			if(last_modif >= lastModified(name) || !Config.CHECK_LANG_FILES_MODIFY) // и новее оригинального файла
				return Strings.bbParse(read(tmp)); // то вернуть локализованный

			_log.warning("Last modify of " + name + " more then " + tmp); // если он существует но устарел - выругаться в лог
		}

		return Strings.bbParse(read(name)); // если локализованный файл отсутствует вернуть оригинальный
	}

	/**
	 * Сохраняет строку в файл в кодировке UTF-8.<br>
	 * Если такой файл существует, то перезаписывает его.
	 * @param path путь к файлу
	 * @param string сохраняемая строка
	 */
	public static void writeFile(String path, String string)
	{
		if(string == null || string.length() == 0)
			return;

		File target = new File(path);

		if(!target.exists())
			try
			{
				target.createNewFile();
			}
			catch(IOException e)
			{
				e.printStackTrace(System.err);
			}

		try
		{
			FileOutputStream fos = new FileOutputStream(target);
			fos.write(string.getBytes("UTF-8"));
			fos.close();
		}
		catch(IOException e)
		{
			e.printStackTrace(System.err);
		}
	}
}