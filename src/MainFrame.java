import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Swing GUI for the Lost and Found Management System.
 * A JTabbedPane hosts five tabs, one per functional module, all backed by the
 * same DataStore / MatchingEngine classes used by the original console version.
 */
public class MainFrame extends JFrame {
    private static final String[] CATEGORIES = {
            "Electronics", "Bag", "Wallet", "ID Card", "Book", "Keys", "Clothing", "Documents", "Other"
    };
    private static final String[] RECORD_COLUMNS = {
            "ID", "Name", "Category", "Color", "Date", "Location", "Status", "Reported By", "Contact"
    };
    private static final String[] MATCH_COLUMNS = {
            "Score", "Found ID", "Name", "Category", "Color", "Date", "Location"
    };

    private final DataStore store;
    private final JLabel statusBar = new JLabel(" Ready");
    private final DefaultTableModel lostModel = new DefaultTableModel(RECORD_COLUMNS, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel foundModel = new DefaultTableModel(RECORD_COLUMNS, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel claimModel = new DefaultTableModel(
            new String[]{"Claim ID", "Lost ID", "Found ID", "Claimant", "Date", "Verified"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel searchResultModel = new DefaultTableModel(RECORD_COLUMNS, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel matchResultModel = new DefaultTableModel(MATCH_COLUMNS, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    public MainFrame(DataStore store) {
        super("Campus Lost and Found Management System");
        this.store = store;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                store.saveAll();
                dispose();
                System.exit(0);
            }
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Report Lost Item", buildLostRegistrationPanel());
        tabs.addTab("Report Found Item", buildFoundRegistrationPanel());
        tabs.addTab("Search & Match", buildSearchPanel());
        tabs.addTab("Claim & Verify", buildClaimPanel());
        tabs.addTab("Records", buildRecordsPanel());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 4) refreshRecordTables();
        });

        statusBar.setBorder(new EmptyBorder(6, 10, 6, 10));
        statusBar.setForeground(new Color(40, 100, 40));

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        setSize(980, 640);
        setMinimumSize(new Dimension(780, 520));
        setLocationRelativeTo(null);

        refreshRecordTables();
    }

    // ================= Module 1: Lost Item Registration =================
    private JPanel buildLostRegistrationPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = formConstraints();

        JTextField name = new JTextField(22);
        JComboBox<String> category = new JComboBox<>(CATEGORIES);
        category.setEditable(true);
        JTextField color = new JTextField(22);
        JTextField date = new JTextField(LocalDate.now().toString(), 22);
        JTextField location = new JTextField(22);
        JTextArea description = new JTextArea(3, 22);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JTextField reporter = new JTextField(22);
        JTextField contact = new JTextField(22);

        int row = 0;
        row = addFormRow(form, gbc, row, "Item name:", name);
        row = addFormRow(form, gbc, row, "Category:", category);
        row = addFormRow(form, gbc, row, "Color:", color);
        row = addFormRow(form, gbc, row, "Date lost (yyyy-MM-dd):", date);
        row = addFormRow(form, gbc, row, "Last seen location:", location);
        row = addFormRow(form, gbc, row, "Description:", new JScrollPane(description));
        row = addFormRow(form, gbc, row, "Your name:", reporter);
        row = addFormRow(form, gbc, row, "Your contact:", contact);

        JButton submit = new JButton("Register Lost Item");
        submit.addActionListener(e -> {
            LocalDate d = parseDateOrWarn(date.getText());
            if (d == null) return;
            if (!validateNonEmpty(name, color, location, reporter, contact) || category.getSelectedItem() == null) {
                warn("Please fill in all fields.");
                return;
            }
            String id = store.nextLostId();
            LostItem item = new LostItem(id, name.getText().trim(), category.getSelectedItem().toString().trim(),
                    color.getText().trim(), d, location.getText().trim(), description.getText().trim(),
                    reporter.getText().trim(), contact.getText().trim());
            store.addLostItem(item);
            store.saveAll();
            refreshRecordTables();
            info("Lost item registered. Reference ID: " + id);
            clearFields(name, color, location, description, reporter, contact);
        });

        gbc.gridx = 1; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        form.add(submit, gbc);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(titled(form, "Report a Lost Item"), BorderLayout.NORTH);
        return wrapper;
    }

    // ================= Module 2: Found Item Registration =================
    private JPanel buildFoundRegistrationPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = formConstraints();

        JTextField name = new JTextField(22);
        JComboBox<String> category = new JComboBox<>(CATEGORIES);
        category.setEditable(true);
        JTextField color = new JTextField(22);
        JTextField date = new JTextField(LocalDate.now().toString(), 22);
        JTextField location = new JTextField(22);
        JTextArea description = new JTextArea(3, 22);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JTextField reporter = new JTextField(22);
        JTextField contact = new JTextField(22);

        int row = 0;
        row = addFormRow(form, gbc, row, "Item name:", name);
        row = addFormRow(form, gbc, row, "Category:", category);
        row = addFormRow(form, gbc, row, "Color:", color);
        row = addFormRow(form, gbc, row, "Date found (yyyy-MM-dd):", date);
        row = addFormRow(form, gbc, row, "Location found:", location);
        row = addFormRow(form, gbc, row, "Identifying features:", new JScrollPane(description));
        row = addFormRow(form, gbc, row, "Your name:", reporter);
        row = addFormRow(form, gbc, row, "Your contact:", contact);

        JButton submit = new JButton("Register Found Item");
        JTextArea matchOutput = new JTextArea(5, 40);
        matchOutput.setEditable(false);
        matchOutput.setBorder(BorderFactory.createTitledBorder("Possible matching lost-item reports"));

        submit.addActionListener(e -> {
            LocalDate d = parseDateOrWarn(date.getText());
            if (d == null) return;
            if (!validateNonEmpty(name, color, location, reporter, contact) || category.getSelectedItem() == null) {
                warn("Please fill in all fields.");
                return;
            }
            String id = store.nextFoundId();
            FoundItem item = new FoundItem(id, name.getText().trim(), category.getSelectedItem().toString().trim(),
                    color.getText().trim(), d, location.getText().trim(), description.getText().trim(),
                    reporter.getText().trim(), contact.getText().trim());
            store.addFoundItem(item);
            store.saveAll();
            refreshRecordTables();
            info("Found item registered. Reference ID: " + id);

            StringBuilder sb = new StringBuilder();
            List<LostItem> activeLost = new ArrayList<>();
            for (LostItem li : store.getLostItems()) if ("ACTIVE".equals(li.getStatus())) activeLost.add(li);
            List<Map.Entry<LostItem, Integer>> hits = new ArrayList<>();
            for (LostItem li : activeLost) {
                int score = MatchingEngine.matchScore(li, item);
                if (score >= 5) hits.add(Map.entry(li, score));
            }
            hits.sort((a, b) -> b.getValue() - a.getValue());
            if (hits.isEmpty()) {
                sb.append("No candidate matches yet.");
            } else {
                for (Map.Entry<LostItem, Integer> h : hits) {
                    sb.append("[Score ").append(h.getValue()).append("] ").append(h.getKey().getId())
                      .append(" - ").append(h.getKey().getName()).append(" (").append(h.getKey().getCategory())
                      .append(", ").append(h.getKey().getColor()).append(")\n");
                }
            }
            matchOutput.setText(sb.toString());
            clearFields(name, color, location, description, reporter, contact);
        });

        gbc.gridx = 1; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        form.add(submit, gbc);

        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.add(titled(form, "Report a Found Item"), BorderLayout.NORTH);
        wrapper.add(new JScrollPane(matchOutput), BorderLayout.CENTER);
        wrapper.setBorder(new EmptyBorder(0, 0, 20, 0));
        return wrapper;
    }

    // ================= Module 3: Search and Matching =================
    private JPanel buildSearchPanel() {
        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(new EmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] modes = {
                "Keyword - Lost items", "Keyword - Found items",
                "Category - Lost items", "Category - Found items",
                "Exact date - Lost items"
        };
        JComboBox<String> mode = new JComboBox<>(modes);
        JTextField query = new JTextField(25);
        JButton searchBtn = new JButton("Search");

        gbc.gridx = 0; gbc.gridy = 0; top.add(new JLabel("Search type:"), gbc);
        gbc.gridx = 1; top.add(mode, gbc);
        gbc.gridx = 2; top.add(new JLabel("Query:"), gbc);
        gbc.gridx = 3; top.add(query, gbc);
        gbc.gridx = 4; top.add(searchBtn, gbc);

        JTable resultTable = new JTable(searchResultModel);
        resultTable.setAutoCreateRowSorter(true);

        searchBtn.addActionListener(e -> {
            searchResultModel.setRowCount(0);
            String q = query.getText().trim();
            if (q.isEmpty()) { warn("Enter a search value first."); return; }
            int m = mode.getSelectedIndex();
            List<? extends Item> results;
            switch (m) {
                case 0: results = MatchingEngine.linearSearchLost(store.getLostItems(), q); break;
                case 1: results = MatchingEngine.linearSearchFound(new ArrayList<>(store.getFoundItems()), q); break;
                case 2: results = store.getLostByCategory(q); break;
                case 3: results = store.getFoundByCategory(q); break;
                default: {
                    LocalDate d = parseDateOrWarn(q);
                    if (d == null) return;
                    ArrayList<LostItem> sorted = new ArrayList<>(store.getLostItems());
                    sorted.sort(Comparator.comparing(LostItem::getDate));
                    results = MatchingEngine.binarySearchLostByDate(sorted, d);
                }
            }
            for (Item it : results) searchResultModel.addRow(rowOf(it));
            statusBar.setText(" Found " + results.size() + " result(s).");
        });

        JPanel searchSection = new JPanel(new BorderLayout());
        searchSection.add(top, BorderLayout.NORTH);
        searchSection.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        searchSection.setBorder(BorderFactory.createTitledBorder("Search lost / found records"));

        // --- Suggest matches sub-section ---
        JPanel matchTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField lostIdField = new JTextField(10);
        JButton suggestBtn = new JButton("Suggest matches");
        matchTop.add(new JLabel("Lost Item ID (e.g. L001):"));
        matchTop.add(lostIdField);
        matchTop.add(suggestBtn);

        JTable matchTable = new JTable(matchResultModel);
        matchTable.setAutoCreateRowSorter(true);

        suggestBtn.addActionListener(e -> {
            matchResultModel.setRowCount(0);
            LostItem lost = store.getLostById(lostIdField.getText().trim());
            if (lost == null) { warn("No lost item with that ID."); return; }
            List<Map.Entry<FoundItem, Integer>> matches = MatchingEngine.suggestMatches(lost, store.getFoundItems(), 3);
            for (Map.Entry<FoundItem, Integer> m2 : matches) {
                FoundItem f = m2.getKey();
                matchResultModel.addRow(new Object[]{ m2.getValue(), f.getId(), f.getName(), f.getCategory(),
                        f.getColor(), f.getDate().toString(), f.getLocation() });
            }
            statusBar.setText(" Found " + matches.size() + " candidate match(es) for " + lost.getId() + ".");
        });

        JPanel matchSection = new JPanel(new BorderLayout());
        matchSection.add(matchTop, BorderLayout.NORTH);
        matchSection.add(new JScrollPane(matchTable), BorderLayout.CENTER);
        matchSection.setBorder(BorderFactory.createTitledBorder("Suggest found-item matches for a lost item"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchSection, matchSection);
        split.setResizeWeight(0.55);
        split.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(split, BorderLayout.CENTER);
        return wrapper;
    }

    // ================= Module 4: Claim and Verification =================
    private JPanel buildClaimPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = formConstraints();

        JTextField lostId = new JTextField(20);
        JTextField foundId = new JTextField(20);
        JTextField claimant = new JTextField(20);
        JTextField detail = new JTextField(20);

        int row = 0;
        row = addFormRow(form, gbc, row, "Your Lost Item ID (e.g. L001):", lostId);
        row = addFormRow(form, gbc, row, "Matching Found Item ID (e.g. F001):", foundId);
        row = addFormRow(form, gbc, row, "Your name (claimant):", claimant);
        row = addFormRow(form, gbc, row, "Unique identifying detail:", detail);

        JButton submit = new JButton("Submit Claim");
        JTextArea resultArea = new JTextArea(6, 40);
        resultArea.setEditable(false);
        resultArea.setBorder(BorderFactory.createTitledBorder("Claim result"));

        submit.addActionListener(e -> {
            LostItem lost = store.getLostById(lostId.getText().trim());
            if (lost == null) { warn("Lost item ID not found."); return; }
            if (!"ACTIVE".equals(lost.getStatus())) { warn("This lost item is already " + lost.getStatus() + "."); return; }
            FoundItem found = store.getFoundById(foundId.getText().trim());
            if (found == null) { warn("Found item ID not found."); return; }
            if (!"ACTIVE".equals(found.getStatus())) { warn("This found item is already " + found.getStatus() + "."); return; }
            if (claimant.getText().trim().isEmpty() || detail.getText().trim().isEmpty()) {
                warn("Please enter your name and an identifying detail.");
                return;
            }

            int score = MatchingEngine.matchScore(lost, found);
            boolean detailMatches = found.getDescription().toLowerCase().contains(detail.getText().trim().toLowerCase())
                    || lost.getDescription().toLowerCase().contains(detail.getText().trim().toLowerCase());
            boolean verified = score >= 5 && detailMatches;

            String claimId = store.nextClaimId();
            Claim claim = new Claim(claimId, lost.getId(), found.getId(), claimant.getText().trim(), LocalDate.now(), verified);
            store.addClaim(claim);

            StringBuilder sb = new StringBuilder();
            sb.append("Automatic match score: ").append(score).append(" (need >= 5)\n");
            sb.append("Identifying detail matched: ").append(detailMatches).append("\n\n");
            if (verified) {
                lost.setStatus("RETURNED");
                found.setStatus("RETURNED");
                store.removeFoundFromActiveList(found.getId());
                sb.append("CLAIM VERIFIED. Item marked as RETURNED.\n");
                sb.append("Claim ID: ").append(claimId).append("\n");
                sb.append("Please coordinate handover with finder contact: ").append(found.getContactInfo());
                info("Claim verified. Item returned. Claim ID: " + claimId);
            } else {
                sb.append("Claim NOT verified. Logged for record-keeping.\n");
                sb.append("Claim ID: ").append(claimId);
                warn("Claim not verified (see details panel).");
            }
            resultArea.setText(sb.toString());
            store.saveAll();
            refreshRecordTables();
        });

        gbc.gridx = 1; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        form.add(submit, gbc);

        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.add(titled(form, "Claim and Verify an Item"), BorderLayout.NORTH);
        wrapper.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        wrapper.setBorder(new EmptyBorder(0, 0, 20, 0));
        return wrapper;
    }

    // ================= Records view =================
    private JPanel buildRecordsPanel() {
        JTable lostTable = new JTable(lostModel);
        JTable foundTable = new JTable(foundModel);
        JTable claimTable = new JTable(claimModel);
        lostTable.setAutoCreateRowSorter(true);
        foundTable.setAutoCreateRowSorter(true);
        claimTable.setAutoCreateRowSorter(true);

        JTabbedPane inner = new JTabbedPane();
        inner.addTab("Lost Items", new JScrollPane(lostTable));
        inner.addTab("Found Items", new JScrollPane(foundTable));
        inner.addTab("Claims Log", new JScrollPane(claimTable));

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshRecordTables());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.add(refresh);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(inner, BorderLayout.CENTER);
        wrapper.setBorder(new EmptyBorder(10, 10, 10, 10));
        return wrapper;
    }

    private void refreshRecordTables() {
        lostModel.setRowCount(0);
        for (LostItem it : store.getLostItems()) lostModel.addRow(rowOf(it));
        foundModel.setRowCount(0);
        for (FoundItem it : store.getFoundItems()) foundModel.addRow(rowOf(it));
        claimModel.setRowCount(0);
        for (Claim c : store.getClaims()) {
            claimModel.addRow(new Object[]{ c.getId(), c.getLostItemId(), c.getFoundItemId(),
                    c.getClaimantName(), c.getClaimDate().toString(), c.isVerified() ? "YES" : "NO" });
        }
    }

    // ================= Helpers =================
    private Object[] rowOf(Item it) {
        return new Object[]{ it.getId(), it.getName(), it.getCategory(), it.getColor(),
                it.getDate().toString(), it.getLocation(), it.getStatus(), it.getReporterName(), it.getContactInfo() };
    }

    private GridBagConstraints formConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private int addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(field, gbc);
        return row + 1;
    }

    private JPanel titled(JPanel inner, String title) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(inner, BorderLayout.CENTER);
        wrapper.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP));
        return wrapper;
    }

    private boolean validateNonEmpty(JTextField... fields) {
        for (JTextField f : fields) if (f.getText().trim().isEmpty()) return false;
        return true;
    }

    private void clearFields(JTextField a, JTextField b, JTextField c, JTextArea d, JTextField e, JTextField f) {
        a.setText(""); b.setText(""); c.setText(""); d.setText(""); e.setText(""); f.setText("");
    }

    private LocalDate parseDateOrWarn(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            warn("Invalid date format. Use yyyy-MM-dd, e.g. 2026-07-05.");
            return null;
        }
    }

    private void info(String msg) {
        statusBar.setForeground(new Color(40, 100, 40));
        statusBar.setText(" " + msg);
    }

    private void warn(String msg) {
        statusBar.setForeground(new Color(160, 40, 40));
        statusBar.setText(" " + msg);
        JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.WARNING_MESSAGE);
    }
}
