package ru.levachev;

import ru.levachev.Controller.ControllerOfSessions;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        JFrame jFrame = new JFrame();
        jFrame.pack();
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) screenSize.getWidth();
        int height = (int) screenSize.getHeight();
        jFrame.setBounds(width/6 , height/6, width*2/3, height*2/3);
        jFrame.setTitle("snake");
        jFrame.setResizable(false);

        JPanel menu = new JPanel();

        jFrame.add(menu);
        jFrame.setVisible(true);

        jFrame.revalidate();

        int mainWidth=menu.getWidth();
        int mainHeight=menu.getHeight();

        ControllerOfSessions controllerOfSessions = new ControllerOfSessions(jFrame, mainWidth, mainHeight);
        controllerOfSessions.run();
    }
}