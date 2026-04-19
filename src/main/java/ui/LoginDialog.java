package ui;

import javax.swing.*;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import java.awt.*;

public class LoginDialog extends JDialog
{
    private final JPasswordField tfMasterPass = new JPasswordField(20);
    private boolean success = false;
    private String password = "";

    public LoginDialog(Frame parent)
    {
        super(parent, "Unlock Database", true);
        this.setSize(340, 170);
        setLayout(new MigLayout("fillx, insets 20", "[][grow]", "[]20[]"));

        // Icon/Header
        JLabel lblHeader = new JLabel("Enter Master Password");
        lblHeader.setIcon(Resources.getIcon("close-padlock"));
        lblHeader.setFont(lblHeader.getFont().deriveFont(Font.BOLD, 16f));
        add(lblHeader, "span 2, center, wrap");

        add(new JLabel("Password:"));
        tfMasterPass.putClientProperty(FlatClientProperties.STYLE,
                "font: +2;" + "arc: 1;" + "focusWidth: 0;" + "showClearButton: true;" + "showRevealButton: true;");
        add(tfMasterPass, "growx, wrap");

        JButton btnUnlock = new JButton("Unlock");
        JButton btnCancel = new JButton("Cancel");

        btnUnlock.addActionListener(_ -> {
            password = new String(tfMasterPass.getPassword());

            if (password.trim().isEmpty())
            {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "Password cannot be empty.");
                return;
            }
            success = true;
            dispose();
        });

        btnCancel.addActionListener(_ -> {
            success = false;
            dispose();
        });

        getRootPane().setDefaultButton(btnUnlock);

        // Layout buttons
        add(btnCancel, "span 2, split 2, right");
        add(btnUnlock, "");

        pack();
        setLocationRelativeTo(parent);
    }

    public boolean isSuccess() { return success; }
    public String getPassword() { return password; }
}