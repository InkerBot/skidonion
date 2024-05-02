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
import java.util.ResourceBundle;

public class Main {

    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE, manualTryCatch = true)
    public static int showVerification() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(System.getProperty("user.home"), "skidonion", "." + Internals.verificationServer().hashCode(), "userinfo"))) {
            Inline.trycatch();
            Properties properties = new Properties();
            properties.load(reader);
            Inline.trycatch();
            String username = properties.getProperty("username");
            Inline.trycatch();
            String password = properties.getProperty("password");
            Inline.trycatch();
            if (username != null && password != null) {
                ResourceBundle bundle = ResourceBundle.getBundle("tech.skidonion.verification.lang");
                Inline.trycatch();
                System.out.println(bundle.getString("VerificationPanel.login.autologin"));
                int result = Wrapper.login(username, password);
                Inline.trycatch();
                if (result == 0) {
                    return 1;
                } else {
                    JOptionPane.showMessageDialog(null, bundle.getString("VerificationPanel.login.code." + result), "skidonion", JOptionPane.WARNING_MESSAGE);
                    Inline.trycatch();
                }
            }
        } catch (Exception ignore) {
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Inline.trycatch();
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
