/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Note:
 * The classes of this project are based on the Java Floorplan example and there are influences from the other Java examples.
 * They can be found on https://github.com/googlesamples/tango-examples-java.
 */

package com.projecttango.unibw.graphmapper.floorplan;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;
import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.methods.SpecularMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.renderer.RajawaliRenderer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.microedition.khronos.opengles.GL10;

import com.projecttango.unibw.graphmapper.graph.MeasurementType;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ScenePoseCalculator;


/**
 * <p>Very simple augmented reality visialization which displays cubes fixed in place for every WallMeasurement.
 * Each time the user clicks on the screen, a cube with the specifiy type is placed flush with the surface detected using the point cloud data at the position clicked.</p>
 *
 * <p>The entrypoint and marker type can be placed on the wall or on the ground or on the wall (option).</p>
 *
 * <p>This class is from the Java Floorlan example (https://github.com/googlesamples/tango-examples-java) and was edited by the author.
 * Added changes of Google's Okul update from June 9th (13.06.2016).</p>
 *
 * @author Konstantin Klinger
 * @version 3.0
 */
public class GraphmapperRenderer extends RajawaliRenderer {
    /** lenght of the cube */
    private static final float CUBE_SIDE_LENGTH = 0.3f;
    /** Tag of this class */
    private static final String TAG = GraphmapperRenderer.class.getSimpleName();
    /** List of normal wall measurements */
    private List<Pose> mWallPoseList = new ArrayList<Pose>();
    /** List of room measurements */
    private List<Pose> mRoomPoseList = new ArrayList<Pose>();
    /** List of entrypoint measurements */
    private List<Pose> mEntryPoseList = new ArrayList<Pose>();
    /** List of marker measurements */
    private List<Pose> mMarkerPoseList = new ArrayList<Pose>();
    /** List of obstacle wall measurements */
    private List<Pose> mCutPoseList = new ArrayList<Pose>();
    /** Signals if the object pose has updated */
    private boolean mObjectPoseUpdated = false;
    /** Signals if the object should be placed on the wall or on the ground */
    private boolean mIsObjectOnGround = false;
    /** wall measurement material */
    private Material mWallMaterial;
    /** room measurement material */
    private Material mRoomMaterial;
    /** entrypoint measurement material */
    private Material mEntryMaterial;
    /** marker measurement material */
    private Material mMarkerMaterial;
    /** obstacle wall measurement material */
    private Material mCutMaterial;
    /** list of all 3D objects (all measurements) */
    private List<Object3D> mMeasurementObjectList = new ArrayList<Object3D>();
    /** Augmented reality related field **/
    private ATexture mTangoCameraTexture;
    /** Augmented reality related fields **/
    private boolean mSceneCameraConfigured;
    /** Type of the latest measurement */
    private MeasurementType mLastType;
    /** reference to the latest 3D object */
    private Object3D mLastObject;
    /** post of the latest measurement */
    private Pose mLastPose;

    /**
     * Constructor (Creates new GraphmapperRenderer object)
     * @param context
     */
    public GraphmapperRenderer(Context context) {
        super(context);
    }

    /**
     * Override the initScene function to set up the scene and create the materials for each measurement type
     */
    @Override
    protected void initScene() {
        // Create a quad covering the whole background and assign a texture to it where the
        // Tango color camera contents will be rendered.
        ScreenQuad backgroundQuad = new ScreenQuad();
        Material tangoCameraMaterial = new Material();
        tangoCameraMaterial.setColorInfluence(0);
        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering
        mTangoCameraTexture =
                new StreamingTexture("camera", (StreamingTexture.ISurfaceListener) null);
        try {
            tangoCameraMaterial.addTexture(mTangoCameraTexture);
            backgroundQuad.setMaterial(tangoCameraMaterial);
        } catch (ATexture.TextureException e) {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e);
        }
        getCurrentScene().addChildAt(backgroundQuad, 0);

        // Add a directional light in an arbitrary direction.
        DirectionalLight light = new DirectionalLight(1, 0.2, -1);
        light.setColor(1, 1, 1);
        light.setPower(0.8f);
        light.setPosition(3, 2, 4);
        getCurrentScene().addLight(light);

        // Set-up wall material.
        mWallMaterial = new Material();
        mWallMaterial.enableLighting(true);
        mWallMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());
        mWallMaterial.setSpecularMethod(new SpecularMethod.Phong());
        mWallMaterial.setColor(0xff009900);
        mWallMaterial.setColorInfluence(0.5f);
        try {
            Texture t = new Texture("wall", R.drawable.wall);
            mWallMaterial.addTexture(t);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
        // Set-up room material.
        mRoomMaterial = new Material();
        mRoomMaterial.enableLighting(true);
        mRoomMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());
        mRoomMaterial.setSpecularMethod(new SpecularMethod.Phong());
        mRoomMaterial.setColor(Color.RED);
        mRoomMaterial.setColorInfluence(0.5f);
        try {
            Texture t = new Texture("room", R.drawable.room);
            mRoomMaterial.addTexture(t);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
        // Set-up entry material.
        mEntryMaterial = new Material();
        mEntryMaterial.enableLighting(true);
        mEntryMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());
        mEntryMaterial.setSpecularMethod(new SpecularMethod.Phong());
        mEntryMaterial.setColor(Color.BLUE);
        mEntryMaterial.setColorInfluence(0.5f);
        try {
            Texture t = new Texture("entry", R.drawable.entry);
            mEntryMaterial.addTexture(t);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
        //Set-up marker material
        mMarkerMaterial = new Material();
        mMarkerMaterial.enableLighting(true);
        mMarkerMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());
        mMarkerMaterial.setSpecularMethod(new SpecularMethod.Phong());
        mMarkerMaterial.setColor(Color.CYAN);
        mMarkerMaterial.setColorInfluence(0.5f);
        try {
            Texture t = new Texture("marker", R.drawable.marker);
            mMarkerMaterial.addTexture(t);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
        // Set-up cut material.
        mCutMaterial = new Material();
        mCutMaterial.enableLighting(true);
        mCutMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());
        mCutMaterial.setSpecularMethod(new SpecularMethod.Phong());
        mCutMaterial.setColor(Color.YELLOW);
        mCutMaterial.setColorInfluence(0.5f);
        try {
            Texture t = new Texture("cut", R.drawable.cut);
            mCutMaterial.addTexture(t);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function updates the AR objects if necessary and creates new objects and place them into the scene.
     * @param elapsedRealTime
     * @param deltaTime
     */
    @Override
    protected void onRender(long elapsedRealTime, double deltaTime) {
        // Update the AR object if necessary
        // Synchronize against concurrent access with the setter below.
        synchronized (this) {
            if (mObjectPoseUpdated) {
                Iterator<Pose> poseIterator = mWallPoseList.iterator();
                Object3D object3D;
                while (poseIterator.hasNext()) {
                    Pose pose = poseIterator.next();
                    object3D = new Plane(CUBE_SIDE_LENGTH, CUBE_SIDE_LENGTH, 2, 2);
                    object3D.setMaterial(mWallMaterial);
                    // Rotate around X axis so the texture is applied correctly.
                    // NOTE: This may be a Rajawali bug.
                    // https://github.com/Rajawali/Rajawali/issues/1561
                    object3D.setDoubleSided(true);
                    object3D.rotate(Vector3.Axis.X, 180);
                    // Place the 3D object in the location of the detected plane.
                    object3D.setPosition(pose.getPosition());
                    object3D.rotate(pose.getOrientation());

                    getCurrentScene().addChild(object3D);
                    mMeasurementObjectList.add(object3D);
                    if(pose.equals(mLastPose)) {
                        mLastObject = object3D;
                    }
                    poseIterator.remove();
                }
                poseIterator = mRoomPoseList.iterator();
                while (poseIterator.hasNext()) {
                    Pose pose = poseIterator.next();
                    object3D = new Plane(CUBE_SIDE_LENGTH, CUBE_SIDE_LENGTH, 2, 2);
                    object3D.setMaterial(mRoomMaterial);
                    // Rotate around X axis so the texture is applied correctly.
                    // NOTE: This may be a Rajawali bug.
                    // https://github.com/Rajawali/Rajawali/issues/1561
                    object3D.setDoubleSided(true);
                    object3D.rotate(Vector3.Axis.X, 180);
                    // Place the 3D object in the location of the detected plane.
                    object3D.setPosition(pose.getPosition());
                    object3D.rotate(pose.getOrientation());

                    getCurrentScene().addChild(object3D);
                    mMeasurementObjectList.add(object3D);
                    if(pose.equals(mLastPose)) {
                        mLastObject = object3D;
                    }
                    poseIterator.remove();
                }
                poseIterator = mEntryPoseList.iterator();
                while (poseIterator.hasNext()) {
                    Pose pose = poseIterator.next();
                    if(mIsObjectOnGround) {
                        object3D = new Plane(CUBE_SIDE_LENGTH, CUBE_SIDE_LENGTH, 2, 2, Vector3.Axis.Y); //Plane on ground
                        object3D.rotate(Vector3.Axis.Y, 180);
                    } else {
                        object3D = new Plane(CUBE_SIDE_LENGTH, CUBE_SIDE_LENGTH, 2, 2);
                        object3D.rotate(Vector3.Axis.X, 180);
                        object3D.rotate(pose.getOrientation());
                    }
                    object3D.setMaterial(mEntryMaterial);
                    object3D.setDoubleSided(true);

                    // Place the 3D object in the location of the detected plane.
                    object3D.setPosition(pose.getPosition());

                    getCurrentScene().addChild(object3D);
                    mMeasurementObjectList.add(object3D);
                    if(pose.equals(mLastPose)) {
                        mLastObject = object3D;
                    }
                    poseIterator.remove();
                }
                poseIterator = mMarkerPoseList.iterator();
                while (poseIterator.hasNext()) {
                    Pose pose = poseIterator.next();
                    if(mIsObjectOnGround) {
                        object3D = new Plane(CUBE_SIDE_LENGTH, CUBE_SIDE_LENGTH, 2, 2, Vector3.Axis.Y); //Plane on ground
                        object3D.rotate(Vector3.Axis.Y, 180);
                    } else {
                        object3D = new Plane(CUBE_SIDE_LENGTH, CUBE_SIDE_LENGTH, 2, 2);
                        object3D.rotate(Vector3.Axis.X, 180);
                        object3D.rotate(pose.getOrientation());
                    }
                    object3D.setMaterial(mMarkerMaterial);
                    object3D.setDoubleSided(true);

                    // Place the 3D object in the location of the detected plane.
                    object3D.setPosition(pose.getPosition());

                    getCurrentScene().addChild(object3D);
                    mMeasurementObjectList.add(object3D);
                    if(pose.equals(mLastPose)) {
                        mLastObject = object3D;
                    }
                    poseIterator.remove();
                }
                poseIterator = mCutPoseList.iterator();
                while (poseIterator.hasNext()) {
                    Pose pose = poseIterator.next();
                    object3D = new Plane(CUBE_SIDE_LENGTH, CUBE_SIDE_LENGTH, 2, 2);
                    object3D.setMaterial(mCutMaterial);
                    object3D.setDoubleSided(true);
                    object3D.rotate(Vector3.Axis.X, 180);
                    // Place the 3D object in the location of the detected plane.
                    object3D.setPosition(pose.getPosition());
                    object3D.rotate(pose.getOrientation());
                    getCurrentScene().addChild(object3D);
                    mMeasurementObjectList.add(object3D);
                    if(pose.equals(mLastPose)) {
                        mLastObject = object3D;
                    }
                    poseIterator.remove();
                }
                mObjectPoseUpdated = false;
            }
        }

        super.onRender(elapsedRealTime, deltaTime);
    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The device pose should match the pose of the device at the time of the last rendered RGB
     * frame, which can be retrieved with this.getTimestamp();
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     * @param cameraPose (position of the camera)
     */
    public void updateRenderCameraPose(TangoPoseData cameraPose) {
        float[] rotation = cameraPose.getRotationAsFloats();
        float[] translation = cameraPose.getTranslationAsFloats();
        Quaternion quaternion = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
        // Conjugating the Quaternion is need because Rajawali uses left handed convention for
        // quaternions.
        getCurrentCamera().setRotation(quaternion.conjugate());
        getCurrentCamera().setPosition(translation[0], translation[1], translation[2]);
    }

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     * @return ID of the Texture
     */
    public int getTextureId() {
        return mTangoCameraTexture == null ? -1 : mTangoCameraTexture.getTextureId();
    }

    /**
     * We need to override this method to mark the camera for re-configuration (set proper
     * projection matrix) since it will be reset by Rajawali on surface changes.
     */
    @Override
    public void onRenderSurfaceSizeChanged(GL10 gl, int width, int height) {
        super.onRenderSurfaceSizeChanged(gl, width, height);
        mSceneCameraConfigured = false;
    }

    /**
     * Getter function if the scene camera is configured
     * @return mSceneCameraConfigured
     */
    public boolean isSceneCameraConfigured() {
        return mSceneCameraConfigured;
    }

    /**
     * Sets the projection matrix for the scen camera to match the parameters of the color camera,
     * provided by the {@code TangoCameraIntrinsics}.
     */
    public void setProjectionMatrix(TangoCameraIntrinsics intrinsics) {
        Matrix4 projectionMatrix = ScenePoseCalculator.calculateProjectionMatrix(
                intrinsics.width, intrinsics.height,
                intrinsics.fx, intrinsics.fy, intrinsics.cx, intrinsics.cy);
        getCurrentCamera().setProjectionMatrix(projectionMatrix);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset,
                                 float xOffsetStep, float yOffsetStep,
                                 int xPixelOffset, int yPixelOffset) {
    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    /**
     * Add a new WallMeasurement.
     * A new cube will be added at the plane position and orientation to represent the measurement.
     * @param wallMeasurement
     */
    public synchronized void addWallMeasurement(WallMeasurement wallMeasurement) {
        float[] openGlTWall = wallMeasurement.getPlaneTransform();
        Matrix4 openGlTWallMatrix = new Matrix4(openGlTWall);
        Pose pose = new Pose(openGlTWallMatrix.getTranslation(), new Quaternion().fromMatrix(openGlTWallMatrix).conjugate());
        if(wallMeasurement.getMeasurementType() == MeasurementType.ENTRY) {
            mEntryPoseList.add(pose);
            mLastType = MeasurementType.ENTRY;
        } else if(wallMeasurement.getMeasurementType() == MeasurementType.ROOM) {
            mRoomPoseList.add(pose);
            mLastType = MeasurementType.ROOM;
        } else if(wallMeasurement.getMeasurementType() == MeasurementType.CUT) {
            mCutPoseList.add(pose);
            mLastType = MeasurementType.CUT;
        } else if(wallMeasurement.getMeasurementType() == MeasurementType.MARKER) {
            mMarkerPoseList.add(pose);
            mLastType = MeasurementType.MARKER;
        } else {
            mWallPoseList.add(pose);
            mLastType = MeasurementType.WALL;
        }
        mLastPose = pose;
        mObjectPoseUpdated = true;
    }

    /**
     * Remove all the measurements from the Scene.
     */
    public synchronized void removeMeasurements() {
        for (Object3D object3D : mMeasurementObjectList) {
            getCurrentScene().removeChild(object3D);
            mWallPoseList.clear();
            mRoomPoseList.clear();
            mEntryPoseList.clear();
            mMarkerPoseList.clear();
            mCutPoseList.clear();
        }
        mLastType = null;
        mLastPose = null;
        mLastObject = null;
    }

    /**
     * Undo the last measurement. Remove the object from the scene.
     */
    public synchronized void undoLastMeasurement() {
        if(mLastPose != null && mLastObject != null && mLastType != null) {
            if(mLastType == MeasurementType.ROOM) {
                mRoomPoseList.remove(mLastPose);
            } else if(mLastType == MeasurementType.ENTRY) {
                mEntryPoseList.remove(mLastPose);
            } else if(mLastType == MeasurementType.CUT) {
                mCutPoseList.remove(mLastObject);
            } else if(mLastType == MeasurementType.MARKER) {
                mMarkerPoseList.remove(mLastObject);
            } else {
                //Wall
                mWallPoseList.remove(mLastPose);
            }
            getCurrentScene().removeChild(mLastObject);
            mMeasurementObjectList.remove(mLastObject);
            mLastType = null;
            mLastPose = null;
            mLastObject = null;
        }
    }

    /**
     * Setter function for the mIsObjectOnGround attribute.
     * @param value
     */
    public void setIsObjectOnGround(boolean value) {
        mIsObjectOnGround = value;
    }

    /**
     * Getter function if the object should be placed on the ground or on the wall.
     * @return mIsObjectOnGround
     */
    public boolean isObjectOnGround() {
        return mIsObjectOnGround;
    }
}
