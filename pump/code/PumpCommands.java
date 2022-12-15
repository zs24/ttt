import java.net.Socket;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.InetAddress;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;


public class PumpCommands {

   Socket sock;
   PrintWriter out;
   BufferedReader in;
   char status;
   String response;
   boolean connection;
   boolean protocol;
   boolean programable;
   boolean debug = true;
   String mode="";
   PumpMessages post;
   
   // constuctor
   PumpCommands (PumpMessages msgs) {
   
      this.post = msgs;
      this.debug = ( System.getenv("PUMP_DEBUG") != null );
      
   }
   

   // constructor
   PumpCommands (PumpMessages msgs,String ip, int port) throws PumpException {     
    
         this(msgs);   
         this.Ping(ip);
         this.Connect(ip,port);
         this.TestConnection();
         this.TestProtocol();
         this.TestProgramable();
         
   }



// ping works in unix and windows but paths are different
// rather than trying to figure out which operating system
// we are using, use a native java tool to try to ping the
// server for us.  Tool is new to java 5.0 and there are some
// bug reports about failing to close sockets in windows which
// eventually lead windows systems filling up and refusing to 
// open any additional sockets.  Its all fixed in very latest
// release though. (build #58 circa 2004-07-21)
   void Ping (String ip) throws PumpException {
      
//      String cmd,ping;
      boolean ping = false;
      try { 
         // stupid java.  the inet tools will only work with a
         // host name or a byte array representing the four parts
         // of an ip address.  So we have to convert the string
         // representation of the ip address to an array of bytes.  
         // stupid java.  even after the string representation of
         // the ip address is broken down to an array of strings we
         // cant convert those substrings direct to a byte.  java bytes
         // can go 0->255 or -127->+127.  The conversion tools assume
         // -127->+127.  Our 128.231.47.20 address blows the capacity of
         // these signed bytes.  Have to convert first to an int then 
         // convert to a byte.  
         int tmp;
         byte [] bytes = new byte [4];
         String [] strings = ip.split("\\.");
         for (int i=0;i<4;i++) {
            tmp = Integer.valueOf(strings[i]);
            bytes[i] = (byte)tmp;
         }
         InetAddress address;
         address = InetAddress.getByAddress(bytes);
         ping = address.isReachable(1000);
//          cmd = "/usr/sbin/ping";
//          ProcessBuilder pb = new ProcessBuilder(cmd, ip);
//          Process p = pb.start();
//          InputStream pinstream = p.getInputStream();
//          BufferedReader pin = new BufferedReader( new InputStreamReader( pinstream ) );
//          ping = pin.readLine();
      } catch ( UnknownHostException e ) {
         String msg;
         msg = "Could not set up java tools to ping terminal server.\n";
         msg += "Call programing support.";
         this.post.error(msg);
      } catch ( IOException e ) {
         String msg;
         msg = "Could not send ping packets to terminal server\n";
         msg += "The server may be fine.  Java itself couldn't create\n";
         msg += "and send the packets\n";
         msg += "Call programing support.";
         this.post.error(msg);
      }   
      
      if ( ! ping ) {
         String msg;
         msg = "Could not talk to terminal server.\n";
         msg += "\n";
         msg += "Make sure the terminal server is turned on.\n";
         msg += "Check the wires connecting the terminal server to the wall.\n";
         msg += "Check the NET indicator light is blinking intermitantly.\n";
         msg += "If all that checks out, try rebooting the terminal server."; 
         this.post.error(msg);
      }
   }




   // connect to pump socket
   // java.net.UnknownHostException -- new Socket
   // java.io.IOException -- new Socket
   // java.net.SocketException -- sock.setSoTimeout(5000)
   // java.net.SocketException -- sock.setTcpNoDelay(true)
   // java.io.IOException -- sock.getInputStream()
   // java.io.IOException -- sock.getOutputStream()
   void Connect (String ip, int port) throws PumpException {      
      
         // connect socket
         try {
            sock = new Socket(ip,port);
            sock.setSoTimeout(5000);  // 5 seconds
            sock.setTcpNoDelay(true);  // nagle's algorithm -- send everything as you get it.  don't cache things in the hopes of building larger more efficient packets
            InputStream instream = sock.getInputStream();
            OutputStream outstream = sock.getOutputStream();
            out = new PrintWriter(new OutputStreamWriter( outstream));
            in = new BufferedReader( new InputStreamReader( instream ) );
         } catch ( UnknownHostException e ) {
            this.post.error("Unknown Host");
//          } catch ( InterruptedIOException e ) {
//             System.out.println("Timed out");
//             return;
         } catch ( SocketException e ) {
            String msg;
            msg = "Could not establish a socket connection.\n";
            msg += "\n";
            msg += "Another computer controlled pump program appears to have\n";
            msg += "established a lock on this pump.  Check for other \n";
            msg += "windows running the pump program.  If that doesn't work\n";
            msg += "logon to the terminal server and break the connection\n";
            msg += "manually.  Sometimes the connection persists for a few \n";
            msg += "seconds after the program is killed.";
            this.post.error(msg);
         } catch ( IOException e ) {
            this.post.error("Error connecting i/o streams to socket");
         } 
                  
   }



   
   
   synchronized void TestConnection () throws PumpException {
      
      // test connection
      // io exception also thrown by method itself if something doesn't work
      try {
         this.Command("VER",true);
      } catch ( Exception e ) {
//         if (e.getMessage().length()>0) {
//            this.post.error(e.getMessage());
//         }
         String msg;
         msg = "Could not talk to pump.\n";
         msg += "\n";
         msg += "Make sure the pump is turned on\n";
         msg += "Make sure pump isn't in some state expecting input\n";
         msg += "Check wires connecting terminal server to pump.\n";
         msg += "Check baud rate on pump and the terminal server.";
         this.post.error(msg);
      }
      if ( ! this.response.contains("PHD1.2") ) {
         String msg;
         msg = "Received unexpected response from pump\n";
         msg += "\n";
         msg += "Sent VER command.\n"; 
         msg += "Expected 'PHD1.2' in response.\n";
         msg += "Instead, pump sent back '"+this.response+"'";         
         this.post.error(msg);
      }
      // with the new programable pumps its possible to "fire and forget".
      // you can start the pump running and close the connection trusting
      // the pump will stop itself when the time comes.  The socket is 
      // released so you could try to establish a new connection and send
      // the pump new commands while its still working on the previous 
      // run.  If the pump status shows the pump is still infusing, give 
      // an error and exit.  
      if ( this.status == '>' ) {
         String msg;
         msg = "Pump is running\n";
         msg += "\n";
         msg += "There is no computer connection driving the pump.\n";
         msg += "It seems to be running on its own.  A previous computer\n";
         msg += "controlled infusion may have left the pump running when\n";
         msg += "it finished or the pump may have been set to run manually.\n";
         msg += "Either way, a new computer controlled connection cannot\n";
         msg += "be established while the pump is running.\n";         
         this.post.error(msg);
      }
         
      this.connection = true;
   }

   
   void TestProtocol () throws PumpException {
      
      if ( ! this.connection ) {
         this.post.error("Attempted to test protocol without first testing connection");
      }
      
      try {
         this.Command("CLD",true); // protocol 44 command to clear volume counters 
      } catch ( Exception e ) {
         this.post.error("Could not test for protocol 44");
      }
      if ( this.response.contains("?") ) {
         this.post.error("Pump not set to protocol 44");
      }
      
      this.protocol = true;
      
      // check for programmable commands
   }
   

   
   void TestProgramable () throws PumpException {
      
      if ( ! this.protocol ) { 
         this.post.error("Attempted to test for programable pump without first testing protocol");
      }
      
      this.Command("SEQ",true); // protocol 44 command valid only on programable pumps
      this.programable = (! this.response.contains("NA"));     
   }
   
   
   
   void Close() throws PumpException {
      
      try {
         sock.close(); sock=null;
         in.close(); in=null;
         out.close(); out=null;
      } catch ( NullPointerException e ) {
         // nothing to close
      } catch ( IOException e ) {
         this.post.error("Could not close socket properly");
      }
   }
   


   // write to pump
   private void Write (String cmd) {
      out.print(cmd+'\r');
      out.flush();
   }
   
   
   // read from pump
   // c[] has to have 1000 elements to hold results of "SEQ" command which
   // is a dump of the program currently loaded into pump.  However, in.read()
   // doesn't seem to like to read any more than 100 chars at a time.  
   private String Read () throws IOException {
      this.status = 'x';
      int bytesRead=0,totalBytesRead=0;
      char [] c = new char[1000];
      while (true) {
         bytesRead = in.read(c,totalBytesRead,100);
         totalBytesRead += bytesRead;
       
         if ( bytesRead == -1 ) { break; }  
         if ( c[totalBytesRead-1] == ':' ) { break; }
         if ( c[totalBytesRead-1] == '>' ) { break; }
         if ( c[totalBytesRead-1] == '<' ) { break; }
         if ( c[totalBytesRead-1] == '/' ) { break; }
         if ( c[totalBytesRead-1] == '*' ) { break; }
         if ( c[totalBytesRead-1] == '^' ) { break; }
         
      }
      // 1st character is always lf
      // last 4 characters are part of status flag
      this.status = c[totalBytesRead-1];            
      String response = new String();
      for (int i=1;i<totalBytesRead-4;i++) {
         response += c[i];
      }
      
      return response.trim();
   }
   
   // datetime command seems to take 35milliseconds first time and 2 milliseconds thereafter
   protected void Command (String cmd) throws PumpException {
      this.Command(cmd,false);
   }
     
   protected void Command (String cmd,boolean test) throws PumpException {   
      try {
         this.Write(cmd);
         this.response = this.Read();
         if ( this.debug ) {
            //Formatter f = new Formatter();
            //f.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL",new Date());
            //String datetime = f.toString();            
            //System.out.println(datetime+"~~~~~~~~~~~~");
            System.out.printf("~~~~~~~~~~~~%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL\n",new Date());
            System.out.println(cmd);
            System.out.println(this.status);
            System.out.println(this.response);
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         }
         if ( this.response.contentEquals("?") && ! test ) {
            this.post.error("Pump command syntactically incorrect\n"+cmd);
         }
      } catch ( IOException e ) {
         throw new PumpException("Error sending command to pump\n"+cmd);
      }
   }
      
   synchronized Map<String,Object> GetMode () throws PumpException {
      String cmd = "MOD";
      String mode;

      this.Command(cmd);
      mode = this.response;
      if ( mode.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      if ( mode.contentEquals("PRGRAM")) {
         mode = "PROGRAM";
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("mode",mode);
      
      return map;     
   }

   // PRGRAM,VOLUME,PUMP
   synchronized Map<String,Object> SetMode (String mode) throws PumpException {
      String cmd = "MOD ";
      String tmp = "";
    
      if ( mode.contentEquals("PROGRAM") ) { tmp = "PGM"; }
      if ( mode.contentEquals("VOLUME") )  { tmp = "VOL"; }
      if ( mode.contentEquals("PUMP") )    { tmp = "PMP"; }
      cmd = cmd + tmp;
      this.Command(cmd);      
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      this.mode = mode;
      
      return map;
   }
   
   
   synchronized Map<String,Object> GetRate () throws PumpException {
      String cmd = "RAT";
      String [] tmpArray;
      String units;
      double rate;
      
      if ( this.mode.contentEquals("PROGRAM") ) { 
         cmd = "PGR";
         this.Command(cmd);
         if ( this.response.contentEquals("NA")) { 
            this.post.error("Command only valid in program mode\n"+cmd);
         }
      } else {
         cmd = "RAT";
         this.Command(cmd);
         if ( this.response.contentEquals("NA")) {
            this.post.error("Command not valid while pump is in program mode\n"+cmd);
         }
      }
      tmpArray = this.response.split(" ");
      rate = Double.parseDouble(tmpArray[0]);
      units = tmpArray[1];
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("rate",rate);
      map.put("units",units);
      
      return map;
   }      
      
   
   synchronized Map<String,Object> SetRate ( double rate ) throws PumpException {
      return this.SetRate(rate,"ml/mn");
   }
   
   synchronized Map<String,Object> SetRate ( double rate, String units ) throws PumpException {
      // round to 5 significant digits -- thats all pump can accept
      int factor = (rate>=10) ? 1000 : 10000;
      rate *= factor;
      rate = Math.round(rate);
      rate /= factor;
      String cmd = "RAT " + rate;

      if ( units.contentEquals("ml/mn") ) { units = "MM"; }
      if ( units.contentEquals("ul/mn") ) { units = "UM"; }
      if ( units.contentEquals("ml/hr") ) { units = "MH"; }
      if ( units.contentEquals("ul/hr") ) { units = "UH"; }
      cmd = cmd + " " + units;
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is in program mode\n"+cmd);
      }
      if ( this.response.contentEquals("OOR")) {
         this.post.error("Invalid Rate\n"+cmd);
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
  
      // everytime user sets a rate or a target vol, make sure its going in the right direction
      // new programable pumps can withdraw as well as infuse
      this.Command("DIR INF");
      
      return map;
   }         
   
   
   synchronized Map<String,Object> GetDiam () throws PumpException {
      String cmd = "DIA";
      double diam;
      
      this.Command(cmd);
      diam = Double.parseDouble(this.response);
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("diam",diam);
      
      return map; 
   }
   
   synchronized Map<String,Object> SetDiam ( double diam ) throws PumpException {
      return this.SetDiam(diam,true);
   }
   
   // changing the diameter will auto-clear the infusion rate
   // run command will give an 'out of range' error if infusion rate
   // is zero even in program mode.  so before changing diameter
   // read current rate and reset that rate afterwards.  
   // ...but when reseting the pump after an infusion we want the rate to go to 
   // zero for safety.  so use a boolean switch to control whether or not we'll 
   // reset the rate when changing the diameter.
   synchronized Map<String,Object> SetDiam ( double diam, boolean keepRate ) throws PumpException {
      // round to 4 digits -- thats all pump can accept
      diam *= 10000;
      diam = Math.round(diam);
      diam /= 10000;
      String cmd = "DIA "+diam;
      double rate;
      
      // save old infusion rate
      rate = (Double)this.GetRate().get("rate");
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      if ( this.response.contentEquals("OOR")) {
         this.post.error("Invalid Diameter\n"+cmd);
      }
         
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
  
      // reset old infusion rate
      if ( rate != 0 && keepRate) {
         this.SetRate(rate);
      }
  
      return map;
   }         
   
   
   synchronized Map<String,Object> GetTargetVol () throws PumpException {
      String cmd = "TGT";
      double vol;
      
      this.Command(cmd);
      vol = Double.parseDouble(this.response);
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("targetVol",vol);
      
      return map; 
   }
      
   
   synchronized Map<String,Object> SetTargetVol ( double vol ) throws PumpException {
      // round to 5 significant digits -- thats all pump can accept
      int factor = (vol>=10) ? 1000 : 10000;
      vol *= factor;
      vol = Math.round(vol);
      vol /= factor;
      String cmd = "TGT " + vol;
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      if ( this.response.contentEquals("OOR")) {
         this.post.error("Invalid Target Volume\n"+cmd);
      }
   
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      
      // everytime user sets a rate or a target vol, make sure its going in the right direction
      // new programable pumps can withdraw as well as infuse
      this.Command("DIR INF");
      
      return map; 
   }
      
      
   synchronized Map<String,Object> GetInfusedVol () throws PumpException {
      String cmd = "DEL";
      double vol;
      
      this.Command(cmd);
      vol = Double.parseDouble(this.response);
               
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("volume",vol);
      
      return map; 
   }
   
   
   synchronized Map<String,Object> ZeroInfusedVol () throws PumpException {
      String cmd = "CLD";
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      
      return map; 
   }
      
   
   synchronized Map<String,Object> Run () throws PumpException {
      String cmd = "RUN";
      
      this.Command(cmd);   
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      if ( this.response.contentEquals("OOR")) {
         this.post.error("Out of Range\n"+cmd);
      }
      if ( this.status != '>' ) {
         String msg;
         msg = "Run command failed.\n";
         msg += "This is likely due to an untrapped 'out of range' error\n";
         msg += "Check pump settings for diameter, infusion rate and\n";
         msg += "target volume.  These all have to be valid even when \n";
         msg += "running in program mode.  Changing the diameter can clear\n";
         msg += "the infusion rate.";
         this.post.error(msg);
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      
      return map; 
   }
   
   // The pump objects if its told to stop when its already stopped.
   // Normally, we dutifly report these objections as a problem someone
   // should do something about.  There is a case though when this isn't 
   // desirable.  When we shutdown the program, a pump stop command is 
   // issued just to make sure.  The pump may or may not still be running.
   // So we may or not get an error message on screen.  We'd rather not
   // get any error message in this one case.  If set to false, the
   // showErrors flag will not report any pump problems.  By default,
   // its set to true. 
   synchronized Map<String,Object> Stop () throws PumpException {
      return this.Stop(true);
   }
   
   synchronized Map<String,Object> Stop (boolean showErrors) throws PumpException {
      String cmd = "STP";
      
      this.Command(cmd);
      if ( showErrors ) {
         if ( this.response.contentEquals("NA")) {
            this.post.error("Command not valid while pump is stopped\n"+cmd);
         }
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      
      return map; 
   }
      
   
   synchronized Map<String,Object> GetVersion () throws PumpException {
      String cmd = "VER";
      
      this.Command(cmd);
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("version",this.response);
      
      return map; 
   }
     
      
   synchronized Map<String,Object> ClearPgm () throws PumpException {    
      String cmd = "SEQ 1 MOD STP";
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
         
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      
      return map;
   }
   
   
   synchronized Map<String,Object> GetPgm () throws PumpException {
      String cmd = "SEQ";
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("program",this.response);
            
      return map;
   }
         
   
   synchronized Map<String,Object> GetPgmMode (int seq) throws PumpException {
      String cmd = "SEQ " + seq + " MOD ";
      String mode;
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
            
      this.Command(cmd);
      mode = this.response;
      if ( mode.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      if ( mode.contentEquals("STP") ) { mode = "stop";      }
      if ( mode.contentEquals("PRO") ) { mode = "profile";   }
      if ( mode.contentEquals("INC") ) { mode = "increment"; }
      if ( mode.contentEquals("DEC") ) { mode = "decrement"; }
      if ( mode.contentEquals("DIS") ) { mode = "dispense";  }
      if ( mode.contentEquals("PAS") ) { mode = "pause";     }
      if ( mode.contentEquals("RST") ) { mode = "restart";   }
      if ( mode.contentEquals("GOT") ) { mode = "go to";     }
      if ( mode.contentEquals("EVN") ) { mode = "event";     }
      if ( mode.contentEquals("PMP") ) { mode = "pump";      }
      if ( mode.contentEquals("OUT") ) { mode = "TTL out";   }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("mode",mode);   
      
      return map;
   }
   
   
   
   synchronized Map<String,Object> SetPgmMode (int seq,String mode) throws PumpException {
      String cmd = "SEQ " + seq + " MOD ";
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
      
      if ( mode.contentEquals("stop") )      { mode = "STP"; }
      if ( mode.contentEquals("profile") )   { mode = "PRO"; }
      if ( mode.contentEquals("increment") ) { mode = "INC"; }
      if ( mode.contentEquals("decrement") ) { mode = "DEC"; }
      if ( mode.contentEquals("dispense") )  { mode = "DIS"; }
      if ( mode.contentEquals("pause") )     { mode = "PAS"; }
      if ( mode.contentEquals("restart") )   { mode = "RST"; }
      if ( mode.contentEquals("go to") )     { mode = "GOT"; }
      if ( mode.contentEquals("event") )     { mode = "EVN"; }
      if ( mode.contentEquals("pump") )      { mode = "PMP"; }
      if ( mode.contentEquals("TTL out") )   { mode = "OUT"; }
      
      cmd = cmd + mode;
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      
      return map;
   }
   
//    
//    // get active rate during while program is running
//    synchronized Map<String,Object> GetPgmRate () throws Exception {
//       String cmd = "PGR";
//       double rate;
//       String units;
//       String [] tmpArray;
//             
//       this.Command(cmd);
//       if ( this.response.contentEquals("NA")) {
//          this.post.error("Command valid only while pump running a profile\n"+cmd);
//       }
//       tmpArray = this.response.split(" ");
//       rate = Double.parseDouble(tmpArray[0]);
//       units = tmpArray[1];
//       
//       Map<String,Object> map = new HashMap<String,Object>();
//       map.put("command",cmd);
//       map.put("status",this.status);
//       map.put("response",this.response);
//       map.put("rate",rate);
//       map.put("units",units);
//       
//       return map;
//    }      
//    
   
   synchronized Map<String,Object> GetPgmRate ( int seq ) throws PumpException {
      String cmd = "SEQ " + seq + " RAT ";
      double rate;
      String units;
      String [] tmpArray;
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      tmpArray = this.response.split(" ");
      rate = Double.parseDouble(tmpArray[0]);
      units = tmpArray[1];
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("rate",rate);
      map.put("units",units);
      
      return map;
   }      
    
    
   synchronized Map<String,Object> SetPgmRate (int seq,double rate) throws PumpException {
      return this.SetPgmRate(seq,rate,"ml/mn");
   }
    
   synchronized Map<String,Object> SetPgmRate (int seq,double rate,String units) throws PumpException {  
      // round to 5 significant digits -- thats all pump can accept
      int factor = (rate>=10) ? 1000 : 10000;      
      rate *= factor;
      rate = Math.round(rate);
      rate /= factor;
      String cmd = "SEQ " + seq + " RAT " + rate; 
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
   
      if ( units.contentEquals("ml/mn") ) { units = "MM"; }
      if ( units.contentEquals("ul/mn") ) { units = "UM"; }
      if ( units.contentEquals("ml/hr") ) { units = "MH"; }
      if ( units.contentEquals("ul/hr") ) { units = "UH"; }
      cmd = cmd + " " + units;
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      if ( this.response.contentEquals("OOR")) {
         this.post.error("Invalid Rate\n"+cmd);
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
  
      // everytime user sets a rate or a target vol, make sure its going in the right direction
      // new programable pumps can withdraw as well as infuse
      this.Command("SEQ " + seq + " DIR INF");
  
      return map;
   }
   
   
   synchronized Map<String,Object> GetPgmTargetVol (int seq) throws PumpException {
      String cmd = "SEQ " + seq + " TGT ";
      double vol;
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      vol = Double.parseDouble(this.response);
   
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("targetVol",vol);
      
      return map;
   }
   
   synchronized Map<String,Object> SetPgmTargetVol (int seq,double vol) throws PumpException {
      // round to 5 significant digits -- thats all pump can accept
      int factor = (vol>=10) ? 1000 : 10000;
      vol *= factor;
      vol = Math.round(vol);
      vol /= factor;
      String cmd = "SEQ " + seq + " TGT " + vol;
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      if ( this.response.contentEquals("OOR")) {
         this.post.error("Invalid Target Volume\n"+cmd);
      }
   
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      
      // everytime user sets a rate or a target vol, make sure its going in the right direction
      // new programable pumps can withdraw as well as infuse
      this.Command("SEQ " + seq + " DIR INF");
      
      return map;
   }
   
   
   synchronized Map<String,Object> GetPgmTime (int seq) throws PumpException {
      String cmd = "SEQ " + seq + " INT";
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("time",this.response);
      
      return map;
   }
   
   // time must be 0 or 0:00:00  
   synchronized Map<String,Object> ClearPgmTime (int seq) throws PumpException {
      return this.SetPgmTime(seq,"0");
   }
    
   // time must be 0 or 0:00:00  
   synchronized Map<String,Object> SetPgmTime (int seq,String time) throws PumpException {
      String cmd = "SEQ " + seq + " INT " + time;
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      if ( this.response.contentEquals("OOR")) {
         this.post.error("Invalid Time Setting\n"+cmd);
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      
      return map;
   }
   
   
   synchronized Map<String,Object> GetPgmDirection () throws PumpException {
      String cmd = "DIR";
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      map.put("direction",this.response);
      
      return map;
   }
   
   
   synchronized Map<String,Object> SetPgmDirection (int seq,String dir) throws PumpException {
      String cmd = "SEQ " + seq + " DIR " + dir;
      if ( this.programable != true ) { 
         this.post.error("Command valid only on programable pumps\n"+cmd);
      }      
      
      this.Command(cmd);
      if ( this.response.contentEquals("NA")) {
         this.post.error("Command not valid while pump is running\n"+cmd);
      }
      if ( this.response.contentEquals("OOR")) {
         this.post.error("Invalid Direction\n"+cmd);
      }
      
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("command",cmd);
      map.put("status",this.status);
      map.put("response",this.response);
      
      return map;
   }
   

   
   synchronized void LoadPgm (Double diam,Double dsRate,Double dsVol,Double bolusRate,Double bolusVol,Double ssRate,Double ssVol) throws PumpException {

      Map<String,Object> map = new HashMap<String,Object>();
      int seq = 1;   
      
      map = SetMode("PROGRAM");
      map = SetDiam(diam);
      map = SetRate(1);
      map = this.ClearPgm();
         
//       if ( ! (dsRate == null) ) {
//          if ( ! (dsVol == null) ) {            
      if ( dsRate != null && dsRate != 0 ) {
         if ( dsVol != null && dsVol != 0 ) {            
            map = this.SetPgmMode(seq,"profile");
            map = this.SetPgmRate(seq,dsRate);
            map = this.SetPgmTime(seq,"0");
            map = this.SetPgmTargetVol(seq,dsVol);
            map = this.SetPgmDirection(seq,"INF");            
            seq++;
         }
      }
         
//       if ( ! (bolusRate == null) ) {
//          if ( ! (bolusVol == null) ) {
      if ( bolusRate != null && bolusRate != 0 ) {
         if ( bolusVol != null && bolusVol != 0 ) {
            map = this.SetPgmMode(seq,"profile");
            map = this.SetPgmRate(seq,bolusRate);
            map = this.SetPgmTime(seq,"0");
            map = this.SetPgmTargetVol(seq,bolusVol);
            map = this.SetPgmDirection(seq,"INF");            
            seq++;
         }
      }
         
//       if ( ! (ssRate == null) ) {
//          if ( ! (ssVol == null) ) {
      if ( ssRate != null && ssRate != 0 ) {
         if ( ssVol != null && ssVol != 0 ) {
            map = this.SetPgmMode(seq,"profile");
            map = this.SetPgmRate(seq,ssRate);
            map = this.SetPgmTime(seq,"0");
            map = this.SetPgmTargetVol(seq,ssVol);
            map = this.SetPgmDirection(seq,"INF");            
            seq++;
         }
      }
              
      this.SetPgmMode(seq,"stop");     
   }
      

}
   
   
