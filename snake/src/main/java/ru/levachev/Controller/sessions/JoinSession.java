package ru.levachev.Controller.sessions;

import com.google.protobuf.InvalidProtocolBufferException;
import ru.levachev.Controller.ControllerOfSessions;
import ru.levachev.Controller.UdpClientListener;
import ru.levachev.Controller.UdpClientSender;
import ru.levachev.Models.*;
import ru.levachev.Models.Direction;
import ru.levachev.Models.GameConfig;
import ru.levachev.Models.GameState;
import ru.levachev.View.PlayCanvas;
import ru.levachev.messages.*;
import ru.levachev.messages.GameState.Snake;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimerTask;

public class JoinSession implements Session {
    private PlayCanvas canvas;
    private final int wightPanel;
    private final int heightPanel;
    public UdpClientSender sender;
    private UdpClientListener listener;
    public GameConfig gameConfig;
    public Player master;
    private java.util.Timer timerInactive = new java.util.Timer();
    private TimerTask task;
    private final ControllerOfSessions controllerOfSessions;
    public SocketAddress masterAddress;
    private volatile GameState state;
    private DatagramSocket socket;
    public int id;
    private String nickname;
    private Thread listenerThread;
    public NodeRole myRole;
    public LocalDateTime lastMastertime = LocalDateTime.now();
    public long current=-1;
    public boolean isJoin=false;

    public JoinSession(int wightPanel, int heightPanel, ControllerOfSessions controllerOfSessions) {
        this.wightPanel=wightPanel;
        this.heightPanel=heightPanel;
        this.controllerOfSessions=controllerOfSessions;
    }

    private void checkInactive(){
        var tmp = ChronoUnit.MILLIS.between(lastMastertime, LocalDateTime.now());
        if(tmp > gameConfig.stateDelayMs*0.8){
            System.out.println("time is more "+myRole);
            if(myRole==NodeRole.NORMAL){
                lastMastertime = LocalDateTime.now();
            } else if(myRole==NodeRole.DEPUTY){
                System.out.println("udalyaaaaaauuuuu "+tmp);
                //throw new RuntimeException();
                deleteMaster();
                setMeMaster();
                needSwitch();
            }
        }
    }

    private void setMeMaster(){
        for(GamePlayer player:state.players){
            if(player.getId()==this.id){
                // InetSocketAddress mAddr = (InetSocketAddress)masterAddress;
                GamePlayer tmp = GamePlayer.newBuilder()
                .setName(player.getName())
                .setId(player.getId())
                .setIpAddress(player.getIpAddress())
                .setPort(player.getPort())
                .setRole(NodeRole.MASTER)
                .setType(player.getType())
                .setScore(player.getScore())
                .build();
                state.players.remove(player);
                state.players.add(tmp);
                return;
            }
        }
    }

    private void removeSnake(int id){
        state.snakes.removeIf(snake->snake.getPlayerId()==id);
    }

    private void deleteMaster(){
        // for(GamePlayer player:state.players){
        //     if(player.getRole()==NodeRole.MASTER){
        //         removeSnake(player.getId());
        //         break;
        //     }
        // }
        state.players.removeIf(tmp->tmp.getRole()==NodeRole.MASTER);
    }

    @Override
    public JPanel getPanel() {
        return canvas;
    }

    public void updateState(GameState newState){
        state = newState;
        //sender.sendPing(masterAddress, id, 0);
        
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        //System.out.println("shlu");
                        sender.sendPing(masterAddress, id, 0);
                        Thread.currentThread().interrupt();
                    }
                },
                (int)(gameConfig.stateDelayMs*0.2)
        );

        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("shlu");
                        sender.sendPing(masterAddress, id, 0);
                        Thread.currentThread().interrupt();
                    }
                },
                (int)(gameConfig.stateDelayMs*0.6)
        );

        if(canvas!=null) {
            canvas.updateState(newState);
        }
    }
    @Override
    public void updateSession(){
        if(canvas!=null) {
            canvas.repaint();
        }
    }

    public void setMaster(String ip, int port, String name, int id, int score, PlayerType type){
        master = new Player(
                ip,
                port,
                name,
                id,
                score,
                type,
                NodeRole.MASTER
        );
    }

    public void setDeputy(int id){
        myRole=NodeRole.DEPUTY;
        System.out.println(myRole+" in setdeputy");
    }

    public GamePlayer getPlayer(int id){
        if(state==null){
            return null;
        }
        for(GamePlayer player : state.players){
            if(id == player.getId()){
                return player;
            }
        }
        return null;
    }

    private void sendDiscover(SocketAddress address){
        GameMessage.DiscoverMsg discoverMsg = GameMessage.DiscoverMsg.newBuilder().build();
        GameMessage message = GameMessage.newBuilder().setDiscover(discoverMsg)
                .setMsgSeq(0)
                .setSenderId(0)
                .setReceiverId(0)
                .build();
        try {
            byte[] buf = message.toByteArray();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address);
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setNewCanvas() throws SocketException {
        isJoin=false;
        String ip = JOptionPane.showInputDialog("enter ip", "192.168.");
        int port = Integer.parseInt(JOptionPane.showInputDialog("enter port"));
        masterAddress = new InetSocketAddress(ip, port);
        nickname = JOptionPane.showInputDialog("enter nickname");
        String role = JOptionPane.showInputDialog("enter Role(NORMAL or VIEWER)", "NORMAL");

        NodeRole nodeRole = NodeRole.NORMAL;
        if(role.equals("NORMAL")){
            nodeRole = NodeRole.NORMAL;
        } else if(role.equals("VIEWER")){
            nodeRole = NodeRole.VIEWER;
        } else{
            System.out.println("invalid role");
            System.exit(0);
        }
        myRole=nodeRole;

        socket = new DatagramSocket();
        listener = new UdpClientListener(this, socket);
        sender = new UdpClientSender(socket, 1000);

        listenerThread = new Thread(listener);
        listenerThread.start();

        while(gameConfig==null){
            sender.sendDiscover(masterAddress, 0, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        sender = new UdpClientSender(socket, gameConfig.stateDelayMs);

        //listenerThread.interrupt();

        sender.sendJoinMsg( masterAddress,
                PlayerType.HUMAN,
                nickname,
                "",
                nodeRole,
                -1,
                -1
        );
        System.out.println("after send "+isJoin);

        while(!isJoin){
            Thread.onSpinWait();
            // System.out.println("jdu join");
            // try {
            //     Thread.sleep(1000);
            // } catch (InterruptedException e) {
            // }
        }

        System.out.println("after send");

        listenerThread.interrupt();
        listenerThread = new Thread(listener);
        listenerThread.start();

        double myPixelHeight = (double) heightPanel /gameConfig.height;
        double myPixelWight = (double) wightPanel /(gameConfig.length*2);


        while (state == null) {
            Thread.onSpinWait();
        }

        for(GamePlayer player:state.players){
            if(player.getId()==this.id){
                myRole=player.getRole();
                System.out.println(myRole+" in new state join");
                break;
            }
        }

        for(GamePlayer player:state.players){
            if(player.getRole()==NodeRole.DEPUTY){
                setDeputy(player.getId());
                break;
            }
        }

        System.out.println("wait state");

        for(GamePlayer player : state.players){
            if(player.getRole() == NodeRole.MASTER){
                setMaster(
                        ip,
                        port,
                        player.getName(),
                        player.getId(),
                        player.getScore(),
                        player.getType()
                );
                break;
            }
        }

        canvas = new PlayCanvas(state, wightPanel, heightPanel, myPixelWight, myPixelHeight, id, gameConfig);

        canvas.requestFocusInWindow();

        if(nodeRole!=NodeRole.VIEWER){
            canvas.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    super.keyTyped(e);
                    if (e.getKeyCode() == KeyEvent.VK_W) {
                        sender.sendSteer(
                                new InetSocketAddress(master.getAddress(), master.getPort()),
                                Direction.UP,
                                id,
                                0);
                                System.out.println("send w");
                    }
                    if (e.getKeyCode() == KeyEvent.VK_S) {
                        sender.sendSteer(
                                new InetSocketAddress(master.getAddress(), master.getPort()),
                                Direction.DOWN,
                                id,
                                0);
                                System.out.println("send s");
                    }
                    if (e.getKeyCode() == KeyEvent.VK_A) {
                        sender.sendSteer(
                                new InetSocketAddress(master.getAddress(), master.getPort()),
                                Direction.LEFT,
                                id,
                                0);
                                System.out.println("send a");
                    }
                    if (e.getKeyCode() == KeyEvent.VK_D) {
                        sender.sendSteer(
                                new InetSocketAddress(master.getAddress(), master.getPort()),
                                Direction.RIGHT,
                                id,
                                0);
                                System.out.println("send d");
                    }
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        long seq = sender.sendRoleExit(
                                new InetSocketAddress(master.getAddress(), master.getPort()),
                                0,
                                0);
                        while(sender.messageChecker.has(seq)){
                            Thread.onSpinWait();
                        }
                        System.exit(0);
                    }
                }
            });

            controllerOfSessions.addToFrame();

            task = new TimerTask() {
                @Override
                public void run() {
                    checkInactive();
                }
            };
            timerInactive.schedule(task, 3000, (long) (gameConfig.stateDelayMs*0.5));
        }
    }

    private void updateMesterAddress(){
        for(GamePlayer player:state.players){
            if(player.getPort() == 0){
                InetSocketAddress mAddr = (InetSocketAddress)masterAddress;
                GamePlayer tmp = GamePlayer.newBuilder()
                .setName(player.getName())
                .setId(player.getId())
                .setIpAddress(mAddr.getAddress().getHostAddress())
                .setPort(mAddr.getPort())
                .setRole(player.getRole())
                .setType(player.getType())
                .setScore(player.getScore())
                .build();
                System.out.println("update master");
                state.players.remove(player);
                state.players.add(tmp);
                return;
            }
        }
    }

    private void printStatePlayersAddress(){
         for(GamePlayer player:state.players){
            System.out.println("!!!! id - "+player.getId()+" name - "+player.getName()+" ip "+player.getIpAddress()+" port - "+player.getPort());
        }
    }

    public void needSwitch(){
        updateMesterAddress();
        printStatePlayersAddress();
        try {
            listenerThread.interrupt();
            timerInactive.cancel();
            sender.messageChecker.deleteAll();
            System.out.println("aftre listener interrupt");
            controllerOfSessions.changeToMaster(
                    state,
                    id,
                    gameConfig,
                    socket,
                    nickname
            );
        } catch (SocketException ignored) {
        }
    }

    @Override
    public void switchTo(GameState state, int id, GameConfig tmpConfig, DatagramSocket socket, String name, InetSocketAddress masterAddress) throws SocketException {
        System.out.println("switch to in join");
        this.masterAddress=masterAddress;
        this.socket=socket;
        this.gameConfig=tmpConfig;
        this.state=null;
        myRole=NodeRole.VIEWER;

        listener = new UdpClientListener(this, socket);
        sender = new UdpClientSender(socket, gameConfig.stateDelayMs);
        listenerThread = new Thread(listener);
        listenerThread.start();
        
        double myPixelHeight = (double) heightPanel /gameConfig.height;
        double myPixelWight = (double) wightPanel /(gameConfig.length*2);

        while (this.state == null) {
            Thread.onSpinWait();
        }

        System.out.println("wait state");

        for(GamePlayer player : this.state.players){
            if(player.getRole() == NodeRole.MASTER){
                setMaster(
                        masterAddress.getAddress().getHostAddress(),
                        masterAddress.getPort(),
                        player.getName(),
                        player.getId(),
                        player.getScore(),
                        player.getType()
                );
                break;
            }
        }

        canvas = new PlayCanvas(this.state, wightPanel, heightPanel, myPixelWight, myPixelHeight, id, gameConfig);

        canvas.requestFocusInWindow();

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyTyped(e);
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    long seq = sender.sendRoleExit(
                            new InetSocketAddress(master.getAddress(), master.getPort()),
                            0,
                            0);
                    while(sender.messageChecker.has(seq)){
                        Thread.onSpinWait();
                    }
                    System.exit(0);
                }
            }
        });
        controllerOfSessions.addToFrame();
    }

    private GameMessage getAnswer(){
        try{
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            int len = packet.getLength();

            byte[] received = Arrays.copyOfRange(packet.getData(), 0, len);

            GameMessage gameMessage = GameMessage.newBuilder().setMsgSeq(1).build();
            try {
                gameMessage = GameMessage.parseFrom(received);
            } catch (InvalidProtocolBufferException e){
                System.out.println("cannot parse message in answer");
            }

            return gameMessage;
        } catch (IOException e) {
            return null;
        }
    }
}
