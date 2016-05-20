package l2d.util.parsers;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Абстрактный парсер
 * 
 * @author VISTALL <p>
 *         Date: 11.07.2009
 *         Time: 1:08:39
 */
public abstract class AbstractParser
{
	protected final Logger _log = Logger.getLogger(getClass().getName());

	protected final void parseDocument(final File f) throws Exception
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		final Document doc = factory.newDocumentBuilder().parse(f);

		for(Node start0 = doc.getFirstChild(); start0 != null; start0 = start0.getNextSibling())
			readData(start0);
	}

	protected abstract void readData(Node node);

	protected abstract void parse();
}