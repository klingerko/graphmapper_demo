# Graphmapper (Demo) - 2D floor plan mapping for indoor navigation graph network

PlayStore: https://play.google.com/store/apps/details?id=com.projecttango.unibw.graphmapper

YouTube:   https://youtu.be/QVfo-_gM7gA

Note: You need a Tango enabled device to run this application.

Accuracy of the measurements (more detailed information is coming soon):
* between 2 - 4% or 5 - 20cm deviation (references were taken with a Bosch DLE 50)
* glas walls or doors, light sources and the sun have an effect on the accuracy

Basic sum up of the functions:
* wall, point of interest (room, entrypoint, marker) and obstacle (cut out an obstacle) measurements
* markers and entrypoints can be placed on the ground or on the wall
* entrypoints can be from type LIFT, STAIRS or DOOR
* functions to add a new hallway / level
* draw floor plan of a whole level and zoom in a specific hallway
* optionally shows names, length and positions of the POIs
* shows current position (in viewer mode)
* save current plan view as jpg image (directory: "Internal Storage"/Graphmapper)
* you can save the adf & graph in JSON format (directory: "Internal Storage"/Graphmapper)
* reload previous sessions and resume recording after relocalization in adf file or start the viewer mode

Graph structure - According to convert the graph format into IndoorGML (coming soon) there was used a simple model:
* nodes: hallways (can have rooms, entrypoints and markers)
* edges: entrypoints (DOOR, LIFT, STAIRS) are the connections between hallways

![alt tag](https://github.com/King-Konsto/graphmapper_demo/blob/master/Screenshot_2016-06-28-16-01-17.png)

A small demo application that allows the user to create a navigation graph of indoor environments 
and a 2D floor plan of a building. The user can mark his point of interests (rooms, entrypoints, etc.) 
and connect hallways to build a hallway network of the building. This is called indoor navigation graph or network. 
As a result the user can save the adf with the navigation graph and load it in the next sessions. 
He can also draw the floor plan and save it as a jpg file. The big aim is to create a graph of an 
indoor environment that a indoor navigation system can use this graph as a map to navigate. 
The application can also be started in a viewer mode. After loading an existing adf and graph and 
relocalization in the area the user can view the floor plans and see his current position in it.

 To get wall measurements or POI measurements you have to click on the wall. After that there will be a plane 
 fitting on the wall. The application uses TangoSupportLibrary to do plane fitting using the point cloud data. 
 When the user clicks on the display, plane detection is done on the surface at the location of 
 the click and a 3D object will be placed in the scene anchored at that location. A Wall Measurement 
 will be recorded for that plane.
 
 You need to take exactly one measurement per wall in clockwise order. After you have taken all the 
 measurements you can press the 'Done' button and the adf and graph will be saved and the final result 
 can be drawn as a 2D floor plan along with labels showing the sizes of the walls and names of the POIs 
 and hallways.
 
 Added changes of Google's Okul update from June 9th (13.06.2016).
 
 ![alt tag](https://github.com/King-Konsto/graphmapper_demo/blob/master/Screenshot_2016-06-28-16-01-37.png)
 ![alt tag](https://github.com/King-Konsto/graphmapper_demo/blob/master/Screenshot_2016-06-28-16-25-52.png)
 ![alt tag](https://github.com/King-Konsto/graphmapper_demo/blob/master/Screenshot_2016-06-28-16-26-21.png)
