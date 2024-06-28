package tech.skidonion.verification;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;
import tech.skidonion.obfuscator.inline.Inline;
import tech.skidonion.obfuscator.inline.Wrapper;
import tech.skidonion.verification.utils.Internals;
import tech.skidonion.verification.utils.VerifyUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;

public class Main {

    @NativeObfuscation
    public static int showVerification() {
        Random rand = new Random(Internals.softwareId() * 1337 + Internals.softwareId());
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder directory = new StringBuilder(".");
        for (int i = 0; i < 16; i++) {
            int number = rand.nextInt(str.length());
            directory.append(str.charAt(number));
        }
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(System.getProperty("user.home"), "skidonion", directory.toString()))) {
            Properties properties = new Properties();
            properties.load(reader);
            String username = properties.getProperty("username");
            String password = properties.getProperty("password");
            if (username != null && password != null) {
                ResourceBundle bundle = ResourceBundle.getBundle("tech.skidonion.verification.lang");
                System.out.println(bundle.getString("VerificationPanel.login.autologin"));
                int result = Wrapper.login(username, password, true);
                if (result == 0) {
                    return 1;
                } else {
                    JOptionPane.showMessageDialog(null, bundle.getString("VerificationPanel.login.code." + result), "skidonion", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (Exception ignore) {
        }
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
