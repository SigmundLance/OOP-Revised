import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GamePanel extends JPanel implements ActionListener {

    Board board;
    Timer timer;

    boolean leftPressed = false;
    boolean rightPressed = false;
    boolean downPressed = false;

    int moveDelay = 0;
    int moveSpeed = 5;

    int flashAlpha = 0;

    int lastLevel = 0;

    public GamePanel() {

        setPreferredSize(new Dimension(360, 640));
        setBackground(Color.BLACK);

        board = new Board();

        timer = new Timer(1000 / 60, this);

        setFocusable(true);

        setupControls();
    }

    public void startGame() {
        timer.start();
        requestFocusInWindow();
    }

    private void setupControls() {

        addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {

                if (board.gameOver && e.getKeyCode() == KeyEvent.VK_R) {

                    board.reset();

                    lastLevel = 0;
                    board.dropSpeed = 35;

                    return;
                }

                switch (e.getKeyCode()) {

                    case KeyEvent.VK_LEFT -> leftPressed = true;
                    case KeyEvent.VK_RIGHT -> rightPressed = true;
                    case KeyEvent.VK_DOWN -> downPressed = true;

                    case KeyEvent.VK_UP -> board.rotate();

                    // =========================
                    // FIXED SPACE (SAFE INSTANT DROP)
                    // =========================
                    case KeyEvent.VK_SPACE -> {

                        // Drop fully
                        board.hardDrop();

                        // SAFELY resolve using normal game loop (NO DESYNC)
                        board.update();

                        flashAlpha = 70;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

                switch (e.getKeyCode()) {

                    case KeyEvent.VK_LEFT -> leftPressed = false;
                    case KeyEvent.VK_RIGHT -> rightPressed = false;
                    case KeyEvent.VK_DOWN -> downPressed = false;
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        moveDelay++;

        if (moveDelay >= moveSpeed) {

            if (leftPressed) board.move(-1);
            if (rightPressed) board.move(1);

            moveDelay = 0;
        }

        board.update();

        int level = board.score / 300;

        if (level != lastLevel) {

            lastLevel = level;

            board.dropSpeed = Math.max(10, 35 - (level * 3));
        }

        if (flashAlpha > 0) {
            flashAlpha -= 4;
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        GradientPaint bg = new GradientPaint(
            0, 0,
            new Color(12, 12, 20),
            0, getHeight(),
            new Color(5, 5, 10)
        );

        g2.setPaint(bg);
        g2.fillRect(0, 0, getWidth(), getHeight());

        board.draw(g2);

        if (flashAlpha > 0) {
            g2.setColor(new Color(0, 255, 255, flashAlpha));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(Color.LIGHT_GRAY);

        g2.drawString("← → Move", 10, 20);
        g2.drawString("↑ Rotate", 10, 40);
        g2.drawString("↓ Soft Drop", 10, 60);
        g2.drawString("SPACE Instant Drop", 10, 80);
        g2.drawString("R Restart", 10, 100);

        if (board.gameOver) {

            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(new Font("Arial", Font.BOLD, 32));
            g2.setColor(Color.RED);
            g2.drawString("SYSTEM FAILURE", 35, 280);

            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(Color.WHITE);
            g2.drawString("PRESS R TO RESTART", 70, 330);
        }
    }
}