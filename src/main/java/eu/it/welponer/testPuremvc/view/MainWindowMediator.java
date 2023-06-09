/*
 * Copyright (C) 2020  Michele Welponer, mwelponer@gmail.com (Fondazione Bruno Kessler)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/> and file GPL3.txt
 */

package eu.it.welponer.testPuremvc.view;

import java.awt.Dimension;
import java.util.Date;
import java.util.List;

import eu.it.welponer.testPuremvc.Main;
import eu.it.welponer.testPuremvc.model.messages.MessageProxy;
import eu.it.welponer.testPuremvc.model.messages.MessageVO;
import eu.it.welponer.testPuremvc.model.server.ServerPreferencesProxy;
import eu.it.welponer.testPuremvc.model.server.ServerProxy;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.puremvc.java.multicore.interfaces.IMediator;
import org.puremvc.java.multicore.interfaces.INotification;
import org.puremvc.java.multicore.patterns.mediator.Mediator;

import eu.it.welponer.testPuremvc.ApplicationFacade;

import javax.swing.*;


public class MainWindowMediator extends Mediator implements IMediator {

	public static final String NAME = "MainWindowMediator";
	private static MainWindow mainWindow = null;
	private MessageProxy messageProxy = null;
	
	public MainWindowMediator() {
		super(NAME, null);
		
		System.out.println("MainWindowMediator()");
		mainWindow = new MainWindow(this);
		setViewComponent(mainWindow);
	}

	/**
	 * Called by the View when the Mediator is registered
	 */
	@Override
	public void onRegister() {
		// TODO Auto-generated method stub
		System.out.println("  MainWindowMediator: onRegister()");
		messageProxy = (MessageProxy) getFacade().retrieveProxy(MessageProxy.NAME);
		super.onRegister();
	}

	/**
	 * List the INotification names this Mediator is interested in being notified of.
	 */
	@Override
	public String[] listNotificationInterests() {
		System.out.println("  MainWindowMediator: listNotificationInterests()");
		// TODO Auto-generated method stub
		return new String[] {
			ApplicationFacade.SHOW_MAIN_WINDOW,
			ApplicationFacade.SEND_MESSAGE,
            ApplicationFacade.MESSAGE_SENT,
			ApplicationFacade.UPDATE_CONSOLE,
			ApplicationFacade.UPDATE_STATUSBAR,
			ApplicationFacade.LOAD_MESSAGES,
			ApplicationFacade.SERVER_STARTED,
			ApplicationFacade.CHANGE_SERVER_PORT,
            ApplicationFacade.SHUTDOWN
		};
		
		//return super.listNotificationInterests();
	}
	
	@Override
	public void handleNotification(INotification notification) {
		System.out.println("  MainWindowMediator: handleNotification(): " +
				notification.getName().toUpperCase());

		// TODO Auto-generated method stub
		switch(notification.getName()) {
        	case ApplicationFacade.SHOW_MAIN_WINDOW:
				mainWindow.setPreferredSize(new Dimension(650, 500));
				mainWindow.pack();
				mainWindow.changeMethod(2); // initial is POST
				mainWindow.setVisible(true);
        		break;

			case ApplicationFacade.SERVER_STARTED:
				int port = (int)notification.getBody();
				mainWindow.changeUrl(" http://localhost:" + port);
				break;

			case ApplicationFacade.MESSAGE_SENT:
				mainWindow.writeToOutputConsole((String)notification.getBody());
				break;

			case ApplicationFacade.UPDATE_CONSOLE:
				//lock = true;
				//mainWindow.clearOutputConsole();
				mainWindow.writeToOutputConsole((String)notification.getBody());
				//lock = false;

				break;

			case ApplicationFacade.UPDATE_STATUSBAR:
				mainWindow.changeStatusbar((String)notification.getBody());
				break;

			case ApplicationFacade.LOAD_MESSAGES:
				//lock = true;
				mainWindow.clearOutputConsole();
				List messages = messageProxy.messages();

				// use only last 100 messages
				List<MessageVO> tail = messages.subList(Math.max(messages.size() - 100, 0), messages.size());
				for(MessageVO messageVO : tail){
					Date resultdate = new Date(messageVO.getTimestamp());
					//mainWindow.writeToOutputConsole(resultdate.toString());

					if(messageVO.getJsonObject().has("coords")){
						JSONArray coordsArray = messageVO.getJsonObject().getJSONArray("coords");
						String coords =  resultdate.toString() + " " + messageVO.getClientIP()
								+ " - received new coordinates: (" +
								coordsArray.getFloat(0) + ", " +
								coordsArray.getFloat(1) + ")";

						System.out.println("  .." + coords);
					}

					if(messageVO.getJsonObject().has("debugLevel")){
						int debugLevel = messageVO.getJsonObject().getInt("debugLevel");

						if(Main.debugLevel > 0)
							System.out.println("  ..setting debugLevel to: " + debugLevel);

						sendNotification(ApplicationFacade.SET_DEBUG_LEVEL, debugLevel);
					}

					if(messageVO.getJsonObject().has("coordX") && messageVO.getJsonObject().has("coordY")){
						String coords =  resultdate.toString() + " " + messageVO.getClientIP()
								+ " - received new coordinates: (" +
								messageVO.getJsonObject().getFloat("coordX") + ", " +
								messageVO.getJsonObject().getFloat("coordY") + ")";
						mainWindow.writeToOutputConsole(coords);
					}else{
						mainWindow.writeToOutputConsole(resultdate.toString() + " " + messageVO.getClientIP()
								+ " - json object received '"
								+ messageVO.getJsonObject().toString() + "'");
					}
				}
				break;

			case ApplicationFacade.SHUTDOWN:
				mainWindow.dispose();
				System.exit(0);
				break;
            
        	default:
        		break;
		}
            
    	//super.handleNotification(notification);
    }

    public void changeServerPort() {
		if(Main.debugLevel > 1)
			System.out.println("  MainWindowMediator: changeServerPort()");

		ServerPreferencesProxy preferencesProxy = (ServerPreferencesProxy) getFacade().retrieveProxy(ServerPreferencesProxy.NAME);

		String m = (String)JOptionPane.showInputDialog(mainWindow, "Server port",
				"Server Configuration", JOptionPane.QUESTION_MESSAGE, null,
				null, preferencesProxy.getServerPrefs().getServerPort());

		int port = -1;
		if (m == null) { // cancel is pressed
			//System.out.println("cancel button");
			return;
		} else if (m.isEmpty()) { // empty
			//mainWindow.getContentPane().setVisible(false);
			//mainWindow.dispose();
			changeServerPort();
		} else if (!m.matches("(0|[1-9]\\d*)")) { // not integer
			JOptionPane.showMessageDialog(null,
					"Enter a valid integer value.", "Alert", JOptionPane.WARNING_MESSAGE);

			changeServerPort();
		} else {
			port = Integer.parseInt(m);

			if (port > 49151 || port < 1024) { // port not in range 1024-49151
				JOptionPane.showMessageDialog(null,
						"Enter a valid user port (1024-49151).", "Alert", JOptionPane.WARNING_MESSAGE);
				changeServerPort();
			}

			sendNotification(ApplicationFacade.CHANGE_SERVER_PORT, port);
		}
	}
}