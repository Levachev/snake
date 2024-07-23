package ru.levachev.Models;

public class GameConfig {
    public final int length;
    public final int height;
    public final int foodStatic;
    public final int stateDelayMs;

    public GameConfig(int length, int height, int foodStatic, int stateDelayMs) {
        this.length = length;
        this.height = height;
        this.foodStatic = foodStatic;
        this.stateDelayMs = stateDelayMs;
    }
}
