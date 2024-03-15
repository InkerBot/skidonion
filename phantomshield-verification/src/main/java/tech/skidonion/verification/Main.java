package tech.skidonion.verification;

import javax.swing.*;
import java.net.URL;

public class Main {
    public static int showVerification() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
        }
        JFrame jFrame = new JFrame();
        VerificationPanel panel = new VerificationPanel(jFrame);
        URL imageURL = Main.class.getResource("/tech/skidonion/verification/skidonion.png");
        if (imageURL != null) {
            jFrame.setIconImage(new ImageIcon(imageURL).getImage());
        }
        jFrame.setTitle("skidonion");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setContentPane(panel);
        jFrame.setSize(600, 400);
        jFrame.setLocationRelativeTo(jFrame.getParent());
        jFrame.setVisible(true);

        return panel.callback();
    }

}
