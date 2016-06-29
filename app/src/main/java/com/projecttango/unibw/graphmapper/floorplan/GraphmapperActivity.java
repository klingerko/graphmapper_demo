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

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.surface.RajawaliSurfaceView;
import android.opengl.Matrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.projecttango.unibw.graphmapper.dialog.ADFPair;
import com.projecttango.unibw.graphmapper.dialog.Dialog;
import com.projecttango.unibw.graphmapper.graph.MeasurementType;
import com.projecttango.unibw.graphmapper.graph.Entrypoint;
import com.projecttango.unibw.graphmapper.graph.Graph;
import com.projecttango.unibw.graphmapper.graph.Hallway;
import com.projecttango.unibw.graphmapper.graph.Marker;
import com.projecttango.unibw.graphmapper.graph.Room;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;
import com.projecttango.tangosupport.TangoSupport.IntersectionPointPlaneModelPair;

/**
 * <p>A small demo application that allows the user to create a navigation graph of indoor environments
 * and a 2D floor plan of a building.</p>
 *
 * <p>The user can mark his point of interests (rooms, entrypoints, etc.) and connect hallways to build a
 * hallway network of the building. This is called indoor navigation graph or network.
 * As a result the user can save the adf with the navigation graph and load it in the next sessions.
 * He can also draw the floor plan and save it as a jpg file. The big aim is to create a graph of an
 * indoor environment that a indoor navigation system can use this graph as a map to navigate.
 * The application can also be started in a viewer mode. After loading an existing adf and graph and
 * relocalization in the area the user can view the floor plans and see his current position in it.</p>
 *
 * <p>The get wall measurements or POI measurements you have to click on the wall. After that there will be a plane
 * fitting on the wall. The application uses TangoSupportLibrary to do plane fitting using the point cloud data.
 * When the user clicks on the display, plane detection is done on the surface at the location of
 * the click and a 3D object will be placed in the scene anchored at that location. A Wall Measurement
 * will be recorded for that plane.</p>
 *
 * <p>You need to take exactly one measurement per wall in clockwise order. After you have taken all the
 * measurements you can press the 'Done' button and the adf and graph will be saved and the final result
 * can be drawn as a 2D floor plan along with labels showing the sizes of the walls and names of the POIs
 * and hallways.</p>
 *
 * <p>Note that it is important to include the KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION configuration
 * parameter in order to achieve the best results synchronizing the Rajawali virtual world with the
 * RGB camera.</p>
 *
 * <p>Added changes of Google's Okul update from June 9th (13.06.2016).
 * This class is from the Java Floorlan example (https://github.com/googlesamples/tango-examples-java) and was edited by the author. </p>
 * @author Konstantin Klinger
 * @version 3.0
 */
public class GraphmapperActivity extends Activity implements View.OnTouchListener {
    /** Tag of this class / activity */
    private static final String TAG = GraphmapperActivity.class.getSimpleName();
    /** ID for invalid textures */
    private static final int INVALID_TEXTURE_ID = 0;
    /** path to the location in the internal storage where the graph and jpg files are stored */
    private static final String mStoragePath = "/Graphmapper";
    /** counter to count images that were saved, so that they haven't the same name */
    private static int mCountJPG = 1;
    /** Surface View from Rajawali Library (https://github.com/Rajawali/Rajawali) */
    private RajawaliSurfaceView mSurfaceView;
    /** Renderer object (displays the AR objects on the view) */
    private GraphmapperRenderer mRenderer;
    /** intrinsic parameters of the camera */
    private TangoCameraIntrinsics mIntrinsics;
    /** manager to handle the tango point cloud data */
    private TangoPointCloudManager mPointCloudManager;
    /** tango system object */
    private Tango mTango;
    /** Signals if the tango system is connected */
    private boolean mIsConnected = false;
    /** timestamp of the pose data */
    private double mCameraPoseTimestamp = 0;

    // Texture rendering related fields
    // NOTE: Naming indicates which thread is in charge of updating this variable
    /** Signals if the tango device and the gl thread are connected */
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    /** Signals if a new frame is available */
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);
    /** timestamp of the rgb color camera */
    private double mRgbTimestampGlThread;

    /** indoor navigation graph object (hallway network) */
    private Graph mGraph;
    /** current recording level */
    private int mCurrentLevel = 0;
    /** current hallway recording */
    private Hallway mCurrentHallway;
    /** current type of action selected (Wall, Room, Entry, Marker, Cut) */
    private MeasurementType mCurrentActionType;
    /** list of current wall measurements */
    private List<WallMeasurement> mWallMeasurementList;
    /** list of current room measurements */
    private List<WallMeasurement> mRoomMeasurementList;
    /** list of current entrypoint measurements */
    private List<WallMeasurement> mEntryMeasurementList;
    /** list of current marker (POI) measurements */
    private List<WallMeasurement> mMarkerMeasurementList;
    /** list of current cutted obstacle wall measurements */
    private List<WallMeasurement> mCutMeasurementList;
    /** list of all current obstacles */
    private List<List<float[]>> mCutObstacleList;

    //Buttons for Main View (floorplan_activity_main layout)
    /** button to select room action */
    private Button mRoomButton;
    /** button to select entrypoint action */
    private Button mEntryButton;
    /** button to select marker action */
    private Button mMarkerButton;
    /** button to select wall measurement action or cut action*/
    private Button mWallButton;
    /** button to select option action (horizontal / vertical entrypoint or finish cut action)  */
    private Button mOptionButton;
    /** button to add a new level and finish the current hallway */
    private Button mLevelButton;
    /** button to add a new hallway and finish the current hallway */
    private Button mAddButton;
    /** button to undo the last measurement action */
    private Button mUndoButton;
    /** button to reset all measurements in the current hallway */
    private Button mResetButton;
    /** button to finish the recording an save adf and start the draw view (floorplan_avtivity_draw layout) */
    private Button mDoneButton;
    /** info text to show the current action selected and the level */
    private TextView mInfoText;
    /** info text to show if there is an adf & graph loaded and if the device is relocalized or not */
    private TextView mADFText;

    /** Signals if the Curt action is selected */
    private boolean isCutSelected = false;
    /** all entrypoints that have to be connected afterwards */
    private Stack<Entrypoint> mUnconnectedEntrys;
    /** Saves the type of the last measurement (for undo action) */
    private MeasurementType mLastType;
    /** Saves the last measurement (for undo action) */
    private WallMeasurement mLastMeasurement;
    /** human readable name of the loaded / saved ADF file */
    private String mADFName = "unnamed";
    /** loaded ADF uuid / name pair */
    private ADFPair mLoadedADFPair;
    /** Signals if ADF loading is finished */
    private boolean mIsADFLoadingFinished = false;
    /** Signal if graph loading was successful (There could be no graph to a loaded ADF) */
    private boolean mIsSuccessGraphLoaded = false;
    /** Stops the ADF while loop (If true the ADF while loop will never stop) */
    private boolean mADFWhile = true;

    /** Task after pressing the Done Button to finish the recording */
    private FinishPlanTask mFinishPlanTask;
    /** progess view that signals the saving adf process */
    private ViewGroup mProgressGroup;

    //Buttons for Draw View (floorplan_activity_draw layout)
    /** button to show current position */
    private Button mPositionButton;
    /** button to restart the whole application */
    private Button mRestartButton;
    /** button to select a level and draw its floorplan */
    private Button mDrawButton;
    /** button to show the names of the elements */
    private Button mNameButton;
    /** button to show the length of the walls */
    private Button mLengthButton;
    /** button to show the POIs (marker, entrypoint, rooms) */
    private Button mPOIButton;
    /** button to show the obstacles of the hallways */
    private Button mObstacleButton;
    /** button to save the current floorplan view as a jpg */
    private Button mSaveButton;
    /** button to destroy the current floorplan view */
    private Button mDestroyButton;
    /** button to zoom in a hallway at a current floorplan level view */
    private Button mSelectButton;
    /** info text to show the name of the adf & graph and which level and hallway is currently drawn */
    private TextView mDrawText;

    /** signal for viewer state (if true the application will be started in viewer state and no recording is possible) */
    private boolean mIsViewer = false;
    /** view of the currently drawn floorplan */
    private PlanView mCurrentPlanDraw = null;
    /** level of the currently drawn floorplan */
    private int mCurrentLevelDraw = 0;
    /** currently drawn hallway */
    private Hallway mCurrentHallwayDraw = null;
    /** Signals if the name of the elements should be drawn or not */
    private boolean mShowNamesDraw = false;
    /** Signals if the length of the walls should be drawn or not */
    private boolean mShowLengthDraw = true;
    /** Signals if the POIs (marker, room, entrypoint) should be drawn or not */
    private boolean mShowPOIDraw = true;
    /** Signals if the obstacles should be drawn or not*/
    private boolean mShowObstaclesDraw = false;

    /**
     * This function is running after starting the application.
     * It instanciates all elements of the GraphmapperActivity object and creates the functions of the main view buttons.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.floorplan_activity_main);
        mCurrentHallway = null;
        isCutSelected = false;
        mLastType = null;
        mLastMeasurement = null;
        mADFName = "unnamed";
        mLoadedADFPair = null;
        mIsADFLoadingFinished = false;
        mIsSuccessGraphLoaded = false;
        mADFWhile = true;
        mIsViewer = false;
        mCurrentPlanDraw = null;
        mCurrentHallwayDraw = null;
        mShowNamesDraw = false;
        mShowLengthDraw = true;
        mShowPOIDraw = true;
        mShowObstaclesDraw = false;
        mCountJPG = 1;
        File graph = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + mStoragePath + "/Graphs");
        File image = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + mStoragePath + "/Images");
        File print = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + mStoragePath + "/Prints");
        if(!graph.exists()) {
            graph.mkdirs();
        }
        if(!image.exists()) {
            image.mkdirs();
        }
        if(!print.exists()) {
            print.mkdirs();
        }
        mSurfaceView = (RajawaliSurfaceView) findViewById(R.id.ar_view);
        mRenderer = new GraphmapperRenderer(this);
        mSurfaceView.setSurfaceRenderer(mRenderer);
        mSurfaceView.setOnTouchListener(this);
        // Set ZOrderOnTop to false so the other views don't get hidden by the SurfaceView.
        mSurfaceView.setZOrderOnTop(false);
        mProgressGroup = (ViewGroup) findViewById(R.id.progress_group);
        mPointCloudManager = new TangoPointCloudManager();
        mCurrentActionType = MeasurementType.WALL;
        mGraph = new Graph();
        mWallMeasurementList = new ArrayList<WallMeasurement>();
        mRoomMeasurementList = new ArrayList<WallMeasurement>();
        mEntryMeasurementList = new ArrayList<WallMeasurement>();
        mMarkerMeasurementList = new ArrayList<WallMeasurement>();
        mCutMeasurementList = new ArrayList<WallMeasurement>();
        mCutObstacleList = new ArrayList<List<float[]>>();

        mUnconnectedEntrys = new Stack<Entrypoint>();

        mInfoText = (TextView) findViewById(R.id.info_text);
        mADFText = (TextView) findViewById(R.id.adf_text);

        mRoomButton = (Button) findViewById(R.id.room_button);
        mRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(GraphmapperActivity.this, "Select the rooms in this hallway!", Toast.LENGTH_SHORT).show();
                mCurrentActionType = MeasurementType.ROOM;
                mRoomButton.setVisibility(View.INVISIBLE);
                mEntryButton.setVisibility(View.VISIBLE);
                mWallButton.setText("Wall");
                isCutSelected = true;
                mWallButton.setVisibility(View.VISIBLE);
                mMarkerButton.setVisibility(View.VISIBLE);
                mOptionButton.setVisibility(View.INVISIBLE);
                mInfoText.setText("touch action: " + mCurrentActionType.toString() + ", level selected: " + mCurrentLevel);
            }
        });
        mEntryButton = (Button) findViewById(R.id.entry_button);
        mEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(GraphmapperActivity.this, "Select the entrypoints in this hallway!", Toast.LENGTH_SHORT).show();
                mCurrentActionType = MeasurementType.ENTRY;
                mEntryButton.setVisibility(View.INVISIBLE);
                mWallButton.setText("Wall");
                isCutSelected = true;
                mWallButton.setVisibility(View.VISIBLE);
                mRoomButton.setVisibility(View.VISIBLE);
                mMarkerButton.setVisibility(View.VISIBLE);
                mOptionButton.setText("On ground");
                mOptionButton.setVisibility(View.VISIBLE);
                mRenderer.setIsObjectOnGround(false);
                mInfoText.setText("touch action: " + mCurrentActionType.toString() + ", level selected: " + mCurrentLevel);
            }
        });

        mMarkerButton = (Button) findViewById(R.id.marker_button);
        mMarkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(GraphmapperActivity.this, "Select your personal markers in this hallway!", Toast.LENGTH_SHORT).show();
                mCurrentActionType = MeasurementType.MARKER;
                mMarkerButton.setVisibility(View.INVISIBLE);
                mRoomButton.setVisibility(View.VISIBLE);
                mEntryButton.setVisibility(View.VISIBLE);
                mWallButton.setText("Wall");
                isCutSelected = true;
                mWallButton.setVisibility(View.VISIBLE);
                mOptionButton.setText("On ground");
                mOptionButton.setVisibility(View.VISIBLE);
                mRenderer.setIsObjectOnGround(false);
                mInfoText.setText("touch action: " + mCurrentActionType.toString() + ", level selected: " + mCurrentLevel);
            }
        });

        mWallButton = (Button) findViewById(R.id.wall_button);
        mWallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCutSelected) {
                    //Wall
                    isCutSelected = false;
                    mWallButton.setText("Cut");
                    Toast.makeText(GraphmapperActivity.this, "Select the walls in this hallway!", Toast.LENGTH_SHORT).show();
                    mCurrentActionType = MeasurementType.WALL;
                    mEntryButton.setVisibility(View.VISIBLE);
                    mRoomButton.setVisibility(View.VISIBLE);
                    mMarkerButton.setVisibility(View.VISIBLE);
                    mOptionButton.setVisibility(View.INVISIBLE);
                    mInfoText.setText("touch action: " + mCurrentActionType.toString() + ", level selected: " + mCurrentLevel);
                } else {
                    //Cut
                    isCutSelected = true;
                    mWallButton.setText("Wall");
                    Toast.makeText(GraphmapperActivity.this, "Select the obstacles (walls) in this hallway!", Toast.LENGTH_SHORT).show();
                    mCurrentActionType = MeasurementType.CUT;
                    mWallButton.setVisibility(View.INVISIBLE);
                    mEntryButton.setVisibility(View.INVISIBLE);
                    mRoomButton.setVisibility(View.INVISIBLE);
                    mMarkerButton.setVisibility(View.INVISIBLE);
                    mAddButton.setVisibility(View.INVISIBLE);
                    mDoneButton.setVisibility(View.INVISIBLE);
                    mResetButton.setVisibility(View.INVISIBLE);
                    mLevelButton.setVisibility(View.INVISIBLE);
                    mOptionButton.setText("Finish");
                    mOptionButton.setVisibility(View.VISIBLE);
                    mInfoText.setText("touch action: " + mCurrentActionType.toString() + ", level selected: " + mCurrentLevel);
                }
            }
        });

        mOptionButton = (Button) findViewById(R.id.option_button);
        mOptionButton.setVisibility(View.INVISIBLE);
        mOptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentActionType == MeasurementType.CUT) {
                    //finish Cut Obstacles
                    mEntryButton.setVisibility(View.VISIBLE);
                    mRoomButton.setVisibility(View.VISIBLE);
                    mMarkerButton.setVisibility(View.VISIBLE);
                    mAddButton.setVisibility(View.VISIBLE);
                    mDoneButton.setVisibility(View.VISIBLE);
                    mResetButton.setVisibility(View.VISIBLE);
                    mLevelButton.setVisibility(View.VISIBLE);
                    mOptionButton.setVisibility(View.INVISIBLE);
                    Toast.makeText(GraphmapperActivity.this, "Select the walls in this hallway!", Toast.LENGTH_SHORT).show();
                    mWallButton.setText("Cut");
                    mWallButton.setVisibility(View.VISIBLE);
                    isCutSelected = false;
                    mCurrentActionType = MeasurementType.WALL;
                    mInfoText.setText("touch action: " + mCurrentActionType.toString() + ", level selected: " + mCurrentLevel);
                    List<float[]> obstacle = GraphBuilder.buildObstacle(mCutMeasurementList);
                    mCutObstacleList.add(obstacle);
                    mCutMeasurementList = new ArrayList<WallMeasurement>();
                } else if ((mCurrentActionType == MeasurementType.ENTRY) || (mCurrentActionType == MeasurementType.MARKER)) {
                    //horizontal / vertical entrypoint or marker
                    if (mRenderer.isObjectOnGround()) {
                        //current on ground => change to walls
                        mRenderer.setIsObjectOnGround(false);
                        mOptionButton.setText("On ground");
                        Toast.makeText(GraphmapperActivity.this, "Measurements will be placed parallel to walls", Toast.LENGTH_SHORT).show();
                    } else {
                        //current on wall => change to ground
                        mRenderer.setIsObjectOnGround(true);
                        mOptionButton.setText("On wall");
                        Toast.makeText(GraphmapperActivity.this, "Measurements will be placed parallel to the ground", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });

        mLevelButton = (Button) findViewById(R.id.level_button);
        mLevelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsViewer) {
                    recreate();
                } else {
                    // Only finish the hallway if we have enough measurements.
                    if (mWallMeasurementList.size() < 3) {
                        Toast.makeText(GraphmapperActivity.this, "At least 3 wall measurements are needed to close the room", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(GraphmapperActivity.this, "New Level selected!", Toast.LENGTH_SHORT).show();
                        Dialog.newLevelDialog(GraphmapperActivity.this);
                        mCurrentActionType = MeasurementType.WALL;
                        mWallButton.setVisibility(View.VISIBLE);
                        mWallButton.setText("Cut");
                        isCutSelected = false;
                        mEntryButton.setVisibility(View.VISIBLE);
                        mRoomButton.setVisibility(View.VISIBLE);
                        mOptionButton.setVisibility(View.INVISIBLE);
                        mInfoText.setText("touch action: " + mCurrentActionType.toString() + ", level selected: " + mCurrentLevel);
                    }
                }
            }
        });

        mAddButton = (Button) findViewById(R.id.add_button);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsViewer) {
                    finishPlan();
                } else {
                    // Only finish the hallway if we have enough measurements.
                    if (mWallMeasurementList.size() < 3) {
                        Toast.makeText(GraphmapperActivity.this, "At least 3 wall measurements are needed to close the room", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(GraphmapperActivity.this, "Add Hallway selected!", Toast.LENGTH_SHORT).show();
                        Dialog.hallwayFinishDialog(GraphmapperActivity.this);
                        mCurrentActionType = MeasurementType.WALL;
                        mWallButton.setVisibility(View.VISIBLE);
                        mWallButton.setText("Cut");
                        isCutSelected = false;
                        mEntryButton.setVisibility(View.VISIBLE);
                        mRoomButton.setVisibility(View.VISIBLE);
                        mOptionButton.setVisibility(View.INVISIBLE);
                        mInfoText.setText("touch action: " + mCurrentActionType.toString() + ", level selected: " + mCurrentLevel);
                    }
                }
            }
        });

        mUndoButton = (Button) findViewById(R.id.undo_button);
        mUndoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoLastMeasurement();
            }
        });

        mResetButton = (Button) findViewById(R.id.reset_button);
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog.resetHallwayDialog(GraphmapperActivity.this);
            }
        });
        mDoneButton = (Button) findViewById(R.id.done_button);
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Only finish the hallway if we have enough measurements.
                if (mWallMeasurementList.size() < 3) {
                    Toast.makeText(GraphmapperActivity.this, "At least 3 wall measurements are needed to close the room", Toast.LENGTH_LONG).show();
                } else {
                    //finish current session (save adf, graph and draw floorpan)
                    Dialog.doneButtonDialog(GraphmapperActivity.this);
                }
            }
        });
        //Start Dialog if the user want to load an adf or start a new session
        Dialog.loadADFDialog(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Synchronize against disconnecting while the service is being used in the OpenGL thread or
        // in the UI thread.
        synchronized (this) {
            if (mIsConnected) {
                mRenderer.getCurrentScene().clearFrameCallbacks();
                mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                // We need to invalidate the connected texture ID so that we cause a re-connection
                // in the OpenGL thread after resume
                mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                mTango.disconnect();
                mIsConnected = false;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if it has permissions.
        // Area learning permissions are needed in order to save the adf.
        if (Tango.hasPermission(this, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)) {
            connectAndStart();
        } else {
            startActivityForResult(
                    Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE),
                    Tango.TANGO_INTENT_ACTIVITYCODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Area Learning Permissions Required!",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * Connect to Tango service and connect the camera to the renderer.
     */
    private void connectAndStart() {
        // Synchronize against disconnecting while the service is being used in the OpenGL thread or
        // in the UI thread.

        if (!mIsConnected) {
            // Initialize Tango Service as a normal Android Service, since we call
            // mTango.disconnect() in onPause, this will unbind Tango Service, so
            // everytime when onResume get called, we should create a new Tango object.
            mTango = new Tango(GraphmapperActivity.this, new Runnable() {
                // Pass in a Runnable to be called from UI thread when Tango is ready,
                // this Runnable will be running on a new thread.
                // When Tango is ready, we can call Tango functions safely here only
                // when there is no UI thread changes involved.
                @Override
                public void run() {
                    try {
                        synchronized (GraphmapperActivity.this) {
                            TangoSupport.initialize();
                            connectTango();
                            connectRenderer();
                            mIsConnected = true;
                        }
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                    }
                }
            });
        }

    }

    /**
     * Configure the Tango service and connect it to callbacks.
     */
    private void connectTango() {
        // Use default configuration for Tango Service, plus low latency
        // IMU integration and area learning.
        TangoConfig config = mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        // NOTE: Low latency integration is necessary to achieve a precise alignment of virtual
        // objects with the RBG image and produce a good AR effect.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);

        while (mADFWhile) {
            if (mIsADFLoadingFinished) {
                //Loading finished (Dialog)
                if (mLoadedADFPair != null) {
                    //Check if there is a graph with the same name available
                    if (loadGraph(mLoadedADFPair.getName() + "_graph.txt")) {
                        //load adf
                        config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, mLoadedADFPair.getUuid());
                        mIsSuccessGraphLoaded = true;
                    } else {
                        //adf is available, but graph not => do not load adf
                        //mIsViewer = false;
                    }
                } else {
                    //do not load adf
                    //mIsViewer = false;
                }
                mADFWhile = false;
                break;
            } else {
                //loading running / not finished => Wait
                continue;
            }
        }
        // NOTE: Area learning is necessary to achieve better precision is pose estimation
        if(!mIsViewer) {
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
        }
        mTango.connect(config);

        // No need to add any coordinate frame pairs since we are not
        // using pose data. So just initialize.
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        mTango.connectListener(framePairs, new OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // We are not using OnPoseAvailable for this app.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // Check if the frame available is for the camera we want and update its frame
                // on the view.
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Mark a camera frame is available for rendering in the OpenGL thread
                    mIsFrameAvailableTangoThread.set(true);
                    mSurfaceView.requestRender();
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // Save the cloud and point data for later use.
                mPointCloudManager.updateXyzIj(xyzIj);
            }

            @Override
            public void onTangoEvent(TangoEvent event) {
                // We are not using OnTangoEvent for this app.
            }
        });

        // Get intrinsics from device for use in transforms. This needs to be done after connecting Tango and listeners.
        mIntrinsics = mTango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
    }

    /**
     * Connects the view and renderer to the color camara and callbacks.
     */
    private void connectRenderer() {
        // Register a Rajawali Scene Frame Callback to update the scene camera pose whenever a new
        // RGB frame is rendered.
        // (@see https://github.com/Rajawali/Rajawali/wiki/Scene-Frame-Callbacks)
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // NOTE: This is called from the OpenGL render thread, after all the renderer
                // onRender callbacks had a chance to run and before scene objects are rendered
                // into the scene.

                // Prevent concurrent access to {@code mIsFrameAvailableTangoThread} from the Tango
                // callback thread and service disconnection from an onPause event.
                synchronized (GraphmapperActivity.this) {
                    // Don't execute any tango API actions if we're not connected to the service
                    if (!mIsConnected) {
                        return;
                    }

                    // Set-up scene camera projection to match RGB camera intrinsics
                    if (!mRenderer.isSceneCameraConfigured()) {
                        mRenderer.setProjectionMatrix(mIntrinsics);
                    }

                    // Connect the camera texture to the OpenGL Texture if necessary
                    // NOTE: When the OpenGL context is recycled, Rajawali may re-generate the
                    // texture with a different ID.
                    if (mConnectedTextureIdGlThread != mRenderer.getTextureId()) {
                        mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                mRenderer.getTextureId());
                        mConnectedTextureIdGlThread = mRenderer.getTextureId();
                        Log.d(TAG, "connected to texture id: " + mRenderer.getTextureId());
                    }

                    // If there is a new RGB camera frame available, update the texture with it
                    if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
                        mRgbTimestampGlThread =
                                mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    }

                    // If a new RGB frame has been rendered, update the camera pose to match.
                    if (mRgbTimestampGlThread > mCameraPoseTimestamp) {
                        // Calculate the camera color pose at the camera frame update time in OpenGL engine.
                        TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(
                                mRgbTimestampGlThread,
                                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                                TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL, 0);
                        if (lastFramePose.statusCode == TangoPoseData.POSE_VALID) {
                            // Update the camera pose from the renderer
                            mRenderer.updateRenderCameraPose(lastFramePose);
                            mCameraPoseTimestamp = lastFramePose.timestamp;

                            //If ADF was loaded, check if re-localization occured and tell it the user
                            if (mIsSuccessGraphLoaded) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mADFText.setText("ADF & Graph loaded: " + mLoadedADFPair.getName() + " (LOCALIZED)");
                                        if(mIsViewer) {
                                            mAddButton.setVisibility(View.VISIBLE);
                                        }
                                    }
                                });
                                mIsSuccessGraphLoaded = false; //the display should be just once
                            }
                        } else {
                            Log.w(TAG, "Can't get device pose at time: " +
                                    mRgbTimestampGlThread);
                        }
                    }
                }
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }

            @Override
            public boolean callPreFrame() {
                return true;
            }
        });
    }

    /**
     * This method handles when the user clicks the screen. It will try to fit a plane to the
     * clicked point using depth data.
     * @param view (current view)
     * @param motionEvent (event that the user had done on the display)
     * @return true (if there is no exception)
     */
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(!mIsViewer) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // Calculate click location in u,v (0;1) coordinates.
                float u = motionEvent.getX() / view.getWidth();
                float v = motionEvent.getY() / view.getHeight();

                try {
                    // Take a wall measurement by fitting a plane on the clicked point using the latest
                    // point cloud data.
                    // Synchronize against concurrent access to the RGB timestamp in the OpenGL thread
                    // and a possible service disconnection due to an onPause event.
                    WallMeasurement wallMeasurement;
                    synchronized (this) {
                        wallMeasurement = doWallMeasurement(u, v, mRgbTimestampGlThread, mCurrentActionType);
                    }
                    // If the measurement was successful add it to the current measurements
                    if (wallMeasurement != null) {
                        if (wallMeasurement.getMeasurementType() == MeasurementType.CUT) {
                            mCutMeasurementList.add(wallMeasurement);
                            mLastType = MeasurementType.CUT;
                        } else if (wallMeasurement.getMeasurementType() == MeasurementType.ENTRY) {
                            Dialog.entryTypeDialog(this, wallMeasurement);
                            mEntryMeasurementList.add(wallMeasurement);
                            mLastType = MeasurementType.ENTRY;
                        } else if (wallMeasurement.getMeasurementType() == MeasurementType.ROOM) {
                            Dialog.roomDialog(this, wallMeasurement);
                            mRoomMeasurementList.add(wallMeasurement);
                            mLastType = MeasurementType.ROOM;
                        } else if (wallMeasurement.getMeasurementType() == MeasurementType.MARKER) {
                            Dialog.markerDialog(this, wallMeasurement);
                            mMarkerMeasurementList.add(wallMeasurement);
                            mLastType = MeasurementType.MARKER;
                        } else {
                            mWallMeasurementList.add(wallMeasurement);
                            mLastType = MeasurementType.WALL;
                        }
                        mLastMeasurement = wallMeasurement;
                        mRenderer.addWallMeasurement(wallMeasurement);
                    }

                } catch (TangoException t) {
                    Toast.makeText(getApplicationContext(),
                            R.string.failed_measurement,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, getString(R.string.failed_measurement), t);
                } catch (SecurityException t) {
                    Toast.makeText(getApplicationContext(),
                            R.string.failed_permissions,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, getString(R.string.failed_permissions), t);
                }
            }
        }
        return true;
    }

    /**
     * Use the TangoSupport library and point cloud data to calculate the plane at the specified
     * location in the color camera frame.
     * @param u (The u-coordinate for the user selection. This is expected to be between 0.0 and 1.0.)
     * @param v (The v-coordinate for the user selection. This is expected to be between 0.0 and 1.0.)
     * @param rgbTimestamp (timestamp of the current measurement)
     * @param type (type of the current measurement)
     * @return new wallMeasurement object with the calculated pose or null if there wasn't a valid pose data
     */
    private WallMeasurement doWallMeasurement(float u, float v, double rgbTimestamp, MeasurementType type) {
        TangoXyzIjData xyzIj = mPointCloudManager.getLatestXyzIj();

        if (xyzIj == null) {
            return null;
        }

        // We need to calculate the transform between the color camera at the
        // time the user clicked and the depth camera at the time the depth
        // cloud was acquired.
        TangoPoseData colorTdepthPose = TangoSupport.calculateRelativePose(
                rgbTimestamp, TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                xyzIj.timestamp, TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH);

        // Perform plane fitting with the latest available point cloud data.
        try {
            IntersectionPointPlaneModelPair intersectionPointPlaneModelPair =
                    TangoSupport.fitPlaneModelNearClick(xyzIj, mIntrinsics,
                            colorTdepthPose, u, v);

            // Get the depth camera transform at the time the plane data was acquired.
            TangoSupport.TangoMatrixTransformData transform =
                    TangoSupport.getMatrixTransformAtTime(xyzIj.timestamp,
                            TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                            TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            TangoSupport.TANGO_SUPPORT_ENGINE_TANGO);
            if (transform.statusCode == TangoPoseData.POSE_VALID) {
                // Update the AR object location.
                float[] planeFitTransform = calculatePlaneTransform(
                        intersectionPointPlaneModelPair.intersectionPoint,
                        intersectionPointPlaneModelPair.planeModel, transform.matrix);

                return new WallMeasurement(planeFitTransform, transform.matrix, xyzIj.timestamp, type);
            } else {
                Log.d(TAG, "Could not get a valid transform from depth to area description at time "
                        + xyzIj.timestamp);
            }
        } catch (TangoException e) {
            Log.d(TAG, "Failed to fit plane");
            Toast.makeText(this, "Failed to fit plane", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    /**
     * Calculate the pose of the plane based on the position and normal orientation of the plane and align it with gravity.
     * @param point (starting point of the plane)
     * @param normal (normal vector of the plane)
     * @param openGlTdepth (depth in openGL frame)
     * @return pose of the plane in device frame
     */
    private float[] calculatePlaneTransform(double[] point, double normal[], float[] openGlTdepth) {
        // Vector aligned to gravity.
        float[] openGlUp = new float[]{0, 1, 0, 0};
        float[] depthTOpenGl = new float[16];
        Matrix.invertM(depthTOpenGl, 0, openGlTdepth, 0);
        float[] depthUp = new float[4];
        Matrix.multiplyMV(depthUp, 0, depthTOpenGl, 0, openGlUp, 0);
        // Create the plane matrix transform in depth frame from a point, the plane normal and the
        // up vector.
        float[] depthTplane = matrixFromPointNormalUp(point, normal, depthUp);
        float[] openGlTplane = new float[16];
        Matrix.multiplyMM(openGlTplane, 0, openGlTdepth, 0, depthTplane, 0);
        return openGlTplane;
    }

    /**
     * Builds the current hallway with the current measurements and its obstacles, rooms, markers and entrypoints.
     */
    private void buildPlan() {
        mCurrentHallway = GraphBuilder.buildHallway(mWallMeasurementList); //walls
        mCurrentHallway.addObstacleList(mCutObstacleList); //cutted obstacles
        mCurrentHallway.addRooms(GraphBuilder.buildRooms(mRoomMeasurementList)); //rooms
        mCurrentHallway.addMarkers(GraphBuilder.buildMarkers(mMarkerMeasurementList)); //markers
        mCurrentHallway.addConnections(GraphBuilder.buildEntrys(GraphmapperActivity.this, mCurrentHallway, mEntryMeasurementList)); //entrypoints
    }

    /**
     * Setter function for the current level (Sets mCurrentLevel and changes the mInfoText)
     * @param level (value of the level)
     */
    public void setGlobalLevel(int level) {
        mCurrentLevel = level;
        mInfoText.setText("touch action: " + mCurrentActionType.toString() + ", level selected: " + mCurrentLevel);
        //Text for successful loading of adf & graph (does not work in connectTango() function)
        if (mLoadedADFPair != null) {
            if (mIsSuccessGraphLoaded) {
                //ADF & graph were loaded
                Toast.makeText(GraphmapperActivity.this, "ADF & graph loaded successfully: "
                        + mLoadedADFPair.getName() + "(" + mLoadedADFPair.getUuid() + ")", Toast.LENGTH_SHORT).show();
                mADFText.setText("ADF & Graph loaded: " + mLoadedADFPair.getName() + " (Not Localized)");
                Toast.makeText(GraphmapperActivity.this, "Please walk around in known areas to localize. This may take several minutes. Don't give up.", Toast.LENGTH_LONG).show();
            } else {
                //ADF is available, but no matching graph
                Toast.makeText(GraphmapperActivity.this, "There is no existing graph for the selected ADF \""
                        + mLoadedADFPair.getName() + "\". No ADF or graph was loaded.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Setter function to set a human readable name to the adf that will be saved later
     * @param name (name of the adf)
     */
    public void setADFName(String name) {
        mADFName = name;
    }

    /**
     * Setter function for the boolean value mIsADFLoadingFinished that signals if the loading process is finished.
     * @param value (new value of the boolean object)
     */
    public void setIsADFLoadingFinished(boolean value) {
        mIsADFLoadingFinished = value;
    }

    /**
     * Getter function for the navigation graph
     * @return mGraph
     */
    public Graph getGraph() {
        return mGraph;
    }

    /**
     * Getter function for the current graph hallways as a List
     * @return new ArrayList with all Hallways stored in the current graph
     */
    public List<Hallway> getCurrentGraphList() {
        return new ArrayList<Hallway>(mGraph.getGraph().values());
    }

    /**
     * This function creates a list of all adf pairs stored at the device.
     * @return list of all stored adf pairs (uuid / human readable name)
     */
    public List<ADFPair> createADFNameList() {
        // Returns a list of ADFs with their UUIDs
        List<String> UUIDList = mTango.listAreaDescriptions();
        List<ADFPair> ADFList = new ArrayList<ADFPair>();
        for (String uuid : UUIDList) {
            TangoAreaDescriptionMetaData metadata = mTango.loadAreaDescriptionMetaData(uuid);
            byte[] bytes = metadata.get(TangoAreaDescriptionMetaData.KEY_NAME);
            String name = "";
            try {
                name = new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            ADFPair pair = new ADFPair(name, uuid);
            ADFList.add(pair);
        }
        return ADFList;
    }

    /**
     * This function checks if the adf loading process is finished yet. If true, it sets the the
     * mLoadedADFPair value to the given pair and signals that the loading process is finished (mIsADFLoadingFinished).
     * @param pair (uuid / name adf pair)
     */
    public void setLoadedADFPair(ADFPair pair) {
        if (!mIsADFLoadingFinished) {
            mLoadedADFPair = pair;
            mIsADFLoadingFinished = true;
        }
    }

    /**
     * Setter function for the signal if the application is in recording or viewer state
     * @param value (if true the application will be started in viewer state)
     */
    public void setViewer (boolean value) {
        mIsViewer = value;
    }

    /**
     * This functions inits the view for re-localization before going into the viewer mode.
     */
    public void initViewer() {
        mRoomButton.setVisibility(View.INVISIBLE);
        mEntryButton.setVisibility(View.INVISIBLE);
        mMarkerButton.setVisibility(View.INVISIBLE);
        mWallButton.setVisibility(View.INVISIBLE);
        mOptionButton.setVisibility(View.INVISIBLE);
        mDoneButton.setVisibility(View.INVISIBLE);
        mUndoButton.setVisibility(View.INVISIBLE);
        mResetButton.setVisibility(View.INVISIBLE);

        mLevelButton.setVisibility(View.VISIBLE);
        mLevelButton.setText("Restart");
        mAddButton.setVisibility(View.INVISIBLE);
        mAddButton.setText("Next");
        mADFText.setText("ADF & Graph loaded: " + mLoadedADFPair.getName() + " (Not localized)");
        mInfoText.setText("Viewer");
        Toast.makeText(GraphmapperActivity.this, "Please walk around in known areas to localize and start the viewer mode.", Toast.LENGTH_LONG).show();
    }

    /**
     * This function creates a new hallway from the current measurements.
     * It builds the hallway with all his rooms, markers, obstacles and entrypoints after upadating all measurements
     * and stores it in the navigation graph structure.
     * After that it removes from the renderer view all current measurements and clears all measurement lists.
     * So the user can start to record a new hallway in the end.
     * @param hallwayName (name of the current hallway to be finished)
     */
    public void addHallway(String hallwayName) {
        mRenderer.removeMeasurements(); //remove elements from renderer
        updateMeasurements(); //update and add measurements
        buildPlan(); //finish current hallway
        mCurrentHallway.setName(hallwayName);
        mCurrentHallway.setLevel(mCurrentLevel);
        mGraph.addHallway(mCurrentHallway); //store current hallway in graph
        mWallMeasurementList.clear(); //clear wall list
        mRoomMeasurementList.clear(); //clear room list
        mMarkerMeasurementList.clear(); //clear marker list
        mEntryMeasurementList.clear(); //clear entrypoint list
        mCutMeasurementList.clear(); //clear obstacle measurement list
        mCutObstacleList.clear(); //clear obstacle list
        mRenderer.removeMeasurements(); //remove elements finally from renderer
        Toast.makeText(GraphmapperActivity.this, "Name of the last hallway: " + mCurrentHallway.getName(), Toast.LENGTH_SHORT).show();
        mCurrentHallway = null;
        mLastMeasurement = null;
        mLastType = null;
    }

    /**
     * This function finishes the current hallway, clears all measurement list and objects
     * and then starts the dialog to ask the user which level he want to record next.
     * @param hallwayName (name of the current hallway to be finished)
     */
    public void addNewLevel(String hallwayName) {
        addHallway(hallwayName); //finish current hallway
        Dialog.levelDialog(GraphmapperActivity.this); //ask for new level
    }

    /**
     * This function resets all current measurement list and objects of the current recording hallway.
     * So the user can start from the beginning with recording the current hallway.
     */
    public void resetHallway() {
        mRenderer.removeMeasurements(); //remove elements from renderer
        mWallMeasurementList.clear(); //clear wall list
        mRoomMeasurementList.clear(); //clear room list
        mMarkerMeasurementList.clear(); //clear marker list
        mEntryMeasurementList.clear(); //clear entrypoint list
        mCutMeasurementList.clear(); //clear obstacle measurement list
        mCutObstacleList.clear(); //clear obstacle list
        mCurrentHallway = null;
        mLastMeasurement = null;
        mLastType = null;
    }

    /**
     * This function eliminates the last taken measurement and removes the objects from the lists.
     */
    public void undoLastMeasurement() {
        if (mLastType != null && mLastMeasurement != null) {
            if (mLastType == MeasurementType.ENTRY) {
                mEntryMeasurementList.remove(mLastMeasurement);
            } else if (mLastType == MeasurementType.ROOM) {
                mRoomMeasurementList.remove(mLastMeasurement);
            } else if (mLastType == MeasurementType.CUT) {
                mCutMeasurementList.remove(mLastMeasurement);
            } else if (mLastType == MeasurementType.MARKER) {
                mMarkerMeasurementList.remove(mLastMeasurement);
            } else {
                //Wall
                mWallMeasurementList.remove(mLastMeasurement);
            }
            mLastType = null;
            mLastMeasurement = null;
            mRenderer.undoLastMeasurement();
        }
    }

    /**
     * Updates every saved measurement of the current hallway.
     * It re-queries the device pose at the time the measurement was taken.
     */
    public void updateMeasurements() {
        for (WallMeasurement wallMeasurement : mWallMeasurementList) {
            // We need to re query the depth transform when the measurements were taken.
            TangoSupport.TangoMatrixTransformData transform =
                    TangoSupport.getMatrixTransformAtTime(wallMeasurement
                                    .getDepthTransformTimeStamp(),
                            TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                            TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            TangoSupport.TANGO_SUPPORT_ENGINE_TANGO);
            if (transform.statusCode == TangoPoseData.POSE_VALID) {
                wallMeasurement.update(transform.matrix);
                mRenderer.addWallMeasurement(wallMeasurement);
            } else {
                Log.d(TAG, "Could not get a valid transform from depth to area description at time " + wallMeasurement.getDepthTransformTimeStamp());
            }
        }
        for (WallMeasurement roomMeasurement : mRoomMeasurementList) {
            // We need to re query the depth transform when the measurements were taken.
            TangoSupport.TangoMatrixTransformData transform =
                    TangoSupport.getMatrixTransformAtTime(roomMeasurement
                                    .getDepthTransformTimeStamp(),
                            TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                            TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            TangoSupport.TANGO_SUPPORT_ENGINE_TANGO);
            if (transform.statusCode == TangoPoseData.POSE_VALID) {
                roomMeasurement.update(transform.matrix);
                mRenderer.addWallMeasurement(roomMeasurement);
            } else {
                Log.d(TAG, "Could not get a valid transform from depth to area description at time " + roomMeasurement.getDepthTransformTimeStamp());
            }
        }
        for (WallMeasurement markerMeasurement : mMarkerMeasurementList) {
            // We need to re query the depth transform when the measurements were taken.
            TangoSupport.TangoMatrixTransformData transform =
                    TangoSupport.getMatrixTransformAtTime(markerMeasurement
                                    .getDepthTransformTimeStamp(),
                            TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                            TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            TangoSupport.TANGO_SUPPORT_ENGINE_TANGO);
            if (transform.statusCode == TangoPoseData.POSE_VALID) {
                markerMeasurement.update(transform.matrix);
                mRenderer.addWallMeasurement(markerMeasurement);
            } else {
                Log.d(TAG, "Could not get a valid transform from depth to area description at time " + markerMeasurement.getDepthTransformTimeStamp());
            }
        }
        for (WallMeasurement entryMeasurement : mEntryMeasurementList) {
            // We need to re query the depth transform when the measurements were taken.
            TangoSupport.TangoMatrixTransformData transform =
                    TangoSupport.getMatrixTransformAtTime(entryMeasurement
                                    .getDepthTransformTimeStamp(),
                            TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                            TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            TangoSupport.TANGO_SUPPORT_ENGINE_TANGO);
            if (transform.statusCode == TangoPoseData.POSE_VALID) {
                entryMeasurement.update(transform.matrix);
                mRenderer.addWallMeasurement(entryMeasurement);
            } else {
                Log.d(TAG, "Could not get a valid transform from depth to area description at time " + entryMeasurement.getDepthTransformTimeStamp());
            }
        }
        for (WallMeasurement cutMeasurement : mCutMeasurementList) {
            // We need to re query the depth transform when the measurements were taken.
            TangoSupport.TangoMatrixTransformData transform =
                    TangoSupport.getMatrixTransformAtTime(cutMeasurement
                                    .getDepthTransformTimeStamp(),
                            TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                            TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            TangoSupport.TANGO_SUPPORT_ENGINE_TANGO);
            if (transform.statusCode == TangoPoseData.POSE_VALID) {
                cutMeasurement.update(transform.matrix);
                mRenderer.addWallMeasurement(cutMeasurement);
            } else {
                Log.d(TAG, "Could not get a valid transform from depth to area description at time " + cutMeasurement.getDepthTransformTimeStamp());
            }
        }
    }

    /**
     * Finish plan, save the adf, and show the final result.
     * Executed as an AsyncTask because saving the adf could be an expensive operation.
     */
    public void finishPlan() {
        // Don't attempt to save if the service is not ready.
        if (!canSaveAdf() && !mIsViewer) {
            Toast.makeText(this, "Tango service not ready to save ADF", Toast.LENGTH_LONG).show();
            return;
        }

        if (mFinishPlanTask != null) {
            Log.w(TAG, "Finish task already executing");
            return;
        }

        //Add all unconnected entrypoints to the stack
        for(Hallway h : getCurrentGraphList()) {
            for(Entrypoint e : h.getConnections()) {
                if(((e.getPositionToList() == null) && (e.getHallwayToIDList() == null)) ||
                        ((e.getPositionToList().isEmpty()) && (e.getHallwayToIDList().isEmpty()))) {
                    mUnconnectedEntrys.push(e);
                } else if(e.getType() == MeasurementType.LIFT) {
                    mUnconnectedEntrys.push(e); //Lifts can always have more connections
                }
            }
        }

        mFinishPlanTask = new FinishPlanTask();
        mFinishPlanTask.execute();
    }

    /**
     * Verifies whether the Tango service is in a state where the ADF can be saved or not.
     * @return returns a boolean value if the adf can be saved or not (true if yes)
     */
    private boolean canSaveAdf() {
        boolean canSaveAdf = false;
        try {
            synchronized (this) {
                TangoPoseData poseData = mTango.getPoseAtTime(0, new TangoCoordinateFramePair(
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));
                if (poseData.statusCode == TangoPoseData.POSE_VALID) {
                    canSaveAdf = true;
                } else {
                    Log.w(TAG, "ADF pose unavailable");
                }
            }
        } catch (TangoException e) {
            Log.e(TAG, "Exception query Tango service before saving ADF.", e);
        }
        return canSaveAdf;
    }

    /**
     * Calculates a transformation matrix based on a point, a normal and the up gravity vector.
     * The coordinate frame of the target transformation will be Z forward, X left, Y up.
     * @param point (starting point)
     * @param normal (normal vector)
     * @param up (up gravity vector)
     * @return transformation matrix (Note the order: x, z, y)
     */
    private float[] matrixFromPointNormalUp(double[] point, double[] normal, float[] up) {
        float[] zAxis = new float[]{(float) normal[0], (float) normal[1], (float) normal[2]};
        normalize(zAxis);
        float[] xAxis = crossProduct(zAxis, up);
        normalize(xAxis);
        float[] yAxis = crossProduct(zAxis, xAxis);
        normalize(yAxis);
        float[] m = new float[16];
        Matrix.setIdentityM(m, 0);
        m[0] = xAxis[0];
        m[1] = xAxis[1];
        m[2] = xAxis[2];
        m[4] = yAxis[0];
        m[5] = yAxis[1];
        m[6] = yAxis[2];
        m[8] = zAxis[0];
        m[9] = zAxis[1];
        m[10] = zAxis[2];
        m[12] = (float) point[0];
        m[13] = (float) point[1];
        m[14] = (float) point[2];
        return m;
    }

    /**
     * Normalize a vector.
     * @param v (un-normalized vector)
     */
    private void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }

    /**
     * Cross product between two vectors following the right hand rule.
     * @param v1 (first vector)
     * @param v2 (second vector)
     * @return cross product
     */
    private float[] crossProduct(float[] v1, float[] v2) {
        float[] result = new float[3];
        result[0] = v1[1] * v2[2] - v2[1] * v1[2];
        result[1] = v1[2] * v2[0] - v2[2] * v1[0];
        result[2] = v1[0] * v2[1] - v2[0] * v1[1];
        return result;
    }

    /**
     * Finish plan AsyncTask.
     * Shows a spinner while it's saving the adf and updating the measurements.
     * Draws the final result on a canvas and shows it.
     */
    private class FinishPlanTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            mProgressGroup.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            //Note: If the current position should be showed on the display the adf haven't to be saved
            // Save and optimize ADF.
            if(!mIsViewer) {
                String uuid = mTango.saveAreaDescription();
                TangoAreaDescriptionMetaData metadata = mTango.loadAreaDescriptionMetaData(uuid);
                metadata.set(TangoAreaDescriptionMetaData.KEY_NAME, mADFName.getBytes());
                mTango.saveAreaDescriptionMetadata(uuid, metadata);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            mProgressGroup.setVisibility(View.GONE);

            //successful saving adf
            Toast.makeText(GraphmapperActivity.this, "ADF saved under: \"" + mADFName + "\" in ADF storage.", Toast.LENGTH_SHORT).show();

            //connect entrypoints
            if (!mUnconnectedEntrys.isEmpty()) {
                Dialog.doneConnectStart(GraphmapperActivity.this);
            } else {
                saveGraph();
            }

            setContentView(R.layout.floorplan_activity_draw);

            mDrawText = (TextView) findViewById(R.id.draw_text);
            mDrawText.setText("Graph & ADF name: " + mADFName);

            mRestartButton = (Button) findViewById(R.id.restart_button);
            mRestartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recreate(); //restart of the whole application
                }
            });

            mDrawButton = (Button) findViewById(R.id.draw_button);
            mDrawButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDrawText.setText("Graph & ADF name: " + mADFName);
                    //1. choose level, 2. draw on canvas
                    Dialog.chooseLevelToDrawDialog(GraphmapperActivity.this);
                    if (mCurrentPlanDraw != null) {
                        FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                        layout.removeView(mCurrentPlanDraw);
                        mCurrentPlanDraw = null;
                        mCurrentHallwayDraw = null;
                    }
                    mDestroyButton.setVisibility(View.VISIBLE);
                    mSaveButton.setVisibility(View.VISIBLE);
                    mSelectButton.setVisibility(View.VISIBLE);
                    mLengthButton.setVisibility(View.VISIBLE);
                    mPOIButton.setVisibility(View.VISIBLE);
                    mObstacleButton.setVisibility(View.VISIBLE);
                    mNameButton.setVisibility(View.VISIBLE);
                    if(mIsViewer) {
                        mPositionButton.setVisibility(View.VISIBLE);
                    }
                }
            });

            mNameButton = (Button) findViewById(R.id.name_button);
            mNameButton.setVisibility(View.INVISIBLE);
            mNameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrentPlanDraw != null) {
                        if (mShowNamesDraw) {
                            mNameButton.setText("Show Names");
                            mShowNamesDraw = false;
                        } else {
                            mNameButton.setText("Hide Names");
                            mShowNamesDraw = true;
                        }

                        if (mCurrentHallwayDraw == null) {
                            //Draw Level
                            FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                            layout.removeView(mCurrentPlanDraw);
                            mCurrentPlanDraw = drawLevelOnCanvas(mCurrentLevelDraw, null, null);
                        } else {
                            //Draw Hallway
                            FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                            layout.removeView(mCurrentPlanDraw);
                            mCurrentPlanDraw = drawHallwayOnCanvas(mCurrentHallwayDraw, null, null);
                        }
                    }
                }
            });
            mLengthButton = (Button) findViewById(R.id.length_button);
            mLengthButton.setVisibility(View.INVISIBLE);
            mLengthButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrentPlanDraw != null) {
                        if (mShowLengthDraw) {
                            mLengthButton.setText("Show Length");
                            mShowLengthDraw = false;
                        } else {
                            mLengthButton.setText("Hide Length");
                            mShowLengthDraw = true;
                        }

                        if (mCurrentHallwayDraw == null) {
                            //Draw Level
                            FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                            layout.removeView(mCurrentPlanDraw);
                            mCurrentPlanDraw = drawLevelOnCanvas(mCurrentLevelDraw, null, null);
                        } else {
                            //Draw Hallway
                            FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                            layout.removeView(mCurrentPlanDraw);
                            mCurrentPlanDraw = drawHallwayOnCanvas(mCurrentHallwayDraw, null, null);
                        }
                    }
                }
            });

            mPOIButton = (Button) findViewById(R.id.poi_button);
            mPOIButton.setVisibility(View.INVISIBLE);
            mPOIButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrentPlanDraw != null) {
                        if (mShowPOIDraw) {
                            mPOIButton.setText("Show POIs");
                            mShowPOIDraw = false;
                        } else {
                            mPOIButton.setText("Hide POIs");
                            mShowPOIDraw = true;
                        }
                        if (mCurrentHallwayDraw == null) {
                            //Draw Level
                            FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                            layout.removeView(mCurrentPlanDraw);
                            mCurrentPlanDraw = drawLevelOnCanvas(mCurrentLevelDraw, null, null);
                        } else {
                            //Draw Hallway
                            FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                            layout.removeView(mCurrentPlanDraw);
                            mCurrentPlanDraw = drawHallwayOnCanvas(mCurrentHallwayDraw, null, null);
                        }
                    }
                }
            });

            mObstacleButton = (Button) findViewById(R.id.obstacle_button);
            mObstacleButton.setVisibility(View.INVISIBLE);
            mObstacleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrentPlanDraw != null) {
                        if (mShowObstaclesDraw) {
                            mObstacleButton.setText("Show Obst.");
                            mShowObstaclesDraw = false;
                        } else {
                            mObstacleButton.setText("Hide Obst.");
                            mShowObstaclesDraw = true;
                        }

                        if (mCurrentHallwayDraw == null) {
                            //Draw Level
                            FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                            layout.removeView(mCurrentPlanDraw);
                            mCurrentPlanDraw = drawLevelOnCanvas(mCurrentLevelDraw, null, null);
                        } else {
                            //Draw Hallway
                            FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                            layout.removeView(mCurrentPlanDraw);
                            mCurrentPlanDraw = drawHallwayOnCanvas(mCurrentHallwayDraw, null, null);
                        }
                    }
                }
            });

            mDestroyButton = (Button) findViewById(R.id.destroy_button);
            mDestroyButton.setVisibility(View.INVISIBLE);
            mDestroyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrentPlanDraw != null) {
                        FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                        layout.removeView(mCurrentPlanDraw);
                        mCurrentPlanDraw = null;
                        mCurrentHallwayDraw = null;
                    }
                    mDrawText.setText("Graph & ADF name: " + mADFName);
                    mSaveButton.setVisibility(View.INVISIBLE);
                    mPositionButton.setVisibility(View.INVISIBLE);
                    mDestroyButton.setVisibility(View.INVISIBLE);
                    mSelectButton.setVisibility(View.INVISIBLE);
                    mLengthButton.setVisibility(View.INVISIBLE);
                    mPOIButton.setVisibility(View.INVISIBLE);
                    mNameButton.setVisibility(View.INVISIBLE);
                    mObstacleButton.setVisibility(View.INVISIBLE);
                    mPOIButton.setText("Hide POIs");
                    mShowPOIDraw = true;
                    mLengthButton.setText("Hide Length");
                    mShowLengthDraw = true;
                    mNameButton.setText("Show Names");
                    mShowNamesDraw = false;
                    mObstacleButton.setText("Show Obst.");
                    mShowObstaclesDraw = false;
                }
            });

            mSaveButton = (Button) findViewById(R.id.save_button);
            mSaveButton.setVisibility(View.INVISIBLE);
            mSaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrentPlanDraw != null) {
                        //Convert the view to a bitmap object and save it as a jpg
                        File storage = Environment.getExternalStorageDirectory();
                        String path = storage.getAbsolutePath() + mStoragePath + "/Images";
                        String name = mADFName + "_floorplan_level" + mCurrentLevelDraw + "_IMG" + mCountJPG;
                        mCountJPG++;
                        if (mCurrentHallwayDraw != null) {
                            name += "_" + mCurrentHallwayDraw.getName() + ".jpg";
                        } else {
                            name += ".jpg";
                        }
                        File file = new File(path, name);
                        try {
                            FileOutputStream fos = new FileOutputStream(file);
                            Bitmap b = mCurrentPlanDraw.getDrawingCache();
                            b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(GraphmapperActivity.this, "Floorplan saved under: " + path + "/" + name, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mSelectButton = (Button) findViewById(R.id.select_button);
            mSelectButton.setVisibility(View.INVISIBLE);
            mSelectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrentPlanDraw != null) {
                        FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                        layout.removeView(mCurrentPlanDraw);
                        mCurrentPlanDraw = null;
                        mSelectButton.setVisibility(View.INVISIBLE);
                        //mPositionButton.setVisibility(View.VISIBLE);
                        Dialog.chooseHallwayToDrawDialog(GraphmapperActivity.this, mCurrentLevelDraw);
                    }
                }
            });

            //show current position on the plan
            mPositionButton = (Button) findViewById(R.id.position_button);
            mPositionButton.setVisibility(View.INVISIBLE);
            mPositionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // If there is a new RGB camera frame available, update the texture with it
                    if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
                        mRgbTimestampGlThread = mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    }
                    // If a new RGB frame has been rendered, update the camera pose to match.
                    if (mRgbTimestampGlThread > mCameraPoseTimestamp) {
                        // Calculate the camera color pose at the camera frame update time in OpenGL engine.
                        TangoPoseData currentPose = TangoSupport.getPoseAtTime(
                                mRgbTimestampGlThread,
                                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                                TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL, 0);
                        if (currentPose.statusCode == TangoPoseData.POSE_VALID) {
                            // Update the camera pose from the renderer
                            mRenderer.updateRenderCameraPose(currentPose);
                            mCameraPoseTimestamp = currentPose.timestamp;
                            float[] position = currentPose.getTranslationAsFloats();
                            float[] orientation = currentPose.getRotationAsFloats();
                            FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
                            layout.removeView(mCurrentPlanDraw);
                            if(mCurrentHallwayDraw == null) {
                                //draw level
                                mCurrentPlanDraw = drawLevelOnCanvas(mCurrentLevelDraw, position, orientation);
                            } else {
                                //draw hallway
                                mCurrentPlanDraw = drawHallwayOnCanvas(mCurrentHallwayDraw, position, orientation);
                            }

                        } else {
                            Log.w(TAG, "Can't get device pose at time: " + mRgbTimestampGlThread);
                        }
                    }
                }
            });

            mFinishPlanTask = null;
        }
    }

    /**
     * This function saves the current navigation graph (network) as a text file on the device in GSON format.
     */
    public void saveGraph() {
        //Save IDCounter
        mGraph.saveIDCounter();
        //Save graph
        File storage = Environment.getExternalStorageDirectory();
        String graphPath = storage.getAbsolutePath() + mStoragePath + "/Graphs";
        String printPath = storage.getAbsolutePath() + mStoragePath + "/Prints";
        String graphName = mADFName + "_graph.txt";
        File graphFile = new File(graphPath, graphName);
        File printFile = new File(printPath, mADFName + "_print.txt");
        Gson gson = new Gson();
        String json = gson.toJson(mGraph);
        String print = mGraph.printGraph();
        try {
            FileOutputStream fos = new FileOutputStream(graphFile);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(json);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream fos = new FileOutputStream(printFile);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(print);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(GraphmapperActivity.this, "Graph saved under: " + graphPath + "/" + graphName, Toast.LENGTH_SHORT).show();
    }

    /**
     * This function loads a prerecorded graph from a textfile in GSON format.
     * @param name (name of the graph to be loaded)
     * @return (true if the loading process was successful, false if not)
     */
    private boolean loadGraph(String name) {
        //Load/Read graph
        File storage = Environment.getExternalStorageDirectory();
        String path = storage.getAbsolutePath() + mStoragePath + "/Graphs";
        File file = new File(path, name);
        Gson gson = new Gson();
        String json = null;
        Graph readGraph = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream is = new ObjectInputStream(fis);
            json = (String) is.readObject();
            is.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (json != null) {
            readGraph = gson.fromJson(json, Graph.class);
            if ((readGraph != null) && (readGraph instanceof Graph)) {
                mGraph = readGraph;
                mGraph.loadIDCounter();
                mADFName = mLoadedADFPair.getName();
                return true;
            }
        }
        return false;
    }

    /**
     * This function builds a PlanView object of the 2D plan from a recorded level.
     * @param level (level of the building to be drawn)
     * @param position (current position of the user or null if not used / drawn)
     * @param orientation (current orientation of the user or null if not used / drawn)
     * @return PlanView object with the current view to be drawn
     */
    public PlanView drawLevelOnCanvas(int level, float[] position, float[] orientation) {
        // Draw final result on Canvas.
        PlanView planView = new PlanView(GraphmapperActivity.this, level, position, orientation, mShowLengthDraw, mShowPOIDraw, mShowNamesDraw, mShowObstaclesDraw);
        planView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        planView.setDrawingCacheEnabled(true);
        planView.invalidate();
        FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
        layout.addView(planView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        return planView;
    }

    /**
     * This function build a PlanView object of the 2D plan from a recorded hallway (zoom into hallways).
     * @param hallway (hallway to be zoomed in)
     * @param position (current position of the user or null if not used / drawn)
     * @param orientation (current orientation of the user or null if not used / drawn)
     * @return PlanView object with the current view to be drawn
     */
    public PlanView drawHallwayOnCanvas(Hallway hallway, float[] position, float[] orientation) {
        // Draw final result on Canvas.
        PlanView planView = new PlanView(GraphmapperActivity.this, hallway, position, orientation, mShowLengthDraw, mShowPOIDraw, mShowNamesDraw, mShowObstaclesDraw);
        planView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        planView.setDrawingCacheEnabled(true);
        planView.invalidate();
        FrameLayout layout = (FrameLayout) findViewById(R.id.draw_frame_layout);
        layout.addView(planView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        return planView;
    }

    /**
     * Getter function for the stack with the unconnected entrypoints
     * @return mUnconnectedEntrys
     */
    public Stack<Entrypoint> getUnconnectedEntrys() {
        return mUnconnectedEntrys;
    }

    /**
     * Setter function to set the current plan view object drawn (mCurrentPlanDraw)
     * @param planView (current plan view object)
     */
    public void setCurrentPlanDraw(PlanView planView) {
        mCurrentPlanDraw = planView;
    }

    /**
     * Setter function to set the current level drawn (mCurrentLevelDraw)
     * @param level (current level drawn)
     */
    public void setCurrentLevelDraw(int level) {
        mCurrentLevelDraw = level;
    }

    /**
     * Setter function to set the current hallway drawn (mCurrentHallwayDraw)
     * @param hallway (current hallway drawn)
     */
    public void setCurrentHallwayDraw(Hallway hallway) {
        mCurrentHallwayDraw = hallway;
    }

    /**
     * Getter function for the info text object in drawing view
     * @return mDrawText
     */
    public TextView getDrawText() {
        return mDrawText;
    }


    /**
     * Custom View that draws the plan in 2D.
     */
    public class PlanView extends View {
        /** color and text size settings */
        private Paint mPaint;
        /** list of all hallway that should be drawn in this PlanView */
        private List<Hallway> mDrawHallwayList;
        /** current position of the user (null if not used) */
        private float[] mPosition;
        /** current orientation of the user (null if not used) */
        private float[] mOrientation;
        /** signals if the length of the hallways should be drawn */
        private boolean mShowLength;
        /** signals if the POIs (rooms, markers, entrypoints) should be drawn */
        private boolean mShowPOI;
        /** signals if the names of the hallways and POIs should be drawn */
        private boolean mShowNames;
        /** signals if the obstacles of the hallways should be drawn */
        private boolean mShowObstacles;

        /**
         * Constructor (Creates a new PlanView object to draw a whole level)
         * @param context (context / activity)
         * @param level (level to be drawn)
         * @param position (current position of the user or null if not used)
         * @param showLength (signals if the length of the hallways should be drawn)
         * @param showPOI (signals if the POIs (rooms, markers, entrypoints) should be drawn)
         * @param showNames (signals if the names of the hallways and POIs should be drawn)
         * @param showObstacles (signals if the obstacles of the hallways should be drawn)
         */
        public PlanView(Context context, int level, float[] position, float[] orientation, boolean showLength, boolean showPOI, boolean showNames, boolean showObstacles) {
            super(context);
            super.setDrawingCacheBackgroundColor(0xffffffff); //white
            mPaint = new Paint();
            mPaint.setStrokeWidth(3);
            mPaint.setTextSize(30);
            mShowLength = showLength;
            mShowPOI = showPOI;
            mShowNames = showNames;
            mShowObstacles = showObstacles;
            //add all hallways to be drawn to the list
            mDrawHallwayList = new ArrayList<Hallway>();
            for (Hallway hallway : getCurrentGraphList()) {
                if (hallway.getLevel() == level) {
                    mDrawHallwayList.add(hallway);
                }
            }
            mPosition = position;
            mOrientation = orientation;
        }

        /**
         * Constructor (Creates a new PlanView object to draw a whole level)
         * @param context (context / activity)
         * @param hallway (hallway to be drawn - zoom in)
         * @param position (current position of the user or null if not used)
         * @param showLength (signals if the length of the hallways should be drawn)
         * @param showPOI (signals if the POIs (rooms, markers, entrypoints) should be drawn)
         * @param showNames (signals if the names of the hallways and POIs should be drawn)
         * @param showObstacles (signals if the obstacles of the hallways should be drawn)
         */
        public PlanView(Context context, Hallway hallway, float[] position, float[] orientation, boolean showLength, boolean showPOI, boolean showNames, boolean showObstacles) {
            super(context);
            super.setDrawingCacheBackgroundColor(0xffffffff); //white
            mPaint = new Paint();
            mPaint.setStrokeWidth(3);
            mPaint.setTextSize(30);
            mDrawHallwayList = new ArrayList<Hallway>();
            mDrawHallwayList.add(hallway);
            mShowLength = showLength;
            mShowPOI = showPOI;
            mShowNames = showNames;
            mShowObstacles = showObstacles;
            mPosition = position;
            mOrientation = orientation;
        }

        /**
         * This function creates a list with all corner points of all hallways from a whole level
         * @return list with points
         */
        private List<float[]> createLevelPointList() {
            //add all points from a level
            List<float[]> pointList = new ArrayList<float[]>();
            for (Hallway hallway : mDrawHallwayList) {
                pointList.addAll(hallway.getHallwayPoints());
            }
            return pointList;
        }

        /**
         * This function draws a hallway on the canvas with the selected settings (POIs, length, names)
         * @param canvas (canvas where the plan is drawn)
         * @param planCenter (center of the plan)
         * @param scale (scale of the plan)
         */
        private void drawHallways(Canvas canvas, float[] planCenter, float scale) {
            for (Hallway hallway : mDrawHallwayList) {
                //draws all hallways with cutted obstacles and the selected settings
                hallway.drawOnCanvas(canvas, mPaint, planCenter, scale, mShowLength, mShowNames, mShowObstacles);
                mPaint.setStrokeWidth(20);
                if(mShowPOI) {
                    //draw rooms as dots
                    for (Room room : hallway.getRooms()) {
                        mPaint.setColor(0xfff44336); //Red
                        room.drawOnCanvas(canvas, mPaint, planCenter, scale, mShowNames);
                    }
                    //draw markers as dots
                    for (Marker marker : hallway.getMarkers()) {
                        mPaint.setColor(0xffcddc39); //Lime
                        marker.drawOnCanvas(canvas, mPaint, planCenter, scale, mShowNames);
                    }
                    //draw entrypoints as dots
                    for (Entrypoint entrypoint : hallway.getConnections()) {
                        mPaint.setColor(0xff2196f3); //Blue
                        entrypoint.drawOnCanvas(canvas, mPaint, planCenter, scale, mShowNames);
                    }
                }
                //draw current position of the user if used (not null)
                if (mPosition != null) {
                    drawPosition(canvas, planCenter, scale);
                }
                mPaint.setStrokeWidth(3);
                mPaint.setColor(0xff000000);
            }
        }

        /**
         * This function calculates the scale of the current user position point
         * and draws the current position of the user on the canvas.
         * @param canvas (canvas whehre the position is drawn)
         * @param planCenter (center of the plan)
         * @param scale (scale of the plan)
         */
        public void drawPosition(Canvas canvas, float[] planCenter, float scale) {
            int centerX = canvas.getWidth() / 2;
            int centerY = canvas.getHeight() / 2;
            float[] newPoint = new float[3];
            for (int i = 0; i < 3; i++) {
                newPoint[i] = (mPosition[i] - planCenter[i]) * scale;
            }
            float pos_x = centerX + newPoint[0];
            float pos_y = centerY + newPoint[2];
            mPaint.setColor(0xffff9800); //orange
            canvas.drawCircle(pos_x, pos_y, 7, mPaint);
        }

        /**
         * This function will be running if a PlanView object is created and invalidated.
         * It is the main drawing function of a PlanView object and
         * calculates the center and the scale of the 2D plan to be drawn.
         * @param canvas (canvas where the plan is drawn)
         */
        @Override
        public void onDraw(Canvas canvas) {
            List<float[]> pointList = createLevelPointList();
            float[] planCenter = GraphBuilder.getPlanCenter(pointList);
            float scale = GraphBuilder.getPlanScale(canvas.getHeight(), canvas.getWidth(), pointList);
            canvas.drawText("Graphmapper (UniBw 2016)", canvas.getWidth() - 600, canvas.getHeight() - 22, mPaint);
            String s = "Recording: " + mADFName + ", level: " + mCurrentLevelDraw;
            if(mCurrentHallwayDraw != null) {
                s += ", hallway: " + mCurrentHallwayDraw.getName();
            }
            canvas.drawText(s, 200, canvas.getHeight() - 22, mPaint);
            drawHallways(canvas, planCenter, scale);
        }
    }
}
