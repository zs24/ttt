import javax.swing.*;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;

public class PumpProgressBar extends JPanel  {
//   JPanel panel;
   JProgressBar leftBar;
   JProgressBar rightBar;
   

   PumpProgressBar () {
      this.leftBar = new JProgressBar(0,50);
      this.leftBar.setStringPainted(false);
      this.leftBar.setBorderPainted(true);
      this.leftBar.setForeground(Color.GREEN);
      
      this.rightBar = new JProgressBar(0,50);
      this.rightBar.setStringPainted(false);
      this.rightBar.setBorderPainted(false);
      
      GridBagConstraints bag = new GridBagConstraints();
      bag.fill = GridBagConstraints.BOTH;
      bag.weighty=1;      
      bag.weightx=1;
      
      this.setLayout(new GridBagLayout());
      this.add(this.leftBar,bag);
      this.add(this.rightBar,bag);
      this.setRatio(1,0);

   }
   
   PumpProgressBar(int leftMin,int leftMax) {
      this();
      this.setRange(leftMin,leftMax);
   }
   
   PumpProgressBar(int leftMin,int leftMax,int rightMin,int rightMax) {
      this();
      this.setRange(leftMin,leftMax,rightMin,rightMax);
   }
   
   
   public JProgressBar getLeft () {
      return this.leftBar;
   }
   
   public JProgressBar getRight () {
      return this.rightBar;
   }
   
   public void setValue (int value) {
      
      this.leftBar.setValue(value);
      this.rightBar.setValue(value); 
      
      if ( value < this.leftBar.getMinimum() ) { value = 0; }
      if ( value > this.leftBar.getMaximum() ) { value = this.leftBar.getMaximum(); }
      int display = value - this.leftBar.getMinimum();
      if ( display < 0 ) { display = 0; }
      String text = String.format("%6d secs",display);
      this.leftBar.setString(text);
      
// 
//       double tmp = (double)value / 1000;
//       String text = String.format("%2.4fcc",tmp);
//       this.leftBar.setString(text);
// //      this.rightBar.setString(text);
          
   }
   
   
   
   public void setRange(int leftMin,int leftMax){
      this.setRange(leftMin,leftMax,-9999,-9999);
   }
   
   
   public void setRange (int leftMin,int leftMax,int rightMin,int rightMax) {
      this.leftBar.setMinimum(leftMin);
      this.leftBar.setMaximum(leftMax);
      this.rightBar.setMinimum(rightMin);
      this.rightBar.setMaximum(rightMax);
      this.leftBar.setValue(0);
      this.rightBar.setValue(0);
   }
   
   

   public void setRatio(double leftRatio,double rightRatio) {
         
      GridBagLayout layout = (GridBagLayout)this.getLayout();
      GridBagConstraints bag;
      
      bag = layout.getConstraints(this.leftBar);
      bag.weightx=leftRatio;
      layout.setConstraints(this.leftBar,bag);
      
      bag = layout.getConstraints(this.rightBar);
      bag.weightx=rightRatio;
      layout.setConstraints(this.rightBar,bag);
      
      Dimension size = new Dimension(20,0);
      this.leftBar.setPreferredSize(size);
      size.width=0;  // have to reset it.  last call changed the value
      this.rightBar.setPreferredSize(size);
      
      this.revalidate();
      
   }
   

}
