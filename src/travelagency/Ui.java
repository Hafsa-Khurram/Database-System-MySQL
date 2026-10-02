package travelagency;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * The look of the whole app: a fresh teal and coral "travel" theme.
 * Colours, fonts and small factory methods for styled components live here.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public final class Ui {

    // ----- Palette -----
    public static final Color TEAL = new Color(15, 118, 110);
    public static final Color TEAL_DARK = new Color(17, 78, 74);
    public static final Color TEAL_LIGHT = new Color(204, 239, 233);
    public static final Color TEAL_SOFT = new Color(236, 248, 245);
    public static final Color CORAL = new Color(244, 114, 76);
    public static final Color CORAL_DARK = new Color(222, 88, 52);
    public static final Color CORAL_LIGHT = new Color(255, 234, 225);
    public static final Color SAND = new Color(247, 245, 240);
    public static final Color WHITE = Color.WHITE;
    public static final Color INK = new Color(30, 41, 59);
    public static final Color MUTED = new Color(100, 116, 139);
    public static final Color LINE = new Color(226, 232, 240);
    public static final Color GREEN = new Color(22, 163, 74);
    public static final Color GREEN_LIGHT = new Color(220, 252, 231);
    public static final Color AMBER = new Color(202, 138, 4);
    public static final Color AMBER_LIGHT = new Color(254, 243, 199);
    public static final Color RED = new Color(220, 38, 38);
    public static final Color RED_LIGHT = new Color(254, 226, 226);

    // ----- Fonts -----
    private static final String FAMILY = "Segoe UI";
    public static final Font DISPLAY = new Font(FAMILY, Font.BOLD, 30);
    public static final Font TITLE = new Font(FAMILY, Font.BOLD, 22);
    public static final Font HEADING = new Font(FAMILY, Font.BOLD, 16);
    public static final Font LABEL = new Font(FAMILY, Font.BOLD, 13);
    public static final Font BODY = new Font(FAMILY, Font.PLAIN, 14);
    public static final Font SMALL = new Font(FAMILY, Font.PLAIN, 12);
    public static final Font NUMBER = new Font(FAMILY, Font.BOLD, 26);

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    public static final String EMPTY_TEXT = "emptyText";
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");

    private Ui() {
    }

    public static void install() {
        try {
            UIManager.put("control", SAND);
            UIManager.put("nimbusBase", TEAL_DARK);
            UIManager.put("nimbusFocus", TEAL);
            UIManager.put("nimbusSelectionBackground", TEAL);
            UIManager.put("nimbusLightBackground", WHITE);
            UIManager.put("text", INK);
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            System.err.println("Look and feel not available: " + ex.getMessage());
        }
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", LABEL);
        UIManager.put("ToolTip.font", SMALL);
    }

    public static String money(double amount) {
        return "Rs " + MONEY.format(amount);
    }

    public static String date(LocalDate date) {
        return date == null ? "-" : date.format(DATE);
    }

    // ===================== Messages =====================

    public static void error(Component parent, Exception ex) {
        String msg = ex instanceof SQLException sql ? Database.friendly(sql) : ex.getMessage();
        JOptionPane.showMessageDialog(parent, msg, ex instanceof IllegalArgumentException ? "Please check" : "Error",
                ex instanceof IllegalArgumentException ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ===================== Basic components =====================

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JTextField field() {
        JTextField f = new JTextField();
        styleInput(f);
        return f;
    }

    public static JPasswordField password() {
        JPasswordField f = new JPasswordField();
        styleInput(f);
        return f;
    }

    public static JTextArea area(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setFont(BODY);
        a.setForeground(INK);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createCompoundBorder(new Round(LINE, 12, 1.2f), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return a;
    }

    public static void styleInput(JTextField f) {
        f.setFont(BODY);
        f.setForeground(INK);
        f.setBackground(WHITE);
        f.setCaretColor(TEAL);
        f.setBorder(inputBorder(LINE));
        f.setPreferredSize(new Dimension(100, 40));
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                f.setBorder(inputBorder(TEAL));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                f.setBorder(inputBorder(LINE));
            }
        });
    }

    private static Border inputBorder(Color c) {
        return BorderFactory.createCompoundBorder(new Round(c, 12, c == TEAL ? 1.8f : 1.2f),
                BorderFactory.createEmptyBorder(6, 12, 6, 12));
    }

    public static <T> JComboBox<T> combo() {
        JComboBox<T> box = new JComboBox<>();
        box.setUI(new FlatComboUI());
        box.setFont(BODY);
        box.setForeground(INK);
        box.setBackground(WHITE);
        box.setOpaque(false);
        box.setBorder(BorderFactory.createCompoundBorder(new Round(LINE, 12, 1.2f), BorderFactory.createEmptyBorder(1, 8, 1, 2)));
        box.setPreferredSize(new Dimension(100, 40));
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, selected, false);
                c.setFont(BODY);
                c.setBorder(BorderFactory.createEmptyBorder(7, index < 0 ? 4 : 12, 7, 12));
                c.setForeground(INK);
                c.setBackground(selected && index >= 0 ? TEAL_LIGHT : WHITE);
                return c;
            }
        });
        return box;
    }

    @SafeVarargs
    public static <T> JComboBox<T> combo(T... items) {
        JComboBox<T> box = combo();
        for (T item : items) {
            box.addItem(item);
        }
        return box;
    }

    public static JSpinner number(int value, int min, int max) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        styleSpinner(s);
        return s;
    }

    /** Date picker (spinner). Use {@link #date(JSpinner)} to read the value. */
    public static JSpinner datePicker(LocalDate value) {
        SpinnerDateModel model = new SpinnerDateModel(toDate(value), null, null, Calendar.DAY_OF_MONTH);
        JSpinner s = new JSpinner(model);
        s.setEditor(new JSpinner.DateEditor(s, "dd MMM yyyy"));
        styleSpinner(s);
        return s;
    }

    public static LocalDate date(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void setDate(JSpinner spinner, LocalDate value) {
        spinner.setValue(toDate(value));
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static void styleSpinner(JSpinner s) {
        s.setUI(new BasicSpinnerUI() {
            @Override
            protected Component createNextButton() {
                Component b = arrowButton(true);
                b.setName("Spinner.nextButton");
                installNextButtonListeners(b);
                return b;
            }

            @Override
            protected Component createPreviousButton() {
                Component b = arrowButton(false);
                b.setName("Spinner.previousButton");
                installPreviousButtonListeners(b);
                return b;
            }
        });
        s.setFont(BODY);
        s.setBackground(WHITE);
        s.setBorder(BorderFactory.createCompoundBorder(new Round(LINE, 12, 1.2f), BorderFactory.createEmptyBorder(1, 8, 1, 2)));
        s.setPreferredSize(new Dimension(100, 40));
        if (s.getEditor() instanceof JSpinner.DefaultEditor editor) {
            JTextField f = editor.getTextField();
            f.setUI(new javax.swing.plaf.basic.BasicFormattedTextFieldUI());
            f.setFont(BODY);
            f.setForeground(INK);
            f.setBackground(WHITE);
            f.setBorder(BorderFactory.createEmptyBorder());
            f.setHorizontalAlignment(JTextField.LEFT);
        }
    }

    private static JButton arrowButton(boolean up) {
        JButton b = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(getModel().isRollover() ? TEAL : MUTED);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int d = up ? -1 : 1;
                g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2 * d, cy + 2 * d, cy - 2 * d}, 3);
                g2.dispose();
            }
        };
        b.setPreferredSize(new Dimension(24, 16));
        b.setBorder(BorderFactory.createEmptyBorder());
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** A label + input stacked vertically, used in every form. */
    public static JPanel field(String label, JComponent input) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(label(label, LABEL, INK), BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    /** Two (or more) fields side by side with equal widths. */
    public static JPanel row(JComponent... parts) {
        JPanel p = new JPanel(new java.awt.GridLayout(1, parts.length, 14, 0));
        p.setOpaque(false);
        for (JComponent c : parts) {
            p.add(c);
        }
        return p;
    }

    // ===================== Buttons =====================

    public enum Style { PRIMARY, ACCENT, OUTLINE, DANGER, SUCCESS, LIGHT }

    public static JButton button(String text, Style style) {
        return new Pill(text, style);
    }

    /** Fully rounded "pill" button with a hover effect. */
    public static class Pill extends JButton {
        private final Style style;
        private boolean hover;

        public Pill(String text, Style style) {
            super(text);
            this.style = style;
            setFont(LABEL);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            setForeground(style == Style.OUTLINE || style == Style.LIGHT ? TEAL_DARK : WHITE);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int h = getHeight();
            Color fill = switch (style) {
                case ACCENT -> hover ? CORAL_DARK : CORAL;
                case DANGER -> hover ? RED.darker() : RED;
                case SUCCESS -> hover ? GREEN.darker() : GREEN;
                case OUTLINE -> hover ? TEAL_SOFT : WHITE;
                case LIGHT -> hover ? TEAL_LIGHT : TEAL_SOFT;
                default -> hover ? TEAL_DARK : TEAL;
            };
            if (!isEnabled()) {
                fill = LINE;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, h - 1, h, h);
            if (style == Style.OUTLINE) {
                g2.setColor(TEAL);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, h - 3, h - 2, h - 2);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===================== Cards and borders =====================

    /** White rounded card with a soft shadow. */
    public static class Card extends JPanel {
        private final Color fill;

        public Card(LayoutManager layout) {
            this(layout, WHITE);
        }

        public Card(LayoutManager layout, Color fill) {
            super(layout);
            this.fill = fill;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(new Color(15, 118, 110, 14));
            g2.fillRoundRect(0, 4, getWidth(), getHeight() - 4, 22, 22);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 5, 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class Round implements Border {
        private final Color color;
        private final int radius;
        private final float width;

        public Round(Color color, int radius, float width) {
            this.color = color;
            this.radius = radius;
            this.width = width;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = smooth(g);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(width));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /** Small rounded label, e.g. a status or "12 seats left". */
    public static JLabel pill(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(new Font(FAMILY, Font.BOLD, 11));
        l.setForeground(fg);
        l.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return l;
    }

    public static Color[] statusColors(String status) {
        return switch (String.valueOf(status)) {
            case "Approved", "Paid" -> new Color[]{GREEN, GREEN_LIGHT};
            case "Pending" -> new Color[]{AMBER, AMBER_LIGHT};
            case "Cancelled", "Refunded" -> new Color[]{RED, RED_LIGHT};
            default -> new Color[]{MUTED, SAND};
        };
    }

    public static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** The app logo: a compass-like globe with a small plane orbit, drawn in code. */
    public static Icon logo(int size, Color ring, Color accent) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = smooth(g);
                g2.translate(x, y);
                float s = size / 40f;
                g2.scale(s, s);
                g2.setStroke(new BasicStroke(2.6f));
                g2.setColor(ring);
                g2.drawOval(6, 6, 28, 28);
                g2.drawArc(13, 6, 14, 28, 90, 180);
                g2.drawArc(13, 6, 14, 28, 270, 180);
                g2.drawLine(7, 20, 33, 20);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(1, 10, 38, 22, 200, 150);
                g2.fillPolygon(new int[]{33, 40, 34}, new int[]{12, 13, 18}, 3);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    // ===================== Tables =====================

    public static DefaultTableModel model(String[] columns, Class<?>... types) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column < types.length ? types[column] : Object.class;
            }
        };
    }

    /** Styled table: fills the width, scrolls sideways when narrow, shows a message when empty. */
    public static JTable table(DefaultTableModel model, String emptyText) {
        JTable t = new JTable(model) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() == null || getPreferredSize().width <= getParent().getWidth();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0 && emptyText != null) {
                    Graphics2D g2 = smooth(g);
                    g2.setFont(BODY);
                    g2.setColor(MUTED);
                    Rectangle v = getVisibleRect();
                    int w = g2.getFontMetrics().stringWidth(emptyText);
                    g2.drawString(emptyText, v.x + Math.max(12, (v.width - w) / 2), v.y + 46);
                    g2.dispose();
                }
            }
        };
        t.setFont(BODY);
        t.setRowHeight(36);
        t.setShowVerticalLines(false);
        t.setGridColor(LINE);
        t.setSelectionBackground(TEAL_LIGHT);
        t.setSelectionForeground(INK);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JTableHeader header = t.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 40));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus,
                                                           int row, int col) {
                String text = String.valueOf(value);
                if (table.getRowSorter() != null) {
                    for (RowSorter.SortKey key : table.getRowSorter().getSortKeys()) {
                        if (key.getColumn() == table.convertColumnIndexToModel(col) && key.getSortOrder() != SortOrder.UNSORTED) {
                            text += key.getSortOrder() == SortOrder.ASCENDING ? "  ▲" : "  ▼";
                        }
                    }
                }
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, text, sel, focus, row, col);
                c.setFont(LABEL);
                c.setForeground(TEAL_DARK);
                c.setBackground(TEAL_SOFT);
                c.setOpaque(true);
                c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, TEAL_LIGHT),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)));
                c.setToolTipText("Click to sort by " + value);
                return c;
            }
        });
        DefaultTableCellRenderer cells = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus,
                                                           int row, int col) {
                Object shown = value;
                if (value instanceof LocalDate d) {
                    shown = date(d);
                } else if (value instanceof Double d) {
                    shown = money(d);
                } else if (value instanceof Boolean b) {
                    shown = b ? "Yes" : "No";
                }
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, shown, sel, false, row, col);
                c.setHorizontalAlignment(SwingConstants.LEFT);
                c.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                c.setForeground(INK);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? WHITE : new Color(250, 251, 250));
                }
                return c;
            }
        };
        t.setDefaultRenderer(Object.class, cells);
        t.setDefaultRenderer(Number.class, cells);
        t.setDefaultRenderer(Double.class, cells);
        t.setDefaultRenderer(Integer.class, cells);
        t.setDefaultRenderer(Boolean.class, cells);
        return t;
    }

    /** Draws the status column as a coloured pill. */
    public static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            private String status = "";
            private boolean selected;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus,
                                                           int row, int col) {
                status = String.valueOf(value);
                selected = sel;
                super.getTableCellRendererComponent(table, "", sel, false, row, col);
                setBackground(sel ? TEAL_LIGHT : row % 2 == 0 ? WHITE : new Color(250, 251, 250));
                return this;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = smooth(g);
                Color[] c = statusColors(status);
                g2.setFont(new Font(FAMILY, Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int w = fm.stringWidth(status) + 22;
                int h = 24;
                int x = 10;
                int y = (getHeight() - h) / 2;
                g2.setColor(c[1]);
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.setColor(c[0]);
                g2.drawString(status, x + 11, y + (h + fm.getAscent()) / 2 - 2);
                g2.dispose();
            }
        };
    }

    /**
     * Sizes every column to its header and content (up to max pixels), so values
     * are not cut off. If the columns do not fit, the table scrolls sideways.
     */
    public static void fitColumns(JTable t, int max) {
        for (int col = 0; col < t.getColumnCount(); col++) {
            javax.swing.table.TableColumn column = t.getColumnModel().getColumn(col);
            Component h = t.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(t, column.getHeaderValue(), false, false, -1, col);
            int width = h.getPreferredSize().width + 6;
            for (int row = 0; row < t.getRowCount(); row++) {
                Component c = t.prepareRenderer(t.getCellRenderer(row, col), row, col);
                width = Math.max(width, c.getPreferredSize().width + 2);
            }
            column.setPreferredWidth(Math.min(width, max));
        }
        t.revalidate();
    }

    public static void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane s = new JScrollPane(view);
        s.setBorder(new Round(LINE, 16, 1.2f));
        s.getViewport().setBackground(WHITE);
        thinBars(s);
        JPanel corner = new JPanel();
        corner.setBackground(TEAL_SOFT);
        s.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        return s;
    }

    /** Transparent scroll pane for long forms. */
    public static JScrollPane clearScroll(Component view) {
        JScrollPane s = new JScrollPane(view);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.setViewportBorder(null);
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        thinBars(s);
        return s;
    }

    public static void thinBars(JScrollPane s) {
        s.getVerticalScrollBar().setUI(new ThinBar());
        s.getHorizontalScrollBar().setUI(new ThinBar());
        s.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        s.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        s.getVerticalScrollBar().setUnitIncrement(18);
    }

    private static class ThinBar extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(178, 214, 207);
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected JButton createDecreaseButton(int o) {
            return zero();
        }

        @Override
        protected JButton createIncreaseButton(int o) {
            return zero();
        }

        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) {
                return;
            }
            Graphics2D g2 = smooth(g);
            g2.setColor(isThumbRollover() ? TEAL : thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    private static class FlatComboUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = arrowButton(false);
            b.setPreferredSize(new Dimension(28, 28));
            return b;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean focus) {
            g.setColor(WHITE);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = smooth(g);
            g2.setColor(WHITE);
            g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 12, 12);
            g2.dispose();
            super.paint(g, c);
        }
    }

    /** Gradient panel used for headers and the login background. */
    public static class Gradient extends JPanel {
        private final Color from;
        private final Color to;
        private final boolean circles;

        public Gradient(LayoutManager layout, Color from, Color to, boolean circles) {
            super(layout);
            this.from = from;
            this.to = to;
            this.circles = circles;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setPaint(new GradientPaint(0, 0, from, getWidth(), getHeight(), to));
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (circles) {
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillOval(getWidth() - 220, -120, 360, 360);
                g2.fillOval(-140, getHeight() - 160, 300, 300);
                g2.setColor(new Color(244, 114, 76, 40));
                g2.fillOval(getWidth() / 2, getHeight() - 90, 160, 160);
            }
            g2.dispose();
        }
    }
}
