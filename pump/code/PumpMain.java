import java.sql.Timestamp;
import java.io.File;

import java.util.HashMap;

public class PumpMain {
   

   PumpMain () {
   }   
   
   
   public static void help () {
      System.out.println("");
      System.out.println("");
      System.out.println("Usage");
      System.out.println("");
      System.out.println("pump <driving file>");
      System.out.println("");
      System.out.println("Driving file is a required parameter that points to an");
      System.out.println("xml file that contains all the information needed for a ");
      System.out.println("particular infusion run.  It can be specified in three ");
      System.out.println("ways.  1) User can give the 'short name' of a particular");
      System.out.println("pre-defined driving file, tztp for example.  2) User can");
      System.out.println("give the full file name to a particular pre-defined ");
      System.out.println("driving file, config/pump_tztp.xml.  3) User can reload");
      System.out.println("the information of a previous infusion run by giving");
      System.out.println("the full file name of the particular xml save file. ");
      System.out.println("");
      System.out.println("pump tztp");
      System.out.println("pump config/pump_tztp.xml");
      System.out.println("pump log/20060201_smith_j_1.xml");
      System.out.println("");
      System.out.println("");
    }
            
      
   public static void main (String [] args) {
      PumpMessages msgs;
      PumpCommands cmds;
      PumpParameters parms;
      PumpGUI gui;
            
      String basePath = System.getenv("PUMP_DIR");
      if ( basePath == null ) { basePath = "."; }
      String pathXMLin = basePath + "/config/";
      String pathXMLout = basePath + "/log/";
            
      // parse arguements
      String filename = "";
      int nargs = args.length;
      switch (nargs) {
         case 0 :
            // display help screen
            help();
            System.exit(1);
         case 1 :
            String test = args[0].toLowerCase();
            if ( test.matches(".*usage.*") || test.matches(".*help.*") ) {
               // display help screen
               help();
               System.exit(1);
            }
            if ( args[0].equalsIgnoreCase("dose") ) {
               // no computer controlled infusion.  go straight to dose tab
               filename = null;
               break;
            }
            if ( args[0].matches(".*xml$")) {
               // user supplied full filename
               filename = args[0];
            } else {
               // user gave short name to a pre-defined driving file
               filename = pathXMLin + "pump_" + args[0] + ".xml";
            }
            File file = new File(filename);
            if ( ! file.exists() ) {
               // no such file
               System.out.println("");
               System.out.println("Error:");
               System.out.println("Driving file does not exist.  Check the spelling.");
               System.out.println("Check the following file location.  Try again.");
               System.out.println("");
               System.out.println(filename);
               System.out.println("");
               System.exit(1);
            }
      }
   
      // initialize major objects
      try { 
         msgs = new PumpMessages();
         parms = new PumpParameters(msgs,filename,pathXMLout);
         cmds = new PumpCommands(msgs);
      } catch ( Exception e ) {
         e.printStackTrace();
         return;
      }
   
      try {
         
         gui = new PumpGUI(msgs,parms,cmds);
      } catch ( Exception e) {
         e.printStackTrace(msgs.getStream());
      }
      
   }
}



