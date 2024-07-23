package ru.levachev.Models;

import java.time.LocalDateTime;

import ru.levachev.messages.GamePlayer;
import ru.levachev.messages.NodeRole;
import ru.levachev.messages.PlayerType;

public class Player {
    private String address;
    private int port;
    private String name;
    private int id;
    private int score;
    private PlayerType type;
    private NodeRole role;
    public LocalDateTime last;

    public Player(){
    }

    public Player(String address, int port, String name, int id, int score, PlayerType type, NodeRole role) {
        this.address = address;
        this.port = port;
        this.name = name;
        this.id = id;
        this.score = score;
        this.type = type;
        this.role = role;
        last = LocalDateTime.now();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public PlayerType getType() {
        return type;
    }

    public void setType(PlayerType type) {
        this.type = type;
    }

    public NodeRole getRole() {
        return role;
    }

    public void setRole(NodeRole role) {
        this.role = role;
    }

    public GamePlayer cast(){
        var builder = GamePlayer.newBuilder()
                .setName(this.getName())
                .setId(this.getId())
                .setScore(this.getScore())
                .setRole(this.getRole());

        if(this.getAddress()!=null) {
            builder.setIpAddress(this.getAddress()).setPort(this.getPort());
        }

        return builder.build();
    }
}
