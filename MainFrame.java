import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class MainFrame extends JFrame {

    private TaskManager taskManager = new TaskManager();
    private DefaultTableModel tableModel;
    private JTable taskTable;
    private JTextField nameField, deadlineField;
    private JComboBox<String> priorityBox, statusBox;
    private int selectedRow = -1;

    private JLabel lblTotal, lblDone, lblPend;

    private final Color BG        = new Color(240, 244, 248);
    private final Color PANEL     = Color.WHITE;
    private final Color NAVY      = new Color(18, 52, 86);
    private final Color BLUE      = new Color(25, 118, 210);
    private final Color GREEN     = new Color(46, 125, 50);
    private final Color RED       = new Color(183, 28, 28);
    private final Color MUTED     = new Color(107, 114, 128);
    private final Color BORDER    = new Color(220, 232, 240);
    private final Color ROW_ALT   = new Color(248, 250, 251);
    private final Color ROW_SEL   = new Color(187, 222, 251);
    private final Color ROW_HOVER = new Color(227, 242, 253);

    private final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 26);
    private final Font FONT_SUB     = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 11);
    private final Font FONT_LABEL   = new Font("Segoe UI", Font.BOLD, 13);
    private final Font FONT_INPUT   = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font FONT_BADGE   = new Font("Segoe UI", Font.BOLD, 11);

    public MainFrame() {
        setTitle("Smart To-Do List");
        setSize(980, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        root.add(buildTitlePanel(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 20, 0));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));
        center.add(buildFormPanel());
        center.add(buildTablePanel());
        root.add(center, BorderLayout.CENTER);

        root.add(buildButtonPanel(), BorderLayout.SOUTH);
        setContentPane(root);
        setVisible(true);
        loadTasks();
    }

    // ── Title ──────────────────────────────────────────────────────────

    private JPanel buildTitlePanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Smart To-Do List", SwingConstants.CENTER);
        title.setFont(FONT_TITLE);
        title.setForeground(NAVY);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Stay organized. Get things done.", SwingConstants.CENTER);
        sub.setFont(FONT_SUB);
        sub.setForeground(MUTED);
        sub.setAlignmentX(CENTER_ALIGNMENT);
        sub.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        p.add(title);
        p.add(sub);
        return p;
    }

    // ── Form Panel ─────────────────────────────────────────────────────

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(PANEL);
        outer.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        outer.add(sectionLabel("TASK INFORMATION"), BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        nameField     = styledField("Enter task name...");
        deadlineField = styledField("YYYY-MM-DD");
        deadlineField.setText(LocalDate.now().toString());
        priorityBox   = styledCombo(new String[]{"Low", "Medium", "High"});
        statusBox     = styledCombo(new String[]{"Pending", "In Progress", "Done"});

        form.add(fieldLabel("Task Name"));
        form.add(Box.createVerticalStrut(5));
        form.add(nameField);
        form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Priority"));
        form.add(Box.createVerticalStrut(5));
        form.add(priorityBox);
        form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Status"));
        form.add(Box.createVerticalStrut(5));
        form.add(statusBox);
        form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Deadline (YYYY-MM-DD)"));
        form.add(Box.createVerticalStrut(5));
        form.add(deadlineField);

        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    // ── Table Panel ────────────────────────────────────────────────────

    private JPanel buildTablePanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(PANEL);
        outer.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        outer.add(sectionLabel("TASK LIST"), BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 3, 10, 0));
        stats.setOpaque(false);
        stats.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));

        lblTotal = new JLabel("0", SwingConstants.CENTER);
        lblDone  = new JLabel("0", SwingConstants.CENTER);
        lblPend  = new JLabel("0", SwingConstants.CENTER);

        stats.add(wrapStat(lblTotal, "Total"));
        stats.add(wrapStat(lblDone,  "Done"));
        stats.add(wrapStat(lblPend,  "Pending"));

        tableModel = new DefaultTableModel(
                new String[]{"Task", "Priority", "Status", "Deadline"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        taskTable = new JTable(tableModel) {
            private int hoverRow = -1;
            {
                addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    public void mouseMoved(java.awt.event.MouseEvent e) {
                        int r = rowAtPoint(e.getPoint());
                        if (r != hoverRow) { hoverRow = r; repaint(); }
                    }
                });
            }
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (isRowSelected(row))   c.setBackground(ROW_SEL);
                else if (row == hoverRow) c.setBackground(ROW_HOVER);
                else                      c.setBackground(row % 2 == 0 ? PANEL : ROW_ALT);
                c.setForeground(new Color(30, 30, 30));
                return c;
            }
        };

        taskTable.setFont(FONT_INPUT);
        taskTable.setRowHeight(34);
        taskTable.setShowGrid(false);
        taskTable.setIntercellSpacing(new Dimension(0, 0));
        taskTable.setFocusable(false);
        taskTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        taskTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Column widths
        taskTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        taskTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        taskTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        taskTable.getColumnModel().getColumn(3).setPreferredWidth(110);

        taskTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        taskTable.getTableHeader().setBackground(NAVY);
        taskTable.getTableHeader().setForeground(Color.WHITE);
        taskTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        taskTable.getTableHeader().setOpaque(true);
        taskTable.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        // Task column — full text + tooltip
        taskTable.getColumnModel().getColumn(0).setCellRenderer(
            new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int col) {
                    Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, col);
                    String text = value != null ? value.toString() : "";
                    setText(text);
                    setToolTipText(text);
                    setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                    if (isSelected) c.setBackground(ROW_SEL);
                    else c.setBackground(row % 2 == 0 ? PANEL : ROW_ALT);
                    c.setForeground(new Color(30, 30, 30));
                    return c;
                }
            }
        );

        taskTable.getColumnModel().getColumn(1).setCellRenderer(badgeRenderer());
        taskTable.getColumnModel().getColumn(2).setCellRenderer(badgeRenderer());

        // Deadline color warning
        taskTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(CENTER);
                if (!isSelected && value != null && !value.toString().isEmpty()) {
                    try {
                        LocalDate deadline = LocalDate.parse(value.toString());
                        LocalDate today    = LocalDate.now();
                        long days = ChronoUnit.DAYS.between(today, deadline);
                        if (days < 0) {
                            c.setBackground(new Color(253, 232, 232));
                            c.setForeground(new Color(183, 28, 28));
                        } else if (days <= 3) {
                            c.setBackground(new Color(255, 243, 224));
                            c.setForeground(new Color(230, 81, 0));
                        } else {
                            c.setBackground(row % 2 == 0 ? PANEL : ROW_ALT);
                            c.setForeground(new Color(30, 30, 30));
                        }
                    } catch (Exception ex) {
                        c.setBackground(row % 2 == 0 ? PANEL : ROW_ALT);
                    }
                }
                return c;
            }
        });

        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedRow = taskTable.getSelectedRow();
                if (selectedRow >= 0) {
                    Task t = taskManager.getTask(selectedRow);
                    nameField.setText(t.getName());
                    priorityBox.setSelectedItem(t.getPriority());
                    statusBox.setSelectedItem(t.getStatus());
                    deadlineField.setText(t.getDeadline());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(taskTable);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(PANEL);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(stats, BorderLayout.NORTH);
        right.add(scroll, BorderLayout.CENTER);

        outer.add(right, BorderLayout.CENTER);
        return outer;
    }

    // ── Buttons ────────────────────────────────────────────────────────

    private JPanel buildButtonPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        p.setOpaque(false);

        JButton add    = fancyBtn("+ Add Task",    GREEN);
        JButton update = fancyBtn("~ Update Task", BLUE);
        JButton delete = fancyBtn("x Delete Task", RED);
        JButton save   = fancyBtn("Save Tasks",    new Color(69, 90, 100));
        JButton load   = fancyBtn("Load Tasks",    new Color(69, 90, 100));

        add.addActionListener(e -> addTask());
        update.addActionListener(e -> updateTask());
        delete.addActionListener(e -> deleteTask());
        save.addActionListener(e -> saveTasks());
        load.addActionListener(e -> loadTasks());

        p.add(add); p.add(update); p.add(delete); p.add(save); p.add(load);
        return p;
    }

    // ── Actions ────────────────────────────────────────────────────────

    private void addTask() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Task name cannot be empty!",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return;
        }
        Task t = new Task(name,
                (String) priorityBox.getSelectedItem(),
                (String) statusBox.getSelectedItem(),
                deadlineField.getText().trim());
        taskManager.addTask(t);
        tableModel.addRow(new Object[]{t.getName(), t.getPriority(), t.getStatus(), t.getDeadline()});
        clearForm();
        updateStats();
    }

    private void updateTask() {
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task first.");
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Task name cannot be empty!",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return;
        }
        Task t = new Task(name,
                (String) priorityBox.getSelectedItem(),
                (String) statusBox.getSelectedItem(),
                deadlineField.getText().trim());
        taskManager.updateTask(selectedRow, t);
        tableModel.setValueAt(t.getName(),     selectedRow, 0);
        tableModel.setValueAt(t.getPriority(), selectedRow, 1);
        tableModel.setValueAt(t.getStatus(),   selectedRow, 2);
        tableModel.setValueAt(t.getDeadline(), selectedRow, 3);
        clearForm();
        updateStats();
    }

    private void deleteTask() {
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this task?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        taskManager.deleteTask(selectedRow);
        tableModel.removeRow(selectedRow);
        selectedRow = -1;
        clearForm();
        updateStats();
    }

    private void saveTasks() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream("tasks.dat"));
            out.writeObject(taskManager.getTasks());
            out.close();
            JOptionPane.showMessageDialog(this,
                "Tasks saved successfully!",
                "Saved",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error saving tasks.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadTasks() {
        try {
            ObjectInputStream in = new ObjectInputStream(
                new FileInputStream("tasks.dat"));
            ArrayList<Task> saved = (ArrayList<Task>) in.readObject();
            in.close();
            taskManager = new TaskManager();
            tableModel.setRowCount(0);
            for (Task t : saved) {
                taskManager.addTask(t);
                tableModel.addRow(new Object[]{
                    t.getName(), t.getPriority(), t.getStatus(), t.getDeadline()});
            }
            updateStats();
        } catch (Exception ex) {
            // No save file yet, that's fine
        }
    }

    private void clearForm() {
        nameField.setText("");
        priorityBox.setSelectedIndex(0);
        statusBox.setSelectedIndex(0);
        deadlineField.setText(LocalDate.now().toString());
        taskTable.clearSelection();
        selectedRow = -1;
    }

    private void updateStats() {
        int total   = taskManager.getTasks().size();
        int done    = (int) taskManager.getTasks().stream().filter(t -> t.getStatus().equals("Done")).count();
        int pending = (int) taskManager.getTasks().stream().filter(t -> t.getStatus().equals("Pending")).count();
        lblTotal.setText(String.valueOf(total));
        lblDone.setText(String.valueOf(done));
        lblPend.setText(String.valueOf(pending));
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private JPanel wrapStat(JLabel valLabel, String type) {
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLabel.setForeground(NAVY);

        JLabel lbl = new JLabel(type, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(MUTED);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(248, 250, 251));
        card.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        valLabel.setAlignmentX(CENTER_ALIGNMENT);
        lbl.setAlignmentX(CENTER_ALIGNMENT);
        card.add(valLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(lbl);
        return card;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SECTION);
        l.setForeground(BLUE);
        l.setBorder(new MatteBorder(0, 0, 2, 0, new Color(200, 220, 245)));
        l.setPreferredSize(new Dimension(0, 28));
        return l;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(new Color(30, 30, 30));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(FONT_INPUT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setPreferredSize(new Dimension(200, 36));
        f.setBorder(new CompoundBorder(
            new LineBorder(new Color(200, 215, 235), 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        f.setAlignmentX(LEFT_ALIGNMENT);
        return f;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(FONT_INPUT);
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cb.setPreferredSize(new Dimension(200, 36));
        cb.setBackground(Color.WHITE);
        cb.setAlignmentX(LEFT_ALIGNMENT);
        return cb;
    }

    private JButton fancyBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(160, 44));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(bg.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(bg);
            }
        });
        return b;
    }

    private DefaultTableCellRenderer badgeRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {

                JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                cell.setOpaque(true);
                cell.setBackground(isSelected ? ROW_SEL : row % 2 == 0 ? PANEL : ROW_ALT);

                String val = value == null ? "" : value.toString();
                JLabel badge = new JLabel(val, SwingConstants.CENTER);
                badge.setFont(FONT_BADGE);
                badge.setOpaque(true);

                switch (val) {
                    case "High":        badge.setBackground(new Color(253,232,232)); badge.setForeground(new Color(183,28,28));  break;
                    case "Medium":      badge.setBackground(new Color(255,243,224)); badge.setForeground(new Color(230,81,0));   break;
                    case "Low":         badge.setBackground(new Color(232,245,233)); badge.setForeground(new Color(46,125,50));  break;
                    case "Done":        badge.setBackground(new Color(232,245,233)); badge.setForeground(new Color(46,125,50));  break;
                    case "In Progress": badge.setBackground(new Color(227,242,253)); badge.setForeground(new Color(21,101,192)); break;
                    case "Pending":     badge.setBackground(new Color(243,244,246)); badge.setForeground(new Color(107,114,128));break;
                    default:            badge.setBackground(new Color(243,244,246)); badge.setForeground(new Color(107,114,128));break;
                }

                badge.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
                cell.add(badge);
                return cell;
            }
        };
    }
}