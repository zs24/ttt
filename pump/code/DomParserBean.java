/**
 * Created on May 19, 2004
 *
 * To change this generated comment edit the template variable "filecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of file comments go to
 * Window>Preferences>Java>Code Generation.
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
//import org.w3c.dom.ls.DOMImplementationLS;
//import org.w3c.dom.ls.DOMWriter;
import org.xml.sax.InputSource;

import java.util.HashMap;
import java.io.FileNotFoundException;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

/**
 * @author CPS
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class DomParserBean implements Serializable {

	private Document doc;
	private boolean multipleTags = false;
	private int tagCount = 0;
        private boolean debug = true;
        private boolean insertTagsOnPut = false;
	/** 
	 * Constructor for DomParserBean.
	 */
	public DomParserBean(String file) throws Exception {
           // throws java.io.FileNotFoundException if file doesn't exist
           // throws org.xml.sax.SAXParseException if xml file not well formed
           // throws javax.xml.parsers.ParserConfigurationException if xml object not configured right
           // throws org.xml.sax.SAXException if there was a problem parsing xml file
          try {
               setDoc(getDocumentFromFile(file));
          } catch ( FileNotFoundException e ) {
                // java.io.FileNotFoundException -- xml file not found
                throw new Exception("XML file not found\n"+file);
          } catch ( SAXParseException e ) {      
                // org.xml.sax.SAXParseException -- xml file not well formed
                throw new Exception ("XML file not well-formed\n"+file);
          } catch ( ParserConfigurationException e ) {
                // javax.xml.parsers.ParserConfigurationException -- xml object not configured right
                throw new Exception("XML object not configured right");
          } catch ( SAXException e ) {
                // org.xml.sax.SAXException -- there was a problem parsing xml file
                throw new Exception("XML object had trouble parsing file");
          }
// 		try {
// 			setDoc(getDocumentFromFile(file));
// 		} catch (Exception e) {
// 			e.printStackTrace();
// 		}
	}
   	
   	public DomParserBean(DomParserBean copyBean) {
   		setDoc(copyBean.getDoc());
   	}
   	
   	public DomParserBean(InputStream iFile) {
   		setDoc(getDocumentFromInputStream(iFile));
   	}

   	public DomParserBean(InputSource iSource) {
   		setDoc(getDocumentFromInputSource(iSource));
   	}
	/**
	 * Method getDocumentFromInputStream.
	 * @param iFile
	 * @return Document
	 */
	private Document getDocumentFromInputStream(InputStream iFile) {
		// Step 1: create a DocumentBuilderFactory
	 	DocumentBuilderFactory dbf =
	  	DocumentBuilderFactory.newInstance();
	
		// Step 2: create a DocumentBuilder
	 	DocumentBuilder db;
	 	Document d = null;
		try {
			db = dbf.newDocumentBuilder();
	
			// Step 3: parse the input file to get a Document object
			d = db.parse(iFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return d;
	}

	private Document getDocumentFromInputSource(InputSource iSource) {
		// Step 1: create a DocumentBuilderFactory
	 	DocumentBuilderFactory dbf =
	  	DocumentBuilderFactory.newInstance();
	
		// Step 2: create a DocumentBuilder
	 	DocumentBuilder db;
	 	Document d = null;
		try {
			db = dbf.newDocumentBuilder();
	
			// Step 3: parse the input file to get a Document object
			d = db.parse(iSource);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return d;
	}

   	
   	public static Document 
   		getDocumentFromFile(String file) throws Exception {
                
		// Step 1: create a DocumentBuilderFactory
	 	DocumentBuilderFactory dbf =
	  	DocumentBuilderFactory.newInstance();
	
		// Step 2: create a DocumentBuilder
	 	DocumentBuilder db = dbf.newDocumentBuilder();
	
		// Step 3: parse the input file to get a Document object
	 	Document d = db.parse(new File(file));
	 	return d;
   	}
   	
   	public String getTagValue(String subTree, String tag) {
   		Vector parentNodes = splitSubTree(subTree);
   		Node tagNode = getTagNode(parentNodes, tag);

//    		if(tagNode == null) {
//                         if ( debug ) {
//    			       System.err.println("GetTagValue: Tag:"+tag+" not found in tree:"+subTree);
//                         }
//    			return null;
//    		}
//    		if(tagNode.getNodeType() == Node.ELEMENT_NODE) {
//    			NodeList child = tagNode.getChildNodes();
//    			if(child.getLength() == 1 
//    				&& child.item(0).getNodeType() == Node.TEXT_NODE)
//    				tagNode = child.item(0);
//    		}
//    		return tagNode.getNodeValue();
                
                return getNodeValue(tagNode);
        }
        
        public String getNodeValue(Node tagNode) {
   		if(tagNode == null) {
   			return null;
   		}
   		if(tagNode.getNodeType() == Node.ELEMENT_NODE) {
   			NodeList child = tagNode.getChildNodes();
   			if(child.getLength() == 1 
   				&& child.item(0).getNodeType() == Node.TEXT_NODE)
   				tagNode = child.item(0);
   		}
   		return tagNode.getNodeValue();
   	}
           
        
   	public boolean putTagValue(String subTree, String tag, String value) {
   		Vector parentNodes = splitSubTree(subTree);
   		Text text = getDoc().createTextNode(value);
   		Node tagNode = getTagNode(parentNodes, tag);
   		if(tagNode != null) {
	   		NodeList childNodes = tagNode.getChildNodes();
	   		boolean foundNode = false;
	   		if(childNodes.getLength() > 0) {
		   		for(int i=0; i < childNodes.getLength(); i++) {
		   			Node curNode = childNodes.item(i);
		   			
		   			if(curNode.getNodeType() == Node.TEXT_NODE) {
		   				tagNode.replaceChild(text, curNode);
		   				foundNode = true;
		   				break;
		   			}
		   		}
		   		if(!foundNode) {
		   			tagNode.appendChild(text);
		   			foundNode = true;
		   		}	
	   		} else {
	   			tagNode.appendChild(text);
	   			foundNode = true;
	   		}
			return foundNode;  
   		} else {
                        if ( insertTagsOnPut && insertTag(subTree,tag) ) {
                                return putTagValue(subTree,tag,value);
                        }
   			System.err.println("PutTagValue: Tag:"+tag+" not found in tree:"+subTree);
   			return false;
   		} 		
   	}
	/**
	 * Method getTagNode.
	 * @param parentNodes
	 * @param tag
	 * @return Node
	 */
	public Node getTagNode(Vector parentNodes, String tag) {
		Node tagNode = null;
		
		tagNode = findMultipleTags(parentNodes, tag);
		
		return tagNode;
	}
        
        
// //         Finds all child nodes of the given subTree.  Returns the key-value
// //         pairings of those 1st generation children in a HashMap.  Children
// //         of children are not considered.  
//         public HashMap getAllTagValues(String subTree) {
//            HashMap map = new HashMap();
//            boolean oldMultipleTags = isMultipleTags();
// 
//            Node tagNode;
//            Vector parentNodes = splitSubTree(subTree);
//            
//            setMultipleTags(true);
//            while ( (tagNode = getTagNode(parentNodes,"*")) != null ) {
//    		if(tagNode.getNodeType() == Node.ELEMENT_NODE) {
//    			NodeList child = tagNode.getChildNodes();
//    			if(child.getLength() == 1 
//    				&& child.item(0).getNodeType() == Node.TEXT_NODE)
//    				tagNode = child.item(0);
//    		}
//                 map.put(tagNode.getParentNode().getNodeName(),tagNode.getNodeValue());
//             }
//            setMultipleTags(oldMultipleTags);  
//             
//            
//            return map;
//         }


        // add or update a tag/value inserting tags as neccessary
        // tree parameter is full tag path delimited by "/"
        // DOES NOT handle duplicate tags.  given there could be many duplicate
        // tags on several levels how do you specify you want the 2nd duplicate
        // on the 1st level, the 5th duplicate on the 2nd level, the 16th duplicate
        // on the 3rd level when xml does not gauruntee any particular tag order
        public void putNestedTagValue(String tree,String value) {
           Vector<String> nodeNames = splitSubTree(tree);
           
           int i,nKids;
           Node node = getDoc();
           for ( String name : nodeNames ) {
              NodeList children = node.getChildNodes();
              nKids = children.getLength();
              for ( i=0;i<nKids;i++) {
                 if ( children.item(i).getNodeName().contentEquals(name) )  { 
                    node = children.item(i);
                    break;
                 }
              }
              if ( i == nKids ) { 
                 // no matches
                 node = node.appendChild(this.doc.createElement(name));
              } 
           }
           node.setTextContent(value);   
        }
        
        
        
        public String getNestedTagValue(String tree) {
           int index = tree.lastIndexOf('/');
           String subtree = tree.substring(0,index);
           String tag = tree.substring(index+1);
           
           return this.getTagValue(subtree,tag);        
        } 
        
        
//         public void testFindTag(Node parent,int space) {
//            
//            NodeList children = parent.getChildNodes();
//            for ( int i=0;i<children.getLength();i++) {
//               Node child = children.item(i);
//               String name = child.getNodeName();
//               String value = "";
//               short type = child.getNodeType();
//               String types=null;
//               
//               switch (type) {
//                  case Node.ELEMENT_NODE : types="element"; break;
//                  case Node.ATTRIBUTE_NODE : types = "attribute";break;
//                  case Node.TEXT_NODE : types = "text";break;
//                  case Node.CDATA_SECTION_NODE : types = "cdata";break;
//                  case Node.ENTITY_REFERENCE_NODE : types = "entity reference";break;
//                  case Node.ENTITY_NODE : types = "entity"; break;
//                  case Node.PROCESSING_INSTRUCTION_NODE : types = "processing_instruction";break;
//                  case Node.COMMENT_NODE : types = "comment"; break;
//                  case Node.DOCUMENT_NODE : types = "document";break;
//                  case Node.DOCUMENT_TYPE_NODE : types = "document type";break;
//                  case Node.DOCUMENT_FRAGMENT_NODE : types = "document fragment";break;
//                  case Node.NOTATION_NODE : types = "notation";break;
//                }
//                     
//               if ( type == Node.ELEMENT_NODE ) {
//                  int grandKids = child.getChildNodes().getLength();
//                  if ( grandKids == 1 )  {
//                     value = child.getTextContent();
//                     for ( int j=0;j<space;j++ ) { System.out.print(" "); }
//                     System.out.println(name+"-->"+value);              
//                  } else if ( grandKids > 1 ) {
//                     for ( int j=0;j<space;j++ ) { System.out.print(" "); }
//                     System.out.println(name+"-->");
//                     testFindTag(child,space+3);                     
//                  }
//               }
//            }
//         } 
//        

	public Node findMultipleTags(Vector parentNodes, String tag) {
		int count = 0;

		NodeList tagNodes = getDoc().getElementsByTagName(tag);
		
		for(int i=0 ; tagNodes != null && i < tagNodes.getLength() ; i++) {
			Node curNode = tagNodes.item(i);
			boolean foundNode = false;
			int j=0;
			for(j=parentNodes.size()-1; j >= 0 ; j--) {
				if(curNode.getParentNode().getNodeName().equals(parentNodes.get(j))) {
					curNode = curNode.getParentNode();
					foundNode = true;
					continue;
				} else {
					break;
				}
			}
			if(j < 0 && foundNode) {
				if(!isMultipleTags()) {
					return tagNodes.item(i);
				} else {
					if(count == tagCount) {
						++tagCount;
						return tagNodes.item(i);
					} else {
						++count;
						foundNode = false;
						continue;
					}
				}
			}
		}
		return null;
	}
	
	public Node findUniqueTags(Vector parentNodes, String tag) {
		NodeList tagNodes = getDoc().getElementsByTagName(tag);
		
		for(int i=0 ; i < tagNodes.getLength() ; i++) {
			Node curNode = tagNodes.item(i);
			boolean foundNode = false;
			int j=0;
			for(j=parentNodes.size()-1; j >= 0 ; j--) {
				if(curNode.getParentNode() != null) {
					if(curNode.getParentNode().getNodeName().equals(parentNodes.get(j))) {
						curNode = curNode.getParentNode();
						foundNode = true;
						continue;
					} else {
						break;
					}
				} else {
					if(curNode.getNodeName().equals(parentNodes.get(j))) {
						curNode = curNode.getParentNode();
						foundNode = true;
						continue;
					} else {
						break;
					}
				}
			}
			if(j < 0 && foundNode) {
				return tagNodes.item(i);
			}
		}
		if(tagNodes.getLength() == 1 && parentNodes.size() == 0) {
			return tagNodes.item(0);	
		}
		return null;
	}
	

	/**
	 * Method splitSubTree.
	 * @param subTree
	 */
	public Vector splitSubTree(String subTree) {
		Vector tree = new Vector();
		if(subTree.lastIndexOf('/') >= 0) {
			while(subTree.lastIndexOf('/') >= 0) {
				int index = subTree.indexOf('/');
				tree.add(subTree.substring(0, index));
				subTree = subTree.substring(index+1);
			}
			
		}	
		tree.add(subTree);			
		return tree;
	}

   	public boolean writeToFile(String file) {
		try {
			OutputFormat outputFormat = new OutputFormat("XML","UTF-8",true);
			outputFormat.setIndent(3);
			FileWriter fileWriter = new FileWriter(file);
			XMLSerializer xmlSerializer = new XMLSerializer(fileWriter, outputFormat);
			xmlSerializer.asDOMSerializer();
			xmlSerializer.serialize(doc);
			
			fileWriter.close();
			/*
			FileOutputStream os = new FileOutputStream(file);
		  	DOMImplementation impl = doc.getImplementation();
		  	if (impl != null) {
		      	DOMImplementationLS implls = (DOMImplementationLS) impl;
		      	DOMWriter writer = implls.createDOMWriter();
		      	
		      	writer.setNewLine(null);
		      	
		      	writer.writeNode(os, doc);



		  	}
		  	else {
		    	System.out.println(
		     	"Could not find the document implementation");
		     	return false;
		  	}  
		  	os.close();
		  	*/
		  	

		}
		catch (Exception e) {
		  	System.err.println(e); 
		  	e.printStackTrace();
		  	return false;
		}  
		return true;
	}

	public boolean insertTag(String subtree, String tag) {
		Vector parentNodes = splitSubTree(subtree);
		String parentTag = (String) parentNodes.remove(parentNodes.size()-1);
		//IGNORING MULTIPLETAGS OPTION !!!!!
		Node parent = findUniqueTags(parentNodes, parentTag);
		Element childNode = doc.createElement(tag);
                
		if(parent != null) {
			parent.appendChild(childNode);
		} else {
			return false;
		}
		return true;
	}

	public boolean deleteTag(String subtree, String tag) {
		Vector parentNodes = splitSubTree(subtree+"/"+tag);
		String curTag = (String) parentNodes.remove(parentNodes.size()-1);
		Node child = getTagNode(parentNodes, curTag);
		if(child != null) {
			Node parent = child.getParentNode();
			parent.removeChild(child);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Returns the doc.
	 * @return Document
	 */
	public Document getDoc() {
		return doc;
	}

	/**
	 * Returns the multipleTags.
	 * @return boolean
	 */
	public boolean isMultipleTags() {
		return multipleTags;
	}

	/**
	 * Sets the multipleTags.
	 * @param multipleTags The multipleTags to set
	 */
	public void setMultipleTags(boolean multipleTags) {
		tagCount = 0;
		this.multipleTags = multipleTags;
	}

	/**
	 * Sets the doc.
	 * @param doc The doc to set
	 */
	public void setDoc(Document doc) {
		this.doc = doc;
	}

	/**
	 * Returns the tagCount.
	 * @return int
	 */
	public int getTagCount() {
		return tagCount;
	}

	/**
	 * Sets the tagCount.
	 * @param tagCount The tagCount to set
	 */
	public void setTagCount(int tagCount) {
		this.tagCount = tagCount;
	}
        
        public void setDebug(boolean flag) {
                this.debug = flag;
        }

        public void setInsertTagsOnPut(boolean flag) {
                this.insertTagsOnPut = flag;
        }
}

