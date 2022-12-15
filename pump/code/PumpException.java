
class PumpException extends Exception {
   // dummy class to distinguish Pump errors from standard Java errors
   
   PumpException () {
      super();
   }
   
   PumpException (String msg) {
      super(msg);
   }

}
