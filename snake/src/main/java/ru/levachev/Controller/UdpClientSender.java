package ru.levachev.Controller;

import ru.levachev.Models.*;
import ru.levachev.messages.*;
import ru.levachev.messages.GameConfig;
import ru.levachev.messages.GameState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class UdpClientSender {
    private final DatagramSocket socket;
    public final MessageChecker messageChecker;
    private int order;

    public UdpClientSender(DatagramSocket socket, int delay) throws SocketException {
        order = 0;
        messageChecker = new MessageChecker(delay, this);
        this.socket=socket;
    }

    public void sendAnnouncement(SocketAddress address, ru.levachev.Models.GameConfig gameConfig, ArrayList<Player> players, String gameName, int sendID, int recvID){

        ArrayList<GamePlayer> gamePlayers = new ArrayList<>();

        for (Player player : players){
            System.out.println(player.getName()+" in announ build "+player.getAddress()+" "+player.getPort());
            gamePlayers.add(player.cast());
        }

        GameAnnouncement announcement = GameAnnouncement.newBuilder()
                .setPlayers(GamePlayers.newBuilder()
                        .addAllPlayers(gamePlayers)
                        .build())
                .setConfig(GameConfig.newBuilder()
                        .setWidth(gameConfig.length)
                        .setHeight(gameConfig.height)
                        .setFoodStatic(gameConfig.foodStatic)
                        .setStateDelayMs(gameConfig.stateDelayMs)
                        .build())
                .setCanJoin(true)
                .setGameName(gameName)
                .build();
        GameMessage.AnnouncementMsg msg  = GameMessage.AnnouncementMsg.newBuilder()
                .addGames(announcement)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder().setAnnouncement(msg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        var tmp = gameMessage.getAnnouncement().getGames(0);
        try {
            send(gameMessage, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
    }

    public void sendPing(SocketAddress address, int sendID, int recvID){
        GameMessage.PingMsg pingMsg = GameMessage.PingMsg.newBuilder().build();
        GameMessage message = GameMessage.newBuilder().setPing(pingMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
    }

    public void sendAck(SocketAddress address, int sendID, int recvID, long seq){
        GameMessage.AckMsg ackMsg = GameMessage.AckMsg.newBuilder().build();
        GameMessage message = GameMessage.newBuilder().setAck(ackMsg)
                .setMsgSeq(seq)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            System.out.println("ack "+order);
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
    }

    public void sendSteer(SocketAddress address, ru.levachev.Models.Direction direction, int sendID, int recvID){
        GameMessage.SteerMsg steerMsg = GameMessage.SteerMsg.newBuilder()
                .setDirection(direction.cast())
                .build();
        GameMessage message = GameMessage.newBuilder().setSteer(steerMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }

        order++;
    }

    public void sendDiscover(SocketAddress address, int sendID, int recvID){
        GameMessage.DiscoverMsg discoverMsg = GameMessage.DiscoverMsg.newBuilder().build();
        GameMessage message = GameMessage.newBuilder().setDiscover(discoverMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            System.out.println("send discover");
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
    }

    public void sendState(SocketAddress address, GameProcess process, ArrayList<Player> players, int sendID, int recvID){
        ArrayList<GamePlayer> gamePlayers = new ArrayList<>();

        for (Player player : players){
            gamePlayers.add(player.cast());
        }

        ArrayList<GameState.Coord> food = new ArrayList<>();

        for (Coordinate coord : process.food){
            food.add(coord.cast());
        }

        ArrayList<GameState.Snake> snakes = new ArrayList<>();

        for (Snake snake : process.snakes){
            snakes.add(snake.cast());
        }

        GamePlayers gamePlayers1 = GamePlayers.newBuilder()
                .addAllPlayers(gamePlayers)
                .build();
        GameState gameState = GameState.newBuilder()
                .addAllFoods(food)
                .addAllSnakes(snakes)
                .setPlayers(gamePlayers1)
                .setStateOrder(order)
                .build();
        GameMessage.StateMsg stateMsg = GameMessage.StateMsg.newBuilder()
                .setState(gameState)
                .build();
        GameMessage message = GameMessage.newBuilder().setState(stateMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            System.out.println("send state");
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
    }

    public void sendError(SocketAddress address, String msg, int sendID, int recvID){
        GameMessage.ErrorMsg errorMsg = GameMessage.ErrorMsg.newBuilder()
                .setErrorMessage(msg)
                .build();
        GameMessage message = GameMessage.newBuilder().setError(errorMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
    }

    public long sendRoleChangeDieMaster(SocketAddress address, int sendID, int recvID){
        GameMessage.RoleChangeMsg roleChangeMsg = GameMessage.RoleChangeMsg.newBuilder()
                .setReceiverRole(NodeRole.MASTER)
                .build();
        GameMessage message = GameMessage.newBuilder().setRoleChange(roleChangeMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            send(message, address, 0);
            System.out.println("send to new master");
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
        return order-1;
    }

    public void sendNewMaster(SocketAddress address, int sendID, int recvID){
        GameMessage.RoleChangeMsg roleChangeMsg = GameMessage.RoleChangeMsg.newBuilder()
                .setSenderRole(NodeRole.MASTER)
                .build();
        GameMessage message = GameMessage.newBuilder().setRoleChange(roleChangeMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
    }

    public long sendRoleChangeNewDeputy(SocketAddress address, int sendID, int recvID){
        GameMessage.RoleChangeMsg roleChangeMsg = GameMessage.RoleChangeMsg.newBuilder()
                .setReceiverRole(NodeRole.DEPUTY)
                .build();
        GameMessage message = GameMessage.newBuilder().setRoleChange(roleChangeMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            System.out.println("send new deputy");
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
        return order-1;
    }

    public long sendRoleExit(SocketAddress address, int sendID, int recvID){
        GameMessage.RoleChangeMsg roleChangeMsg = GameMessage.RoleChangeMsg.newBuilder()
                .setSenderRole(NodeRole.VIEWER)
                .build();
        GameMessage message = GameMessage.newBuilder().setRoleChange(roleChangeMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
        return order-1;
    }

    public long sendRoleChangeDieDefault(SocketAddress address, int sendID, int recvID){
        GameMessage.RoleChangeMsg roleChangeMsg = GameMessage.RoleChangeMsg.newBuilder()
                .setReceiverRole(NodeRole.VIEWER)
                .build();
        GameMessage message = GameMessage.newBuilder().setRoleChange(roleChangeMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
        return order-1;
    }

    public void sendJoinMsg(SocketAddress address, PlayerType type, String name, String gameName, NodeRole role, int sendID, int recvID){
        GameMessage.JoinMsg joinMsg = GameMessage.JoinMsg.newBuilder()
                .setPlayerType(type)
                .setPlayerName(name)
                .setGameName(gameName)
                .setRequestedRole(role)
                .build();
        GameMessage message = GameMessage.newBuilder().setJoin(joinMsg)
                .setMsgSeq(order)
                .setSenderId(sendID)
                .setReceiverId(recvID)
                .build();
        try {
            System.out.println("send join");
            send(message, address, 0);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        order++;
    }

    public void send(GameMessage message, SocketAddress address, int number) throws IOException {
        if(number>5){
            return;
        }
        byte[] buf = message.toByteArray();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address);
        socket.send(packet);
        if(!message.hasAck() && !message.hasAnnouncement() && !message.hasDiscover()) {
            messageChecker.addToList(message, address, number);
        }
        //System.out.println("in send "+packet.getLength());
    }
}
