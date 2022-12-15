
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.util.HashMap;
import java.net.InetAddress;
import java.net.UnknownHostException;
 

public class PickRoom {
      HashMap<String,String> IPlist = new HashMap<String,String>();
      String room="";
      Element root = null;
      String XMLfile = "/PickRoom.xml";
      
      
      PickRoom () {
         String basePath = System.getenv("PUMP_DIR");
         if ( basePath == null ) { basePath = "."; }
         this.XMLfile = basePath + this.XMLfile;
         
         readXML(this.XMLfile);
         String hostname = this.getHostName();
         getIPlistFromXML();
         getRoomFromXML(hostname);
      }
      
      
      public String getHostName () {
         // what machine are we running on
         String hostIP = "";
         String host = "";
         String display = "";
         try {
            InetAddress addr = InetAddress.getLocalHost();
            hostIP = addr.getHostAddress();
            host = addr.getHostName();
            display = System.getenv("DISPLAY");
         } catch (UnknownHostException e) {
            // InetAddress.getLocalHost() failed
            e.printStackTrace();
            return "xxx";
         }
         host = (host.split("\\."))[0];
         display = (display.split(":"))[0];
         if ( ! display.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$") ) {
            display = (display.split("\\."))[0];
         }
         String hostname = ( display.equals("") ) ? host : display;
         
         return hostname;
      }
      
      
      
      private void readXML (String filename) {
         try {
            this.root = (new DomParserBean(filename)).getDoc().getDocumentElement();
         } catch ( Exception e ) {
            e.printStackTrace();
         }  
      }
      


      private void getIPlistFromXML () {
      
  //       Element root = doc.getDocumentElement();
         NodeList list = this.root.getElementsByTagName("terminal");
         for (int i=0;i<list.getLength();i++) {
            Element node = (Element)list.item(i);         
            this.IPlist.put(node.getParentNode().getNodeName(),node.getTextContent());
         }
      }
      

      private void getRoomFromXML (String hostname) {
      
         NodeList list = this.root.getElementsByTagName("computer");
         for (int i=0;i<list.getLength();i++) {
            Element node = (Element)list.item(i);
            if ( node.getTextContent().equals(hostname) ) {
               this.room = node.getParentNode().getNodeName();
               break;
            }      
         }
      }
      
      
      public HashMap<String,String> getIPlist () {
         return this.IPlist;
      }
      
      public String getRoom () {
         return this.room;
      }
      
  }
         
