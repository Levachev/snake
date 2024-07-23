package ru.levachev.Controller;

import com.google.protobuf.InvalidProtocolBufferException;
import ru.levachev.Models.Announcement;
import ru.levachev.messages.GameAnnouncement;
import ru.levachev.messages.GameConfig;
import ru.levachev.messages.GameMessage;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Timer;

public class MulticastListener implements Runnable{
    private Map<SocketAddress, Announcement> members = new HashMap<>();
    private int port;
    private InetAddress groupAddr;
    private int timeOut=1;
    private MulticastSocket multicastSocket;
    private Timer timer;
    private TimerTask task;
    private JTextArea activeGames;
    public MulticastListener(JTextArea activeGames) throws IOException {
        this.groupAddr = InetAddress.getByName("239.192.0.4");
        this.port = 9192;
        this.activeGames=activeGames;

        multicastSocket = new MulticastSocket(port);
        multicastSocket.joinGroup(groupAddr);

        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                deleteInactiveMembers();
                printMembers();
            }
        };
    }

    @Override
    public void run() {
        byte[] message = new byte[2560];
        timer.schedule(task, 0, timeOut*1000L);
        while (true) {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(message, message.length);

                multicastSocket.receive(datagramPacket);
                //System.out.println("in receive "+datagramPacket.getLength());
                int len = datagramPacket.getLength();

                byte[] received = Arrays.copyOfRange(datagramPacket.getData(), 0, len);

                //System.out.println(received.length);

                GameMessage gameMessage = GameMessage.newBuilder().setMsgSeq(1).build();
                try {
                    gameMessage = GameMessage.parseFrom(received);
                } catch (InvalidProtocolBufferException e){
                    System.out.println("cannot parse message");
                }

                if (gameMessage.hasAnnouncement()) {
                    GameMessage.AnnouncementMsg announcement = gameMessage.getAnnouncement();
                    GameAnnouncement gameAnnouncement = announcement.getGames(0);
                    String gameName = gameAnnouncement.getGameName();
                    GameConfig gameConfig = gameAnnouncement.getConfig();

                    int height = gameConfig.getHeight();
                    int length = gameConfig.getWidth();
                    int foodStatic = gameConfig.getFoodStatic();
                    int stateDelayMs = gameConfig.getStateDelayMs();

                    members.put(datagramPacket.getSocketAddress(),
                            new Announcement(LocalDateTime.now(),
                                    new ru.levachev.Models.GameConfig(length, height, foodStatic, stateDelayMs),
                                    gameName));
                }
            } catch (IOException ignored) {
                throw new RuntimeException();
            }
        }
    }

    private void deleteInactiveMembers(){
        members.entrySet().removeIf(item -> ChronoUnit.SECONDS.between(item.getValue().last, LocalDateTime.now()) > 2L *timeOut);
    }

    private void printMembers(){
        activeGames.setText("");
        activeGames.append("members:\n");
        for(var item : members.entrySet()){
            activeGames.append(item.getKey()+" "+
                    item.getValue().gameName+" "+
                    item.getValue().gameConfig.length+" "+
                    item.getValue().gameConfig.height+" "+
                    item.getValue().gameConfig.foodStatic+" "+
                    item.getValue().gameConfig.stateDelayMs+" "+"\n");
        }
    }
}
