package ru.levachev.Models;

import ru.levachev.messages.GameMessage;

import java.net.SocketAddress;

public class GameMessageWrap {
    public GameMessage message;
    public SocketAddress address;
    public int number;

    public GameMessageWrap(GameMessage message, SocketAddress address, int number) {
        this.message = message;
        this.address = address;
        this.number=number;
    }
}
