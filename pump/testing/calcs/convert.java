import java.io.*;   
import java.util.List;
import java.util.ArrayList;
import java.sql.Timestamp;

// have to run locally as pettech to get vax rshell to work 
//
// java encloses cmd parms in double quotes which screws up unix "ls *.java" commands
//
// have to go with a list<String> cmd rather than one big String cmd or
// java will try to break the vax part of the rshell into separate pieces
//
// like to run this command
// rsh nmdhst '@petsource:dump_savefiles_to_xml file' > file.xml
// trouble is that java command tools treat the unix redirect as additional 
// arguments to the vax command not a unix redirect.  So have to read in the
// vax stdout and write the unix file myself.  
   
public class convert {


   public static List<String> command (List<String> cmd) {
   
      List<String> stdout = new ArrayList<String>();
      try { 
         ProcessBuilder pb = new ProcessBuilder(cmd);
         Process p = pb.start();
         InputStream pinstream = p.getInputStream();
         BufferedReader pin = new BufferedReader(new InputStreamReader(pinstream));
         String line = "";
         while ( (line = pin.readLine()) != null ) {
            stdout.add(line);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      
      return stdout;
      
   }
   


   public static void main (String [] args) {
      String vaxfile;
      List<String> cmd = new ArrayList<String>();
      List<String> stdout;
   
      cmd.clear();
      cmd.add("rsh");
      cmd.add("nmdhst");
      cmd.add("'dir user$disk:[pettech.pump]*.bld'");
      stdout = command(cmd);      
      List<String> fileList = new ArrayList<String>();
      for ( String line : stdout ) {
         if ( ! line.matches(".*BLD.*") ) { continue; } // exclude things that aren't actually files
//          if ( line.matches("FRED_1_2DEC1994_PUMP.*") ) { continue; } // exclude this one pump run
//          if ( line.matches("SC_3_11FEB2004_PUMP.*") ) { continue; }  // exclude this one pump run         
         line = line.substring(0,line.indexOf(".BLD"));  // drop BLD and vax version suffix
         if ( fileList.contains(line) ) { continue; } // exclude duplicates
                
         fileList.add(line);
      }
            
//       fileList.clear();
// // // //       fileList.add("ZIMMERMAN_1_7MAY2004_PUMP");
// // //       fileList.add("BATEMAN_1_18AUG2005_PUMP");
// //       fileList.add("ADOTE_1_17AUG2004_PUMP");
//       fileList.add("BAYSDON_1_8JUL2004_PUMP");
      for ( String file : fileList ) {
         System.out.println(file);
         
         // open new tmp xml file
         PrintWriter xml;
         try { 
            File f = new File(file+".xml");
            FileWriter fw = new FileWriter(f);
            xml = new PrintWriter(fw);
         } catch ( Exception e ) {
            System.out.println("couldn't create new xml file");
            continue;
         }
         
         // convert bld memory dump to xml using vax tools
         // write results to tmp xml file         
         cmd.clear();
         cmd.add("rsh");
         cmd.add("nmdhst");
         cmd.add("'@petsource:dump_pump_savefiles_to_xml "+file+"'");
         stdout = command(cmd);      
         for ( String line : stdout ) {
            xml.println(line);
         }
                  
         // close tmp xml file
         xml.close();                 
         
      }
   }
}
