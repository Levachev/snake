package ru.levachev.Controller.sessions;

import ru.levachev.Controller.ControllerOfSessions;
import ru.levachev.Controller.MulticastListener;
import ru.levachev.Models.GameConfig;
import ru.levachev.Models.GameState;
import ru.levachev.View.Menu;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class MenuSession implements Session{
    private Menu menu;
    private int wightPanel;
    private int heightPanel;
    private int wightOfWorld=520;
    private int heightOfWorld=360;
    private ControllerOfSessions controllerOfSessions;
    private MulticastListener multicastListener;
    private JTextArea activeGames;
    public MenuSession(int wightPanel, int heightPanel, ControllerOfSessions controllerOfSessions) throws IOException {
        this.wightPanel=wightPanel;
        this.heightPanel=heightPanel;
        this.controllerOfSessions=controllerOfSessions;
        activeGames = new JTextArea();
        setNewCanvas();
    }
    @Override
    public JPanel getPanel() {
        return menu;
    }


    @Override
    public void updateSession() {
        menu.repaint();
        menu.setBackground(Color.BLUE);
    }

    public void setNewCanvas() throws IOException {
        double myPixelHeight= (double) heightPanel /heightOfWorld;
        double myPixelWight= (double) wightPanel /wightOfWorld;

        menu = new Menu(myPixelWight, myPixelHeight);
        menu.setLayout(new BorderLayout());

        JPanel headerPanel = getHeaderPanel();

        menu.add(headerPanel, BorderLayout.NORTH);
        menu.add(activeGames, BorderLayout.CENTER);

        multicastListener = new MulticastListener(activeGames);
        new Thread(multicastListener).start();
    }

    @Override
    public void switchTo(GameState state, int id, GameConfig config, DatagramSocket socket, String name, InetSocketAddress masterAddress) throws SocketException {
    }

    private JPanel getHeaderPanel() {
        JPanel headerPanel = new JPanel();

        JButton createButton=new JButton("create game");
        JButton joinButton=new JButton("join game");
        JButton exitButton=new JButton("exit");

        createButton.addActionListener(e -> controllerOfSessions.switchSession(SessionType.GAME));

        joinButton.addActionListener(e -> controllerOfSessions.switchSession(SessionType.JOIN));

        exitButton.addActionListener(e -> System.exit(0));

        headerPanel.add(createButton);
        headerPanel.add(joinButton);
        headerPanel.add(exitButton);
        return headerPanel;
    }


}
