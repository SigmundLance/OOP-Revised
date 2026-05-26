import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// =========================================
// MAIN MENU CLASS
// =========================================

public class MainMenuPanel extends JPanel implements ActionListener {

    private JFrame frame;

    private JButton startButton;
    private JButton loginButton;
    private JButton leaderboardButton;
    private JButton exitButton;

    private Timer glowTimer;

    private float glow = 0f;
    private boolean increasing = true;

    public MainMenuPanel(JFrame frame) {

        this.frame = frame;

        setPreferredSize(new Dimension(600, 700));

        setBackground(new Color(10, 15, 20));

        setLayout(null);

        initializeButtons();

        // Glow animation
        glowTimer = new Timer(40, e -> {

            if (increasing) {

                glow += 0.03f;

                if (glow >= 1f) {
                    increasing = false;
                }

            } else {

                glow -= 0.03f;

                if (glow <= 0f) {
                    increasing = true;
                }
            }

            repaint();
        });

        glowTimer.start();
    }

    private void initializeButtons() {

        Font buttonFont = new Font("SansSerif", Font.BOLD, 18);

        startButton = createButton("START GAME", 200, 300);

        loginButton = createButton("LOGIN", 200, 370);

        leaderboardButton = createButton("LEADERBOARD", 200, 440);

        exitButton = createButton("EXIT", 200, 510);

        startButton.setFont(buttonFont);

        loginButton.setFont(buttonFont);

        leaderboardButton.setFont(buttonFont);

        exitButton.setFont(buttonFont);

        add(startButton);

        add(loginButton);

        add(leaderboardButton);

        add(exitButton);
    }

    private JButton createButton(String text, int x, int y) {

        JButton button = new JButton(text);

        button.setBounds(x, y, 200, 50);

        button.setFocusPainted(false);

        button.setBorderPainted(false);

        button.setBackground(new Color(20, 30, 40));

        button.setForeground(Color.CYAN);

        button.setFont(new Font("SansSerif", Font.BOLD, 18));

        button.addActionListener(this);

        // Hover effect
        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {

                button.setBackground(new Color(0, 120, 140));

                button.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {

                button.setBackground(new Color(20, 30, 40));

                button.setForeground(Color.CYAN);
            }
        });

        return button;
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        int width = getWidth();

        int height = getHeight();

        // Background Gradient
        GradientPaint bg = new GradientPaint(
                0,
                0,
                new Color(5, 15, 25),
                0,
                height,
                new Color(15, 25, 40)
        );

        g2.setPaint(bg);

        g2.fillRect(0, 0, width, height);

        // Grid lines
        g2.setColor(new Color(255, 255, 255, 15));

        for (int i = 0; i < width; i += 40) {

            g2.drawLine(i, 0, i, height);
        }

        for (int i = 0; i < height; i += 40) {

            g2.drawLine(0, i, width, i);
        }

        // Title
        String title = "TETRIXEL";

        Font titleFont = new Font("SansSerif", Font.BOLD, 64);

        g2.setFont(titleFont);

        FontMetrics fm = g2.getFontMetrics();

        int textWidth = fm.stringWidth(title);

        int x = (width - textWidth) / 2;

        int y = 180;

        int alpha = (int) (100 + glow * 155);

        // Glow effect
        for (int i = 8; i >= 1; i--) {

            g2.setColor(new Color(0, 255, 255, alpha / (i + 2)));

            g2.drawString(title, x - i / 2, y - i / 2);
        }

        // Main title
        g2.setColor(Color.CYAN);

        g2.drawString(title, x, y);

        // Subtitle
        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));

        String subtitle = "Arcade Neon Block Puzzle";

        int subWidth = g2.getFontMetrics().stringWidth(subtitle);

        g2.setColor(Color.LIGHT_GRAY);

        g2.drawString(subtitle, (width - subWidth) / 2, 220);

        // Footer
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));

        g2.setColor(new Color(180, 180, 180));

        String footer = "Created with Java Swing";

        int footerWidth = g2.getFontMetrics().stringWidth(footer);

        g2.drawString(
                footer,
                (width - footerWidth) / 2,
                height - 30
        );
    }

    // =========================================
    // BUTTON FUNCTIONS
    // =========================================

    @Override
    public void actionPerformed(ActionEvent e) {

        Object source = e.getSource();

        // =====================================
        // START GAME BUTTON
        // =====================================

        if (source == startButton) {

            frame.dispose();

            JFrame gameFrame = new JFrame("Tetrixel");

            Tetrixel game = new Tetrixel();

            gameFrame.add(game);

            gameFrame.pack();

            gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            gameFrame.setResizable(false);

            gameFrame.setLocationRelativeTo(null);

            gameFrame.setVisible(true);
        }

        // =====================================
        // LOGIN BUTTON
        // =====================================

        else if (source == loginButton) {

            frame.setContentPane(new LoginMenuPanel(frame));

            frame.revalidate();

            frame.repaint();
        }

        // =====================================
        // LEADERBOARD BUTTON
        // =====================================

        else if (source == leaderboardButton) {

            frame.setContentPane(new LeaderboardPanel(frame));

            frame.revalidate();

            frame.repaint();
        }

        // =====================================
        // EXIT BUTTON
        // =====================================

        else if (source == exitButton) {

            System.exit(0);
        }
    }

    // =========================================
    // MAIN METHOD
    // =========================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Tetrixel Main Menu");

            MainMenuPanel menu = new MainMenuPanel(frame);

            frame.setContentPane(menu);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.pack();

            frame.setResizable(false);

            frame.setLocationRelativeTo(null);

            frame.setVisible(true);
        });
    }
}

// =========================================
// LOGIN PANEL
// =========================================

class LoginMenuPanel extends JPanel {

    public LoginPanel(JFrame frame) {

        setBackground(Color.BLACK);

        setLayout(new BorderLayout());

        JLabel title = new JLabel("LOGIN PANEL", SwingConstants.CENTER);

        title.setForeground(Color.CYAN);

        title.setFont(new Font("SansSerif", Font.BOLD, 40));

        add(title, BorderLayout.CENTER);

        JButton backButton = new JButton("BACK");

        backButton.setBackground(Color.DARK_GRAY);

        backButton.setForeground(Color.WHITE);

        backButton.addActionListener(e -> {

            frame.setContentPane(new MainMenuPanel(frame));

            frame.revalidate();

            frame.repaint();
        });

        add(backButton, BorderLayout.SOUTH);
    }
}

// =========================================
// LEADERBOARD PANEL
// =========================================

class LeaderboardPanel extends JPanel {

    public LeaderboardPanel(JFrame frame) {

        setBackground(Color.BLACK);

        setLayout(new BorderLayout());

        JLabel title = new JLabel("LEADERBOARD", SwingConstants.CENTER);

        title.setForeground(Color.YELLOW);

        title.setFont(new Font("SansSerif", Font.BOLD, 40));

        add(title, BorderLayout.CENTER);

        JButton backButton = new JButton("BACK");

        backButton.setBackground(Color.DARK_GRAY);

        backButton.setForeground(Color.WHITE);

        backButton.addActionListener(e -> {

            frame.setContentPane(new MainMenuPanel(frame));

            frame.revalidate();

            frame.repaint();
        });

        add(backButton, BorderLayout.SOUTH);
    }
}