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

import com.projecttango.unibw.graphmapper.graph.Entrypoint;
import com.projecttango.unibw.graphmapper.graph.Hallway;
import com.projecttango.unibw.graphmapper.graph.Marker;
import com.projecttango.unibw.graphmapper.graph.MeasurementType;
import com.projecttango.unibw.graphmapper.graph.Room;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder that knows how to build and scale a Hallway, Room, Entrypoint, Obstacle or Marker given a list of WallMeasurements.
 * This class is from the Java Floorlan example (https://github.com/googlesamples/tango-examples-java) and was edited by the author.
 *
 * Added changes of Google's Okul update from June 9th (13.06.2016).
 *
 * @author Konstantin Klinger
 * @version 3.0
 */
public class GraphBuilder {

    /** Scale factor */
    private static final float RENDER_PADDING_SCALE_FACTOR = 0.8f;

    /** Epsilon to compare equality of float values */
    private static final float EPSILON = 0.000001f;

    /**
     * This function calculates the center of a plan with a given list of points.
     * @param list (point list with all points from the plan)
     * @return Center of the plan as a float[] 3D vector
     */
    public static float[] getPlanCenter(List<float[]> list) {
        float[] bounds = getPlanBounds(list);
        return new float[]{(bounds[0] + bounds[1]) / 2, (bounds[2] + bounds[3]) / 2,
                (bounds[4] + bounds[5]) / 2};
    }

    /**
     * This function calculates the scale of a floorplan with a given list of points.
     * @param height (height of the canvas)
     * @param width (weight of the canvas)
     * @param list (point list with all points from the plan)
     * @return scale of the plan (float value)
     */
    public static float getPlanScale(int height, int width, List<float[]> list) {
        float[] bounds = getPlanBounds(list);
        float xScale = RENDER_PADDING_SCALE_FACTOR * width / (bounds[1] - bounds[0]);
        float zScale = RENDER_PADDING_SCALE_FACTOR * height / (bounds[5] - bounds[4]);
        return xScale < zScale ? xScale : zScale;
    }

    /**
     * This function calculates a bounding box around all points of a hallway or level.
     * So the all elements can be fitted on the display of the tablet with a specific scale.
     * @param list (point list with all points from the plan)
     * @return float array with start and end values for x, y, and z coordinates in a calculated scale
     */
    private static float[] getPlanBounds(List<float[]> list) {
        float xStart = Float.MAX_VALUE;
        float yStart = Float.MAX_VALUE;
        float zStart = Float.MAX_VALUE;
        float xEnd = Float.MIN_VALUE;
        float yEnd = Float.MIN_VALUE;
        float zEnd = Float.MIN_VALUE;
        for (float[] point : list) {
            if (point[0] < xStart) {
                xStart = point[0];
            }
            if (point[0] > xEnd) {
                xEnd = point[0];
            }
            if (point[1] < yStart) {
                yStart = point[1];
            }
            if (point[1] > yEnd) {
                yEnd = point[1];
            }
            if (point[2] < zStart) {
                zStart = point[2];
            }
            if (point[2] > zEnd) {
                zEnd = point[2];
            }
        }
        return new float[]{xStart, xEnd, yStart, yEnd, zStart, zEnd};
    }

    /**
     * Creates a list of rooms from the wall measurements of the current hallway.
     * @param roomMeasurementList (wall measurements of the rooms)
     * @return List of Rooms
     */
    public static List<Room> buildRooms(List<WallMeasurement> roomMeasurementList) {
        List<Room> roomList = new ArrayList<Room>();
        for(WallMeasurement roomMeasurement : roomMeasurementList) {
            float[] openGlWall = roomMeasurement.getPlaneTransform();
            float[] position = new float[]{openGlWall[12], openGlWall[13], openGlWall[14]};
            String number = roomMeasurement.getText();
            Room room = new Room(position, number);
            roomList.add(room);
        }
        return roomList;
    }

    /**
     * Creates a list of markers from the wall measurements of the current hallway.
     * @param markerMeasurementList (wall measurements of the markers)
     * @return List of markers
     */
    public static List<Marker> buildMarkers(List<WallMeasurement> markerMeasurementList) {
        List<Marker> markerList = new ArrayList<Marker>();
        for(WallMeasurement markerMeasurement : markerMeasurementList) {
            float[] openGlWall = markerMeasurement.getPlaneTransform();
            float[] position = new float[]{openGlWall[12], openGlWall[13], openGlWall[14]};
            String number = markerMeasurement.getText();
            Marker marker = new Marker(position, number);
            markerList.add(marker);
        }
        return markerList;
    }

    /**
     * Creates a list of entrypoints from the wall measurements of the current hallway.
     * @param contextActivity (context - needed to use functions from the GraphmapperActivity)
     * @param currentHallway (hallway where the entrypoint is located)
     * @param entryMeasurementList (wall measurements of the entrypoints)
     * @return List of entrypoints
     */
    public static List<Entrypoint> buildEntrys(GraphmapperActivity contextActivity, Hallway currentHallway, List<WallMeasurement> entryMeasurementList) {
        List<Entrypoint> entryList = new ArrayList<Entrypoint>();
        for(WallMeasurement entryMeasurement: entryMeasurementList) {
            float[] openGlWall = entryMeasurement.getPlaneTransform();
            float[] positionFrom = new float[]{openGlWall[12], openGlWall[13], openGlWall[14]};

            if((entryMeasurement.getPositionToList() == null) || (entryMeasurement.getHallwayToList() == null) ||
                    (entryMeasurement.getPositionToList().isEmpty()) || (entryMeasurement.getHallwayToList().isEmpty())) {
                //entrypoint not connected yet
                Entrypoint entrypoint = new Entrypoint(entryMeasurement.getText(), entryMeasurement.getMeasurementType(),
                        positionFrom, currentHallway.getID(), null, null);
                entryList.add(entrypoint);
            } else {
                //entrypoint has a connection yet (just one side)
                List<Integer> idList = new ArrayList<Integer>();
                for(Hallway hallway : entryMeasurement.getHallwayToList()) {
                    idList.add(hallway.getID());
                }
                Entrypoint entrypoint = new Entrypoint(entryMeasurement.getText(), entryMeasurement.getMeasurementType(),
                        positionFrom, currentHallway.getID(), entryMeasurement.getPositionToList(), idList);
                entryList.add(entrypoint);
                if(entrypoint.getType() == MeasurementType.LIFT) {
                    //Set the other side of the connection for type LIFT (There can be more than one connection)
                    for(int i = 0; i < idList.size(); i++) {
                        Hallway connection = contextActivity.getGraph().searchHallway(idList.get(i));
                        for(Entrypoint e : connection.getConnections()) {
                            if ((Math.abs(e.getPositionFrom()[0] - entryMeasurement.getPositionToList().get(i)[0]) < EPSILON) &&
                                    (Math.abs(e.getPositionFrom()[1] - entryMeasurement.getPositionToList().get(i)[1]) < EPSILON) &&
                                    (Math.abs(e.getPositionFrom()[2] - entryMeasurement.getPositionToList().get(i)[2]) < EPSILON)) {
                                //found connected entrypoint
                                e.addConnection(positionFrom, currentHallway.getID());
                                break;
                            }
                        }
                    }
                } else {
                    //Set the other side of the connection for type DOOR and STAIRS
                    Hallway connection = contextActivity.getGraph().searchHallway(idList.get(0));
                    if (connection != null) {
                        for (Entrypoint e : connection.getConnections()) {
                            if ((Math.abs(e.getPositionFrom()[0] - entryMeasurement.getPositionToList().get(0)[0]) < EPSILON) &&
                                    (Math.abs(e.getPositionFrom()[1] - entryMeasurement.getPositionToList().get(0)[1]) < EPSILON) &&
                                    (Math.abs(e.getPositionFrom()[2] - entryMeasurement.getPositionToList().get(0)[2]) < EPSILON)) {
                                //found connected entrypoint
                                e.addConnection(positionFrom, currentHallway.getID());
                                break;
                            }
                        }
                    }
                }
            }
        }
        return entryList;
    }

    /**
     * Creates a new Hallway object based on the measurements that we have so far.
     * It intersects all wall measurements (planes) to get the corners of the hallway.
     * @param wallMeasurementList List of WallMeasurements to use as input to build the hallway.
     *                            It must have only one measurement per wall.
     * @return New Hallway created by the function.
     */
    public static Hallway buildHallway(List<WallMeasurement> wallMeasurementList) {
        List<float[]> planPoints = new ArrayList<float[]>();
        WallMeasurement lastWallMeasurement = null;
        // Intersect every measurement with the previous one and add the result to the hallway.
        if (!wallMeasurementList.isEmpty()) {
            boolean first = true;
            float[] lastAddedPoint = null;
            for (WallMeasurement wallMeasurement : wallMeasurementList) {
                if (lastWallMeasurement != null) {
                    if (!first) {
                        planPoints.remove(lastAddedPoint);
                    }
                    planPoints.add(wallMeasurement.intersect(lastWallMeasurement));
                    first = false;
                }
                float[] openGlWall = wallMeasurement.getPlaneTransform();
                float[] measurementPoint = new float[]{openGlWall[12], openGlWall[13], openGlWall[14]};
                planPoints.add(measurementPoint);
                lastWallMeasurement = wallMeasurement;
                lastAddedPoint = measurementPoint;
            }

            //closing the hallway, intersect the first and last measurements.
            planPoints.remove(lastAddedPoint);
            planPoints.add(lastWallMeasurement.intersect(wallMeasurementList.get(0)));
            planPoints.remove(planPoints.get(0));
        }
        return new Hallway(planPoints);
    }

    /**
     * Creates obstacle points based on the measurements that we have so far.
     * @param cutMeasurementList List of WallMeasurements to use as input to build the obstacle points.
     *                            It must have only one measurement per wall.
     * @return List of corner points from the obstacle.
     */
    public static List<float[]> buildObstacle(List<WallMeasurement> cutMeasurementList) {
        List<float[]> obstaclePoints = new ArrayList<float[]>();
        WallMeasurement lastCutMeasurement = null;
        // Intersect every measurement with the previous one and add the result to the plan.
        if (!cutMeasurementList.isEmpty()) {
            boolean first = true;
            float[] lastAddedPoint = null;
            for (WallMeasurement cutMeasurement : cutMeasurementList) {
                if (lastCutMeasurement != null) {
                    if (!first) {
                        obstaclePoints.remove(lastAddedPoint);
                    }
                    obstaclePoints.add(cutMeasurement.intersect(lastCutMeasurement));
                    first = false;
                }
                float[] openGlWall = cutMeasurement.getPlaneTransform();
                float[] measurementPoint = new float[]{openGlWall[12], openGlWall[13], openGlWall[14]};
                obstaclePoints.add(measurementPoint);
                lastCutMeasurement = cutMeasurement;
                lastAddedPoint = measurementPoint;
            }
            //intersect the first and last measurements.
            obstaclePoints.remove(lastAddedPoint);
            obstaclePoints.add(lastCutMeasurement.intersect(cutMeasurementList.get(0)));
            obstaclePoints.remove(obstaclePoints.get(0));
        }
        return obstaclePoints;
    }

}
