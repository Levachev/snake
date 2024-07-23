package ru.levachev.Controller.sessions;

import ru.levachev.Models.GameConfig;
import ru.levachev.Models.GameState;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public interface Session {
    JPanel getPanel();
    void updateSession();
    void setNewCanvas() throws IOException;
    void switchTo(GameState state, int id, GameConfig config, DatagramSocket socket, String name, InetSocketAddress masterAddress) throws SocketException;
}
