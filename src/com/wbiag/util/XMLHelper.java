package com.wbiag.util;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author bviveiros
 * 
 * Helper methods not available in the core XMLHelper class.
 *
 */
public class XMLHelper {

	/**
	 * Return an instance of a new DOM Document.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static Document getNewDocument() throws Exception {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);

        DocumentBuilder builder = null;
        try {
        	builder = dbf.newDocumentBuilder();
        } catch (Exception e) {
        	throw e;
        }
        
        Document doc = builder.newDocument();
        
        return doc;
	}
	
	/**
	 * Return an instance of a new DOM Document with the given XML String.
	 * 
	 * @param xmlString
	 * @return
	 * @throws Exception
	 */
	public static Document createDocument(String xmlString) throws Exception {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		
		Document doc = null;
        DocumentBuilder builder = null;
        
        try {
        	builder = dbf.newDocumentBuilder();

	        Reader r = new StringReader( xmlString );
	        doc = builder.parse( new InputSource(r) );
	        r.close();

        } catch (Exception e) {
        	throw e;
        }

        return doc;
	}

	/**
	 * Given an XML Element, and a NodeList, find the element in the node list that is equal
	 * including it's child nodes.  If not found returns false.
	 * 
	 * @param findElement
	 * @param nodeList
	 * @return
	 */
	public static boolean existsElementMatchingChildNodes(Element findElement, NodeList searchInList) {
		
		Node searchInElement = null;
		boolean foundElement = false;
		boolean possibleMatch = false;
		Node findNode = null;
		String findName = null;
		String findValue = null;
		String searchString = null;
		
		// Check each Node in searchInList.
		int searchInListIndex = 0;
		while (!foundElement && searchInListIndex < searchInList.getLength()) {
			
			searchInElement = searchInList.item(searchInListIndex);
			
			// for each child node in the findElement, find a child node
			// with the same name and value exists in the searchInElement.
			possibleMatch = true;
			findNode = findElement.getFirstChild();
			while (findNode != null && possibleMatch) {

				findName = findNode.getNodeName();

				if (findNode.getFirstChild() != null) {
					findValue = findNode.getFirstChild().getNodeValue();
					searchString = "?/" + findName + "=" + findValue;
				} else {
					searchString = "?/" + findName;
				}
				
				// If a matching child node was not found then go to the next Element in the search list.
				if (com.workbrain.util.XMLHelper.findElement((Element)searchInElement, searchString) == null) {
					possibleMatch = false;
			
				} else {
				
					// Move to the next sibling which could be a text node.
					findNode = findNode.getNextSibling();
					if (findNode != null && findNode.getNodeType() == Node.TEXT_NODE) {
						// If the next sibling was a text node then need to move again.
						findNode = findNode.getNextSibling();
					}
				}
				
			}
			
			// If we iterated through all child nodes successfully then we found a match.
			if (possibleMatch) {
				foundElement = true;
			} else {
				searchInListIndex++;
			}
		}
		
		// All child nodes matched.
		return foundElement;
	}
}
