import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.*;

// Make sure you save this file as exactly: Tetrixel.java

public class Tetrixel extends JPanel implements Runnable, KeyListener {

    private final int ROWS = 20;
    private final int COLS = 10;
    private final int BLOCK_SIZE = 28;
    
    // Position of the grid on the screen
    private final int BOARD_X = 200;
    private final int BOARD_Y = 50;

    private Color[][] grid;
    private Tetromino currentPiece;
    private Tetromino nextPiece;
    
    private Thread gameThread;
    private boolean isRunning = false;
    
    // Game States
    private boolean inMenu = true;
    private boolean isPaused = false;
    private boolean gameOver = false;
    private boolean scoreSubmitted = false;

    private int delay = 500; // Speed of the game

    public Tetrixel() {
        setPreferredSize(new Dimension(600, 700));
        setBackground(new Color(10, 15, 20)); 
        setFocusable(true);
        addKeyListener(this);

        Tetromino.loadLeaderboard();
        
        // Start the thread immediately for rendering, but game logic waits for Menu
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    private void startGame() {
        grid = new Color[ROWS][COLS];
        Tetromino.resetScore();
        currentPiece = new Tetromino();
        nextPiece = new Tetromino();
        gameOver = false;
        isPaused = false;
        scoreSubmitted = false;
    }

    @Override
    public void run() {
        while (isRunning) {
            if (!inMenu && !isPaused && !gameOver) {
                updateGame();
            }
            repaint();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateGame() {
        if (!currentPiece.moveDown(grid)) {
            currentPiece.lock(grid);
            checkLines();
            currentPiece = nextPiece;
            nextPiece = new Tetromino();

            if (currentPiece.collision(grid, currentPiece.x, currentPiece.y)) {
                gameOver = true;
            }
        }
    }

    private void checkLines() {
        int linesCleared = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == null) {
                    full = false;
                    break;
                }
            }
            if (full) {
                linesCleared++;
                for (int moveRow = r; moveRow > 0; moveRow--) {
                    for (int c = 0; c < COLS; c++) {
                        grid[moveRow][c] = grid[moveRow - 1][c];
                    }
                }
                for (int c = 0; c < COLS; c++) {
                    grid[0][c] = null;
                }
                r++; 
            }
        }

        if (linesCleared > 0) {
            Tetromino.addScore(linesCleared * 100);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (inMenu) {
            drawStartMenu(g2);
        } else {
            drawUI(g2);
            drawGrid(g2);

            if (currentPiece != null && !gameOver) {
                currentPiece.drawGhost(g2, BLOCK_SIZE, BOARD_X, BOARD_Y, grid);
                // Pause particles from moving by only running update if not paused
                currentPiece.draw(g2, BLOCK_SIZE, BOARD_X, BOARD_Y, !isPaused); 
            }

            if (isPaused && !gameOver) {
                drawPauseOverlay(g2);
            } else if (gameOver) {
                drawGameOverOverlay(g2);
                
                if (!scoreSubmitted) {
                    scoreSubmitted = true;
                    handleLeaderboardSubmission();
                }
            }
        }
    }

    private void drawStartMenu(Graphics2D g2) {
        int width = getWidth();
        int height = getHeight();

        // Background elements
        g2.setColor(new Color(5, 20, 25));
        g2.fillRect(0, 0, width, height);
        
        // Grid pattern for background
        g2.setColor(new Color(255, 255, 255, 10));
        for (int i = 0; i < width; i += 40) g2.drawLine(i, 0, i, height);
        for (int i = 0; i < height; i += 40) g2.drawLine(0, i, width, i);

        // Title
        String title = "TETRIXEL";
        g2.setFont(new Font("SansSerif", Font.BOLD, 64));
        int tW = g2.getFontMetrics().stringWidth(title);
        
        // Title shadow/glow
        g2.setColor(new Color(0, 255, 255, 100));
        g2.drawString(title, (width - tW) / 2 + 4, height / 3 + 4);
        g2.setColor(new Color(0, 255, 255));
        g2.drawString(title, (width - tW) / 2, height / 3);

        // Subtitle
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        String sub = "PRESS [ ENTER ] TO START";
        int sW = g2.getFontMetrics().stringWidth(sub);
        
        // Pulsing effect for start text
        int alpha = (int) (128 + 127 * Math.sin(System.currentTimeMillis() / 200.0));
        g2.setColor(new Color(255, 255, 50, alpha));
        g2.drawString(sub, (width - sW) / 2, height / 2 + 50);

        // Controls info
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(Color.GRAY);
        String controls = "Arrows: Move / Rotate | Space: Drop | ESC: Pause";
        int cW = g2.getFontMetrics().stringWidth(controls);
        g2.drawString(controls, (width - cW) / 2, height - 50);
    }

    private void drawPauseOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(BOARD_X, BOARD_Y, COLS * BLOCK_SIZE, ROWS * BLOCK_SIZE);

        g2.setFont(new Font("SansSerif", Font.BOLD, 42));
        String msg1 = "PAUSED";
        int m1W = g2.getFontMetrics().stringWidth(msg1);
        
        g2.setColor(Color.BLACK); // Shadow
        g2.drawString(msg1, BOARD_X + ((COLS * BLOCK_SIZE) - m1W) / 2 + 3, BOARD_Y + ROWS * BLOCK_SIZE / 2 + 3);
        
        g2.setColor(new Color(0, 255, 255)); // Cyan text
        g2.drawString(msg1, BOARD_X + ((COLS * BLOCK_SIZE) - m1W) / 2, BOARD_Y + ROWS * BLOCK_SIZE / 2);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String msg2 = "Press ESC to Resume";
        int m2W = g2.getFontMetrics().stringWidth(msg2);
        g2.setColor(Color.WHITE);
        g2.drawString(msg2, BOARD_X + ((COLS * BLOCK_SIZE) - m2W) / 2, BOARD_Y + ROWS * BLOCK_SIZE / 2 + 40);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(5, 20, 25)); 
        g2.fillRect(BOARD_X, BOARD_Y, COLS * BLOCK_SIZE, ROWS * BLOCK_SIZE);

        g2.setColor(new Color(255, 255, 255, 20));
        for (int r = 0; r <= ROWS; r++) {
            g2.drawLine(BOARD_X, BOARD_Y + r * BLOCK_SIZE, BOARD_X + COLS * BLOCK_SIZE, BOARD_Y + r * BLOCK_SIZE);
        }
        for (int c = 0; c <= COLS; c++) {
            g2.drawLine(BOARD_X + c * BLOCK_SIZE, BOARD_Y, BOARD_X + c * BLOCK_SIZE, BOARD_Y + ROWS * BLOCK_SIZE);
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != null) {
                    int px = BOARD_X + c * BLOCK_SIZE;
                    int py = BOARD_Y + r * BLOCK_SIZE;

                    GradientPaint gp = new GradientPaint(px, py, grid[r][c].brighter(), px + BLOCK_SIZE, py + BLOCK_SIZE, grid[r][c].darker());
                    g2.setPaint(gp);
                    g2.fillRoundRect(px + 1, py + 1, BLOCK_SIZE - 2, BLOCK_SIZE - 2, 8, 8);
                    
                    g2.setColor(new Color(255, 255, 255, 100));
                    g2.drawRoundRect(px, py, BLOCK_SIZE - 1, BLOCK_SIZE - 1, 8, 8);
                }
            }
        }
        
        g2.setColor(new Color(0, 150, 150));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(BOARD_X, BOARD_Y, COLS * BLOCK_SIZE, ROWS * BLOCK_SIZE);
    }

    private void drawUI(Graphics2D g2) {
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        int textX = 20;
        int textY = 80;
        g2.drawString("<- -> Move", textX, textY);
        g2.drawString("^ Rotate", textX, textY + 25);
        g2.drawString("v Soft Drop", textX, textY + 50);
        g2.drawString("SPACE Instant Drop", textX, textY + 75);
        g2.drawString("ESC Pause", textX, textY + 100);
        g2.drawString("R Restart", textX, textY + 125);

        int boxX = BOARD_X + (COLS * BLOCK_SIZE) + 20;
        g2.setColor(new Color(20, 30, 40));
        g2.fillRoundRect(boxX, 50, 150, 80, 15, 15);
        g2.setColor(new Color(0, 100, 100));
        g2.drawRoundRect(boxX, 50, 150, 80, 15, 15);
        
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("SCORE", boxX + 45, 75);
        
        g2.setColor(Color.YELLOW);
        String scoreText = String.valueOf(Tetromino.currentScore);
        int scoreWidth = g2.getFontMetrics().stringWidth(scoreText);
        g2.drawString(scoreText, boxX + (150 - scoreWidth) / 2, 105);

        g2.setColor(new Color(20, 30, 40));
        g2.fillRoundRect(boxX, 150, 150, 120, 15, 15);
        g2.setColor(new Color(0, 100, 100));
        g2.drawRoundRect(boxX, 150, 150, 120, 15, 15);
        
        g2.setColor(Color.GRAY);
        g2.drawString("NEXT", boxX + 50, 175);
        
        if (nextPiece != null) {
            nextPiece.drawPreviewCentered(g2, boxX, 180, 150, 80, BLOCK_SIZE);
        }
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(BOARD_X, BOARD_Y, COLS * BLOCK_SIZE, ROWS * BLOCK_SIZE);

        g2.setColor(new Color(255, 50, 50));
        g2.setFont(new Font("SansSerif", Font.BOLD, 42));
        String msg1 = "GAME OVER";
        int m1W = g2.getFontMetrics().stringWidth(msg1);
        
        // Add a slight drop shadow to the Game Over text
        g2.setColor(Color.BLACK);
        g2.drawString(msg1, BOARD_X + ((COLS * BLOCK_SIZE) - m1W) / 2 + 3, BOARD_Y + 103);
        
        g2.setColor(new Color(255, 50, 50));
        g2.drawString(msg1, BOARD_X + ((COLS * BLOCK_SIZE) - m1W) / 2, BOARD_Y + 100);

        Tetromino.drawLeaderboard(g2, getWidth(), getHeight());
    }

    private void handleLeaderboardSubmission() {
        SwingUtilities.invokeLater(() -> {
            String name = JOptionPane.showInputDialog(this, "Game Over!\nEnter your name for the leaderboard:", "New High Score!", JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.trim().isEmpty()) {
                name = "Player";
            }
            if (name.length() > 8) {
                name = name.substring(0, 8);
            }
            Tetromino.submitScore(name);
            repaint(); 
        });
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // 1. Handle Menu Input
        if (inMenu) {
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                inMenu = false;
                startGame();
            }
            return;
        }

        // 2. Handle Game Over Input
        if (gameOver) {
            if (key == KeyEvent.VK_R) {
                startGame();
            }
            return; 
        }

        // 3. Handle Pause Input
        if (key == KeyEvent.VK_ESCAPE) {
            isPaused = !isPaused;
            repaint();
            return;
        }

        // 4. Ignore other inputs if paused
        if (isPaused) {
            return;
        }

        // 5. Normal Game Controls
        if (key == KeyEvent.VK_LEFT) {
            currentPiece.moveSide(grid, -1);
        } else if (key == KeyEvent.VK_RIGHT) {
            currentPiece.moveSide(grid, 1);
        } else if (key == KeyEvent.VK_UP) {
            currentPiece.rotate(grid);
        } else if (key == KeyEvent.VK_DOWN) {
            currentPiece.moveDown(grid);
        } else if (key == KeyEvent.VK_SPACE) {
            while (currentPiece.moveDown(grid)) {
            }
            currentPiece.lock(grid);
            checkLines();
            currentPiece = nextPiece;
            nextPiece = new Tetromino();
            if (currentPiece.collision(grid, currentPiece.x, currentPiece.y)) {
                gameOver = true;
            }
        } else if (key == KeyEvent.VK_R) {
            startGame();
        }
        
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tetrixel");
        Tetrixel game = new Tetrixel();
        
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
    }
}

// ==========================================
// Tetromino Class 
// ==========================================

class Tetromino {

    int[][] shape;
    Color color;
    int x, y;

    static Random rand = new Random();

    ArrayList<Particle> particles = new ArrayList<>();

    public static int currentScore = 0;
    private static final String SCORE_FILE = "leaderboard.dat";
    public static final int MAX_LEADERBOARD_SIZE = 5;
    public static List<ScoreEntry> leaderboard = new ArrayList<>();

    static int[][][] shapes = {
        {{1,1,1,1}},
        {{1,1},{1,1}},
        {{0,1,0},{1,1,1}},
        {{1,1,0},{0,1,1}},
        {{0,1,1},{1,1,0}},
        {{1,0,0},{1,1,1}},
        {{0,0,1},{1,1,1}}
    };

    static Color[] colors = {
        new Color(0, 255, 255),
        Color.YELLOW,
        new Color(255, 0, 255),
        Color.GREEN,
        Color.RED,
        new Color(255, 165, 0),
        new Color(0, 100, 255)
    };

    public Tetromino() {
        int i = rand.nextInt(shapes.length);
        shape = shapes[i];
        color = colors[i];
        resetPosition();
    }

    public static class ScoreEntry implements Comparable<ScoreEntry> {
        public String name;
        public int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public int compareTo(ScoreEntry other) {
            return Integer.compare(other.score, this.score);
        }
    }

    public static void loadLeaderboard() {
        leaderboard.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(SCORE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    leaderboard.add(new ScoreEntry(parts[0], Integer.parseInt(parts[1])));
                }
            }
            Collections.sort(leaderboard);
        } catch (Exception e) {}
    }

    public static void saveLeaderboard() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(SCORE_FILE))) {
            for (ScoreEntry entry : leaderboard) {
                bw.write(entry.name + "," + entry.score);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving leaderboard: " + e.getMessage());
        }
    }

    public static void addScore(int points) {
        currentScore += points;
    }

    public static void submitScore(String playerName) {
        leaderboard.add(new ScoreEntry(playerName, currentScore));
        Collections.sort(leaderboard);
        if (leaderboard.size() > MAX_LEADERBOARD_SIZE) {
            leaderboard = new ArrayList<>(leaderboard.subList(0, MAX_LEADERBOARD_SIZE));
        }
        saveLeaderboard();
    }

    public static void resetScore() {
        currentScore = 0;
    }

    // ==========================================
    // Arcade / Neon Leaderboard UI
    // ==========================================
    public static void drawLeaderboard(Graphics g, int panelWidth, int panelHeight) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fullscreen dark overlay for dramatic effect
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, panelWidth, panelHeight);

        int boxW = 340;
        int boxH = 400;
        int boxX = (panelWidth - boxW) / 2;
        int boxY = (panelHeight - boxH) / 2 + 20;

        // Background Gradient for the panel
        GradientPaint bgGradient = new GradientPaint(boxX, boxY, new Color(20, 25, 40, 240), boxX, boxY + boxH, new Color(10, 10, 20, 240));
        g2.setPaint(bgGradient);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 25, 25);

        // Simulated Neon Glow Border
        for (int i = 0; i < 4; i++) {
            g2.setColor(new Color(0, 255, 255, 40 - (i * 10)));
            g2.drawRoundRect(boxX - i, boxY - i, boxW + (i * 2), boxH + (i * 2), 25, 25);
        }
        g2.setColor(new Color(0, 220, 255));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 25, 25);

        // Title text with drop shadow
        g2.setFont(new Font("SansSerif", Font.BOLD, 26));
        String title = "HALL OF FAME";
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        
        g2.setColor(Color.BLACK); // Shadow
        g2.drawString(title, boxX + (boxW - titleWidth) / 2 + 2, boxY + 42);
        
        g2.setColor(new Color(255, 255, 50)); // Bright yellow
        g2.drawString(title, boxX + (boxW - titleWidth) / 2, boxY + 40);

        // Glowing Underline gradient
        GradientPaint lineGradient = new GradientPaint(boxX + 40, boxY + 55, new Color(0, 200, 255, 0), boxX + boxW/2, boxY + 55, new Color(0, 255, 255), true);
        g2.setPaint(lineGradient);
        g2.fillRect(boxX + 40, boxY + 55, boxW - 80, 2);

        // Entries
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        int startY = boxY + 95;
        
        for (int i = 0; i < MAX_LEADERBOARD_SIZE; i++) {
            int rowY = startY + (i * 48);
            
            // Alternating Row Backgrounds (Pill shapes)
            g2.setColor(new Color(255, 255, 255, i % 2 == 0 ? 15 : 5));
            g2.fillRoundRect(boxX + 20, rowY - 26, boxW - 40, 38, 18, 18);

            if (i < leaderboard.size()) {
                ScoreEntry entry = leaderboard.get(i);
                
                Color rankColor;
                if (i == 0) rankColor = new Color(255, 215, 0); // Gold
                else if (i == 1) rankColor = new Color(224, 224, 224); // Silver
                else if (i == 2) rankColor = new Color(205, 127, 50); // Bronze
                else rankColor = new Color(0, 255, 255); // Cyan

                g2.setColor(rankColor);
                String rank = "#" + (i + 1);
                g2.drawString(rank, boxX + 40, rowY);

                g2.setColor(Color.WHITE);
                g2.drawString(entry.name, boxX + 90, rowY);
                
                g2.setColor(rankColor);
                String score = String.valueOf(entry.score);
                int scoreWidth = g2.getFontMetrics().stringWidth(score);
                g2.drawString(score, boxX + boxW - 40 - scoreWidth, rowY);
            } else {
                g2.setColor(new Color(100, 100, 100));
                g2.drawString("#" + (i + 1), boxX + 40, rowY);
                g2.drawString("---", boxX + 90, rowY);
                g2.drawString("0", boxX + boxW - 55, rowY);
            }
        }
        
        // Footnote Instructions integrated into the panel
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        String msg = "[ PRESS 'R' TO RESTART ]";
        int msgWidth = g2.getFontMetrics().stringWidth(msg);
        
        g2.setColor(new Color(255, 255, 255, 150));
        g2.drawString(msg, boxX + (boxW - msgWidth) / 2, boxY + boxH - 25);
    }

    public final void resetPosition() {
        x = 3;
        y = 0;
    }

    public boolean moveDown(Color[][] grid) {
        if (!collision(grid, x, y + 1)) {
            y++;
            return true;
        }
        return false;
    }

    public void moveSide(Color[][] grid, int dir) {
        if (!collision(grid, x + dir, y)) {
            x += dir;
        }
    }

    public void rotate(Color[][] grid) {
        int[][] rotated = new int[shape[0].length][shape.length];
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[0].length; j++) {
                rotated[j][shape.length - 1 - i] = shape[i][j];
            }
        }
        int[][] old = shape;
        shape = rotated;

        if (collision(grid, x, y)) {
            if (!collision(grid, x - 1, y)) x--;
            else if (!collision(grid, x + 1, y)) x++;
            else shape = old;
        }
    }

    public boolean collision(Color[][] grid, int nx, int ny) {
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                if (shape[i][j] != 0) {
                    int gx = nx + j;
                    int gy = ny + i;
                    if (gx < 0 || gx >= 10 || gy >= 20) return true;
                    if (gy >= 0 && grid[gy][gx] != null) return true;
                }
            }
        }
        return false;
    }

    public void lock(Color[][] grid) {
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                if (shape[i][j] != 0) {
                    grid[y + i][x + j] = color;
                    createExplosion((x + j) * 28 + 14, (y + i) * 28 + 14);
                }
            }
        }
    }

    // Overloaded draw method to allow passing a flag on whether to update particles
    public void draw(Graphics g, int size, int offsetX, int offsetY, boolean updateParticles) {
        drawShape(g, x, y, size, offsetX, offsetY, color, 255);
        if (updateParticles) {
            updateParticles(g, offsetX, offsetY);
        } else {
            // If paused, just draw the particles without updating their life/position
            drawParticles(g, offsetX, offsetY);
        }
    }
    
    // Backwards compatibility for original code flow if needed elsewhere
    public void draw(Graphics g, int size, int offsetX, int offsetY) {
        draw(g, size, offsetX, offsetY, true);
    }

    public void drawGhost(Graphics g, int size, int offsetX, int offsetY, Color[][] grid) {
        int ghostY = y;
        while (!collision(grid, x, ghostY + 1)) {
            ghostY++;
        }
        drawShape(g, x, ghostY, size, offsetX, offsetY, color, 60);
    }

    private void drawShape(Graphics g, int tx, int ty, int size, int offsetX, int offsetY, Color c, int alpha) {
        Graphics2D g2 = (Graphics2D) g;
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                if (shape[i][j] != 0) {
                    int px = (tx + j) * size + offsetX;
                    int py = (ty + i) * size + offsetY;

                    GradientPaint gp = new GradientPaint(px, py, c.brighter(), px + size, py + size, c.darker());
                    g2.setPaint(gp);
                    g2.fillRoundRect(px + 1, py + 1, size - 2, size - 2, 8, 8);
                    
                    g2.setColor(new Color(255, 255, 255, alpha));
                    g2.drawRoundRect(px, py, size - 1, size - 1, 8, 8);
                }
            }
        }
    }

    public void drawPreviewCentered(Graphics g, int bx, int by, int bw, int bh, int size) {
        int offsetX = bx + (bw - shape[0].length * size) / 2;
        int offsetY = by + (bh - shape.length * size) / 2 + 5;
        Graphics2D g2 = (Graphics2D) g;
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                if (shape[i][j] != 0) {
                    int px = offsetX + j * size;
                    int py = offsetY + i * size;
                    GradientPaint gp = new GradientPaint(px, py, color.brighter(), px + size, py + size, color.darker());
                    g2.setPaint(gp);
                    g2.fillRoundRect(px, py, size - 2, size - 2, 6, 6);
                }
            }
        }
    }

    private void createExplosion(int px, int py) {
        for (int i = 0; i < 8; i++) {
            particles.add(new Particle(px, py, color));
        }
    }

    private void updateParticles(Graphics g, int offsetX, int offsetY) {
        Graphics2D g2 = (Graphics2D) g;
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            p.update();
            p.draw(g2, offsetX, offsetY);
            if (p.life <= 0) {
                particles.remove(i);
                i--;
            }
        }
    }
    
    private void drawParticles(Graphics g, int offsetX, int offsetY) {
        Graphics2D g2 = (Graphics2D) g;
        for (Particle p : particles) {
            p.draw(g2, offsetX, offsetY);
        }
    }

    class Particle {
        float x, y, dx, dy;
        int life = 25;
        Color c;

        Particle(float x, float y, Color c) {
            this.x = x;
            this.y = y;
            this.c = c;
            dx = (rand.nextFloat() - 0.5f) * 6;
            dy = (rand.nextFloat() - 0.5f) * 6;
        }

        void update() {
            x += dx;
            y += dy;
            dy += 0.05f;
            life--;
        }

        void draw(Graphics2D g2, int ox, int oy) {
            int alpha = Math.max(0, life * 10);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            g2.fillOval((int)x + ox, (int)y + oy, 6, 6);
        }
    }
}