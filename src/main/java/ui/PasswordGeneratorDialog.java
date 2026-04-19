package ui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import net.miginfocom.swing.MigLayout;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.security.SecureRandom;

public class PasswordGeneratorDialog extends JDialog
{
    private JTextField tfGeneratedPassword;
    private JProgressBar strengthBar;
    private JLabel lblStrengthText;
    private JSlider lengthSlider;
    private JLabel lblLength;

    private JCheckBox cbUpper, cbLower, cbNumbers, cbSymbols;

    private String finalPassword = null;
    private final SecureRandom random = new SecureRandom();

    public PasswordGeneratorDialog(Frame parent)
    {
        super(parent, "Password Generator", true);

        setLayout(new MigLayout("fillx, insets 20", "[grow][][]", "[]10[]15[]10[]10[]10[]20[]"));

        initComponents();
        setupListeners();

        generateNewPassword();

        pack();
        setMinimumSize(new Dimension(400, 350));
        setLocationRelativeTo(parent);
    }

    private void initComponents()
    {
        //  Row 1: Password Display & Regenerate Button 
        tfGeneratedPassword = new JTextField(20);
        tfGeneratedPassword.setEditable(false);
        tfGeneratedPassword.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        tfGeneratedPassword.setHorizontalAlignment(JTextField.CENTER);

        JButton btnRegenerate = new JButton(Resources.getIcon("refresh"));
        btnRegenerate.setToolTipText("Generate a new password");
        btnRegenerate.addActionListener(_ -> generateNewPassword());

        add(tfGeneratedPassword, "growx, span 2");
        add(btnRegenerate, "wrap");

        //  Row 2: Strength Indicator 
        strengthBar = new JProgressBar(0, 100);
        strengthBar.setStringPainted(false); // We'll use a custom label below it
        lblStrengthText = new JLabel("Strength: Good");
        lblStrengthText.setFont(lblStrengthText.getFont().deriveFont(Font.BOLD));

        add(strengthBar, "growx, span 3, wrap");
        add(lblStrengthText, "span 3, center, wrap");

        //  Row 3: Length Slider 
        lblLength = new JLabel("Length: 16");
        lengthSlider = new JSlider(8, 64, 16);
        lengthSlider.setMajorTickSpacing(8);
        lengthSlider.setPaintTicks(true);

        add(lblLength, "span 3, wrap");
        add(lengthSlider, "growx, span 3, wrap");

        //  Rows 4-7: Character Options 
        cbUpper = new JCheckBox("A-Z (Uppercase)", true);
        cbLower = new JCheckBox("a-z (Lowercase)", true);
        cbNumbers = new JCheckBox("0-9 (Numbers)", true);
        cbSymbols = new JCheckBox("!@#$ (Symbols)", true);

        add(cbUpper, "span 3, wrap");
        add(cbLower, "span 3, wrap");
        add(cbNumbers, "span 3, wrap");
        add(cbSymbols, "span 3, wrap");

        //  Row 8: Action Buttons 
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(_ -> dispose());

        JButton btnCopyUse = new JButton("Copy & Close");
        btnCopyUse.addActionListener(_ -> {
            finalPassword = tfGeneratedPassword.getText();
            
            StringSelection selection = new StringSelection(finalPassword);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            dispose();
        });

        // Right-align buttons
        add(btnCancel, "span 3, split 2, right");
        add(btnCopyUse, "");
    }

    private void setupListeners()
    {
        // Whenever the slider changes, update the label and regenerate
        lengthSlider.addChangeListener((ChangeEvent _) -> {
            lblLength.setText("Length: " + lengthSlider.getValue());
            generateNewPassword();
        });

        // Whenever a checkbox is toggled, regenerate
        var actionListener = (ActionListener) _ -> generateNewPassword();
        cbUpper.addActionListener(actionListener);
        cbLower.addActionListener(actionListener);
        cbNumbers.addActionListener(actionListener);
        cbSymbols.addActionListener(actionListener);
    }

    private void generateNewPassword()
    {
        int length = lengthSlider.getValue();
        StringBuilder pool = new StringBuilder();

        if (cbUpper.isSelected()) pool.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (cbLower.isSelected()) pool.append("abcdefghijklmnopqrstuvwxyz");
        if (cbNumbers.isSelected()) pool.append("0123456789");
        if (cbSymbols.isSelected()) pool.append("!@#$%^&*()-_=+[]{}|;:,.<>?");

        if (pool.isEmpty())
        {
            cbLower.setSelected(true);
            pool.append("abcdefghijklmnopqrstuvwxyz");
        }

        String charPool = pool.toString();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(charPool.length());
            password.append(charPool.charAt(randomIndex));
        }

        tfGeneratedPassword.setText(password.toString());
        calculateStrength(length);
    }

    private void calculateStrength(int length)
    {
        int score = 0;

        // 1. Length contribution (up to 60 points)
        if (length >= 8) score += 10;
        if (length >= 12) score += 20;
        if (length >= 16) score += 20;
        if (length >= 24) score += 10;

        // 2. Character variety contribution (up to 40 points)
        if (cbUpper.isSelected() && cbLower.isSelected()) score += 10;
        if (cbNumbers.isSelected()) score += 10;
        if (cbSymbols.isSelected()) score += 20;

        strengthBar.setValue(score);

        // 3. Visual feedback
        if (score < 40)
        {
            strengthBar.setForeground(new Color(220, 53, 69)); // Bootstrap Red
            lblStrengthText.setText("Strength: Weak");
            lblStrengthText.setForeground(new Color(220, 53, 69));
        }
        else if (score < 80)
        {
            strengthBar.setForeground(new Color(255, 193, 7)); // Bootstrap Yellow
            lblStrengthText.setText("Strength: Good");
            lblStrengthText.setForeground(new Color(200, 150, 0)); // Darker yellow for text readability
        }
        else
        {
            strengthBar.setForeground(new Color(40, 167, 69)); // Bootstrap Green
            lblStrengthText.setText("Strength: Excellent");
            lblStrengthText.setForeground(new Color(40, 167, 69));
        }
    }

    /**
     * Returns the generated password if the user clicked "Copy & Close", or null if they canceled.
     */
    public String getFinalPassword() {
        return finalPassword;
    }
}