package ru.levachev.Models;

import ru.levachev.messages.GameState;

public class Coordinate {
    private int x;
    private int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean equal(Coordinate coordinate){
        return x==coordinate.getX() && y==coordinate.getY();
    }

    public GameState.Coord cast(){
        return GameState.Coord.newBuilder()
                .setX(this.getX())
                .setY(this.getY())
                .build();
    }

    private int newValuePlus(int base, int append, int max){
        if(append < 0){
            return (base + append + max)%max;
        } else{
            return (base + append)%max;
        }
    }

    public Coordinate getNewCoordinatePlus(Coordinate base, int length, int height){
        int baseX = base.getX();
        int appendX = this.getX();
        int newX = newValuePlus(baseX, appendX, length);

        int baseY = base.getY();
        int appendY = this.getY();
        int newY = newValuePlus(baseY, appendY, height);
        return new Coordinate(newX, newY);
    }

    public void plus(Coordinate tmp, int length, int height){
        this.setX(newValuePlus(this.x, tmp.getX(), length));
        this.setY(newValuePlus(this.y, tmp.getY(), height));
    }

}
