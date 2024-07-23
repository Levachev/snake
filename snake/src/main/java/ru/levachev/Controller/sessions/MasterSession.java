package ru.levachev.Controller.sessions;

import ru.levachev.Controller.ControllerOfSessions;
import ru.levachev.Controller.MulticastSender;
import ru.levachev.Controller.UdpServerListener;
import ru.levachev.Controller.UdpClientSender;
import ru.levachev.Models.*;
import ru.levachev.View.PlayCanvas;
import ru.levachev.messages.GamePlayer;
import ru.levachev.messages.GameState;
import ru.levachev.messages.NodeRole;
import ru.levachev.messages.PlayerType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.TimerTask;

import static ru.levachev.Models.Snake.cast2;

public class MasterSession implements Session, ActionListener {
    private PlayCanvas canvas;
    private final int wightPanel;
    private final int heightPanel;
    public GameProcess gameProcess;
    private Timer timer;
    private java.util.Timer timerInactive = new java.util.Timer();
    private TimerTask task;
    private final ControllerOfSessions controllerOfSessions;
    public ArrayList<Player> players;
    public UdpClientSender sender;
    private UdpServerListener listener;
    public GameConfig gameConfig;
    private MulticastSender multicastSender;
    private DatagramSocket socket;
    public int id;
    public String name;
    private ru.levachev.Models.GameState state;
    private Thread listenerThread;

    public MasterSession(int wightPanel, int heightPanel, ControllerOfSessions controllerOfSessions) {
        this.wightPanel=wightPanel;
        this.heightPanel=heightPanel;
        this.controllerOfSessions=controllerOfSessions;
    }

    private void checkInactive(){
        //players.removeIf(item -> ChronoUnit.MILLIS.between(item.last, LocalDateTime.now()) > gameConfig.stateDelayMs*0.8);
        ArrayList<Player> delete = new ArrayList<>();
        for(Player player:players){
            var tmp=ChronoUnit.MILLIS.between(player.last, LocalDateTime.now());
            if(player.getId()!=id && tmp > gameConfig.stateDelayMs*10){
                if(player.getRole()==NodeRole.DEPUTY){
                    System.out.println(player.last);
                    System.out.println(tmp+" deleteeeeeeeeeeeeee");
                    setNewDeputy();
                    delete.add(player);
                } else{
                    System.out.println(player.last);
                    System.out.println(tmp+" deleteeeeeeeeeeeeee222");
                    delete.add(player);
                }
            }
        }
        for(Player player:delete){
            sender.messageChecker.clearLeaveManMsg(player.getAddress(), player.getPort());
            //gameProcess.snakes.removeIf(snake->snake.getId()==player.getId());
            players.removeIf(tmp->tmp.getId()==player.getId());
        }
    }

    private int getScore(int id){
        for (Player player : players){
            if(id == player.getId()){
                return player.getScore();
            }
        }
        return 0;
    }

    private Player getDeputy(){
        for(Player player:players){
            if(player.getRole() == NodeRole.DEPUTY){
                return player;
            }
        }
        return null;
    }

    private void setMeViewer(){
        for(Player player:players){
            if(player.getId()==id){
                player.setRole(NodeRole.VIEWER);
                return;
            }
        }
    }

    private void die(int id) throws UnknownHostException, SocketException {
        if(id==this.id){
            System.out.println("in if die");
            Player tmp = getDeputy();
            if(tmp == null){
                System.exit(0);
            }
            System.out.println("nick deputy - "+tmp.getName());
            //gameProcess.snakes.removeIf(snakes->snakes.getId()==id);
            setNewDeputy();
            setMeViewer();
            setNewMaster(tmp.getId());
            
            timer.stop();
            timerInactive.cancel();
            listenerThread.interrupt();
            multicastSender.stopAnnouncement();
            sender.messageChecker.deleteAll();
            
            controllerOfSessions.changeToUser(
                    id,
                    gameConfig,
                    socket,
                    name,
                    new InetSocketAddress(tmp.getAddress(),tmp.getPort())
            );
        } else{
            System.out.println("de in else "+id);
            Player tmp = getPlayer(id);
            if(tmp == null){
                return;
            }
            if(tmp.getRole()==NodeRole.DEPUTY){
                setNewDeputy();
            }
            //gameProcess.snakes.removeIf(snakes->snakes.getId()==id);
            setRole(id, NodeRole.VIEWER);
            //players.remove(tmp);
            sender.sendRoleChangeDieDefault(
                    new InetSocketAddress(tmp.getAddress(), tmp.getPort()),
                    this.id,
                    id
            );
            System.out.println("send die else");
        }
    }

    private void setRole(int id, NodeRole role){
        for(Player player:players){
            if(player.getId()==id){
                player.setRole(role);
                return;
            }
        }
    }

    private boolean isDeputy(){
        for(Player player : players){
            if(player.getRole()==NodeRole.DEPUTY){
                return true;
            }
        }
        return false;
    }

    private void setNewDeputy(){
        for(Player player : players){
            if(player.getRole()==NodeRole.NORMAL){
                player.setRole(NodeRole.DEPUTY);
                long seq=sender.sendRoleChangeNewDeputy(
                        new InetSocketAddress(player.getAddress(), player.getPort()),
                        0,
                        0
                );
                while(sender.messageChecker.has(seq)){
                    Thread.onSpinWait();
                }
                return;
            }
        }
    }

    private void setNewMaster(int id) throws UnknownHostException{
        for(Player player : players){
            if(player.getId()==id){
                player.setRole(NodeRole.MASTER);
                sendState();
                long seq=sender.sendRoleChangeDieMaster(
                    new InetSocketAddress(player.getAddress(), player.getPort()),
                    this.id,
                    player.getId()
                );
                while(sender.messageChecker.has(seq)){
                    Thread.onSpinWait();
                }
                return;
            }
        }
    }

    public void playerExit(int id){
        players.removeIf(player -> id == player.getId());
    }

    @Override
    public void switchTo(ru.levachev.Models.GameState state, int id, GameConfig config, DatagramSocket socket, String name, InetSocketAddress masterAddress) throws SocketException {
        System.out.println("switch to in master"+id);
        this.id=id;

        this.socket=socket;
        sender = new UdpClientSender(socket, config.stateDelayMs);
        listener = new UdpServerListener(this, socket);

        timer = new Timer(config.stateDelayMs, this);

        players = new ArrayList<>();

        for (GamePlayer player : state.players){
            System.out.println("id - "+player.getId()+" name - "+player.getName()+" port - "+player.getPort()+" role - "+player.getRole());
            if(player.getId()==this.id){
                players.add(new Player(
                        player.getIpAddress(),
                        player.getPort(),
                        player.getName(),
                        player.getId(),
                        player.getScore(),
                        player.getType(),
                        NodeRole.MASTER
                ));
            }
            else if(player.getRole()==NodeRole.MASTER && player.getId()!=this.id){
                players.add(new Player(
                        player.getIpAddress(),
                        player.getPort(),
                        player.getName(),
                        player.getId(),
                        player.getScore(),
                        player.getType(),
                        NodeRole.VIEWER
                ));
            } else{
                players.add(new Player(
                        player.getIpAddress(),
                        player.getPort(),
                        player.getName(),
                        player.getId(),
                        player.getScore(),
                        player.getType(),
                        player.getRole()
                ));
            }
        }

        if(isDeputy()){
            setNewDeputy();
        }

        gameConfig = config;
        gameProcess = new GameProcess();
        gameProcess.gameConfig = config;
        gameProcess.food = state.food;
        for(GameState.Snake snake : state.snakes){
            System.out.println("id snakes in switch "+snake.getPlayerId());
            gameProcess.snakes.add(cast2(snake, gameConfig, getScore(snake.getPlayerId())));
        }
        gameProcess.gameName = name;

        multicastSender = new MulticastSender(sender, gameProcess, players);

        double myPixelHeight = (double) heightPanel /gameConfig.height;
        double myPixelWight = (double) wightPanel /(gameConfig.length*2);

        this.state = new ru.levachev.Models.GameState(gameProcess, players);

        canvas = new PlayCanvas(this.state, wightPanel, heightPanel, myPixelWight, myPixelHeight, id, gameConfig);

        canvas.requestFocusInWindow();

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyTyped(e);
                if (e.getKeyCode() == KeyEvent.VK_W) {
                    gameProcess.snakes.get(0).setNewDirection(Direction.UP);
                }
                if (e.getKeyCode() == KeyEvent.VK_S) {
                    gameProcess.snakes.get(0).setNewDirection(Direction.DOWN);
                }
                if (e.getKeyCode() == KeyEvent.VK_A) {
                    gameProcess.snakes.get(0).setNewDirection(Direction.LEFT);
                }
                if (e.getKeyCode() == KeyEvent.VK_D) {
                    gameProcess.snakes.get(0).setNewDirection(Direction.RIGHT);
                }
            }
        });

        while(canvas==null){
            System.out.println("null canvas");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
            }
        }

        controllerOfSessions.addToFrame();

        //System.out.println("timer start");
        listenerThread = new Thread(listener);
        listenerThread.start();
        for(Player player : players){
            if(player.getRole()==NodeRole.MASTER){
                continue;
            }
            sender.sendNewMaster(
                    new InetSocketAddress(player.getAddress(), player.getPort()),
                    this.id,
                    player.getId()
            );
        }
        timer.start();
        multicastSender.startAnnouncement();

        task = new TimerTask() {
            @Override
            public void run() {
                checkInactive();
            }
        };
        timerInactive.schedule(task, 5000, (long) (gameConfig.stateDelayMs*0.5));
    }

    public void sendAnnouncement(SocketAddress address){
        sender.sendAnnouncement(
                address,
                gameProcess.gameConfig,
                players,
                gameProcess.gameName,
                id,
                id
        );
    }



    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("timer tick "+players.size());
        updateSession();
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try{
                            for(Player player:players){
                                if(player.getId() == id){
                                    continue;
                                }
                                sender.sendPing(new InetSocketAddress(
                                    InetAddress.getByName(player.getAddress()),player.getPort()), 
                                    id,
                                    player.getId()
                                );
                            }
                            Thread.currentThread().interrupt();
                        } catch(IOException ignored){
                        }
                    }
                },
                (int)(gameConfig.stateDelayMs*0.5)
        );
    }

    @Override
    public JPanel getPanel() {
        return canvas;
    }

    private void addScorePlayer(int id, int score){
        for(Player player:players){
            if(player.getId()==id){
                player.setScore(score);
                return;
            }
        }
    }
    private void appendScore(){
        for (Snake snake:gameProcess.snakes){
            addScorePlayer(snake.getId(),snake.getScore());
        }
    }
    private void updateStateOfPlayer() throws UnknownHostException, SocketException {
        //System.out.println("in update "+gameProcess.snakes.size());
        gameProcess.moveAll();
        gameProcess.addFood();
        appendScore();
        boolean isMasterDead=false;
        for(Integer id:gameProcess.deadUsers){
            System.out.println(id);
            if(id==this.id){
                isMasterDead=true;
                continue;
            }
            die(id);
        }
        if(isMasterDead){
            die(this.id);
        }
        gameProcess.deadUsers.clear();
    }

    private void updateView(){
        canvas.repaint();
    }

    private void printSnake(){
        Snake snake = gameProcess.snakes.get(0);
        for (Coordinate coordinate : snake.body){
            System.out.println();
            System.out.println(coordinate.getX());
            System.out.println(coordinate.getY());
            System.out.println();
        }
        System.out.println("in session");
    }

    public void updateState(){
        state.update(gameProcess, players);
    }

    @Override
    public void updateSession(){
        //printSnake();
        try {
            updateStateOfPlayer();
        } catch (UnknownHostException | SocketException ignored) {
        }
        //printSnake();
        try {
            sendState();
        } catch (UnknownHostException ignored) {
        }

        updateState();
        updateView();
    }

    public void addPlayer(Player player, Snake snake){
        players.add(player);
        gameProcess.addSnake(snake);
    }

    @Override
    public void setNewCanvas() throws SocketException {
        String nickname = JOptionPane.showInputDialog("enter nickname");
        int length = Integer.parseInt(JOptionPane.showInputDialog("enter length", "50"));
        int height = Integer.parseInt(JOptionPane.showInputDialog("enter height", "50"));
        int foodStatic = Integer.parseInt(JOptionPane.showInputDialog("enter food static", "30"));
        int delay = Integer.parseInt(JOptionPane.showInputDialog("enter delay", "400"));
        id = 0;

        socket = new DatagramSocket();
        sender = new UdpClientSender(socket, delay);
        listener = new UdpServerListener(this, socket);

        timer = new Timer(delay, this);

        players = new ArrayList<>();

        gameConfig = new GameConfig(length, height, foodStatic, delay);
        gameProcess = new GameProcess(gameConfig, nickname);

        multicastSender = new MulticastSender(sender, gameProcess, players);

        double myPixelHeight = (double) heightPanel /gameConfig.height;
        double myPixelWight = (double) wightPanel /(gameConfig.length*2);

        Snake snake = new Snake(gameConfig, gameProcess.getEmptyArea(), GameState.Snake.SnakeState.ALIVE, 0);

        addPlayer(
                new Player(
                        null,
                        socket.getPort(),
                        nickname,
                        0,
                        0,
                        PlayerType.HUMAN,
                        NodeRole.MASTER
                ),
                snake
        );

        state = new ru.levachev.Models.GameState(gameProcess, players);

        canvas = new PlayCanvas(state, wightPanel, heightPanel, myPixelWight, myPixelHeight, id, gameConfig);

        canvas.requestFocusInWindow();

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyTyped(e);
                if (e.getKeyCode() == KeyEvent.VK_W) {
                    gameProcess.snakes.get(0).setNewDirection(Direction.UP);
                }
                if (e.getKeyCode() == KeyEvent.VK_S) {
                    gameProcess.snakes.get(0).setNewDirection(Direction.DOWN);
                }
                if (e.getKeyCode() == KeyEvent.VK_A) {
                    gameProcess.snakes.get(0).setNewDirection(Direction.LEFT);
                }
                if (e.getKeyCode() == KeyEvent.VK_D) {
                    gameProcess.snakes.get(0).setNewDirection(Direction.RIGHT);
                }
            }
        });

        timer.start();
        listenerThread = new Thread(listener);
        listenerThread.start();
        multicastSender.startAnnouncement();

        task = new TimerTask() {
            @Override
            public void run() {
                checkInactive();
            }
        };
        timerInactive.schedule(task, 3000, (long) (gameConfig.stateDelayMs*0.5));
    }

    // public void allUnMove(){
    //     for(Player player : players){
    //         player.isMove = false;
    //     }
    // }

    public void move(int id){
        for(Player player : players){
            if(player.getId() == id) {
                player.last = LocalDateTime.now();
                System.out.println("update "+id+" "+LocalDateTime.now());
            }
        }
    }

    // private void checkActivePlayers(){
    //     int len1 = players.size();
    //     players.removeIf(player -> (!player.isMove && player.getId()!=id));
    //     System.out.println(players.size()-len1);
    // }

    private void sendState() throws UnknownHostException {
        for (Player player : players){
            System.out.println("name "+player.getName()+" id "+player.getId());
            if(player.getId() == id){
                continue;
            }
            System.out.println("name "+player.getName());
            sender.sendState(
                    new InetSocketAddress(
                            InetAddress.getByName(player.getAddress()),
                            player.getPort()
                    ),
                    gameProcess,
                    players,
                    0,
                    player.getId()
            );
        }
    }
    public NodeRole getNewNodeRole(){
        for(Player player : players){
            if(player.getRole() == NodeRole.DEPUTY){
                return NodeRole.NORMAL;
            }
        }
        return NodeRole.DEPUTY;
    }

    public int getNewID() {
        boolean isNew = true;
        int id = 0;
        while (true){
            for(Player player : players){
                if(id==player.getId()){
                    isNew = false;
                    break;
                }
            }
            if(!isNew){
                isNew = true;
                id++;
            } else{
                return id;
            }
        }
    }

    public boolean isUniqueNickname(String nickname){
        for(Player player : players){
            if(player.getName().equals(nickname)){
                return false;
            }
        }
        return true;
    }

    public Player getPlayer(int id){
        for(Player player : players){
            if(id == player.getId()){
                return player;
            }
        }
        return null;
    }
}
