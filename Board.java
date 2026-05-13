import java.awt.*;

public class Board {
    int width = 10, height = 20, size = 28;
    Color[][] grid = new Color[height][width];
    Tetromino current, next;
    int score = 0;
    static int highScore = 0;
    boolean gameOver = false;
    int dropCounter = 0, dropSpeed = 35;

    // Effects
    int[] clearLinesY = new int[4];
    int clearLinesCount = 0, clearEffectTimer = 0;

    public Board() { 
        reset(); 
    }

    public final void reset() {
        grid = new Color[height][width];
        current = new Tetromino();
        next = new Tetromino();
        score = 0;
        gameOver = false;
    }

    public void update() {
        if (gameOver) return;

        if (clearEffectTimer > 0) {
            if (--clearEffectTimer == 0) removeClearedLines();
            return;
        }

        if (++dropCounter >= dropSpeed) {
            dropCounter = 0;

            if (!current.moveDown(grid)) {
                current.lock(grid);

                int lines = checkLines();

                if (lines > 0) triggerClearEffect(lines);
                else spawnNextPiece();
            }
        }
    }

    private int checkLines() {
        int count = 0;

        for (int i = height - 1; i >= 0; i--) {

            boolean full = true;

            for (int j = 0; j < width; j++) {
                if (grid[i][j] == null) full = false;
            }

            if (full) clearLinesY[count++] = i;
        }

        return count;
    }

    private void triggerClearEffect(int lines) {
        clearLinesCount = lines;
        clearEffectTimer = 10;
    }

    private void removeClearedLines() {

        for (int i = 0; i < clearLinesCount; i++) {

            for (int k = clearLinesY[i]; k > 0; k--) {
                grid[k] = grid[k - 1].clone();
            }

            grid[0] = new Color[width];
        }

        score += clearLinesCount * 100;

        if (score > highScore) {
            highScore = score;
        }

        spawnNextPiece();
    }

    private void spawnNextPiece() {
        current = next;
        next = new Tetromino();

        current.resetPosition();

        if (current.collision(grid, current.x, current.y)) {
            gameOver = true;
        }
    }

    public void move(int dir) {
        current.moveSide(grid, dir);
    }

    public void rotate() {
        current.rotate(grid);
    }

    public void hardDrop() {

        while(current.moveDown(grid)) {

        }

        update();
    }

    public void draw(Graphics g) {

        int offsetX = (360 - width * size) / 2;
        int offsetY = 120;

        drawAnimatedBackground(g);

        drawBackground(g, offsetX, offsetY);

        drawNeonBorder(g, offsetX, offsetY);

        drawGrid(g, offsetX, offsetY);

        for (int i = 0; i < height; i++) {

            for (int j = 0; j < width; j++) {

                if (grid[i][j] != null) {

                    Graphics2D g2 = (Graphics2D) g;

                    GradientPaint gp = new GradientPaint(
                        j * size + offsetX,
                        i * size + offsetY,
                        grid[i][j].brighter(),
                        j * size + offsetX + size,
                        i * size + offsetY + size,
                        grid[i][j].darker()
                    );

                    g2.setPaint(gp);

                    g2.fillRoundRect(
                        j * size + offsetX,
                        i * size + offsetY,
                        size - 2,
                        size - 2,
                        8,
                        8
                    );

                    g2.setColor(Color.WHITE);

                    g2.drawRoundRect(
                        j * size + offsetX,
                        i * size + offsetY,
                        size - 2,
                        size - 2,
                        8,
                        8
                    );
                }
            }
        }

        if (!gameOver) {

            current.drawGhost(g, size, offsetX, offsetY, grid);

            current.draw(g, size, offsetX, offsetY);
        }

        drawScoreBox(g);

        drawNextBox(g);

        next.drawPreviewCentered(g, 260, 130, 80, 80, size / 2);

        if (gameOver) {

            Graphics2D g2 = (Graphics2D) g;

            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, 360, 700);

            g2.setFont(new Font("Arial", Font.BOLD, 32));
            g2.setColor(Color.RED);
            g2.drawString("GAME OVER", 70, 300);

            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.setColor(Color.WHITE);
            g2.drawString("PRESS R TO RESTART", 75, 340);
        }
    }

    private void drawBackground(Graphics g, int ox, int oy) {

        Graphics2D g2 = (Graphics2D) g;

        GradientPaint bg = new GradientPaint(
            0,
            oy,
            new Color(20, 20, 30),
            0,
            oy + height * size,
            new Color(10, 10, 15)
        );

        g2.setPaint(bg);

        g2.fillRoundRect(
            ox,
            oy,
            width * size,
            height * size,
            20,
            20
        );

        g2.setColor(Color.GRAY);

        g2.fillRect(
            ox - 10,
            oy - 40,
            width * size + 20,
            40
        );
    }

    private void drawGrid(Graphics g, int ox, int oy) {

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(255, 255, 255, 30));

        for (int i = 0; i <= width; i++) {

            g2.drawLine(
                ox + i * size,
                oy,
                ox + i * size,
                oy + height * size
            );
        }

        for (int i = 0; i <= height; i++) {

            g2.drawLine(
                ox,
                oy + i * size,
                ox + width * size,
                oy + i * size
            );
        }
    }

    private void drawNeonBorder(Graphics g, int ox, int oy) {

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(0, 255, 255, 60));

        g2.fillRoundRect(
            ox - 8,
            oy - 8,
            width * size + 16,
            height * size + 16,
            20,
            20
        );

        g2.setColor(Color.CYAN);

        g2.drawRoundRect(
            ox - 4,
            oy - 4,
            width * size + 8,
            height * size + 8,
            20,
            20
        );
    }

    private void drawAnimatedBackground(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        long time = System.currentTimeMillis();

        for (int i = 0; i < 25; i++) {

            int x = (int)((time / 10 + i * 70) % 360);

            int y = (
                i * 45 +
                (int)(Math.sin(time * 0.002 + i) * 20)
            ) % 700;

            g2.setColor(new Color(255, 255, 255, 25));

            g2.fillOval(x, y, 6, 6);
        }
    }

    private void drawScoreBox(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(20, 20, 20, 220));

        g2.fillRoundRect(245, 10, 100, 60, 15, 15);

        g2.setColor(Color.CYAN);

        g2.drawRoundRect(245, 10, 100, 60, 15, 15);

        g2.setFont(new Font("Arial", Font.BOLD, 16));

        g2.setColor(Color.WHITE);

        g2.drawString("SCORE", 268, 28);

        g2.setColor(Color.YELLOW);

        g2.drawString(String.valueOf(score), 278, 52);
    }

    private void drawNextBox(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(20, 20, 20, 220));

        g2.fillRoundRect(250, 100, 100, 110, 20, 20);

        g2.setColor(Color.CYAN);

        g2.drawRoundRect(250, 100, 100, 110, 20, 20);

        g2.setFont(new Font("Arial", Font.BOLD, 16));

        g2.setColor(Color.WHITE);

        g2.drawString("NEXT", 278, 120);
    }
}