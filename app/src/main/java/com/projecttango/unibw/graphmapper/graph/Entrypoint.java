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

package com.projecttango.unibw.graphmapper.graph;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>A Entrypoint is a connection between two hallways. It can be a simple door or stairs or a lift.</p>
 *
 * <p>In this version the user have to create a entrypoint for each level a lift can reach and
 * connect the entrypoints of each level. A entrypoint can be seen as the edges of the navigation graph / network. </p>
 *
 * <p>This class was created by Konstantin Klinger on 25.04.16.
 * Added changes of Google's Okul update from June 9th (13.06.2016). </p>
 *
 * @author Konstantin Klinger
 * @version 2.0
 */
public class Entrypoint {
    /** Name of the entrypoint */
    private String mName;
    /** Type of the entrypoint (DOOR, LIFT or STAIRS) */
    private MeasurementType mType;
    /** Position of the entrypoint */
    private float[] mPositionFrom;
    /** ID of the hallway where this entrypoint is located */
    private int mHallwayFromID;
    /** Positions of the connected entrypoints */
    private List<float[]> mPositionToList = new ArrayList<float[]>();
    /** ID of the hallways where the connections of this entrypoint are located */
    private List<Integer> mHallwayToIDList = new ArrayList<Integer>();

    /**
     * Constructor (Creates a new Entrypoint object with the give parameters)
     * @param name (Name of the entrypoint)
     * @param type (Type of the entrypoint)
     * @param positionFrom (Position of the entrypoint)
     * @param hallwayFromID (ID of the hallway where this entrypoint is located)
     * @param positionToList (Positions of all connected entrypoints stored in an ArrayList structure)
     * @param hallwayToIDList (ID of all hallways where the connections of this entrypoint are located)
     */
    public Entrypoint(String name, MeasurementType type, float[] positionFrom, int hallwayFromID, List<float[]> positionToList, List<Integer> hallwayToIDList) {
        mName = name;
        mType = type;
        mPositionFrom = positionFrom;
        mHallwayFromID = hallwayFromID;
        if((positionToList != null) && (hallwayToIDList != null)) {
            mPositionToList.addAll(positionToList);
            mHallwayToIDList.addAll(hallwayToIDList);
        }
    }

    /**
     * Getter function for the name of the entrypoint.
     * @return mName
     */
    public String getName() {
        return mName;
    }

    /**
     * Getter function for the position of the entrypoint
     * @return mPositionFrom
     */
    public float[] getPositionFrom() {
        return mPositionFrom;
    }

    /**
     * Getter function for the array list of the positions of the connected entrypoints.
     * @return mPositionToList
     */
    public List<float[]> getPositionToList() {
        return mPositionToList;
    }

    /**
     * Getter function for the ID of the entrypoint.
     * @return mHallwayFromID
     */
    public int getHallwayFromID() {
        return mHallwayFromID;
    }

    /**
     * Getter function for the list of IDs of the connected entrypoints
     * @return mHallwayToIDList
     */
    public List<Integer> getHallwayToIDList() {
        return mHallwayToIDList;
    }

    /**
     * Getter function for the type of the entrypoint (DOOR, LIFT or STAIRS)
     * @return mType
     */
    public MeasurementType getType() {
        return mType;
    }

    /**
     * This function adds a new connection to this entrypoint.
     * It adds the position as a float[] value to the mPositionToList and
     * adds the id of the connected hallway as a int to the mHallwayToIDList.
     * @param pos (position of the connected entrypoint)
     * @param id (id of the connected hallway)
     */
    public void addConnection(float[] pos, int id) {
        mPositionToList.add(pos);
        mHallwayToIDList.add(id);
    }

    /**
     * This function draws the entrypoint as a point on a given canvas and optionally also its name
     * @param canvas (Canvas where the entrypoint will be drawn.)
     * @param paint (Color settings and size of the point)
     * @param planCenter (Precalculated center of the canvas)
     * @param scale (Precalculated scale of the plan)
     * @param showName (if true the name of the entrypoint will also be drawn)
     */
    public void drawOnCanvas(Canvas canvas, Paint paint, float[] planCenter, float scale, boolean showName) {
        float[] pos = draw2dPoint(canvas, paint, planCenter, scale);
        if(showName) {
            drawName(canvas, paint, pos);
        }
    }

    /**
     * This function draws the name of the entrypoint near to its position.
     * @param canvas (Canvas where the name will be drawn.)
     * @param paint (Color settings and size of the point)
     * @param pos (position of the entrypoint)
     */
    private void drawName(Canvas canvas, Paint paint, float[] pos) {
        canvas.drawText(mName, (float)(pos[0] - 30.0), (float)(pos[1] - 15.0), paint);
    }

    /**
     * This function draws the entrypoint as a dot on the canvas.
     * @param canvas (canvas where the entrypoint / plan is drawn)
     * @param paint (color and size settings)
     * @param planCenter (precalulated center of the plan)
     * @param scale (precalculated scale of the plan)
     */
    private float[] draw2dPoint(Canvas canvas, Paint paint, float[] planCenter, float scale) {
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        float[] newPoint = new float[3];
        for (int i = 0; i < 3; i++) {
            newPoint[i] = (mPositionFrom[i] - planCenter[i]) * scale;
        }
        float pos_x = centerX + newPoint[0];
        float pos_y = centerY + newPoint[2];
        canvas.drawPoint(pos_x, pos_y, paint);
        return new float[]{pos_x, pos_y};
    }
}
