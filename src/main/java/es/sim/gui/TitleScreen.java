package es.sim.gui;

import es.sim.*;
import es.sim.game.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static es.sim.Main.*;

///The screen that shows on startup of the program
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

        JButton startButton = new JButton("START!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        startButton.setBounds(
                midX - buttonWidth / 2,
                midY - buttonHeight / 2,
                buttonWidth,
                buttonHeight
        );
        startButton.addActionListener(_ -> {
            LOGGER.info("Starting Simulation...");
            loop.start();
            Main.getWindow().showScreen(board);
        });

        JButton fullScreenButton = new JButton("Toggle fullscreen");
        fullScreenButton.setBounds(midX - buttonWidth / 2, midY - buttonHeight / 2, buttonWidth, buttonHeight);
        fullScreenButton.addActionListener(_ -> Main.getWindow().toggleFullScreen());

        add(startButton);
        add(fullScreenButton);
    }

    @Override
    public void render(Graphics2D g) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        g.setFont(new Font("Sans Serif", Font.PLAIN, 80));
        drawCenteredString(g, "Hello Testfjdsljeoif jsdmkjfo", screenSize.width / 2, screenSize.height / 2);
    }

    @Override
    public void onClose() {
        System.exit(0);
    }
}
