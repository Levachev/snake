package ru.levachev.View;


import ru.levachev.Models.Coordinate;
import ru.levachev.Models.GameConfig;
import ru.levachev.Models.GameState;
import ru.levachev.messages.GamePlayer;
import ru.levachev.messages.NodeRole;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public class PlayCanvas extends JPanel{
    private volatile GameState gameState;
    private double myPixelWight;
    private double myPixelHeight;
    private int myID;
    private GameConfig gameConfig;
    private int wightPanel;
    private int heightPanel;

    public PlayCanvas(GameState gameState, int wightPanel, int heightPanel, double myPixelWight, double myPixelHeight, int myID, GameConfig gameConfig) {
        this.gameState = gameState;
        this.myPixelWight=myPixelWight;
        this.myPixelHeight=myPixelHeight;
        this.wightPanel=wightPanel;
        this.heightPanel=heightPanel;
        this.myID=myID;
        this.gameConfig=gameConfig;
        this.setBackground(Color.PINK);
    }

    public void updateState(GameState state){
        this.gameState=state;
    }

    private void drawSnakes(ArrayList<ru.levachev.messages.GameState.Snake> snakes, Graphics g){
        BufferedImage chunk = null;
        try {
            chunk = ImageIO.read(getClass().getResource("/images/block.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BufferedImage main = null;
        try {
            main = ImageIO.read(getClass().getResource("/images/main.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for(ru.levachev.messages.GameState.Snake snake : snakes){

            Coordinate next = new Coordinate(snake.getPoints(0).getX(), snake.getPoints(0).getY());

            drawChunk(g, chunk, main, snake, next);

            for (int j=1;j<snake.getPointsCount();j++){
                ru.levachev.messages.GameState.Coord tmp = snake.getPoints(j);

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
                    //System.out.println("in canvas x - "+next.getX());
                    //System.out.println("in canvas y - "+next.getY());
                    next.plus(ij, gameConfig.length, gameConfig.height);
                    drawChunk(g, chunk, main, snake, next);
                }
            }
        }
    }

    /*private PlayerType getPlayerTypeByID(int id, ){
        for(gameState.)
    }*/

    private void drawChunk(Graphics g, BufferedImage chunk, BufferedImage main, ru.levachev.messages.GameState.Snake snake, Coordinate next) {
        if (snake.getPlayerId() == myID) {
            g.drawImage(chunk, (int) (next.getX() * myPixelWight), (int) (next.getY() * myPixelHeight), (int) myPixelWight, (int) myPixelHeight, null);
        } else {
            g.drawImage(main, (int) (next.getX() * myPixelWight), (int) (next.getY() * myPixelHeight), (int) myPixelWight, (int) myPixelHeight, null);
        }
    }

    private void drawFood(ArrayList<Coordinate> food, Graphics g){
        BufferedImage foodImg = null;
        try {
            foodImg = ImageIO.read(getClass().getResource("/images/food.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int i=0;i<food.size();i++){
            Coordinate tmp = food.get(i);
            g.drawImage(foodImg, (int) (tmp.getX()*myPixelWight), (int) (tmp.getY()*myPixelHeight), (int) myPixelWight, (int) myPixelHeight, null);
        }
    }

    private void drawPlayer(GamePlayer player, Graphics g, int i, int n){
        int height = heightPanel/(n+2);
        AffineTransform affinetransform = new AffineTransform();
        FontRenderContext frc = new FontRenderContext(affinetransform, true, true);
        Font font = new Font("TimesRoman", Font.PLAIN, (int) myPixelHeight*4);
        g.setFont(font);
        String message = player.getName()+" score : " + player.getScore();
        //g.setColor(Color.CYAN);
        //g.fillRect(wightPanel - (int) (font.getStringBounds(message, frc).getWidth()) - 5, (int) myPixelHeight*(i+1), (int) (font.getStringBounds(message, frc).getWidth()), (int) myPixelHeight);
        g.setColor(Color.BLACK);
        g.drawString(message, wightPanel - (int) (font.getStringBounds(message, frc).getWidth()) - 5, height*(i+1));
    }

    private void drawStatus(NodeRole role, Graphics g, int n){
        int height = heightPanel/(n+2);
        if(role == NodeRole.VIEWER){
            AffineTransform affinetransform = new AffineTransform();
            FontRenderContext frc = new FontRenderContext(affinetransform, true, true);
            Font font = new Font("TimesRoman", Font.PLAIN, (int) myPixelHeight*4);
            g.setFont(font);
            String message = "You died"+myID;
            g.setColor(Color.BLACK);
            g.drawString(message, wightPanel - (int) (font.getStringBounds(message, frc).getWidth()) - 5, height);
        } else{
            AffineTransform affinetransform = new AffineTransform();
            FontRenderContext frc = new FontRenderContext(affinetransform, true, true);
            Font font = new Font("TimesRoman", Font.PLAIN, (int) myPixelHeight*4);
            g.setFont(font);
            String message = "You alive"+myID;
            g.setColor(Color.BLACK);
            g.drawString(message, wightPanel - (int) (font.getStringBounds(message, frc).getWidth()) - 5, height);
        }
    }

    private void drawScoreBoard(List<GamePlayer> players, Graphics g){
        int n = players.size();
        for(int i=0;i<players.size();i++){
            drawPlayer(players.get(i), g, i+1, n);
            if(players.get(i).getId()==myID){
                //System.out.println("in status name - "+players.get(i).getName()+" role "+players.get(i).getRole());
                drawStatus(players.get(i).getRole(), g, n);
            }
        }
    }

    private void drawBorder(Graphics g){
        BufferedImage chunk = null;
        try {
            chunk = ImageIO.read(getClass().getResource("/images/main.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for(int i=0;i<gameConfig.height;i++){
            g.drawImage(chunk, (int) (gameConfig.length * myPixelWight), (int) (i * myPixelHeight), (int) myPixelWight, (int) myPixelHeight, null);
        }
    }

    public void paintComponent( Graphics g) {
        super.paintComponent(g);

        System.out.println(gameState.snakes.size()+" size");
        System.out.println("begiiiiiiiiiiiiiiiiin");
        drawBorder(g);
        drawFood(gameState.food, g);
        drawSnakes(gameState.snakes, g);
        drawScoreBoard(gameState.players, g);
        drawBorder(g);
        System.out.println("enddddddddddddddddddddddddd");
    }

    private void printSnake(ru.levachev.messages.GameState.Snake snake){
        System.out.println("in canvas");
        for (ru.levachev.messages.GameState.Coord coordinate : snake.getPointsList()){
            System.out.println();
            System.out.println(coordinate.getX());
            System.out.println(coordinate.getY());
            System.out.println();
        }
        System.out.println("in canvas");
    }
}
