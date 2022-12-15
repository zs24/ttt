

public class AlarmBells implements Runnable {
   int beeps = 3; // how many beeps in sequence
   int pause = 5000;  // how long to pause between sequences
   int loops = 10;  // how many times to repeat sequence

   AlarmBells () {
   }
   
   AlarmBells (int beeps,int pause,int loops) {
      this.beeps = beeps;
      this.pause = pause;
      this.loops = loops;
   }

   public void run () {
   
      int loop = 0;
      char beep = (char)(byte)7;
      while (loop++ < this.loops) {
         for (int i=0;i<this.beeps;i++) {
            System.out.print(beep);
            this.sleep(250);
         }
         System.out.print("");
         this.sleep(this.pause);
      }
   }
   
   public void stop () {
      this.loops = 0;
   }

   private void sleep (int time) {
      try {
         Thread.sleep(time);
      } catch ( Exception e ) {
         // do nothing
      }
   }
}
