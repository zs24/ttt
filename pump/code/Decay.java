import java.util.Date;
import java.sql.Timestamp;

public class Decay {
   enum HalfLifeUnits { Minutes,Hours,Days,Years };

   static public double activity (double act0,Date time0,Date timeN,double hl,HalfLifeUnits units) {
      double delta;
             
      delta = (timeN.getTime() - time0.getTime());

      // Date.getTime returns time in milliseconds 
      // convert delta to the same units as halflife    
      switch ( units ) {
         case Minutes : delta /= ( 60 * 1000 ); break;
         case Hours   : delta /= ( 60 * 60 * 1000 ); break;
         case Days    : delta /= ( 24 * 60 * 60 * 1000 ); break;
         case Years   : delta /= ( 365 * 24 * 60 * 1000 ); break;
      }
      
      return activity(act0,delta,hl);
   }
   

   // assume delta and hl are given in the same units
   static public double activity (double act0,double delta,double hl) {
      double actN;

      actN = act0 * Math.pow(2,(-delta/hl));
      
      return actN;
   }



   static public double deltaTime ( double act0,double actN,double hl) {
      double delta;
      
      // t = -(hl/0.693) * ln(N/No)
      delta = (hl/Math.log(2)) * Math.log(actN/act0);
   
      return delta;
   }



   static public Timestamp timeStamp ( double act0,Date time0,double actN,double hl,HalfLifeUnits units) {
      double delta;
      Timestamp timeN;
      
      // t = -(hl/0.693) * ln(N/No)
      delta = deltaTime(act0,actN,hl);
      
      // convert delta time to milliseconds    
      // Date.getTime returns time in milliseconds 
      // convert delta to the same units as halflife    
      switch ( units ) {
         case Minutes : delta /= ( 60 * 1000 ); break;
         case Hours   : delta /= ( 60 * 60 * 1000 ); break;
         case Days    : delta /= ( 24 * 60 * 60 * 1000 ); break;
         case Years   : delta /= ( 365 * 24 * 60 * 1000 ); break;
      }
      
      // add delta time to time0      
      timeN = new Timestamp((long)(time0.getTime() + delta));
      
      return timeN;
   }

}
