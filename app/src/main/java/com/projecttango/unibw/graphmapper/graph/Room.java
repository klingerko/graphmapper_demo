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

/**
 * <p>A room object can be placed on a door for example. It will be part of the navigation graph.
 * The user can mark rooms that will be shown on the floor plan later.</p>
 *
 * <p>Added changes of Google's Okul update from June 9th (13.06.2016).
 * This class was created by Konstantin Klinger on 25.04.16.</p>
 *
 * @author Konstantin Klinger
 * @version 2.0
 */
public class Room {
    /** 3D Position of the Room (in respect to the start of the ADF) */
    private float[] mPosition;
    /** Name or number of the room */
    private String mNumber;

    /**
     * Constructor (creates a new room object with the given position and number)
     * @param position
     * @param number
     */
    public Room(float[] position, String number) {
        mPosition = position;
        mNumber = number;
    }

    /**
     * mPosition Getter function
     * @return mPosition (Gets the position of the room back.)
     */
    public float[] getPosition() {
        return mPosition;
    }

    /**
     * mNumber Setter function
     * @param number (Sets the number / name of the room.)
     */
    public void setNumber(String number) {
        mNumber = number;
    }

    /**
     * mNumber Getter function
     * @return mNumber (Gets the number / name of the room back.)
     */
    public String getNumber() {
        return mNumber;
    }

    /**
     * This function draws the room as a point on a given canvas and optionally also its name
     * @param canvas (Canvas where the room will be drawn.)
     * @param paint (Color settings and size of the point)
     * @param planCenter (Precalculated center of the canvas)
     * @param scale (Precalculated scale of the plan)
     * @param showName (if true the name of the room will also be drawn)
     */
    public void drawOnCanvas(Canvas canvas, Paint paint, float[] planCenter, float scale, boolean showName) {
        float[] pos = draw2dPoint(canvas, paint, planCenter, scale);
        if(showName) {
            drawName(canvas, paint, pos);
        }
    }

    /**
     * This function draws the name of the room near to its point.
     * @param canvas (Canvas where the name will be drawn.)
     * @param paint (Color settings and size of the point)
     * @param pos (position of the room point)
     */
    private void drawName(Canvas canvas, Paint paint, float[] pos) {
        canvas.drawText(mNumber, (float)(pos[0] - 30.0), (float)(pos[1] - 15.0), paint);
    }

    /**
     * This function draws the position of the room as a point on a given canvas with a predefined scale.
     * @param canvas (Canvas where the point will be drawn.)
     * @param paint (Color settings and size of the point)
     * @param planCenter (Precalculated center of the canvas)
     * @param scale (Precalculted scale of the plan)
     * @return float array of the x and y coordinate of the room point
     */
    private float[] draw2dPoint(Canvas canvas, Paint paint, float[] planCenter, float scale) {
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        float[] newPoint = new float[3];
        for (int i = 0; i < 3; i++) {
            newPoint[i] = (mPosition[i] - planCenter[i]) * scale;
        }
        float pos_x = centerX + newPoint[0];
        float pos_y = centerY + newPoint[2];
        canvas.drawPoint(pos_x, pos_y, paint);
        return new float[]{pos_x, pos_y};
    }
}
