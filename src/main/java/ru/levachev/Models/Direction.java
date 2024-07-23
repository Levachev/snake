package ru.levachev.Models;

import java.util.Random;

public enum Direction {
    UP,
    RIGHT,
    DOWN,
    LEFT;

    private static final Random PRNG = new Random();

    public static Direction randomDirection()  {
        Direction[] directions = values();
        return directions[PRNG.nextInt(directions.length)];
    }

    public ru.levachev.messages.Direction cast(){
        switch (this){
            case RIGHT -> {
                return ru.levachev.messages.Direction.RIGHT;
            }
            case LEFT -> {
                return ru.levachev.messages.Direction.LEFT;
            }
            case UP -> {
                return ru.levachev.messages.Direction.UP;
            }
            case DOWN -> {
                return ru.levachev.messages.Direction.DOWN;
            }
        }
        return ru.levachev.messages.Direction.DOWN;
    }

    public static Direction castReverse(ru.levachev.messages.Direction direction){
        switch (direction){
            case RIGHT -> {
                return RIGHT;
            }
            case LEFT -> {
                return LEFT;
            }
            case UP -> {
                return UP;
            }
            case DOWN -> {
                return DOWN;
            }
        }
        return DOWN;
    }
}
