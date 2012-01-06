package kinect.viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import javax.swing.JPanel;
import kinect.storage.KinectStorageProtos.Sequence;
import kinect.storage.KinectStorageProtos.Frame;
import kinect.storage.KinectStorageProtos.Joint;
import kinect.storage.KinectStorageProtos.Joint.JointType;
import kinect.storage.KinectStorageProtos.Point3D;
import kinect.storage.KinectStorageProtos.Skeleton;

/**
 *
 * @author samo
 */
public class SkeletonProjective extends JPanel {

    BufferedImage bufImg;
    Dimension size;
    Sequence sequence;
    Graphics2D g2;
    HashMap<JointType, Joint> jointHash;
    Frame currentFrame;
    int maxFrame;
    int idFrame;
    
    public SkeletonProjective() {
        size = new Dimension(640,480);  
        sequence = null; 
    }
    
    public SkeletonProjective(Sequence sequence) {
        size = new Dimension(640,480);        
        this.sequence = sequence;
        
        initSkeletonProjective();
        drawImage();
    }

    private void initSkeletonProjective() {
        jointHash = new HashMap<>();
        
        idFrame = 0;
        maxFrame = sequence.getFrameCount();
        
        currentFrame = sequence.getFrame(idFrame);               
        createHash(currentFrame.getSkeleton());
    }
    
    private void createHash(Skeleton skeleton) {
        jointHash.clear();
        for(Joint joint:skeleton.getJointsList()) {
            jointHash.put(joint.getType(), joint);
        }
    }
    
    /*
     * Drawing - projective 
     */
    private void drawLine(Graphics2D g, JointType type1, JointType type2) {
        if(!jointHash.containsKey(type1) || !jointHash.containsKey(type2)) { return; }
                
        Joint joint1 = jointHash.get(type1);
        Joint joint2 = jointHash.get(type2);
        
        
        Point3D pos1 = joint1.getProjective();
        Point3D pos2 = joint2.getProjective();

        if (joint1.getConfidence() < 0.1 || joint2.getConfidence() < 0.1) {
            return;
        }

        g.drawLine((int) pos1.getX(), (int) pos1.getY(), (int) pos2.getX(), (int) pos2.getY());
    }

    private void drawJoint(Graphics2D g, JointType type) {
        if(!jointHash.containsKey(type)) { return; }
        
        Joint joint = jointHash.get(type);
        Point3D pos = joint.getProjective();

        // TODO: set color - green conf > 0.5, yellow 0 < x < 0.5, red ==0 !
        if (joint.getConfidence() > 0.5) {
            g.setColor(Color.green);
            //g.fillOval((int) pos.getX() - 3, (int) pos.getY() - 3, 5, 5);
            g.fillOval((int) pos.getX() - 4, (int) pos.getY() - 4, 7, 7);
            g.setColor(Color.yellow);
            return;
        }
        if (joint.getConfidence() > 0.1) {
            g.setColor(Color.orange);
            //g.fillOval((int) pos.getX() - 3, (int) pos.getY() - 3, 5, 5);
            g.fillOval((int) pos.getX() - 4, (int) pos.getY() - 4, 7, 7);
            g.setColor(Color.yellow);
            return;
        }
        // TODO: check borders!
        g.setColor(Color.red);
        //g.fillOval((int) pos.getX() - 3, (int) pos.getY() - 3, 5, 5);
        g.fillOval((int) pos.getX() - 4, (int) pos.getY() - 4, 7, 7);
        g.setColor(Color.yellow);
    }

    private void drawFrameID(Graphics2D g) {
        g.drawString("Frame "+Integer.toString(currentFrame.getNumber()), 550, 30);
    }
    
    private void drawSkeleton(Graphics2D g, Skeleton skeleton) {
        g.setColor(Color.yellow);
        
        drawLine(g, JointType.HEAD, JointType.NECK);

        drawLine(g, JointType.LEFT_SHOULDER, JointType.TORSO);
        drawLine(g, JointType.RIGHT_SHOULDER, JointType.TORSO);

        drawLine(g, JointType.NECK, JointType.LEFT_SHOULDER);
        drawLine(g, JointType.LEFT_SHOULDER, JointType.LEFT_ELBOW);
        drawLine(g, JointType.LEFT_ELBOW, JointType.LEFT_HAND);

        drawLine(g, JointType.NECK, JointType.RIGHT_SHOULDER);
        drawLine(g, JointType.RIGHT_SHOULDER, JointType.RIGHT_ELBOW);
        drawLine(g, JointType.RIGHT_ELBOW, JointType.RIGHT_HAND);

        drawLine(g, JointType.LEFT_HIP, JointType.TORSO);
        drawLine(g, JointType.RIGHT_HIP, JointType.TORSO);
        drawLine(g, JointType.LEFT_HIP, JointType.RIGHT_HIP);

        drawLine(g, JointType.LEFT_HIP, JointType.LEFT_KNEE);
        drawLine(g, JointType.LEFT_KNEE, JointType.LEFT_FOOT);

        drawLine(g, JointType.RIGHT_HIP, JointType.RIGHT_KNEE);
        drawLine(g, JointType.RIGHT_KNEE, JointType.RIGHT_FOOT);

        // Draw Joints
        drawJoint(g, JointType.HEAD);
        drawJoint(g, JointType.NECK);
        drawJoint(g, JointType.TORSO);

        drawJoint(g, JointType.LEFT_SHOULDER);
        drawJoint(g, JointType.LEFT_ELBOW);
        drawJoint(g, JointType.LEFT_HAND);

        drawJoint(g, JointType.RIGHT_SHOULDER);
        drawJoint(g, JointType.RIGHT_ELBOW);
        drawJoint(g, JointType.RIGHT_HAND);

        drawJoint(g, JointType.LEFT_HIP);
        drawJoint(g, JointType.LEFT_KNEE);
        drawJoint(g, JointType.LEFT_FOOT);

        drawJoint(g, JointType.RIGHT_HIP);
        drawJoint(g, JointType.RIGHT_KNEE);
        drawJoint(g, JointType.RIGHT_FOOT);
    }
    
    private void drawImage() {
        bufImg = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        g2 = bufImg.createGraphics();
        g2.setBackground(Color.black);
        g2.setColor(Color.yellow);
        drawSkeleton(g2,currentFrame.getSkeleton());
        drawFrameID(g2);
    }
    
    public int nextFrameIndex() {
        if(idFrame < maxFrame-1) {
            currentFrame = sequence.getFrame(++idFrame);               
            createHash(currentFrame.getSkeleton());
            drawImage();
        }
        return currentFrame.getNumber();
    }
    
    public int previousFrameIndex() {
        if(idFrame > 0) {
            currentFrame = sequence.getFrame(--idFrame);               
            createHash(currentFrame.getSkeleton());            
            drawImage();
        }
        return currentFrame.getNumber();
    }
    
    public int firstFrameIndex() {
        currentFrame = sequence.getFrame(idFrame);
        createHash(currentFrame.getSkeleton());            
        drawImage();    
        return currentFrame.getNumber();
    }
    
    public void selectFrame(int id) {
        if(id >= 0 && id <= maxFrame) {
            idFrame = id;
            currentFrame = sequence.getFrame(idFrame);               
            createHash(currentFrame.getSkeleton());            
            drawImage();
        }
    }
    
    public int selectFrame(Frame f) {
        currentFrame = f;               
        idFrame = f.getNumber();
        createHash(currentFrame.getSkeleton());  
        drawImage();
        return currentFrame.getNumber();
    }
    
    public int getFrameCount() {
        return sequence.getFrameCount();
    }
    
    public int getCurrentFrameID() {
        return idFrame;
    }
    
    public Frame getCurrentFrame() {
        return currentFrame;
    }
    
    public Frame getFrame(int id) {
        return sequence.getFrame(id);
    }
    
    public void setNewSequence(Sequence sequence) {    
        this.sequence = sequence;
        initSkeletonProjective();
        drawImage();
    }
    
    public BufferedImage getSkeletonImage() {
        return bufImg;
    }
    
    protected void paintComponent(Graphics g) {
        g.drawImage(bufImg, 0, 0, this);
    }
    
    public Dimension getPreferredSize() { return size; }
}
