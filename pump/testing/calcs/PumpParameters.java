//    Double diam;             // syringe diameter (mm)
//    String syringe_name;     // syringe name
//    Double dose_warning;     // gui flashes warning to user if user says activity in syringe is greater than this
//    Double minvol_warning;   // gui flashes warning to user if user says volume in syringe is less than this
//    Double maxvol_warning;   // gui flashes warning to user if user says volume in syringe is more than this
//    Double ds;               // dead space volume (cc)
//    Double dsdef;            // default dead space volume (cc) 
//    Boolean prompt_ds;       // gui prompts user for dead space volume if this flag is set
//    Double kbol;             // 
//    Double tbolus;           // duration of bolus phase (min)
//    Double t;                // duration of infusion phase (min)
//    Double tdef;             // default duration of infusion phase (min)
//    Double tmin;             // 
//    Double tmax;             //
//    Double desired_def;      // default desired dose (mCi)
//    Double desired_warning;  // gui flashes a warning to user if user says desired dose is greater than this
//    Boolean inject_all_dose; // inject whole syringe if this flag is set
//    Boolean mass_check;      // check for mass limits to volume if this flag is set
//    Double rmax;             // maximum infusion rate (ml/min)
//    Double time_warning;     // gui flashes a warning to user if the time (in minutes) between assay time and the current time is greater than this
//    Boolean dec;             // isotope does not decay if this flag is not set
//    Double hl;               // isotope halflife (minutes)
//    Vector<String> notes = new Vector<String>();
//    
//    String XMLfile;
//    Double frac;             //
//    Double desired;          // dose to be given to patient (mCi) (user-specified)
//    Double vtot;             // total volume in syringe (ml) (user-specified)
//    Double max_vol_mass;     // total volume allowed due to mass limitations (ml) (user-specified)
//    Double vol;              // volume to infuse (ml) (calculated)
//    Double dose_predicted;   // expected dose to patient (mCi) calculated)

import java.util.Vector;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Date;
import java.sql.Timestamp;
import java.lang.Math;

public class PumpParameters {
   // object internals
   String emptyXML = "/pet/charles/java/blank.xml";
   Boolean debug = false;
   HashMap<String,Object> parms = new HashMap<String,Object>();
   Vector<String> notes = new Vector<String>();
   PumpMessages post;
   String xmlroot = "parms/";
   enum Types { STRING,DOUBLE,FLOAT,INTEGER,BOOLEAN,DATETIME,TIME };
   String today = new Timestamp(System.currentTimeMillis()).toString().substring(0,11);
   
    
   PumpParameters (PumpMessages post) {
      this.post = post;
   } 
    
   PumpParameters (PumpMessages post,String file) throws PumpException {
      this(post);
      readXMLin(file);
   }
   
   
   public void put (String key,Object value) {
      this.parms.put(key,value);
   }

   
   public void put (String key,String value,Types type) {
   
      // if there are any Exceptions set field to null
      this.put(key,null); 
      switch ( type ) {
         case STRING : 
            try { 
               this.put(key,value); 
            }
            catch ( NullPointerException e ) { if ( debug ) { System.out.println("Error: Null Pointer\n" + key + "-->" + value ); } }
            catch ( NumberFormatException e ) { if ( debug ) { System.out.println("Error: Not a Number\n" + key + "-->" + value ); } }
            break;
         case DOUBLE   :
            try { 
               this.put(key,Double.valueOf(value)); 
            }
            catch ( NullPointerException e ) { if ( debug ) { System.out.println("Error: Null Pointer\n" + key + "-->" + value ); } }
            catch ( NumberFormatException e ) { if ( debug ) { System.out.println("Error: Not a Number\n" + key + "-->" + value ); } }
            break;
         case FLOAT :         
            try { 
               this.put(key,Float.valueOf(value));
            }
            catch ( NullPointerException e ) { if ( debug ) { System.out.println("Error: Null Pointer\n" + key + "-->" + value ); } } 
            catch ( NumberFormatException e ) { if ( debug ) { System.out.println("Error: Not a Number\n" + key + "-->" + value ); } }
            break;
         case INTEGER :
            try {
               this.put(key,Integer.valueOf(value));
            }
            catch ( NullPointerException e ) { if ( debug ) { System.out.println("Error: Null Pointer\n" + key + "-->" + value ); } } 
            catch ( NumberFormatException e ) { if ( debug ) { System.out.println("Error: Not a Number\n" + key + "-->" + value ); } }
            break;
         case BOOLEAN :
            // should be able to use Boolean.valueOf(value) but it doesn't work
            // "true" becomes true, "false" becomes false but "1" and "0" are both false
            // override java behavior so "1" and "true" are true and "0" and "false" are false
            
            this.put(key,null);
            if ( value != null ) {
               value = value.trim().toUpperCase();            
               if ( value.equals("1") ) { this.put(key,true); }
               if ( value.equals("0") ) { this.put(key,false); }
               if ( value.equals("TRUE") ) { this.put(key,true); }
               if ( value.equals("FALSE") ) { this.put(key,false); }
            }
            
//             try {
//                this.put(key,Boolean.valueOf(value));
//             }
//             catch ( NullPointerException e ) { if ( debug ) { System.out.println("Error: Null Pointer\n" + key + "-->" + value ); } } 
//             catch ( NumberFormatException e ) { if ( debug ) { System.out.println("Error: Not a Boolean\n" + key + "-->" + value ); } }            
            break;
         case DATETIME :
            try {
               this.put(key,Timestamp.valueOf(value));
            }
            catch ( IllegalArgumentException e ) { if ( debug ) { System.out.println("Error: Not a Date\n" + key + "-->" + value); } } 
            break;
         case TIME :
            // treat as regular string that just happens to be formated hh:mm:ss 
            try { 
               this.put(key,value); 
            }
            catch ( NullPointerException e ) { if ( debug ) { System.out.println("Error: Null Pointer\n" + key + "-->" + value ); } }
            catch ( NumberFormatException e ) { if ( debug ) { System.out.println("Error: Not a Number\n" + key + "-->" + value ); } }
            break;
      }
   }
   
   
   public Object get (String key) {
      return this.parms.get(key);
   }

   
   public Object get (String key,Types type) {
      Object value = this.get(key);
      switch ( type ) {
         case STRING : 
            break;
         case DOUBLE   :
            break;
         case FLOAT :         
            break;
         case INTEGER :
            break;
         case BOOLEAN :
            break;
         case DATETIME :
            break;
         case TIME :
            // value is a formatted string (hh:mm:ss)
            // prepend current date string and create a Timestamp object
            String datetime = this.today + (String)value;
            value = Timestamp.valueOf(datetime);
            break;
      }
      return value;
   }

   
   public String getString (String key) {
      Object tmp = this.parms.get(key);
      return ( tmp == null )  ? "" : tmp.toString();
   }
   
 //   public Set<String> keySet () {
//       Set<String> tmp1 = this.parms.keySet();
//       Set<String> tmp2 = new List<String>();
//       int index = this.xmlroot.length();
//       System.out.println(index);
//       for ( String key : tmp1 ) {
//          System.out.println(key);
//          System.out.println(key.substring(index));
//          tmp2.add(key.substring(index));
//       }
//       
//       System.out.println(tmp1);
//       System.out.println(tmp2);
//       
//       return tmp2;
//    }
//       
      
   
   
   
   public void readXMLin (String file) throws PumpException {
      String tmp;
      
      DomParserBean xml = null;
      try {
         xml = new DomParserBean(file);
      } catch ( Exception e ) {
         this.post.error(e.getMessage());
      }         
      xml.setDebug(false);
      
      // set default values
      
      // xml file stores everything as string.
      // parms HashMap stores everything by its proper variable type.
      // put accepts a variable type declaration (of a sort) so it
      // can do the string to variable type conversion for you.  It also
      // does all the necessary try/catch blocking so program doesn't crash
      // if it attempts to read a tag that isn't in the xml file.
      this.put("config/diam"            , xml.getNestedTagValue(this.xmlroot+"config/diam")            , Types.DOUBLE);           
      this.put("config/syringe_name"    , xml.getNestedTagValue(this.xmlroot+"config/syringe_name")    , Types.STRING);     
      this.put("config/dose_warning"    , xml.getNestedTagValue(this.xmlroot+"config/dose_warning")    , Types.DOUBLE);    
      this.put("config/minvol_warning"  , xml.getNestedTagValue(this.xmlroot+"config/minvol_warning")  , Types.DOUBLE);  
      this.put("config/maxvol_warning"  , xml.getNestedTagValue(this.xmlroot+"config/maxvol_warning")  , Types.DOUBLE);  
      this.put("config/dsdef"           , xml.getNestedTagValue(this.xmlroot+"config/dsdef")           , Types.DOUBLE);           
      this.put("config/prompt_ds"       , xml.getNestedTagValue(this.xmlroot+"config/prompt_ds")       , Types.BOOLEAN);       
      this.put("config/kbol"            , xml.getNestedTagValue(this.xmlroot+"config/kbol")            , Types.DOUBLE);            
      this.put("config/tbolus"          , xml.getNestedTagValue(this.xmlroot+"config/tbolus")          , Types.DOUBLE);          
      this.put("config/tdef"            , xml.getNestedTagValue(this.xmlroot+"config/tdef")            , Types.DOUBLE);            
      this.put("config/tmin"            , xml.getNestedTagValue(this.xmlroot+"config/tmin")            , Types.DOUBLE);            
      this.put("config/tmax"            , xml.getNestedTagValue(this.xmlroot+"config/tmax")            , Types.DOUBLE);            
      this.put("config/desired_def"     , xml.getNestedTagValue(this.xmlroot+"config/desired_def")     , Types.DOUBLE);     
      this.put("config/desired_warning" , xml.getNestedTagValue(this.xmlroot+"config/desired_warning") , Types.DOUBLE); 
      this.put("config/inject_all_dose" , xml.getNestedTagValue(this.xmlroot+"config/inject_all_dose") , Types.BOOLEAN); 
      this.put("config/mass_check"      , xml.getNestedTagValue(this.xmlroot+"config/mass_check")      , Types.BOOLEAN);      
      this.put("config/rmax"            , xml.getNestedTagValue(this.xmlroot+"config/rmax")            , Types.DOUBLE);            
      this.put("config/time_warning"    , xml.getNestedTagValue(this.xmlroot+"config/time_warning")    , Types.DOUBLE);    
      this.put("config/dec"             , xml.getNestedTagValue(this.xmlroot+"config/dec")             , Types.BOOLEAN);             
      this.put("config/hl"              , xml.getNestedTagValue(this.xmlroot+"config/hl")              , Types.DOUBLE);              
      this.put("config/min_ds_rate"     , xml.getNestedTagValue(this.xmlroot+"config/min_ds_rate")     , Types.DOUBLE);     
      this.put("config/XMLfile"         , xml.getNestedTagValue(this.xmlroot+"config/XMLfile")         , Types.STRING);          

      this.put("user/room"              , xml.getNestedTagValue(this.xmlroot+"user/room")              , Types.STRING);                
      this.put("user/ip"                , xml.getNestedTagValue(this.xmlroot+"user/ip")                , Types.STRING);                  
      this.put("user/line"              , xml.getNestedTagValue(this.xmlroot+"user/line")              , Types.INTEGER);               
      this.put("user/port"              , xml.getNestedTagValue(this.xmlroot+"user/port")              , Types.INTEGER);               
   
      this.put("user/name"              , xml.getNestedTagValue(this.xmlroot+"user/name")              , Types.STRING);                
      this.put("user/id"                , xml.getNestedTagValue(this.xmlroot+"user/id")                , Types.STRING);                  
      this.put("user/injnum"            , xml.getNestedTagValue(this.xmlroot+"user/injnum")            , Types.INTEGER);             
      this.put("user/ds"                , xml.getNestedTagValue(this.xmlroot+"user/ds")                , Types.DOUBLE);                 
      this.put("user/t"                 , xml.getNestedTagValue(this.xmlroot+"user/t")                 , Types.DOUBLE);                  
      this.put("user/desired"           , xml.getNestedTagValue(this.xmlroot+"user/desired")           , Types.DOUBLE);            
      this.put("user/dose0"             , xml.getNestedTagValue(this.xmlroot+"user/dose0")             , Types.DOUBLE);              
      this.put("user/time0"             , xml.getNestedTagValue(this.xmlroot+"user/time0")             , Types.STRING);              
      this.put("user/vtot"              , xml.getNestedTagValue(this.xmlroot+"user/vtot")              , Types.DOUBLE);               
      this.put("user/max_vol_mass"      , xml.getNestedTagValue(this.xmlroot+"user/max_vol_mass")      , Types.DOUBLE);       

      this.put("user/injtime"           , xml.getNestedTagValue(this.xmlroot+"user/injtime")           , Types.STRING);            
      this.put("user/res_dose"          , xml.getNestedTagValue(this.xmlroot+"user/res_dose")          , Types.DOUBLE);           
      this.put("user/res_time"          , xml.getNestedTagValue(this.xmlroot+"user/res_time")          , Types.STRING);           
      this.put("user/res_vol"           , xml.getNestedTagValue(this.xmlroot+"user/res_vol")           , Types.DOUBLE);            

      this.put("calc/frac"              , xml.getNestedTagValue(this.xmlroot+"calc/frac")              , Types.DOUBLE);               
      this.put("calc/vol"               , xml.getNestedTagValue(this.xmlroot+"calc/vol")               , Types.DOUBLE);                
      this.put("calc/dose_predicted"    , xml.getNestedTagValue(this.xmlroot+"calc/dose_predicted")    , Types.DOUBLE);     
      this.put("calc/res_bolus"         , xml.getNestedTagValue(this.xmlroot+"calc/res_bolus")         , Types.DOUBLE);          
      this.put("calc/res_decay"         , xml.getNestedTagValue(this.xmlroot+"calc/res_decay")         , Types.DOUBLE);          
      this.put("calc/dose_diff"         , xml.getNestedTagValue(this.xmlroot+"calc/dose_diff")         , Types.DOUBLE);          

      this.put("infuse/ds/time"         , xml.getNestedTagValue(this.xmlroot+"infuse/ds/time")         , Types.DOUBLE);          
      this.put("infuse/ds/rate"         , xml.getNestedTagValue(this.xmlroot+"infuse/ds/rate")         , Types.DOUBLE);          
      this.put("infuse/ds/vol"          , xml.getNestedTagValue(this.xmlroot+"infuse/ds/vol")          , Types.DOUBLE);           
      this.put("infuse/ds/dur"          , xml.getNestedTagValue(this.xmlroot+"infuse/ds/dur")          , Types.DOUBLE);           
      this.put("infuse/bolus/time"      , xml.getNestedTagValue(this.xmlroot+"infuse/bolus/time")      , Types.DOUBLE);       
      this.put("infuse/bolus/rate"      , xml.getNestedTagValue(this.xmlroot+"infuse/bolus/rate")      , Types.DOUBLE);       
      this.put("infuse/bolus/vol"       , xml.getNestedTagValue(this.xmlroot+"infuse/bolus/vol")       , Types.DOUBLE);        
      this.put("infuse/bolus/dur"       , xml.getNestedTagValue(this.xmlroot+"infuse/bolus/dur")       , Types.DOUBLE);        
      this.put("infuse/ss/time"         , xml.getNestedTagValue(this.xmlroot+"infuse/ss/time")         , Types.DOUBLE);          
      this.put("infuse/ss/rate"         , xml.getNestedTagValue(this.xmlroot+"infuse/ss/rate")         , Types.DOUBLE);          
      this.put("infuse/ss/vol"          , xml.getNestedTagValue(this.xmlroot+"infuse/ss/vol")          , Types.DOUBLE);           
      this.put("infuse/ss/dur"          , xml.getNestedTagValue(this.xmlroot+"infuse/ss/dur")          , Types.DOUBLE);           

      // set default values
      if ( this.get("config/XMLfile")      == null ) { this.put("config/XMLfile",file); }
      if ( this.get("config/min_ds_rate")  == null ) { this.put("config/min_ds_rate",(Double)15.0); }
      if ( this.get("user/ds")             == null ) { this.put("user/ds",this.get("config/dsdef")); }
      if ( this.get("config/prompt_ds")    == null ) { this.put("config/prompt_ds",true); }
      if ( this.get("config/mass_check")   == null ) { this.put("config/mass_check",false); }      
      if ( this.get("user/time0")          == null ) { this.put("user/time0","00:00:00"); }
      if ( this.get("user/injtime")        == null ) { this.put("user/injtime","00:00:00"); }
      if ( this.get("user/res_time")       == null ) { this.put("user/res_time","00:00:00"); }
      if ( this.get("config/tbolus")       == null ) { this.put("config/tbolus",0.0); }
      if ( this.get("config/kbol")         == null ) { this.put("config/kbol",0.0); }
      if ( this.get("user/max_vol_mass")   == null ) { this.put("user/max_vol_mass",0.0); }

      // variables from vax bld savefiles
      // used only on initial validation of java calculations
      this.put("user/systime"              , xml.getNestedTagValue(this.xmlroot+"user/systime")              , Types.STRING);
      this.put("vax_profile/time1"         , xml.getNestedTagValue(this.xmlroot+"vax_profile/time1")         , Types.DOUBLE);          
      this.put("vax_profile/time2"         , xml.getNestedTagValue(this.xmlroot+"vax_profile/time2")         , Types.DOUBLE);          
      this.put("vax_profile/time3"         , xml.getNestedTagValue(this.xmlroot+"vax_profile/time3")         , Types.DOUBLE);          
      this.put("vax_profile/rate1"         , xml.getNestedTagValue(this.xmlroot+"vax_profile/rate1")         , Types.DOUBLE);          
      this.put("vax_profile/rate2"         , xml.getNestedTagValue(this.xmlroot+"vax_profile/rate2")         , Types.DOUBLE);          
      this.put("vax_profile/rate3"         , xml.getNestedTagValue(this.xmlroot+"vax_profile/rate3")         , Types.DOUBLE);          
      this.put("vax_calc/dose_predicted"   , xml.getNestedTagValue(this.xmlroot+"vax_calc/dose_predicted")   , Types.DOUBLE);          
      


//       String time = "";
//       int length = 0;
//       String blankTime = "00:00:00";
//       time = (String)this.get("user/time0");
//       length = time.length();
//       if ( time.length() < 8 ) { this.put("user/time0",time + blankTime.substring(length+1)); }
//       time = (String)this.get("user/injtime");
//       length = time.length();
//       if ( time.length() < 8 ) { this.put("user/injtime",time + blankTime.substring(length+1)); }
//       time = (String)this.get("user/res_time");
//       length = time.length();
//       if ( time.length() < 8 ) { this.put("user/res_time",time + blankTime.substring(length+1)); }


      if ( debug ) { 
         Set<String> keys = this.parms.keySet();
         for ( String key : keys ) {
            System.out.println(key+"-->"+this.get(key));
         }
      }      

      // notes is special
      // it is the one tag that is expected to come as an array
      xml.setMultipleTags(true);
      while ( (tmp = xml.getNestedTagValue(this.xmlroot+"config/notes")) != null ) {
         this.notes.add(tmp);
      }
      xml.setMultipleTags(false);
      
//       // check required fields
//       Vector<String> bad = new Vector<String>();
//       if ( this.diam   == null ) { bad.add("diam   - diameter (mm)"); }
//       if ( this.hl     == null ) { bad.add("hl     - half-life (min)"); }
//       if ( this.t      == null ) { bad.add("t      - infusion duration (min)"); }
//       if ( this.kbol   == null ) { bad.add("kbol   - ??? (min)"); }
//       if ( this.tbolus == null ) { bad.add("tbolus - bolus duration (min)"); }
//       if ( ! bad.isEmpty() ) {
//          String msg;
//          msg = "Cannot read required fields from the XML file\n";
//          msg += "The tags may be missing or misspelled.  Or, the\n";
//          msg += "values may not be formatted properly. e.g. letters\n";
//          msg += "in a number field\n";
//          msg += "\n";
//          msg += this.XMLfile + "\n";
//          for ( String field : bad ) {
//             msg += field + "\n";
//          }
//          throw new PumpException(msg);
//       }

      
   }


      
             
   public void writeXMLout (String file) throws PumpException {
      
      DomParserBean xml = null;
      try {
         xml = new DomParserBean(this.emptyXML);
      } catch ( Exception e ) {
         this.post.error(e.getMessage());
      }         
      xml.setInsertTagsOnPut(true);
      
      Set<String> keys = this.parms.keySet();
      for ( String key : keys ) {       
         xml.putNestedTagValue(this.xmlroot+key,this.getString(key));
      }
      
      xml.writeToFile(file);
   }
      
      

//    find the fraction of the current dose that would get into the patient
//    allowing for decay over the duration of the injection
//    there may be some confusion in the use of ln(2) and log(2).  The original
//    vax program used log(2) for the natural log but...
//    on the vax, log(2) = 0.693147
//    in excel,   log(2) = 0.30103 <-- log base 10 not natural logrithm
//    in excel,   ln(2)  = 0.693147
//    in perl,    log(2) = 0.693147
//    in java,    log(2) = 0.693147
   // Read the parms out to local variables to make the formulas more readable.
   // Define local variables as primitive types as much as possible.  
   // Primitives seem to work better with other primitive types and they probably
   // have less overhead but they dont accept null values and objects do.
   // Is that a big deal?  Java will throw a run-time NullPointerException if
   // a primitive is assinged a value (null) of a tag that doesn't exist.  
   // Objects will take the null but java will crash a little bit later when 
   // the null value is used in formula.  Objects do allow for the possibility
   // of testing for null values but that's tedious and the end result is the same.
   public void calc_fraction() throws PumpException {
   
      double lamda;  // internal
      double hl     = (Double)this.get("config/hl");
      double t      = (Double)this.get("user/t");
      double kbol   = (Double)this.get("config/kbol");
      double tbolus = (Double)this.get("config/tbolus");
      Double frac;  // output
            
      lamda = Math.log(2)/hl;
      if (kbol <= 0) {
         // no bolus
         // frac=(1-#(-lamda*t))/(lamda*t)
         frac = ( 1 - Math.exp(-1*lamda*t) ) / (lamda*t);
      } else {
         // yes bolus
         // frac=( kbol*(1-#(-lamda*tbolus))/(lamda*tbolus) + (#(-lamda*tbolus)-#(-lamda*t))/lamda ) / (kbol+t-tbolus)
         frac = ( kbol * (1 - Math.exp(-1*lamda*tbolus))/(lamda*tbolus) + (Math.exp(-1*lamda*tbolus)-Math.exp(-1*lamda*t))/lamda)/(kbol + t - tbolus);
      }
      
      this.put("calc/frac",frac);   
   }



   // a lot of other methods use this value
   // thats a bit of a problem in that its time dependant
   // if everybody figured this on their own they'd get slightly different answers   
   // So create one central method that gets called first.  
   // Then everybody gets the static results passed in as an argument.
   // Decided not to write this to the parms HashMap for the same reason.
   // Its a time dependant value.  How do you know if its gone stale sitting up on the HashMap.ale
   public double calc_dose (Timestamp now) {
      double    dose0 =    (Double)this.get("user/dose0");
      Timestamp time0 = (Timestamp)this.get("user/time0",Types.TIME);
      double    hl    =    (Double)this.get("config/hl");
      double dose_now;
      
      dose_now = Decay.activity(dose0,time0,now,hl,Decay.HalfLifeUnits.Minutes); 
      
      return dose_now;
   }
   
   

   public void calc_volume (double dose_now) throws PumpException {
      // show warning messages by default
      calc_volume(dose_now,true);
   }

   // Output variables have to be Objects to be written back out to parms HashMap
   // Read the parms out to local variables to make the formulas more readable.
   // Define local variables as primitive types as much as possible.  
   // Primitives seem to work better with other primitive types and they probably
   // have less overhead but they dont accept null values and objects do.
   // Is that a big deal?  Java will throw a run-time NullPointerException if
   // a primitive is assinged a value (null) of a tag that doesn't exist.  
   // Objects will take the null but java will crash a little bit later when 
   // the null value is used in formula.  Objects do allow for the possibility
   // of testing for null values but that's tedious and the end result is the same.
   public void calc_volume (double dose_now,boolean showWarnings) throws PumpException {
      double need,max_vol_ds;   
      double    desired      =     (Double)this.get("user/desired");
      double    frac         =     (Double)this.get("calc/frac");
      double    vtot         =     (Double)this.get("user/vtot");
      double    ds           =     (Double)this.get("user/ds");
      boolean   mass_check   =    (Boolean)this.get("config/mass_check");
      double    max_vol_mass =     (Double)this.get("user/max_vol_mass");
      double    dose0        =     (Double)this.get("user/dose0");
      Timestamp time0        =  (Timestamp)this.get("user/time0",Types.TIME); 
      double    hl           =     (Double)this.get("config/hl");
      Double vol,dose_predicted,max_vol;      

         
      // how much current activity do we have to inject to get the desired dose over time
      need = desired / frac;
   
      // how much volume do we have to inject to get the desired dose over time
      max_vol = vtot;
      vol = vtot*need/dose_now;
      
      if ( desired == -1 ) {
         // user has asked to inject everything
         // 99% is a saftey factor
         vol = 0.99 * (vtot - ds);  
         max_vol = vol;
      }
   
      // check volume limit
      max_vol_ds = vtot - ds;
      if ( max_vol_ds < vol ) {
         vol = max_vol_ds;
         max_vol = vol;
         if ( showWarnings ) {
            this.post.warning("Injection limited by volume to "+max_vol_ds+" ml");
         }
      }
   
      // check mass limit -- if requested
      if ( mass_check ) { 
         if ( max_vol_mass < vol ) {
            vol = max_vol_mass;
            max_vol = vol;
            if ( showWarnings ) {
               this.post.warning("Injection limited by mass to "+max_vol_mass+" ml");
            }
         }
      }
   
//       // how much activity would the patient receive over time if we injected now
//       dose_predicted = frac * dose_now * vol / vtot;
//       
//       this.put("dose_predicted",dose_predicted);
      this.put("calc/max_vol",max_vol);
      this.put("calc/vol",vol);
   }

// find default values for all variables


   
   public void calc_profile(double dose_now) {
      // display warning messages by default
      calc_profile(dose_now,true);
   }
   
   // Output variables have to be Objects to be written back out to parms HashMap
   // Read the parms out to local variables to make the formulas more readable.
   // Define local variables as primitive types as much as possible.  
   // Primitives seem to work better with other primitive types and they probably
   // have less overhead but they dont accept null values and objects do.
   // Is that a big deal?  Java will throw a run-time NullPointerException if
   // a primitive is assinged a value (null) of a tag that doesn't exist.  
   // Objects will take the null but java will crash a little bit later when 
   // the null value is used in formula.  Objects do allow for the possibility
   // of testing for null values but that's tedious and the end result is the same.
   public void calc_profile (double dose_now,boolean showWarnings) {
      double t           = (Double)this.get("user/t");
      double vol         = (Double)this.get("calc/vol");
      double kbol        = (Double)this.get("config/kbol");
      double tbolus      = (Double)this.get("config/tbolus");
      double rmax        = (Double)this.get("config/rmax");
      double frac        = (Double)this.get("calc/frac");
      double vtot        = (Double)this.get("user/vtot");
      double ds          = (Double)this.get("user/ds");
      double min_ds_rate = (Double)this.get("config/min_ds_rate");
      Double bol_time,bol_rate,bol_vol,bol_dur;
      Double ds_time,ds_rate,ds_vol,ds_dur;
      Double ss_time,ss_rate,ss_vol,ss_dur;
      
      // steady state
      ss_time = t;
      ss_rate = vol/(kbol + t - tbolus);
      if ( ss_rate > rmax ) {
         ss_rate = rmax;
         vol = rmax * t;
         double inj_dose = vol * frac * dose_now / vtot;
         String msg;
         if ( showWarnings ) {
            msg = "Injection is limited by maximum infusion rate\n";
            msg += "Patient will only get "+inj_dose+" mCi during the steady state phase";
            this.post.warning(msg);
         }
      }
      this.put("infuse/ss/time",ss_time);
      this.put("infuse/ss/rate",ss_rate);   
      this.put("infuse/ss/vol",vol);
      this.put("infuse/ss/dur",ss_time);
      
      // bolus
      bol_rate = (Double)0.0;
      bol_time = (Double)0.0;      
      if ( kbol != 0 ) {
         bol_time = tbolus;
         bol_rate = kbol * ss_rate / tbolus;
         if ( bol_rate > rmax ) { 
            ss_time = kbol * bol_rate / rmax;
            bol_rate = rmax;
            if ( showWarnings ) {
               String msg;
               msg = "Bolus injection is limited by maximum infusion rate \n";
               msg += "Extending bolus time";
               this.post.warning(msg);
            }
         }
      }
      this.put("infuse/bolus/time",bol_time);
      this.put("infuse/bolus/rate", bol_rate);
      this.put("infuse/bolus/vol", bol_rate * bol_time);
      this.put("infuse/bolus/dur", bol_time);
      this.put("infuse/ss/dur",ss_time - bol_time);
      this.put("infuse/ss/vol",ss_rate * ( ss_time - bol_time));
      
      // dead space
      ds_rate = (Double)0.0;
      ds_time = (Double)0.0;
      if ( ds != 0 ) {
         ds_rate = Math.min(15.0,rmax);
         ds_rate = Math.max(ds_rate,Math.max(ss_rate,bol_rate));
         ds_time = ds / ds_rate;
      }
      this.put("infuse/ds/time",(Double)0.0);
      this.put("infuse/ds/rate",ds_rate);
      this.put("infuse/ds/vol",ds);
      this.put("infuse/ds/dur",ds_time);
      this.put("infuse/ds/time",ds_time);
      this.put("infuse/bolus/time",bol_time + ds_time);
      this.put("infuse/ss/time",ss_time + ds_time);
   }
   
   
   
   // how much time before activity in syringe decays down so far
   // patient cant get full desired dose even if the whole syringe was infused
   // time interval has same units as hl which is assumed to be minutes.
   public Timestamp calc_injectBy (Timestamp time_now,double dose_now) {
      double max_vol = (Double)this.get("calc/max_vol");
      double desired = (Double)this.get("user/desired");
      double vtot    = (Double)this.get("user/vtot");
      double frac    = (Double)this.get("calc/frac");
      double hl      = (Double)this.get("config/hl");
      double dose_min,time_min;
      Timestamp time_injectBy;

      dose_min = max_vol*desired/(vtot*frac);
      time_min = Decay.deltaTime(dose_min,dose_now,hl);      
      time_injectBy = new Timestamp(time_now.getTime() + (long)(time_min*60*1000));
      
      return time_injectBy;
   }
  
  
   // Output variables have to be Objects to be written back out to parms HashMap
   // Read the parms out to local variables to make the formulas more readable.
   // Define local variables as primitive types as much as possible.  
   // Primitives seem to work better with other primitive types and they probably
   // have less overhead but they dont accept null values and objects do.
   // Is that a big deal?  Java will throw a run-time NullPointerException if
   // a primitive is assinged a value (null) of a tag that doesn't exist.  
   // Objects will take the null but java will crash a little bit later when 
   // the null value is used in formula.  Objects do allow for the possibility
   // of testing for null values but that's tedious and the end result is the same.
   public void calc_predictedDose () {
      double    dose_inj;
      double    frac    =    (Double)this.get("calc/frac");
      double    vol     =    (Double)this.get("calc/vol");
      double    vtot    =    (Double)this.get("user/vtot");
      double    dose0   =    (Double)this.get("user/dose0");
      Timestamp time0   = (Timestamp)this.get("user/time0",Types.TIME);
      Timestamp injtime = (Timestamp)this.get("user/injtime",Types.TIME);
      double    hl      =    (Double)this.get("config/hl");
      Double dose_predicted;
   
      dose_inj = Decay.activity(dose0,time0,injtime,hl,Decay.HalfLifeUnits.Minutes); 
      dose_predicted = frac * dose_inj * vol / vtot;
      
      this.put("calc/dose_predicted",dose_predicted);   
   }
  
   
   public void calc_actualDose () {       
      double preinj_dose,postinj_dose;
      double    dose0    =    (Double)this.get("user/dose0");
      Timestamp time0    = (Timestamp)this.get("user/time0",Types.TIME);
      Timestamp injtime  = (Timestamp)this.get("user/injtime",Types.TIME);
      double    hl       =    (Double)this.get("config/hl");
      Double    frac     =    (Double)this.get("calc/frac");
      double    res_dose =    (Double)this.get("user/res_dose");
      Timestamp res_time = (Timestamp)this.get("user/res_time",Types.TIME);
      double    desired  =    (Double)this.get("user/desired");
      Double res_bolus,res_decay,dose_diff;                
      if ( frac == null ) { frac = 0.0; }
            
      // decay correct pre and post assays to injection time
      preinj_dose  = Decay.activity(dose0,time0,injtime,hl,Decay.HalfLifeUnits.Minutes); 
      postinj_dose = Decay.activity(res_dose,res_time,injtime,hl,Decay.HalfLifeUnits.Minutes); 
      
      // dose patient received corrected to injection time
      res_bolus = preinj_dose - postinj_dose;
      
      // dose patient received accounting for decay over the infusion
      res_decay = res_bolus;      
      if ( frac != 0 ) {
         res_decay = res_bolus * frac;
      }
      
      this.put("calc/res_bolus",res_bolus);
      this.put("calc/res_decay",res_decay);
      this.put("calc/dose_diff",(Double)((res_decay-desired)/desired)*100.0);
   }

//    // time is elapsed time since start of infusion in minutes
//    public double plannedVol (double time) {
//       
//       double dsTime = (Double)this.get("infuse/ds/time");
//       double bolTime = (Double)this.get("infuse/bolus/time");
//       double ssTime = (Double)this.get("infuse/ss/time");
//       
//       double vol = 0;
//       if ( time < 0 ) {
//          return vol;
//       }
//       if ( time < dsTime ) {
//          vol += (Double)this.get("infuse/ds/rate") * time;
//          return vol;
//       }
//       vol += (Double)this.get("infuse/ds/vol");
//       if ( time < bolTime ) {
//          vol += (Double)this.get("infuse/bolus/rate") * (time-dsTime);
//          return vol;
//       }
//       vol += (Double)this.get("infuse/bolus/vol");
//       if ( time < ssTime ) {
//          vol += (Double)this.get("infuse/ss/rate") * (time-bolTime);
//          return vol;
//       }
//       vol += (Double)this.get("infuse/ss/vol");
//       return vol;
//    }
//    
//       
//    // time is elapsed time since start of infusion in minutes
//    public double plannedRate (double time) {
//    
//       double dsTime = (Double)this.get("infuse/ds/time");
//       double bolTime = (Double)this.get("infuse/bolus/time");
//       double ssTime = (Double)this.get("infuse/ss/time");
//       
//       if ( time < 0 ) {
//          return 0;
//       }
//       if ( time < dsTime ) {
//          return (Double)this.get("infuse/ds/rate");
//       }
//       if ( time < bolTime ) {
//          return (Double)this.get("infuse/bolus/rate");
//       }
//       if ( time < ssTime ) {
//          return (Double)this.get("infuse/ss/rate");
//       }
//       return 0;
//    }
// 
      
   public String report () {
      String report = "";
      Timestamp now = new Timestamp(System.currentTimeMillis());
      double    dose0     =    (Double)this.get("user/dose0");
      Timestamp time0     = (Timestamp)this.get("user/time0",Types.TIME);
      double    vtot      =    (Double)this.get("user/vtot");
      double    hl        =    (Double)this.get("config/hl");
      Timestamp injtime   = (Timestamp)this.get("user/injtime",Types.TIME);
      Timestamp res_time  = (Timestamp)this.get("user/res_time",Types.TIME);
      double    res_dose  =    (Double)this.get("user/res_dose");
      Double    res_vol   =    (Double)this.get("user/res_vol");
      double    desired   =    (Double)this.get("user/desired");
      Double    predicted =    (Double)this.get("calc/dose_predicted");
      double    res_bolus =    (Double)this.get("calc/res_bolus");
      double    res_decay =    (Double)this.get("calc/res_decay");
      double    dose_diff =    (Double)this.get("calc/dose_diff");
      double    ds        =    (Double)this.get("user/ds");
      if ( predicted == null ) { predicted = 0.0; }
      if ( res_vol == null ) { res_vol = 0.0; }
      
      report += "Name: " + this.getString("user/name") + "\n";
      report += "Dose: " + this.getString("user/injnum") + "\n";
      report += "Date: " + now.toString() + "\n";
      report += "\n";
      report += "\n";
      report += "\n";
      report += String.format("%19s %14s %14s \n","assay time","assayed dose","total volume");
      report += String.format("%19s %14s %14s \n","","(mCi)","(cc)");
      report += String.format("%19s %14.2f %14.2f \n",time0.toString().substring(0,19),dose0,vtot);
      report += "\n";
      report += "\n";
      report += "\n";
      report += String.format("%19s  \n","injection time");
      report += String.format("%19s  \n","");
      report += String.format("%19s  \n",injtime.toString().substring(0,19));
      report += "\n";
      report += "\n";
      report += "\n";
      report += String.format("%19s %14s %14s %14s \n","reassay time","residual act","vol injected","residual vol");
      report += String.format("%19s %14s %14s %14s \n","","(mCi)","(cc)","(cc)");
      report += String.format("%19s %14.2f %14.2f %14.2f \n",res_time.toString().substring(0,19),res_dose,vtot-res_vol,res_vol);
      report += "\n";
      report += "\n";
      report += "\n";
      report += String.format("%19s %14s %14s %14s %14s  \n","desired","predicted","actual dose","actual dose","% diff");
      report += String.format("%19s %14s %14s %14s %14s  \n","dose","dose","at injection","over infusion","");
      report += String.format("%19s %14s %14s %14s %14s  \n","(mCi)","(mCi)","(mCi)","(mCi)","");
      report += String.format("%19.5f %14.5f %14.5f %14.5f %14.2f  \n",desired,predicted,res_bolus,res_decay,dose_diff);
      report += "\n";
      report += "\n";
      report += "\n";
      report += String.format("%19s %14s  \n","dead space","half life");
      report += String.format("%19s %14s  \n","(cc)","(min)");
      report += String.format("%19.2f %14.2f  \n",ds,hl);
      report += "\n";
      report += "\n";
      report += "\n";
      report += String.format("%19s %14s %14s %14s %14s  \n","","times","rates","duration","volume");
      report += String.format("%19s %14s %14s %14s %14s\n\n","","(min)","(cc/min)","(min)","(cc)");
      if ( (Double)this.get("infuse/ds/vol") != null ) {
         report += String.format("%19s %14.5f %14.5f %14.5f %14.5f  \n  \n","dead space",(Double)this.get("infuse/ds/time"),(Double)this.get("infuse/ds/rate"),(Double)this.get("infuse/ds/dur"),(Double)this.get("infuse/ds/vol"));
      }
      if ( (Double)this.get("infuse/bolus/vol") != null ) {
         report += String.format("%19s %14.5f %14.5f %14.5f %14.5f  \n  \n","bolus",(Double)this.get("infuse/bolus/time"),(Double)this.get("infuse/bolus/rate"),(Double)this.get("infuse/bolus/dur"),(Double)this.get("infuse/bolus/vol"));
      }
      if ( (Double)this.get("infuse/ss/vol") != null ) {
         report += String.format("%19s %14.5f %14.5f %14.5f %14.5f  \n  \n","infusion",(Double)this.get("infuse/ss/time"),(Double)this.get("infuse/ss/rate"),(Double)this.get("infuse/ss/dur"),(Double)this.get("infuse/ss/vol"));
      }
      
      return report;
   
   }
}
   
   
//enumerate time units in Decay.activity(), it might save some exception throws
//error/warning handlers.  Stay with exceptions.  Switch to callbacks.  Go to some hybrid?

//calc_predictedDose should take injtime as an arguement.  That way it can work with the actual injtime or a hypothetical injtime    
//how efficient is this HashMap
//should dose_now be saved to HashMap
