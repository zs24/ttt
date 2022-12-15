import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.sql.Timestamp;


public class PumpMonitor implements Runnable { 
   PumpCommands pump;
   PumpCallback receiver;
   long interval;
   boolean loop = true;

   PumpMonitor(PumpCommands p,PumpCallback r,long i) {
      pump = p;
      receiver = r;
      interval = i;
   }

   public void run () {
      Map<String,Object> map = new HashMap<String,Object>();
      double rate;
      double vol;
      char status;
      Timestamp datetime = new Timestamp(System.currentTimeMillis()); 
      
      while ( loop ) {
         datetime.setTime(System.currentTimeMillis());
         try { 
            map = pump.GetInfusedVol();
            vol = (Double)map.get("volume");
            map = pump.GetRate();
            rate = (Double)map.get("rate");
            status = (Character)map.get("status");
                   
            map.clear();
            map.put("rate",rate);
            map.put("vol",vol);
            map.put("status",status);
            map.put("datetime",datetime);
            receiver.pumpInfo(map);
         } catch (PumpException e) {
            map = new HashMap<String,Object>();
            map.put("rate",-999.99);
            map.put("vol",-999.99);
            map.put("status",'?');
            map.put("datetime",datetime);
            receiver.pumpInfo(map);
         } catch (NullPointerException e ) {
            System.out.println("Program Error");
            System.out.println("Screwed up a HashMap key");
            System.out.println(e.getMessage());
            e.printStackTrace();
         } catch ( Exception e ) {
            e.printStackTrace();
            map = new HashMap<String,Object>();
            map.put("rate",-999.99);
            map.put("vol",-999.99);
            map.put("status",'?');
            map.put("datetime",datetime);
            receiver.pumpInfo(map);
         }
         
         try {
            Thread.sleep(interval);
         } catch (InterruptedException e ) {
            // do nothing special
         }
      }
   }


   public void stop () {
      this.loop = false;
   }

}
