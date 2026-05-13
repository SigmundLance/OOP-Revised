import java.awt.*;
import javax.swing.*;

public class LoginPanel extends JPanel {
    public LoginPanel(TetrixelLauncher launcher) {
        setPreferredSize(new Dimension(360, 640));
        setBackground(new Color(20, 20, 30));
        setLayout(new GridBagLayout());
        
        JButton startBtn = new JButton("START GAME");
        startBtn.addActionListener(e -> launcher.showGame());
        
        JLabel title = new JLabel("TETRIXEL");
        title.setForeground(Color.CYAN);
        title.setFont(new Font("Monospaced", Font.BOLD, 40));
        
        add(title);
        add(startBtn);
    }
}