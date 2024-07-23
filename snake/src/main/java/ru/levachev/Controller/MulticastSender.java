package ru.levachev.Controller;

import ru.levachev.Models.GameProcess;
import ru.levachev.Models.Player;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class MulticastSender implements ActionListener {
    private final Timer timer;
    private final UdpClientSender sender;
    private final GameProcess gameProcess;
    public final ArrayList<Player> players;

    public MulticastSender(UdpClientSender sender, GameProcess gameProcess, ArrayList<Player> players){
        this.sender=sender;
        this.gameProcess=gameProcess;
        this.players=players;
        timer = new Timer(1000, this);
    }

    public void startAnnouncement(){
        System.out.println("start announcment");
        timer.start();
    }

    public void stopAnnouncement(){
        timer.stop();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        sender.sendAnnouncement(
                new InetSocketAddress("239.192.0.4", 9192),
                gameProcess.gameConfig,
                players,
                gameProcess.gameName,
                0,
                0);
        System.out.println("send announcment");
    }
}
