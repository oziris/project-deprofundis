package kinect.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author samo
 */
public class KinectPreferences {
    
    private Properties properties;
    
    public KinectPreferences() {
        properties = new Properties();
    }
        
    public void reload() {
        FileInputStream in = null;
        try {
            in = new FileInputStream("kinectPreferences");
            properties.load(in);
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void save() {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream("kinectPreferences");
            properties.store(out, "---No Comment---");
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void load(File f) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            properties.load(in);
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void load() {
        FileInputStream in = null;
        try {
            in = new FileInputStream("kinectPreferences");
            properties.load(in);
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void initPrefFile() {
        FileInputStream in = null;
        try {
            in = new FileInputStream("kinectPreferences");
            properties.load(in);
            in.close();
        } catch (IOException ex) {
            if(ex instanceof FileNotFoundException) {
                properties.setProperty("REC", "/home/user/kinect/recordings");
                properties.setProperty("ONI", "/home/user/kinect/oni");
                properties.setProperty("DATA", "/home/user/kinect/Data");
                properties.setProperty("ViewerWindowPositionX", "50");
                properties.setProperty("ViewerWindowPositionY", "50");
                properties.setProperty("RecorderWindowPositionX", "50");
                properties.setProperty("RecorderWindowPositionY", "50");
                save();
            } else {
                ex.printStackTrace();
            }
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public Object setProperty(String key, String value) {
        return properties.setProperty(key, value);
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * 
     * @param args 
     */
    public static void main(String[] args) {
        KinectPreferences kp = new KinectPreferences();
        kp.load();
    }
}