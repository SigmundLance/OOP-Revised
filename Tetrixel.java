import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class Tetrixel extends JPanel implements Runnable, KeyListener {

    private final int ROWS = 20;
    private final int COLS = 10;

    private int BLOCK_SIZE = 32;

    private int BOARD_X;
    private int BOARD_Y;

    private Color[][] grid;

    private Tetromino currentPiece;
    private Tetromino nextPiece;

    private Thread gameThread;
    private boolean isRunning = false;

    private boolean inMenu = true;
    private boolean isPaused = false;
    private boolean gameOver = false;
    private boolean scoreSubmitted = false;

    private final int INITIAL_DELAY = 450; // Keep track of base speed
    private int delay = INITIAL_DELAY;

    // --- SCREEN SHAKE VARIABLES ---
    private int shakeDuration = 0;
    private int shakeMagnitude = 0;
    private int shakeX = 0;
    private int shakeY = 0;
    private boolean shakeTriggered1000 = false;
    private final Random shakeRand = new Random();
    // ------------------------------

    // Line clear animation
    private ArrayList<BreakEffect> breakEffects = new ArrayList<>();

    public Tetrixel() {

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        setPreferredSize(screen);

        setBackground(new Color(10, 15, 20));

        BOARD_X = (screen.width - (COLS * BLOCK_SIZE)) / 2;
        BOARD_Y = 70;

        setFocusable(true);
        addKeyListener(this);

        Tetromino.loadLeaderboard();

        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    private void startGame() {

        grid = new Color[ROWS][COLS];

        Tetromino.resetScore();
        
        // RESET SPEED FOR NEW GAME
        delay = INITIAL_DELAY; 

        currentPiece = new Tetromino();
        nextPiece = new Tetromino();

        gameOver = false;
        isPaused = false;
        scoreSubmitted = false;

        // Reset shake tracking for a new game
        shakeTriggered1000 = false;
        shakeDuration = 0;
        shakeX = 0;
        shakeY = 0;
    }

    @Override
    public void run() {
        // Track the last time the game logic updated
        long lastUpdate = System.currentTimeMillis();

        while (isRunning) {
            long now = System.currentTimeMillis();

            if (!inMenu && !isPaused && !gameOver) {
                // Only drop the block if 'delay' milliseconds have passed
                if (now - lastUpdate >= delay) {
                    updateGame();
                    lastUpdate = now;
                }

                // ==========================================================
                // UPDATED: SCREEN SHAKE LOGIC (CONTINUOUS AT 1500+ POINTS)
                // ==========================================================
                if (Tetromino.currentScore >= 1500) {
                    int constantMagnitude = 10; // Constant intense shake intensity
                    shakeX = shakeRand.nextInt(constantMagnitude * 2 + 1) - constantMagnitude;
                    shakeY = shakeRand.nextInt(constantMagnitude * 2 + 1) - constantMagnitude;
                } else if (shakeDuration > 0) {
                    shakeX = shakeRand.nextInt(shakeMagnitude * 2 + 1) - shakeMagnitude;
                    shakeY = shakeRand.nextInt(shakeMagnitude * 2 + 1) - shakeMagnitude;
                    shakeDuration--;
                } else {
                    shakeX = 0;
                    shakeY = 0;
                }
                // ==========================================================
            } else {
                // Keep the timer fresh if we are paused or in menu
                lastUpdate = now;
                
                // Prevent screen from staying permanently offset when paused/gameover
                shakeX = 0;
                shakeY = 0;
            }

            // Always repaint at 60FPS so animations remain perfectly smooth
            repaint();

            try {
                // Sleep for ~16ms (approx 60 Frames Per Second)
                Thread.sleep(16);
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

                // ADD BREAK EFFECTS
                for (int c = 0; c < COLS; c++) {

                    int px = BOARD_X + c * BLOCK_SIZE;
                    int py = BOARD_Y + r * BLOCK_SIZE;

                    breakEffects.add(new BreakEffect(px, py, grid[r][c]));
                }

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

        // TRIGGER SCREEN SHAKE AT 1000 POINTS
        if (Tetromino.currentScore >= 1000 && !shakeTriggered1000) {
            shakeDuration = 40;   // Shakes for 40 frames (~0.66 seconds)
            shakeMagnitude = 12;  // Up to 12 pixels of offset in any direction
            shakeTriggered1000 = true;
        }

        // DYNAMIC SPEED CALCULATOR
        // Every 1000 points drops the delay by 45ms. Clamped at a minimum of 90ms.
        int currentLevel = Tetromino.currentScore / 1000;
        delay = Math.max(90, INITIAL_DELAY - (currentLevel * 45));
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // APPLY SCREEN SHAKE TRANSLATION
        g2.translate(shakeX, shakeY);

        if (inMenu) {

            drawMainMenu(g2);

        } else {

            drawGrid(g2);

            drawUI(g2);

            drawBreakEffects(g2);

            if (currentPiece != null && !gameOver) {

                currentPiece.drawGhost(g2,
                        BLOCK_SIZE,
                        BOARD_X,
                        BOARD_Y,
                        grid);

                currentPiece.draw(g2,
                        BLOCK_SIZE,
                        BOARD_X,
                        BOARD_Y,
                        !isPaused);
            }

            if (isPaused && !gameOver) {
                drawPauseOverlay(g2);
            }

            if (gameOver) {

                drawGameOverOverlay(g2);

                if (!scoreSubmitted) {

                    scoreSubmitted = true;

                    handleLeaderboardSubmission();
                }
            }
        }

        // RESET TRANSLATION TO KEEP GRAPHICS CONTEXT CLEAN
        g2.translate(-shakeX, -shakeY);
    }

    // =========================================
    // MAIN MENU
    // =========================================

    private void drawMainMenu(Graphics2D g2) {

        int w = getWidth();
        int h = getHeight();

        GradientPaint bg = new GradientPaint(
                0, 0,
                new Color(5, 10, 20),
                0, h,
                new Color(15, 30, 50));

        g2.setPaint(bg);
        g2.fillRect(0, 0, w, h);

        // Grid background
        g2.setColor(new Color(255,255,255,15));

        for (int i = 0; i < w; i += 40)
            g2.drawLine(i, 0, i, h);

        for (int i = 0; i < h; i += 40)
            g2.drawLine(0, i, w, i);

        // Title shadow LOWER OPACITY
        g2.setFont(new Font("SansSerif", Font.BOLD, 90));

        String title = "TETRIXEL";

        int tw = g2.getFontMetrics().stringWidth(title);

        g2.setColor(new Color(0,0,0,60));

        g2.drawString(title, (w - tw)/2 + 5, h/3 + 5);

        g2.setColor(new Color(0,255,255));

        g2.drawString(title, (w - tw)/2, h/3);

        // Start text
        g2.setFont(new Font("SansSerif", Font.BOLD, 26));

        int alpha = (int)(140 + 100 *
                Math.sin(System.currentTimeMillis() / 250.0));

        g2.setColor(new Color(255,255,100,alpha));

        String press = "PRESS ENTER TO START";

        int pw = g2.getFontMetrics().stringWidth(press);

        g2.drawString(press, (w - pw)/2, h/2 + 60);

    }

    // =========================================
    // GAME UI
    // =========================================

    private void drawUI(Graphics2D g2) {

        int boxX = 30;

        // CONTROLS CONTAINER
        g2.setColor(new Color(20, 30, 40, 220));
        g2.fillRoundRect(boxX, 60, 240, 190, 20, 20);

        g2.setColor(new Color(0, 255, 255, 100));
        g2.drawRoundRect(boxX, 60, 240, 190, 20, 20);

        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.setColor(Color.CYAN);
        g2.drawString("CONTROLS", boxX + 60, 90);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2.setColor(Color.WHITE);

        int textY = 120;

        g2.drawString("LEFT / RIGHT  - Move", boxX + 20, textY);
        g2.drawString("UP ARROW      - Rotate", boxX + 20, textY + 25);
        g2.drawString("DOWN ARROW    - Soft Drop", boxX + 20, textY + 50);
        g2.drawString("SPACE         - Instant Drop", boxX + 20, textY + 75);
        g2.drawString("ESC           - Pause", boxX + 20, textY + 100);
        g2.drawString("R             - Restart", boxX + 20, textY + 125);

        // SCORE BOX
        int scoreX = BOARD_X + (COLS * BLOCK_SIZE) + 30;

        g2.setColor(new Color(20,30,40,220));
        g2.fillRoundRect(scoreX, 60, 180, 100, 20,20);

        g2.setColor(new Color(0,255,255,100));
        g2.drawRoundRect(scoreX, 60, 180, 100, 20,20);

        g2.setColor(Color.CYAN);
        g2.setFont(new Font("SansSerif", Font.BOLD, 24));

        g2.drawString("SCORE", scoreX + 45, 95);

        g2.setColor(Color.YELLOW);

        String score = String.valueOf(Tetromino.currentScore);

        int sw = g2.getFontMetrics().stringWidth(score);

        g2.drawString(score, scoreX + (180-sw)/2, 130);

        // SPEED LEVEL DISPLAY
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(Color.LIGHT_GRAY);
        String speedTxt = "SPEED LVL: " + (Tetromino.currentScore / 1000 + 1);
        int stw = g2.getFontMetrics().stringWidth(speedTxt);
        g2.drawString(speedTxt, scoreX + (180-stw)/2, 150);

        // NEXT BOX
        g2.setColor(new Color(20,30,40,220));
        g2.fillRoundRect(scoreX, 190, 180, 150, 20,20);

        g2.setColor(new Color(0,255,255,100));
        g2.drawRoundRect(scoreX, 190, 180, 150, 20,20);

        g2.setColor(Color.CYAN);

        g2.drawString("NEXT", scoreX + 55, 225);

        if (nextPiece != null) {
            nextPiece.drawPreviewCentered(
                    g2,
                    scoreX,
                    240,
                    180,
                    90,
                    BLOCK_SIZE
            );
        }
    }

    private void drawGrid(Graphics2D g2) {

        g2.setColor(new Color(5,20,25));

        g2.fillRect(BOARD_X,
                BOARD_Y,
                COLS * BLOCK_SIZE,
                ROWS * BLOCK_SIZE);

        g2.setColor(new Color(255,255,255,20));

        for (int r = 0; r <= ROWS; r++) {

            g2.drawLine(
                    BOARD_X,
                    BOARD_Y + r * BLOCK_SIZE,
                    BOARD_X + COLS * BLOCK_SIZE,
                    BOARD_Y + r * BLOCK_SIZE);
        }

        for (int c = 0; c <= COLS; c++) {

            g2.drawLine(
                    BOARD_X + c * BLOCK_SIZE,
                    BOARD_Y,
                    BOARD_X + c * BLOCK_SIZE,
                    BOARD_Y + ROWS * BLOCK_SIZE);
        }

        for (int r = 0; r < ROWS; r++) {

            for (int c = 0; c < COLS; c++) {

                if (grid[r][c] != null) {

                    int px = BOARD_X + c * BLOCK_SIZE;
                    int py = BOARD_Y + r * BLOCK_SIZE;

                    GradientPaint gp = new GradientPaint(
                            px, py,
                            grid[r][c].brighter(),
                            px + BLOCK_SIZE,
                            py + BLOCK_SIZE,
                            grid[r][c].darker());

                    g2.setPaint(gp);

                    g2.fillRoundRect(
                            px + 1,
                            py + 1,
                            BLOCK_SIZE - 2,
                            BLOCK_SIZE - 2,
                            8,
                            8);

                    // LIGHTER SHADOW
                    g2.setColor(new Color(0,0,0,40));

                    g2.drawRoundRect(
                            px + 2,
                            py + 2,
                            BLOCK_SIZE - 4,
                            BLOCK_SIZE - 4,
                            8,
                            8);

                    g2.setColor(new Color(255,255,255,90));

                    g2.drawRoundRect(
                            px,
                            py,
                            BLOCK_SIZE - 1,
                            BLOCK_SIZE - 1,
                            8,
                            8);
                }
            }
        }

        g2.setColor(new Color(0,255,255,120));

        g2.setStroke(new BasicStroke(2));

        g2.drawRect(
                BOARD_X,
                BOARD_Y,
                COLS * BLOCK_SIZE,
                ROWS * BLOCK_SIZE);
    }

    // =========================================
    // BREAK EFFECTS (Optimized for performance)
    // =========================================

    private void drawBreakEffects(Graphics2D g2) {

        for (int i = 0; i < breakEffects.size(); i++) {

            BreakEffect b = breakEffects.get(i);

            // Update runs at 60 FPS now, so it will animate smoothly!
            b.update();
            b.draw(g2);

            if (b.life <= 0) {
                breakEffects.remove(i);
                i--;
            }
        }
    }

    class BreakEffect {

        int life = 20; // Lasts ~0.33 seconds at 60FPS
        ArrayList<MiniParticle> particles = new ArrayList<>();

        BreakEffect(int x, int y, Color color) {
            // Lower Quality: Dropped to 5 particles per block instead of 14
            for (int i = 0; i < 5; i++) {
                particles.add(new MiniParticle(x, y, color));
            }
        }

        void update() {
            life--;
            for (MiniParticle p : particles) {
                p.update();
            }
        }

        void draw(Graphics2D g2) {
            for (MiniParticle p : particles) {
                p.draw(g2);
            }
        }
    }

    class MiniParticle {

        float x, y, dx, dy;
        int life = 20; 
        Color c;

        MiniParticle(float x, float y, Color c) {
            this.x = x + BLOCK_SIZE/2;
            this.y = y + BLOCK_SIZE/2;
            this.c = c;

            Random r = new Random();

            // Faster initial burst
            dx = (r.nextFloat() - 0.5f) * 12;
            dy = (r.nextFloat() - 0.5f) * 12;
        }

        void update() {
            x += dx;
            y += dy;
            dy += 0.5f; // Gravity pulling down slightly
            life--;
        }

        void draw(Graphics2D g2) {
            if (life <= 0) return;
            
            // Fast, solid color rendering (no laggy alpha channels or ovals)
            g2.setColor(c);
            g2.fillRect((int)x, (int)y, 5, 5);
        }
    }

    // =========================================

    private void drawPauseOverlay(Graphics2D g2) {

        g2.setColor(new Color(0,0,0,170));

        g2.fillRect(
                BOARD_X,
                BOARD_Y,
                COLS * BLOCK_SIZE,
                ROWS * BLOCK_SIZE);

        g2.setFont(new Font("SansSerif", Font.BOLD, 44));

        String text = "PAUSED";

        int tw = g2.getFontMetrics().stringWidth(text);

        g2.setColor(new Color(0,0,0,60));

        g2.drawString(
                text,
                BOARD_X + ((COLS * BLOCK_SIZE)-tw)/2 + 4,
                BOARD_Y + ROWS * BLOCK_SIZE/2 + 4);

        g2.setColor(Color.CYAN);

        g2.drawString(
                text,
                BOARD_X + ((COLS * BLOCK_SIZE)-tw)/2,
                BOARD_Y + ROWS * BLOCK_SIZE/2);
    }

    private void drawGameOverOverlay(Graphics2D g2) {

        g2.setColor(new Color(0,0,0,190));

        g2.fillRect(0,0,getWidth(),getHeight());

        g2.setFont(new Font("SansSerif", Font.BOLD, 60));

        String over = "GAME OVER";

        int ow = g2.getFontMetrics().stringWidth(over);

        g2.setColor(new Color(0,0,0,60));

        g2.drawString(over,
                (getWidth()-ow)/2 + 4,
                120 + 4);

        g2.setColor(Color.RED);

        g2.drawString(over,
                (getWidth()-ow)/2,
                120);

        Tetromino.drawLeaderboard(g2, getWidth(), getHeight());
    }

    private void handleLeaderboardSubmission() {

        SwingUtilities.invokeLater(() -> {

            String name = JOptionPane.showInputDialog(
                    this,
                    "Enter your name:");

            if (name == null || name.trim().isEmpty()) {
                name = "PLAYER";
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

        if (inMenu) {

            if (key == KeyEvent.VK_ENTER) {

                inMenu = false;

                startGame();
            }

            return;
        }

        if (gameOver) {

            if (key == KeyEvent.VK_R) {
                startGame();
            }

            return;
        }

        if (key == KeyEvent.VK_ESCAPE) {

            isPaused = !isPaused;

            repaint();

            return;
        }

        if (isPaused) return;

        if (key == KeyEvent.VK_LEFT) {
            currentPiece.moveSide(grid, -1);
        }

        else if (key == KeyEvent.VK_RIGHT) {
            currentPiece.moveSide(grid, 1);
        }

        else if (key == KeyEvent.VK_UP) {
            currentPiece.rotate(grid);
        }

        else if (key == KeyEvent.VK_DOWN) {
            currentPiece.moveDown(grid);
        }

        else if (key == KeyEvent.VK_SPACE) {

            while (currentPiece.moveDown(grid)) {}

            currentPiece.lock(grid);

            checkLines();

            currentPiece = nextPiece;
            nextPiece = new Tetromino();

            if (currentPiece.collision(grid,
                    currentPiece.x,
                    currentPiece.y)) {

                gameOver = true;
            }
        }

        else if (key == KeyEvent.VK_R) {

            startGame();
        }

        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {

        JFrame frame = new JFrame("Tetrixel");

        Tetrixel game = new Tetrixel();

        frame.add(game);

        frame.setUndecorated(true);

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.pack();

        frame.setVisible(true);
    }
}

// =========================================
// TETROMINO
// =========================================

class Tetromino {

    int[][] shape;
    Color color;
    int x, y;

    static Random rand = new Random();

    public static int currentScore = 0;

    private static final String SCORE_FILE = "leaderboard.dat";

    public static final int MAX_LEADERBOARD_SIZE = 5;

    public static List<ScoreEntry> leaderboard =
            new ArrayList<>();

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
            new Color(0,255,255),
            Color.YELLOW,
            new Color(255,0,255),
            Color.GREEN,
            Color.RED,
            new Color(255,165,0),
            new Color(0,100,255)
    };

    public Tetromino() {

        int i = rand.nextInt(shapes.length);

        shape = shapes[i];
        color = colors[i];

        resetPosition();
    }

    public static class ScoreEntry
            implements Comparable<ScoreEntry> {

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

        try (BufferedReader br =
                     new BufferedReader(
                             new FileReader(SCORE_FILE))) {

            String line;

            while ((line = br.readLine()) != null) {

                String[] parts = line.split(",");

                if (parts.length == 2) {

                    leaderboard.add(
                            new ScoreEntry(
                                    parts[0],
                                    Integer.parseInt(parts[1])));
                }
            }

            Collections.sort(leaderboard);

        } catch (Exception e) {}
    }

    public static void saveLeaderboard() {

        try (BufferedWriter bw =
                     new BufferedWriter(
                             new FileWriter(SCORE_FILE))) {

            for (ScoreEntry entry : leaderboard) {

                bw.write(entry.name + "," + entry.score);

                bw.newLine();
            }

        } catch (IOException e) {}
    }

    public static void addScore(int points) {
        currentScore += points;
    }

    public static void submitScore(String playerName) {

        leaderboard.add(
                new ScoreEntry(playerName, currentScore));

        Collections.sort(leaderboard);

        if (leaderboard.size() > MAX_LEADERBOARD_SIZE) {

            leaderboard =
                    new ArrayList<>(
                            leaderboard.subList(
                                    0,
                                    MAX_LEADERBOARD_SIZE));
        }

        saveLeaderboard();
    }

    public static void resetScore() {
        currentScore = 0;
    }

    public static void drawLeaderboard(
            Graphics g,
            int panelWidth,
            int panelHeight) {

        Graphics2D g2 = (Graphics2D) g;

        int boxW = 400;
        int boxH = 400;

        int boxX = (panelWidth - boxW) / 2;
        int boxY = 170;

        g2.setColor(new Color(20,30,40,240));

        g2.fillRoundRect(boxX, boxY, boxW, boxH, 25,25);

        g2.setColor(Color.CYAN);

        g2.drawRoundRect(boxX, boxY, boxW, boxH, 25,25);

        g2.setFont(new Font("SansSerif", Font.BOLD, 30));

        g2.drawString("LEADERBOARD", boxX + 70, boxY + 50);

        g2.setFont(new Font("SansSerif", Font.BOLD, 20));

        int y = boxY + 110;

        for (int i = 0; i < MAX_LEADERBOARD_SIZE; i++) {

            if (i < leaderboard.size()) {

                ScoreEntry e = leaderboard.get(i);

                g2.setColor(Color.WHITE);

                g2.drawString(
                        (i+1) + ". " + e.name,
                        boxX + 40,
                        y);

                g2.setColor(Color.YELLOW);

                g2.drawString(
                        String.valueOf(e.score),
                        boxX + 300,
                        y);
            }

            y += 50;
        }

        g2.setColor(Color.LIGHT_GRAY);

        g2.drawString("PRESS R TO RESTART",
                boxX + 70,
                boxY + 350);
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

        int[][] rotated =
                new int[shape[0].length][shape.length];

        for (int i = 0; i < shape.length; i++) {

            for (int j = 0; j < shape[0].length; j++) {

                rotated[j][shape.length - 1 - i]
                        = shape[i][j];
            }
        }

        int[][] old = shape;

        shape = rotated;

        if (collision(grid, x, y)) {

            if (!collision(grid, x - 1, y))
                x--;

            else if (!collision(grid, x + 1, y))
                x++;

            else
                shape = old;
        }
    }

    public boolean collision(Color[][] grid,
                             int nx,
                             int ny) {

        for (int i = 0; i < shape.length; i++) {

            for (int j = 0; j < shape[i].length; j++) {

                if (shape[i][j] != 0) {

                    int gx = nx + j;
                    int gy = ny + i;

                    if (gx < 0 || gx >= 10 || gy >= 20)
                        return true;

                    if (gy >= 0 &&
                            grid[gy][gx] != null)
                        return true;
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
                }
            }
        }
    }

    public void draw(Graphics g,
                     int size,
                     int offsetX,
                     int offsetY,
                     boolean updateParticles) {

        drawShape(g,
                x,
                y,
                size,
                offsetX,
                offsetY,
                color,
                255);
    }

    public void drawGhost(Graphics g,
                          int size,
                          int offsetX,
                          int offsetY,
                          Color[][] grid) {

        int ghostY = y;

        while (!collision(grid, x, ghostY + 1)) {
            ghostY++;
        }

        drawShape(g,
                x,
                ghostY,
                size,
                offsetX,
                offsetY,
                color,
                60);
    }

    private void drawShape(Graphics g,
                           int tx,
                           int ty,
                           int size,
                           int offsetX,
                           int offsetY,
                           Color c,
                           int alpha) {

        Graphics2D g2 = (Graphics2D) g;

        Color cBright = c.brighter();
        Color cDark = c.darker();
        
        Color fillBrighter = new Color(cBright.getRed(), cBright.getGreen(), cBright.getBlue(), alpha);
        Color fillDarker = new Color(cDark.getRed(), cDark.getGreen(), cDark.getBlue(), alpha);

        for (int i = 0; i < shape.length; i++) {

            for (int j = 0; j < shape[i].length; j++) {

                if (shape[i][j] != 0) {

                    int px = (tx + j) * size + offsetX;
                    int py = (ty + i) * size + offsetY;

                    GradientPaint gp =
                            new GradientPaint(
                                    px,
                                    py,
                                    fillBrighter,
                                    px + size,
                                    py + size,
                                    fillDarker);

                    g2.setPaint(gp);

                    g2.fillRoundRect(
                            px + 1,
                            py + 1,
                            size - 2,
                            size - 2,
                            8,
                            8);

                    g2.setColor(
                            new Color(255,255,255,alpha));

                    g2.drawRoundRect(
                            px,
                            py,
                            size - 1,
                            size - 1,
                            8,
                            8);
                }
            }
        }
    }

    public void drawPreviewCentered(
            Graphics g,
            int bx,
            int by,
            int bw,
            int bh,
            int size) {

        int offsetX =
                bx + (bw - shape[0].length * size) / 2;

        int offsetY =
                by + (bh - shape.length * size) / 2 + 5;

        Graphics2D g2 = (Graphics2D) g;

        for (int i = 0; i < shape.length; i++) {

            for (int j = 0; j < shape[i].length; j++) {

                if (shape[i][j] != 0) {

                    int px = offsetX + j * size;
                    int py = offsetY + i * size;

                    GradientPaint gp =
                            new GradientPaint(
                                    px,
                                    py,
                                    color.brighter(),
                                    px + size,
                                    py + size,
                                    color.darker());

                    g2.setPaint(gp);

                    g2.fillRoundRect(
                            px,
                            py,
                            size - 2,
                            size - 2,
                            6,
                            6);
                }
            }
        }
    }
}