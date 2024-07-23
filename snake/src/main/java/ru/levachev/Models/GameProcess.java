package ru.levachev.Models;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public class GameProcess {
    public String gameName;
    public GameConfig gameConfig;
    public ArrayList<Coordinate> food = new ArrayList<>();
    public ArrayList<Snake> snakes = new ArrayList<>();
    public ArrayList<Integer> deadUsers = new ArrayList<>();

    public GameProcess(){
    }

    public GameProcess(GameConfig gameConfig, String gameName){
        this.gameName=gameName;
        this.gameConfig = gameConfig;
    }

    public void moveAll(){
        checkDead();
        checkEat();
        for(Snake snake : snakes){
            //System.out.println("moveeee");
            snake.switchDirection();
            snake.move();
        }
    }

    private void checkDead(){
        ArrayList<Snake> deadSnakes = new ArrayList<>();
        for(Snake mainSnake : snakes){

            Coordinate head = mainSnake.getHead();
            Coordinate next = new Coordinate(head.getX(), head.getY());
            for (int j=1;j<mainSnake.body.size();j++){
                Coordinate tmp = mainSnake.body.get(j);
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

                boolean isShouldBreak = false;

                for(int kk = 0;kk<max(abs(tmp.getX()), abs(tmp.getY()));kk++){
                    next.plus(ij, gameConfig.length, gameConfig.height);
                    if(head.equal(next)){
                        deadSnakes.add(mainSnake);
                        isShouldBreak = true;
                        break;
                    }
                }
                if(isShouldBreak){
                    break;
                }
            }

            for(Snake otherSnake : snakes){
                if(otherSnake!=mainSnake && mainSnake.isTouch(otherSnake)){
                    deadSnakes.add(mainSnake);
                    break;
                }
            }
        }
        for(Snake snake : deadSnakes){
            getFooaddedSnake(snake);
            snakes.remove(snake);
            //System.out.println(snake.getId());
            deadUsers.add(snake.getId());
        }
    }

    private boolean getRandomBool(){
        Random random = new Random();
        boolean tmp = random.nextBoolean();
        System.out.println("veroyat - "+tmp);
        return tmp;
    }

    private void getFooaddedSnake(Snake snake){
        Coordinate next = new Coordinate(snake.body.get(0).getX(), snake.body.get(0).getY());
        //maze[next.getX()][next.getY()] = true;
        if(getRandomBool()){
            food.add(new Coordinate(next.getX(), next.getY()));
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
                if(getRandomBool()){
                    food.add(new Coordinate(next.getX(), next.getY()));
                }            
            }
        }
    }

    private void checkEat(){
        for(Snake snake : snakes){
            Coordinate head = snake.getHead();
            for(Coordinate foodCoordinate : food){
                if(head.equal(foodCoordinate)){
                    snake.eat();
                    food.remove(foodCoordinate);
                    break;
                }
            }
        }
    }

    public void addSnake(Snake snake){
        snakes.add(snake);
    }

    private boolean isValidCoordinate(int x, int y){
        return x>=0 && x<gameConfig.length && y>=0 && y<gameConfig.height;
    }

    private void fillMaze(boolean[][] maze){
        for(int i = 0; i< snakes.size(); i++){
            Snake snake = snakes.get(i);
            Coordinate next = new Coordinate(snake.body.get(0).getX(), snake.body.get(0).getY());
            maze[next.getX()][next.getY()] = true;
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
                    maze[next.getX()][next.getY()] = true;
                }
            }
        }

        for (int i=0;i<food.size();i++){
            Coordinate tmp = food.get(i);
            maze[tmp.getX()][tmp.getY()] = true;
        }
    }

    private boolean isEmptyArea(Coordinate coordinate){
        boolean[][] maze = new boolean[gameConfig.length][gameConfig.height];
        fillMaze(maze);
        int x = coordinate.getX() - 2;
        int y = coordinate.getY() - 2;

        for(int i=0;i<5;i++){
            for(int j=0;j<5;j++){
                if(!(isValidCoordinate(x+i, y+j) && !maze[x+i][y+j])){
                    return false;
                }
            }
        }
        return true;
    }

    public Coordinate getEmptyArea(){
        for (int i=0;i<gameConfig.length;i++){
            for (int j=0;j<gameConfig.height;j++){
                Coordinate coordinate = new Coordinate(i, j);
                if(isEmptyArea(coordinate)){
                    return coordinate;
                }
            }
        }
        return null;
    }

    public void addFood(){
        boolean[][] maze = new boolean[gameConfig.length][gameConfig.height];
        fillMaze(maze);

        while(food.size() < (snakes.size()+ gameConfig.foodStatic)) {
            Coordinate tmp = getRandomCoordinate();
            if (!maze[tmp.getY()][tmp.getX()]) {
                food.add(tmp);
                maze[tmp.getY()][tmp.getX()] = true;
            }
        }
    }

    private Coordinate getRandomCoordinate(){
        int y = ThreadLocalRandom.current().nextInt(0, gameConfig.height-1);
        int x = ThreadLocalRandom.current().nextInt(0, gameConfig.length-1);
        return new Coordinate(x, y);
    }
}
