package ui;

import global.DateUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class EntryDetailsPanel extends JPanel
{
    private final JLabel lblCreated = new JLabel("-");
    private final JLabel lblCategory = new JLabel("-");
    private final JTextArea taNotes = new JTextArea(5, 20);
    private final JTextField tfUrl = new JTextField();

    public EntryDetailsPanel()
    {
        setLayout(new MigLayout("fillx, insets 15", "[right][grow]", "[]15[]10[]10[][grow]"));
        setBorder(new TitledBorder("Entry Details"));
        setPreferredSize(new Dimension(300, 0));
        setFont(getFont().deriveFont(20f));

        taNotes.setLineWrap(true);
        taNotes.setWrapStyleWord(true);

        taNotes.setEditable(false);
        tfUrl.setEditable(false);

        add(new JLabel("Category:"));
        add(lblCategory, "growx, wrap");

        add(new JLabel("URL:"));
        add(tfUrl, "growx, wrap");

        add(new JLabel("Created:"));
        add(lblCreated, "growx, wrap");

        add(new JLabel("Notes:"), "top");
        add(new JScrollPane(taNotes), "grow, push, wrap");
    }

    public void updateDetails(String category, String url, String notes, String createdAt)
    {
        lblCategory.setText(category != null ? category : "Uncategorized");
        tfUrl.setText(url != null ? url : "");
        taNotes.setText(notes != null ? notes : "");

        // PROFESSIONAL DATE FORMATTING
        String prettyDate = DateUtils.getRelativeTime(createdAt);
        lblCreated.setText(prettyDate);

        lblCreated.setFont(lblCreated.getFont().deriveFont(Font.ITALIC, 17f));
    }

    public void clear()
    {
        lblCategory.setText("-");
        tfUrl.setText("");
        taNotes.setText("");
        lblCreated.setText("-");
    }
}