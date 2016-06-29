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

import android.opengl.Matrix;
import com.projecttango.unibw.graphmapper.graph.Hallway;
import com.projecttango.unibw.graphmapper.graph.MeasurementType;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Measurement representation of wall as a measured plane.
 * The wall measurements can be from type WALL, ROOM, MARKER or ENTRY.
 * Markers and Entrypoints can be placed on the wall or on the ground (option).</p>
 *
 * <p>This class is from the Java Floorlan example (https://github.com/googlesamples/tango-examples-java) and was edited by the author.
 * Added changes of Google's Okul update from June 9th (13.06.2016).</p>
 *
 * @author Konstantin Klinger
 * @version 3.0
 */
public class WallMeasurement {
    /** The pose of the plane in OpenGl frame */
    private float[] mOpenGlTPlaneTransform;
    /** The pose of the depth camera when the measurement was taken in OpenGl frame */
    private float[] mOpenGlTDepthTransform;
    /** The mTimestamp of the measurement */
    private double mTimestamp;
    /** Type of the measurement */
    private MeasurementType mType;
    /** Text / Name of the measurement (not needed for every type) */
    private String mText;
    /** Connections of a entrypoint if the measurement type is a entrypoint */
    private List<Hallway> mHallwayToList = new ArrayList<Hallway>();
    /** Positions of the connections of a entrypoint if the measurement type is a entrypoint */
    private List<float[]> mPositionToList = new ArrayList<float[]>();

    /**
     * Constructor (Creates a new wall measurement object with given pose data and a specific type)
     * @param type (type of the wall measurement)
     */
    public WallMeasurement(float[] openGlTPlaneTransform, float[] openGlTDepthTransform, double timestamp, MeasurementType type) {
        mOpenGlTPlaneTransform = openGlTPlaneTransform;
        mOpenGlTDepthTransform = openGlTDepthTransform;
        mTimestamp = timestamp;
        mType = type;
        mText = null;
    }

    /**
     * Update the plane pose of the measurement given an updated device pose at the timestamp of
     * the measurement.
     */
    public void update(float[] newOpenGlTDepthTransform) {
        float[] depthTOpenGl = new float[16];
        Matrix.invertM(depthTOpenGl, 0, mOpenGlTDepthTransform, 0);
        float[] newOpenGlTOldOpenGl = new float[16];
        Matrix.multiplyMM(newOpenGlTOldOpenGl, 0, newOpenGlTDepthTransform, 0, depthTOpenGl, 0);
        float[] newOpenGlTPlane = new float[16];
        Matrix.multiplyMM(newOpenGlTPlane, 0, newOpenGlTOldOpenGl, 0, mOpenGlTPlaneTransform, 0);
        mOpenGlTPlaneTransform = newOpenGlTPlane;
        mOpenGlTDepthTransform = newOpenGlTDepthTransform;
    }

    /**
     * Intersect this measurement with another WallMeasurement to get the corners of the plan.
     * @param otherWallMeasurement (The other WallMeasurement to intersect with)
     * @return The point of intersection in world frame.
     */
    public float[] intersect(WallMeasurement otherWallMeasurement) {
        float[] openGlTPlane = getPlaneTransform();
        float[] openGlTOtherPlane = otherWallMeasurement.getPlaneTransform();
        // We will calculate the intersection in the frame of the first transformation.
        // Transform the second wall measurement to the first measurement frame
        float[] planeTOpenGl = new float[16];
        Matrix.invertM(planeTOpenGl, 0, openGlTPlane, 0);
        float[] firstPlaneTsecondPlane = new float[16];
        Matrix.multiplyMM(firstPlaneTsecondPlane, 0, planeTOpenGl, 0, openGlTOtherPlane, 0);

        // The translation of the second transform origin, in the first one's frame
        float[] wallPsecond = new float[]{firstPlaneTsecondPlane[12], firstPlaneTsecondPlane[13],
                firstPlaneTsecondPlane[14]};
        // The vector representing the X axis of the second transform, in the first's frame
        float[] wallTXsecond = new float[]{firstPlaneTsecondPlane[0], firstPlaneTsecondPlane[1],
                firstPlaneTsecondPlane[2]};

        float[] wallPintersection =
                new float[]{wallPsecond[0] - wallTXsecond[0] / wallTXsecond[2] * wallPsecond[2],
                        0, 0, 1};

        float[] worldPIntersection = new float[4];
        Matrix.multiplyMV(worldPIntersection, 0, openGlTPlane, 0, wallPintersection, 0);

        return worldPIntersection;
    }

    /**
     * Gettern function for the pose / transform of the wall measurement.
     * @return mOpenGlTPlaneTransform
     */
    public float[] getPlaneTransform() {
        return mOpenGlTPlaneTransform;
    }

    /**
     * Gettern function for the timestamp of the pse / transform of the wall measurement.
     * @return mTimestamp
     */
    public double getDepthTransformTimeStamp() {
        return mTimestamp;
    }

    /**
     * Getter function for the measurement type
     * @return mType
     */
    public MeasurementType getMeasurementType() {
        return mType;
    }

    /**
     * Setter function for the measurement type
     * @param type (type of the measurement)
     */
    public void setMeasurementType(MeasurementType type) {
        mType = type;
    }

    /**
     * Getter function for the text / name of the measurement
     * @return mText
     */
    public String getText() { return mText; }

    /**
     * Setter function for the text / name of the measurement.
     * @param s (text / name)
     */
    public void setText(String s) {
        mText = s;
    }

    /**
     * Getter function for the list of connections of a entrypoint measurement.
     * @return mHallwayToList
     */
    public List<Hallway> getHallwayToList() {
        return mHallwayToList;
    }

    /**
     * This function adds a new connection to the entrypoint measurement. (only used if the type is ENTRY)
     * @param pos (position of the connected entrypoint)
     * @param hallway (connected hallway)
     */
    public void addConnection(float[] pos, Hallway hallway) {
        mPositionToList.add(pos);
        mHallwayToList.add(hallway);
    }

    /**
     * Getter function for the positions of all connections of a entrypoint.
     * @return mPositionToList
     */
    public List<float[]> getPositionToList() {
        return mPositionToList;
    }

}
