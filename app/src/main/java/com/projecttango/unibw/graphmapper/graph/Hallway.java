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
 * <p>A hallway is a closed area in a building. It can have connections to other hallway with entrypoints.
 * It can also have markers and rooms and cutted obstacles within it.
 * A hallway can be seen as a node of the navigation graph / network. </p>
 *
 * <p>This class is based on the class "Floorplan" of the Jave floorplan example found on https://github.com/googlesamples/tango-examples-java.
 * Added changes of Google's Okul update from June 9th (13.06.2016).
 * This class was created by Konstantin Klinger on 25.04.16. </p>
 * @author Konstantin Klinger
 * @version 2.0
 */
public class Hallway {
    /** List of Points (usually corners) of the hallway. Each point is a float[3] with x, z and y coordinates. */
    private List<float[]> mHallwayPoints = new ArrayList<float[]>();
    /** Name of the hallway */
    private String mName;
    /** Level where the hallway is located */
    private int mLevel;
    /** List of rooms at this hallway */
    private List<Room> mRooms = new ArrayList<Room>();
    /** List of entrypoints at this hallway */
    private List<Entrypoint> mConnections = new ArrayList<Entrypoint>();
    /** List of obstacles that muste be cutted out of this hallway */
    private List<List<float[]>> mCuttedObstacleList = new ArrayList<List<float[]>>(); //Cut (Obstacles)
    /** List of markers (points of interest, e.g. position of bluetooth beacons) */
    private List<Marker> mMarkers = new ArrayList<Marker>();
    /** Unique ID of this hallway to avoid recursive connections between hallways. So the graph could be saved with GSON */
    private int mID;

    /**
     * Constructor (Creates new hallway object with a unique ID and given corner points)
     * @param points
     */
    public Hallway(List<float[]> points) {
        mHallwayPoints.addAll(points);
        mID = Graph.getNextID();
    }

    /**
     * This function adds a List of obstacles to the hallway. Every obstacles contains a List of Points (float[]).
     * @param obstacleList
     */
    public void addObstacleList(List<List<float[]>> obstacleList) {
        mCuttedObstacleList.addAll(obstacleList);
    }

    /**
     * Getter function for the unique ID
     * @return mID
     */
    public int getID() {
        return mID;
    }

    /**
     * This function adds a list of entrypoints to the hallway.
     * @param entrys
     */
    public void addConnections(List<Entrypoint> entrys) {
        mConnections.addAll(entrys);
    }

    /**
     * Getter function for the list of entrypoints (connections) of this hallway.
     * @return mConnections
     */
    public List<Entrypoint> getConnections() {
        return mConnections;
    }

    /**
     * This function adds a list of rooms to the hallway.
     * @param rooms
     */
    public void addRooms(List<Room> rooms) {
        mRooms.addAll(rooms);
    }

    /**
     * This function adds a list of markers to the hallway.
     * @param markers
     */
    public void addMarkers(List<Marker> markers) {
        mMarkers.addAll(markers);
    }

    /**
     * Getter function for the list of markers of this hallway.
     * @return mMarkers
     */
    public List<Marker> getMarkers() {
        return mMarkers;
    }

    /**
     * Getter function for the list of rooms of this hallway.
     * @return mRooms
     */
    public List<Room> getRooms() {
        return mRooms;
    }

    /**
     * Setter function for the level (mLevel) of this hallway.
     * @param level
     */
    public void setLevel(int level) {
        mLevel = level;
    }

    /**
     * Getter function for the level of this hallway.
     * @return mLevel
     */
    public int getLevel() {
        return mLevel;
    }

    /**
     * Setter function for the name (mName) of this hallway.
     * @param name
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Getter function for the name of this hallway
     * @return mName
     */
    public String getName() {
        return mName;
    }

    /**
     * This is the public plan drawing functions. It draws the plan with the user's wished options.
     * @param canvas (canvas where the plan is drawn)
     * @param paint (color and size settings)
     * @param planCenter (precalulated center of the plan)
     * @param scale (precalculated scale of the plan)
     * @param showText (if true the lenght of the lines are shown)
     * @param showName (if true the name of the hallway will also be drawn)
     * @param showObstacles (if true the obstacles of the hallway will also be drawn)
     */
    public void drawOnCanvas(Canvas canvas, Paint paint, float[] planCenter, float scale, boolean showText, boolean showName, boolean showObstacles) {
        float[] pos = draw2dlines(canvas, paint, planCenter, scale);
        if(showText) {
            drawTexts(canvas, paint, planCenter, scale);
        }
        if(showName) {
            drawName(canvas, paint, pos);
        }
        if(showObstacles) {
            draw2dlinesObstacles(canvas, paint, planCenter, scale);
            drawTextsObstacles(canvas, paint, planCenter, scale);
        }
    }

    /**
     * This function draws the name of the hallway in the middle of it.
     * @param canvas (Canvas where the name will be drawn.)
     * @param paint (Color settings and size of the point)
     * @param pos (position of the text)
     */
    private void drawName(Canvas canvas, Paint paint, float[] pos) {
        canvas.drawText(mName, (float)(pos[0] - 30.0), (float)(pos[1] - 15.0), paint);
    }

    /**
     * This function draws a line between each corner of the hallway.
     * @param canvas (canvas where the plan is drawn)
     * @param paint (color and size settings)
     * @param planCenter (precalulated center of the plan)
     * @param scale (precalculated scale of the plan)
     * @return x and y position of the last point to draw the name of the hallway above it
     */
    private float[] draw2dlines(Canvas canvas, Paint paint, float[] planCenter, float scale) {
        // Get center of the canvas.
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        float[] lines = new float[4 * mHallwayPoints.size()];
        int i = 0;
        float pos_x = 0;
        float pos_y = 0;
        if (!mHallwayPoints.isEmpty()) {
            float[] lastPoint = null;
            Hallway scaledHallway = translateAndScaleHallway(planCenter, scale, mHallwayPoints);
            // For every point add a line to the last point. Start from the center of the canvas.
            for (float[] nextPoint : scaledHallway.mHallwayPoints) {
                if (lastPoint != null) {
                    lines[i++] = centerX + lastPoint[0];
                    lines[i++] = centerY + lastPoint[2];
                    lines[i++] = centerX + nextPoint[0];
                    lines[i++] = centerY + nextPoint[2];
                }
                lastPoint = nextPoint;
            }
            pos_x = centerX + lastPoint[0];
            pos_y = centerY + lastPoint[2];
            lines[i++] = pos_x;
            lines[i++] = pos_y;
            lines[i++] = centerX + scaledHallway.mHallwayPoints.get(0)[0];
            lines[i++] = centerY + scaledHallway.mHallwayPoints.get(0)[2];

            if(i >= 15) {
                if(lines[6] > lines[0]) {
                    pos_x = (float) (((lines[6] - lines[0]) * 0.5) + lines[0] - (mName.length() * 5));
                } else {
                    pos_x = (float) (((lines[0] - lines[6]) * 0.5) + lines[6] - (mName.length() * 5));
                }
                if(lines[7] > lines[1]) {
                    pos_y = (float) (((lines[7] - lines[1]) * 0.5) + lines[1] - (mName.length() * 5));
                } else {
                    pos_y = (float) (((lines[1] - lines[7]) * 0.5) + lines[7] - (mName.length() * 5));
                }
            }
        }
        canvas.drawLines(lines, paint);
        return new float[]{pos_x, pos_y};
    }

    /**
     * This function draws every obstacle on the canvas. It draws a line between each corner of every obstacle.
     * @param canvas (canvas where the plan is drawn)
     * @param paint (color and size settings)
     * @param planCenter (precalulated center of the plan)
     * @param scale (precalculated scale of the plan)
     */
    private void draw2dlinesObstacles(Canvas canvas, Paint paint, float[] planCenter, float scale) {
        // Get center of the canvas.
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        for(List<float[]> obstacle : mCuttedObstacleList) {
            float[] lines = new float[4 * obstacle.size()];
            int i = 0;
            if (!obstacle.isEmpty()) {
                float[] lastPoint = null;
                Hallway scaledHallway = translateAndScaleHallway(planCenter, scale, obstacle);
                // For every point add a line to the last point. Start from the center of the canvas.
                for (float[] nextPoint : scaledHallway.mHallwayPoints) {
                    if (lastPoint != null) {
                        lines[i++] = centerX + lastPoint[0];
                        lines[i++] = centerY + lastPoint[2];
                        lines[i++] = centerX + nextPoint[0];
                        lines[i++] = centerY + nextPoint[2];
                    }
                    lastPoint = nextPoint;
                }
                lines[i++] = centerX + lastPoint[0];
                lines[i++] = centerY + lastPoint[2];
                lines[i++] = centerX + scaledHallway.mHallwayPoints.get(0)[0];
                lines[i++] = centerY + scaledHallway.mHallwayPoints.get(0)[2];
            }
            canvas.drawLines(lines, paint);
        }
    }

    /**
     * This function calculates the length of the lines (from corner to corner) of the hallway and draws it in the middle of each line on the canvas.
     * @param canvas (canvas where the text is drawn)
     * @param paint (color and size settings)
     * @param planCenter (precalculated center of the plan)
     * @param scale (precalculated scale of the plan)
     */
    private void drawTexts(Canvas canvas, Paint paint, float[] planCenter, float scale) {
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        if (!mHallwayPoints.isEmpty()) {
            float[] lastPoint = null;
            float[] nextPoint;
            float[] lastScaledPoint = null;
            float[] nextScaledPoint;
            Hallway scaledHallway = translateAndScaleHallway(planCenter, scale, mHallwayPoints);
            for (int i = 0; i < mHallwayPoints.size(); i++) {
                nextPoint = mHallwayPoints.get(i);
                nextScaledPoint = scaledHallway.mHallwayPoints.get(i);
                if (lastPoint != null) {
                    // Get the length of the original unscaled plan.
                    double length = Math.sqrt(
                            (lastPoint[0] - nextPoint[0]) * (lastPoint[0] - nextPoint[0]) +
                                    (lastPoint[2] - nextPoint[2]) * (lastPoint[2] - nextPoint[2]));
                    // Draw the label in the middle of each wall.
                    double posX = centerX + (lastScaledPoint[0] + nextScaledPoint[0]) / 2;
                    double posY = centerY + (lastScaledPoint[2] + nextScaledPoint[2]) / 2;
                    canvas.drawText(String.format("%.2f", length) + "m", (float) posX, (float) posY, paint);
                }
                lastPoint = nextPoint;
                lastScaledPoint = nextScaledPoint;
            }
            // Get the length of the original unscaled plan.
            double length = Math.sqrt((lastPoint[0] - mHallwayPoints.get(0)[0]) * (lastPoint[0] - mHallwayPoints.get(0)[0]) +
                            (lastPoint[2] - mHallwayPoints.get(0)[2]) * (lastPoint[2] - mHallwayPoints.get(0)[2]));
            // Draw the label in the middle of each wall.
            double posX = centerX + (lastScaledPoint[0] + scaledHallway.mHallwayPoints.get(0)[0]) / 2;
            double posY = centerY + (lastScaledPoint[2] + scaledHallway.mHallwayPoints.get(0)[2]) / 2;
            canvas.drawText(String.format("%.2f", length) + "m", (float) posX, (float) posY, paint);
        }
    }

    /**
     * This function calculates the length of the lines (from corner to corner) of the hallway's obstacles and draws it in the middle of each line on the canvas.
     * @param canvas (canvas where the text is drawn)
     * @param paint (color and size settings)
     * @param planCenter (precalculated center of the plan)
     * @param scale (precalculated scale of the plan)
     */
    private void drawTextsObstacles(Canvas canvas, Paint paint, float[] planCenter, float scale) {
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        for(List<float[]> obstacle : mCuttedObstacleList) {
            if (!obstacle.isEmpty()) {
                float[] lastPoint = null;
                float[] nextPoint;
                float[] lastScaledPoint = null;
                float[] nextScaledPoint;
                Hallway scaledHallway = translateAndScaleHallway(planCenter, scale, obstacle);
                for (int i = 0; i < obstacle.size(); i++) {
                    nextPoint = obstacle.get(i);
                    nextScaledPoint = scaledHallway.mHallwayPoints.get(i);
                    if (lastPoint != null) {
                        // Get the length of the original unscaled plan.
                        double length = Math.sqrt(
                                (lastPoint[0] - nextPoint[0]) * (lastPoint[0] - nextPoint[0]) +
                                        (lastPoint[2] - nextPoint[2]) * (lastPoint[2] - nextPoint[2]));
                        // Draw the label in the middle of each wall.
                        double posX = centerX + (lastScaledPoint[0] + nextScaledPoint[0]) / 2;
                        double posY = centerY + (lastScaledPoint[2] + nextScaledPoint[2]) / 2;
                        canvas.drawText(String.format("%.2f", length) + "m", (float) posX, (float) posY, paint);
                    }
                    lastPoint = nextPoint;
                    lastScaledPoint = nextScaledPoint;
                }
                // Get the length of the original unscaled plan.
                double length = Math.sqrt((lastPoint[0] - obstacle.get(0)[0]) * (lastPoint[0] - obstacle.get(0)[0]) +
                        (lastPoint[2] - obstacle.get(0)[2]) * (lastPoint[2] - obstacle.get(0)[2]));
                // Draw the label in the middle of each wall.
                double posX = centerX + (lastScaledPoint[0] + scaledHallway.mHallwayPoints.get(0)[0]) / 2;
                double posY = centerY + (lastScaledPoint[2] + scaledHallway.mHallwayPoints.get(0)[2]) / 2;
                canvas.drawText(String.format("%.2f", length) + "m", (float) posX, (float) posY, paint);
            }
        }
    }

    /**
     * This function calculates a new hallway for a list of unscaled hallway points with a predefined / precalculated planCenter and scale.
     * @param planCenter (precalculated center of the plan / canvas)
     * @param scale (precalculated scale of the plan / canvas)
     * @param unscaledPoints (list of unscaled points)
     * @return new hallway with all points in the precalculated scale
     */
    private Hallway translateAndScaleHallway(float[] planCenter, float scale, List<float[]> unscaledPoints) {
        List<float[]> scaledPoints = new ArrayList<float[]>();
        for (float[] nextPoint : unscaledPoints) {
            float[] newPoint = new float[3];
            for (int i = 0; i < 3; i++) {
                newPoint[i] = (nextPoint[i] - planCenter[i]) * scale;
            }
            scaledPoints.add(newPoint);
        }
        return new Hallway(scaledPoints);
    }

    /**
     * Getter function for the hallway points of this hallway (mHallwayPoints).
     * @return new List of the hallway points from this Hallway.
     */
    public List<float[]> getHallwayPoints() {
        return new ArrayList<float[]>(mHallwayPoints);
    }
}
