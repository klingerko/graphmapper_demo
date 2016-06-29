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

package com.projecttango.unibw.graphmapper.dialog;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import com.projecttango.unibw.graphmapper.floorplan.GraphmapperActivity;
import com.projecttango.unibw.graphmapper.floorplan.R;
import com.projecttango.unibw.graphmapper.graph.MeasurementType;
import com.projecttango.unibw.graphmapper.floorplan.WallMeasurement;
import com.projecttango.unibw.graphmapper.graph.Entrypoint;
import com.projecttango.unibw.graphmapper.graph.Hallway;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class contains all user input dialogs as static functions.
 * For example the user can give a hallway a specific name.
 * This class was created by Konstantin Klinger on 26.04.16.
 * @author Konstantin Klinger
 * @version 1.0
 */
public class Dialog {

    /**
     * This function scales a drawable icon to display it in a smaller size at the dialogs on the screen.
     * @param contextActivity (context - activity where the dialog displays)
     * @param drawable (Integer id of the drawable that should be scaled)
     * @return BitmapDrawable (scaled drawable)
     */
    private static BitmapDrawable createScaledIcon(GraphmapperActivity contextActivity, int drawable) {
        // load the origial BitMap (500 x 500 px)
        Bitmap bitmapOrg = BitmapFactory.decodeResource(contextActivity.getResources(), drawable);
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        int newWidth = width / 4;
        int newHeight = height / 4;
        // calculate the scale - in this case = 0.4f
        float scaledWidth = ((float) newWidth) / width;
        float scaledHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaledWidth, scaledHeight);
        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
        // make a Drawable from Bitmap to allow to set the BitMap to the ImageView, ImageButton or what ever
        BitmapDrawable bmd = new BitmapDrawable(contextActivity.getResources(), resizedBitmap);
        return bmd;
    }

    /**
     * Dialog if the user want to finish the current record.
     * @param contextActivity (context - activity where the dialog displays)
     */
    public static void doneButtonDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Done:");
        builder.setMessage("Are you sure you want to finish the current recording?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //finish
                hallwayDoneDialog(contextActivity);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //do nothing
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.done2_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for giving the hallway a name.
     * @param contextActivity (context - activity where the dialog displays)
     */
    private static void hallwayDoneDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        final EditText input = new EditText(contextActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setTitle("Done:");
        builder.setMessage("Please type in the name of the current hallway before finishing:");
        builder.setView(input);
        builder.setPositiveButton("Admit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String s = input.getText().toString();
                if(s == null) {
                    contextActivity.addHallway("NULL");
                } else {
                    contextActivity.addHallway(s);
                }
                saveAdfDialog(contextActivity);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.done1_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for giving the ADF a name before saving it.
     * @param contextActivity (context - activity where the dialog displays)
     */
    private static void saveAdfDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        final EditText input = new EditText(contextActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setTitle("Save Graph & ADF");
        builder.setMessage("Please type in the name of the Graph & ADF to be saved:");
        builder.setView(input);
        builder.setPositiveButton("Admit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String s = input.getText().toString();
                contextActivity.setADFName(s);
                contextActivity.finishPlan();
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.save_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog if the user want to load an existing ADF when starting a new session.
     * @param contextActivity (context - activity where the dialog displays)
     */
    public static void loadADFDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Load existing ADF:");
        builder.setMessage("Do you want to load an existing area description file (ADF) with a floorplan graph?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //load ADF
                chooseADFDialog(contextActivity);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //finish Dialog / do nothing
                levelDialog(contextActivity);
                contextActivity.setIsADFLoadingFinished(true);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.load_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog which ADF the user want to be loaded.
     * @param contextActivity (context - activity where the dialog displays)
     */
    private static void chooseADFDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the ADF you want to load (The corresponding graph will be automatically added, if it exists):");
        final List<ADFPair> adfList = contextActivity.createADFNameList();
        List<String> items = new ArrayList<String>();
        for(ADFPair pair : adfList) {
            items.add(pair.getName());
        }
        ArrayAdapter<String> arrayAdapterItems = new ArrayAdapter<String>(contextActivity,
                android.R.layout.simple_expandable_list_item_1, items);
        builder.setAdapter(arrayAdapterItems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ADFPair pair = adfList.get(which);
                resumeRecordingDialog(contextActivity, pair);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //finish Dialog / do nothing
                levelDialog(contextActivity);
                contextActivity.setIsADFLoadingFinished(true);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.load_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog to ask the user if he want to start the application in viewer or recording state.
     * Viewer state: The user can load an exisiting graph/adf and see his current position at the floor plans
     * Recording state: The user can load an existing graph/adf and resume recording.
     * @param contextActivity (context - activity where the dialog displays)
     * @param pair (selected adf pair to be loaded)
     */
    private static void resumeRecordingDialog(final GraphmapperActivity contextActivity, final ADFPair pair) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Resume Recording:");
        builder.setMessage("Do you want to resume recording or just view the current graph?");
        builder.setPositiveButton("Viewer", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Viewer
                contextActivity.setViewer(true);
                contextActivity.setLoadedADFPair(pair);
                contextActivity.initViewer();
            }
        });
        builder.setNegativeButton("Record", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Resume Recording
                levelDialog(contextActivity);
                contextActivity.setLoadedADFPair(pair);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.load_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog which level the user want to record.
     * @param contextActivity (context - activity where the dialog displays)
     */
    public static void levelDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        final EditText input = new EditText(contextActivity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setTitle("Level");
        builder.setMessage("Please type in the level you want to record:");
        builder.setView(input);
        builder.setPositiveButton("Admit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String s = input.getText().toString();
                int level;
                try {
                    level = Integer.parseInt(s);
                } catch(NumberFormatException e) {
                    level = -99;
                }
                contextActivity.setGlobalLevel(level);
            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog if the user want to reset the current hallway.
     * @param contextActivity (context - activity where the dialog displays)
     */
    public static void resetHallwayDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Reset hallway selected:");
        builder.setMessage("Are you sure you want to delete the current hallway?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //current hallway can be deleted
                contextActivity.resetHallway();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //finish Dialog / do nothing
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.reset_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for giving a room a name.
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (room measurement where the name will be stored)
     */
    public static void roomDialog(GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        final EditText input = new EditText(contextActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setTitle("Room");
        builder.setMessage("Please type in the room number you've just selected:");
        builder.setView(input);
        builder.setPositiveButton("Admit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String s = input.getText().toString();
                wallMeasurement.setText(s);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.room_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for giving a marker a name.
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (marker measurement where the name will be stored)
     */
    public static void markerDialog(GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        final EditText input = new EditText(contextActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setTitle("Marker");
        builder.setMessage("Please type in the name of the marker you've just selected:");
        builder.setView(input);
        builder.setPositiveButton("Admit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String s = input.getText().toString();
                wallMeasurement.setText(s);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.marker_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog if the user want to add a new level and finish the current hallway.
     * @param contextActivity (context - activity where the dialog displays)
     */
    public static void newLevelDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("New Level selected:");
        builder.setMessage("Are you sure you want to add a new level and finish the current hallway?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //current hallway has to be named
                newLevelHallwayTextDialog(contextActivity);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //finish Dialog => Cancel action in GraphmapperActivity
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.level_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for giving the current hallway a name after selecting the new level button
     * @param contextActivity (context - activity where the dialog displays)
     */
    private static void newLevelHallwayTextDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        final EditText input = new EditText(contextActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setTitle("Hallway");
        builder.setMessage("Please type in the name of the current hallway you want to finish:");
        builder.setView(input);
        builder.setPositiveButton("Admit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String s = input.getText().toString();
                if(s == null) {
                    contextActivity.addNewLevel("NULL");
                } else {
                    contextActivity.addNewLevel(s);
                }
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.level_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog if the user want to finish the current hallway.
     * @param contextActivity (context - activity where the dialog displays)
     */
    public static void hallwayFinishDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Add hallway selected:");
        builder.setMessage("Are you sure you want to finish the current hallway?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //current hallway has to be named
                hallwayTextDialog(contextActivity);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //finish Dialog => Cancel action in FloorplanAcitivity
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.hallway_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for giving the hallway a name after selecting the Add hallway button.
     * @param contextActivity (context - activity where the dialog displays)
     */
    private static void hallwayTextDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        final EditText input = new EditText(contextActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setTitle("Hallway");
        builder.setMessage("Please type in the name of the current hallway you want to finish:");
        builder.setView(input);
        builder.setPositiveButton("Admit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String s = input.getText().toString();
                if(s == null) {
                    contextActivity.addHallway("NULL");
                } else {
                    contextActivity.addHallway(s);
                }
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.hallway_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for choosing a type for a entrypoint
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (entrypoint measurement where the type will be stored)
     */
    public static void entryTypeDialog(final GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose your entrypoint type:");
        List<String> items = new ArrayList<String>();
        items.add("Door");
        items.add("Lift");
        items.add("Stairs");
        ArrayAdapter<String> arrayAdapterItems = new ArrayAdapter<String>(contextActivity,
                android.R.layout.simple_expandable_list_item_1, items);
        builder.setAdapter(arrayAdapterItems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                    case 0:
                        wallMeasurement.setMeasurementType(MeasurementType.DOOR);
                        entryNameDialog(contextActivity, wallMeasurement);
                        break;
                    case 1:
                        wallMeasurement.setMeasurementType(MeasurementType.LIFT);
                        entryNameDialog(contextActivity, wallMeasurement);
                        break;
                    case 2:
                        wallMeasurement.setMeasurementType(MeasurementType.STAIRS);
                        entryNameDialog(contextActivity, wallMeasurement);
                        break;
                    default:
                        break;
                }
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.door_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for giving a entrypoint a name.
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (entrypoint measurement where the name will be stored)
     */
    private static void entryNameDialog(final GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        final EditText input = new EditText(contextActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setTitle("Entrypoint ("+wallMeasurement.getMeasurementType().toString()+")");
        builder.setMessage("Please type in the name of the entrypoint you've just selected:");
        builder.setView(input);
        builder.setPositiveButton("Admit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String s = input.getText().toString();
                wallMeasurement.setText(s);
                if(wallMeasurement.getMeasurementType() == MeasurementType.LIFT) {
                    //a lift can have more than one connections
                    entryConnectionLiftDialog(contextActivity, wallMeasurement);
                } else {
                    //doors and stairs can have only one connection
                    entryConnectionDialog(contextActivity, wallMeasurement);
                }
            }
        });
        if(wallMeasurement.getMeasurementType() == MeasurementType.LIFT) {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.lift_icon));
        } else if(wallMeasurement.getMeasurementType() == MeasurementType.STAIRS) {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.stairs_icon));
        } else {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.door_icon));
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog if there are already connections recorded for this lift (entrypoint).
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (lift (entrypoint) measurement where the connection will be stored)
     */
    private static void entryConnectionLiftDialog(final GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Entrypoint \""+wallMeasurement.getText()+"\" ("+wallMeasurement.getMeasurementType().toString()+"):");
        builder.setMessage("Are there other recorded entrypoints connected to this lift?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //you can select connections
                connectEntryToLevelLift(contextActivity, wallMeasurement);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //you can't select connections yet
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.lift_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog to choose the level of the lift (entrypoint) to connect with.
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (lift (entrypoint) measurement where the connection will be stored)
     */
    private static void connectEntryToLevelLift(final GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the level of the recorded hallway where the lift \""
                +wallMeasurement.getText()+"\" is going:");
        final List<Hallway> hallwayList = contextActivity.getCurrentGraphList();
        final List<Integer> items = new ArrayList<Integer>();
        for(Hallway hallway : hallwayList) {
            int level = hallway.getLevel();
            if(!items.contains(level)) {
                items.add(level);
            }
        }
        Collections.sort(items); //sort levels upwards
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(contextActivity,
                android.R.layout.simple_spinner_dropdown_item, items);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int level = items.get(which);
                connectEntryToHallwayLift(contextActivity, wallMeasurement, level);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //cancel
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.lift_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for choosing the hallway of the lift (entrypoint) to connect with.
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (lift (entrypoint) measurement where the connection will be stored)
     * @param level (level of the connected lift)
     */
    private static void connectEntryToHallwayLift(final GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement, final int level) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the hallway that you want to connect with your lift \""
                +wallMeasurement.getText()+"\":");
        final List<Hallway> hallwayList = contextActivity.getCurrentGraphList();
        final List<String> items = new ArrayList<String>();
        for(Hallway hallway : hallwayList) {
            if(hallway.getLevel() == level) {
                items.add(hallway.getName());
            }
        }
        Collections.sort(items); //sort hallways upwards
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(contextActivity,
                android.R.layout.simple_spinner_dropdown_item, items);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String hallwayName = items.get(which);
                for(Hallway h : hallwayList) {
                    if(h.getName().equals(hallwayName)) {
                        connectEntryToEntryLift(contextActivity, wallMeasurement, level, h);
                        break;
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //cancel
            }
        });
        builder.setPositiveButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //go back to the last dialog
                connectEntryToLevelLift(contextActivity, wallMeasurement);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.lift_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for choosing the lift (entrypoint) to connect with.
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (lift (entrypoint) measurement where the connection will be stored)
     * @param level (level of the connected lift)
     * @param hallway (hallway of the connected lift)
     */
    private static void connectEntryToEntryLift(final GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement, final int level, final Hallway hallway) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the entrypoint that you want to connect with your lift \""
                +wallMeasurement.getText()+"\":");
        final List<String> items = new ArrayList<String>();
        final List<Entrypoint> entrypointList = new ArrayList<Entrypoint>(hallway.getConnections());
        for(Entrypoint entrypoint : entrypointList) {
            //add only unconnected entrypoints from type LIFT to the item list (they can have more than one connection)
            if(entrypoint.getType() == MeasurementType.LIFT) {
                //go over exisiting connections
                boolean exists = false;
                for(int i = 0; i < wallMeasurement.getHallwayToList().size(); i++) {
                    if((hallway.getName().equals(wallMeasurement.getHallwayToList().get(i).getName()))) {
                        //same hallway & entrypoint already connected
                        exists = true;
                    }
                }
                if(!exists) {
                    items.add(entrypoint.getName());
                }
            }
        }
        if((items.isEmpty()) || (entrypointList.isEmpty())) {
            builder.setMessage("There are no unconnected lifts in this hallway!");
        } else {
            Collections.sort(items); //sort entrys upwards
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(contextActivity,
                    android.R.layout.simple_spinner_dropdown_item, items);
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String name = items.get(which);
                    Entrypoint connection = null;
                    for(Entrypoint e : entrypointList) {
                        if(e.getName().equals(name)) {
                            connection = e;
                            break;
                        }
                    }
                    if (connection != null) {
                        wallMeasurement.addConnection(connection.getPositionFrom(), hallway); //Set connection of the new measurement
                        entryConnectionLiftDialog(contextActivity, wallMeasurement); //repeat (there can be more than one connection)
                    }
                }
            });
        }
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //cancel
            }
        });
        builder.setPositiveButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //go back to the last dialog
                connectEntryToHallwayLift(contextActivity, wallMeasurement, level);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.lift_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     * Dialog if the connection for the entrypoint is already recorded.
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (entrypoint measurement where the connection will be stored)
     */
    private static void entryConnectionDialog(final GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Entrypoint \""+wallMeasurement.getText()+"\" ("+wallMeasurement.getMeasurementType().toString()+"):");
        builder.setMessage("Is the entrypoint / hallway you want to connect to already recorded?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //you can select the connection
                connectEntryToLevel(contextActivity, wallMeasurement);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //you can't select the connection yet
            }
        });
        if(wallMeasurement.getMeasurementType() == MeasurementType.STAIRS) {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.stairs_icon));
        } else {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.door_icon));
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog to choose the level of the entrypoint to connect with.
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (entrypoint measurement where the connection will be stored)
     */
    private static void connectEntryToLevel(final GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the level of the hallway that you want to connect with your entrypoint \""
                +wallMeasurement.getText()+"\" ("+wallMeasurement.getMeasurementType().toString()+"):");
        final List<Hallway> hallwayList = contextActivity.getCurrentGraphList();
        final List<Integer> items = new ArrayList<Integer>();
        for(Hallway hallway : hallwayList) {
            int level = hallway.getLevel();
            if(!items.contains(level)) {
                items.add(level);
            }
        }
        Collections.sort(items); //sort levels upwards
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(contextActivity,
                android.R.layout.simple_spinner_dropdown_item, items);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int level = items.get(which);
                connectEntryToHallway(contextActivity, wallMeasurement, level);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //cancel
            }
        });
        if(wallMeasurement.getMeasurementType() == MeasurementType.STAIRS) {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.stairs_icon));
        } else {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.door_icon));
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for choosing the hallway of the entrypoint to connect with.
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (entrypoint measurement where the connection will be stored)
     * @param level (level of the connected entrypoint)
     */
    private static void connectEntryToHallway(final GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement, final int level) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the hallway that you want to connect with your entrypoint \""
                +wallMeasurement.getText()+"\" ("+wallMeasurement.getMeasurementType().toString()+"):");
        final List<Hallway> hallwayList = contextActivity.getCurrentGraphList();
        final List<String> items = new ArrayList<String>();
        for(Hallway hallway : hallwayList) {
            if(hallway.getLevel() == level) {
                items.add(hallway.getName());
            }
        }
        Collections.sort(items); //sort hallways upwards
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(contextActivity,
                android.R.layout.simple_spinner_dropdown_item, items);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String hallwayName = items.get(which);
                for(Hallway h : hallwayList) {
                    if(h.getName().equals(hallwayName)) {
                        connectEntryToEntry(contextActivity, wallMeasurement, level, h);
                        break;
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //cancel
            }
        });
        builder.setPositiveButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //go back to the last dialog
                connectEntryToLevel(contextActivity, wallMeasurement);
            }
        });
        if(wallMeasurement.getMeasurementType() == MeasurementType.STAIRS) {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.stairs_icon));
        } else {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.door_icon));
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for choosing the entrypoint to connect with.
     * @param contextActivity (context - activity where the dialog displays)
     * @param wallMeasurement (entrypoint measurement where the connection will be stored)
     * @param level (level of the connected entrypoint)
     * @param hallway (hallway of the connected entrypoint)
     */
    private static void connectEntryToEntry(final GraphmapperActivity contextActivity, final WallMeasurement wallMeasurement, final int level, final Hallway hallway) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the entrypoint that you want to connect with your entrypoint \""
                +wallMeasurement.getText()+"\" ("+wallMeasurement.getMeasurementType().toString()+"):");
        final List<String> items = new ArrayList<String>();
        final List<Entrypoint> entrypointList = new ArrayList<Entrypoint>(hallway.getConnections());
        for(Entrypoint entrypoint : entrypointList) {
            //add only unconnected entrypoints from type DOOR and STAIRS to the item list
            if((entrypoint.getType() != MeasurementType.LIFT) &&
                    (((entrypoint.getPositionToList() == null) && (entrypoint.getHallwayToIDList() == null))
                    || ((entrypoint.getPositionToList().isEmpty()) && (entrypoint.getHallwayToIDList().isEmpty())))) {
                items.add(entrypoint.getName());
            }
        }
        if((items.isEmpty()) || (entrypointList.isEmpty())) {
            builder.setMessage("There are no unconnected entrypoints in this hallway!");
        } else {
            Collections.sort(items); //sort entrys upwards
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(contextActivity,
                    android.R.layout.simple_spinner_dropdown_item, items);
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String name = items.get(which);
                    Entrypoint connection = null;
                    for(Entrypoint e : entrypointList) {
                        if(e.getName().equals(name)) {
                            connection = e;
                            break;
                        }
                    }
                    if (connection != null) {
                        wallMeasurement.addConnection(connection.getPositionFrom(), hallway); //Set connection of the new measurement
                    }
                }
            });
        }
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //cancel
            }
        });
        builder.setPositiveButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //go back to the last dialog
                connectEntryToHallway(contextActivity, wallMeasurement, level);
            }
        });
        if(wallMeasurement.getMeasurementType() == MeasurementType.STAIRS) {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.stairs_icon));
        } else {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.door_icon));
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for asking missing connections after finishing the recording.
     * @param contextActivity (context - activity where the dialog displays)
     */
    public static void doneConnectStart(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Missing entrypoint connections:");
        builder.setMessage("There are unconnected entrypoints. Please choose for every entrypoint if you want to CONNECT it or let the connection UNKNOWN.");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Get next element from stack and ask connections
                if(!contextActivity.getUnconnectedEntrys().empty()) {
                    Entrypoint next = contextActivity.getUnconnectedEntrys().pop();
                    if(next.getType() == MeasurementType.LIFT) {
                        doneConnectEntryLift(contextActivity, next);
                    } else {
                        doneConnectEntry(contextActivity, next);
                    }
                }
            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for asking if the user want to connect a entrypoint with a missing connection.
     * @param contextActivity (context - activity where the dialog displays)
     * @param entrypoint (entrypoint with a missing connection)
     */
    private static void doneConnectEntry(final GraphmapperActivity contextActivity, final Entrypoint entrypoint) {
        Hallway hallwayFrom = contextActivity.getGraph().searchHallway(entrypoint.getHallwayFromID());
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Entrypoint \""+entrypoint.getName()+"\" ("+entrypoint.getType().toString()
                +") from Hallway " + hallwayFrom.getName() + ":");
        builder.setMessage("Do you want to connect this entrypoint with its destination?");
        builder.setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //connection can be choosen
                doneConnectEntryToLevel(contextActivity, entrypoint);
            }
        });
        builder.setNegativeButton("UNKNOWN", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Connection can't be choosen yet. Get next element and ask connections.
                if(!contextActivity.getUnconnectedEntrys().empty()) {
                    Entrypoint next = contextActivity.getUnconnectedEntrys().pop();
                    if(next.getType() == MeasurementType.LIFT) {
                        doneConnectEntryLift(contextActivity, next);
                    } else {
                        doneConnectEntry(contextActivity, next);
                    }
                } else {
                    contextActivity.saveGraph();
                }
            }
        });
        if(entrypoint.getType() == MeasurementType.STAIRS) {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.stairs_icon));
        } else {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.door_icon));
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for asking if the user want to connect a lift (entrypoint) with a connection.
     * @param contextActivity (context - activity where the dialog displays)
     * @param entrypoint (lift (entrypoint) with a missing connection)
     */
    private static void doneConnectEntryLift(final GraphmapperActivity contextActivity, final Entrypoint entrypoint) {
        Hallway hallwayFrom = contextActivity.getGraph().searchHallway(entrypoint.getHallwayFromID());
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Lift \""+entrypoint.getName()+"\" from Hallway " + hallwayFrom.getName() + ":");
        String message = "Do you want to connect this lift with other lift entrypoints?";
        boolean first = true;
        for(int id : entrypoint.getHallwayToIDList()) {
            Hallway h = contextActivity.getGraph().searchHallway(id);
            if(h != null) {
                if(first) {
                    message += " (existing connections: ";
                    first = false;
                } else {
                    message += ", ";
                }
                message += h.getName() + " - level: " + h.getLevel();
            }
        }
        if(!first) {
            message += ")";
        }
        builder.setMessage(message);
        builder.setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //connection can be choosen
                doneConnectEntryToLevelLift(contextActivity, entrypoint);
            }
        });
        builder.setNegativeButton("NO THANKS", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Connection can't be choosen yet. Get next element and ask connections.
                if(!contextActivity.getUnconnectedEntrys().empty()) {
                    Entrypoint next = contextActivity.getUnconnectedEntrys().pop();
                    if(next.getType() == MeasurementType.LIFT) {
                        doneConnectEntryLift(contextActivity, next);
                    } else {
                        doneConnectEntry(contextActivity, next);
                    }
                } else {
                    contextActivity.saveGraph();
                }
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.lift_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog which level the connection of the lift (entrypoint) is located.
     * @param contextActivity (context - activity where the dialog displays)
     * @param entrypoint (lift (entrypoint) the user want to connect)
     */
    private static void doneConnectEntryToLevelLift(final GraphmapperActivity contextActivity, final Entrypoint entrypoint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the level of the hallway that you want to connect with your lift \""
                +entrypoint.getName()+"\":");
        final List<Hallway> hallwayList = contextActivity.getCurrentGraphList();
        final List<Integer> items = new ArrayList<Integer>();
        for(Hallway hallway : hallwayList) {
            int level = hallway.getLevel();
            if(!items.contains(level)) {
                items.add(level);
            }
        }
        Collections.sort(items); //sort level upwards
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(contextActivity,
                android.R.layout.simple_spinner_dropdown_item, items);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int level = items.get(which);
                doneConnectEntryToHallwayLift(contextActivity, entrypoint, level);
            }
        });
        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //go back
                doneConnectEntryLift(contextActivity, entrypoint);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.lift_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog which hallway the connection of the lift (entrypoint) is located.
     * @param contextActivity (context - activity where the dialog displays)
     * @param entrypoint (lift (entrypoint) with the user want to connect)
     * @param level (level of the connected lift)
     */
    private static void doneConnectEntryToHallwayLift(final GraphmapperActivity contextActivity, final Entrypoint entrypoint, final int level) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the hallway that you want to connect with your lift \""
                +entrypoint.getName()+"\":");
        final List<Hallway> hallwayList = contextActivity.getCurrentGraphList();
        final List<String> items = new ArrayList<String>();
        for(Hallway hallway : hallwayList) {
            if(hallway.getLevel() == level) {
                items.add(hallway.getName());
            }
        }
        Collections.sort(items); //sort hallways upwards
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(contextActivity,
                android.R.layout.simple_spinner_dropdown_item, items);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String hallwayName = items.get(which);
                for(Hallway h : hallwayList) {
                    if(h.getName().equals(hallwayName)) {
                        doneConnectEntryToEntryLift(contextActivity, entrypoint, level, h);
                        break;
                    }
                }
            }
        });
        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //go back to the last dialog
                doneConnectEntryToLevelLift(contextActivity, entrypoint);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.lift_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     *
     * @param contextActivity
     * @param entrypoint
     * @param level
     * @param hallway
     */
    /**
     * Dialog which lift the user want to connect with the lift (entrypoint).
     * @param contextActivity (context - activity where the dialog displays)
     * @param entrypoint (lift (entrypoint) the user want to connect)
     * @param level (level of the connected lift)
     * @param hallway (hallway of the connected lift)
     */
    private static void doneConnectEntryToEntryLift(final GraphmapperActivity contextActivity, final Entrypoint entrypoint, final int level, final Hallway hallway) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the entrypoint that you want to connect with your lift \""
                +entrypoint.getName()+"\":");
        final List<String> items = new ArrayList<String>();
        final List<Entrypoint> entrypointList = new ArrayList<Entrypoint>(hallway.getConnections());
        for(Entrypoint e : entrypointList) {
            //add only entrypoints from type LIFT to the item list
            if((e.getType() == MeasurementType.LIFT)) {
                //go over exisiting connections
                boolean exists = false;
                for(int i = 0; i < entrypoint.getHallwayToIDList().size(); i++) {
                    Hallway h = contextActivity.getGraph().searchHallway(entrypoint.getHallwayToIDList().get(i));
                    if((h != null) && (hallway.getName().equals(h.getName()))) {
                        //same hallway & lift already connected & you can't connect with yourself
                        exists = true;
                    }
                }
                if(!exists) {
                    items.add(e.getName());
                }
            }
        }
        if((items.isEmpty()) || (entrypointList.isEmpty())) {
            builder.setMessage("There are no unconnected entrypoints in this hallway!");
        } else {
            Collections.sort(items); //sort entrys upwards
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(contextActivity,
                    android.R.layout.simple_spinner_dropdown_item, items);
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String name = items.get(which);
                    Entrypoint connection = null;
                    for(Entrypoint e : entrypointList) {
                        if(e.getName().equals(name)) {
                            connection = e;
                            break;
                        }
                    }
                    if (connection != null) {
                        //Set both connection of both elements
                        entrypoint.addConnection(connection.getPositionFrom(), hallway.getID());
                        connection.addConnection(entrypoint.getPositionFrom(), entrypoint.getHallwayFromID());
                        //Start again
                        doneConnectEntryLift(contextActivity, entrypoint);
                    }
                }
            });
        }
        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                doneConnectEntryToHallwayLift(contextActivity, entrypoint, level); //go back to the last dialog
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.lift_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog which level the connection of a missing entrypoint is located.
     * @param contextActivity (context - activity where the dialog displays)
     * @param entrypoint (entrypoint with a missing connection)
     */
    private static void doneConnectEntryToLevel(final GraphmapperActivity contextActivity, final Entrypoint entrypoint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the level of the hallway that you want to connect with your entrypoint \""
                +entrypoint.getName()+"\" ("+entrypoint.getType().toString()+"):");
        final List<Hallway> hallwayList = contextActivity.getCurrentGraphList();
        final List<Integer> items = new ArrayList<Integer>();
        for(Hallway hallway : hallwayList) {
            int level = hallway.getLevel();
            if(!items.contains(level)) {
                items.add(level);
            }
        }
        Collections.sort(items); //sort level upwards
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(contextActivity,
                android.R.layout.simple_spinner_dropdown_item, items);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int level = items.get(which);
                doneConnectEntryToHallway(contextActivity, entrypoint, level);
            }
        });
        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //go back
                doneConnectEntry(contextActivity, entrypoint);
            }
        });
        if(entrypoint.getType() == MeasurementType.STAIRS) {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.stairs_icon));
        } else {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.door_icon));
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog which hallway the connection of a missing entrypoint is located.
     * @param contextActivity (context - activity where the dialog displays)
     * @param entrypoint (entrypoint with a missing connection)
     * @param level (level of the missing connection)
     */
    private static void doneConnectEntryToHallway(final GraphmapperActivity contextActivity, final Entrypoint entrypoint, final int level) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the hallway that you want to connect with your entrypoint \""
                +entrypoint.getName()+"\" ("+entrypoint.getType().toString()+"):");
        final List<Hallway> hallwayList = contextActivity.getCurrentGraphList();
        final List<String> items = new ArrayList<String>();
        for(Hallway hallway : hallwayList) {
            if(hallway.getLevel() == level) {
                items.add(hallway.getName());
            }
        }
        Collections.sort(items); //sort hallways upwards
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(contextActivity,
                android.R.layout.simple_spinner_dropdown_item, items);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String hallwayName = items.get(which);
                for(Hallway h : hallwayList) {
                    if(h.getName().equals(hallwayName)) {
                        doneConnectEntryToEntry(contextActivity, entrypoint, level, h);
                        break;
                    }
                }
            }
        });
        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //go back to the last dialog
                doneConnectEntryToLevel(contextActivity, entrypoint);
            }
        });
        if(entrypoint.getType() == MeasurementType.STAIRS) {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.stairs_icon));
        } else {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.door_icon));
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog which entrypoint is the connection of a missing entrypoint.
     * @param contextActivity (context - activity where the dialog displays)
     * @param entrypoint (entrypoint with a missing connection)
     * @param level (level of the missing connection)
     * @param hallway (hallway of the connection)
     */
    private static void doneConnectEntryToEntry(final GraphmapperActivity contextActivity, final Entrypoint entrypoint, final int level, final Hallway hallway) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Choose the entrypoint that you want to connect with your entrypoint \""
                +entrypoint.getName()+"\" ("+entrypoint.getType().toString()+"):");
        final List<String> items = new ArrayList<String>();
        final List<Entrypoint> entrypointList = new ArrayList<Entrypoint>(hallway.getConnections());
        for(Entrypoint e : entrypointList) {
            //add only unconnected entrypoints from type DOOR and STAIRS to the item list
            if((e.getType() != MeasurementType.LIFT) && (!e.getName().equals(entrypoint.getName())) &&
                    (((e.getPositionToList() == null) && (e.getHallwayToIDList() == null))
                            || ((e.getPositionToList().isEmpty()) && (e.getHallwayToIDList().isEmpty())))) {
                items.add(e.getName());
            }
        }
        if((items.isEmpty()) || (entrypointList.isEmpty())) {
            builder.setMessage("There are no unconnected entrypoints in this hallway!");
        } else {
            Collections.sort(items); //sort entrys upwards
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(contextActivity,
                    android.R.layout.simple_spinner_dropdown_item, items);
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String name = items.get(which);
                    Entrypoint connection = null;
                    for(Entrypoint e : entrypointList) {
                        if(e.getName().equals(name)) {
                            connection = e;
                            break;
                        }
                    }
                    if (connection != null) {
                        //Set both connection of both elements
                        entrypoint.addConnection(connection.getPositionFrom(), hallway.getID());
                        connection.addConnection(entrypoint.getPositionFrom(), entrypoint.getHallwayFromID());
                        //Remove connection from stack (entrypoint has been removed earlier)
                        contextActivity.getUnconnectedEntrys().remove(connection);
                        //Get next element from stack and ask connections
                        if(!contextActivity.getUnconnectedEntrys().empty()) {
                            Entrypoint next = contextActivity.getUnconnectedEntrys().pop();
                            if(next.getType() == MeasurementType.LIFT) {
                                doneConnectEntryLift(contextActivity, next);
                            } else {
                                doneConnectEntry(contextActivity, next);
                            }
                        } else {
                            contextActivity.saveGraph(); //Save graph
                        }
                    }
                }
            });
        }
        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                doneConnectEntryToHallway(contextActivity, entrypoint, level); //go back to the last dialog
            }
        });
        if(entrypoint.getType() == MeasurementType.STAIRS) {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.stairs_icon));
        } else {
            builder.setIcon(createScaledIcon(contextActivity, R.drawable.door_icon));
        }
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for choosing a level to be drawn on a canvas.
     * @param contextActivity (context - activity where the dialog displays)
     */
    public static void chooseLevelToDrawDialog(final GraphmapperActivity contextActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Please choose the level of your building to draw a floorplan for it:");
        final List<Hallway> hallwayList = contextActivity.getCurrentGraphList();
        final List<Integer> items = new ArrayList<Integer>();
        for(Hallway hallway : hallwayList) {
            int level = hallway.getLevel();
            if(!items.contains(level)) {
                items.add(level);
            }
        }
        Collections.sort(items); //sort level upwards
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(contextActivity,
                android.R.layout.simple_spinner_dropdown_item, items);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int level = items.get(which);
                GraphmapperActivity.PlanView p = contextActivity.drawLevelOnCanvas(level, null, null);
                contextActivity.setCurrentLevelDraw(level);
                contextActivity.setCurrentPlanDraw(p);
                contextActivity.getDrawText().append(", level: " + level);
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.done1_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Dialog for choosing a hallway from a level to be drawn on a canvas.
     * @param contextActivity (context - activity where the dialog displays)
     * @param level (selected level)
     */
    public static void chooseHallwayToDrawDialog(final GraphmapperActivity contextActivity, int level) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle("Select hallway: Please choose a hallway of your level!");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //do nothing
            }
        });
        List<Hallway> hallwayList = contextActivity.getCurrentGraphList();
        final List<String> items = new ArrayList<String>();
        final List<Hallway> levelHallwayList = new ArrayList<Hallway>();
        for(Hallway h : hallwayList) {
            if(h.getLevel() == level) {
                items.add(h.getName());
                levelHallwayList.add(h);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(contextActivity,
                android.R.layout.simple_spinner_dropdown_item, items);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String s = items.get(which);
                for(Hallway h : levelHallwayList) {
                    if(h.getName().equals(s)) {
                        //found hallway
                        GraphmapperActivity.PlanView p = contextActivity.drawHallwayOnCanvas(h, null, null);
                        contextActivity.setCurrentPlanDraw(p);
                        contextActivity.setCurrentHallwayDraw(h);
                        contextActivity.getDrawText().append(", Hallway: " + s);
                        break;
                    }
                }
            }
        });
        builder.setIcon(createScaledIcon(contextActivity, R.drawable.done1_icon));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}