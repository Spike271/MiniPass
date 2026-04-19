package ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.IntelliJTheme;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import db.DatabaseManager;
import db.SecurityUtils;
import global.ApplicationUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.formdev.flatlaf.FlatLightLaf;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.sql.*;
import java.util.prefs.Preferences;

public class MainWindow extends JFrame
{
    private JTable entryTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private static boolean isDarkMode = false;
    private String currentMasterPassword = null;

    public MainWindow()
    {
        setTitle("MiniPass");
        setIconImage(new ImageIcon(ApplicationUtils.jarFilePath + "res/icons/logo.png").getImage());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 550);
        setLocationRelativeTo(null);

        setLayout(new MigLayout("fill", "[grow]", "[][grow][]"));

        initUI();
        if (!lockApplication()) System.exit(0);
    }

    /**
     * Locks the app, clears memory, and demands the password.
     */
    private boolean lockApplication()
    {
        currentMasterPassword = null;
        tableModel.setRowCount(0);

        statusLabel.setText("Database is locked.");

        // 3. Show Login Dialog
        LoginDialog login = new LoginDialog(this);
        login.setVisible(true);

        if (login.isSuccess()) {
            tryUnlock(login.getPassword());
            return true;
        }
        return false;
    }

    /**
     * Attempts to open the database with the provided password.
     */
    private void tryUnlock(String attemptedPassword)
    {
        DatabaseManager db = new DatabaseManager();

        try (Connection _ = db.connect(attemptedPassword))
        {
            db.initializeDatabase(attemptedPassword);
            this.currentMasterPassword = attemptedPassword;

            this.setVisible(true);
            refreshTableData();
        }
        catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Incorrect Master Password!", "Access Denied", JOptionPane.ERROR_MESSAGE);
            lockApplication();
        }
    }

    private void initUI()
    {
        JPanel toolbar = getJPanel();
        add(toolbar, "growx, wrap");

        EntryDetailsPanel detailsPanel = new EntryDetailsPanel();

        String[] columns = {"ID", "Title", "Username", "URL", "Password", "Category", "Notes", "Created"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        entryTable = new JTable(tableModel);
        entryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entryTable.setAutoCreateRowSorter(true);
        entryTable.setRowHeight(30);
        entryTable.setShowVerticalLines(false);
        int[] hiddenIndices = {7, 6, 0}; // ID, Notes, Created
        for (int index : hiddenIndices)
            entryTable.removeColumn(entryTable.getColumnModel().getColumn(index));

        entryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
            {
                int selectedRow = entryTable.getSelectedRow();
                if (selectedRow != -1)
                {
                    int modelRow = entryTable.convertRowIndexToModel(selectedRow);

                    // Extract all data from the row
                    String url      = (String) tableModel.getValueAt(modelRow, 3);
                    String category = (String) tableModel.getValueAt(modelRow, 5);
                    String notes    = (String) tableModel.getValueAt(modelRow, 6);
                    String created  = (String) tableModel.getValueAt(modelRow, 7);

                    // Pass the ID along with the rest of the data
                    detailsPanel.updateDetails(category, url, notes, created);
                }
                else {
                    detailsPanel.clear();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(entryTable);

        // 2. Create a SplitPane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, detailsPanel);
        splitPane.putClientProperty(FlatClientProperties.STYLE, "style: plain;");
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.5);

        // 3. Add the splitPane to your main frame instead of the tableScroll
        add(splitPane, "grow, push, wrap");

        setupTableContextMenu();

        statusLabel = new JLabel("Database unlocked. 2 entries.");
        statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        add(statusLabel, "growx");
    }

    private JPanel getJPanel()
    {
        JPanel toolbar = new JPanel(new MigLayout("insets 0", "[][][][][]push[][]"));

        JButton btnAdd = addBtn();
        JButton btnEdit = editBtn();
        JButton btnRemove = revBtn();
        JButton btnGenerate = genBtn();

        JButton btnTheme = isDarkMode ? new JButton("Light Mode", Resources.getIcon("sun")) :
                                           new JButton("Dark Mode", Resources.getIcon("moon"));
        btnTheme.putClientProperty(FlatClientProperties.STYLE, "focusWidth: 0;");
        btnTheme.addActionListener(_ -> toggleTheme(btnTheme));

        JButton btnLock = new JButton("Lock Database", Resources.getIcon("close-padlock"));
        btnLock.addActionListener(_ -> {
            if (btnLock.getText().contains("Lock"))
            {
                btnAdd.setEnabled(false);
                btnRemove.setEnabled(false);

                tableModel.setRowCount(0);
                btnLock.setText("Unlock");
                btnLock.setIcon(Resources.getIcon("open-padlock"));
                statusLabel.setText("Database Locked.");
            }
            else
            {
                if (lockApplication())
                {
                    btnAdd.setEnabled(true);
                    btnRemove.setEnabled(true);

                    btnLock.setText("Lock");
                    btnLock.setIcon(Resources.getIcon("close-padlock"));
                }
            }
        });

        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnRemove);
        toolbar.add(btnGenerate);
        toolbar.add(btnTheme);
        toolbar.add(btnLock);
        return toolbar;
    }

    private void editSelectedEntry()
    {
        int selectedRow = entryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an entry to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = entryTable.convertRowIndexToModel(selectedRow);
        int dbId = (int) tableModel.getValueAt(modelRow, 0);

        // Fetch current data from the table model
        String currentTitle = (String) tableModel.getValueAt(modelRow, 1);
        String currentUser = (String) tableModel.getValueAt(modelRow, 2);
        String currentUrl = (String) tableModel.getValueAt(modelRow, 3);
        String currentNotes = (String) tableModel.getValueAt(modelRow, 6);

        // Fetch and decrypt the real password from the database
        String realPassword = fetchAndDecryptPassword(dbId);
        if (realPassword == null) return; // Error fetching password

        // Build the UI for the Dialog
        JTextField tfTitle = new JTextField(currentTitle, 20);
        JTextField tfUser = new JTextField(currentUser, 20);
        JPasswordField pfPass = new JPasswordField(realPassword, 20);
        JTextField tfUrl = new JTextField(currentUrl, 20);
        JTextArea taNotes = new JTextArea(currentNotes, 3, 20);

        JPanel panel = new JPanel(new MigLayout("fillx", "[right][grow]"));
        panel.add(new JLabel("Title:")); panel.add(tfTitle, "growx, wrap");
        panel.add(new JLabel("Username:")); panel.add(tfUser, "growx, wrap");
        panel.add(new JLabel("Password:")); panel.add(pfPass, "growx, wrap");
        panel.add(new JLabel("URL:")); panel.add(tfUrl, "growx, wrap");
        panel.add(new JLabel("Notes:"), "top"); panel.add(new JScrollPane(taNotes), "growx, wrap");

        // Show the dialog
        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Entry", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION)
        {
            String newTitle = tfTitle.getText().trim();
            String newUser = tfUser.getText().trim();
            String newPass = new String(pfPass.getPassword());
            String newUrl = tfUrl.getText().trim();
            String newNotes = taNotes.getText().trim();

            if (newTitle.isEmpty() || newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title and Password cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Re-encrypt the potentially new password
            String newlyEncryptedPass = SecurityUtils.encrypt(newPass, currentMasterPassword);

            // Update the database
            DatabaseManager db = new DatabaseManager();
            String sql = "UPDATE entries SET title = ?, username = ?, password = ?, url = ?, notes = ? WHERE id = ?";

            try (Connection conn = db.connect(currentMasterPassword);
                 PreparedStatement pstmt = conn.prepareStatement(sql))
            {

                pstmt.setString(1, newTitle);
                pstmt.setString(2, newUser);
                pstmt.setString(3, newlyEncryptedPass);
                pstmt.setString(4, newUrl);
                pstmt.setString(5, newNotes);
                pstmt.setInt(6, dbId);

                pstmt.executeUpdate();

                // Refresh UI
                refreshTableData();
                statusLabel.setText("Entry '" + newTitle + "' updated successfully.");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error updating database.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton addBtn()
    {
        var btnAdd =  new JButton("Add", Resources.getIcon("add"));
        btnAdd.putClientProperty(FlatClientProperties.STYLE, "focusWidth: 0;");
        btnAdd.addActionListener(_ -> {
            AddEntryDialog dialog = new AddEntryDialog(this);
            dialog.setVisible(true);
            dialog.repaint();

            if (dialog.isSucceeded())
            {
                try {
                    // 1. Encrypt the password before it touches the DB
                    String encryptedPass = SecurityUtils.encrypt(dialog.getPassword(), currentMasterPassword);

                    // 2. Save to Database
                    DatabaseManager db = new DatabaseManager();
                    try (Connection conn = db.connect(currentMasterPassword);
                         PreparedStatement pstmt = conn.prepareStatement(
                                 "INSERT INTO entries (category_id, title, username, password) VALUES (?, ?, ?, ?)")) {

                        pstmt.setInt(1, dialog.getCategoryId());
                        pstmt.setString(2, dialog.getTitleText());
                        pstmt.setString(3, dialog.getUsername());
                        pstmt.setString(4, encryptedPass);
                        pstmt.executeUpdate();

                        // 3. Update Table UI
                        tableModel.addRow(new Object[]{
                                dialog.getTitleText(),
                                dialog.getUsername(),
                                "Added Successfully",
                                "********" // Keep hidden in UI
                        });

                        refreshTableData();
                        statusLabel.setText("Entry saved securely.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error saving: " + ex.getMessage());
                }
            }
        });
        return btnAdd;
    }

    private JButton revBtn()
    {
        var btnRemove = new JButton("Remove", Resources.getIcon("cross", 13));
        btnRemove.putClientProperty(FlatClientProperties.STYLE, "focusWidth: 0;");
        btnRemove.addActionListener(_ -> {
            int selectedRow = entryTable.getSelectedRow();

            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select an entry to remove.");
                return;
            }

            int modelRow = entryTable.convertRowIndexToModel(selectedRow);
            Object id = tableModel.getValueAt(modelRow, 0);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete this entry?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION)
            {
                DatabaseManager db = new DatabaseManager();
                try (Connection conn = db.connect(currentMasterPassword);
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM entries WHERE id = ?")) {

                    pstmt.setInt(1, (Integer) id);
                    pstmt.executeUpdate();

                    tableModel.removeRow(modelRow);
                    statusLabel.setText("Entry removed successfully.");
                } catch (SQLException _) {}
            }
        });
        return btnRemove;
    }

    private JButton editBtn()
    {
        var btnEdit = new JButton("Edit", Resources.getIcon("pen"));
        btnEdit.putClientProperty(FlatClientProperties.STYLE, "focusWidth: 0;");
        btnEdit.addActionListener(_ -> editSelectedEntry());
        return btnEdit;
    }

    private JButton genBtn()
    {
        var btnGenerate = new JButton("Generate", Resources.getIcon("key"));
        btnGenerate.putClientProperty(FlatClientProperties.STYLE, "focusWidth: 0;");
        btnGenerate.addActionListener(_ -> {
            PasswordGeneratorDialog dialog = new PasswordGeneratorDialog(this);
            dialog.setVisible(true);

            String newPass = dialog.getFinalPassword();

            if (newPass != null && !newPass.isEmpty())
            {
                statusLabel.setText("New password generated and copied to clipboard!");
            }
        });
        return btnGenerate;
    }

    private void toggleTheme(JButton themeButton)
    {
        Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);
        boolean currentlyDark = UIManager.getLookAndFeel() instanceof IntelliJTheme.ThemeLaf;

        if (!currentlyDark)
        {
            themeButton.setText("Light Mode");
            themeButton.setIcon(Resources.getIcon("sun"));

            FlatAnimatedLafChange.showSnapshot();
            FlatXcodeDarkIJTheme.setup();
            prefs.putBoolean("isDarkMode", true);
        }
        else
        {
            themeButton.setText("Dark Mode");
            themeButton.setIcon(Resources.getIcon("moon"));

            FlatAnimatedLafChange.showSnapshot();
            FlatLightLaf.setup();
            prefs.putBoolean("isDarkMode", false);
        }

        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
        SwingUtilities.updateComponentTreeUI(this);
    }

    protected void refreshTableData()
    {
        tableModel.setRowCount(0);

        if (currentMasterPassword == null) return;

        tableModel.setRowCount(0);
        DatabaseManager dbManager = new DatabaseManager();
        String sql = """
            SELECT e.id, e.title, e.username, e.url, e.password,
                   c.name AS category_name, e.notes, e.created_at
            FROM entries e
            LEFT JOIN categories c ON e.category_id = c.id
           """;

        try (Connection conn = dbManager.connect(currentMasterPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql))
        {

            int count = 0;
            while (rs.next())
            {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String user = rs.getString("username");
                String url = rs.getString("url");

                String category = rs.getString("category_name");
                String notes = rs.getString("notes");
                String created = rs.getString("created_at");

                String displayPass = "********";

                tableModel.addRow(new Object[]{id, title, user, url, displayPass, category, notes, created});
                count++;
            }

            statusLabel.setText("Database loaded. Total entries: " + count);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * securely fetches and decrypts the password for a specific database ID.
     */
    private String fetchAndDecryptPassword(int dbId)
    {
        if (currentMasterPassword == null) return null;

        DatabaseManager db = new DatabaseManager();
        String decryptedPass = null;

        try (Connection conn = db.connect(currentMasterPassword);
             PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM entries WHERE id = ?"))
        {
            pstmt.setInt(1, dbId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String encrypted = rs.getString("password");
                decryptedPass = SecurityUtils.decrypt(encrypted, currentMasterPassword);
            }
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error retrieving password.", "Security Error", JOptionPane.ERROR_MESSAGE);
        }
        return decryptedPass;
    }

    private void setupTableContextMenu()
    {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem editItem = new JMenuItem("Edit Entry", Resources.getIcon("pen"));
        JMenuItem copyUserItem = new JMenuItem("Copy Username", Resources.getIcon("clipboard"));
        JMenuItem copyPassItem = new JMenuItem("Copy Password", Resources.getIcon("clipboard"));
        JMenuItem togglePassItem = new JMenuItem("Show/Hide Password", Resources.getIcon("eye"));

        // Action: Edit Entries
        editItem.addActionListener(_ -> editSelectedEntry());

        // Action: Copy Username
        copyUserItem.addActionListener(_ -> {
            int row = entryTable.getSelectedRow();

            if (row != -1)
            {
                int modelRow = entryTable.convertRowIndexToModel(row);
                String username = (String) tableModel.getValueAt(modelRow, 2);

                StringSelection selection = new StringSelection(username);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                statusLabel.setText("Username copied to clipboard!");
            }
        });

        // Action: Copy Password
        copyPassItem.addActionListener(_ -> {
            int row = entryTable.getSelectedRow();
            if (row != -1)
            {
                int modelRow = entryTable.convertRowIndexToModel(row);
                int dbId = (int) tableModel.getValueAt(modelRow, 0);

                try
                {
                    String realPassword = fetchAndDecryptPassword(dbId);

                    if (realPassword != null)
                    {
                        StringSelection selection = new StringSelection(realPassword);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                        statusLabel.setText("Password securely copied to clipboard!");
                    }
                    else {
                        statusLabel.setText("Failed to retrieve password!");
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error accessing password!");
                }
            }
        });

        // Action: Show/Hide Password
        togglePassItem.addActionListener(_ -> {
            int row = entryTable.getSelectedRow();
            if (row != -1)
            {
                int modelRow = entryTable.convertRowIndexToModel(row);
                int passColumnIndex = 4;

                String currentDisplay = (String) tableModel.getValueAt(modelRow, passColumnIndex);

                if (currentDisplay.equals("********"))
                {
                    int dbId = (int) tableModel.getValueAt(modelRow, 0);
                    String realPassword = fetchAndDecryptPassword(dbId);

                    if (realPassword != null) {
                        tableModel.setValueAt(realPassword, modelRow, passColumnIndex);
                    }
                } else {
                    tableModel.setValueAt("********", modelRow, passColumnIndex);
                }
            }
        });

        popupMenu.add(editItem);
        popupMenu.add(copyUserItem);
        popupMenu.add(copyPassItem);
        popupMenu.add(togglePassItem);

        entryTable.setComponentPopupMenu(popupMenu);
    }

    public static void Run()
    {
        Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);
        isDarkMode = prefs.getBoolean("isDarkMode", true);

        FlatLaf.registerCustomDefaultsSource("res.themes");
        FlatRobotoFont.install();

        if (isDarkMode) {
            FlatXcodeDarkIJTheme.setup();
        } else {
            FlatLightLaf.setup();
        }

        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 14));
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}