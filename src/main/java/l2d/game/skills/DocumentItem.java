package l2d.game.skills;

import java.io.File;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilderFactory;

import l2d.game.tables.ItemTable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class DocumentItem extends DocumentBase
{
	private Document _doc;

	public DocumentItem(File file)
	{
		super(file);

		Document doc;
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(file);
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Error loading file " + file, e);
			return;
		}
		_doc = doc;
		try
		{
			parseDocument(doc);
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Error in file " + file, e);
			return;
		}
	}

	@Override
	protected Number getTableValue(String name)
	{
		return null;
	}

	@Override
	protected Number getTableValue(String name, int idx)
	{
		return null;
	}

	@Override
	protected void parseDocument(Document null_doc)
	{
		for(Node n = _doc.getFirstChild(); n != null; n = n.getNextSibling())
			if("list".equalsIgnoreCase(n.getNodeName()))
			{
				for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					if("item".equalsIgnoreCase(d.getNodeName()))
						parseTemplate(d, ItemTable.getInstance().getTemplate(Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue())));
			}
			else if("item".equalsIgnoreCase(n.getNodeName()))
				parseTemplate(n, ItemTable.getInstance().getTemplate(Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue())));
	}
}