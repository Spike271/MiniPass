package ui;

import javax.swing.*;

import db.SecurityUtils;
import net.miginfocom.swing.MigLayout;
import java.awt.*;

public class AddEntryDialog extends JDialog
{
    private final JTextField tfTitle = new JTextField(20);
    private final JTextField tfUser = new JTextField(20);
    private final JPasswordField tfPass = new JPasswordField(20);
    private final JComboBox<String> cbCategory = new JComboBox<>(new String[]{"Personal", "Work", "Banking", "Email"});
    private boolean succeeded = false;

    public AddEntryDialog(Frame parent)
    {
        super(parent, "Add New Entry", true);
        setLayout(new MigLayout("fillx, insets 20", "[right][grow]", "[]10[]10[]10[]20[]"));

        add(new JLabel("Category:"));
        add(cbCategory, "growx, wrap");

        add(new JLabel("Title:"));
        add(tfTitle, "growx, wrap");

        add(new JLabel("Username:"));
        add(tfUser, "growx, wrap");

        add(new JLabel("Password:"));
        JPanel passPanel = new JPanel(new MigLayout("insets 0", "[grow][]", ""));
        JButton btnGen = new JButton(Resources.getIcon("dice"));
        btnGen.addActionListener(_ -> tfPass.setText(SecurityUtils.generateSecurePassword(8)));

        JToggleButton btnTogglePass = new JToggleButton(Resources.getIcon("eye"));
        btnTogglePass.setFocusable(false);
        btnTogglePass.addActionListener(_ -> {

            if (btnTogglePass.isSelected()) {
                tfPass.setEchoChar((char) 0);
            } else {
                tfPass.setEchoChar('•');
            }
        });

        passPanel.add(tfPass, "growx");
        passPanel.add(btnTogglePass);
        passPanel.add(btnGen);
        add(passPanel, "growx, wrap");

        JButton btnSave = getJButton();
        add(btnSave, "span 2, center");

        pack();
        setLocationRelativeTo(parent);
    }

    private JButton getJButton()
    {
        JButton btnSave = new JButton("Save Entry");
        btnSave.addActionListener(_ -> {
            if (!getTitleText().isBlank() &&
                !getUsername().isBlank() &&
                !getPassword().isBlank())
            {
                succeeded = true;
                dispose();
            }
            else
            {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "Make sure to fill all the entries.");
            }
        });
        return btnSave;
    }

    public boolean isSucceeded() { return succeeded; }
    public String getTitleText() { return tfTitle.getText(); }
    public String getUsername() { return tfUser.getText(); }
    public String getPassword() { return new String(tfPass.getPassword()); }
    public int getCategoryId() { return cbCategory.getSelectedIndex() + 1; }
}