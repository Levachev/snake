package ru.levachev.View;

import javax.swing.*;
import java.awt.*;



public class Menu extends JPanel {
    private double myPixelWight;
    private double myPixelHeight;
    public Menu(double myPixelWight, double myPixelHeight) {
        this.myPixelWight=myPixelWight;
        this.myPixelHeight=myPixelHeight;
        this.setBackground(Color.CYAN);
    }

    public void paintComponent(Graphics g) {
    }
}
