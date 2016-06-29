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

/**
 * A simple demo application showing how to build a 2d floor plan for indoor navigation purpose using Tango.
 * It is based on the Java Floorplan example.
 * This package contains the classes for measuring and building the plan and also for displaying it on the tablet.
 *
 * <p>Icon "Graphmapper" of this project / application:</p>
 *      <p>- ic_launcher.png (created by hannah.ne)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/ic_launcher.png" /></p>
 *
 * <p>References of external icons that are used in this project from https://thenounproject.com/:</p>
 *      <p>- done1_icon.png (created by Matthew Bailey from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/done1_icon.png" /></p>
 *      <p>- done2_icon.png (created by Kyle Lynn from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/done2_icon.png" /></p>
 *      <p>- door_icon.png (created by Arthur Shlain from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/door_icon.png" /></p>
 *      <p>- entry_icon.png (created by Doub.co from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/entry_icon.png" /></p>
 *      <p>- hallway_icon.png (created by Designify.me from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/hallway_icon.png" /></p>
 *      <p>- level_icon.png (created by Mentaltoy from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/level_icon.png" /></p>
 *      <p>- lift_icon.png (created by Miguel C Balandrano from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/lift_icon.png" /></p>
 *      <p>- load_icon.png (created by BraveBros. from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/load_icon.png" /></p>
 *      <p>- marker_icon.png (created by Edward Boatman from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/marker_icon.png" /></p>
 *      <p>- reset_icon.png (created by Mister Pixel from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/reset_icon.png" /></p>
 *      <p>- save_icon.png (created by BraveBros. from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/save_icon.png" /></p>
 *      <p>- stairs_icon.png (public domain from Noun Project)</p>
 * <p><img src="/Users/konstantinklinger/Documents/Master/Masterarbeit/Entwicklung/okul-tango-examples-java-master/graphmapper_demo/app/src/main/res/drawable-xxhdpi/stairs_icon.png" /></p>
 */
package com.projecttango.unibw.graphmapper.floorplan;

