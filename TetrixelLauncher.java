import java.awt.*;
import javax.swing.*;

public class TetrixelLauncher extends JFrame {
    CardLayout cardLayout = new CardLayout();
    JPanel mainPanel = new JPanel(cardLayout);
    GamePanel gamePanel = new GamePanel();

    public TetrixelLauncher() {
        setTitle("Tetrixel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainPanel.add(new LoginPanel(this), "MENU");
        mainPanel.add(gamePanel, "GAME");

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void showGame() {
        cardLayout.show(mainPanel, "GAME");
        gamePanel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TetrixelLauncher());
    }
}