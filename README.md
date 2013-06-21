# WolframCA #

## What Is WolframCA? ##

This is an android app which displays the successive 'generations' of a one-dimensional cellular
automaton (CA).  This was inspired by chapter 6 of Allen B. Downey's excellent book, 
'*Think Complexity*', where he describes the set of CAs as presented by Steven Wolfram in a set of 
early 1980s papers.

*Wolfram Mathwold*'s page '[Elementary Cellular Automaton](http://mathworld.wolfram.com/ElementaryCellularAutomaton.html)'
	has an excellent summary of the topic.

The book *'Think Complexity'* is freely available as a PDF from [the publisher's bookpage](http://www.greenteapress.com/compmod/). 

### Installation ###
**[Version 1.4 of the app is available in the Play Store](https://play.google.com/store/apps/details?id=net.nologin.meep.ca)**, 
and supports devices running Android **2.3.3** (API Level: 10, GINGERBREAD_MR1) or later. 

### The Display ###

Apart from a menu bar at the top, the application dedicates most of the area to the CA's output. 
Below is an example of [Rule 90](http://mathworld.wolfram.com/Rule90.html) (click for bigger):

[![click for bigger](http://barryoneill.github.io/WolframCA/screenshots/rule90_6pxcell_start_small.png)](http://barryoneill.github.io/WolframCA/screenshots/rule90_6pxcell_start.png)

The above screenshot was taken at a 'zoom' of 6 pixels per cell on a Samsung Galaxy Tab 8.9".  The 
number of pixels per cell can be chosen (see below), and the view supports screen rotation.  

Regardless of which rule number is chosen, we start the first generation as empty (all cells off), 
except for a single cell in the center of the topmost 'cell row' on the visible display.  Here's a 
closer look at  the view above for the first 15 steps (generations) of rule 90:

![click for bigger](http://barryoneill.github.io/WolframCA/screenshots/rule90_6pxcell_firstgenerations.png)

### Menus/Options ###

The ActionBar across the top shows the currently selected rule, and offers dialogs such as:

* **Change Rule** - Select one of the 256 rules using a seekbar, with prev/next buttons for finer
	selection of adjacent rules (click for bigger).

	[![click for bigger](http://barryoneill.github.io/WolframCA/screenshots/changerule_overview_small.png)](http://barryoneill.github.io/WolframCA/screenshots/changerule_overview.png)

* **Change Zoom** - Change the number of pixels in the side length of each square 'cell'.  Note 
	that smaller cells will result in more generations being calculated during scrolling; This 
	amount of calculation + bitmap generation is quite taxing on some older, or single core devices
	(click for bigger).

	[![click for bigger](http://barryoneill.github.io/WolframCA/screenshots/changezoom_overview_small.png)](http://barryoneill.github.io/WolframCA/screenshots/changezoom_overview.png)

* **Back to Top** - Move the view right back up to the first generation of the CA. 

* **Settings** - Secondary options, such as debug (a feature of the [TiledBitmapView](https://github.com/barryoneill/TiledBitmapView)
	, shows the tile grid along with info on variables), an 'About' dialog, and links to this project page. 

## The Code ##

### IDE Setup ###

To build this application, the following dependency libraries are needed:

* [TiledBitmapView](https://github.com/barryoneill/TiledBitmapView) - This is an android view which
	supports loading the required content on demand in a tilewise manner.
* [ActionBarSherlock](http://actionbarsherlock.com/) - This app supports API 10, but the 
	[ActionBar](http://developer.android.com/guide/topics/ui/actionbar.html) component isn't 
	available until API 11 (Android 3.0).  ABS introduces its own dependency on the 
	[Android Support Library](http://developer.android.com/tools/extras/support-library.html).

This project contains no IDE specific files.  Pointing the 'import project' wizard of your favourite 
IDE (i.e. [Android Studio](http://developer.android.com/sdk/installing/studio.html) / 
[Intellij IDEA](http://www.jetbrains.com/idea/)) at the root of this repository should be sufficient.
If prompted, select Android 4.x to compile, but keep Android 2.3.3/API 10 as the minimum SDK. Add
the libraries mentioned above as dependencies, and the project should compile and deploy.

### Getting started with the code ###

In order to understand how the tiles are fetched and displayed, you should take a look at 
the README of the [TiledBitmapView library](https://github.com/barryoneill/TiledBitmapView).  
All the classes are well commented, so the main/settings activity classes should be self explanatory. 
The more subject-matter related classes are:

* [**WolframTileProvider**](https://github.com/barryoneill/WolframCA/blob/master/src/net/nologin/meep/ca/model/WolframTileProvider.java)
	- This implementation of the TBV library's [TileProvider](http://barryoneill.github.io/TiledBitmapView/javadoc/index.html?net/nologin/meep/tbv/TileProvider.html) 
		interface is where the majority of the interesting code is.  The generation data of the 
		currently selected rule is calculated and stored here, and the code for rendering this 
		data into tiles for display is also in this class.
* [**WolframRuleTable**](https://github.com/barryoneill/WolframCA/blob/master/src/net/nologin/meep/ca/model/WolframRuleTable.java)
	- The code for calculating the next generation's state for a specific rule/cell.
* [**WolframCAView**](https://github.com/barryoneill/WolframCA/blob/master/src/net/nologin/meep/ca/view/WolframCAView.java)
	- Subclass of the library's [TiledBitmapView](https://github.com/barryoneill/TiledBitmapView) 
		class, encapsulates code to let the main activity interact with our provider implementation.


## To Do ##

Except for fixing bugs that I might discover, this was just a fun learning exercise for me; I don't 
have any further features planned at the moment.  However, if you have a feature request or feedback
(or find any bugs!), please get in touch.  
 

