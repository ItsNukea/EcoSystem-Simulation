package es.sim.gui;

import es.sim.*;
import es.sim.game.TickLoop;
import es.sim.texture.*;
import es.sim.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TitleScreen extends Screen {

    public TitleScreen() {
        super(null);

        Dimension bounds = Toolkit.getDefaultToolkit().getScreenSize();
        int screenwidth = bounds.width;
        int screenheight = bounds.height;

        int midX = screenwidth / 2;
        int midY = screenheight / 2;
        int buttonWidth = 200;
        int buttonHeight = 50;

        JButton startButton = new JButton("START!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        startButton.setBounds(
                midX - buttonWidth / 2,
                midY - buttonHeight / 2,
                buttonWidth,
                buttonHeight
        );
        startButton.addActionListener(_ -> {
            TickLoop.start(30);
        });

        add(startButton);
    }

    @Override
    public void render(Graphics2D g) {

    }
}
