package com.lineage.util.parsers;

import java.io.File;

/**
 * Абстрактный парсер папки
 * 
 * @author VISTALL <p>
 *         Date: 11.07.2009
 *         Time: 1:08:39
 */
public abstract class AbstractDirParser extends AbstractParser
{
	private String _root;
	private String _ignoringFile;

	protected AbstractDirParser(final String root, final String ignoringFile)
	{
		_root = root;
		_ignoringFile = ignoringFile;
	}

	@Override
	protected final void parse()
	{
		parse(_root);
	}

	protected final void parse(final String root)
	{
		File file = null;
		try
		{
			final File dir = new File(root);

			if(!dir.exists())
			{
				_log.info("[" + getClass().getSimpleName() + "] Dir " + dir.getAbsolutePath() + " not exists");
				return;
			}

			final File[] files = dir.listFiles();
			for(final File f : files)
				if(f.isDirectory())
					parse(f.getAbsolutePath());
				else if(f.getName().endsWith(".xml") && !f.getName().equals(_ignoringFile)) // затычка для шаблона
				{
					file = f;
					parseDocument(f);
				}
		}
		catch(final Exception e)
		{
			_log.info("[" + getClass().getSimpleName() + "] parse(): error " + e + " in file " + file.getName());
			e.printStackTrace();
		}
	}
}