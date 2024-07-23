package ru.levachev.Controller;

import com.google.protobuf.InvalidProtocolBufferException;
import ru.levachev.Controller.sessions.JoinSession;
import ru.levachev.Models.GameConfig;
import ru.levachev.Models.Player;
import ru.levachev.messages.GameAnnouncement;
import ru.levachev.messages.GameMessage;
import ru.levachev.messages.GamePlayer;
import ru.levachev.messages.GameState;
import ru.levachev.messages.NodeRole;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.Arrays;

public class UdpClientListener implements Runnable{
    private DatagramSocket socket;
    private JoinSession session;

    public UdpClientListener(JoinSession session, DatagramSocket socket) throws SocketException {
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

                // System.out.println("catch in client");
                // System.out.println("1 "+LocalDateTime.now());

                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                int len = packet.getLength();

                byte[] received = Arrays.copyOfRange(packet.getData(), 0, len);

                GameMessage gameMessage = GameMessage.newBuilder().setMsgSeq(1).build();
                try {
                    gameMessage = GameMessage.parseFrom(received);
                } catch (InvalidProtocolBufferException e){
                    System.out.println(len+" cannot parse message");
                }

                handleMessage(gameMessage, address.getHostAddress(), port);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleAck(GameMessage message){
        boolean retVal=session.sender.messageChecker.handleAck(message.getMsgSeq());
        //System.out.println("remove "+retVal);
        if(retVal && message.hasAck() && !session.isJoin){
            System.out.println("ack");
            session.id = message.getReceiverId();
            System.out.println("moeeeeeeeeeee "+session.id);
            session.sender.messageChecker.handleAck(message.getMsgSeq());
            System.out.println("after handle ack");
            session.isJoin=true;
        }
    }

    private void handleError(GameMessage message){
        boolean retVal=session.sender.messageChecker.handleAck(message.getMsgSeq());
        System.out.println("remove "+retVal);
        if(retVal && message.hasError() && !session.isJoin){
            System.out.println("error");
            session.sender.sendAck(session.masterAddress, 0, 0, message.getMsgSeq());
            System.exit(0);
        }
    }

    private void handleMessage(GameMessage message, String address, int port){
        if(session.master!=null && session.master.getAddress().equals(address) && session.master.getPort()==port){
            session.lastMastertime=LocalDateTime.now();
        }
        if(message.hasAck()){
            handleAck(message);
        } else if(message.hasError()){
            handleError(message);
        } else if(message.hasPing()){
            handlePing(message.getPing(), 0, address, port, message.getMsgSeq());
        } else if(message.hasRoleChange()){
            handleRoleChange(message.getRoleChange(), message.getSenderId(), address, port, message.getMsgSeq());
        } else if(message.hasAnnouncement()){
            handleAnnouncement(message.getAnnouncement());
        } else if(message.hasState()){
            System.out.println("change state "+address+" "+port);
            GameState state = handleState(message.getState(), address, port, message.getMsgSeq());
            var tmp = message.getState();
            if(tmp.getState().getStateOrder()>session.current){
                session.current=tmp.getState().getStateOrder();
                session.updateState(new ru.levachev.Models.GameState(state));
                session.updateSession();
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

    private GameState handleState(GameMessage.StateMsg msg, String address, int port, long msgSeq){
        session.sender.sendAck(
                new InetSocketAddress(address, port),
                session.id,
                session.id,
                msgSeq);
        System.out.println(LocalDateTime.now());
        // System.out.println("2 "+LocalDateTime.now());
        // System.out.println("msgSeq "+msgSeq);
        return msg.getState();
    }

    private void handleAnnouncement(GameMessage.AnnouncementMsg msg){
        GameAnnouncement gameAnnouncement = msg.getGames(0);
        ru.levachev.messages.GameConfig config = gameAnnouncement.getConfig();
        session.gameConfig = new GameConfig(
                    config.getWidth(),
                    config.getHeight(),
                    config.getFoodStatic(),
                    config.getStateDelayMs()
        );
    }

    private void handleRoleChange(GameMessage.RoleChangeMsg msg, int id, String address, int port, long msgSeq){
        System.out.println("on role change");
        GamePlayer player = session.getPlayer(id);

        if(msg.getSenderRole() == NodeRole.MASTER){
            if(player == null){
                return;
            }
            System.out.println("new MASTER");
            session.current=-1;
            session.setMaster(
                    address,
                    port,
                    player.getName(),
                    id,
                    player.getScore(),
                    player.getType()
            );
            if(msg.getReceiverRole() == NodeRole.DEPUTY){
                System.out.println("im deputy");
                session.setDeputy(id);
            }
        } else if(msg.getReceiverRole() == NodeRole.VIEWER){
            session.myRole=NodeRole.VIEWER;
            System.out.println("die receive");
        }
        else if(msg.getReceiverRole() == NodeRole.MASTER){
            if(player == null){
                return;
            }
            System.out.println("im MASTER");
            session.needSwitch();
        }
        else if(msg.getReceiverRole() == NodeRole.DEPUTY){
            System.out.println("im deputy");
            session.setDeputy(id);
        }

        session.sender.sendAck(
                new InetSocketAddress(address, port),
                session.id,
                id,
                msgSeq);
                System.out.println("send ack in receive");
    }
}

