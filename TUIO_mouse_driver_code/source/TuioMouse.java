/*
    TUIO Mouse Driver - part of the reacTIVision project
    http://reactivision.sourceforge.net/

    Copyright (c) 2005-2016 Martin Kaltenbrunner <martin@tuio.org>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

/*
	CMU MHCI Harman Capstone, Technical POC Part 4: TUIO Mouse Driver
	Based on source/TuioMouse.java found at https://github.com/mkalten/TUIO11_Mouse
*/

import java.awt.*;
import java.awt.event.*;
import TUIO.*;

public class TuioMouse implements TuioListener {
	
	private Robot robot = null;
	private int width = 0;
	private int height = 0;
	private long mouse = -1;
	private int y = 0;
	private int x = 0;
	/* 
	Defining Variables for Scaling X and Y based on Google Chrome Window
	displaying Framer on the projector. Values below will vary depending
	on projector setup
	*/
	private int minY = 598;
	private double scale = 4.31;
	private int minX = 763;
	private double scaleX = 4.7;
	// Offset the X value because the window is not all the way to the left
	private int xOffset = 200;

	public void addTuioObject(TuioObject tobj) {}
	public void updateTuioObject(TuioObject tobj) {}	
	public void removeTuioObject(TuioObject tobj) {}
	public void addTuioBlob(TuioBlob tblb) {}
	public void updateTuioBlob(TuioBlob tblb) {}	
	public void removeTuioBlob(TuioBlob tblb) {}
	public void refresh(TuioTime bundleTime) {}

	// translateX
	public int translateX(int xCoordinate){
		// Take in the raw X coordinate on a scale of 0-1079 and map to
		// be a point in the Chrome Window displaying Framer (scaling)
		return xOffset + (int)((double)(xCoordinate - minX) * scaleX);
	}

	// translateY
	public int translateY(int yCoordinate) {
		// Take in the raw Y coordinate on a scale of 0-1919 and map to
		// be a point in the Chrome Window displaying Framer (scaling)
		return (int)((double)(yCoordinate - minY) * scale);
	}
	
	// addTuioCursor
	public void addTuioCursor(TuioCursor tcur) {
		// Adds a new mouse object to the canvas and clicks it immediately
		if (mouse<0) {
			mouse = tcur.getSessionID();
			x = translateX(tcur.getScreenX(width));
			y = translateY(tcur.getScreenY(height));
			if (robot!=null) robot.mouseMove(x, y);
			if (robot!=null) robot.mousePress(InputEvent.BUTTON1_MASK);
		}
	}

	// updateTuioCursor
	public void updateTuioCursor(TuioCursor tcur) {
		// Update the position of the existing mouse based on the user's scaled
		// hand position
		if (mouse==tcur.getSessionID()) {
			x = translateX(tcur.getScreenX(width));
			y = translateY(tcur.getScreenY(height));
			if (robot!=null) robot.mouseMove(x, y);
		} 
	}
	
	// removeTuioCursor
	public void removeTuioCursor(TuioCursor tcur) {
		// Remove the mouse from the canvas
		if (robot!=null) robot.mouseRelease(InputEvent.BUTTON1_MASK);
		if (mouse==tcur.getSessionID()) {
			mouse=-1;
		} else {
			if (robot!=null) robot.mouseRelease(InputEvent.BUTTON1_MASK);
		}
		
	}
	
	public TuioMouse() {
		try { robot = new Robot(); }
		catch (Exception e) {
			System.out.println("failed to initialize mouse robot");
			System.exit(0);
		}
		
		width  = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		height = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
	}

	/*
	Listen for TUIO messages on port 3333 and handle accordingly (from CCV)
	*/
	public static void main(String argv[]) {
	
		int port = 3333;
		System.setProperty("apple.awt.UIElement", "true");
		java.awt.Toolkit.getDefaultToolkit();
 
		if (argv.length==1) {
			try { port = Integer.parseInt(argv[1]); }
			catch (Exception e) { System.out.println("usage: java TuioMouse [port]"); }
		}

 		TuioMouse mouse = new TuioMouse();
		
		final TuioClient client = new TuioClient(port);
		System.out.println("listening to TUIO messages at port "+port);
		client.addTuioListener(mouse);
		client.connect();
		
		if (SystemTray.isSupported()) {
		
			final PopupMenu popup = new PopupMenu();
			final TrayIcon trayIcon =
			new TrayIcon(Toolkit.getDefaultToolkit().getImage(mouse.getClass().getResource("tuio.gif")));
			trayIcon.setToolTip("Tuio Mouse");
			final SystemTray tray = SystemTray.getSystemTray();
			
			final CheckboxMenuItem pauseItem = new CheckboxMenuItem("Pause");
			final MenuItem exitItem = new MenuItem("Exit");
			
			popup.add(pauseItem);
			pauseItem.addItemListener( new ItemListener() { public void itemStateChanged(ItemEvent evt) {
				
				if (evt.getStateChange() == ItemEvent.SELECTED) {
					if (client.isConnected()) client.disconnect();
				} else {
					if (!client.isConnected()) client.connect();
				}
			} } );
			popup.add(exitItem);
			exitItem.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent evt) {
				client.disconnect();
				System.exit(0);
			} } );
			
			trayIcon.setPopupMenu(popup);
			
			try {
				tray.add(trayIcon);
			} catch (AWTException e) {
				System.out.println("SystemTray could not be added.");
			}
			
		} else System.out.println("SystemTray is not supported");

	}
}
