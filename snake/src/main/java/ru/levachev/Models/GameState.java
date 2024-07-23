package ru.levachev.Models;

import ru.levachev.messages.GamePlayer;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public ArrayList<Coordinate> food = new ArrayList<>();
    public ArrayList<ru.levachev.messages.GameState.Snake> snakes = new ArrayList<>();
    public List<GamePlayer> players;

    public GameState(GameProcess process, ArrayList<Player> players){
        for (Snake snake : process.snakes){
            snakes.add(snake.cast());
        }
        this.players = new ArrayList<>();
        for (Player player : players){
            this.players.add(player.cast());
        }
        this.food=process.food;
    }

    public GameState(ru.levachev.messages.GameState state){
        List<ru.levachev.messages.GameState.Coord> gameFood = state.getFoodsList();
        for (ru.levachev.messages.GameState.Coord coord : gameFood){
            food.add(new Coordinate(
                    coord.getX(),
                    coord.getY()
            ));
        }

        players = new ArrayList<>(state.getPlayers().getPlayersList());

        List<ru.levachev.messages.GameState.Snake> snakeList = state.getSnakesList();
        snakes.addAll(snakeList);
    }

    public void update(GameProcess process, ArrayList<Player> players){
        snakes.clear();
        //food.clear();
        for (Snake snake : process.snakes){
            snakes.add(snake.cast());
        }
        this.players = new ArrayList<>();
        for (Player player : players){
            this.players.add(player.cast());
        }
        food=process.food;
    }
}
