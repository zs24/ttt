import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.util.HashMap;


public class PickIsotope {
      HashMap<String,Double> isoList = new HashMap<String,Double>();
      String def = "";  // cant use 'default'.  it seems to be a keyword
      Element root = null;
      String XMLfile = "PickIsotope.xml";

      PickIsotope () {
         readXML(this.XMLfile);
         getListFromXML();
      }

      private void readXML (String filename) {
         try {
            this.root = (new DomParserBean(filename)).getDoc().getDocumentElement();
         } catch ( Exception e ) {
            e.printStackTrace();
         }  
      }

      private void getListFromXML () {
      
         NodeList list = this.root.getElementsByTagName("*");
         for (int i=0;i<list.getLength();i++) {
            Element node = (Element)list.item(i);    
            String tag =  node.getNodeName();
            if ( tag == "default" ) {
               String value = node.getTextContent();
               this.def = value;
            } else {
               Double value = Double.valueOf(node.getTextContent());
               this.isoList.put(tag,value);
            }
         }
      }
      
      public HashMap<String,Double> getList () {
         return this.isoList;
      }
      
      public String getDefault () {
         return this.def;
      }
      
      public static void main (String [] args) {
         PickIsotope x = new PickIsotope();
         System.out.println(x.getList());
         System.out.println(x.getDefault());
      }
}

