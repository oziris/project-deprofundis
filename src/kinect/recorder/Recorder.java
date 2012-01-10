package kinect.recorder;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class Recorder {

    public UserTracker userTracker;
    private boolean shouldRun = true;
    private JFrame frame;

    public Recorder(JFrame frame) {
        this.frame = frame;
        frame.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent arg0) {
            }

            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent arg0) {
                if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    shouldRun = false;
                }
            }
        });
    }

    public static void main(String args[]) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName()); 
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(RecorderUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
                
        JFrame f = new JFrame("OpenNI User Tracker");
        f.setResizable(false);
        
        f.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        
        Recorder app = new Recorder(f);

        app.userTracker = new UserTracker();        
        f.add("Center", app.userTracker);
        f.pack();
        f.setVisible(true);
        RecorderUI rui = new RecorderUI(app.userTracker);
        rui.pack();
        rui.setLocation(680,50);
        rui.setVisible(true);
        app.run();
    }

    void run() {
        while (shouldRun) {
            userTracker.updateDepth();
            userTracker.repaint();
        }
        frame.dispose();
    }
}
