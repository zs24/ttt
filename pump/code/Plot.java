import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.List;
import java.util.ArrayList;

import java.awt.geom.AffineTransform;  // font magic


// 2006-04-18 cf Allowed users to specify colors for each individual point as
//               they add them. 
public class Plot extends JComponent {
   class PlotPoint extends Point2D.Double {
      Color color;
   }
   List<Point2D.Double> baseline = new ArrayList<Point2D.Double>();
   double maxX=0,maxY=0;
   List<PlotPoint> data = new ArrayList<PlotPoint>();
   Point2D.Double box = new Point2D.Double(0,0);
   int resX = 600;
   int resY = 100;
   double ticksX=1;
   double ticksY=1;
   String labelX = "";
   String labelY = "";


   Plot () {
   }


   // java graphic coordinates are specified in integers
   // user will probably give coordinates as float or double
   // java will chop decimals to display points on chart.
   // allow user to apply a scaling factor so the chop point occurs where he wants
   // for example, user provides data in terms of fractions of a minute.  Normally,
   // plot will collapse everything to whole minutes.  With a scale factor
   // say 600, plot will collapse everything to tenths of a second.  Much better.
   public void setResolution(int x,int y) {
      this.resX = x;
      this.resY = y;
   }
   

   // figure out how to space tick marks along the axes.
   // try each of a given set of spacings against the current max value
   // use first one that will give 10 or fewer ticks 
   private void findTicks () {
      double [] steps = {0.1,0.25,0.5,1,2,5,10,20,50,100,200,500,1000};
      
      this.ticksX = steps[steps.length-1];
      for (int i=0;i<steps.length;i++) {
         if ( this.maxX / steps[i] < 10 ) { 
            this.ticksX = steps[i];
            break;
         }
      }
      this.ticksY = steps[steps.length-1];
      for (int i=0;i<steps.length;i++) {
         if ( this.maxY / steps[i] < 10 ) { 
            this.ticksY = steps[i];
            break; 
         }
      }
   }

   
   private void findMax() {
      double maxX=0,maxY=0;
      
      for (Point2D.Double base : this.baseline) {
         maxX = Math.max(maxX,base.getX());
         maxY = Math.max(maxY,base.getY());
      }
      for (Point2D.Double data : this.data) {
         maxX = Math.max(maxX,data.getX());
         maxY = Math.max(maxY,data.getY());
      }  
      this.setMax(new Point2D.Double(maxX,maxY));
   }

   
   public void setMax(Point2D.Double point) {
      this.maxX = point.getX()*1.1;
      this.maxY = point.getY()*1.1;      
      this.box = new Point2D.Double(this.maxX*this.resX/50,this.maxY*this.resY/50);
      this.findTicks();
   } 
         
   
   public void setTicks (double x, double y) {
      this.ticksX = x;
      this.ticksY = y;
   }
   
   
   public void setLabels (String x, String y) {
      this.labelX = x;
      this.labelY = y;
   }
       
    
   public void addBaseline(List<Point2D.Double> baseline) {
      this.baseline = baseline;
      this.findMax();
   }
   
   public synchronized void addData(Point2D.Double point,Color color) {
      PlotPoint tmp = new PlotPoint();
      tmp.x = point.x;
      tmp.y = point.y;
      tmp.color = color;
      
      this.data.add(tmp);
      this.findMax();
      repaint();     
   }
   
   public synchronized void paint(Graphics g) {
   
      Graphics2D g2 = (Graphics2D)g;
      
      int textHeightX = g2.getFontMetrics().getHeight();
      int textWidthX = g2.getFontMetrics().stringWidth(this.labelX);
      int textWidthY = g2.getFontMetrics().stringWidth(this.labelY);
      int textHeightY = textHeightX;


      // scale graphics area   
      // by default origin is in the top left corner and positive y moves down    
      // fix it so origin is bottom left and postive y moves up
      int h = getSize().height;
      int w = getSize().width;
      int offsetX = textHeightX*3;
      int offsetY = textHeightY*3;
      double scaleX =  1.0 * (w-offsetX) / (this.maxX * this.resX);
      double scaleY = -1.0 * (h-offsetY) / (this.maxY * this.resY);      
      g2.translate(offsetX,h-offsetY);
      g2.scale(scaleX,scaleY);
      textHeightX /= scaleY;
      textWidthX /= scaleX;
      
      textHeightY /= -1*scaleX;
      textWidthY /= scaleY;
      
      
      // transform current font to counter-act plot scaling
      AffineTransform t = new AffineTransform();
      t.setToScale(1/scaleX,1/scaleY);
      g2.setFont(g2.getFont().deriveFont(t));

      // draw axes
      g2.drawLine(0,0,(int)(this.maxX*this.resX),0);
      g2.drawLine(0,0,0,(int)(this.maxY*this.resY));
      for (double i=0;i<this.maxX;i+=this.ticksX) {
         int x = (int)(i*this.resX);
         String tick = String.format("%5.2f",i);
         if ( this.ticksX >= 1 ) { 
            tick = String.valueOf((int)i);
         }
         g2.drawLine(x,0,x,(int)(0.5*textHeightX));
         g2.drawString(tick,x,(int)(1.5*textHeightX));
      }
      g2.drawString(this.labelX,(int)((this.maxX*this.resX/2)-(textWidthX/2)),(int)(2.5*textHeightX));
      // rotate font to write along y axis
      // font pivots on lower-left corner of letter so line locations are a little different
      t.rotate(-1*Math.PI/2);
      g2.setFont(g2.getFont().deriveFont(t));
      for (double i=0;i<this.maxY;i+=this.ticksY) {
         int y = (int)(i*this.resY);
         String tick = String.format("%5.2f",i);
         if ( this.ticksY >= 1 ) {
            tick = String.valueOf((int)i);
         }
         g2.drawLine(0,y,(int)(0.5*textHeightY),y);
         g2.drawString(tick,(int)(1.0*textHeightY),y);
      }
      g2.drawString(this.labelY,(int)(2.0*textHeightY),(int)((this.maxY*this.resY/2)+(textWidthY/2)));
      
      
      // draw points
      g2.setPaint(Color.GREEN); 
      g2.setStroke(new BasicStroke(10)); 
      Double lastY = null;
      for (PlotPoint data : this.data) {
         double boxX = this.box.getX()/2;
         double boxY = this.box.getY()/2;
         double dataX = data.getX()*this.resX;
         double dataY = data.getY()*this.resY;
         
         int left = (int)(dataX-boxX);
         int right = (int)(dataX+boxX);
         int top = (int)(dataY-boxY);
         int bottom = (int)(dataY+boxY);
         
         g2.setPaint(data.color);
         g2.drawLine(left,top,right,bottom);
         g2.drawLine(right,top,left,bottom);
      }
      
      
      // draw baseline   
      g2.setPaint(Color.BLACK);  
      g2.setStroke(new BasicStroke(1)); 
      for (int i=1;i<this.baseline.size();i++) {
         int x1 = (int)(this.baseline.get(i-1).getX()*this.resX);
         int y1 = (int)(this.baseline.get(i-1).getY()*this.resY);
         int x2 = (int)(this.baseline.get(i).getX()*this.resX);
         int y2 = (int)(this.baseline.get(i).getY()*this.resY);
      
         g2.drawLine(x1,y1,x2,y2);
      }
      
      
   }   
   
}
