package ru.levachev.Models;

import ru.levachev.Controller.UdpClientSender;
import ru.levachev.messages.GameMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;


public class MessageChecker {
    private final Vector<GameMessageWrap> list;
    private final Timer timer;
    private int delay;
    private Object lock = new Object();
    private final UdpClientSender clientSender;
    public boolean isCanceled;

    public MessageChecker(int delay, UdpClientSender clientSender){
        this.clientSender=clientSender;
        this.delay=delay;
        list = new Vector<>();
        timer = new Timer();
        isCanceled = false;
    }

    public boolean has(long seq){
        synchronized(lock){
            for(GameMessageWrap messageWrap : list){
                if(messageWrap.message.getMsgSeq()==seq){
                    return true;
                }
            }
        }
        return false;
    }

    public void addToList(GameMessage message, SocketAddress address, int number){
        synchronized(lock){
        list.add(new GameMessageWrap(message, address, number));
        int len = list.size();
        
        long seq = message.getMsgSeq();
        
        new java.util.Timer().schedule(
        new java.util.TimerTask() {
            public void run() {
                synchronized(lock){
                    if(!isCanceled){
                        try {
                            for(GameMessageWrap msg:list){
                                if(seq==msg.message.getMsgSeq() && !msg.message.hasRoleChange() && !msg.message.hasSteer()){
                                    list.removeIf(msg1->msg1.message.getMsgSeq()==seq);
                                    //System.out.println(msg.address);
                                    clientSender.send(msg.message, msg.address, msg.number+1);
                                    break;
                                }
                            }
                            Thread.currentThread().interrupt();
                            //list.removeIf(msg->msg.message.getMsgSeq()==seq);
                        } catch (IOException | IndexOutOfBoundsException ignored){
                        }
                    }
                }
            }
            }, delay, delay
         );
        }
    }

    public void clearLeaveManMsg(String addr, int port){
        synchronized(lock){
            //System.out.println("int lock");
            list.removeIf(messageWrap -> {
                InetSocketAddress tmp = (InetSocketAddress)messageWrap.address;
                return tmp.getAddress().getHostAddress().equals(addr) && tmp.getPort()==port;
            });
            //System.out.println("remove "+tmp);
        }
    }

    public boolean handleAck(long seq){
        //System.out.println("wait lock");
        synchronized(lock){
            //System.out.println("int lock");
            var tmp=list.removeIf(messageWrap -> messageWrap.message.getMsgSeq() == seq);
            //System.out.println("remove "+tmp);
            return tmp;
        }
    }

    public void deleteAll(){
        synchronized(lock){
            list.clear();
            isCanceled=true;
        }
    }
}
