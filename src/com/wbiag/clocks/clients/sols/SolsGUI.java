package com.wbiag.clocks.clients.sols;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

public class SolsGUI extends JPanel implements ActionListener {
    JCheckBox cBox1;
    JTable statusTable;
    JLabel statusLabel;
    AbstractTableModel tModel;
    Object [] readers;
    
    AutoUpdateThread auThread;

    class AutoUpdateThread extends Thread {
        AutoUpdateThread (){
            super();
        }
        public void run() {
            
            try{
                long timer, timerBallance;
                timer = System.currentTimeMillis();
                timerBallance = 0;
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                
                while (true) {
                tModel.fireTableDataChanged();
                
                int totalRecLeft =0;
                for(int i=0;i < statusTable.getRowCount();i++){
                    totalRecLeft = totalRecLeft + ((Integer)statusTable.getValueAt(i, 3)).intValue();
                }
                
                if (totalRecLeft>0){
                    if (SynelReader.active){
                        timerBallance = timerBallance - timer + System.currentTimeMillis();
                    }
                    statusLabel.setText("   Time elapsed: "+(timerBallance/(1000*60*60*60))%24 + ":" + format.format(new Date(timerBallance)) + "   Records left: " + totalRecLeft);
                } else {
                    cBox1.setSelected(SynelReader.active = false);
                    statusLabel.setText("   Time elapsed: "+(timerBallance/(1000*60*60*60))%24 + ":" + format.format(new Date(timerBallance)) + "   Finished.");
                }
                
                timer = System.currentTimeMillis();
                sleep(999);
                }
                
            }catch (InterruptedException ie){
                    return;  
            }
            
 
        }
    }
    
    public SolsGUI(){
        super(new BorderLayout());


        cBox1 = new JCheckBox("Active",SynelReader.active);
        
        statusLabel = new JLabel(" ");
        JPanel topPane = new JPanel(new BorderLayout());
        
        topPane.add(cBox1,BorderLayout.WEST);
        topPane.add(statusLabel);
        
        add(topPane,BorderLayout.NORTH);
        
        readers = SynelOfflineLoadSimulator.readerList.keySet().toArray();
        Arrays.sort(readers);

        tModel = new AbstractTableModel(){
            String [] columnNames ={"Reader Name","IP","Swipes","Records","Shadow","Interactive"};
            public String getColumnName(int col) {
                return columnNames[col].toString();
            }
            public int getRowCount() { return readers.length;}
            public int getColumnCount() { return columnNames.length; }
            public Object getValueAt(int row, int col) {
                Object value;
                SynelReader r = (SynelReader) SynelOfflineLoadSimulator.readerList.get(readers[row]);
                switch (col){
                case 0:
                    value = r.readerName;
                    break;
                case 1:
                    value = r.ipAddress;
                    break;
                case 2:
                    value = new Integer(r.queueCounter());
                    break;
                case 3:
                    value = new Integer(r.qBuffer.size());
                    break;
                case 4:
                    value = new Boolean(r.shadowThread.isAlive());
                    break;
                case 5:
                    value = new Boolean(r.interactiveThread.isAlive());
                    break;
                default: value = null;    
                }
                return value;
            }
            public Class getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }

        };
        
        statusTable = new JTable(tModel);
        
        JScrollPane scrollPane = new JScrollPane(statusTable);
        add(scrollPane,BorderLayout.CENTER);
        
        cBox1.addActionListener(this);
        
        auThread = new AutoUpdateThread();
        auThread.start();
    }
    
    public static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Workbrain SY-780 offline load simulator "+SynelOfflineLoadSimulator.VERSION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new SolsGUI();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
        
    }

    
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        if (e.getSource().equals(cBox1)){
            SynelReader.active=cBox1.isSelected();
        } 

    }

}
