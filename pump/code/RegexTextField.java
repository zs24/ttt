import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

public class RegexTextField extends JTextField {
   String regex;
   
   RegexTextField() {
      super();
   }
   
   RegexTextField(Document doc, String text, int columns) {
      super(doc,text,columns);
   }
   
   RegexTextField(int columns) {
      super(columns);
   }
   
   RegexTextField(String text) {
      super(text);
   }
   
   RegexTextField(String text, int columns) {
      super(text,columns);
   }

   public void setMask (String mask) {
      this.setMask(mask,"");
   }
   
   public void setMask (String mask,String filler) {
      this.regex = mask;
      final String sample = filler;
   
      ((AbstractDocument)(getDocument())).setDocumentFilter(new DocumentFilter() {      

         public void insertString(FilterBypass fb,int offset,String string,AttributeSet attr) throws BadLocationException {
            Document doc = fb.getDocument();
            String oldvalue = doc.getText(0,doc.getLength());
            String newvalue = oldvalue.substring(0,offset) + string + oldvalue.substring(offset);
            String testvalue = newvalue;
            if ( sample.length() > newvalue.length() ) {
               testvalue += sample.substring(newvalue.length());
            }
                  
            if ( testvalue.matches(regex) ) {
               fb.insertString(offset,string,attr);
            }
         }
               
         public void replace(FilterBypass fb,int offset,int length,String string,AttributeSet attr) throws BadLocationException {
            Document doc = fb.getDocument();
            String oldvalue = doc.getText(0,doc.getLength());
            String newvalue = oldvalue.substring(0,offset) + string + oldvalue.substring(offset+length);
            String testvalue = newvalue;
            if ( sample.length() > newvalue.length() ) {
               testvalue += sample.substring(newvalue.length());
            }
             
            if ( testvalue.matches(regex) ) {
               fb.replace(offset,length,string,attr);
            }
         }  

         public void remove(FilterBypass fb,int offset,int length) throws BadLocationException {
            Document doc = fb.getDocument();
            String oldvalue = doc.getText(0,doc.getLength());
            String newvalue = oldvalue.substring(0,offset) + oldvalue.substring(offset+length);
            String testvalue = newvalue;
                if ( sample.length() > newvalue.length() ) {
               testvalue += sample.substring(newvalue.length());
            }
        
            if ( testvalue.matches(regex) ) {
               fb.remove(offset,length);
            }
         }  
      });
   }
}
