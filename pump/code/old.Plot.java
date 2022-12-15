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

public class Plot extends JComponent {
   List<Point> baseline = new ArrayList<Point>();
   int maxX=0,maxY=0;
   List<Point> data = new ArrayList<Point>();
   Point box = new Point(0,0);
   int resX = 600;
   int resY = 100;
   int ticksX=1;
   int ticksY=1;
   String labelX = "";
   String labelY = "";


   Plot () {
   }


   
   // graphic coordinates given in integers
   // multiply times by 600 so graphs show tenths of a second
   // multiply volumes by 100 so graphs show hundreths of a ml
   private Point convert(Point2D.Double point) {
      int x = (int)(point.getX()*resX);
      int y = (int)(point.getY()*resY);
      return new Point(x,y);
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
   
   public void setMax(Point2D.Double point) {
      Point maxPoint = convert(point);
      this.maxX = (int)(maxPoint.getX()*1.1);
      this.maxY = (int)(maxPoint.getY()*1.1);      
      this.box = new Point((int)this.maxX/50,(int)this.maxY/50);
   } 
         
   
   public void setTicks (double x, double y) {
      this.ticksX = (int)(x*resX);
      this.ticksY = (int)(y*resY);
   }
   
   
   public void setLabels (String x, String y) {
      this.labelX = x;
      this.labelY = y;
   }
       
    
   public void addBaseline(List<Point2D.Double> baseline) {
      for (Point2D.Double point : baseline) {
         this.baseline.add(convert(point));
      }   
   }
   
   public synchronized void addData(Point2D.Double point) {
      this.data.add(convert(point));
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
      double scaleX =  1.0 * (w-offsetX) / this.maxX;
      double scaleY = -1.0 * (h-offsetY) / this.maxY;      
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
      g2.drawLine(0,0,this.maxX,0);
      g2.drawLine(0,0,0,this.maxY);
      for (int i=0;i<this.maxX;i+=this.ticksX) {
         g2.drawLine(i,0,i,(int)(0.5*textHeightX));
         g2.drawString(String.valueOf(i/this.resX),i,(int)(1.5*textHeightX));
      }
      g2.drawString(this.labelX,(int)((this.maxX/2)-(textWidthX/2)),(int)(2.5*textHeightX));
      // rotate font to write along y axis
      // font pivots on lower-left corner of letter so line locations are a little different
      t.rotate(-1*Math.PI/2);
      g2.setFont(g2.getFont().deriveFont(t));
      for (int i=0;i<this.maxY;i+=this.ticksY) {
         g2.drawLine(0,i,(int)(0.5*textHeightY),i);
         g2.drawString(String.valueOf(i/this.resY),(int)(1.0*textHeightY),i);
      }
      g2.drawString(this.labelY,(int)(2.0*textHeightY),(int)((this.maxY/2)+(textWidthY/2)));
      
      
      // draw points
      g2.setPaint(Color.GREEN); 
      g2.setStroke(new BasicStroke(10)); 
      for (Point data : this.data) {
         double boxX = this.box.getX()/2;
         double boxY = this.box.getY()/2;
         double dataX = data.getX();
         double dataY = data.getY();
         
         int left = (int)(dataX-boxX);
         int right = (int)(dataX+boxX);
         int top = (int)(dataY-boxY);
         int bottom = (int)(dataY+boxY);
       
         g2.drawLine(left,top,right,bottom);
         g2.drawLine(right,top,left,bottom);
      }
      
      
      // draw baseline   
      g2.setPaint(Color.BLACK);  
      g2.setStroke(new BasicStroke(1)); 
      for (int i=1;i<this.baseline.size();i++) {
         int x1 = (int)this.baseline.get(i-1).getX();
         int y1 = (int)this.baseline.get(i-1).getY();
         int x2 = (int)this.baseline.get(i).getX();
         int y2 = (int)this.baseline.get(i).getY();
      
         g2.drawLine(x1,y1,x2,y2);
      }
      
      
   }   
   
}
