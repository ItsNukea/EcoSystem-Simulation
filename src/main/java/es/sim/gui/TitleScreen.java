package es.sim.gui;

import es.sim.Main;

import javax.swing.*;
import java.awt.*;

import static es.sim.Main.*;

///The screen that shows on startup of the program
public class TitleScreen extends Screen {
    private final JButton continueButton;
    
    public TitleScreen() {
        super(null);

        Dimension bounds = Toolkit.getDefaultToolkit().getScreenSize();
        int screenwidth = bounds.width;
        int screenheight = bounds.height;

        int midX = screenwidth / 2;
        int midY = screenheight / 2;
        int buttonWidth = 200;
        int buttonHeight = 50;

        JButton startButton = new JButton("Start New Simulation");
        startButton.setBounds(
                midX - buttonWidth / 2,
                midY - buttonHeight / 2,
                buttonWidth,
                buttonHeight
        );
        startButton.addActionListener(_ -> {
            LOGGER.info("Starting New Simulation...");
            resetBoard();
            tickLoop.start();
            if(!renderLoop.isRunning()) {
                renderLoop.start();
            }
            Main.getWindow().showScreen(board);
        });
        
        continueButton = new JButton("Continue Last Simulation");
        continueButton.setBounds(
                midX - buttonWidth / 2,
                midY - buttonHeight / 2 + 20,
                buttonWidth,
                buttonHeight
        );
        continueButton.setEnabled(board != null);
        continueButton.addActionListener(_ -> {
            LOGGER.info("Continuing Last Simulation...");
            tickLoop.start();
            if(!renderLoop.isRunning()) {
                renderLoop.start();
            }
            
            Main.getWindow().showScreen(board);
        });

        JButton fullScreenButton = new JButton("Toggle fullscreen");
        fullScreenButton.setBounds(midX - buttonWidth / 2, midY - buttonHeight / 2, buttonWidth, buttonHeight);
        fullScreenButton.addActionListener(_ -> Main.getWindow().toggleFullScreen());

        add(startButton);
        add(continueButton);
        add(fullScreenButton);
    }

    @Override
    public void render(Graphics2D g) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        g.setFont(new Font("Sans Serif", Font.PLAIN, 80));
        drawCenteredString(g, "Eco System Simulation", screenSize.width / 2, screenSize.height / 2);
        continueButton.setEnabled(board != null);
    }

    @Override
    public void onClose() {
        System.exit(0);
    }
}