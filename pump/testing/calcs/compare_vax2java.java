import java.io.*;   
import java.sql.Timestamp;



   
public class compare_vax2java {


   
//          if ( line.matches("FRED_1_2DEC1994_PUMP.*") ) { continue; } // exclude this one pump run
//          if ( line.matches("SC_3_11FEB2004_PUMP.*") ) { continue; }  // exclude this one pump run         


   public static void main (String [] args) {
      
      // get directory listing of all xml files
      File dir = new File("/pet/charles/java/vax_savefiles");
      FilenameFilter filter = new FilenameFilter() {
         public boolean accept(File dir, String name) {
            if ( ! name.contains("BAYSDON_1_8JUL2004") ) { return false; }
                
            if ( ! name.matches(".*.xml") ) { return false; }
            if ( name.equals("tmp.xml") ) { return false; }
            if ( name.contains("FRED_1_2DEC1994") ) { return false; }
            if ( name.contains("TEST_1_2DEC1994") ) { return false; }
            
            return true;            
          }
      };
      File [] xmlList = dir.listFiles(filter);
      

      PumpMessages post = new PumpMessages();
     
     
      // loop through all xml files
      for (int i=0;i<xmlList.length;i++) {
         String file = xmlList[i].toString();
         String run = xmlList[i].getName().replaceAll("_PUMP.xml","");
         
      
         // read xml file
         PumpParameters parms=null;
         try {
            parms = new PumpParameters(post,file);
         } catch ( PumpException e ) {
            System.out.println("Parms load failed");
            e.printStackTrace();
         }
         
         
         // some vax times show fractions of a second like so "08:15:.05" meaning
         // 8 hours 15 minutes and 0.05 seconds.  Java cant handle that.  
         // convert all ":." to ":0.".  Then "08:15:.05" becomes "08:15:0.05" 
         // which java can handle.  
         parms.put("user/systime",parms.getString("user/systime").replace(":.",":0."));
         parms.put("user/time0"  ,parms.getString("user/time0").replace(":.",":0."));
         parms.put("user/injtime",parms.getString("user/injtime").replace(":.",":0."));


         // vax decay calculations are done as of "systime"
         // injtime is usually 2 or 3 seconds later
         // sometimes injtime is 1 or 2 minutes later
         // this difference is throwing off %errors 
         // use vax systime as java injtime.  
         parms.put("user/injtime",parms.getString("user/systime"));       

         // do calculations as of injtime
         // bld memory dump only records the time portion of the datetime stamp
         // substitute today's date to create java timestamp
         String today = new Timestamp(System.currentTimeMillis()).toString().substring(0,11);
         Timestamp injtime = Timestamp.valueOf(today + parms.getString("user/injtime"));
         
         // do calculations
         try { 
            double dose_now = parms.calc_dose(injtime);
            parms.calc_fraction();
            parms.calc_volume(dose_now,false);  // disable warnings
            parms.calc_profile(dose_now,false);  // disable warnings
            parms.calc_predictedDose();
            parms.writeXMLout("tmp.xml");
         } catch ( PumpException e ) {
            System.out.println("Problems doing calculations");
            e.printStackTrace();
         }
       
         double vaxDSrate = 0.0;
         double vaxBSrate = 0.0;
         double vaxSSrate = 0.0;
         Double vaxDStime = 0.0;
         Double vaxBStime = 0.0;
         Double vaxSStime = 0.0;
         double vaxPredicted = 0.0;
         if ( parms.get("vax_profile/rate1") == null ) {
            // do nothing
            vaxPredicted = (Double)parms.get("vax_calc/dose_predicted");
         } else if ( parms.get("vax_profile/rate2") == null ) {
            if ( (Double)parms.get("user/ds") != 0 ) {
               // vax time1 --> ds/time
               vaxDSrate = (Double)parms.get("vax_profile/rate1");
               vaxDStime = (Double)parms.get("vax_profile/time1");
               vaxPredicted = (Double)parms.get("vax_calc/dose_predicted");
            } else if ( (Double)parms.get("config/kbol") != 0 ) {
               // vax time1 --> bs/time
               vaxBSrate = (Double)parms.get("vax_profile/rate1");
               vaxBStime = (Double)parms.get("vax_profile/time1");
               vaxPredicted = (Double)parms.get("vax_calc/dose_predicted");
            } else {
               // vax time1 --> ss/time
               vaxSSrate = (Double)parms.get("vax_profile/rate1");
               vaxSStime = (Double)parms.get("vax_profile/time1");
               vaxPredicted = (Double)parms.get("vax_calc/dose_predicted");
            }
         } else if ( parms.get("vax_profile/rate3") == null ) {
            if ( (Double)parms.get("user/ds") == 0 ) {
               // vax time1 --> Bs/time
               // vax time2 --> ss/time
               vaxBSrate = (Double)parms.get("vax_profile/rate1");
               vaxSSrate = (Double)parms.get("vax_profile/rate2");
               vaxBStime = (Double)parms.get("vax_profile/time1");
               vaxSStime = (Double)parms.get("vax_profile/time2");
               vaxPredicted = (Double)parms.get("vax_calc/dose_predicted");
            } else {
               // vax time1 --> ds/time
               // vax time2 --> ss/time
               vaxDSrate = (Double)parms.get("vax_profile/rate1");
               vaxSSrate = (Double)parms.get("vax_profile/rate2");
               vaxDStime = (Double)parms.get("vax_profile/time1");
               vaxSStime = (Double)parms.get("vax_profile/time2");
               vaxPredicted = (Double)parms.get("vax_calc/dose_predicted");
            }
         } else {
            // vax time1 --> ds/time
            // vax time2 --> bolus/time
            // vax time3 --> ss/time
            vaxDSrate = (Double)parms.get("vax_profile/rate1");
            vaxBSrate = (Double)parms.get("vax_profile/rate2");
            vaxSSrate = (Double)parms.get("vax_profile/rate3");
            vaxDStime = (Double)parms.get("vax_profile/time1");
            vaxBStime = (Double)parms.get("vax_profile/time2");
            vaxSStime = (Double)parms.get("vax_profile/time3");
            vaxPredicted = (Double)parms.get("vax_calc/dose_predicted");
         }
      
         double unixDSrate = (Double)parms.get("infuse/ds/rate");
         double unixBSrate = (Double)parms.get("infuse/bolus/rate");
         double unixSSrate = (Double)parms.get("infuse/ss/rate");
         double unixDStime = (Double)parms.get("infuse/ds/time");
         double unixBStime = (Double)parms.get("infuse/bolus/time");
         double unixSStime = (Double)parms.get("infuse/ss/time");
         double unixPredicted = (Double)parms.get("calc/dose_predicted");
         
         double diffDStime = (unixDStime*100.0-vaxDStime*100.0)/vaxDStime;
         double diffBStime = (unixBStime*100.0-vaxBStime*100.0)/vaxBStime;
         double diffSStime = (unixSStime*100.0-vaxSStime*100.0)/vaxSStime;
         double diffDSrate = (unixDSrate*100.0-vaxDSrate*100.0)/vaxDSrate;
         double diffBSrate = (unixBSrate*100.0-vaxBSrate*100.0)/vaxBSrate;
         double diffSSrate = (unixSSrate*100.0-vaxSSrate*100.0)/vaxSSrate;
         double diffPredicted = (unixPredicted*100.0-vaxPredicted*100.0)/vaxPredicted;

         // clear 0/0 errors that appear on excel as NaN
         if ( unixDStime == 0 && vaxDStime == 0 ) { diffDStime = 0; }
         if ( unixBStime == 0 && vaxBStime == 0 ) { diffBStime = 0; }
         if ( unixSStime == 0 && vaxSStime == 0 ) { diffSStime = 0; }
         if ( unixDSrate == 0 && vaxDSrate == 0 ) { diffDSrate = 0; }
         if ( unixBSrate == 0 && vaxBSrate == 0 ) { diffBSrate = 0; }
         if ( unixBSrate == 0 && vaxSSrate == 0 ) { diffSSrate = 0; }         
         if ( unixPredicted == 0 && unixPredicted == 0 ) { diffPredicted = 0; }
         
         // when a phase gets skipped (i.e. no bolus) this program reads a
         // 0 for vax time for that phase and carries the time from the 
         // previous phase over for java time.  That creates n/0 errors
         // that appear on excel as 'infinity'.  
         if ( unixDSrate == 0 && vaxDSrate == 0 ) { diffDStime = 0; }
         if ( unixBSrate == 0 && vaxBSrate == 0 ) { diffBStime = 0; }
         if ( unixSSrate == 0 && vaxSSrate == 0 ) { diffSStime = 0; }
                  
         
         String line = String.format("%20s,%8.6f,%8.6f,%6.2f,%8.6f,%8.6f,%6.2f,%8.6f,%8.6f,%6.2f,%8.6f,%8.6f,%6.2f,%8.6f,%8.6f,%6.2f,%8.6f,%8.6f,%6.2f,%8.6f,%8.6f,%6.2f",
               run,
               vaxDStime,
               unixDStime,
               diffDStime,
               vaxBStime,
               unixBStime,
               diffBStime,
               vaxSStime,
               unixSStime,
               diffSStime,
               vaxDSrate,
               unixDSrate,
               diffDSrate,
               vaxBSrate,
               unixBSrate,
               diffBSrate,
               vaxSSrate,
               unixSSrate,
               diffSSrate,
               vaxPredicted,
               unixPredicted,
               diffPredicted);
               
         
         System.out.println(line);
        
         
       }
   }
}
