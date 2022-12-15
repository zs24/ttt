import java.awt.print.*;

//import javax.swing.JTextArea;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.Font;

public class PrintText implements Printable {
   String text;
//   JTextArea page;
   
   PrintText (String text) {
      this.text = text;
//      this.page = new JTextArea(text,80,50);
   }
   
   public int print (Graphics g,PageFormat pf,int pageIndex) throws PrinterException {
      if ( pageIndex > 0 ) { return NO_SUCH_PAGE; }
//      this.page.print(g);
      
//      Graphics2D g2 = (Graphics2D)g;
//      g2.drawString("Save a tree!",96,144);
//      g2.drawString(this.text,10,10);
//       System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
//       System.out.println(this.text);
//       System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
      
//      Font font = g.getFont();
//      Font font = new Font ("CourierNew", Font.PLAIN, 12);
      Font font = new Font ("Monospaced", Font.PLAIN, 12);
      g.setFont(font);
      
      int xo = (int) pf.getImageableX ();
      int yo = (int) pf.getImageableY ();
      int y = font.getSize ();
    
      String [] lines = this.text.split("\n");
      int nlines = lines.length;
      for (int i=0;i<nlines-1;i++) {
         g.drawString(lines[i],xo,yo+y+(y*i));
      }
      
      return PAGE_EXISTS;
   }
}
