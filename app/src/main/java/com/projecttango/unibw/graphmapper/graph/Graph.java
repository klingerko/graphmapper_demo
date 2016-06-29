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

import java.util.HashMap;

/**
 * <p>Top class for the navigation graph.</p>
 *
 * <p>It contains all elements of the navigation network and can be stored in JSON (Gson) format
 * and can be loaded again in this version. In future version the graph should be converted to an indoor navigation
 * data format (e.g. IndoorGML or Indoor OSM).</p>
 *
 * <p>This class was created by Konstantin Klinger on 25.04.16. </p>
 * @author Konstantin Klinger
 * @version 1.0
 */
public class Graph {
    /** This variable counts the number of hallway to set their unique IDs */
    private static int mHallwayIDCounter;
    /** Hallway Network (Navigation Graph)
     * The network is currently HashMap with ID / Hallway pairs.
     * This should be adapted in the future (better storage format and convert the graph into an official indoor navigation data format)
     */
    private HashMap<Integer, Hallway> mHallwayNet;
    /** When saving the graph, the ID must also be saved. */
    private int mSavedIDCounter;

    /**
     * Constructor (This function creates a new navigation graph object.)
     */
    public Graph() {
        mHallwayIDCounter = 0;
        mHallwayNet = new HashMap<Integer, Hallway>();
        mSavedIDCounter = 0;
    }

    /**
     * Add a hallway to the indoor navigation graph network and set its unique ID.
     * @param hallway (hallway to be added)
     */
    public void addHallway(Hallway hallway) {
        mHallwayNet.put(hallway.getID(), hallway);
    }

    /**
     * Returns the current navigation graph as a HashMap.
     * @return mHallwayNet (hallway network)
     */
    public HashMap<Integer, Hallway> getGraph() {
        return mHallwayNet;
    }

    /**
     * With this function you can search for a hallway with a given ID in the navigation graph.
     * @param id (search id)
     * @return Hallway with the searched ID or null if there isn't any hallway with this ID.
     */
    public Hallway searchHallway(int id) {
        if(mHallwayNet.containsKey(id)) {
            return mHallwayNet.get(id);
        } else {
            return null;
        }
    }

    /**
     * This function returns the value of the ID-Counter (current hallway ID) increments it afterwards.
     * @return ID of the next hallway.
     */
    public static int getNextID() {
        int temp = mHallwayIDCounter;
        mHallwayIDCounter++;
        return temp;
    }

    /**
     * This function stores the current value of mHallwayIDCounter into mSavedIDCounter (save ID).
     */
    public void saveIDCounter() {
        mSavedIDCounter = mHallwayIDCounter;
    }

    /**
     * This function sets mHallwayIDCounter to the value of mSavedIDCounter (load ID).
     */
    public void loadIDCounter() {
        mHallwayIDCounter = mSavedIDCounter;
    }

    /**
     * This function is a small demo print function to visualize the graph.
     * All containing hallways with their connections are printed.
     * @return String with the printed text
     */
    public String printGraph() {
        String s = "";
        for(Hallway hallway : mHallwayNet.values()) {
            s += "Hallway \"" + hallway.getName() + "\" (id: " + hallway.getID() +
                    ", level: " + hallway.getLevel() + ") connected to: ";
            boolean first = true;
            for(Entrypoint entrypoint : hallway.getConnections()) {
                if(first) {
                    first = false;
                } else {
                    s += "; ";
                }
                boolean start = true;
                for(int id : entrypoint.getHallwayToIDList()) {
                    if(start) {
                        start = false;
                    } else {
                        s += ", ";
                    }
                    s += "id: " + id;
                }
            }
            s += "\n\r";
        }
        return s;
    }
}
