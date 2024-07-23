package ru.levachev.Controller;

import com.google.protobuf.InvalidProtocolBufferException;
import ru.levachev.Controller.sessions.MasterSession;
import ru.levachev.Models.Coordinate;
import ru.levachev.Models.Direction;
import ru.levachev.Models.Player;
import ru.levachev.Models.Snake;
import ru.levachev.messages.GameMessage;
import ru.levachev.messages.GamePlayer;
import ru.levachev.messages.GameState;
import ru.levachev.messages.NodeRole;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;


public class UdpServerListener implements Runnable{
    private final DatagramSocket socket;
    private final MasterSession session;

    public UdpServerListener(MasterSession session, DatagramSocket socket) throws SocketException {
        this.session=session;
        this.socket=socket;
    }

    @Override
    public void run() {
        while(true){
            try {
                byte[] buf = new byte[2560];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                int len = packet.getLength();

                byte[] received = Arrays.copyOfRange(packet.getData(), 0, len);

                GameMessage gameMessage = GameMessage.newBuilder().setMsgSeq(1).build();
                try {
                    gameMessage = GameMessage.parseFrom(received);
                } catch (InvalidProtocolBufferException e){
                    System.out.println("cannot parse message");
                }

                handleMessage(gameMessage, address.getHostAddress(), port, gameMessage.getMsgSeq());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Player getPlayer(String address, int port){
        for (Player player : session.players){
            if(player.getAddress()!=null && player.getAddress().equals(address) && player.getPort()==port){
                return player;
            }
        }
        return null;
    }

    private void handleMessage(GameMessage message, String address, int port, long msgSeq){
        Player tmp = getPlayer(address, port);
        if(tmp!=null){
            System.out.println("in move "+msgSeq);
            session.move(tmp.getId());
        }
        
        if(message.hasAck()){
            session.sender.messageChecker.handleAck(message.getMsgSeq());
        } else if(message.hasJoin()){
            handleJoin(message.getJoin(), address, port, msgSeq);
        } else if(message.hasPing()){
            Player player = getPlayer(address, port);
            if(player == null) {
                handlePing(message.getPing(),0, address, port, msgSeq);
            } else {
                handlePing(message.getPing(), player.getId(), address, port, msgSeq);
            }
        } else if(message.hasDiscover()){
            handleDiscover(address, port);
        } else if(message.hasSteer()){
            handleSteer(message.getSteer(), getPlayer(address, port).getId(), address, port, msgSeq);
        } else if(message.hasRoleChange()){
            handleRoleChange(message.getRoleChange(), getPlayer(address, port).getId(), address, port, msgSeq);
        }
    }

    private void handleJoin(GameMessage.JoinMsg msg, String address, int port, long msgSeq){
        System.out.println("get join");
        Coordinate tmp = session.gameProcess.getEmptyArea();
        if(tmp!=null && session.isUniqueNickname(msg.getPlayerName())){
            int id = session.getNewID();
            System.out.println("new id "+id);
            Snake snake = new Snake(session.gameConfig, tmp, GameState.Snake.SnakeState.ALIVE, id);

            NodeRole role = session.getNewNodeRole();
            if(msg.getRequestedRole() == NodeRole.VIEWER){
                role = NodeRole.VIEWER;
            }
            System.out.println(role+" -role in join");
            session.addPlayer(
                    new Player(address,
                            port,
                            msg.getPlayerName(),
                            id,
                            0,
                            msg.getPlayerType(),
                            role
                    ),
                    snake
            );
            session.sender.sendAck(
                    new InetSocketAddress(address, port),
                    session.id,
                    id,
                    msgSeq);
            System.out.println("seq "+msgSeq);
            System.out.println("ack join");
            if(role == NodeRole.DEPUTY){
                session.sender.sendRoleChangeNewDeputy(
                        new InetSocketAddress(address, port),
                        session.id,
                        id
                );
                System.out.println("send change to deputy");
            }
        } else{
            session.sender.sendError(
                    new InetSocketAddress(address, port),
                    "net mesta",
                    session.id,
                    session.id);
            System.out.println("error join");
        }
    }

    private void handleSteer(GameMessage.SteerMsg msg, int id, String address, int port, long msgSeq){
        session.sender.sendAck(
                new InetSocketAddress(address, port),
                session.id,
                id,
                msgSeq);

        Direction direction = Direction.castReverse(msg.getDirection());

        for(Snake snake : session.gameProcess.snakes){
            if(snake.getId() == id){
                snake.setNewDirection(direction);
            }
        }

    }

    private void handlePing(GameMessage.PingMsg msg, int id, String address, int port, long msgSeq){
        session.sender.sendAck(
                new InetSocketAddress(address, port),
                session.id,
                id,
                msgSeq);
    }

    private void handleDiscover(String address, int port){
        System.out.println("get discover");
        session.sendAnnouncement(new InetSocketAddress(address, port));
    }

    private void handleRoleChange(GameMessage.RoleChangeMsg msg, int id, String address, int port, long msgSeq){

        session.sender.sendAck(
                new InetSocketAddress(address, port),
                session.id,
                id,
                msgSeq);
        if(msg.getSenderRole() == NodeRole.VIEWER){
            session.playerExit(id);
        }
    }
}

