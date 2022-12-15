import java.util.Map;
import java.util.HashMap;
import java.sql.Timestamp;
import java.lang.IllegalArgumentException; // time conversions

import java.awt.print.*;

import javax.swing.*;
import java.awt.Container;
import java.awt.Color;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.KeyEvent;
import java.awt.Event;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.*;
import java.awt.Font;

// import javax.swing.plaf.ProgressBarUI;
// import java.awt.Graphics;
// import java.awt.geom.*;
// import java.awt.*;
import java.awt.Dimension;
import java.awt.Component;

import java.awt.print.PrinterJob;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.BorderLayout;

import java.awt.print.Printable;
import java.awt.print.PrinterException;

import java.util.Date;

// pump log file
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

// supports plots
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;

// changing font sizes
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class PumpGUI extends JPanel implements PumpCallback,CountdownCallback {
   PumpMessages post;
   PumpParameters parms;
   PumpCommands cmds;
   boolean goodConnection=false;
   boolean goodParms=false;
   boolean infused=false;
   boolean monitor=true;
   boolean error=false;
   JTextPane connectPane = null;
   JTabbedPane tabs;
   JFrame frame = null;
   JLabel goLabel;
   PumpProgressBar goBar;
   PumpProgressBar dsBar;
   PumpProgressBar bolusBar;
   PumpProgressBar ssBar;
   JLabel dsRateLabel;
   JLabel dsVolLabel;
   JLabel dsDurLabel;
   JLabel bolusRateLabel;
   JLabel bolusVolLabel;
   JLabel bolusDurLabel;
   JLabel ssRateLabel;
   JLabel ssVolLabel;
   JLabel ssDurLabel;
   JLabel resVolLabel;   
   JLabel status;
   Plot volumePanel;
   Plot ratePanel; 
   String oldlogfile="";
   boolean warnLog=false;
   Boolean logopen=false;
   PrintWriter log=null;
   Font originalFont;
   
//   Thread countdownThread;
   CountDown countdown = null;
   PumpMonitor pumpMonitor = null;

   PumpGUI (PumpMessages msgs,PumpParameters parms,PumpCommands cmds) {
      this.parms = parms;
      this.post = msgs;
      this.cmds = cmds;
      
      
// traps shutdown events
// cancel buttons and menu exit item do frame.dispose() which eventually terminates jvm and gets caught here
// os window close gets trapped
// os cntl-c gets trapped
// os kill and kill -15 gets trapped
// os kill -9 does not get trapped
      Runtime.getRuntime().addShutdownHook(new Thread() {
         public void run() {
            shutdown(0);
         }
      });


            
      // injtime defaults to 00:00:00 when null
      if ( ! this.parms.getString("user/injtime").equals("00:00:00") ) {
         this.infused = true;
      }
      
      // dose flag
      // decide if user is coming in for dose only calculations before
      // gui builds windows.  dose panel will set hl if it isn't already set
      boolean dose = ( (Double)this.parms.get("config/hl") == 0.0 );
            
      this.tabs = new JTabbedPane();
      this.tabs.add("Connect",this.connectPanel());
      this.tabs.add("Parms",this.parmsPanel());
      this.tabs.add("Infuse",this.infusePanel());
      this.tabs.add("Dose",this.dosePanel());
      if ( ! this.infused ) {
         this.tabs.setEnabledAt(1,false);  // parms tab
         this.tabs.setEnabledAt(2,false);  // infuse tab
      }
      if ( dose ) {
         this.tabs.setEnabledAt(0,false);  // test tab
         this.tabs.setEnabledAt(1,false);  // parms tab
         this.tabs.setEnabledAt(2,false);  // infuse tab
         this.tabs.setSelectedIndex(3);  // dose tab
      }
      
      this.frame = new JFrame("Computer Controlled Pump Injection");
      frame.add(this.tabs);
      frame.setJMenuBar(this.menu());                 
           
      // EXIT_ON_CLOSE has problems.
      // When operating system intiates the close with a window close or a
      // cntrl-c operation the frame.dispose call hangs.  Forums suggest
      // this is some kind of conflict within java and suggest switching
      // to some other close operation.  DISPOSE_ON_CLOSE seems to work
      // for now.  
      
//       EXIT_ON_CLOSE will hang inside shutdown() when os closes window and
//       there is an pump monitor error visible.  
//       DISPOSE_ON_CLOSE will close windows but never get to shutdown() when
//       os closes while pump is running.  
           
      frame.pack();
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setLocation(200,200);
      frame.setVisible(true);      
   
   }

   
   private JMenuBar menu () {        
     
      JMenu file  = new JMenu("File");
      JMenuItem openItem = new JMenuItem("Open");
      openItem.setEnabled(false);
      openItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            // do nothing -- yet
         }
      });
      JMenuItem saveItem = new JMenuItem("Save");
      saveItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            parms.setWarnOut(false); // always try to save on demand regardless of previous success/failure
            parms.saveXML();
         }
      });
      JMenuItem saveAsItem = new JMenuItem("Save As..");
      saveAsItem.setEnabled(false);
      saveAsItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            // do nothing -- yet
         }
      });
      JMenuItem printItem = new JMenuItem("Print...");
      printItem.setEnabled(false);
      printItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            // do nothing -- yet
         }
      });
      JMenuItem exitItem = new JMenuItem("Exit");
      exitItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            stopThreads();
            frame.dispose();
         }
      });
      
      file.add(openItem);
      file.add(saveItem);
      file.add(saveAsItem);
      file.addSeparator();
      file.add(printItem);
      file.addSeparator();
      file.add(exitItem);



      JMenu edit = new JMenu("Edit");
      JMenu setFontMenu = new JMenu("Set Font");
      final JMenuItem smallerItem = new JMenuItem("Smaller");
      final JMenuItem biggerItem = new JMenuItem("Larger");
      final JMenuItem item050 = new JMenuItem(" 50%");
      final JMenuItem item075 = new JMenuItem(" 75%");
      final JMenuItem item100 = new JMenuItem("100%");
      final JMenuItem item125 = new JMenuItem("125%");
      final JMenuItem item150 = new JMenuItem("150%");
      final JMenuItem item200 = new JMenuItem("200%");
      final JMenuItem item400 = new JMenuItem("400%");

      
      setFontMenu.add(smallerItem);
      setFontMenu.add(biggerItem);
      setFontMenu.addSeparator();
      setFontMenu.add(item050);
      setFontMenu.add(item075);
      setFontMenu.add(item100);
      setFontMenu.add(item125);
      setFontMenu.add(item150);
      setFontMenu.add(item200);
      setFontMenu.add(item400);
      
      edit.add(setFontMenu);
            
   
      JMenu help = new JMenu("Help");
      help.add(new JMenuItem("About..."));
      
      JMenuBar main = new JMenuBar();
      main.add(file);
      main.add(edit);
      main.add(help);
      
      
      // make font smaller
      smallerItem.setAccelerator(
         KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT,Event.CTRL_MASK));
      smallerItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            Font oldfont = frame.getFont();
            float oldsize = oldfont.getSize2D();
            float newsize = oldsize * (float)0.90;
            Font newfont = oldfont.deriveFont(newsize);
            changeFont(frame,newfont);
         }
      });
      // make font bigger
      biggerItem.setAccelerator(
         KeyStroke.getKeyStroke(KeyEvent.VK_ADD,Event.CTRL_MASK));  // VK_PLUS doesnt work
      biggerItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            Font oldfont = frame.getFont();
            float oldsize = oldfont.getSize2D();
            float newsize = oldsize * (float)1.10;
            Font newfont = oldfont.deriveFont(newsize);
            changeFont(frame,newfont);
         }
      });
      // make font 50% of original
      item050.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            float oldsize = originalFont.getSize2D();
            float newsize = oldsize * (float)0.50;
            Font newfont = originalFont.deriveFont(newsize);
            changeFont(frame,newfont);
         }
      });
      item075.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            float oldsize = originalFont.getSize2D();
            float newsize = oldsize * (float)0.75;
            Font newfont = originalFont.deriveFont(newsize);
            changeFont(frame,newfont);
         }
      });
      item100.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            float oldsize = originalFont.getSize2D();
            float newsize = oldsize * (float)1.00;
            Font newfont = originalFont.deriveFont(newsize);
            changeFont(frame,newfont);
         }
      });
      item125.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            float oldsize = originalFont.getSize2D();
            float newsize = oldsize * (float)1.25;
            Font newfont = originalFont.deriveFont(newsize);
            changeFont(frame,newfont);
         }
      });
      item150.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            float oldsize = originalFont.getSize2D();
            float newsize = oldsize * (float)1.50;
            Font newfont = originalFont.deriveFont(newsize);
            changeFont(frame,newfont);
         }
      });
      item200.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            float oldsize = originalFont.getSize2D();
            float newsize = oldsize * (float)2.00;
            Font newfont = originalFont.deriveFont(newsize);
            changeFont(frame,newfont);
         }
      });
      item400.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            float oldsize = originalFont.getSize2D();
            float newsize = oldsize * (float)4.00;
            Font newfont = originalFont.deriveFont(newsize);
            changeFont(frame,newfont);
         }
      });
      
      this.originalFont = main.getFont();
      return main;
      
   }
      
     
   
   
   private JPanel connectPanel () {
      JPanel mainPanel = new JPanel(new BorderLayout());
      
      JPanel textPanel = new JPanel();
      textPanel.add(new JLabel(this.parms.getString("config/syringe_name")));
      
      final JPanel connectPanel = new JPanel(new GridBagLayout());
      GridBagConstraints tmpBag = new GridBagConstraints();
      tmpBag.weightx=1;
            
      tmpBag.gridx=0;
      tmpBag.gridy=0;
      tmpBag.anchor=GridBagConstraints.EAST;
      connectPanel.add(new JLabel("Room: "),tmpBag);
      
      PickRoom pick = new PickRoom();
      final HashMap<String,String> IPlist = pick.getIPlist();
//       SortedSet roomList = IPlist.keySet();
      Object [] roomList = IPlist.keySet().toArray();
      String room = pick.getRoom();
      
      tmpBag.gridx=1;
      tmpBag.gridy=0;
      tmpBag.anchor=GridBagConstraints.WEST;
      final JComboBox roomBox = new JComboBox(roomList);
      roomBox.setSelectedItem(room);
      connectPanel.add(roomBox,tmpBag);
      
      tmpBag.gridx=0;
      tmpBag.gridy=1;
      tmpBag.anchor=GridBagConstraints.EAST;
      connectPanel.add(new JLabel("Line: "),tmpBag);
      
      tmpBag.gridx=1;
      tmpBag.gridy=1;
      tmpBag.anchor=GridBagConstraints.WEST;
      String [] lines = { "1", "2" };
      final JComboBox lineBox = new JComboBox(lines);
      connectPanel.add(lineBox,tmpBag);

      tmpBag.gridx=0;
      tmpBag.gridy=2;
      tmpBag.gridwidth=2;
      tmpBag.anchor=GridBagConstraints.CENTER;
      tmpBag.fill=GridBagConstraints.BOTH;
      tmpBag.weighty=1;
      this.connectPane = new JTextPane();
      this.connectPane.setEditable(false);
      connectPanel.add(new JScrollPane(this.connectPane),tmpBag);  // 15,40
      tmpBag.gridwidth=1;
      tmpBag.fill=GridBagConstraints.NONE;
      tmpBag.weighty=0;
      
      
      JPanel buttonPanel = new JPanel();
      final JButton testButton;
      
      testButton = new JButton("Test Pump");
      testButton.addActionListener(new ActionListener() {
         public void actionPerformed (ActionEvent ae) {
            
            String room = (String)roomBox.getSelectedItem();
            String ip = IPlist.get(room);
            String line = (String)lineBox.getSelectedItem();
            int port = 10001 + Integer.parseInt(line);
            
            parms.put("user/ip",ip);
            parms.put("user/room",room);
            parms.put("user/line",line);
            parms.put("user/port",port);
      
            goodConnection = testConnection();    
            if ( goodConnection ) {
               tabs.setEnabledAt(1,true); // parms tab
//               tabs.setEnabledAt(2,true); // infuse tab
               testButton.setEnabled(false);
               roomBox.setEnabled(false);
               lineBox.setEnabled(false);
            }            
         }
      });      
      testButton.setEnabled(! this.goodConnection);
      buttonPanel.add(testButton);
      
      final JButton stopButton = new JButton("Stop Pump");
      stopButton.addActionListener(new ActionListener() {
         public void actionPerformed (ActionEvent ae) {
            if ( goodConnection ) {
               stopPump();
            }
         }
      });      
      buttonPanel.add(stopButton);
      
      
      JButton nextButton = new JButton("Next Tab");
      nextButton.addActionListener(new ActionListener() {
         public void actionPerformed (ActionEvent ae) {
            int nTabs = tabs.getTabCount();
            int next = tabs.getSelectedIndex()+1;
            if ( next < nTabs && tabs.isEnabledAt(next)) {
               tabs.setSelectedIndex(next);
            }
         }
      });      
      buttonPanel.add(nextButton);
      
      
      mainPanel.add(textPanel,BorderLayout.NORTH);
      mainPanel.add(connectPanel,BorderLayout.CENTER);
      mainPanel.add(buttonPanel,BorderLayout.SOUTH);
      return mainPanel;
   }
   
   
   private JPanel parmsPanel () {
      JPanel mainPanel = new JPanel(new BorderLayout());
      
      
      JPanel textPanel = new JPanel();
      textPanel.add(new JLabel(this.parms.getString("config/syringe_name")));
      
      
      final JPanel parmPanel = new JPanel(new GridBagLayout());
      // GridBag defaults for JTextField widgets
      GridBagConstraints textBag = new GridBagConstraints();
      textBag.anchor=GridBagConstraints.CENTER;
      textBag.fill=GridBagConstraints.HORIZONTAL;
      textBag.weightx=1;
      textBag.insets = new Insets(3,3,3,3);
      // GridBag defaults for JLabel widgets
      GridBagConstraints labelBag = new GridBagConstraints();
      labelBag.insets = new Insets(3,3,3,3);

            
      labelBag.gridx=0;
      labelBag.gridy=0;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Patient Name: "),labelBag);
            
      textBag.gridx=1;
      textBag.gridy=0;
      parmPanel.add(this.makePNameField(),textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=0;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("e.g. smith,john"),labelBag);
      
      labelBag.gridx=0;
      labelBag.gridy=1;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Patient ID: "),labelBag);

                  
      textBag.gridx=1;
      textBag.gridy=1;
      parmPanel.add(this.makePidField(),textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=1;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("nn-nn-nn-n"),labelBag);
      
      labelBag.gridx=0;
      labelBag.gridy=2;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Injection Number: "),labelBag);
      
   
      textBag.gridx=1;
      textBag.gridy=2;
      parmPanel.add(this.makeInjNumField(),textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=2;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel(""),labelBag);
      
      
      // spacer
      labelBag.gridx=2;
      labelBag.gridy=3;
      parmPanel.add(new JLabel(" "),labelBag);
      
      if ( (Boolean)parms.get("config/prompt_ds") ) {
         labelBag.gridx=0;
         labelBag.gridy=4;
         labelBag.anchor=GridBagConstraints.EAST;
         parmPanel.add(new JLabel("Dead Space: "),labelBag);
      
         
         textBag.gridx=1;
         textBag.gridy=4;
         parmPanel.add(this.makeDsField(),textBag);
         
         labelBag.gridx=2;
         labelBag.gridy=4;
         labelBag.anchor=GridBagConstraints.WEST;
         parmPanel.add(new JLabel("cc's"),labelBag);
      }
      
      
      labelBag.gridx=0;
      labelBag.gridy=5;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Length of Infusion: "),labelBag);
      
      
      textBag.gridx=1;
      textBag.gridy=5;
      parmPanel.add(this.makeTField(),textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=5;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("minutes"),labelBag);


      labelBag.gridx=0;
      labelBag.gridy=6;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Activity to Inject: "),labelBag);
      
         
      textBag.gridx=1;
      textBag.gridy=6;
      parmPanel.add(this.makeDesiredField(),textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=6;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("mCi"),labelBag);

      labelBag.gridx=0;
      labelBag.gridy=7;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Assayed Activity in Syringe: "),labelBag);
      
                   
      textBag.gridx=1;
      textBag.gridy=7;
      parmPanel.add(this.makeDose0Field(),textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=7;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("mCi"),labelBag);


      labelBag.gridx=0;
      labelBag.gridy=8;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Assay Time: "),labelBag);
      
                   
      textBag.gridx=1;
      textBag.gridy=8;
      parmPanel.add(this.makeTime0Field(),textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=8;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("hh:mm:ss"),labelBag);


      labelBag.gridx=0;
      labelBag.gridy=9;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Total Volume in Syringe: "),labelBag);
      
                  
      textBag.gridx=1;
      textBag.gridy=9;
      final JTextField tmp = this.makeVtotField(); // used to seed checkParms()
      parmPanel.add(tmp,textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=9;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("cc's"),labelBag);
      
      if ( (Boolean)this.parms.get("config/mass_check") ) {
         labelBag.gridx=0;
         labelBag.gridy=10;
         labelBag.anchor=GridBagConstraints.EAST;
         parmPanel.add(new JLabel("Max Injectable Volume from Radiopharmacist: "),labelBag);
         
                      
         textBag.gridx=1;
         textBag.gridy=10;
         parmPanel.add(this.makeMassField(),textBag);
         
         labelBag.gridx=2;
         labelBag.gridy=10;
         labelBag.anchor=GridBagConstraints.WEST;
         parmPanel.add(new JLabel("cc's"),labelBag);
      }

      // as user tabs away from this panel, check the data in all the fields
      // show dialog boxes for any warnings/errors.  set goodParms flag as appropriate           
      parmPanel.addAncestorListener(new AncestorListener() {          
         public void ancestorAdded(AncestorEvent e) {}
         public void ancestorMoved(AncestorEvent e) {}
         public void ancestorRemoved(AncestorEvent e ) {
            goodParms = checkFields(parmPanel,false);
         }
      });
      
      JScrollPane parmPane = new JScrollPane(parmPanel);
      parmPane.setBorder(null);

      
      JPanel buttonPanel = new JPanel();
      JButton nextButton = new JButton("Next Tab");
      nextButton.addActionListener(new ActionListener() {
         public void actionPerformed (ActionEvent ae) {
            int nTabs = tabs.getTabCount();
            int next = tabs.getSelectedIndex()+1;
            if ( next < nTabs && tabs.isEnabledAt(next)) {
               tabs.setSelectedIndex(next);
            }
         }
      });      
      buttonPanel.add(nextButton);
      
      
      mainPanel.add(textPanel,BorderLayout.NORTH);
      mainPanel.add(parmPane,BorderLayout.CENTER);
      mainPanel.add(buttonPanel,BorderLayout.SOUTH);
      
      
      return mainPanel;
   }
   
   
   private JPanel infusePanel () {
      JPanel mainPanel = new JPanel(new BorderLayout());
      
      JPanel textPanel = new JPanel();
      textPanel.add(new JLabel(this.parms.getString("config/syringe_name")));


      JPanel tmpPanel;
      JPanel infusePanel = new JPanel(new GridBagLayout());
      // GridBag defaults for PumpTextField widgets
      GridBagConstraints barBag = new GridBagConstraints();
      barBag.anchor=GridBagConstraints.WEST;
      barBag.fill=GridBagConstraints.BOTH;
      barBag.weightx=1;
      barBag.weighty=0;
//      barBag.insets = new Insets(3,3,3,3);
      barBag.gridheight = 3;
      // GridBag defaults for JLabel widgets
      GridBagConstraints labelBag = new GridBagConstraints();
      labelBag.insets = new Insets(0,3,0,3);
      labelBag.weightx=0;
      labelBag.weighty=0;
      labelBag.anchor=GridBagConstraints.EAST;
      labelBag.fill=GridBagConstraints.HORIZONTAL;
      GridBagConstraints tmpBag = new GridBagConstraints();
      tmpBag.fill = GridBagConstraints.BOTH;
      tmpBag.weighty=1;      
      tmpBag.weightx=1;
      
      labelBag.gridx=0;
      labelBag.gridy=0;
      infusePanel.add(new JLabel("Time to Infuse:"),labelBag);
      
      // set limits based on halflife
      // 2 halflives on the low side, 1 halflife on the high side
      int minGo = (int)((Double)this.parms.get("config/hl")*60*2*-1);
      int maxGo = (int)((Double)this.parms.get("config/hl")*60*1);
      this.goBar = new PumpProgressBar(minGo,0,0,maxGo);
      goBar.setValue(0);
      goBar.setRatio(2,1);
      JProgressBar left = goBar.getLeft();
      left.setForeground(infusePanel.getBackground());
      left.setBackground(Color.GREEN);
      JProgressBar right = goBar.getRight();
      right.setBorderPainted(true);
      right.setForeground(Color.RED);
      
      barBag.gridx=1;
      barBag.gridy=0;
      barBag.gridheight=1;
      infusePanel.add(goBar,barBag);
      barBag.gridheight=3;

//       labelBag.gridx=1;
//       labelBag.gridy=1;
//       infusePanel.add(new JLabel("-20 min",SwingConstants.LEFT),labelBag);
      
      this.goLabel = new JLabel("",SwingConstants.CENTER);      
      labelBag.gridx=1;
      labelBag.gridy=1;
      infusePanel.add(this.goLabel,labelBag);
      this.setGoFont();
      
//       labelBag.gridx=1;
//       labelBag.gridy=1;
//       infusePanel.add(new JLabel("+10 min",SwingConstants.RIGHT),labelBag); 
      
      labelBag.gridx=1;
      labelBag.gridy=2;
      infusePanel.add(new JLabel(" "),labelBag); // spacer
      
      labelBag.gridx=0;
      labelBag.gridy=4;
      infusePanel.add(new JLabel("Dead Space:",SwingConstants.RIGHT),labelBag);
   
      final PumpProgressBar dsBar;
      dsBar = new PumpProgressBar(0,100);
      dsBar.setRatio(1,0);
      left = dsBar.getLeft();      
//       left.setForeground(infusePanel.getBackground());
//       left.setBackground(Color.GREEN);
      dsBar.setValue(0);
      
      barBag.gridx=1;
      barBag.gridy=3;
      infusePanel.add(dsBar,barBag);
      

      labelBag.gridx=2;
      labelBag.gridy=3;
      this.dsRateLabel = new JLabel("",SwingConstants.RIGHT);
      infusePanel.add(dsRateLabel,labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=3;
      infusePanel.add(new JLabel("cc/min",SwingConstants.LEFT),labelBag);

      labelBag.gridx=2;
      labelBag.gridy=4;
      this.dsVolLabel = new JLabel("",SwingConstants.RIGHT);
      infusePanel.add(dsVolLabel,labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=4;
      infusePanel.add(new JLabel("cc",SwingConstants.LEFT),labelBag);

      labelBag.gridx=2;
      labelBag.gridy=5;
      this.dsDurLabel = new JLabel("",SwingConstants.RIGHT);
      infusePanel.add(dsDurLabel,labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=5;
      infusePanel.add(new JLabel("secs",SwingConstants.LEFT),labelBag);
      
      labelBag.gridx=2;
      labelBag.gridy=6;
      infusePanel.add(new JLabel(" "),labelBag); // spacer

      labelBag.gridx=0;
      labelBag.gridy=8;
      infusePanel.add(new JLabel("Bolus:",SwingConstants.RIGHT),labelBag);
      
      final PumpProgressBar bolusBar;
      bolusBar = new PumpProgressBar(100,200);
      bolusBar.setRatio(1,0);
      left = bolusBar.getLeft();            
//       left.setForeground(infusePanel.getBackground());
//       left.setBackground(Color.GREEN);
      left.setStringPainted(true);
      bolusBar.setValue(0);
      
      barBag.gridx=1;
      barBag.gridy=7;
      infusePanel.add(bolusBar,barBag);
      
      
      labelBag.gridx=2;
      labelBag.gridy=7;
      this.bolusRateLabel = new JLabel("",SwingConstants.RIGHT);
      infusePanel.add(bolusRateLabel,labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=7;
      infusePanel.add(new JLabel("cc/min",SwingConstants.LEFT),labelBag);
            
      labelBag.gridx=2;
      labelBag.gridy=8;
      this.bolusVolLabel = new JLabel("",SwingConstants.RIGHT);
      infusePanel.add(bolusVolLabel,labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=8;
      infusePanel.add(new JLabel("cc",SwingConstants.LEFT),labelBag);

      labelBag.gridx=2;
      labelBag.gridy=9;
      this.bolusDurLabel = new JLabel("",SwingConstants.RIGHT);
      infusePanel.add(bolusDurLabel,labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=9;
      infusePanel.add(new JLabel("secs",SwingConstants.LEFT),labelBag);
      
      labelBag.gridx=0;
      labelBag.gridy=10;
      infusePanel.add(new JLabel(" "),labelBag); // spacer
      
      labelBag.gridx=0;
      labelBag.gridy=12;
      infusePanel.add(new JLabel("Infusion:",SwingConstants.RIGHT),labelBag);
      
      final PumpProgressBar ssBar;
      ssBar = new PumpProgressBar(300,400);
      ssBar.setRatio(1,0);
      left = ssBar.getLeft();      
//       left.setForeground(infusePanel.getBackground());
//       left.setBackground(Color.GREEN);
      left.setStringPainted(true);
      ssBar.setValue(0);
      
      barBag.gridx=1;
      barBag.gridy=11;
      infusePanel.add(ssBar,barBag);
      
      labelBag.gridx=2;
      labelBag.gridy=11;
      this.ssRateLabel = new JLabel("",SwingConstants.RIGHT);
      infusePanel.add(ssRateLabel,labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=11;
      infusePanel.add(new JLabel("cc/min",SwingConstants.LEFT),labelBag);

      labelBag.gridx=2;
      labelBag.gridy=12;
      this.ssVolLabel = new JLabel("",SwingConstants.RIGHT);
      infusePanel.add(ssVolLabel,labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=12;
      infusePanel.add(new JLabel("cc",SwingConstants.LEFT),labelBag);

      labelBag.gridx=2;
      labelBag.gridy=13;
      this.ssDurLabel = new JLabel("",SwingConstants.RIGHT);
      infusePanel.add(ssDurLabel,labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=13;
      infusePanel.add(new JLabel("mins",SwingConstants.LEFT),labelBag);
      
      labelBag.gridx=2;
      labelBag.gridy=14;
      infusePanel.add(new JLabel(" "),labelBag); // spacer
      
      labelBag.gridx=1;
      labelBag.gridy=15;
      infusePanel.add(new JLabel("Expected Residual Volume:",SwingConstants.RIGHT),labelBag);

      labelBag.gridx=2;
      labelBag.gridy=15;
      this.resVolLabel = new JLabel("",SwingConstants.RIGHT);
      infusePanel.add(resVolLabel,labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=15;
      infusePanel.add(new JLabel("cc",SwingConstants.LEFT),labelBag);
      
      labelBag.gridx=1;
      labelBag.gridy=16;
      infusePanel.add(new JLabel("Infusion Status:",SwingConstants.RIGHT),labelBag);
      
      String statusLabel = (this.infused) ? "Done" : "Not Started Yet";
      this.status = new JLabel(statusLabel,SwingConstants.LEFT);
      status.setForeground(Color.BLUE);
      
      labelBag.gridx=2;
      labelBag.gridy=16;
      labelBag.gridwidth=GridBagConstraints.REMAINDER;
      infusePanel.add(status,labelBag);
      labelBag.gridwidth=1;

      labelBag.gridx=2;
      labelBag.gridy=17;
      infusePanel.add(new JLabel(" "),labelBag); // spacer
      
      
      JScrollPane infusePane = new JScrollPane(infusePanel);
      infusePane.setBorder(null);
        
      double time,vol;
      List<Point2D.Double> baseline = new ArrayList<Point2D.Double>();
      
      this.volumePanel = new Plot();
      volumePanel.setResolution(600,100);
      volumePanel.setTicks(1,1);
      volumePanel.setLabels("Time (min)","Vol (cc)");
      volumePanel.setMax(new Point2D.Double(5,10));
      JScrollPane volumePane = new JScrollPane(volumePanel);
      volumePane.setBorder(null);
      
           
      this.ratePanel = new Plot();
      ratePanel.setResolution(600,100);
      ratePanel.setTicks(1,1);
      ratePanel.setLabels("Time (min)","Rate (cc/min)");
      ratePanel.setMax(new Point2D.Double(5,20));
      JScrollPane ratePane = new JScrollPane(ratePanel);
      ratePane.setBorder(null);
      
      
      JTabbedPane views = new JTabbedPane();
      views.add("Time",infusePane);
      views.add("Volume",volumePane);
      views.add("Rate",ratePane);
      

      
      // polling the pump for vol and rate takes about 120ms.  so sleeping
      // for 880ms between pump queries gives an average polling rate of 
      // about once a second.
//      final Thread pumpMonitor = new Thread(new PumpMonitor(cmds,this,880)); 
      
      this.pumpMonitor = new PumpMonitor(cmds,this,880);
      final Thread pumpMonitorThread = new Thread(pumpMonitor);
      this.countdown = new CountDown(this);
      final Thread countdownThread = new Thread(countdown);
      
      final JButton infuseButton = new JButton("Infuse");
      final JButton stopButton = new JButton("Stop Pump");     
      final JButton nextButton = new JButton("Next Tab");      
      
      // infuse button actions
      infuseButton.setEnabled(false);
      infuseButton.addActionListener(new ActionListener() {
         public void actionPerformed (ActionEvent ae) {
            try {
               // update infusion profile
               Timestamp time_now = new Timestamp(System.currentTimeMillis());
               double dose_now = parms.calc_dose(time_now);
               parms.calc_fraction();
               parms.calc_volume(dose_now,false);  // disable warnings
               parms.calc_profile(dose_now,false);  // disable warnings
               
               // load pump program
               cmds.LoadPgm(
                  (Double)parms.get("config/diam"),
                  (Double)parms.get("infuse/ds/rate"),
                  (Double)parms.get("infuse/ds/vol"),
                  (Double)parms.get("infuse/bolus/rate"),
                  (Double)parms.get("infuse/bolus/vol"),
                  (Double)parms.get("infuse/ss/rate"),
                  (Double)parms.get("infuse/ss/vol"));
               
               // start pump
               cmds.Run();
               // takes just under 4 seconds to recalculate infusion profile and
               // program the pump.  The infusion profile is calculated on an
               // injection time at the start of that four seconds.  Not accounting
               // for that 4 second delay will tend to underdose the patient 
               // slightly.  Is this a significant problem?  Probably not, but
               // track both just in case.                 
               parms.put("user/systime",time_now.toString().substring(11,19));
               parms.put("user/injtime",(new Timestamp(System.currentTimeMillis())).toString().substring(11,19));
               // start pump monitor
               pumpMonitorThread.start();
               countdown.stop();
               // update predicted dose (needed to wait for injtime)
               parms.calc_predictedDose();               
               // update gui
               displayInfusionProfile();
               infuseButton.setEnabled(false);
               stopButton.setEnabled(true);
               status.setText("Running");
               infused = true;
               // update plots
               List<Point2D.Double> volbaseline = new ArrayList<Point2D.Double>();
               double vol=0;
               volbaseline.clear();
               volbaseline.add(new Point2D.Double(0,0));
               if ((Double)parms.get("infuse/ds/vol") != 0 ) {
                  volbaseline.add(new Point2D.Double((Double)parms.get("infuse/ds/time"),vol+=(Double)parms.get("infuse/ds/vol")));
               }
               if ((Double)parms.get("infuse/bolus/vol") != 0 ) {
                  volbaseline.add(new Point2D.Double((Double)parms.get("infuse/bolus/time"),vol+=(Double)parms.get("infuse/bolus/vol")));
               }
               if ((Double)parms.get("infuse/ss/vol") != 0 ) {
                  volbaseline.add(new Point2D.Double((Double)parms.get("infuse/ss/time"),vol+=(Double)parms.get("infuse/ss/vol")));
               }
               volumePanel.addBaseline(volbaseline);
               List<Point2D.Double> ratebaseline = new ArrayList<Point2D.Double>();
               ratebaseline.clear();
               if ((Double)parms.get("infuse/ds/rate") != 0 ) {
                  ratebaseline.add(new Point2D.Double(0                                  ,(Double)parms.get("infuse/ds/rate")));
                  ratebaseline.add(new Point2D.Double((Double)parms.get("infuse/ds/time"),(Double)parms.get("infuse/ds/rate")));
               }
               if ((Double)parms.get("infuse/bolus/rate") != 0 ) {               
                  ratebaseline.add(new Point2D.Double((Double)parms.get("infuse/ds/time"),(Double)parms.get("infuse/bolus/rate")));
                  ratebaseline.add(new Point2D.Double((Double)parms.get("infuse/bolus/time"),(Double)parms.get("infuse/bolus/rate")));
               }
               if ((Double)parms.get("infuse/ss/rate") != 0 ) {
                  ratebaseline.add(new Point2D.Double((Double)parms.get("infuse/bolus/time"),(Double)parms.get("infuse/ss/rate")));
                  ratebaseline.add(new Point2D.Double((Double)parms.get("infuse/ss/time"),(Double)parms.get("infuse/ss/rate")));
               }
               Double maxY = Math.max((Double)parms.get("infuse/ds/rate"),Math.max((Double)parms.get("infuse/bolus/rate"),(Double)parms.get("infuse/ss/rate")));
               ratePanel.addBaseline(ratebaseline);
               // save info to xml file
               parms.saveXML();
            } catch (PumpException exp) {
               System.out.println("Could not run program");
               // do something
            }
         }
      });
      infuseButton.addAncestorListener(new AncestorListener() {
         public void ancestorAdded(AncestorEvent e) {
            infuseButton.setEnabled(goodConnection && goodParms && ! infused);
            
            boolean display = true;
            if ( ! infused ) {
               Timestamp time_now = new Timestamp(System.currentTimeMillis());
               Timestamp time_inj = null;
               if ( goodConnection && goodParms ) {
                  // get new infusion profile
                  try {
                     double dose_now = parms.calc_dose(time_now);
                     parms.calc_fraction();
                     parms.calc_volume(dose_now);
                     parms.calc_profile(dose_now);
                     parms.calc_predictedDose();
                     time_inj = parms.calc_injectBy(time_now,dose_now);
                     if ( ! parms.checkProfile() ) {
                        infuseButton.setEnabled(false);
                        String msg="";
                        msg += "Infusion Profile looks funky\n";
                        msg += "Recheck your infusion parameters\n";
                        post.warning(msg);
                     }
                  } catch (PumpException e2) {
                     // null values in parm list
                     // dont display any profile info
                     display = false;
                  }
               } else {
                  // clear infusion profile
                  parms.put("infuse/ds/rate",null);
                  parms.put("infuse/ds/vol",null);
                  parms.put("infuse/ds/dur",null);
                  parms.put("infuse/ds/time",null);
                  parms.put("infuse/bol/rate",null);
                  parms.put("infuse/bol/vol",null);
                  parms.put("infuse/bol/dur",null);
                  parms.put("infuse/bol/rate",null);
                  parms.put("infuse/ss/vol",null);
                  parms.put("infuse/ss/dur",null);
                  parms.put("infuse/ss/time",null);
                  parms.put("infuse/ss/time",null);
                  display = false;
               }
               // display infusion profile & size progress bars
               if ( display ) {
                  displayInfusionProfile();
                  countdown.setTime(time_inj);
                  Thread countdownThread = new Thread(countdown);
                  countdownThread.start();
               }
               // open log file for pump progress reports  
               String logfile = parms.getXMLfilename().replace(".xml",".csv"); 
               if ( ! logfile.equals(oldlogfile) ) { warnLog = false; oldlogfile = logfile;}                   
               if ( (! logopen) && (! warnLog) ) { 
                  try {
                     File f = new File(logfile);
                     FileWriter fw = new FileWriter(f);
                     log = new PrintWriter(fw,true);  // set auto-flush on
                     logopen = true;
                  } catch ( Exception el ) {
                     String msg;
                     msg = "Couldn't open log file \n";
                     msg += logfile + "\n";
                     msg += "\n";
                     msg += "This is not a fatal error.  If time is an issue you can \n";
                     msg += "continue the infusion in spite of this problem.  Its just \n";
                     msg += "that the infusion progress information you see on screen\n";
                     msg += "won't be recorded for possible review as it normally would.\n";
                     msg += "\n";
                     msg += "Contact Physics Support when time allows.\n";
//                      msg += "Every second or so the pump is queried for \n";
//                      msg += "current rate, volume infused to date and \n";
//                      msg += "current status information.  The data is stored\n";
//                      msg += "in a log file so it can be reviewed later if \n";
//                      msg += "there was a problem. \n";
//                      msg += "\n";
//                      msg += "For some reason, the program cannot open this file.\n";
//                      msg += "Check that the directory exists and the permissions\n";
//                      msg += "are correct.  If the file already exists check its \n";
//                      msg += "permissions.  They may be keeping the program from\n";
//                      msg += "overwriting it.\n";
//                      msg += "\n";
//                      msg += "This is not a fatal error.  The infusion can proceed\n";
//                      msg += "without this log file\n";
//                      msg += "\n";
                     post.warning(msg);
                     warnLog = true;
                  }
               }
               // save info to xml file (really just testing file permissions at this point)
               parms.saveXML();
            } else {
               displayInfusionProfile();
            }
         }
         public void ancestorMoved(AncestorEvent e) {}
         public void ancestorRemoved(AncestorEvent e ) {
            countdown.stop();
         }
      });
      
      // stop button actions
      stopButton.addActionListener(new ActionListener() {
         public void actionPerformed (ActionEvent ae) {
            stopPump();
            stopThreads();
//            stopButton.setEnabled(false);
         }
      });
      stopButton.addAncestorListener(new AncestorListener() {
         public void ancestorAdded(AncestorEvent e) {
            stopButton.setEnabled(infused);
         }
         public void ancestorMoved(AncestorEvent e) {}
         public void ancestorRemoved(AncestorEvent e ) {}
      });
      
      // next button actions
      nextButton.addActionListener(new ActionListener() {
         public void actionPerformed (ActionEvent ae) {
            int nTabs = tabs.getTabCount();
            int next = tabs.getSelectedIndex()+1;
            if ( next < nTabs && tabs.isEnabledAt(next)) {
               tabs.setSelectedIndex(next);
            }
         }
      });      
      
      
      JPanel buttonPanel = new JPanel();
      buttonPanel.add(infuseButton);
      buttonPanel.add(stopButton);
      buttonPanel.add(nextButton);
                  
      this.dsBar = dsBar;
      this.bolusBar = bolusBar;
      this.ssBar = ssBar;

      mainPanel.add(textPanel,BorderLayout.NORTH);
      mainPanel.add(views,BorderLayout.CENTER);
      mainPanel.add(buttonPanel,BorderLayout.SOUTH);
      
      return mainPanel;
   }
      
   
   private JPanel dosePanel () {
      JPanel mainPanel = new JPanel(new BorderLayout());
      PumpTextField tmp;
      int defaultWidth = 12;
      
      JPanel textPanel = new JPanel();
      textPanel.add(new JLabel(this.parms.getString("config/syringe_name")));


      JPanel parmPanel = new JPanel(new GridBagLayout());
      // GridBag defaults for PumpTextField widgets
      GridBagConstraints textBag = new GridBagConstraints();
      textBag.anchor=GridBagConstraints.WEST;
      textBag.fill=GridBagConstraints.HORIZONTAL;
      textBag.weightx=1;
      textBag.insets = new Insets(3,3,3,3);
      // GridBag defaults for JLabel widgets
      GridBagConstraints labelBag = new GridBagConstraints();
      labelBag.insets = new Insets(3,3,3,3);
      labelBag.weightx=0;

      int row=0;
      
      labelBag.gridx=0;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Patient Name: "),labelBag);
      
      textBag.gridx=1;
      textBag.gridy=row;
      parmPanel.add(this.makePNameField(),textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("e.g. smith,john"),labelBag);
      
      row++;
      
      labelBag.gridx=0;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Patient ID: "),labelBag);

      textBag.gridx=1;
      textBag.gridy=row;
      parmPanel.add(this.makePidField(),textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("nn-nn-nn-n"),labelBag);
      
      row++;

      labelBag.gridx=0;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Injection Number: "),labelBag);
                    
      textBag.gridx=1;
      textBag.gridy=row;
      parmPanel.add(this.makeInjNumField(),textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel(""),labelBag);
      
      row++;
      
      if ( (Double)this.parms.get("config/hl") == 0.0 ) {
         labelBag.gridx=0;
         labelBag.gridy=row;
         labelBag.anchor=GridBagConstraints.EAST;
         parmPanel.add(new JLabel("Isotope: "),labelBag);
         
         PickIsotope isotope = new PickIsotope();
         final HashMap<String,Double> isoHash = isotope.getList();
         Object [] isoList = isoHash.keySet().toArray();
         String isoPick = isotope.getDefault();
         
         final JComboBox isoBox = new JComboBox(isoList);
         isoBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               String isoPick = (String)isoBox.getSelectedItem();
               parms.put("config/hl",isoHash.get(isoPick));
            }
         });
         isoBox.setSelectedItem(isoPick);
         
         textBag.gridx=1;
         textBag.gridy=row;
         parmPanel.add(isoBox,textBag);
         
         row++;
      }
         
      
      // spacer
      labelBag.gridx=2;
      labelBag.gridy=row;
      parmPanel.add(new JLabel(" "),labelBag);

      row++;

      labelBag.gridx=1;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("Time"),labelBag);
      
      labelBag.gridx=2;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("Activity"),labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("Volume"),labelBag);     
      
      row++;
      
      labelBag.gridx=1;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("(hh:mm:ss)"),labelBag);
      
      labelBag.gridx=2;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("(mCi)"),labelBag);
      
      labelBag.gridx=3;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("(cc)"),labelBag);
      
      row++;      
      
      labelBag.gridx=0;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Pre-Injection: "),labelBag);
      
      textBag.gridx=1;
      textBag.gridy=row;
      parmPanel.add(this.makeTime0Field(),textBag);
      
      textBag.gridx=2;
      textBag.gridy=row;
      parmPanel.add(this.makeDose0Field(),textBag);      
            
      textBag.gridx=3;
      textBag.gridy=row;
      parmPanel.add(this.makeVtotField(),textBag);
      
      row++;
      
      labelBag.gridx=0;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Injection: "),labelBag);
      
      textBag.gridx=1;
      textBag.gridy=row;
      parmPanel.add(this.makeInjTime(),textBag);
            
      labelBag.gridx=2;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("(uCi)"),labelBag);      

      row++;

      labelBag.gridx=0;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Post-Injection: "),labelBag);

      textBag.gridx=1;
      textBag.gridy=row;
      parmPanel.add(this.makeResTime(),textBag);
      
      textBag.gridx=2;
      textBag.gridy=row;
      parmPanel.add(this.makeResDose(),textBag);
      
      textBag.gridx=3;
      textBag.gridy=row;
      parmPanel.add(this.makeResVol(),textBag);

      row++;

      // spacer
      labelBag.gridx=0;
      labelBag.gridy=row;
      parmPanel.add(new JLabel(" "),labelBag);
       
      row++;

      labelBag.gridx=0;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Desired Dose: "),labelBag);

      textBag.gridx=1;
      textBag.gridy=row;
      parmPanel.add(this.makeDesiredField(),textBag);

      labelBag.gridx=2;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("mCi"),labelBag);

      row++;

      labelBag.gridx=0;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Predicted Dose: "),labelBag);

      textBag.gridx=1;
      textBag.gridy=row;
      parmPanel.add(this.makeDosePredicted(),textBag);

      labelBag.gridx=2;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("mCi"),labelBag);

      row++;

      labelBag.gridx=0;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("Actual Dose: "),labelBag);

      final JTextField actDoseText = this.makeDoseActual();

      textBag.gridx=1;
      textBag.gridy=row;
      parmPanel.add(actDoseText,textBag);
      
      labelBag.gridx=2;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.WEST;
      parmPanel.add(new JLabel("mCi"),labelBag);
            
      row++;
      
      labelBag.gridx=0;
      labelBag.gridy=row;
      labelBag.anchor=GridBagConstraints.EAST;
      parmPanel.add(new JLabel("% Difference: "),labelBag);

      final JTextField diffText = this.makeDoseDiff();
      
      textBag.gridx=1;
      textBag.gridy=row;
      parmPanel.add(diffText,textBag);
      JScrollPane parmPane = new JScrollPane(parmPanel);
      parmPane.setBorder(null);
   
      JButton calcButton = new JButton("Calculate");
      calcButton.addActionListener(new ActionListener() {
         public void actionPerformed (ActionEvent ae) {
            actDoseText.setVisible(false);
            actDoseText.setVisible(true);
            diffText.setVisible(false);
            diffText.setVisible(true);
            parms.calc_actualDose();
         }
      });
      
      JButton printButton = new JButton("Print");
      printButton.addActionListener(new ActionListener() {
         public void actionPerformed (ActionEvent ae) {
            parms.saveXML();
            String report = parms.report();
           
            PrinterJob job = PrinterJob.getPrinterJob();
            PageFormat pf = job.defaultPage();
            Paper p = pf.getPaper();
            p.setImageableArea(18,72,585,648);  // 1/4" left margin,1" top margin,8" writable width,9" writeable height
            pf.setPaper(p);
                        
            job.setPrintable(new PrintText(report),pf);
            try {
               // two copies.  one for techs, one for pharmacist
               job.print();
               job.print();
            } catch (PrinterException e) { e.printStackTrace() ; }
         }
      });      
      
      JButton exitButton = new JButton("Exit");
      exitButton.addActionListener(new ActionListener() {
         public void actionPerformed (ActionEvent ae) {
            stopThreads();
            frame.dispose();
         }
      });      
      
      
      JPanel buttonPanel = new JPanel();
      buttonPanel.add(calcButton);
      buttonPanel.add(printButton);
      buttonPanel.add(exitButton);
                  
      mainPanel.add(textPanel,BorderLayout.NORTH);
      mainPanel.add(parmPane,BorderLayout.CENTER);
      mainPanel.add(buttonPanel,BorderLayout.SOUTH);
      
      return mainPanel;
   }

 
   private JTextField makePNameField () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void focusLost (FocusEvent e) {
            super.focusLost(e);
            if ( tabs.getSelectedIndex() == 1) {
               tabs.setEnabledAt(2,checkFields(this.getParent(),false));
            }
         }
         public boolean editableRule () {
            return ( ! infused );
         }
      };
      tmp.setMask("^.*$");
      tmp.setDescription("Patient Name");
      tmp.setExternalData(parms,"user/name",PumpParameters.Types.STRING);
      
      return tmp;
   }
   
   
   private JTextField makePidField () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public boolean editableRule () {
            return ( ! infused );
         }
         public void focusLost (FocusEvent e) {
            super.focusLost(e);
            if ( tabs.getSelectedIndex() == 1 ) {
               tabs.setEnabledAt(2,checkFields(this.getParent(),false));
            }
         }
      };
      tmp.setMask("^[0-9][0-9]-[0-9][0-9]-[0-9][0-9]-[0-9]$","99-99-99-9");
      tmp.setDescription("Patient ID");
      tmp.setExternalData(parms,"user/id",PumpParameters.Types.STRING);
      
      return tmp;
   }
   
   
   
   private JTextField makeInjNumField () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public boolean editableRule () {
            return ( ! infused );
         }
         public void focusLost (FocusEvent e) {
            super.focusLost(e);
            if ( tabs.getSelectedIndex() == 1 ) {
               tabs.setEnabledAt(2,checkFields(this.getParent(),false));
            }
         }
      };
      tmp.setMask("^[0-9]*$");
      tmp.setDescription("Injection Number");
      tmp.setExternalData(parms,"user/injnum",PumpParameters.Types.INTEGER);
      
      return tmp;
   }
   
   
   
   private JTextField makeDsField () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void focusLost (FocusEvent e) {
            super.focusLost(e);
            if ( tabs.getSelectedIndex() == 1 ) {
               tabs.setEnabledAt(2,checkFields(this.getParent(),false));
            }
         }
         public boolean test (boolean dialogs) {
            // run normal tests
            super.test(dialogs);
            if ( ! (this.testResults == null) ) { return this.testResults; }
                                    
            try {          
               Double value = Double.valueOf(this.getText());
               Double min = 0.0;
               Double max = 0.6;
               if ( min > value || value > max ) { 
//                   String msg = "Dead Space looks funky\n";
//                   msg += "Please Confirm";
//                   String msg = min + " < Dead Space < "+max ;
                  String msg = "Dead Space must be more than "+min+" cc\n";
                  msg += "and less than or equal to "+max+" cc";               
                  this.setBackground(Color.RED);
                  this.setForeground(Color.WHITE);
                  this.setToolTipText(msg);
                  if ( dialogs ) {
                     Component root = SwingUtilities.getRoot(this);
                     JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
                  }
                  return false;
               }
            } 
            catch ( NullPointerException e ) { } 
            catch ( NumberFormatException e ) { }
                 
            this.resetBackground();
            this.setForeground(Color.BLACK);
            this.setToolTipText("");
            return true;            
         }
         public boolean editableRule () {
            return ( ! infused );
         }
      };
      tmp.setMask("^[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Dead Space");
      tmp.setExternalData(parms,"user/ds",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
   
   
   
   private JTextField makeTField () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void focusLost (FocusEvent e) {
            super.focusLost(e);
            if ( tabs.getSelectedIndex() == 1 ) {
               tabs.setEnabledAt(2,checkFields(this.getParent(),false));
            }
         }
         public boolean test (boolean dialogs) {
            super.test(dialogs);
            if ( ! (this.testResults == null) ) { return this.testResults; }
                                    
            try {          
               Double value = Double.valueOf(this.getText());
               Double min = (Double)parms.get("config/tmin");
               Double max = (Double)parms.get("config/tmax");
               if ( min > value || value > max ) {   
                  String msg = "Infusion Duration looks funky\n";
                  msg += "Please confirm";            
//                   String msg = min + " < time < "+max;
                  this.setBackground(Color.YELLOW);
                  this.setForeground(Color.BLACK);
                  this.setToolTipText(msg);
                  if ( dialogs ) {
                     Component root = SwingUtilities.getRoot(this);
                     JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.WARNING_MESSAGE);      
                  }
                  return true;
               }
            } 
            catch ( NullPointerException e ) { } 
            catch ( NumberFormatException e ) { }
                 
            this.resetBackground();
            this.setForeground(Color.BLACK);
            this.setToolTipText("");
            return true;            
         }
         public boolean editableRule () {
            return ( ! infused );
         }
      };
      tmp.setMask("^[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Infusion Length");
      tmp.setExternalData(parms,"user/t",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
   
   private JTextField makeDesiredField () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void focusLost (FocusEvent e) {
            super.focusLost(e);
            if ( tabs.getSelectedIndex() == 1 ) {
               tabs.setEnabledAt(2,checkFields(this.getParent(),false));
            }
         }
         public boolean test (boolean dialogs) {
            super.test(dialogs);
            if ( ! (this.testResults == null) ) { return this.testResults; }

            try {          
               Double value = Double.valueOf(this.getText());
               Double min = (Double)parms.get("config/desired_min_error");
               Double max = (Double)parms.get("config/desired_warning");
               if ( value <= min ) {
                  String msg = "Activity to Inject must be greater than "+min+" mCi";
                  this.setBackground(Color.RED);
                  this.setForeground(Color.WHITE);
                  this.setToolTipText(msg);
                  if ( dialogs ) {
                     Component root = SwingUtilities.getRoot(this);
                     JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
                  }
                  return false;
               }
                 
               if ( value > max ) {    
                  String msg = "Activity to Inject looks funky\n";
                  msg += "Please confirm";
//                   String msg = "Activity to Inject exceeds range\n";
//                   msg += "Should be less than "+max+" mCi";
                  
                  this.setBackground(Color.YELLOW);
                  this.setForeground(Color.BLACK);
                  this.setToolTipText(msg);
                  if ( dialogs ) {
                     Component root = SwingUtilities.getRoot(this);
                     JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.WARNING_MESSAGE);      
                  }
                  return true;
               }
            } 
            catch ( NullPointerException e ) { } 
            catch ( NumberFormatException e ) { }
                                                     
            this.resetBackground();
            this.setForeground(Color.BLACK);
            this.setToolTipText("");
            return true;            
         }
         public boolean editableRule () {
            return ( ! infused );
         }
      };
      tmp.setMask("^[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Desired Dose");
      tmp.setExternalData(parms,"user/desired",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
   
   
   private JTextField makeDose0Field () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void focusLost (FocusEvent e) {
            super.focusLost(e);
            if ( tabs.getSelectedIndex() == 1 ) {
               tabs.setEnabledAt(2,checkFields(this.getParent(),false));
            }
         }
         public boolean test (boolean dialogs) {
            super.test(dialogs);
            if ( ! (this.testResults == null) ) { return this.testResults; }

             try {          
               Double value = Double.valueOf(this.getText());
               Double min = (Double)parms.get("config/dose_min_error");
               Double max = (Double)parms.get("config/dose_warning");
               if ( value <= min ) {
                  String msg = "Activity in Syringe must be more than "+min+" mCi";
                  this.setBackground(Color.RED);
                  this.setForeground(Color.WHITE);
                  this.setToolTipText(msg);
                  if ( dialogs ) {
                     Component root = SwingUtilities.getRoot(this);
                     JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
                  }
                  return false;
               }
              
               if ( value > max ) {     
                  String msg = "Activity in Syringe looks funky\n";
                  msg += "Please confirm";    
//                   String msg = "dose < "+max+" mCi";     
                  this.setBackground(Color.YELLOW);
                  this.setForeground(Color.BLACK);
                  this.setToolTipText(msg);
                  if ( dialogs ) {
                     Component root = SwingUtilities.getRoot(this);
                     JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.WARNING_MESSAGE);      
                  }
                  return true;
               }
            } 
            catch ( NullPointerException e ) { } 
            catch ( NumberFormatException e ) { }
                                                    
            this.resetBackground();
            this.setForeground(Color.BLACK);
            this.setToolTipText("");
            return true;            
         }
         public boolean editableRule () {
            return ( ! infused );
         }
      };
      tmp.setMask("^[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Activity in Syringe");
      tmp.setExternalData(parms,"user/dose0",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
         
   
   private JTextField makeVtotField () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void focusLost (FocusEvent e) {
            super.focusLost(e);
            if ( tabs.getSelectedIndex() == 1 ) {
               tabs.setEnabledAt(2,checkFields(this.getParent(),false));
            }
         }
         public boolean test (boolean dialogs) {
            super.test(dialogs);
            if ( ! (this.testResults == null) ) { return this.testResults; }

            try {          
               Double value = Double.valueOf(this.getText());
               Double min = (Double)parms.get("config/minvol_warning");
               Double max = (Double)parms.get("config/maxvol_warning");
               if ( min > value || value > max ) {  
                  String msg = "Syringe Volume looks funky\n";
                  msg += "Please confirm";
//                   String msg = min+" < volume < "+max;                
                  this.setBackground(Color.YELLOW);
                  this.setForeground(Color.BLACK);
                  this.setToolTipText(msg);
                  if ( dialogs ) {
                     Component root = SwingUtilities.getRoot(this);
                     JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.WARNING_MESSAGE);      
                  }
                  return true;
               }
            } 
            catch ( NullPointerException e ) { } 
            catch ( NumberFormatException e ) { }
                                                    
            this.resetBackground();
            this.setForeground(Color.BLACK);
            this.setToolTipText("");
            return true;            
         }
         public boolean editableRule () {
            return ( ! infused );
         }
      };
      tmp.setMask("^[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Volume in Syringe");
      tmp.setExternalData(parms,"user/vtot",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
   
   private JTextField makeMassField () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void focusLost (FocusEvent e) {
            super.focusLost(e);
            if ( tabs.getSelectedIndex() == 1 ) {
               tabs.setEnabledAt(2,checkFields(this.getParent(),false));
            }
         }
         public boolean test (boolean dialogs) {
            super.test(dialogs);
            if ( ! (this.testResults == null) ) { return this.testResults; }
            
            Double mass = Double.valueOf(this.getText());
            Double vtot = (Double)this.parms.get("user/vtot");
            if ( vtot == null ) {
               String msg = "Mass Volume invalid without a valid Syringe Volume";   
               this.setBackground(Color.RED);
               this.setForeground(Color.WHITE);
               this.setToolTipText(msg);
               if ( dialogs ) {
                  Component root = SwingUtilities.getRoot(this);
                  JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
               }
               return false;
            }
            if ( mass == 0 ) {
               String msg = "Mass Volume cannot be zero";
               this.setBackground(Color.RED);
               this.setForeground(Color.WHITE);
               this.setToolTipText(msg);
               if ( dialogs ) {
                  Component root = SwingUtilities.getRoot(this);
                  JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
               }
               return false;
            }
            if ( mass > vtot ) {
               String msg = "Mass Volume cannot be greater than Syringe Volume";
               this.setBackground(Color.RED);
               this.setForeground(Color.WHITE);
               this.setToolTipText(msg);
               if ( dialogs ) {
                  Component root = SwingUtilities.getRoot(this);
                  JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
               }
               return false;
            }
            this.resetBackground();
            this.setForeground(Color.BLACK);
            this.setToolTipText("");
            return true;            
         }
         public boolean editableRule () {
            return ( ! infused );
         }
      };
      tmp.setMask("^[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Mass Limit");
      tmp.setExternalData(parms,"user/max_vol_mass",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
   
   private JTextField makeTime0Field () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void focusLost (FocusEvent e) {
            super.focusLost(e);
            if ( tabs.getSelectedIndex() == 1 ) {
               tabs.setEnabledAt(2,checkFields(this.getParent(),false));
            }
         }
         public void upload () {
            String time = this.padTime(this.getText());
            this.parms.put(this.key,time,this.type);
            
            // this field may be used by others -- recheck everybody
            checkFields(this.getParent(),false);
         }  
         public void download () {
            this.setText(this.parms.getString(this.key));
            this.test(false);
         }                   
         public boolean test (boolean dialogs) {
            super.test(dialogs);
            if ( ! (this.testResults == null) ) { return this.testResults; }
            
            // don't need try/catch block here
            // already tested for nulls and regex mask assures proper format
            String date = this.parms.getDate();
            String time = this.padTime(this.getText());
            String datetime = date + " "+ time;
            Timestamp value = Timestamp.valueOf(datetime);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            this.setText(time);
            
            if ( value.after(now) ) {
               String msg = "Assay time hasn't happened yet";
               this.setBackground(Color.RED);
               this.setForeground(Color.WHITE);
               this.setToolTipText(msg);
               if ( dialogs ) {
                  Component root = SwingUtilities.getRoot(this);
                  JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
               }
               return false;
            }
            
            // if time_warning is null, skip this check   
            try {          
               Double max = (Double)parms.get("config/time_warning");
               Double diff = (now.getTime() - value.getTime())/(1000.0*60.0);

               if ( diff > max ) {             
                  String msg = "Assay time was more than "+max+" min ago";
                  this.setBackground(Color.YELLOW);
                  this.setForeground(Color.BLACK);
                  this.setToolTipText(msg);
                  if ( dialogs ) {
                     Component root = SwingUtilities.getRoot(this);
                     JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.WARNING_MESSAGE);      
                  }
                  return true;
               }
            } 
            catch ( IllegalArgumentException e ) { } 
                                                    
            this.resetBackground();
            this.setForeground(Color.BLACK);
            this.setToolTipText("");
            return true;            
         }
         public boolean editableRule () {
            return ( ! infused );
         }
      };
      tmp.setMask("^[0-9]{2}:[0-9]{2}:[0-9]{2}$","99:99:99");
      tmp.setDescription("Pre-Assay Time");
      tmp.setExternalData(parms,"user/time0",PumpParameters.Types.STRING);
      
      return tmp;
   }
   


   private JTextField makeInjTime () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void upload () {
            String time = this.padTime(this.getText());
            this.parms.put(this.key,time,this.type);
            
            // this field may be used by others -- recheck everybody
            checkFields(this.getParent(),false);
         }  
         public void download () {
            this.setText(this.parms.getString(this.key));
            this.test(false);
         }                   
         public boolean test (boolean dialogs) {
            super.test(dialogs);
            if ( ! (this.testResults == null) ) { return this.testResults; }
            
            if ( parms.get("user/time0") == null || ((String)parms.get("user/time0")).length() < 8 ) {
               // time0 required to validate injtime.
               // If time0 is missing, set error flag but don't report it.
               // time0 has its own textfield with its own error checking that 
               // should report any problems.
               this.resetBackground();
               this.setForeground(Color.BLACK);
               this.setToolTipText("");
               return false;
            }
            
            // dont need try/catch block here
            // already tested for nulls and regex mask assures proper format
            String time = this.padTime(this.getText());
            String date = this.parms.getDate();
            String datetime = date + " " + time;
            Timestamp value = Timestamp.valueOf(datetime);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Timestamp time0 = (Timestamp)parms.get("user/time0",PumpParameters.Types.TIME);
            this.setText(time);   
               
            if ( ! value.after(time0) ) {
               String msg = "Inj Time preceeds Pre-Assay Time";
               this.setBackground(Color.RED);
               this.setForeground(Color.WHITE);
               this.setToolTipText(msg);
               if ( dialogs ) {
                  Component root = SwingUtilities.getRoot(this);
                  JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
               }
               return false;
            }
               
            if ( value.after(now) ) {
               String msg = "Inj Time hasn't happened yet";
               this.setBackground(Color.RED);
               this.setForeground(Color.WHITE);
               this.setToolTipText(msg);
               if ( dialogs ) {
                  Component root = SwingUtilities.getRoot(this);
                  JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
               }
               return false;
            }
                                                    
            this.resetBackground();
            this.setForeground(Color.BLACK);
            this.setToolTipText("");
            return true;            
         }
         public boolean editableRule () {
            return ( ! infused );
         }
      };
      tmp.setMask("^[0-9]{2}:[0-9]{2}:[0-9]{2}$","99:99:99");
      tmp.setDescription("Injection Time");
      tmp.setExternalData(parms,"user/injtime",PumpParameters.Types.STRING);
      
      return tmp;
   }

   
   private JTextField makeResTime () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void upload () {
            String time = this.padTime(this.getText());
            this.parms.put(this.key,time,this.type);
            
            // this field may be used by others -- recheck everybody
            checkFields(this.getParent(),false);
         }  
         public void download () {
            this.setText(this.parms.getString(this.key));
            this.test(false);
         }                   
         public boolean test (boolean dialogs) {
            super.test(dialogs);
            if ( ! (this.testResults == null) ) { return this.testResults; }
            
            if ( parms.get("user/injtime") == null || ((String)parms.get("user/injtime")).length() < 8) {
               // injtime required to validate injtime.
               // if injtime is missing, assume whatever user entered is bad
               String msg = "Need Injection Time to Validate Post-Assay Time";
               this.setBackground(Color.RED);
               this.setForeground(Color.WHITE);
               this.setToolTipText(msg);
               if ( dialogs ) {
                  Component root = SwingUtilities.getRoot(this);
                  JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
               }
               return true;
            }
            
            // dont need try/catch block here
            // already tested for nulls and regex mask assures proper format
            String time = this.padTime(this.getText());
            String date = this.parms.getDate();
            String datetime = date + " " + time;
            Timestamp value = Timestamp.valueOf(datetime);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Timestamp injtime = (Timestamp)parms.get("user/injtime",PumpParameters.Types.TIME);            
            this.setText(time);
            
            if ( ! value.after(injtime) ) {
               String msg = "Post-Assay Time preceeds Inj Time";
               this.setBackground(Color.RED);
               this.setForeground(Color.WHITE);
               this.setToolTipText(msg);
               if ( dialogs ) {
                  Component root = SwingUtilities.getRoot(this);
                  JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
               }
               return false;
            }
            
            if ( value.after(now) ) {
               String msg = "Post-Assay Time hasn't happened yet";
               this.setBackground(Color.RED);
               this.setForeground(Color.WHITE);
               this.setToolTipText(msg);
               if ( dialogs ) {
                  Component root = SwingUtilities.getRoot(this);
                  JOptionPane.showMessageDialog(root,msg,"Bad Parameter",JOptionPane.ERROR_MESSAGE);      
               }
               return false;
            }
                                                   
            this.resetBackground();
            this.setForeground(Color.BLACK);
            this.setToolTipText("");
            return true;            
         }
      };
      tmp.setMask("^[0-9]{2}:[0-9]{2}:[0-9]{2}$","99:99:99");
      tmp.setDescription("Post-Assay Time");
      tmp.setExternalData(parms,"user/res_time",PumpParameters.Types.STRING);
      
      return tmp;
   }
   
   
   private JTextField makeResDose () {
      
      PumpTextField tmp = new PumpTextField(8) {
         // override default method for pushing/pulling info to data array
         // user uses units of micro-curies, data stored with units of milli-curies
         // have to do unit conversion here
         public void upload () {
            String text = this.getText();
            if ( ! text.equals("") ) {
               Double value = Double.valueOf(text)/1000;
               this.parms.put(this.key,value);
            }
         }   
         public void download () { 
            String text = "";
            Double value = (Double)this.parms.get(key);
            if ( value != null ) {
               value *= 1000;
               text = value.toString();
            }
            this.setText(text);
            this.test(false);   
         }
     };
            
      tmp.setMask("^[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Residual Dose");
      tmp.setExternalData(parms,"user/res_dose",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
   
   private JTextField makeResVol () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public boolean test (boolean dialogs) {
            return true;            // override required field check
         }
      };
      tmp.setMask("^[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Residual Volume");
      tmp.setExternalData(parms,"user/res_vol",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
      
   
   private JTextField makeDosePredicted () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void download () {
            String value = String.format("%2.4f",(Double)parms.get(key));
            this.setText(value);         
            this.test(false);    
         }
         public boolean test (boolean dialogs) {
            return true;   // override required field check
         }
         public boolean editableRule () {
            return ( false );  // always read only
         }
      };
      tmp.setMask("^[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Dose Predicted");
      tmp.setExternalData(parms,"calc/dose_predicted",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
   
   
   private JTextField makeDoseActual () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void download () {
            String value = String.format("%2.4f",(Double)parms.get(key));
            this.setText(value);         
            this.test(false); 
         }
         public boolean test (boolean dialogs) {
            return true;   // override required field check
         }
         public boolean editableRule () {
            return ( false );  // always read only
         }
      };
      tmp.setMask("^[+-]{0,1}[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Actual Dose");
      tmp.setExternalData(parms,"calc/res_decay",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
   
   
   private JTextField makeDoseDiff () {
      
      PumpTextField tmp = new PumpTextField(8) {
         public void download () {
            String value = String.format("%4.4f",(Double)parms.get(key));
            this.setText(value);     
            this.test(false);    
         }
         public boolean test (boolean dialogs) {
            // no super -- override required field check
            
            if ( this.getText().equals("") ) {
               this.resetBackground();
               this.setForeground(Color.BLACK);
               this.setToolTipText("");
               return true;
            }            
            if ( Double.valueOf(this.getText()) > 10 ) {
               this.setBackground(Color.RED);
               this.setForeground(Color.WHITE);
               this.setToolTipText("Dose to Patient 10% more than desired");
               return false;
            }
            if ( Double.valueOf(this.getText()) > 5 ) {
               this.setBackground(Color.YELLOW);
               this.setForeground(Color.BLACK);
               this.setToolTipText("Dose to Patient 5% more than desired");
               return true;
            }
            
            this.resetBackground();
            this.setForeground(Color.BLACK);
            this.setToolTipText("");
            return true;            
         }
         public boolean editableRule () {
            return ( false );  // always read only
         }
      };
      tmp.setMask("^[+-]{0,1}[0-9]*\\.{0,1}[0-9]*$");
      tmp.setDescription("Dose Difference");
      tmp.setExternalData(parms,"calc/dose_diff",PumpParameters.Types.DOUBLE);
      
      return tmp;
   }
   
   
   
//    All JTextField objects have the potential to have an InputVerifier 
//    object installed which is supposed to verify the user input meets
//    certain data integrity rules.  In this code, all JTextFields have
//    set InputVerifiers and all InputVerifiers are actually subclassed
//    PumpVerify objects.  The main difference is what happens when it fails.
//    InputVerify has only one function verify().  When that fails it won't
//    allow the user to tab out of the field.  PumpVerify wraps verify() in
//    another function test().  Verify() always returns true so the user can
//    tab out.  Test() returns the real pass/fail results.  
//    
//    This method loops through every JTextField that shares the same 
//    container as the sample JComponent (JTextField,JButton,JLabel,etc).  
//    If the field has an InputVerifier set, it is recast as a PumpVerify
//    subclass and the PumpVerify.test() function is run. 
//    If any one field returns false, the whole method returns false.

   private boolean checkFields (Container parent,boolean dialog) {
      boolean results = true;
      
      // other fields may depend on the value of this field
      // when this field changes recheck all fields on this screen
//      Container parent = check.getParent();
      Component [] children = parent.getComponents();
      for ( Component child : children ) {
         if ( child instanceof PumpTextField ) {
            PumpTextField field = (PumpTextField)child;
            if ( ! field.test(dialog) ) {
               results = false;
            }
         }
      }
      return results;
   }   
   
   private void changeFont (Container parent,Font font) {
      parent.setFont(font);
      Component [] children = parent.getComponents();
      for ( Component child : children ) {
         changeFont((Container)child,font);
      }
      this.setGoFont();
   }

// tried to spin of separate thread to do text updates.  didnt work
//    try { 
//       SwingUtilities.invokeAndWait( new Runnable () {
//          public void run () {
//             ...
//          }
//      });
//    } 
//    catch ( InterruptedException e ) {} // do nothing
//    catch ( InvocationTargetException e ) {} // do nothing 



// guys on the web say JTextPanes will go faster if the documents
// are updated offline and then swapped in to the JTextPane.  Has something
// to do with all the gui updates the object would have to do if its document
// was visible when it was updated.  doesn't help me.  
// 
//     JTextPane jtp = new JTextPane();
//     Document doc = jtp.getDocument();
//     Document blank = new DefaultStyledDocument();
//     jtp.setDocument(blank);
// 
//     for (... iteration over large chunk of parsed text ...) {
//         ...
//         doc.insertString(offset, partOfText, attrsForPartOfText);
//         ...
//     }
//     jtp.setDocument(doc);   private boolean testConnection () {
      

   private boolean testConnection () {
      
      String ip = (String)parms.get("user/ip");
      int port = (Integer)parms.get("user/port");
      
      Font oldfont = this.connectPane.getFont();
      int oldsize = oldfont.getSize();
      final SimpleAttributeSet header = new SimpleAttributeSet();
      StyleConstants.setForeground(header,Color.BLACK);
      StyleConstants.setBold(header,true);
      StyleConstants.setFontSize(header,oldsize*1);
      final SimpleAttributeSet success = new SimpleAttributeSet();
      StyleConstants.setForeground(success,Color.GREEN);
      StyleConstants.setBold(success,true);
      StyleConstants.setFontSize(success,oldsize*4);
      final SimpleAttributeSet failed = new SimpleAttributeSet();
      StyleConstants.setForeground(failed,Color.RED);
      StyleConstants.setBold(failed,true);
      StyleConstants.setFontSize(failed,oldsize*4);
    
      try {
         System.out.println("Info...");
         this.append(this.connectPane,"Info...\n",header);   
         this.append(this.connectPane,"Time: "+(new Timestamp(System.currentTimeMillis())).toString()+"\n",null);
         this.append(this.connectPane,"Room: "+this.parms.getString("user/room")+"\n",null);   
         this.append(this.connectPane,"Line: "+this.parms.getString("user/line")+"\n",null);   
         this.append(this.connectPane,"IP: "+this.parms.getString("user/ip")+"\n",null);   
         this.append(this.connectPane,"Port: "+this.parms.getString("user/port")+"\n",null);   
         this.append(this.connectPane,"\n",null);   
         this.cmds.Ping(ip);
         
         System.out.println("Pinging...");
         this.append(this.connectPane,"Pinging...\n",header);   
         this.append(this.connectPane,"\n",null);  
//         this.connectPane.updateUI();
//         this.connectPane.repaint(); 
         // text isn't updated immediately -- usually not until an exception or the program quits
         // JComponent.repaint() doesn't help.  It should redraw the component
         // but maybe the request doesn't float to the top of the list very quickly.
         // revalidate,invalidate,validate work when changes are made to JProgressBar
         // changes but they don't work here.  The difference is that the JProgressBar
         // applications change the components size and it needs to be laid out again.
         // Adding text to the JTextPane don't change the layout so there's nothing for
         // revalidate to do.  
//          this.connectPane.setPreferredSize(new Dimension(10,10));    
//          ((JComponent)this.connectPane.getParent()).revalidate();
         this.cmds.Ping(ip);
//          try { Thread.sleep(5000); } catch ( Exception e ) {}
         
         System.out.println("Connecting...");
         this.append(this.connectPane,"Connecting...\n",header);                  
         this.append(this.connectPane,"\n",null);   
         this.cmds.Connect(ip,port);

         System.out.println("Testing Connection...");
         this.append(this.connectPane,"Testing Connection...\n",header);                           
         this.append(this.connectPane,"\n",null);   
         this.cmds.TestConnection();
         
         System.out.println("Testing Protocol 44...");
         this.append(this.connectPane,"Testing Protocol 44...\n",header);                           
         this.append(this.connectPane,"\n",null);   
         this.cmds.TestProtocol();
         
         System.out.println("Testing Programable Mode...");
         this.append(this.connectPane,"Testing Programmable Mode...\n",header);                           
         this.append(this.connectPane,"\n",null);   
         this.cmds.TestProgramable();
         
         System.out.println("Running Pump...");
         System.out.println("15 cc/min for 30 sec");
         this.append(this.connectPane,"Running Pump...\n",header);                           
         this.append(this.connectPane,"15 cc/min for 30 sec\n",null);                           
         cmds.LoadPgm((Double)parms.get("config/diam"),15.0,7.5,0.0,0.0,0.0,0.0);
         cmds.Run();
         
         
      } catch ( PumpException e ) {
         this.append(this.connectPane,e.getMessage()+"\n",null);
         this.append(this.connectPane,"FAILED\n\n",failed);   
         try { this.cmds.Close(); } catch (PumpException e2) { e2.printStackTrace(); }
         return false;
      }
      
      this.append(this.connectPane,"SUCCESS\n\n",success);                           
      return true;
   
   }


   private void append (JTextPane t,String s,AttributeSet a) {
      Document doc = t.getDocument();  // actually its a DefaultStyledDocument
      int length = doc.getLength();
      
      try { doc.insertString(length,s,a); }
      catch ( BadLocationException e ) {} // never happen 
      
//       length = doc.getLength();
//       System.out.println(length);
//       try { 
//          System.out.println(doc.getText(0,length));
//       } catch ( BadLocationException e ) {}
//       
//       if ( doc instanceof Document ) { System.out.println("Document"); }
//       if ( doc instanceof StyledDocument ) { System.out.println("StyledDocument"); }
//       if ( doc instanceof AbstractDocument ) { System.out.println("AbstractDocument"); }
//       if ( doc instanceof DefaultStyledDocument ) { System.out.println("DefaultStyledDocument"); }
//       if ( doc instanceof PlainDocument ) { System.out.println("PlainDocument"); }
   }
   

// timestamp taken, then volume read, then rate and status read.
// it may take up to 250ms to query the pump and get a response. 
// the reported volume may be 250ms newer and the rate 500ms newer than
// the associated timestamp.  Calculate theoretical volumes and rates
// at the timestamp and at the timestamp + 1 second.  Pump values should
// fall somewhere between these min and max values.  If not throw an error.
//
// 2006-04-18 cf Added color signals to the points sent to the volume and rate
//               plots.  Red if rate=0.  Green if everything is normal.
   public void pumpInfo (Map map) {
   
      if ( this.logopen ) {
         this.log.println(map.get("datetime")+","+map.get("rate")+","+map.get("vol")+","+map.get("status"));
         this.log.flush();
      }
      
      
      // progress bars work on integer numbers only
         Timestamp injtime = (Timestamp)this.parms.get("user/injtime",PumpParameters.Types.TIME);
      Timestamp sampletime = (Timestamp)map.get("datetime");
               double rate =    (Double)map.get("rate");
                double vol =    (Double)map.get("vol");
               char status = (Character)map.get("status");
      
      double runtime = sampletime.getTime() - injtime.getTime();
      runtime /= 1000;  // convert msec to sec;
      runtime /= 60;  // convert secs to mins
            
      int progressTime = (int)(runtime * 60);   // progress bars take integer seconds
      this.dsBar.setValue(progressTime);
      this.bolusBar.setValue(progressTime);
      this.ssBar.setValue(progressTime);
      
      Color color = Color.GREEN;
      if ( status != '>' ) { rate = 0; color = Color.RED; }
      if ( status == '?' ) { rate = -99; }
      this.ratePanel.addData(new Point2D.Double(runtime,rate),color);
      this.volumePanel.addData(new Point2D.Double(runtime,vol),color);
            
      // skip error checks only if it appears pump is still chewing on previous error
      if ( ! this.monitor && status != '>' ) {
         // appears to still be broken from old error
         return;
      }
      this.monitor = true;
      
     // cant talk to pump
      if ( status == '?' ) {
         String msg;
         msg = "Can't Talk To Pump\n";
         msg += "\n";
         msg += "The pump may have been turned off.  A wire may have fallen\n";
         msg += "loose.  The terminal server may be having trouble.  There may\n";
         msg += "be trouble with the network.\n";
         msg += "\n";
         msg += "So long as the pump is still running, it can continue the infusion\n";
         msg += "as normal without any computer interaction.  On screen monitoring\n";
         msg += "won't be possible though so you'll need to watch the pump to be sure\n";
         msg += "it changes speed and stops at the appropriate times.\n";
         msg += "\n";
         this.error = true;
         this.monitor = false;
         this.status.setText("Error");
         this.post.alert(msg);
         return;
      }
         
      // pump not running during infusion
      // pump may finish a few tenths of a second early. 
      // allow a few seconds leeway.
      if ( runtime < (Double)this.parms.get("infuse/ss/time") - (2.0/60)) {
         if ( status != '>' ) {
            String msg;
            msg = "Pump Not Running\n";
            msg += "\n";
            msg += "Someone may have pressed the stop button on the pump.\n";
            msg += "The infusion is actually paused, not stopped.  The infusion \n";
            msg += "profile is still in the pumps memory.  Pressing run/stop again\n";
            msg += "may resume the infusion.\n";
            msg += "\n";
            msg += "The pump may have stopped because the pusher block is physically \n";
            msg += "obstructed.  Again, the profile is still in the pump's memory\n";
            msg += "Remove the obstruction and hit the pump's run/stop button.\n";
            msg += "\n";
            msg += "The pump may have stopped due to some internal problem.\n";
            msg += "The pump is likely broken and the infusion profile is \n";
            msg += "certainly lost.  Swap in a new pump and key the old infusion\n";
            msg += "profile into the new pump manually adjusting for whatever part\n";
            msg += "of the infusion was accomplished with the old pump\n";
            msg += "\n";      
            this.error = true;
            this.monitor = false;          
            this.status.setText("Error");
            this.post.alert(msg);
            return;
         }
      }
      
      // pump still running after infusion
      // end infusion time is padded a little bit because there is some
      // descrepancy between sending the start command to the pump and 
      // actually having it act on that command.  There is also some deadtime
      // as the pump switches speeds between phases.  Alltogether, it shouldn't
      // add up to more than a second or two.  
      // skip this check if there was an error. techs may have corrected the error
      // and continued but the time they took to fix things would wrecks the 
      // anticipated stop time.  so this check becomes worthless
      if ( ! this.error ) {
         if ( runtime > (Double)this.parms.get("infuse/ss/time") + (2.0/60.0)) {
            if ( status == '>' ) {
               String msg;
               msg = "Pump Still Running\n";
               msg += "\n";
               msg += "The infusion should be over by now but the pump is still running.\n";
               msg += "Shut the pump down manually ---NOW---.  Use the stop button on \n";
               msg += "on the front panel of the pump itself.\n";
               msg += "\n";
               this.error = true;
               this.monitor = false;
               this.status.setText("Error");
               this.post.alert(msg);
               return;
            }
         }
      }
      
      Double maxVol = (Double)this.parms.get("infuse/ds/vol")+(Double)this.parms.get("infuse/bolus/vol")+(Double)this.parms.get("infuse/ss/vol");
      if ( vol > maxVol*1.01 ) {
         if ( status == '>' ) {
            String msg;
            msg = "Pump Still Running\n";
            msg += "\n";
            msg += "The pump reports it has infused the full volume but it is still running.\n";
            msg += "Shut the pump down manually ---NOW---.  Use the stop button on \n";
            msg += "on the front panel of the pump itself.\n"; 
            this.error = true;
            this.monitor = false;
            this.status.setText("Error");
            this.post.alert(msg);
            return;
         }
      }
      
      // pump stopped normally as planned
      // stop monitor thread
      if ( ! this.error ) {
         if ( runtime > (Double)this.parms.get("infuse/ss/time") + (2.0/60.0)) {
            if ( status == ':' ) {
               this.pumpMonitor.stop();
               this.status.setText("Done");
            }
         }
      }
      
//       // pump running at wrong speed
//       if ( ! (rate == minRate || rate == maxRate) ) {
//          String msg;
//          msg = "Pump Running at Wrong Rate\n";
//          msg += "\n";
//          msg += "The pump may be off profile\n";
//          msg += "You can continue as-is or you can abort the infusion\n";
//          msg += "\n";         
//          this.monitor = false;
//          this.post.alert(msg);
//       }         
//       
//       // total volume reported by pump not correct
//       if ( vol < minVol || vol > maxVol ) {
//          String msg;
//          msg = "Pump Volume Counters don't Match Expectations\n";
//          msg += "\n";
//          msg += "The pump may be off profile\n";
//          msg += "You can continue as-is or you can abort the infusion\n";
//          msg += "\n";
//          this.monitor = false;
//          this.post.alert(msg);
//       }
         
   }
   

   public void countdownInfo (int time) {
      this.setGoValue(time/1000);   
   }
   
   
   public void setGoValue (int x) {
            
      x = Math.max(-3599,x);
      x = Math.min(3599,x);
      this.goBar.setValue(x);
            
      String sign = ( x>0 ) ? "-" : "+";
      int mins = (int)Math.floor(Math.abs(x/60));   // floor rounds to negative infinity --not to zero
      int secs = (int)(Math.abs(x) - mins*60);            
      this.goLabel.setText(String.format("%s%02d:%02d",sign,mins,secs));
   }
   
   
   // countdown label font always has to be bigger than normal
   // this method should be run when JLabel first created and 
   // whenever global change font tools are run.  
   public void setGoFont () {
      Font parentFont = this.goLabel.getParent().getFont();
      double newSize = parentFont.getSize2D() * 2.0;
      Font newFont = parentFont.deriveFont(Font.BOLD,(float)newSize);
      this.goLabel.setFont(newFont);
   }
      
   
   public void displayInfusionProfile () {
   
         dsRateLabel.setText(String.format("%2.4f",(Double)parms.get("infuse/ds/rate")));
          dsVolLabel.setText(String.format("%2.4f",(Double)parms.get("infuse/ds/vol")));
          dsDurLabel.setText(String.format("%4.2f",(Double)parms.get("infuse/ds/dur")*60));
      bolusRateLabel.setText(String.format("%2.4f",(Double)parms.get("infuse/bolus/rate")));
       bolusVolLabel.setText(String.format("%2.4f",(Double)parms.get("infuse/bolus/vol")));
       bolusDurLabel.setText(String.format("%4.2f",(Double)parms.get("infuse/bolus/dur")*60));
         ssRateLabel.setText(String.format("%2.4f",(Double)parms.get("infuse/ss/rate")));
          ssVolLabel.setText(String.format("%2.4f",(Double)parms.get("infuse/ss/vol")));
          ssDurLabel.setText(String.format("%4.2f",(Double)parms.get("infuse/ss/dur")));          
         resVolLabel.setText(String.format("%2.4f",(Double)parms.get("user/vtot") - (Double)parms.get("infuse/ds/vol") - (Double)parms.get("infuse/bolus/vol") - (Double)parms.get("infuse/ss/vol") ));

      Double dsDur  = (Double)parms.get("infuse/ds/dur")    * 60;
      Double bolDur = (Double)parms.get("infuse/bolus/dur") * 60;
      Double ssDur  = (Double)parms.get("infuse/ss/dur")    * 60;

      double dsTime  = dsDur;
      double bolTime = dsTime + bolDur;
      double ssTime  = bolTime + ssDur;
      double longestDur = Math.max(bolDur,ssDur);
      longestDur = Math.min(600,longestDur);  // for display, max 10 min
      
      dsBar.setRange(0,(int)dsTime);
      bolusBar.setRange((int)dsTime,(int)bolTime);
      ssBar.setRange((int)bolTime,(int)ssTime);
      
      dsBar.setRatio(1,0);
      bolusBar.setRatio(1,0);
      ssBar.setRatio(1,0);

   }
   
   
   
//    2006-04-18 cf Added some commands for safety.  The infusion profile currently
//                  residing in the pump is cleared so it cant accidentally be used
//                  on a future manual infusion.  Cleared the pump rate and reset
//                  the diameter to 40mm for the same reason.  Set the mode back
//                  to "PUMP" at users request.   
   public void stopPump () {      
      try { 
         this.cmds.Stop(false);   // make sure pump is stopped.
         this.status.setText("Stopped");
         Map<String,Object> boo1 = this.cmds.ClearPgm();  // clear program so its not accidentally used later
         Map<String,Object> boo2 = this.cmds.SetMode("PUMP");  // set pump back to pump mode for techs comfort
         Map<String,Object> boo3 = this.cmds.SetDiam(40.0,false);
      } catch (NullPointerException e) {
         // never was a pump socket connection 
         // failure of the pump stop command is therefore irrelevant
      } catch (PumpException e ) {
         String msg="";
         msg += "Received error in response to reset pump command.\n";
         msg += "\n";
         msg += "Most likely explaination is that the pump has been\n";
         msg += "turned off or disconnected from the network.\n";
         msg += "\n";
         msg += "Visually verify pump is actually stopped.\n";
         msg += "\n";
         this.post.alert(msg,true);  // modal message to keep it from shutting down before user acknowledges message
      }
   }

         
   public void stopThreads () {
      if ( this.countdown != null )   { this.countdown.stop(); }
      if ( this.pumpMonitor != null ) { this.pumpMonitor.stop(); }      
   }
      

//    cntl-c
//    cntl-c while sitting on a text field that will give errors
//    cntl-c while pump is running
//    cntl-c while pump is running and there is a pump monitor error window active
//    kill -15 but not kill -9
//    kill -15 while sitting on a text field that will give errors
//    kill -15 while pump is running
//    kill -15 while pump is running and there is a pump monitor error window active
//    exit button
//    exit button while sitting on a text field that will give errors
//    exit button while pump is running
//    exit button while pump is running and there is a pump monitor error window active (failed so long as original error window was visible)
//    menu exit
//    menu exit while sitting on a text field that will give errors (failed.  main window closed,error window appeared,java hung. cntl-c required)
//    menu exit while pump is running
//    menu exit while pump is running and there is a pump monitor error window active (failed so long as original error window was visible)
//    os window close
//    os window close while sitting on a text field that will give errors
//    os window close while pump is running
//    os window close while pump is running and there is a pump monitor error window active (failed)


// There is a passage in the java documentation on Runtime.addShutdownHook()...
//    Shutdown hooks should also finish their work quickly. When a program 
//    invokes exit the expectation is that the virtual machine will promptly 
//    shut down and exit. When the virtual machine is terminated due to user 
//    logoff or system shutdown the underlying operating system may only 
//    allow a fixed amount of time in which to shut down and exit. It is 
//    therefore inadvisable to attempt any user interaction or to perform a 
//    long-running computation in a shutdown hook.
// Not surprising then that modal dialog windows waiting for user response are
// giving the shutdown() method problems.  Need to figure a way to force these
// dialogs to close without user input or just accept the consequences.

   public void shutdown (int status) { 
   
      System.out.println("shutdown");  
      
      System.out.println("stopping pump");
      this.stopPump();
      System.out.println("stopping threads");
      this.stopThreads();
      
      System.out.println("closing log file");
      if ( this.logopen ) {
         this.log.close();
      }
      
      System.out.println("saving xml");
      this.parms.saveXML();      
      
      //System.out.println("disposing");
      //this.frame.dispose();      
//       System.out.println("exiting");
//       System.exit(0);
   }   
      
      
      
}


// =====================================================
// still to do
// =====================================================
//
//
// testing
//
// logging ???
//
// does the System.getenv("DISPLAY") work on pc machines
//
// throw error from PumpParameters calc functions if any required fields are null 
//
// when changing tabs when cursor is on a bad textfield, the error dialog pops
// up after the new tab appears.  be nice if dialog would pop on the current tab
// and maybe keep user in that tab until error is fixed.
// 
// on test connection, textfield box does not update immediately.  Like to be able
// to see what step its currently on.
// reconnect to pump (if possible) while infusion is running
//
// padTime used when fields writing to parms array.  It really should be used
// when time values (whether being pulled from the parms array or the text fields)
// are used to build Timestamp variables.  That way the time values in 
// the parms array and the time values in the text fields agree.  Maybe should
// tweak parms.get/parms.getString.  If a variable type is supplied and if it 
// is "TIME" then it could build the Timestring variable.  Date would be stored
// internally initialized when the object is instantiated.  Time would come
// from the array but get passed through the padTime function first.  
//
// when the cursor leaves a time field with an incomplete time, java has trouble
// building the corresponding timestamp variable.  errors are thrown.  maybe
// time fields should get auto-completed with zeroes when left incomplete.
// cant just leave them half finished or cant write to parms array.  have to
// have them in the parms array or parms doesn't match gui.  
//
//
// 
// save xml on infuse
// 
// save xml on print
// 
// save xml on exit
// 
// clean up font tools
// 
//
// 


// done
//
// printing
//
// load specified xml
//
// room and line selection
//
// on successful connection test disable test button and the room and line switches
//
// time to inject progress bar
//
// need warning boxes when pump monitoring looks strange - bad rates,bad volume,
// pump not running, etc.  dropped monitoring on rates and volume.  the pump
// has some dead time as it switches speeds between infusion phases.  without
// modeling that somehow, its not possible to get a reasonable number to compare
// to what the pump reports for rates and volumes.  
//
// User tabs into the infusion screen.  At the time there are no warnings
// for mass or volume limits.  They then take 10 to 15 minutes to set up the
// room and get the patient settled.  During that time the dose has decayed
// down to the point where it will trigger one or both warnings but they won't
// see that until they hit the "infuse" button.  Shielah says they should be
// warned at that point and given the opportunity to abort.  Trouble is that
// the time they take to read the message, figure out what it means, consult
// with others and then respond, means the dose has decayed even further 
// requiring yet another recalculation of the infusion profile which may show
// the dose is in even worse shape.  What should be done here?
// --Margaret says all the volume/mass warnings get should get rolled up in
// the time warning.  "to get the full dose you must inject in the next 2 minutes
// and 35 seconds."  As long as the time is accurate and as long as the profile
// is recalculated with all the mass and volume limits in mind when the user
// hits the inject button, everything is fine.  
// --Steve suggested including any mass/volume warnings on the final report.
// That way if they didn't get the full dose its very clear why.  
//
// pump doesn't like skipping bolus phase.  it comes back with a syntax
// error on the first line of sequence 3.  
//
// gui shutdown should stop pump and kill all threads
//
// java had been a little unstable on shutdowns.  sometimes it would hang.  
// changed default behavior to DISPOSE_ON_CLOSE and upgraded from java version
// 1.5.0.01 b08 to 1.5.0.06 b05.  

