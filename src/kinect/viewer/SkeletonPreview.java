package kinect.viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import kinect.storage.KinectStorageProtos.Frame;
import kinect.storage.KinectStorageProtos.Joint;
import kinect.storage.KinectStorageProtos.Joint.JointType;
import kinect.storage.KinectStorageProtos.Point3D;
import kinect.storage.KinectStorageProtos.Skeleton;

public class SkeletonPreview extends JPanel {
    static SkeletonPreview selectedPreview = new SkeletonPreview();
    
    Border emptyBorder  = BorderFactory.createEmptyBorder(1,1,1,1);
    Border overBorder = BorderFactory.createLineBorder(Color.orange, 2);
    Border selectBorder = BorderFactory.createLineBorder(Color.blue, 2);
    
    BufferedImage bufImg;
    Dimension size;
    Graphics2D g2;
    HashMap<JointType, Joint> jointHash;
    Frame frame;
    int idFrame;
    double kX, kY;
    boolean selected;
    SkeletonPreview myself;
    Viewer viewer;
    
    public SkeletonPreview() {
        this.selected = false;
    }
    
    public SkeletonPreview(Viewer viewer, Frame frame) {
        this.viewer = viewer;
        size = new Dimension(640,480);   
        this.setSize(size);
        this.kX = 1.0;
        this.kY = 1.0;
        this.selected = false;
        
        this.frame = frame;
        initSkeletonPreview();
        drawImage();
    }

    public SkeletonPreview(Viewer v, Frame f, int width, int height) {
        this.viewer = v;
        size = new Dimension(width, height);   
        this.setSize(size);
        this.kX = 1.0*640 / width;
        this.kY = 1.0*480 / height;
        this.selected = false;
        
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if(!selected) {
                    // Set
                    ((JPanel) me.getSource()).setBorder(selectBorder);
                    viewer.setSkeletonProjectiveAndRepaint(frame);
                    selected = true;
                    // Reset
                    SkeletonPreview.selectedPreview.setBorder(emptyBorder);
                    SkeletonPreview.selectedPreview.selected = false;
                    SkeletonPreview.selectedPreview = (SkeletonPreview) me.getSource();
                }
                // System.out.println(me);
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent me) {
                ((JPanel) me.getSource()).setBorder(overBorder);
                //System.out.println(me); 
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent me) {
                if(selected) {
                    ((JPanel) me.getSource()).setBorder(selectBorder);
                    //System.out.println(me); 
                } else {
                    //((JPanel) me.getSource()).setBorder(emptyBorder);
                    ((JPanel) me.getSource()).setBorder(null);
                }
            }
        }); 
        
        this.frame = f;
        initSkeletonPreview();
        drawImage();
    }
    
    private void initSkeletonPreview() {
        this.jointHash = new HashMap<>();
        this.idFrame = frame.getNumber();
        createHash(frame.getSkeleton());
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

        g.drawLine((int) (pos1.getX()/this.kX), (int) (pos1.getY()/this.kY), (int) (pos2.getX()/this.kX), (int) (pos2.getY()/this.kY));
    }

    private void drawJoint(Graphics2D g, JointType type) {
        if(!jointHash.containsKey(type)) { return; }
        
        Joint joint = jointHash.get(type);
        Point3D pos = joint.getProjective();

        // TODO: set color - green conf > 0.5, yellow 0 < x < 0.5, red ==0 !
        if (joint.getConfidence() < 0.1) {
            return;
        }
        
        // TODO: check borders!
        g.fillOval((int) (pos.getX()/this.kX) - 1, (int) (pos.getY()/this.kY) - 1, 3, 3);
    }

    private void drawFrameID(Graphics2D g) {
        g.drawString(Integer.toString(frame.getNumber()), this.size.width-35, 20);
    }
    
    private void drawSkeleton(Graphics2D g, Skeleton skeleton) {
        g.setColor(Color.YELLOW);
        
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
        drawSkeleton(g2,frame.getSkeleton());
        drawFrameID(g2);
    }
        
    public int getFrameID() {
        return idFrame;
    }
    
    protected void paintComponent(Graphics g) {
        g.drawImage(bufImg, 0, 0, this);
    }
    
    public Dimension getPreferredSize() { return size; }
    
    public void setSelected() {
        if(this != SkeletonPreview.selectedPreview) {
            // Set
            setBorder(selectBorder);
            selected = true;
            // Reset
            SkeletonPreview.selectedPreview.setBorder(emptyBorder);
            SkeletonPreview.selectedPreview.selected = false;
            SkeletonPreview.selectedPreview = this;
        }
    }
}
