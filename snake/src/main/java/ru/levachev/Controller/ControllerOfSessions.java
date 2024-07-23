package ru.levachev.Controller;

import ru.levachev.Controller.sessions.*;
import ru.levachev.Models.GameConfig;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class ControllerOfSessions  {
    private final JFrame jFrame;
    private Session currentSession;
    private Map<SessionType, Session> sessions;
    private int wightPanel;
    private int heightPanel;
    private JTextArea activeGames;
    public ControllerOfSessions(JFrame jFrame, int wightPanel, int heightPanel){
        this.jFrame=jFrame;
        this.wightPanel=wightPanel;
        this.heightPanel=heightPanel;
        sessions = new HashMap<>();
    }

    public void run() throws IOException {
        sessions.put(SessionType.MENU, new MenuSession(wightPanel, heightPanel, this));
        sessions.put(SessionType.GAME, new MasterSession(wightPanel, heightPanel, this));
        sessions.put(SessionType.JOIN, new JoinSession(wightPanel, heightPanel, this));


        currentSession=sessions.get(SessionType.MENU);
        currentSession.setNewCanvas();
        addToFrame();
    }

    public void changeToMaster(ru.levachev.Models.GameState state, int id, GameConfig config, DatagramSocket socket, String name) throws SocketException {
        var tmp = sessions.get(SessionType.GAME);
        tmp = null;
        sessions.put(SessionType.GAME, new MasterSession(wightPanel, heightPanel, this));
        var tmp2 = sessions.get(SessionType.JOIN);
        tmp2 = null;
        sessions.put(SessionType.JOIN, new JoinSession(wightPanel, heightPanel, this));
        currentSession = sessions.get(SessionType.GAME);
        currentSession.switchTo(state, id, config, socket, name, null);
        //addToFrame();
    }

    public void changeToUser(int id, GameConfig config, DatagramSocket socket, String name, SocketAddress masterAddress) throws SocketException {
        var tmp = sessions.get(SessionType.JOIN);
        tmp = null;
        sessions.put(SessionType.JOIN, new JoinSession(wightPanel, heightPanel, this));
        var tmp2 = sessions.get(SessionType.GAME);
        tmp2 = null;
        sessions.put(SessionType.GAME, new MasterSession(wightPanel, heightPanel, this));
        currentSession = sessions.get(SessionType.JOIN);
        currentSession.switchTo(null, id, config, socket, name, (InetSocketAddress)masterAddress);
        //addToFrame();
    }


    public void switchSession(SessionType type){
        jFrame.remove(currentSession.getPanel());
        currentSession = sessions.get(type);

        try {
            currentSession.setNewCanvas();
        } catch (IOException ignored) {
        }
        addToFrame();
    }

    public void addToFrame(){
        jFrame.revalidate();
        jFrame.add(currentSession.getPanel());
        System.out.println("add new canvas");
        jFrame.revalidate();
        currentSession.getPanel().requestFocusInWindow();
    }
}
