/*
 * Created by JFormDesigner on Fri Mar 15 12:44:03 CST 2024
 */

package tech.skidonion.verification;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;
import tech.skidonion.obfuscator.inline.Wrapper;
import tech.skidonion.verification.utils.Internals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


/**
 * @author ImFl0wow
 */
public class VerificationPanel extends JPanel {
    private final ResourceBundle bundle;
    private final JFrame frame;
    private PipedInputStream input;
    private PipedOutputStream output;
    private static final ExecutorService service = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    public VerificationPanel(JFrame frame) {
        this.frame = frame;
        this.bundle = ResourceBundle.getBundle("tech.skidonion.verification.lang");
        initComponents();
    }


    public int callback() {
        try {
            this.input = new PipedInputStream();
            this.output = new PipedOutputStream();
            this.input.connect(this.output);
            return input.read();
        } catch (Exception ignore) {
            return 0;
        }
    }

    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_RED)
    private void login(ActionEvent e) {
        if (this.loginButton.isEnabled()) {
            this.loginButton.setEnabled(false);
            service.submit(this::loginThread);
        }
    }

    private void loginThread() {
        try {
            byte result = (byte) (Wrapper.login(this.usernameField.getText(), new String(this.passwordField.getPassword())) >> 8 & 0xFF);
            if (result == 0) {
                try {
                    Path dataPath = Paths.get(System.getProperty("user.home"), "skidonion", "." + Internals.verificationServer().hashCode());
                    Files.createDirectories(dataPath);
                    try (BufferedWriter writer = Files.newBufferedWriter(dataPath.resolve("userinfo"))) {
                        Properties properties = new Properties();
                        properties.setProperty("username", this.usernameField.getText());
                        properties.setProperty("password", new String(this.passwordField.getPassword()));
                        properties.store(writer, "don't leak to anyone^^");
                    }
                } catch (Exception ignore) {
                }

                this.output.write(1);
                SwingUtilities.invokeLater(frame::dispose);
            } else {
                JOptionPane.showMessageDialog(this, bundle.getString("VerificationPanel.login.code." + result), "skidonion", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, this.bundle.getString("VerificationPanel.login.exception"));
        }
        this.loginButton.setEnabled(true);
    }

    private void termsLabelMouseClicked(MouseEvent e) {
        try {
            Desktop.getDesktop().browse(URI.create("https://skidonion.tech/resources/end-user-license-agreement.html"));
        } catch (Exception ignore) {
        }
    }

    private void termsLabelMouseEntered(MouseEvent e) {
        termsLabel.setForeground(new Color(0x4141FF));
    }

    private void termsLabelMouseExited(MouseEvent e) {
        termsLabel.setForeground(Color.black);
    }

    private void privacyLabelMouseClicked(MouseEvent e) {
        try {
            Desktop.getDesktop().browse(URI.create("https://skidonion.tech/resources/privacy-policy.html"));
        } catch (Exception ignore) {
        }
    }

    private void privacyLabelMouseEntered(MouseEvent e) {
        privacyLabel.setForeground(new Color(0x4141FF));
    }

    private void privacyLabelMouseExited(MouseEvent e) {
        privacyLabel.setForeground(Color.black);
    }

    private void registerLabelMouseClicked(MouseEvent e) {
        try {
            Desktop.getDesktop().browse(URI.create(Internals.verificationServer() + "#/register"));
        } catch (Exception ignore) {
        }
    }

    private void registerLabelMouseEntered(MouseEvent e) {
        registerLabel.setForeground(new Color(0x4141FF));
    }

    private void registerLabelMouseExited(MouseEvent e) {
        registerLabel.setForeground(Color.black);
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        ResourceBundle bundle = ResourceBundle.getBundle("tech.skidonion.verification.lang");
        usernameLabel = new JLabel();
        usernameField = new JTextField();
        passwordLabel = new JLabel();
        passwordField = new JPasswordField();
        loginButton = new JButton();
        registerLabel = new JLabel();
        termsLabel = new JLabel();
        privacyLabel = new JLabel();

        //======== this ========
        setFont(new Font("Microsoft YaHei", Font.PLAIN, 20));
        setLayout(new GridBagLayout());
        ((GridBagLayout)getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        ((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        ((GridBagLayout)getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0E-4};
        ((GridBagLayout)getLayout()).rowWeights = new double[] {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0E-4};

        //---- usernameLabel ----
        usernameLabel.setText(bundle.getString("VerificationPanel.usernameLabel.text"));
        usernameLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 18));
        usernameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        add(usernameLabel, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- usernameField ----
        usernameField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 18));
        add(usernameField, new GridBagConstraints(4, 3, 5, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- passwordLabel ----
        passwordLabel.setText(bundle.getString("VerificationPanel.passwordLabel.text"));
        passwordLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 18));
        passwordLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        add(passwordLabel, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- passwordField ----
        passwordField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 18));
        add(passwordField, new GridBagConstraints(4, 6, 5, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- loginButton ----
        loginButton.setText(bundle.getString("VerificationPanel.loginButton.text"));
        loginButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 20));
        loginButton.addActionListener(e -> login(e));
        add(loginButton, new GridBagConstraints(5, 9, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- registerLabel ----
        registerLabel.setText(bundle.getString("VerificationPanel.registerLabel.text"));
        registerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        registerLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 18));
        registerLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                registerLabelMouseClicked(e);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                registerLabelMouseEntered(e);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                registerLabelMouseExited(e);
            }
        });
        add(registerLabel, new GridBagConstraints(7, 10, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- termsLabel ----
        termsLabel.setText(bundle.getString("VerificationPanel.termsLabel.text"));
        termsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        termsLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 20));
        termsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                termsLabelMouseClicked(e);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                termsLabelMouseEntered(e);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                termsLabelMouseExited(e);
            }
        });
        add(termsLabel, new GridBagConstraints(4, 12, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 5), 0, 0));

        //---- privacyLabel ----
        privacyLabel.setText(bundle.getString("VerificationPanel.privacyLabel.text"));
        privacyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        privacyLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 20));
        privacyLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                privacyLabelMouseClicked(e);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                privacyLabelMouseEntered(e);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                privacyLabelMouseExited(e);
            }
        });
        add(privacyLabel, new GridBagConstraints(6, 12, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 5), 0, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel usernameLabel;
    private JTextField usernameField;
    private JLabel passwordLabel;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel registerLabel;
    private JLabel termsLabel;
    private JLabel privacyLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
