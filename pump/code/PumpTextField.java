import javax.swing.JTextField;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.text.Document;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import java.awt.Color;
import javax.swing.SwingUtilities;
import java.awt.Component;

import java.awt.Font;
import java.awt.Insets;

public class PumpTextField extends RegexTextField implements FocusListener,AncestorListener {
   PumpParameters parms;
   String key;
   PumpParameters.Types type;
   Color normal,disabled;
   String desc;
   Boolean testResults;

   PumpTextField() {
      super();
      this.setup();
   }
   
   PumpTextField(Document doc, String text, int columns) {
      super(doc,text,columns);
      this.setup();
   }
   
   PumpTextField(int columns) {
      super(columns);
      this.setup();
   }
   
   PumpTextField(String text) {
      super(text);
      this.setup();
   }
   
   PumpTextField(String text, int columns) {
      super(text,columns);
      this.setup();
   }
   
   private void setup () {
      this.setMargin(new Insets(0,5,0,5));
      this.setFont(this.getFont().deriveFont(Font.BOLD));
      this.setColors();
      this.addFocusListener(this);
      this.addAncestorListener(this);
   }
      
   
   private void setColors() {
      this.setEditable(true);
      this.normal = this.getBackground();
      this.setEditable(false);
      this.disabled = this.getBackground();
      this.setEditable(true);
   }
   
   public void setDescription (String desc) {
      this.desc = desc;
   }
      
   public void setExternalData (PumpParameters parms,String key,PumpParameters.Types type) {
      this.parms = parms;
      this.key = key;
      this.type = type;
      this.setText(this.parms.getString(this.key));
   }
   
   
   
   // push data from textfield to external data array
   public void upload () {
       this.parms.put(this.key,this.getText(),this.type);
   }
   
   
   // pull data from external data array to textfield
   public void download () { 
      this.setText(this.parms.getString(key));
      this.test(false);   
   }
   
   // placeholder.  user overrides with editable test applied when field becomes visible
   public boolean editableRule () {
      return true;
   }
      
   protected void resetBackground () {
      this.setBackground((this.isEditable()) ? this.normal : this.disabled);
   }
      

   public boolean test (boolean dialogs) {
      
      this.testResults = null;
      if ( this.getText().equals("") ) {
         String msg = this.desc + " required";
         this.setBackground(Color.RED);
         this.setForeground(Color.WHITE);
         this.setCaretColor(Color.WHITE);
         this.setToolTipText(msg);
         if ( dialogs ) {
            Component root = SwingUtilities.getRoot(this);
            JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
         }
         this.testResults = false;
         return false;
      }
      if ( ! this.getText().matches(this.regex) ) {
         String msg = this.desc + " formatted improperly";
         this.setBackground(Color.RED);
         this.setForeground(Color.WHITE);
         this.setCaretColor(Color.WHITE);
         this.setToolTipText(msg);
         if ( dialogs ) {
            Component root = SwingUtilities.getRoot(this);
            JOptionPane.showMessageDialog(root,msg,"Bad Format",JOptionPane.ERROR_MESSAGE);      
         }
         this.testResults = false;
         return false;
      }
         
      this.resetBackground();
      this.setForeground(Color.BLACK);
      this.setCaretColor(Color.BLACK);
      this.setToolTipText("");
      return true;
   }   
   
   
   public String padTime (String time) {
      // assume time is partial or complete time formatted string (hh:mm:ss)
      // pad to the right with zeroes if it is not complete
      String pad = "00:00:00";
      if ( (time == null) || (time == "") ) {
         time = pad;
      } else if (time.length() < 8) {
         time += pad.substring(time.length()+1);      
      }
      return time;   
   }
   
   // ancestor listener
   public void ancestorAdded (AncestorEvent e) {
      this.download();
      this.setEditable(this.editableRule());
   }
   public void ancestorMoved (AncestorEvent e) {}
   public void ancestorRemoved (AncestorEvent e) {} 
   
   // focus listener
   // e.isTemporary() is the workaround for an old java bug that was
   // first reported in 1999 on version 1.1.7 as bugid 4246149.
   // Apparently java creates spurious focuslost events when a JOptionPane
   // is used within a focuslost event.  The extra focuslost events mean 
   // extra passes through the focuslost method which means extra dialog 
   // windows.  The e.isTemporary() kludge essentially ignores the extra
   // focuslost events.  
   public void focusLost (FocusEvent e) {
      if ( e.isTemporary() ) { return; }
      this.upload();
      test(true);
   }
   public void focusGained (FocusEvent e) {}
   

}
