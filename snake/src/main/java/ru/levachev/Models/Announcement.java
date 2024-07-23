package ru.levachev.Models;

import java.time.LocalDateTime;

public class Announcement {
    public LocalDateTime last;
    public final GameConfig gameConfig;
    public final String gameName;

    public Announcement(LocalDateTime last, GameConfig gameConfig, String gameName) {
        this.last = last;
        this.gameConfig = gameConfig;
        this.gameName=gameName;
    }
}
