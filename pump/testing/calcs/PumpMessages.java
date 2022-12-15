import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JDialog;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.BorderLayout;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.util.Date;

public class PumpMessages implements Messages {
   JFrame frame = null;
   PrintStream out = System.out;

   PumpMessages () {
   }
   
   PumpMessages (JFrame frame) {
      this.frame = frame;
   }
   
   PumpMessages (String filename) throws PumpException {
      try {
         this.out = new PrintStream(filename);
      } catch ( FileNotFoundException e ) {
         String msg = "Couldn't open log file\n";
         msg += filename;         
         error(msg);
      }
   }
   
   PumpMessages (JFrame frame,String filename) throws PumpException {
      this(filename);
      this.frame = frame;
   }
   
   public PrintStream getStream () {
      return out;
   }
   
   public void error (String msg) throws PumpException {
      this.log("Pump Error\n" + msg);
      JOptionPane.showMessageDialog(frame,msg,"Pump Error",JOptionPane.ERROR_MESSAGE);
      throw new PumpException(msg);
   }
      
   public void warning (String msg) {
      this.log("Pump Warning\n"+msg);
      JOptionPane.showMessageDialog(frame,msg,"Pump Warning",JOptionPane.WARNING_MESSAGE);      
   }
   
   public void log (String msg) {
      this.out.printf("~~~~~~~~~~~~%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL\n",new Date());
      this.out.println(msg);
      this.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
   }
   
   
   public void alert (String msg) {
      this.log("Pump Alert\n"+msg);
      
      final AlarmBells bells = new AlarmBells(1,500,500);
      Thread thread = new Thread(bells);
      thread.start();
      
      final JOptionPane optionPane = new JOptionPane(msg,
                JOptionPane.ERROR_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION
               );
               
      String [] options =  {"Silence Alarm","Close Window"};
      optionPane.setOptions(options);

      final JDialog dialog = new JDialog(frame,"Pump Alert");
                             
      dialog.setContentPane(optionPane);
      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      
      optionPane.addPropertyChangeListener(new PropertyChangeListener() {
         public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();

            if (dialog.isVisible() 
               && (e.getSource() == optionPane)
               && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
               
               String value = (String)optionPane.getValue();
               Object [] options = optionPane.getOptions();
               if ( value.equals((String)options[0]) ) {
                  // 1st button -- silence alarm
                  bells.stop();
               }
               if ( value.equals((String)options[1]) ) {
                  // 2nd button -- close window
                  bells.stop();
                  dialog.dispose();
               }
            }
         }
      });
      dialog.pack();
      dialog.setVisible(true);

   }
   
}
