package kinect.util;

import java.util.HashMap;
import java.util.List;
import kinect.storage.KinectStorageProtos.Frame;
import kinect.storage.KinectStorageProtos.Joint.JointType;
import kinect.storage.KinectStorageProtos.Point3D;

/**
 *
 * @author samo
 */
public class KinectUtil {
    static HashMap<JointType,String> joinMap = new HashMap<>();
    
    public KinectUtil() {
        
    }

    static List<Point3D> convertRealWorldToProjective(List<Point3D> points) {
            
        return null;
    }
        
    static List<Point3D> convertProjectiveToRealWorld(List<Point3D> points) {
            
        return null;
    }
    /*
     * aProjective[i].X = (XnFloat)fCoeffX * aRealWorld[i].X / aRealWorld[i].Z + nHalfXres;
     * aProjective[i].Y = nHalfYres - (XnFloat)fCoeffY * aRealWorld[i].Y / aRealWorld[i].Z;
     * aProjective[i].Z = aRealWorld[i].Z;
     */
    static Point3D convertRealWorldToProjective(Point3D point) {
            
        return null;
    }
    
    /**
      * X_RW = (X_proj / X_res - 1/2) * Z * x_to_z
	

	XnMapOutputMode outputMode;
	nRetVal = xnGetMapOutputMode(hInstance, &outputMode);
	XN_IS_STATUS_OK(nRetVal);

	xn::DepthPrivateData* pDepthPrivate = (xn::DepthPrivateData*)hInstance->pPrivateData;
	XnDouble fXToZ = pDepthPrivate->GetRealWorldXtoZ();
	XnDouble fYToZ = pDepthPrivate->GetRealWorldYtoZ();

	for (XnUInt32 i = 0; i < nCount; ++i)
	{
		XnDouble fNormalizedX = (aProjective[i].X / outputMode.nXRes - 0.5);
		aRealWorld[i].X = (XnFloat)(fNormalizedX * aProjective[i].Z * fXToZ);

		XnDouble fNormalizedY = (0.5 - aProjective[i].Y / outputMode.nYRes);
		aRealWorld[i].Y = (XnFloat)(fNormalizedY * aProjective[i].Z * fYToZ);

		aRealWorld[i].Z = aProjective[i].Z;
	}
     */
    static Point3D convertProjectiveToRealWorld(Point3D point) {
            
        return null;
    }
}
