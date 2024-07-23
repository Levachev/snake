package ru.levachev.Models;

import ru.levachev.messages.GameState;

import java.util.ArrayList;

import static java.lang.Math.*;
import static ru.levachev.Models.Direction.castReverse;

public class Snake {
    private Direction direction;
    private Direction newDirection;
    public ArrayList<Coordinate> body;
    private GameConfig gameConfig;
    private boolean isEat;
    private int score;
    private int id;
    public GameState.Snake.SnakeState snakeState;

    private Snake(){
        body = new ArrayList<>();
    }

    public Snake(GameConfig gameConfig, Coordinate beginCoordinate, GameState.Snake.SnakeState snakeState, int id){
        body = new ArrayList<>();
        this.id=id;
        this.gameConfig = gameConfig;
        this.snakeState=snakeState;
        isEat = false;
        score=0;
        body.add(beginCoordinate);

        direction = Direction.randomDirection();
        newDirection = direction;
        Coordinate tail = null;
        switch (direction){
            case DOWN -> tail = new Coordinate(0, -1);
            case UP -> tail = new Coordinate(0, 1);
            case LEFT -> tail = new Coordinate(1, 0);
            case RIGHT -> tail = new Coordinate(-1, 0);
        }
        body.add(tail);
    }

    private boolean onLine(Coordinate v1, Coordinate v2, Coordinate mid){
        if(v1.getX() == v2.getX()){
            if(mid.getX() == v1.getX()){
                return true;
            }
        } else if(v1.getY() == v2.getY()){
            if(mid.getY() == v1.getY()){
                return true;
            }
        }
        return false;
    }
    private Coordinate getUpdateCoord(Coordinate v2) {
        switch (direction){
            case DOWN -> v2 = new Coordinate(0, -1);
            case UP -> v2 = new Coordinate(0, 1);
            case LEFT -> v2 = new Coordinate(1, 0);
            case RIGHT -> v2 = new Coordinate(-1, 0);
        }
        return v2;
    }

    public void move() {

        Coordinate newHead = getNewHead();
        Coordinate head = getHead();
        Coordinate afterHead = body.get(1).getNewCoordinatePlus(head, gameConfig.length, gameConfig.height);

        if(onLine(newHead, afterHead, head)){
            body.set(0, newHead);
            updateAfterHead();
        } else{
            body.set(0, getUpdateCoord(head));
            body.add(0, newHead);
        }

        if(!isEat){
            moveTail();
        } else{
            isEat = false;
        }
    }

    private void printBody(){
        System.out.println();
        System.out.println("size "+body.size());
        for (Coordinate coordinate : body){
            System.out.println("x - "+coordinate.getX());
            System.out.println("y - "+coordinate.getY());
        }
        System.out.println();
    }

    private void updateAfterHead(){
        Coordinate afterHead = body.get(1);
        switch (direction){
            case RIGHT -> {
                afterHead = new Coordinate(afterHead.getX()-1, afterHead.getY());
            }
            case LEFT -> {
                afterHead = new Coordinate(afterHead.getX()+1, afterHead.getY());
            }
            case UP -> {
                afterHead = new Coordinate(afterHead.getX(), afterHead.getY()+1);
            }
            case DOWN -> {
                afterHead = new Coordinate(afterHead.getX(), afterHead.getY()-1);
            }
        }
        body.set(1, afterHead);
    }

    private void moveTail(){
        Coordinate tail = body.get(body.size()-1);

        int ii;
        int jj;
        if(tail.getX() == 0){
            ii = 0;
            jj = tail.getY()/abs(tail.getY());
        } else{
            ii = tail.getX()/abs(tail.getX());
            jj = 0;
        }

        tail = new Coordinate(tail.getX() - ii, tail.getY() - jj);

        body.set(body.size()-1, tail);
        if(tail.getX()==0 && tail.getY()==0){
            body.remove(body.size()-1);
        }
    }

    public void eat(){
        isEat=true;
        score++;
    }

    public Coordinate getHead(){
        return body.get(0);
    }
    public Coordinate getTail(){
        return body.get(body.size()-1);
    }
    public void setNewDirection(Direction newDirection){
        this.newDirection = newDirection;
    }

    public void switchDirection(){
        switch (direction){
            case RIGHT, LEFT -> {
                switch (newDirection){
                    case UP, DOWN -> direction = newDirection;
                }
            }
            case UP, DOWN -> {
                switch (newDirection){
                    case RIGHT, LEFT -> direction = newDirection;
                }
            }

        }
    }

    private Coordinate getNewHead(){
        switch (direction){
            case RIGHT -> {
                return new Coordinate((body.get(0).getX()+1)%gameConfig.length, body.get(0).getY());
            }
            case LEFT -> {
                return new Coordinate((body.get(0).getX()-1+gameConfig.length)%gameConfig.length, body.get(0).getY());
            }
            case UP -> {
                return new Coordinate(body.get(0).getX(), (body.get(0).getY()-1+gameConfig.height)%gameConfig.height);
            }
            case DOWN -> {
                return new Coordinate(body.get(0).getX(), (body.get(0).getY()+1)%gameConfig.height);
            }
        }
        return null;
    }

    public boolean isTouch(Snake snake){
        Coordinate next = new Coordinate(snake.body.get(0).getX(), snake.body.get(0).getY());
        if(body.get(0).equal(next)){
            return true;
        }
        for (int j=1;j<snake.body.size();j++){
            Coordinate tmp = snake.body.get(j);
            int ii;
            int jj;
            if(tmp.getX() == 0){
                ii = 0;
                jj = tmp.getY()/abs(tmp.getY());
            } else{
                ii = tmp.getX()/abs(tmp.getX());
                jj = 0;
            }
            Coordinate ij = new Coordinate(ii, jj);

            for(int kk = 0;kk<max(abs(tmp.getX()), abs(tmp.getY()));kk++){
                next.plus(ij, gameConfig.length, gameConfig.height);
                if(body.get(0).equal(next)){
                    return true;
                }
            }
        }
        return false;
    }

    public int getScore(){
        return score;
    }

    public GameState.Snake cast(){
        ArrayList<GameState.Coord> body = new ArrayList<>();
        for (Coordinate coord : this.body){
            body.add(coord.cast());
        }
        return GameState.Snake.newBuilder()
                .addAllPoints(body)
                .setState(this.snakeState)
                .setHeadDirection(direction.cast())
                .setPlayerId(id)
                .build();
    }

    public static Snake cast2(GameState.Snake snake, GameConfig config, int score){
        Snake snake1 = new Snake();
        for (GameState.Coord coord : snake.getPointsList()){
            snake1.body.add(new Coordinate(coord.getX(), coord.getY()));
        }
        snake1.direction = castReverse(snake.getHeadDirection());
        snake1.newDirection = snake1.direction;
        snake1.gameConfig = config;
        snake1.isEat = false;
        snake1.id = snake.getPlayerId();
        snake1.snakeState = snake.getState();
        snake1.score = score;
        return snake1;
    }

    public int getId() {
        return id;
    }
}
