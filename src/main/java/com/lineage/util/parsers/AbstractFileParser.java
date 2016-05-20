package com.lineage.util.parsers;

import java.io.File;

/**
 * Абстрактный парсер одного файла
 * 
 * @author VISTALL <p>
 *         Date: 11.07.2009
 *         Time: 1:08:48
 */
public abstract class AbstractFileParser extends AbstractParser
{
	private String _file;

	protected AbstractFileParser(final String file)
	{
		_file = file;
	}

	@Override
	protected final void parse()
	{
		try
		{
			final File file = new File(_file);

			if(!file.exists())
			{
				_log.info("[" + getClass().getSimpleName() + "] file " + file.getAbsolutePath() + " not exists");
				return;
			}
			parseDocument(file);
		}
		catch(final Exception e)
		{
			_log.info("[" + getClass().getSimpleName() + "] parse(): error " + e);
			e.printStackTrace();
		}
	}
}