/****************************************************************************
 *                                                                           *
 *  OpenNI 1.x Alpha                                                         *
 *  Copyright (C) 2011 PrimeSense Ltd.                                       *
 *                                                                           *
 *  This file is part of OpenNI.                                             *
 *                                                                           *
 *  OpenNI is free software: you can redistribute it and/or modify           *
 *  it under the terms of the GNU Lesser General Public License as published *
 *  by the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                      *
 *                                                                           *
 *  OpenNI is distributed in the hope that it will be useful,                *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the             *
 *  GNU Lesser General Public License for more details.                      *
 *                                                                           *
 *  You should have received a copy of the GNU Lesser General Public License *
 *  along with OpenNI. If not, see <http://www.gnu.org/licenses/>.           *
 *                                                                           *
 ****************************************************************************/
package kinect.recorder;

import org.OpenNI.*;

import java.nio.ShortBuffer;
import java.util.HashMap;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

import kinect.storage.KinectStorageProtos.Sequence;
import kinect.storage.KinectStorageProtos.Frame;
import kinect.storage.KinectStorageProtos.Skeleton;
import kinect.storage.KinectStorageProtos.Joint;
import kinect.storage.KinectStorageProtos.FOV;
//import kinect.storage.KinectStorageProtos.Point3D;

public class UserTracker extends Component {

    class NewUserObserver implements IObserver<UserEventArgs> {

        @Override
        public void update(IObservable<UserEventArgs> observable,
                UserEventArgs args) {
            System.out.println("New user " + args.getId());
            try {
                if (skeletonCap.needPoseForCalibration()) {
                    // poseDetectionCap.StartPoseDetection(calibPose, args.getId());
                    poseDetectionCap.startPoseDetection(calibPose, args.getId());
                } else {
                    skeletonCap.requestSkeletonCalibration(args.getId(), true);
                }
            } catch (StatusException e) {
                e.printStackTrace();
            }
        }
    }

    class LostUserObserver implements IObserver<UserEventArgs> {

        @Override
        public void update(IObservable<UserEventArgs> observable,
                UserEventArgs args) {
            System.out.println("Lost user " + args.getId());
            joints.remove(args.getId());
        }
    }

    class CalibrationCompleteObserver implements IObserver<CalibrationProgressEventArgs> {

        @Override
        public void update(IObservable<CalibrationProgressEventArgs> observable,
                CalibrationProgressEventArgs args) {
            System.out.println("Calibraion complete: " + args.getStatus());
            try {
                if (args.getStatus() == CalibrationProgressStatus.OK) {
                    System.out.println("starting tracking " + args.getUser());
                    skeletonCap.startTracking(args.getUser());
                    joints.put(new Integer(args.getUser()), new HashMap<SkeletonJoint, SkeletonJointPosition>());
                } else {
                    if (skeletonCap.needPoseForCalibration()) {
                        //poseDetectionCap.StartPoseDetection(calibPose, args.getUser());
                        poseDetectionCap.startPoseDetection(calibPose, args.getUser());
                    } else {
                        skeletonCap.requestSkeletonCalibration(args.getUser(), true);
                    }
                }
            } catch (StatusException e) {
                e.printStackTrace();
            }
        }
    }

    class PoseDetectedObserver implements IObserver<PoseDetectionEventArgs> {

        @Override
        public void update(IObservable<PoseDetectionEventArgs> observable,
                PoseDetectionEventArgs args) {
            System.out.println("Pose " + args.getPose() + " detected for " + args.getUser());
            try {
                //poseDetectionCap.StopPoseDetection(args.getUser());
                poseDetectionCap.stopPoseDetection(args.getUser());
                skeletonCap.requestSkeletonCalibration(args.getUser(), true);
            } catch (StatusException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private OutArg<ScriptNode> scriptNode;
    private Context context;
    private DepthGenerator depthGen;
    private UserGenerator userGen;
    private SkeletonCapability skeletonCap;
    private PoseDetectionCapability poseDetectionCap;
    private byte[] imgbytes;
    private float histogram[];
    String calibPose = null;
    HashMap<Integer, HashMap<SkeletonJoint, SkeletonJointPosition>> joints;
    private boolean drawBackground = true;
    private boolean drawPixels = true;
    private boolean drawSkeleton = true;
    private boolean printID = true;
    private boolean printState = true;
    private BufferedImage bimg;
    int width, height;
    private final String SAMPLE_XML_FILE = "/home/samo/OpenNI/Data/Config.xml";
    
    // Recording vars
    public RecorderUI recorderUI;
//    private Skeleton.Builder skeleton;
//    private Joint rjoint;
    public int frameNumber;
    public Sequence.Builder sequence;
    public boolean recording = false;
    private int oldDepthGenFrameNumber = 0;
    private int newDepthGenFrameNumber = 0;
    public boolean bool = false;
    
    public UserTracker() {
        
        try {
            scriptNode = new OutArg<ScriptNode>();
            context = Context.createFromXmlFile(SAMPLE_XML_FILE, scriptNode);

            depthGen = DepthGenerator.create(context);
            DepthMetaData depthMD = depthGen.getMetaData();
         
            histogram = new float[10000]; // depthGen.getDeviceMaxDepth()
            width = depthMD.getFullXRes();
            height = depthMD.getFullYRes();

            imgbytes = new byte[width * height * 3];

            userGen = UserGenerator.create(context);
            skeletonCap = userGen.getSkeletonCapability();
            poseDetectionCap = userGen.getPoseDetectionCapability();

            userGen.getNewUserEvent().addObserver(new NewUserObserver());
            userGen.getLostUserEvent().addObserver(new LostUserObserver());
            skeletonCap.getCalibrationCompleteEvent().addObserver(new CalibrationCompleteObserver());
            poseDetectionCap.getPoseDetectedEvent().addObserver(new PoseDetectedObserver());

            calibPose = skeletonCap.getSkeletonCalibrationPose();
            joints = new HashMap<Integer, HashMap<SkeletonJoint, SkeletonJointPosition>>();

            skeletonCap.setSkeletonProfile(SkeletonProfile.ALL);

            context.startGeneratingAll();
        } catch (GeneralException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void calcHist(ShortBuffer depth) {
        // reset
        for (int i = 0; i < histogram.length; ++i) {
            histogram[i] = 0;
        }

        depth.rewind();

        int points = 0;
        while (depth.remaining() > 0) {
            short depthVal = depth.get();
            if (depthVal != 0) {
                histogram[depthVal]++;
                points++;
            }
        }

        for (int i = 1; i < histogram.length; i++) {
            histogram[i] += histogram[i - 1];
        }

        if (points > 0) {
            for (int i = 1; i < histogram.length; i++) {
                histogram[i] = 1.0f - (histogram[i] / (float) points);
            }
        }
    }

    void updateDepth() {
        try {
            // Waits for any node to have new data. Once new data is
            // available from any node, all nodes are updated.
            context.waitAnyUpdateAll();

            DepthMetaData depthMD = depthGen.getMetaData();
            SceneMetaData sceneMD = userGen.getUserPixels(0);

            ShortBuffer scene = sceneMD.getData().createShortBuffer();
            ShortBuffer depth = depthMD.getData().createShortBuffer();
            calcHist(depth);
            depth.rewind();

            while (depth.remaining() > 0) {
                int pos = depth.position();
                short pixel = depth.get();
                short user = scene.get();

                imgbytes[3 * pos] = 0;
                imgbytes[3 * pos + 1] = 0;
                imgbytes[3 * pos + 2] = 0;

                if (drawBackground || pixel != 0) {
                    int colorID = user % (colors.length - 1);
                    if (user == 0) {
                        colorID = colors.length - 1;
                    }
                    if (pixel != 0) {
                        float histValue = histogram[pixel];
                        imgbytes[3 * pos] = (byte) (histValue * colors[colorID].getRed());
                        imgbytes[3 * pos + 1] = (byte) (histValue * colors[colorID].getGreen());
                        imgbytes[3 * pos + 2] = (byte) (histValue * colors[colorID].getBlue());
                    }
                }
            }
            
             // RECORD
            if(recording) {
                newDepthGenFrameNumber = depthGen.getFrameID();
                if(newDepthGenFrameNumber != oldDepthGenFrameNumber) {
                    oldDepthGenFrameNumber = newDepthGenFrameNumber;
                    
                    int[] users = userGen.getUsers();
                    Skeleton.Builder skeleton = Skeleton.newBuilder();

                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.HEAD, SkeletonJoint.HEAD));
                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.NECK, SkeletonJoint.NECK));
                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.TORSO, SkeletonJoint.TORSO));

                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.LEFT_SHOULDER, SkeletonJoint.LEFT_SHOULDER));
                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.LEFT_ELBOW, SkeletonJoint.LEFT_ELBOW));
                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.LEFT_HAND, SkeletonJoint.LEFT_HAND));

                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.RIGHT_SHOULDER, SkeletonJoint.RIGHT_SHOULDER));
                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.RIGHT_ELBOW, SkeletonJoint.RIGHT_ELBOW));
                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.RIGHT_HAND, SkeletonJoint.RIGHT_HAND));

                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.LEFT_HIP, SkeletonJoint.LEFT_HIP));
                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.LEFT_KNEE, SkeletonJoint.LEFT_KNEE));
                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.LEFT_FOOT, SkeletonJoint.LEFT_FOOT));

                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.RIGHT_HIP, SkeletonJoint.RIGHT_HIP));
                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.RIGHT_KNEE, SkeletonJoint.RIGHT_KNEE));
                    skeleton.addJoints(buildJoint(users[0], Joint.JointType.RIGHT_FOOT, SkeletonJoint.RIGHT_FOOT));

                    skeleton.setCoM(convertPoint3D(depthGen.convertRealWorldToProjective(userGen.getUserCoM(users[0]))));
                    
                    Frame.Builder frame = Frame.newBuilder();
                    frame.setSkeleton(skeleton.build());
                    FOV.Builder fov = FOV.newBuilder();
                    fov.setFHFOV(depthGen.getFieldOfView().getHFOV());
                    fov.setFVFOV(depthGen.getFieldOfView().getVFOV());
                    frame.setFov(fov);
                    
                    System.out.println("DepthGen-Frame:"+newDepthGenFrameNumber+", Recording-Frame: " +frameNumber);

                    frame.setNumber(frameNumber++);
                    sequence.addFrame(frame.build());
               }
            }
           
        } catch (GeneralException e) {
            e.printStackTrace();
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
    
    Color colors[] = {Color.RED, Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.PINK, Color.YELLOW, Color.WHITE};

    public void getJoint(int user, SkeletonJoint joint) throws StatusException {
        SkeletonJointPosition pos = skeletonCap.getSkeletonJointPosition(user, joint);
     
        if (pos.getPosition().getZ() != 0) {
            joints.get(user).put(joint, new SkeletonJointPosition(depthGen.convertRealWorldToProjective(pos.getPosition()), pos.getConfidence()));
        } else {
            joints.get(user).put(joint, new SkeletonJointPosition(new Point3D(), 0));
        }
    }

    public void getJoints(int user) throws StatusException {
        getJoint(user, SkeletonJoint.HEAD);
        getJoint(user, SkeletonJoint.NECK);

        getJoint(user, SkeletonJoint.LEFT_SHOULDER);
        getJoint(user, SkeletonJoint.LEFT_ELBOW);
        getJoint(user, SkeletonJoint.LEFT_HAND);

        getJoint(user, SkeletonJoint.RIGHT_SHOULDER);
        getJoint(user, SkeletonJoint.RIGHT_ELBOW);
        getJoint(user, SkeletonJoint.RIGHT_HAND);

        getJoint(user, SkeletonJoint.TORSO);

        getJoint(user, SkeletonJoint.LEFT_HIP);
        getJoint(user, SkeletonJoint.LEFT_KNEE);
        getJoint(user, SkeletonJoint.LEFT_FOOT);

        getJoint(user, SkeletonJoint.RIGHT_HIP);
        getJoint(user, SkeletonJoint.RIGHT_KNEE);
        getJoint(user, SkeletonJoint.RIGHT_FOOT);

    }

    void drawLine(Graphics g, HashMap<SkeletonJoint, SkeletonJointPosition> jointHash, SkeletonJoint joint1, SkeletonJoint joint2) {
        Point3D pos1 = jointHash.get(joint1).getPosition();
        Point3D pos2 = jointHash.get(joint2).getPosition();

        if (jointHash.get(joint1).getConfidence() == 0 || jointHash.get(joint1).getConfidence() == 0) {
            return;
        }

        g.drawLine((int) pos1.getX(), (int) pos1.getY(), (int) pos2.getX(), (int) pos2.getY());
    }
    
    void drawJoint(Graphics g, HashMap<SkeletonJoint, SkeletonJointPosition> jointHash, SkeletonJoint joint) {
        Point3D pos = jointHash.get(joint).getPosition();

        if (jointHash.get(joint).getConfidence() == 0 ) {
            return;
        }

        g.fillOval((int) pos.getX() -3, (int) pos.getY()-3, 5, 5);
    }

    public void drawSkeleton(Graphics g, int user) throws StatusException {
        getJoints(user);
        HashMap<SkeletonJoint, SkeletonJointPosition> dict = joints.get(new Integer(user));

        drawLine(g, dict, SkeletonJoint.HEAD, SkeletonJoint.NECK);

        drawLine(g, dict, SkeletonJoint.LEFT_SHOULDER, SkeletonJoint.TORSO);
        drawLine(g, dict, SkeletonJoint.RIGHT_SHOULDER, SkeletonJoint.TORSO);

        drawLine(g, dict, SkeletonJoint.NECK, SkeletonJoint.LEFT_SHOULDER);
        drawLine(g, dict, SkeletonJoint.LEFT_SHOULDER, SkeletonJoint.LEFT_ELBOW);
        drawLine(g, dict, SkeletonJoint.LEFT_ELBOW, SkeletonJoint.LEFT_HAND);

        drawLine(g, dict, SkeletonJoint.NECK, SkeletonJoint.RIGHT_SHOULDER);
        drawLine(g, dict, SkeletonJoint.RIGHT_SHOULDER, SkeletonJoint.RIGHT_ELBOW);
        drawLine(g, dict, SkeletonJoint.RIGHT_ELBOW, SkeletonJoint.RIGHT_HAND);

        drawLine(g, dict, SkeletonJoint.LEFT_HIP, SkeletonJoint.TORSO);
        drawLine(g, dict, SkeletonJoint.RIGHT_HIP, SkeletonJoint.TORSO);
        drawLine(g, dict, SkeletonJoint.LEFT_HIP, SkeletonJoint.RIGHT_HIP);

        drawLine(g, dict, SkeletonJoint.LEFT_HIP, SkeletonJoint.LEFT_KNEE);
        drawLine(g, dict, SkeletonJoint.LEFT_KNEE, SkeletonJoint.LEFT_FOOT);

        drawLine(g, dict, SkeletonJoint.RIGHT_HIP, SkeletonJoint.RIGHT_KNEE);
        drawLine(g, dict, SkeletonJoint.RIGHT_KNEE, SkeletonJoint.RIGHT_FOOT);
        
        // Draw Joints
        drawJoint(g, dict, SkeletonJoint.HEAD);
        drawJoint(g, dict, SkeletonJoint.NECK);
        drawJoint(g, dict, SkeletonJoint.TORSO);

        drawJoint(g, dict, SkeletonJoint.LEFT_SHOULDER);
        drawJoint(g, dict, SkeletonJoint.LEFT_ELBOW);
        drawJoint(g, dict, SkeletonJoint.LEFT_HAND);

        drawJoint(g, dict, SkeletonJoint.RIGHT_SHOULDER);
        drawJoint(g, dict, SkeletonJoint.RIGHT_ELBOW);
        drawJoint(g, dict, SkeletonJoint.RIGHT_HAND);

        drawJoint(g, dict, SkeletonJoint.LEFT_HIP);
        drawJoint(g, dict, SkeletonJoint.LEFT_KNEE);
        drawJoint(g, dict, SkeletonJoint.LEFT_FOOT);

        drawJoint(g, dict, SkeletonJoint.RIGHT_HIP);
        drawJoint(g, dict, SkeletonJoint.RIGHT_KNEE);
        drawJoint(g, dict, SkeletonJoint.RIGHT_FOOT);
    }
    

    public void paint(Graphics g) {
        if (drawPixels) {
            DataBufferByte dataBuffer = new DataBufferByte(imgbytes, width * height * 3);

            WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width * 3, 3, new int[]{0, 1, 2}, null);

            ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8}, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);

            bimg = new BufferedImage(colorModel, raster, false, null);

            g.drawImage(bimg, 0, 0, null);
        }
        try {
            int[] users = userGen.getUsers();
            for (int i = 0; i < users.length; ++i) {
                Color c = colors[users[i] % colors.length];
                c = new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue());

                g.setColor(c);
                if (drawSkeleton && skeletonCap.isSkeletonTracking(users[i])) {
                    drawSkeleton(g, users[i]);
                }

                if (printID) {
                    Point3D com = depthGen.convertRealWorldToProjective(userGen.getUserCoM(users[i]));
                    String label = null;
                    if (!printState) {
                        label = new String("" + users[i]);
                    } else if (skeletonCap.isSkeletonTracking(users[i])) {
                        // Tracking
                        label = new String(users[i] + " - Tracking");
                    } else if (skeletonCap.isSkeletonCalibrating(users[i])) {
                        // Calibrating
                        label = new String(users[i] + " - Calibrating");
                    } else {
                        // Nothing
                        label = new String(users[i] + " - Looking for pose (" + calibPose + ")");
                    }

                    g.drawString(label, (int) com.getX(), (int) com.getY());
                }
            }
        } catch (StatusException e) {
            e.printStackTrace();
        }

    }
    
    public void setDrawSkeleton(boolean drawSkeleton) {
        this.drawSkeleton = drawSkeleton;
    }
    
    public void setDrawDepthImage(boolean drawPixels) {
        this.drawPixels = drawPixels;
    }
    
    public void setPrintID(boolean printID) {
        this.printID = printID;
    }
    
    private kinect.storage.KinectStorageProtos.Joint buildJoint(int user, Joint.JointType jt ,SkeletonJoint sj){
        Joint.Builder joint = Joint.newBuilder();
        try {
            SkeletonJointPosition pos = skeletonCap.getSkeletonJointPosition(user, sj);
            joint.setType(jt);
            joint.setRealWorld(convertPoint3D(pos.getPosition()));
            joint.setProjective(convertPoint3D(depthGen.convertRealWorldToProjective(pos.getPosition())));
            joint.setConfidence(pos.getConfidence());
        } catch (StatusException ex) {
            ex.printStackTrace();
        }
        return joint.build();
    }
            
    
    private kinect.storage.KinectStorageProtos.Point3D convertPoint3D(org.OpenNI.Point3D point) {
        kinect.storage.KinectStorageProtos.Point3D.Builder custom = kinect.storage.KinectStorageProtos.Point3D.newBuilder();
        custom.setX(point.getX());
        custom.setY(point.getY());
        custom.setZ(point.getZ());
        return custom.build();
    }
}
