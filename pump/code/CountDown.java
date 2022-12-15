import java.sql.Timestamp;

public class CountDown implements Runnable {
   long end;
   long delay;
   boolean loop = true;
   CountdownCallback callback;
      
   
   CountDown (CountdownCallback callback) {
      this(null,1000,callback);
   }
   
   CountDown (Timestamp endTime,CountdownCallback callback) {
      this(endTime,1000,callback);
   }
   
   CountDown (Timestamp endTime,int delay,CountdownCallback callback) {
      this.setTime(endTime);
      this.callback = callback;   
      this.delay = delay;
   }
   
   public void setTime (Timestamp endTime) {
      this.end = ( endTime == null ) ? 0 : endTime.getTime();
   }
   
   public void run () {
      this.loop = true;
      long now = 0;
      int diff = 0;
      while ( this.loop ) {
         now = System.currentTimeMillis();
         diff = (int)(now-end);
         this.callback.countdownInfo(diff);
         try {
            Thread.sleep(this.delay);
         } catch (InterruptedException e ) {
            // do nothing special
            System.out.println("cant sleep");
         }
      }
   }
   
   public void stop () {
      this.loop = false;
   }
}
