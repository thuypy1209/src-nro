package nro.panel;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import nro.consts.ConstNpc;
import nro.models.map.Zone;
import nro.models.npc.NpcFactory;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;
import nro.jdbc.DBService;
import nro.jdbc.daos.ShopDAO;
import nro.models.boss.Boss;
import nro.models.boss.BossFactory;
import nro.models.boss.BossManager;
import nro.models.item.Item;
import nro.models.item.CaiTrang;
import nro.models.item.ItemOption;
import nro.models.item.ItemOptionTemplate;
import nro.models.item.ItemTemplate;
import nro.models.player.Player;
import nro.models.shop.Shop;
import nro.server.Client;
import nro.server.Maintenance;
import nro.server.Manager;
import nro.server.ServerManager;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.func.ChangeMapService;
import nro.utils.Log;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Admin Panel GUI JFrame quản lý Server Realtime cho SRC NRO NEW 01:
 * - Theo dõi trạng thái Server, Online, RAM, Uptime, Sessions
 * - Quản lý nhân vật realtime: buff tiền, ngọc, ruby, hồi phục, dịch chuyển, kick, lưu
 * - Quản lý Vật phẩm: Thêm, Sửa, Xóa Item Template trong CSDL & Buff đồ kèm Options
 * - Quản lý Boss: xem boss, kick boss, triệu hồi boss
 * - Quản lý Tài khoản (MySQL): kích hoạt, ban/unban, đổi pass, cấp admin, thỏi vàng
 * - Quản lý Cửa Hàng & GiftCode: Tạo GiftCode trực quan chọn đồ, sửa, xóa
 * - Thông báo toàn máy chủ & Bảo trì hẹn giờ
 *
 * @author DragonBoy Dev
 */
public class AdminPanelFrame extends JFrame {

    private static AdminPanelFrame instance;
    private static volatile boolean isDialogOpen = false;
    private final long startTime;
    private final NumberFormat numFmt = NumberFormat.getInstance(Locale.US);

    // Top status labels
    private JLabel lblOnline;
    private JLabel lblSessions;
    private JLabel lblBossCount;
    private JLabel lblMemory;
    private JLabel lblUptime;

    // Tabs
    private JTabbedPane tabbedPane;

    // Player Tab
    private DefaultTableModel playerTableModel;
    private JTable playerTable;
    private TableRowSorter<DefaultTableModel> playerSorter;
    private JTextField txtPlayerSearch;
    private JLabel lblPlayerDetail;
    private Player selectedPlayer;

    // Item Tab
    private DefaultTableModel itemTableModel;
    private JTable itemTable;
    private TableRowSorter<DefaultTableModel> itemSorter;
    private JTextField txtItemSearch;
    private JTextField txtBuffPlayer;
    private JTextField txtBuffQuantity;
    private JComboBox<String> cbPresetOption;

    // Item Tab Live Preview Components
    private JLabel lblPreviewItemIcon;
    private JLabel lblPreviewItemTitle;
    private JLabel lblPreviewItemBadges;
    private JLabel lblPreviewItemPart;
    private JLabel lblPreviewItemDesc;
    private JEditorPane epPreviewOptions;
    private JButton btnPreviewEdit;
    private JButton btnPreviewDelete;
    private JTextField txtCustomOptId;
    private JTextField txtOptionParam;

    // Boss Tab
    private DefaultTableModel bossTableModel;
    private JTable bossTable;
    private JComboBox<String> cbBossSpawn;

    // Account Tab
    private DefaultTableModel accountTableModel;
    private JTable accountTable;
    private JTextField txtAccountSearch;

    // Shop & Giftcode Tabs
    private JComboBox<String> cbMapSelect;
    private DefaultTableModel shopItemTableModel;
    private JTable shopItemTable;
    private JComboBox<String> cbShopNpcSelect;
    private JComboBox<String> cbShopTabSelect;
    private DefaultTableModel giftcodeTableModel;
    private JTable giftcodeTable;
    private JLabel lblCurrentEventStatus;
    // NPC Management Fields (Referenced from AdminPanel.java)
    private JComboBox<String> cbMapList;
    private JTable tblNpcs;
    private DefaultTableModel modelNpcs;
    private JTextField txtNpcTempId, txtNpcX, txtNpcY, txtNpcAvatar, txtNpcStatus;
    private JComboBox<String> cbNpcTemplates;
    private JCheckBox chkSaveDbNpc;
    private nro.models.map.Map selectedMap;
    private final List<nro.models.npc.Npc> currentMapNpcList = new ArrayList<>();

    // Event & Rates Fields
    private JComboBox<String> cbEventList;
    private JSpinner spExpRate;
    private JCheckBox chkMonsterDropGem;
    private JTextField txtDuaTopTime;
    private JLabel lblEventCurrentDisplay;

    // Console & SQL Fields
    private JTextArea txtConsoleLog;
    private JTextArea txtSqlQuery;
    private JTable tblSqlResult;
    private DefaultTableModel modelSqlResult;
    public static final List<String> RECENT_LOGS = Collections.synchronizedList(new LinkedList<>());


    // Modern Deep Slate Dark Theme Palette
    public static final Color COLOR_BG_DARK = new Color(15, 23, 42);        // Slate 900
    public static final Color COLOR_PANEL_DARK = new Color(30, 41, 59);     // Slate 800
    public static final Color COLOR_CARD_DARK = new Color(51, 65, 85);       // Slate 700
    public static final Color COLOR_BORDER_DARK = new Color(71, 85, 105);
    public static final Color COLOR_INPUT_DARK = new Color(30, 41, 59);   // Slate 600
    public static final Color COLOR_TEXT_LIGHT = new Color(248, 250, 252);  // Slate 50
    public static final Color COLOR_TEXT_MUTED = new Color(148, 163, 184);  // Slate 400
    public static final Color COLOR_ACCENT = new Color(56, 189, 248);       // Sky 400
    public static final Color COLOR_SUCCESS = new Color(34, 197, 94);       // Emerald 500
    public static final Color COLOR_DANGER = new Color(225, 29, 72);        // Rose 600
    public static final Color COLOR_WARNING = new Color(245, 158, 11);      // Amber 500
    public static final Color COLOR_TABLE_ROW1 = new Color(15, 23, 42);     // Slate 900
    public static final Color COLOR_TABLE_ROW2 = new Color(23, 32, 51);     // Slate 850
    public static final Color COLOR_TABLE_HEADER = new Color(30, 41, 59);   // Slate 800
    public static final Color COLOR_TABLE_SEL = new Color(37, 99, 235);     // Blue 600

    public static class ModernTabbedPaneUI extends javax.swing.plaf.basic.BasicTabbedPaneUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(8, 14, 8, 14);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(1, 0, 0, 0);
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected) {
                g2.setColor(new Color(15, 23, 42)); // Slate 900
                g2.fillRect(x, y, w, h);
                // Glowing cyan indicator underline
                g2.setColor(new Color(56, 189, 248)); // Sky 400
                g2.fillRect(x + 2, y + h - 3, w - 4, 3);
            } else {
                g2.setColor(new Color(30, 41, 59)); // Slate 800
                g2.fillRect(x, y, w, h);
            }
            g2.dispose();
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(51, 65, 85)); // Slate 700 divider
            g2.drawLine(x + w - 1, y + 4, x + w - 1, y + h - 4);
            g2.dispose();
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(51, 65, 85));
            g2.drawLine(0, 0, tabPane.getWidth(), 0);
            g2.dispose();
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
            // Remove ugly dotted focus rectangle
        }
    }

    public static JButton createModernButton(String text, Color accentColor, Color textColor) {
        JButton btn = new JButton(text) {
            private boolean isHover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHover = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHover = false;
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Dark Slate Background with subtle hover highlight
                if (getModel().isPressed()) {
                    g2.setColor(new Color(15, 23, 42)); // Pressed deep slate
                    g2.fillRoundRect(0, 0, w, h, 8, 8);
                } else if (isHover) {
                    g2.setColor(new Color(51, 65, 85)); // Hover slate
                    g2.fillRoundRect(0, 0, w, h, 8, 8);
                } else {
                    g2.setColor(new Color(30, 41, 59)); // Normal slate 800
                    g2.fillRoundRect(0, 0, w, h, 8, 8);
                }

                // Border Outline
                Color borderClr = isHover ? accentColor : new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 140);
                g2.setColor(borderClr);
                g2.setStroke(new BasicStroke(isHover ? 1.5f : 1.2f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 7, 7);

                // Text
                FontMetrics fm = g2.getFontMetrics(getFont());
                int textX = (w - fm.stringWidth(getText())) / 2;
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(isHover ? Color.WHITE : (textColor != null ? textColor : COLOR_TEXT_LIGHT));
                g2.setFont(getFont());
                g2.drawString(getText(), textX, textY);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 34));
        return btn;
    }

    private JLabel createStatusBadge(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 120));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 13, 13);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(fg);
        lbl.setOpaque(false);
        lbl.setBorder(new EmptyBorder(5, 12, 5, 12));
        return lbl;
    }

    private JPanel createActionGroup(String title, Color accent, JButton[] buttons) {
        JPanel group = new JPanel(new BorderLayout(0, 6));
        group.setOpaque(false);
        
        JLabel lblHeader = new JLabel("● " + title);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblHeader.setForeground(accent);
        lblHeader.setBorder(new EmptyBorder(0, 4, 2, 0));
        group.add(lblHeader, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new GridLayout(1, buttons.length, 6, 0));
        btnRow.setOpaque(false);
        for (JButton b : buttons) {
            b.setPreferredSize(new Dimension(b.getPreferredSize().width, 36));
            btnRow.add(b);
        }
        group.add(btnRow, BorderLayout.CENTER);
        return group;
    }

    public static String getTypeName(int type) {
        switch (type) {
            case 0: return "Áo (Shirt)";
            case 1: return "Quần (Pants)";
            case 2: return "Găng tay (Glove)";
            case 3: return "Giày (Shoes)";
            case 4: return "Ra-đa (Radar)";
            case 5: return "Cải trang (Costume)";
            case 6: return "Đậu thần (Magic Bean)";
            case 7: return "Sách kỹ năng";
            case 11: return "Đeo lưng / Phụ kiện";
            case 12: return "Ngọc Rồng (Dragon Ball)";
            case 14: return "Bình nước / Thuốc lá";
            case 21: return "Pet / Đệ tử";
            case 23: return "Ván bay (Flying board)";
            case 27: return "Hộp quà (Giftbox)";
            case 29: return "Máy quét / Dò Boss";
            case 30: return "Đá nâng cấp";
            case 72: return "Linh thú";
            default: return "Loại " + type;
        }
    }

    private JPanel createMiniFieldPanel(String label, JComponent tf) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(251, 146, 60)); // Amber Orange
        p.add(l, BorderLayout.NORTH);
        p.add(tf, BorderLayout.CENTER);
        return p;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, String label, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(new Color(248, 250, 252));
        panel.add(l, gbc);

        gbc.gridx = 1;
        panel.add(comp, gbc);
    }

    public static JTextField createDarkTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tf.setBackground(new Color(15, 23, 42)); // Deep Slate Dark
        tf.setForeground(Color.WHITE); // Crisp pure white
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(56, 189, 248), 1, true), // Sky 400
            new EmptyBorder(6, 10, 6, 10)
        ));
        return tf;
    }

    private void addFormField(JPanel panel, String labelText, JComponent comp) {
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(COLOR_TEXT_LIGHT);
        panel.add(lbl);
        panel.add(comp);
    }

    public static synchronized AdminPanelFrame gI() {
        if (instance == null) {
            instance = new AdminPanelFrame();
        }
        return instance;
    }

    public AdminPanelFrame() {
        this.startTime = System.currentTimeMillis();
        initComponents();
        startAutoRefreshTimer();
    }

    private void initComponents() {
        // Dark Theme UI Defaults
        try {
            UIManager.put("Panel.background", COLOR_PANEL_DARK);
            UIManager.put("OptionPane.background", COLOR_PANEL_DARK);
            UIManager.put("OptionPane.messageForeground", COLOR_TEXT_LIGHT);
            UIManager.put("Label.foreground", COLOR_TEXT_LIGHT);
            UIManager.put("Table.background", COLOR_TABLE_ROW1);
            UIManager.put("Table.foreground", COLOR_TEXT_LIGHT);
            UIManager.put("Table.gridColor", COLOR_BORDER_DARK);
            UIManager.put("Table.selectionBackground", COLOR_TABLE_SEL);
            UIManager.put("Table.selectionForeground", Color.WHITE);
            UIManager.put("TableHeader.background", COLOR_TABLE_HEADER);
            UIManager.put("TableHeader.foreground", COLOR_ACCENT);
            UIManager.put("TextField.background", new Color(35, 44, 62));
            UIManager.put("TextField.foreground", Color.WHITE);
            UIManager.put("TextField.caretForeground", COLOR_ACCENT);
            UIManager.put("ComboBox.background", new Color(35, 44, 62));
            UIManager.put("ComboBox.foreground", Color.WHITE);
            UIManager.put("ScrollPane.background", COLOR_BG_DARK);
            UIManager.put("Viewport.background", COLOR_BG_DARK);
            UIManager.put("TitledBorder.titleColor", COLOR_ACCENT);
            UIManager.put("TabbedPane.background", COLOR_PANEL_DARK);
            UIManager.put("TabbedPane.foreground", COLOR_TEXT_LIGHT);
            UIManager.put("TabbedPane.selected", COLOR_CARD_DARK);
        } catch (Exception ignored) {
        }

        setTitle("Polar Server - BẢNG QUẢN TRỊ SERVER REALTIME (DARK PANEL)");
        setSize(1320, 800);
        setMinimumSize(new Dimension(1150, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        getContentPane().setBackground(COLOR_BG_DARK);
        setLayout(new BorderLayout(5, 5));

        // 1. TOP STATUS PANEL
        add(createTopStatusPanel(), BorderLayout.NORTH);

        // 2. MAIN TABBED PANE (TÁCH CỬA HÀNG VÀ GIFTCODE THÀNH 2 TAB RIÊNG)
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setUI(new ModernTabbedPaneUI());
        tabbedPane.setBackground(COLOR_PANEL_DARK);
        tabbedPane.setForeground(COLOR_TEXT_LIGHT);

        tabbedPane.addTab("Người Chơi (Online)", createPlayerPanel());
        tabbedPane.addTab("Vật Phẩm & Buff Đồ", createItemPanel());
        tabbedPane.addTab("Quản Lý NPC Bản Đồ", createNpcPanel());
        tabbedPane.addTab("Sự Kiện & Tỷ Lệ", createEventPanel());
        tabbedPane.addTab("Quản Lý Boss Realtime", createBossPanel());
        tabbedPane.addTab("Cửa Hàng (Shop NPC)", createShopPanel());
        tabbedPane.addTab("Mã Quà Tặng (GiftCode)", createGiftCodePanel());
        tabbedPane.addTab("Quản Lý Tài Khoản (CSDL)", createAccountPanel());
        tabbedPane.addTab("Thông Báo & Bảo Trì", createSystemPanel());
        tabbedPane.addTab("Console & Truy Vấn SQL", createConsolePanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // 1. TOP STATUS PANEL
    // ==========================================
    private JPanel createTopStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(COLOR_PANEL_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER_DARK),
            new EmptyBorder(10, 16, 10, 16)
        ));

        // TIER 1: BRAND TITLE & STATUS BADGES
        JPanel row1 = new JPanel(new BorderLayout(15, 0));
        row1.setOpaque(false);

        JLabel lblTitle = new JLabel("● Polar NRO • SERVER MANAGEMENT CONSOLE");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(COLOR_ACCENT);
        row1.add(lblTitle, BorderLayout.WEST);

        JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        badgesPanel.setOpaque(false);

        lblOnline = createStatusBadge("Online: 0", new Color(34, 197, 94), new Color(20, 83, 45));
        lblSessions = createStatusBadge("Sessions: 0", new Color(147, 197, 253), new Color(30, 58, 138));
        lblBossCount = createStatusBadge("Boss: 0", new Color(251, 146, 60), new Color(124, 45, 18));
        lblMemory = createStatusBadge("RAM: 0 MB", new Color(253, 224, 71), new Color(113, 63, 18));
        lblUptime = createStatusBadge("Uptime: 00:00:00", new Color(216, 180, 254), new Color(88, 28, 135));

        badgesPanel.add(lblOnline);
        badgesPanel.add(lblSessions);
        badgesPanel.add(lblBossCount);
        badgesPanel.add(lblMemory);
        badgesPanel.add(lblUptime);
        row1.add(badgesPanel, BorderLayout.EAST);

        panel.add(row1, BorderLayout.NORTH);

        // TIER 2: ACTION TOOLBAR
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);

        JButton btnGC = createModernButton("Dọn Rác RAM (GC)", new Color(71, 85, 105), Color.WHITE);
        btnGC.addActionListener(e -> {
            System.gc();
            updateServerStatus();
            JOptionPane.showMessageDialog(this, "Đã dọn dẹp và thu hồi bộ nhớ JVM thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
        row2.add(btnGC);

        JButton btnSaveAll = createModernButton("Lưu Toàn Bộ (Save All)", new Color(16, 185, 129), Color.WHITE);
        btnSaveAll.addActionListener(e -> saveAllPlayers());
        row2.add(btnSaveAll);

        JButton btnRestartTop = createModernButton("Khởi Động Lại Server", new Color(225, 29, 72), Color.WHITE);
        btnRestartTop.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Xác nhận lưu toàn bộ dữ liệu và khởi động lại Server?\n- YES: Khởi động lại Server\n- NO: Hủy thao tác", 
                "Xác nhận Restart Server", 
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this, "Đang lưu toàn bộ dữ liệu và khởi động lại Server...", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                ServerManager.gI().restart(false);
            }
        });
        row2.add(btnRestartTop);

        JButton btnOpenWeb = createModernButton("Mở Bản Web (Browser)", new Color(37, 99, 235), Color.WHITE);
        btnOpenWeb.addActionListener(e -> {
            try {
                int p = (Manager.apiPort > 0) ? Manager.apiPort : 8181;
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("http://localhost:" + p + "/"));
                } else {
                    JOptionPane.showMessageDialog(this, "Vui lòng mở trình duyệt và truy cập: http://localhost:" + p + "/", "Bản Web", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi mở trình duyệt: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        row2.add(btnOpenWeb);

        JButton btnRefreshAll = createModernButton("Làm Mới Dữ Liệu", new Color(99, 102, 241), Color.WHITE);
        btnRefreshAll.addActionListener(e -> refreshAllData());
        row2.add(btnRefreshAll);

        panel.add(row2, BorderLayout.SOUTH);
        return panel;
    }

    // ==========================================
    // 2. TAB QUẢN LÝ NGƯỜI CHƠI (REALTIME)
    // ==========================================
    private JPanel createPlayerPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Search Bar
        JPanel topSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topSearch.add(new JLabel("Tìm kiếm người chơi (Tên hoặc ID):"));
        txtPlayerSearch = new JTextField(20);
        txtPlayerSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterPlayers();
            }
        });
        topSearch.add(txtPlayerSearch);

        JButton btnRefreshPlayers = new JButton("Làm Mới Danh Sách");
        btnRefreshPlayers.addActionListener(e -> refreshPlayerList());
        topSearch.add(btnRefreshPlayers);

        panel.add(topSearch, BorderLayout.NORTH);

        // Center: SplitPane (Table left, Details & Actions right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.68);

        String[] columns = {"ID", "Tên Nhân Vật", "User ID", "Giới Tính", "Bản Đồ (Khu)", "Sức Mạnh", "Tiềm Năng", "Vàng", "Ngọc", "Hồng Ngọc", "HP / Max"};
        playerTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        playerTable = new JTable(playerTableModel);
        styleDarkTable(playerTable);
        playerTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        playerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        playerSorter = new TableRowSorter<>(playerTableModel);
        playerTable.setRowSorter(playerSorter);

        playerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onPlayerSelected();
            }
        });

        JScrollPane scrollPane = new JScrollPane(playerTable);
        splitPane.setLeftComponent(scrollPane);

        // Right Detail & Action Panel
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(COLOR_PANEL_DARK);
        rightPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // 1. Thẻ Thông Tin Nhân Vật Đang Chọn
        JPanel pnlPlayerCard = new JPanel(new BorderLayout(8, 8));
        pnlPlayerCard.setBackground(new Color(15, 23, 42)); // Slate 900
        pnlPlayerCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));

        lblPlayerDetail = new JLabel("<html><div style='color:#94a3b8; font-size:12px; line-height:1.6;'>"
            + "<b style='color:#38bdf8; font-size:13px;'>BẢNG THAO TÁC NHÂN VẬT</b><br>"
            + "Chưa chọn người chơi nào từ danh sách.<br>"
            + "<i>Vui lòng chọn 1 người chơi bên trái để thực hiện quản trị.</i>"
            + "</div></html>");
        pnlPlayerCard.add(lblPlayerDetail, BorderLayout.CENTER);
        rightPanel.add(pnlPlayerCard, BorderLayout.NORTH);

        // 2. Thao Tác Phân Nhóm Thẻ
        JPanel pnlActions = new JPanel();
        pnlActions.setLayout(new BoxLayout(pnlActions, BoxLayout.Y_AXIS));
        pnlActions.setOpaque(false);

        // Group 1: Tiền Tệ & Tài Sản
        JButton btnAddGold = createModernButton("+ Vàng", new Color(245, 158, 11), Color.WHITE);
        btnAddGold.addActionListener(e -> handleAddGold());
        JButton btnAddGem = createModernButton("+ Ngọc Xanh", new Color(6, 182, 212), Color.WHITE);
        btnAddGem.addActionListener(e -> handleAddGem());
        JButton btnAddRuby = createModernButton("+ Hồng Ngọc", new Color(244, 63, 94), Color.WHITE);
        btnAddRuby.addActionListener(e -> handleAddRuby());
        pnlActions.add(createActionGroup("TIỀN TỆ & TÀI SẢN", new Color(245, 158, 11), new JButton[]{btnAddGold, btnAddGem, btnAddRuby}));
        pnlActions.add(Box.createVerticalStrut(10));

        // Group 2: Chỉ Số & Hồi Phục
        JButton btnBuffPoint = createModernButton("Buff Sư Phụ", new Color(16, 185, 129), Color.WHITE);
        btnBuffPoint.addActionListener(e -> openBuffPointDialog());
        JButton btnAddPower = createModernButton("+ SM & TN", new Color(168, 85, 247), Color.WHITE);
        btnAddPower.addActionListener(e -> handleAddPower());
        JButton btnHeal = createModernButton("Hồi HP & KI", new Color(59, 130, 246), Color.WHITE);
        btnHeal.addActionListener(e -> handleHealPlayer());
        pnlActions.add(createActionGroup("CHỈ SỐ & SỨC MẠNH", new Color(16, 185, 129), new JButton[]{btnBuffPoint, btnAddPower, btnHeal}));
        pnlActions.add(Box.createVerticalStrut(10));

        // Group 3: Điều Khiển & Dịch Chuyển
        JButton btnTeleport = createModernButton("Dịch Chuyển", new Color(14, 165, 233), Color.WHITE);
        btnTeleport.addActionListener(e -> handleTeleport());
        JButton btnMsg = createModernButton("Gửi Tin Nhắn", new Color(99, 102, 241), Color.WHITE);
        btnMsg.addActionListener(e -> handleSendPlayerMessage());
        JButton btnSelectToBuff = createModernButton("Buff Đồ (Tab 2)", new Color(20, 184, 166), Color.WHITE);
        btnSelectToBuff.addActionListener(e -> handleSelectToBuff());
        pnlActions.add(createActionGroup("ĐIỀU KHIỂN & TƯƠNG TÁC", new Color(14, 165, 233), new JButton[]{btnTeleport, btnMsg, btnSelectToBuff}));
        pnlActions.add(Box.createVerticalStrut(10));

        // Group 4: Quản Trị & An Ninh
        JButton btnSavePl = createModernButton("Lưu Data", new Color(100, 116, 139), Color.WHITE);
        btnSavePl.addActionListener(e -> handleSaveSelectedPlayer());
        JButton btnKick = createModernButton("Kick Thoát", new Color(249, 115, 22), Color.WHITE);
        btnKick.addActionListener(e -> handleKickPlayer());
        JButton btnKickAll = createModernButton("Ngắt Toàn Bộ", new Color(239, 68, 68), Color.WHITE);
        btnKickAll.addActionListener(e -> handleKickAllPlayers());
        pnlActions.add(createActionGroup("QUẢN TRỊ & BẢO MẬT", new Color(239, 68, 68), new JButton[]{btnSavePl, btnKick, btnKickAll}));

        JScrollPane actionScroll = new JScrollPane(pnlActions);
        actionScroll.setBorder(null);
        actionScroll.setOpaque(false);
        actionScroll.getViewport().setOpaque(false);
        rightPanel.add(actionScroll, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        panel.add(splitPane, BorderLayout.CENTER);
        refreshPlayerList();
        return panel;
    }

    private void filterPlayers() {
        String text = txtPlayerSearch.getText().trim();
        if (text.isEmpty()) {
            playerSorter.setRowFilter(null);
        } else {
            playerSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1));
        }
    }

    private void refreshPlayerList() {
        playerTableModel.setRowCount(0);
        selectedPlayer = null;
        if (lblPlayerDetail != null) {
            lblPlayerDetail.setText("<html><div style='padding:8px;'><i>Hãy chọn một người chơi từ danh sách để thao tác...</i></div></html>");
        }

        try {
            List<Player> players = Client.gI().getPlayers();
            for (Player pl : players) {
                if (pl != null && pl.nPoint != null && pl.inventory != null) {
                    String genderStr = switch (pl.gender) {
                        case 0 -> "Trái Đất";
                        case 1 -> "Namếc";
                        case 2 -> "Xayda";
                        default -> "Khác";
                    };
                    String mapStr = (pl.zone != null && pl.zone.map != null) ? (pl.zone.map.mapName + " (" + pl.zone.zoneId + ")") : "Chưa rõ";
                    int userId = (pl.getSession() != null) ? pl.getSession().userId : -1;

                    playerTableModel.addRow(new Object[]{
                        pl.id,
                        pl.name,
                        userId,
                        genderStr,
                        mapStr,
                        numFmt.format(pl.nPoint.power),
                        numFmt.format(pl.nPoint.tiemNang),
                        numFmt.format(pl.inventory.gold),
                        numFmt.format(pl.inventory.gem),
                        numFmt.format(pl.inventory.ruby),
                        numFmt.format(pl.nPoint.hp) + " / " + numFmt.format(pl.nPoint.hpMax)
                    });
                }
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    private void onPlayerSelected() {
        int row = playerTable.getSelectedRow();
        if (row < 0) {
            selectedPlayer = null;
            return;
        }

        int modelRow = playerTable.convertRowIndexToModel(row);
        long id = (long) playerTableModel.getValueAt(modelRow, 0);
        selectedPlayer = Client.gI().getPlayer(id);

        if (selectedPlayer != null && selectedPlayer.nPoint != null && selectedPlayer.inventory != null) {
            String genderStr = switch (selectedPlayer.gender) {
                case 0 -> "Trái Đất";
                case 1 -> "Namếc";
                case 2 -> "Xayda";
                default -> "Khác";
            };
            int userId = (selectedPlayer.getSession() != null) ? selectedPlayer.getSession().userId : -1;
            String mapStr = (selectedPlayer.zone != null && selectedPlayer.zone.map != null) ? selectedPlayer.zone.map.mapName : "N/A";
            int zoneId = (selectedPlayer.zone != null) ? selectedPlayer.zone.zoneId : -1;

            String detailHtml = String.format(
                "<html><div style='color:#e2e8f0; font-size:12px; line-height: 1.6;'>"
                + "<div style='margin-bottom:6px;'>"
                + "  <span style='font-size:14px; font-weight:bold; color:#38bdf8;'>%s</span> "
                + "  <span style='color:#94a3b8;'>(ID: %d | User: %d)</span> "
                + "  <span style='background:#334155; color:#f8fafc; padding:1px 6px; border-radius:4px; font-size:10px;'>%s</span>"
                + "</div>"
                + "<div style='color:#cbd5e1; margin-bottom:4px; font-size:11px;'>"
                + "  <b>Vị trí:</b> <span style='color:#a5f3fc;'>%s</span> (Khu: <span style='color:#fde047;'>%d</span>)"
                + "</div>"
                + "<div style='font-size:11px; margin-bottom:3px;'>"
                + "  <b>HP:</b> <span style='color:#4ade80;'>%s / %s</span> | "
                + "  <b>KI:</b> <span style='color:#60a5fa;'>%s / %s</span>"
                + "</div>"
                + "<div style='font-size:11px; margin-bottom:3px;'>"
                + "  <b>Sức đánh:</b> <span style='color:#f87171;'>%s</span> | "
                + "  <b>Sức mạnh:</b> <span style='color:#c084fc;'>%s</span>"
                + "</div>"
                + "<div style='font-size:11px;'>"
                + "  <span style='color:#fbbf24;'><b>Vàng:</b> %s</span> | "
                + "  <span style='color:#38bdf8;'><b>Ngọc:</b> %s</span> | "
                + "  <span style='color:#fb7185;'><b>Hồng ngọc:</b> %s</span>"
                + "</div>"
                + "</div></html>",
                selectedPlayer.name,
                selectedPlayer.id,
                userId,
                genderStr,
                mapStr,
                zoneId,
                numFmt.format(selectedPlayer.nPoint.hp), numFmt.format(selectedPlayer.nPoint.hpMax),
                numFmt.format(selectedPlayer.nPoint.mp), numFmt.format(selectedPlayer.nPoint.mpMax),
                numFmt.format(selectedPlayer.nPoint.dame),
                numFmt.format(selectedPlayer.nPoint.power),
                numFmt.format(selectedPlayer.inventory.gold),
                numFmt.format(selectedPlayer.inventory.gem),
                numFmt.format(selectedPlayer.inventory.ruby)
            );
            lblPlayerDetail.setText(detailHtml);
            if (txtBuffPlayer != null) {
                txtBuffPlayer.setText(selectedPlayer.name);
            }
        }
    }

    private void handleAddGold() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Nhập số Vàng cần cộng (+) hoặc trừ (-):", "100000000");
        if (input != null && !input.trim().isEmpty()) {
            try {
                long amount = Long.parseLong(input.trim().replace(",", "").replace(".", ""));
                if (amount >= 0) {
                    selectedPlayer.inventory.addGold(amount);
                } else {
                    selectedPlayer.inventory.subGold(Math.abs(amount));
                }
                Service.getInstance().sendMoney(selectedPlayer);
                Service.getInstance().sendThongBao(selectedPlayer, "Tài khoản của bạn vừa được Admin thay đổi " + numFmt.format(amount) + " Vàng!");
                onPlayerSelected();
                refreshPlayerList();
                JOptionPane.showMessageDialog(this, "Cập nhật Vàng thành công cho " + selectedPlayer.name + "!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số vàng không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleAddGem() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Nhập số Ngọc Xanh cần cộng (+) hoặc trừ (-):", "10000");
        if (input != null && !input.trim().isEmpty()) {
            try {
                int amount = Integer.parseInt(input.trim().replace(",", "").replace(".", ""));
                selectedPlayer.inventory.gem += amount;
                if (selectedPlayer.inventory.gem < 0) selectedPlayer.inventory.gem = 0;
                Service.getInstance().sendMoney(selectedPlayer);
                Service.getInstance().sendThongBao(selectedPlayer, "Tài khoản của bạn vừa được Admin thay đổi " + numFmt.format(amount) + " Ngọc Xanh!");
                onPlayerSelected();
                refreshPlayerList();
                JOptionPane.showMessageDialog(this, "Cập nhật Ngọc Xanh thành công cho " + selectedPlayer.name + "!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số ngọc không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleAddRuby() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Nhập số Hồng Ngọc cần cộng (+) hoặc trừ (-):", "10000");
        if (input != null && !input.trim().isEmpty()) {
            try {
                int amount = Integer.parseInt(input.trim().replace(",", "").replace(".", ""));
                selectedPlayer.inventory.ruby += amount;
                if (selectedPlayer.inventory.ruby < 0) selectedPlayer.inventory.ruby = 0;
                Service.getInstance().sendMoney(selectedPlayer);
                Service.getInstance().sendThongBao(selectedPlayer, "Tài khoản của bạn vừa được Admin thay đổi " + numFmt.format(amount) + " Hồng Ngọc!");
                onPlayerSelected();
                refreshPlayerList();
                JOptionPane.showMessageDialog(this, "Cập nhật Hồng Ngọc thành công cho " + selectedPlayer.name + "!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số hồng ngọc không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleAddPower() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Nhập số Sức Mạnh & Tiềm Năng muốn cộng thêm:", "100000000");
        if (input != null && !input.trim().isEmpty()) {
            try {
                long amount = Long.parseLong(input.trim().replace(",", "").replace(".", ""));
                selectedPlayer.nPoint.power += amount;
                selectedPlayer.nPoint.tiemNang += amount;
                Service.getInstance().point(selectedPlayer);
                Service.getInstance().sendThongBao(selectedPlayer, "Bạn vừa được Admin cộng " + numFmt.format(amount) + " Sức mạnh & Tiềm năng!");
                onPlayerSelected();
                refreshPlayerList();
                JOptionPane.showMessageDialog(this, "Cộng Sức Mạnh & Tiềm Năng thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Điểm không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleHealPlayer() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            selectedPlayer.nPoint.setHp(selectedPlayer.nPoint.hpMax);
            selectedPlayer.nPoint.setMp(selectedPlayer.nPoint.mpMax);
            Service.getInstance().hsChar(selectedPlayer, selectedPlayer.nPoint.hpMax, selectedPlayer.nPoint.mpMax);
            Service.getInstance().point(selectedPlayer);
            Service.getInstance().sendThongBao(selectedPlayer, "Bạn đã được Admin hồi sinh đầy HP & KI!");
            onPlayerSelected();
            JOptionPane.showMessageDialog(this, "Đã hồi sinh và hồi đầy HP & KI cho " + selectedPlayer.name + "!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    private void handleTeleport() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel p = new JPanel(new GridLayout(2, 2, 5, 5));
        p.add(new JLabel("Mã Bản Đồ (Map ID):"));
        JTextField txtMap = new JTextField("0");
        p.add(txtMap);

        p.add(new JLabel("Khu Vực (Zone ID):"));
        JTextField txtZone = new JTextField("0");
        p.add(txtZone);

        int result = JOptionPane.showConfirmDialog(this, p, "Dịch Chuyển Người Chơi Đến Bản Đồ", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int mapId = Integer.parseInt(txtMap.getText().trim());
                int zoneId = Integer.parseInt(txtZone.getText().trim());
                ChangeMapService.gI().changeMapBySpaceShip(selectedPlayer, mapId, zoneId, -1);
                Service.getInstance().sendThongBao(selectedPlayer, "Admin vừa dịch chuyển bạn đến Map ID: " + mapId + ", Khu: " + zoneId);
                onPlayerSelected();
                JOptionPane.showMessageDialog(this, "Dịch chuyển thành công " + selectedPlayer.name + " đến Map " + mapId + " (Zone " + zoneId + ")!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Mã Map hoặc Khu không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleSendPlayerMessage() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String msg = JOptionPane.showInputDialog(this, "Nhập nội dung thông báo gửi riêng cho " + selectedPlayer.name + ":");
        if (msg != null && !msg.trim().isEmpty()) {
            Service.getInstance().sendThongBao(selectedPlayer, "[ADMIN]: " + msg.trim());
            JOptionPane.showMessageDialog(this, "Đã gửi thông báo thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleSelectToBuff() {
        if (selectedPlayer != null) {
            txtBuffPlayer.setText(selectedPlayer.name);
            tabbedPane.setSelectedIndex(1);
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người chơi trước!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleKickAllPlayers() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Bạn có chắc chắn muốn ngắt kết nối TOÀN BỘ người chơi trên máy chủ?", 
            "Xác nhận Kick All", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Client.gI().close();
                logConsole("Đã ngắt kết nối toàn bộ người chơi trên máy chủ.");
                refreshPlayerList();
                JOptionPane.showMessageDialog(this, "Đã ngắt kết nối toàn bộ người chơi thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleKickPlayer() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn kick " + selectedPlayer.name + " khỏi game?", "Xác nhận Kick", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (selectedPlayer.getSession() != null) {
                Client.gI().kickSession(selectedPlayer.getSession());
            }
            refreshPlayerList();
            JOptionPane.showMessageDialog(this, "Đã ngắt kết nối người chơi thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleSaveSelectedPlayer() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean ok = PlayerService.gI().savePlayer(selectedPlayer);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Đã lưu dữ liệu cho " + selectedPlayer.name + " thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Lưu dữ liệu thất bại hoặc có lỗi xảy ra!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openBuffPointDialog() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi từ danh sách trước!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final Player target = selectedPlayer;
        final String titleText = "Buff Chỉ Số Nhân Vật (Sư Phụ) - " + target.name + " (ID: " + target.id + ")";

        JDialog dlg = new JDialog(this, titleText, true);
        dlg.setSize(580, 620);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(COLOR_BG_DARK);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Header Card
        JPanel headerCard = new JPanel(new GridLayout(2, 1, 4, 4));
        headerCard.setBackground(COLOR_PANEL_DARK);
        headerCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER_DARK, 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        JLabel lblHeaderTitle = new JLabel("CHỈNH SỬA VÀ BUFF CHỈ SỐ NHÂN VẬT CHÍNH (SƯ PHỤ)");
        lblHeaderTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblHeaderTitle.setForeground(COLOR_ACCENT);

        JLabel lblSub = new JLabel("Nhân vật: " + target.name + " | HP hiện tại: " + numFmt.format(target.nPoint.hpMax) + " | Sức đánh hiện tại: " + numFmt.format(target.nPoint.dame));
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(203, 213, 225));
        headerCard.add(lblHeaderTitle);
        headerCard.add(lblSub);
        mainPanel.add(headerCard);
        mainPanel.add(Box.createVerticalStrut(12));

        // Presets Panel
        JPanel presetPanel = new JPanel(new BorderLayout(5, 5));
        presetPanel.setBackground(COLOR_PANEL_DARK);
        presetPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK), "Gán Nhanh Bộ Chỉ Số (Presets)", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), COLOR_TEXT_LIGHT),
            new EmptyBorder(6, 8, 8, 8)
        ));

        JPanel presetBtns = new JPanel(new GridLayout(1, 4, 6, 6));
        presetBtns.setOpaque(false);

        JButton btnPreTanThu = createModernButton("Tân Thủ", new Color(14, 116, 144), Color.WHITE);
        JButton btnPreVip = createModernButton("VIP Pro", new Color(16, 185, 129), Color.WHITE);
        JButton btnPreSieuCap = createModernButton("Siêu Cấp", new Color(217, 119, 6), Color.WHITE);
        JButton btnPreMax = createModernButton("Max Khủng", new Color(225, 29, 72), Color.WHITE);

        presetBtns.add(btnPreTanThu);
        presetBtns.add(btnPreVip);
        presetBtns.add(btnPreSieuCap);
        presetBtns.add(btnPreMax);
        presetPanel.add(presetBtns, BorderLayout.CENTER);
        mainPanel.add(presetPanel);
        mainPanel.add(Box.createVerticalStrut(12));

        // Form fields
        JPanel formPanel = new JPanel(new GridLayout(8, 2, 8, 8));
        formPanel.setBackground(COLOR_PANEL_DARK);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK), "Thông Số Cơ Bản (Gốc)", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), COLOR_TEXT_LIGHT),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JTextField txtHpg = createDarkTextField(String.valueOf(target.nPoint.hpg));
        JTextField txtMpg = createDarkTextField(String.valueOf(target.nPoint.mpg));
        JTextField txtDameg = createDarkTextField(String.valueOf(target.nPoint.dameg));
        JTextField txtDefg = createDarkTextField(String.valueOf(target.nPoint.defg));
        JTextField txtCritg = createDarkTextField(String.valueOf(target.nPoint.critg));
        JTextField txtLimitPower = createDarkTextField(String.valueOf(target.nPoint.limitPower));
        JTextField txtSpeed = createDarkTextField(String.valueOf(target.nPoint.speed));
        JTextField txtStamina = createDarkTextField(String.valueOf(target.nPoint.stamina));

        addFormField(formPanel, "HP Gốc (hpg):", txtHpg);
        addFormField(formPanel, "KI Gốc (mpg):", txtMpg);
        addFormField(formPanel, "Sức Đánh Gốc (dameg):", txtDameg);
        addFormField(formPanel, "Giáp Gốc (defg):", txtDefg);
        addFormField(formPanel, "Chí Mạng Gốc (%):", txtCritg);
        addFormField(formPanel, "Giới Hạn Sức Mạnh (0-10):", txtLimitPower);
        addFormField(formPanel, "Tốc Độ Di Chuyển (speed):", txtSpeed);
        addFormField(formPanel, "Thể Lực (stamina):", txtStamina);

        mainPanel.add(formPanel);

        // Preset actions
        btnPreTanThu.addActionListener(e -> {
            txtHpg.setText("100000");
            txtMpg.setText("100000");
            txtDameg.setText("5000");
            txtDefg.setText("100");
            txtCritg.setText("10");
            txtLimitPower.setText("1");
            txtSpeed.setText("8");
            txtStamina.setText("2500");
        });

        btnPreVip.addActionListener(e -> {
            txtHpg.setText("1000000");
            txtMpg.setText("1000000");
            txtDameg.setText("50000");
            txtDefg.setText("1000");
            txtCritg.setText("30");
            txtLimitPower.setText("5");
            txtSpeed.setText("10");
            txtStamina.setText("5000");
        });

        btnPreSieuCap.addActionListener(e -> {
            txtHpg.setText("10000000");
            txtMpg.setText("10000000");
            txtDameg.setText("500000");
            txtDefg.setText("5000");
            txtCritg.setText("50");
            txtLimitPower.setText("8");
            txtSpeed.setText("12");
            txtStamina.setText("10000");
        });

        btnPreMax.addActionListener(e -> {
            txtHpg.setText("2000000000");
            txtMpg.setText("2000000000");
            txtDameg.setText("10000000");
            txtDefg.setText("50000");
            txtCritg.setText("100");
            txtLimitPower.setText("10");
            txtSpeed.setText("15");
            txtStamina.setText("30000");
        });

        // Bottom Action buttons
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomBar.setBackground(COLOR_PANEL_DARK);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER_DARK));

        JButton btnClose = createModernButton("Đóng", new Color(71, 85, 105), Color.WHITE);
        btnClose.addActionListener(e -> dlg.dispose());

        JButton btnApply = createModernButton("CẬP NHẬT CHỈ SỐ NGAY", new Color(16, 185, 129), Color.WHITE);
        btnApply.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnApply.addActionListener(e -> {
            try {
                int hpg = Integer.parseInt(txtHpg.getText().trim().replace(",", "").replace(".", ""));
                int mpg = Integer.parseInt(txtMpg.getText().trim().replace(",", "").replace(".", ""));
                int dameg = Integer.parseInt(txtDameg.getText().trim().replace(",", "").replace(".", ""));
                int defg = Integer.parseInt(txtDefg.getText().trim().replace(",", "").replace(".", ""));
                int critg = Integer.parseInt(txtCritg.getText().trim().replace(",", "").replace(".", ""));
                byte limitPower = Byte.parseByte(txtLimitPower.getText().trim());
                byte speed = Byte.parseByte(txtSpeed.getText().trim());
                short stamina = Short.parseShort(txtStamina.getText().trim().replace(",", "").replace(".", ""));

                target.nPoint.hpg = hpg;
                target.nPoint.mpg = mpg;
                target.nPoint.dameg = dameg;
                target.nPoint.defg = defg;
                target.nPoint.critg = critg;
                target.nPoint.limitPower = limitPower;
                target.nPoint.speed = speed;
                target.nPoint.stamina = stamina;
                target.nPoint.maxStamina = stamina;

                target.nPoint.calPoint();
                Service.getInstance().point(target);
                PlayerService.gI().sendInfoHpMpMoney(target);
                Service.getInstance().sendThongBao(target, "Chỉ số nhân vật của bạn vừa được Admin nâng cấp!");
                PlayerService.gI().savePlayer(target);

                onPlayerSelected();
                refreshPlayerList();
                JOptionPane.showMessageDialog(dlg, "Đã cập nhật chỉ số thành công cho " + target.name + "!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Lỗi định dạng dữ liệu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        bottomBar.add(btnClose);
        bottomBar.add(btnApply);

        dlg.add(new JScrollPane(mainPanel), BorderLayout.CENTER);
        dlg.add(bottomBar, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void saveAllPlayers() {
        try {
            ServerManager.gI().saveAll(false);
            JOptionPane.showMessageDialog(this, "Đã phát lệnh lưu toàn bộ dữ liệu người chơi vào CSDL thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu dữ liệu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // 3. TAB QUẢN LÝ VẬT PHẨM (THÊM, SỬA, XÓA & BUFF)
    // ==========================================
    private JPanel createItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Filter & CRUD Bar
        JPanel topBar = new JPanel(new BorderLayout(10, 5));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        searchPanel.add(new JLabel("Tìm kiếm (Tên/ID):"));
        txtItemSearch = new JTextField(18);
        txtItemSearch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtItemSearch.setBackground(new Color(15, 23, 42));
        txtItemSearch.setForeground(Color.WHITE);
        txtItemSearch.setCaretColor(Color.WHITE);
        txtItemSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(56, 189, 248), 1, true),
            new EmptyBorder(5, 8, 5, 8)
        ));
        txtItemSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterItems();
            }
        });
        searchPanel.add(txtItemSearch);

        JButton btnReloadTemplates = createModernButton("Nạp Lại CSDL", new Color(51, 65, 85), Color.WHITE);
        btnReloadTemplates.addActionListener(e -> {
            reloadItemTemplatesFromDB();
            loadItemTemplatesToTable();
        });
        searchPanel.add(btnReloadTemplates);

        topBar.add(searchPanel, BorderLayout.WEST);

        // CRUD Action Buttons
        JPanel crudBtnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));

        JButton btnAddItem = createModernButton("Thêm Item Mới", new Color(40, 167, 69), Color.WHITE);
        btnAddItem.addActionListener(e -> openItemTemplateDialog(null));
        crudBtnBar.add(btnAddItem);

        JButton btnEditItem = createModernButton("Sửa Item Đang Chọn", new Color(217, 119, 6), Color.WHITE);
        btnEditItem.addActionListener(e -> handleEditSelectedItem());
        crudBtnBar.add(btnEditItem);

        JButton btnDeleteItemDB = createModernButton("Xóa Khỏi CSDL", new Color(225, 29, 72), Color.WHITE);
        btnDeleteItemDB.addActionListener(e -> handleDeleteSelectedItemFromDB());
        crudBtnBar.add(btnDeleteItemDB);

        JButton btnManageIcon = createModernButton("Quản Lý Icon", new Color(111, 66, 193), Color.WHITE);
        btnManageIcon.addActionListener(e -> openIconManagerDialog());
        crudBtnBar.add(btnManageIcon);

        topBar.add(crudBtnBar, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        // Center Split: Item Table left, Preview & Buff Form right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.58);

        String[] itemCols = {"ID Template", "Tên Vật Phẩm", "Loại (Type)", "Giới Tính", "Mô Tả", "Chỉ Số (Options)"};
        itemTableModel = new DefaultTableModel(itemCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        itemTable = new JTable(itemTableModel);
        styleDarkTable(itemTable);
        itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        itemSorter = new TableRowSorter<>(itemTableModel);
        itemTable.setRowSorter(itemSorter);

        itemTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        itemTable.getColumnModel().getColumn(4).setPreferredWidth(300);

        JScrollPane scrollPane = new JScrollPane(itemTable);
        splitPane.setLeftComponent(scrollPane);

        // Right Side: Live Item Preview Card + Buff Form
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(COLOR_PANEL_DARK);
        rightPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        // 1. LIVE ITEM PREVIEW CARD ("Xem trước vật phẩm tìm kiếm")
        JPanel pnlPreviewCard = new JPanel(new BorderLayout(8, 6));
        pnlPreviewCard.setBackground(new Color(15, 23, 42)); // Slate 900
        pnlPreviewCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248), 1, true),
                "XEM TRƯỚC VẬT PHẨM (TOOLTIP PREVIEW)",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(56, 189, 248)
            ),
            new EmptyBorder(8, 12, 10, 12)
        ));

        // Header of Preview: Icon left, Titles right
        JPanel pnlPrevHead = new JPanel(new BorderLayout(10, 0));
        pnlPrevHead.setOpaque(false);

        lblPreviewItemIcon = new JLabel();
        lblPreviewItemIcon.setPreferredSize(new Dimension(54, 54));
        lblPreviewItemIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lblPreviewItemIcon.setBorder(BorderFactory.createLineBorder(new Color(56, 189, 248), 1, true));
        pnlPrevHead.add(lblPreviewItemIcon, BorderLayout.WEST);

        JPanel pnlPrevTitleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlPrevTitleBox.setOpaque(false);
        lblPreviewItemTitle = new JLabel("<html><b style='color:#38bdf8;font-size:14px;'>Hãy chọn một vật phẩm</b></html>");
        lblPreviewItemBadges = new JLabel("<html><span style='color:#94a3b8;font-size:11px;'>Chọn từ bảng hoặc gõ tìm kiếm để xem trực quan</span></html>");
        pnlPrevTitleBox.add(lblPreviewItemTitle);
        pnlPrevTitleBox.add(lblPreviewItemBadges);
        pnlPrevHead.add(pnlPrevTitleBox, BorderLayout.CENTER);
        pnlPreviewCard.add(pnlPrevHead, BorderLayout.NORTH);

        // Body of Preview: Part Cải Trang + Description + Options Area
        JPanel pnlPrevBody = new JPanel();
        pnlPrevBody.setLayout(new BoxLayout(pnlPrevBody, BoxLayout.Y_AXIS));
        pnlPrevBody.setOpaque(false);
        pnlPrevBody.setBorder(new EmptyBorder(6, 0, 6, 0));

        lblPreviewItemPart = new JLabel("");
        lblPreviewItemPart.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblPreviewItemPart.setForeground(new Color(251, 146, 60));
        lblPreviewItemPart.setVisible(false);
        pnlPrevBody.add(lblPreviewItemPart);

        lblPreviewItemDesc = new JLabel("<html><span style='color:#cbd5e1;font-size:11px;font-style:italic;'>Mô tả vật phẩm...</span></html>");
        lblPreviewItemDesc.setBorder(new EmptyBorder(3, 0, 4, 0));
        pnlPrevBody.add(lblPreviewItemDesc);

        JLabel lblOptsTitle = new JLabel("Chỉ số Option trong game:");
        lblOptsTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblOptsTitle.setForeground(new Color(52, 211, 153));
        lblOptsTitle.setBorder(new EmptyBorder(4, 0, 2, 0));
        pnlPrevBody.add(lblOptsTitle);

        epPreviewOptions = new JEditorPane();
        epPreviewOptions.setContentType("text/html");
        epPreviewOptions.setEditable(false);
        epPreviewOptions.setBackground(new Color(15, 23, 42));
        epPreviewOptions.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        epPreviewOptions.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JScrollPane spOpts = new JScrollPane(epPreviewOptions);
        spOpts.setPreferredSize(new Dimension(300, 110));
        spOpts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        spOpts.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));
        pnlPrevBody.add(spOpts);

        pnlPreviewCard.add(pnlPrevBody, BorderLayout.CENTER);

        // Quick Actions Bar on Preview Card
        JPanel pnlPrevActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlPrevActions.setOpaque(false);

        btnPreviewEdit = createModernButton("Sửa Mẫu Này", new Color(14, 116, 144), Color.WHITE);
        btnPreviewEdit.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnPreviewEdit.setEnabled(false);
        btnPreviewEdit.addActionListener(e -> handleEditSelectedItem());
        pnlPrevActions.add(btnPreviewEdit);

        btnPreviewDelete = createModernButton("Xóa Khỏi CSDL", new Color(225, 29, 72), Color.WHITE);
        btnPreviewDelete.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnPreviewDelete.setEnabled(false);
        btnPreviewDelete.addActionListener(e -> handleDeleteSelectedItemFromDB());
        pnlPrevActions.add(btnPreviewDelete);

        pnlPreviewCard.add(pnlPrevActions, BorderLayout.SOUTH);
        rightPanel.add(pnlPreviewCard);

        rightPanel.add(Box.createVerticalStrut(8));

        // 2. BUFF FORM (High contrast typography)
        JPanel formCenter = new JPanel(new GridBagLayout());
        formCenter.setBackground(COLOR_PANEL_DARK);
        formCenter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DARK, 1, true),
                "Buff Vật Phẩm Vào Hành Trang Người Chơi",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                COLOR_ACCENT
            ),
            new EmptyBorder(8, 10, 8, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 4, 5, 4);

        txtBuffPlayer = createDarkTextField("");
        txtBuffQuantity = createDarkTextField("1");

        String[] presets = {
            "Mặc định theo template",
            "50 - Tăng Sức Đánh (%)",
            "77 - Tăng HP (%)",
            "103 - Tăng KI (%)",
            "14 - Tăng Chí Mạng (%)",
            "93 - Hạn Sử Dụng (Ngày)",
            "30 - Không thể giao dịch",
            "73 - Tự động hồi phục HP",
            "Tùy chỉnh (Nhập ID bên dưới)"
        };
        cbPresetOption = new JComboBox<>(presets);
        cbPresetOption.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbPresetOption.setBackground(new Color(15, 23, 42));
        cbPresetOption.setForeground(Color.WHITE);

        txtCustomOptId = createDarkTextField("50");
        txtCustomOptId.setEnabled(false);
        txtOptionParam = createDarkTextField("20");

        cbPresetOption.addActionListener(e -> {
            boolean isCustom = cbPresetOption.getSelectedIndex() == 8;
            txtCustomOptId.setEnabled(isCustom);
        });

        JLabel l1 = new JLabel("Người nhận (Tên nhân vật):");
        l1.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l1.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; formCenter.add(l1, gbc);
        gbc.gridx = 1; formCenter.add(txtBuffPlayer, gbc);

        JLabel l2 = new JLabel("Số lượng buff:");
        l2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l2.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 1; formCenter.add(l2, gbc);
        gbc.gridx = 1; formCenter.add(txtBuffQuantity, gbc);

        JLabel l3 = new JLabel("Bộ chỉ số (Preset):");
        l3.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l3.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 2; formCenter.add(l3, gbc);
        gbc.gridx = 1; formCenter.add(cbPresetOption, gbc);

        JPanel pnlCustomOpt = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlCustomOpt.setOpaque(false);
        JLabel lOpt = new JLabel("ID:");
        lOpt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lOpt.setForeground(Color.WHITE);
        pnlCustomOpt.add(lOpt);
        txtCustomOptId.setPreferredSize(new Dimension(60, 28));
        pnlCustomOpt.add(txtCustomOptId);
        JLabel lPrm = new JLabel("Param:");
        lPrm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lPrm.setForeground(Color.WHITE);
        pnlCustomOpt.add(lPrm);
        txtOptionParam.setPreferredSize(new Dimension(70, 28));
        pnlCustomOpt.add(txtOptionParam);

        JLabel l4 = new JLabel("Chỉ số tùy chỉnh:");
        l4.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l4.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 3; formCenter.add(l4, gbc);
        gbc.gridx = 1; formCenter.add(pnlCustomOpt, gbc);

        JPanel p5 = new JPanel(new GridLayout(2, 1, 6, 6));
        p5.setOpaque(false);
        p5.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton btnBuff = createModernButton("BUFF VÀO HÀNH TRANG NGAY", new Color(37, 99, 235), Color.WHITE);
        btnBuff.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBuff.addActionListener(e -> handleBuffItem());
        p5.add(btnBuff);

        JButton btnDeleteItem = createModernButton("Thu Hồi Khỏi Túi Đồ", new Color(225, 29, 72), Color.WHITE);
        btnDeleteItem.addActionListener(e -> handleDeleteItemFromBag());
        p5.add(btnDeleteItem);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        formCenter.add(p5, gbc);

        rightPanel.add(formCenter);

        JScrollPane spRight = new JScrollPane(rightPanel);
        spRight.setBorder(null);
        spRight.getVerticalScrollBar().setUnitIncrement(16);
        splitPane.setRightComponent(spRight);
        splitPane.setDividerLocation(780);
        panel.add(splitPane, BorderLayout.CENTER);

        // Selection listener on item table
        itemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedItemPreview();
            }
        });

        loadItemTemplatesToTable();
        return panel;
    }

    private void updateSelectedItemPreview() {
        int r = itemTable.getSelectedRow();
        if (r < 0) {
            clearItemPreview();
            return;
        }
        int mRow = itemTable.convertRowIndexToModel(r);
        int tempId = Integer.parseInt(String.valueOf(itemTableModel.getValueAt(mRow, 0)));
        String iName = (String) itemTableModel.getValueAt(mRow, 1);
        String iDesc = (String) itemTableModel.getValueAt(mRow, 4);

        ItemTemplate it = nro.services.ItemService.gI().getTemplate(tempId);
        short iconId = (it != null) ? it.iconID : 0;
        int typeVal = (it != null) ? it.type : 0;
        int genderVal = (it != null) ? it.gender : 3;
        int powerReq = (it != null) ? it.strRequire : 0;
        boolean stackable = (it != null) && it.isUpToUp;

        // Icon
        File f = new File("resources/normal/image/2/icon/" + iconId + ".png");
        if (!f.exists()) f = new File("resources/normal/image/1/icon/" + iconId + ".png");
        if (f.exists()) {
            ImageIcon ic = new ImageIcon(f.getAbsolutePath());
            lblPreviewItemIcon.setIcon(new ImageIcon(ic.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH)));
            lblPreviewItemIcon.setText("");
        } else {
            lblPreviewItemIcon.setIcon(null);
            lblPreviewItemIcon.setText("<html><b style='color:#94a3b8;'>#" + iconId + "</b></html>");
        }

        // Title with type/rarity color
        String titleColor = (typeVal == 5) ? "#c084fc" : (typeVal <= 4 ? "#38bdf8" : (typeVal == 12 ? "#fbbf24" : "#34d399"));
        lblPreviewItemTitle.setText("<html><span style='color:" + titleColor + "; font-size:14px; font-weight:bold;'>[" + tempId + "] " + iName + "</span></html>");

        String genderStr = (genderVal == 0) ? "Trái Đất" : (genderVal == 1 ? "Namếc" : (genderVal == 2 ? "Xayda" : "Chung"));
        String typeName = getTypeName(typeVal);
        String badgesHtml = "<html><span style='color:#38bdf8; font-weight:bold;'>[" + typeName + "]</span> "
                          + "<span style='color:#a855f7; font-weight:bold;'>[" + genderStr + "]</span> "
                          + (powerReq > 0 ? "<span style='color:#fbbf24; font-weight:bold;'>[SM: " + String.format("%,d", powerReq) + "]</span> " : "")
                          + (stackable ? "<span style='color:#34d399; font-weight:bold;'>[Cộng dồn]</span>" : "")
                          + "</html>";
        lblPreviewItemBadges.setText(badgesHtml);

        // Part Cải Trang
        if (typeVal == 5) {
            CaiTrang ct = Manager.getCaiTrangByItemId(tempId);
            int h = -1, b = -1, l = -1, bg = -1;
            if (ct != null && ct.getID() != null && ct.getID().length >= 3) {
                h = ct.getID()[0];
                b = ct.getID()[1];
                l = ct.getID()[2];
                if (ct.getID().length >= 4) bg = ct.getID()[3];
            }
            lblPreviewItemPart.setText("<html><span style='color:#fb923c; font-size:11px; font-weight:bold;'>Part Cải Trang: Đầu: " + h + " | Thân: " + b + " | Chân: " + l + " | Bag: " + bg + "</span></html>");
            lblPreviewItemPart.setVisible(true);
        } else {
            lblPreviewItemPart.setVisible(false);
        }

        // Description
        lblPreviewItemDesc.setText("<html><span style='color:#cbd5e1; font-style:italic; font-size:11px;'>\"" + (iDesc != null && !iDesc.isEmpty() ? iDesc : "Không có mô tả") + "\"</span></html>");

        // Options List HTML
        StringBuilder sbOpts = new StringBuilder();
        sbOpts.append("<html><body style='font-family:Segoe UI; font-size:11px; color:#ffffff; background:#0f172a; margin:4px;'>");
        if (it != null && it.itemOptions != null && !it.itemOptions.isEmpty()) {
            for (ItemOption io : it.itemOptions) {
                if (io != null && io.optionTemplate != null) {
                    int oId = io.optionTemplate.id;
                    String oColor = (oId == 50 || oId == 0 || oId == 95 || oId == 96) ? "#fbbf24"
                                  : (oId == 77 || oId == 103 || oId == 73) ? "#34d399"
                                  : (oId == 14 || oId == 5) ? "#f43f5e"
                                  : (oId == 93 || oId == 30) ? "#c084fc" : "#38bdf8";
                    sbOpts.append("<div style='margin-bottom:3px;'>")
                          .append("<span style='color:").append(oColor).append("; font-weight:bold;'>• ")
                          .append(io.getOptionString())
                          .append("</span></div>");
                }
            }
        } else {
            sbOpts.append("<div style='color:#64748b; font-style:italic;'>(Không có chỉ số option)</div>");
        }
        sbOpts.append("</body></html>");
        epPreviewOptions.setText(sbOpts.toString());
        epPreviewOptions.setCaretPosition(0);

        btnPreviewEdit.setEnabled(true);
        btnPreviewDelete.setEnabled(true);
    }

    private void clearItemPreview() {
        if (lblPreviewItemIcon != null) {
            lblPreviewItemIcon.setIcon(null);
            lblPreviewItemIcon.setText("<html><b style='color:#64748b;'>N/A</b></html>");
        }
        if (lblPreviewItemTitle != null) {
            lblPreviewItemTitle.setText("<html><b style='color:#64748b; font-size:13px;'>Chưa chọn vật phẩm</b></html>");
        }
        if (lblPreviewItemBadges != null) {
            lblPreviewItemBadges.setText("<html><span style='color:#475569;'>Hãy chọn hoặc tìm kiếm vật phẩm</span></html>");
        }
        if (lblPreviewItemPart != null) {
            lblPreviewItemPart.setVisible(false);
        }
        if (lblPreviewItemDesc != null) {
            lblPreviewItemDesc.setText("<html><span style='color:#475569; font-style:italic;'>Không có dữ liệu</span></html>");
        }
        if (epPreviewOptions != null) {
            epPreviewOptions.setText("<html><body style='font-family:Segoe UI; font-size:11px; color:#64748b; background:#0f172a; margin:4px;'>(Chưa chọn vật phẩm)</body></html>");
        }
        if (btnPreviewEdit != null) btnPreviewEdit.setEnabled(false);
        if (btnPreviewDelete != null) btnPreviewDelete.setEnabled(false);
    }

    private void loadItemTemplatesToTable() {
        itemTableModel.setRowCount(0);
        try {
            for (ItemTemplate temp : Manager.ITEM_TEMPLATES) {
                if (temp != null) {
                    String genderStr = switch (temp.gender) {
                        case 0 -> "Trái Đất";
                        case 1 -> "Namếc";
                        case 2 -> "Xayda";
                        default -> "Chung";
                    };
                    StringBuilder optStr = new StringBuilder();
                    if (temp.itemOptions != null) {
                        for (ItemOption io : temp.itemOptions) {
                            if (optStr.length() > 0) optStr.append(", ");
                            optStr.append(io.getOptionString());
                        }
                    }
                    itemTableModel.addRow(new Object[]{
                        temp.id,
                        temp.name,
                        temp.type,
                        genderStr,
                        temp.description != null ? temp.description : "",
                        optStr.toString()
                    });
                }
            }
            if (itemTable.getRowCount() > 0) {
                itemTable.setRowSelectionInterval(0, 0);
                updateSelectedItemPreview();
            } else {
                clearItemPreview();
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    public static void reloadItemTemplatesFromDB() {
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM item_template ORDER BY id ASC");
             ResultSet rs = ps.executeQuery()) {
            List<ItemTemplate> tempList = new ArrayList<>();
            int maxId = 0;
            while (rs.next()) {
                ItemTemplate itemTemp = new ItemTemplate();
                itemTemp.id = rs.getShort("id");
                itemTemp.type = rs.getByte("type");
                itemTemp.gender = rs.getByte("gender");
                itemTemp.name = rs.getString("name");
                itemTemp.description = rs.getString("description");
                itemTemp.iconID = rs.getShort("icon_id");
                itemTemp.part = rs.getShort("part");
                itemTemp.isUpToUp = rs.getBoolean("is_up_to_up");
                itemTemp.strRequire = rs.getInt("power_require");
                tempList.add(itemTemp);
                if (itemTemp.id > maxId) {
                    maxId = itemTemp.id;
                }
            }

            List<ItemTemplate> fullList = new ArrayList<>(maxId + 1);
            for (int i = 0; i <= maxId; i++) {
                fullList.add(null);
            }
            for (ItemTemplate t : tempList) {
                if (t.id >= 0 && t.id < fullList.size()) {
                    fullList.set(t.id, t);
                }
            }
            // Fill gaps with dummy
            for (int i = 0; i < fullList.size(); i++) {
                if (fullList.get(i) == null) {
                    ItemTemplate dummy = new ItemTemplate();
                    dummy.id = (short) i;
                    dummy.name = "Unknown #" + i;
                    fullList.set(i, dummy);
                }
            }

            synchronized (Manager.ITEM_TEMPLATES) {
                Manager.ITEM_TEMPLATES.clear();
                Manager.ITEM_TEMPLATES.addAll(fullList);
            }

            try (PreparedStatement psOpt = con.prepareStatement("SELECT template_id, option_id, param FROM item_template_option");
                 ResultSet rsOpt = psOpt.executeQuery()) {
                while (rsOpt.next()) {
                    int tempId = rsOpt.getInt("template_id");
                    int optId = rsOpt.getInt("option_id");
                    int param = rsOpt.getInt("param");
                    if (tempId >= 0 && tempId < Manager.ITEM_TEMPLATES.size()) {
                        ItemTemplate it = Manager.ITEM_TEMPLATES.get(tempId);
                        if (it != null && it.itemOptions != null) {
                            it.itemOptions.add(new ItemOption(optId, param));
                        }
                    }
                }
            } catch (Exception exOpt) {
                Log.error(AdminPanelFrame.class, exOpt);
            }

            // Reload cai_trang tu CSDL vao bo nho Server
            try (PreparedStatement psCt = con.prepareStatement("SELECT * FROM cai_trang");
                 ResultSet rsCt = psCt.executeQuery()) {
                List<CaiTrang> newCts = new ArrayList<>();
                while (rsCt.next()) {
                    newCts.add(new CaiTrang(rsCt.getInt("id_temp"),
                            rsCt.getInt("head"), rsCt.getInt("body"), rsCt.getInt("leg"), rsCt.getInt("bag")));
                }
                synchronized (Manager.CAI_TRANGS) {
                    Manager.CAI_TRANGS.clear();
                    Manager.CAI_TRANGS.addAll(newCts);
                }
                Log.success("Reload cai trang thanh cong (" + Manager.CAI_TRANGS.size() + " cai trang)");
            } catch (Exception exCt) {
                Log.error(AdminPanelFrame.class, exCt);
            }

            Log.success("Reload item template thanh cong (" + Manager.ITEM_TEMPLATES.size() + " templates)");
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    private void filterItems() {
        String text = txtItemSearch.getText().trim();
        if (text.isEmpty()) {
            itemSorter.setRowFilter(null);
        } else {
            itemSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1));
        }
        if (itemTable.getRowCount() > 0) {
            itemTable.setRowSelectionInterval(0, 0);
            updateSelectedItemPreview();
        } else {
            clearItemPreview();
        }
    }

    private void handleBuffItem() {
        String plName = txtBuffPlayer.getText().trim();
        if (plName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên người chơi nhận vật phẩm!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Player target = Client.gI().getPlayer(plName);
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy người chơi '" + plName + "' đang online!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int row = itemTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 vật phẩm từ bảng bên trái!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = itemTable.convertRowIndexToModel(row);
        short tempId = (short) itemTableModel.getValueAt(modelRow, 0);

        int quantity = 1;
        try {
            quantity = Integer.parseInt(txtBuffQuantity.getText().trim());
            if (quantity <= 0) quantity = 1;
        } catch (Exception ignored) {
        }

        if (InventoryService.gI().getCountEmptyBag(target) <= 0) {
            JOptionPane.showMessageDialog(this, "Hành trang của " + target.name + " đã đầy, không thể buff thêm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Item item = ItemService.gI().createNewItem(tempId, quantity);

        int presetIdx = cbPresetOption.getSelectedIndex();
        int param = 0;
        try {
            param = Integer.parseInt(txtOptionParam.getText().trim());
        } catch (Exception ignored) {
        }

        if (presetIdx > 0 && presetIdx < 8) {
            int optId = switch (presetIdx) {
                case 1 -> 50;
                case 2 -> 77;
                case 3 -> 103;
                case 4 -> 14;
                case 5 -> 93;
                case 6 -> 30;
                case 7 -> 73;
                default -> -1;
            };
            if (optId != -1) {
                item.itemOptions.add(new ItemOption(optId, param));
            }
        } else if (presetIdx == 8) {
            try {
                int optId = Integer.parseInt(txtCustomOptId.getText().trim());
                item.itemOptions.add(new ItemOption(optId, param));
            } catch (Exception ignored) {
            }
        }

        InventoryService.gI().addItemBag(target, item, 99);
        InventoryService.gI().sendItemBags(target);
        Service.getInstance().sendThongBao(target, "Bạn vừa nhận được " + item.template.name + " x" + quantity + " từ Admin!");

        JOptionPane.showMessageDialog(this, "Buff thành công " + item.template.name + " x" + quantity + " cho " + target.name + "!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleDeleteItemFromBag() {
        String plName = txtBuffPlayer.getText().trim();
        if (plName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên người chơi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Player target = Client.gI().getPlayer(plName);
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy người chơi '" + plName + "' đang online!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int row = itemTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn vật phẩm cần xóa từ bảng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = itemTable.convertRowIndexToModel(row);
        short tempId = (short) itemTableModel.getValueAt(modelRow, 0);

        Item inBag = InventoryService.gI().findItemBagByTemp(target, tempId);
        if (inBag == null) {
            JOptionPane.showMessageDialog(this, "Người chơi không có vật phẩm này trong túi!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int quantity = 1;
        try {
            quantity = Integer.parseInt(txtBuffQuantity.getText().trim());
        } catch (Exception ignored) {
        }

        InventoryService.gI().subQuantityItemsBag(target, inBag, quantity);
        InventoryService.gI().sendItemBags(target);
        Service.getInstance().sendThongBao(target, "Admin đã thu hồi " + inBag.template.name + " x" + quantity + " khỏi túi đồ của bạn.");

        JOptionPane.showMessageDialog(this, "Đã thu hồi vật phẩm khỏi túi đồ của " + target.name + " thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // CRUD ITEM TEMPLATES (DATABASE)
    private void openItemTemplateDialog(ItemTemplate editItem) {
        isDialogOpen = true;
        JDialog dlg = new JDialog(this, editItem == null ? "Thêm Vật Phẩm Mới Vào CSDL" : "Chỉnh Sửa Vật Phẩm (ID: " + editItem.id + ")", true);
        dlg.setSize(1020, 720);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(COLOR_BG_DARK);
        dlg.setLayout(new BorderLayout(8, 8));

        dlg.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                isDialogOpen = false;
            }
            @Override
            public void windowClosing(WindowEvent e) {
                isDialogOpen = false;
            }
        });

        // LEFT PANEL: THÔNG TIN CƠ BẢN
        JPanel formLeft = new JPanel(new GridBagLayout());
        formLeft.setOpaque(true);
        formLeft.setBackground(COLOR_PANEL_DARK);
        formLeft.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK), "Thông Tin Cơ Bản Vật Phẩm", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 13), COLOR_ACCENT),
            new EmptyBorder(12, 14, 12, 14)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Auto ID
        int nextId = 0;
        if (editItem == null) {
            try (Connection con = DBService.gI().getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM item_template")) {
                if (rs.next()) nextId = rs.getInt(1);
            } catch (Exception ignored) {
            }
        }

        JTextField txtId = createDarkTextField(editItem == null ? String.valueOf(nextId) : String.valueOf(editItem.id));
        txtId.setEnabled(editItem == null);
        JTextField txtName = createDarkTextField(editItem == null ? "" : editItem.name);

        String[] types = {
            "0 - Áo (Shirt)", "1 - Quần (Pants)", "2 - Găng tay (Glove)", "3 - Giày (Shoes)",
            "4 - Ra-đa (Radar)", "5 - Cải trang (Costume)", "6 - Đậu thần (Magic Bean)",
            "7 - Sách kỹ năng (Skill Book)", "11 - Đeo lưng / Phụ kiện", "12 - Ngọc Rồng (Dragon Ball)",
            "14 - Bình nước / Thuốc lá", "21 - Pet / Đệ tử", "23 - Ván bay (Flying board)",
            "27 - Hộp quà (Giftbox)", "29 - Máy quét / Dò Boss", "30 - Đá nâng cấp", "72 - Linh thú", "99 - Đặc biệt"
        };
        JComboBox<String> cbType = new JComboBox<>(types);
        cbType.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbType.setBackground(new Color(15, 23, 42));
        cbType.setForeground(Color.WHITE);
        if (editItem != null) {
            for (int i = 0; i < types.length; i++) {
                if (types[i].startsWith(editItem.type + " -")) {
                    cbType.setSelectedIndex(i);
                    break;
                }
            }
        }

        String[] genders = {"0 - Trái Đất", "1 - Namếc", "2 - Xayda", "3 - Cả 3 Tộc / Chung"};
        JComboBox<String> cbGender = new JComboBox<>(genders);
        cbGender.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbGender.setBackground(new Color(15, 23, 42));
        cbGender.setForeground(Color.WHITE);
        if (editItem != null && editItem.gender >= 0 && editItem.gender < genders.length) {
            cbGender.setSelectedIndex(editItem.gender);
        } else if (editItem == null) {
            cbGender.setSelectedIndex(3);
        }

        JTextArea txtDesc = new JTextArea(editItem == null ? "" : editItem.description, 3, 16);
        txtDesc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtDesc.setBackground(new Color(15, 23, 42));
        txtDesc.setForeground(Color.WHITE);
        txtDesc.setCaretColor(Color.WHITE);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBorder(new EmptyBorder(6, 8, 6, 8));

        JTextField txtIcon = createDarkTextField(editItem == null ? "0" : String.valueOf(editItem.iconID));
        JTextField txtPart = createDarkTextField(editItem == null ? "-1" : String.valueOf(editItem.part));
        JTextField txtPower = createDarkTextField(editItem == null ? "0" : String.valueOf(editItem.strRequire));
        JCheckBox chkStack = new JCheckBox("Có thể cộng dồn số lượng (is_up_to_up)", editItem != null && editItem.isUpToUp);
        chkStack.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkStack.setForeground(COLOR_TEXT_LIGHT);
        chkStack.setOpaque(false);

        // Add components to formLeft
        addFormField(formLeft, gbc, "Mã Template ID:", txtId);
        addFormField(formLeft, gbc, "Tên Vật Phẩm:", txtName);
        addFormField(formLeft, gbc, "Phân Loại (Type):", cbType);
        addFormField(formLeft, gbc, "Giới Tính:", cbGender);

        // Icon row with preview
        gbc.gridx = 0; gbc.gridy++;
        JLabel lblIconText = new JLabel("Icon ID:");
        lblIconText.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblIconText.setForeground(Color.WHITE);
        formLeft.add(lblIconText, gbc);

        gbc.gridx = 1;
        JPanel pnlIconRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlIconRow.setOpaque(false);
        txtIcon.setPreferredSize(new Dimension(80, 30));
        pnlIconRow.add(txtIcon);

        JLabel lblIconPreview = new JLabel();
        lblIconPreview.setPreferredSize(new Dimension(38, 38));
        lblIconPreview.setBorder(BorderFactory.createLineBorder(COLOR_ACCENT, 1));
        lblIconPreview.setHorizontalAlignment(SwingConstants.CENTER);
        pnlIconRow.add(lblIconPreview);

        Runnable updateIconPreview = () -> {
            try {
                int id = Integer.parseInt(txtIcon.getText().trim());
                File f = new File("resources/normal/image/2/icon/" + id + ".png");
                if (!f.exists()) f = new File("resources/normal/image/1/icon/" + id + ".png");
                if (f.exists()) {
                    ImageIcon ic = new ImageIcon(f.getAbsolutePath());
                    Image img = ic.getImage().getScaledInstance(34, 34, Image.SCALE_SMOOTH);
                    lblIconPreview.setIcon(new ImageIcon(img));
                    lblIconPreview.setText("");
                } else {
                    lblIconPreview.setIcon(null);
                    lblIconPreview.setText("N/A");
                }
            } catch (Exception ex) {
                lblIconPreview.setIcon(null);
                lblIconPreview.setText("N/A");
            }
        };
        updateIconPreview.run();

        txtIcon.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { updateIconPreview.run(); }
        });
        txtIcon.addActionListener(e -> updateIconPreview.run());

        JButton btnChangeIconImg = createModernButton("Thay PNG...", new Color(71, 85, 105), Color.WHITE);
        btnChangeIconImg.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnChangeIconImg.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn file ảnh PNG làm Icon");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Hình ảnh PNG (*.png)", "png"));
            int r = chooser.showOpenDialog(dlg);
            if (r == JFileChooser.APPROVE_OPTION) {
                try {
                    File sel = chooser.getSelectedFile();
                    int iconId = Integer.parseInt(txtIcon.getText().trim());
                    byte[] bytes = java.nio.file.Files.readAllBytes(sel.toPath());
                    for (int z = 1; z <= 4; z++) {
                        File dir = new File("resources/normal/image/" + z + "/icon");
                        if (!dir.exists()) dir.mkdirs();
                        File f = new File(dir, iconId + ".png");
                        java.nio.file.Files.write(f.toPath(), bytes);
                    }
                    try {
                        if (nro.resources.Resources.getInstance().find(0) != null) {
                            nro.resources.Resources.getInstance().find(0).initSmallVersion();
                        }
                    } catch (Exception ignored) {}
                    updateIconPreview.run();
                    JOptionPane.showMessageDialog(dlg, "Đã lưu ảnh làm Icon " + iconId + ".png thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        pnlIconRow.add(btnChangeIconImg);
        formLeft.add(pnlIconRow, gbc);

        addFormField(formLeft, gbc, "Part ID:", txtPart);

        // Cải Trang Part Inputs (Head, Body, Leg, Bag)
        int initCtHead = -1, initCtBody = -1, initCtLeg = -1, initCtBag = -1;
        if (editItem != null) {
            CaiTrang ctMem = Manager.getCaiTrangByItemId(editItem.id);
            if (ctMem != null && ctMem.getID() != null && ctMem.getID().length >= 3) {
                initCtHead = ctMem.getID()[0];
                initCtBody = ctMem.getID()[1];
                initCtLeg = ctMem.getID()[2];
                if (ctMem.getID().length >= 4) initCtBag = ctMem.getID()[3];
            } else {
                try (Connection conCt = DBService.gI().getConnection();
                     PreparedStatement psCt = conCt.prepareStatement("SELECT head, body, leg, bag FROM cai_trang WHERE id_temp = ?")) {
                    psCt.setInt(1, editItem.id);
                    try (ResultSet rsCt = psCt.executeQuery()) {
                        if (rsCt.next()) {
                            initCtHead = rsCt.getInt("head");
                            initCtBody = rsCt.getInt("body");
                            initCtLeg = rsCt.getInt("leg");
                            initCtBag = rsCt.getInt("bag");
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        JTextField txtCtHead = createDarkTextField(String.valueOf(initCtHead));
        JTextField txtCtBody = createDarkTextField(String.valueOf(initCtBody));
        JTextField txtCtLeg = createDarkTextField(String.valueOf(initCtLeg));
        JTextField txtCtBag = createDarkTextField(String.valueOf(initCtBag));

        JPanel pnlCtBox = new JPanel(new GridLayout(1, 4, 8, 0));
        pnlCtBox.setOpaque(false);
        pnlCtBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(234, 88, 12), 1, true), "Part Cải Trang Ngoại Hình (Dành cho Type 5)", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), new Color(251, 146, 60)),
            new EmptyBorder(6, 8, 8, 8)
        ));

        pnlCtBox.add(createMiniFieldPanel("Đầu (Head):", txtCtHead));
        pnlCtBox.add(createMiniFieldPanel("Thân (Body):", txtCtBody));
        pnlCtBox.add(createMiniFieldPanel("Chân (Leg):", txtCtLeg));
        pnlCtBox.add(createMiniFieldPanel("Lưng / Bag:", txtCtBag));

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        formLeft.add(pnlCtBox, gbc);
        gbc.gridwidth = 1;

        addFormField(formLeft, gbc, "Yêu Cầu Sức Mạnh:", txtPower);

        gbc.gridx = 0; gbc.gridy++;
        JLabel lblDesc = new JLabel("Mô Tả Vật Phẩm:");
        lblDesc.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDesc.setForeground(Color.WHITE);
        formLeft.add(lblDesc, gbc);

        gbc.gridx = 1;
        JScrollPane scDesc = new JScrollPane(txtDesc);
        scDesc.setPreferredSize(new Dimension(240, 65));
        scDesc.setBorder(BorderFactory.createLineBorder(new Color(56, 189, 248), 1));
        formLeft.add(scDesc, gbc);

        gbc.gridx = 1; gbc.gridy++;
        formLeft.add(chkStack, gbc);

        // RIGHT PANEL: OPTIONS TABLE + PRESETS + CONTROLS
        JPanel pnlRight = new JPanel(new BorderLayout(8, 8));
        pnlRight.setOpaque(true);
        pnlRight.setBackground(COLOR_PANEL_DARK);
        pnlRight.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK), "Chỉ Số Option Của Vật Phẩm", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 13), COLOR_ACCENT),
            new EmptyBorder(8, 8, 8, 8)
        ));

        // Options Table & Presets
        JPanel pnlOptsCenter = new JPanel(new BorderLayout(6, 6));
        pnlOptsCenter.setOpaque(false);

        String[] optTableCols = {"Mã Opt", "Tên Chỉ Số / Hiển Thị", "Param"};
        DefaultTableModel optModel = new DefaultTableModel(optTableCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col == 2; }
        };
        JTable tblOpts = new JTable(optModel);
        styleDarkTable(tblOpts);
        tblOpts.setRowHeight(26);
        tblOpts.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblOpts.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblOpts.getColumnModel().getColumn(0).setPreferredWidth(60);
        tblOpts.getColumnModel().getColumn(1).setPreferredWidth(260);
        tblOpts.getColumnModel().getColumn(2).setPreferredWidth(80);

        // Populate options from editItem
        if (editItem != null) {
            if (editItem.itemOptions != null && !editItem.itemOptions.isEmpty()) {
                for (ItemOption io : editItem.itemOptions) {
                    if (io != null) {
                        int optId = (io.optionTemplate != null) ? io.optionTemplate.id : -1;
                        optModel.addRow(new Object[]{optId, io.getOptionString(), io.param});
                    }
                }
            } else {
                try (Connection con = DBService.gI().getConnection();
                     PreparedStatement psOpt = con.prepareStatement("SELECT option_id, param FROM item_template_option WHERE template_id = ? ORDER BY id ASC")) {
                    psOpt.setInt(1, editItem.id);
                    try (ResultSet rsOpt = psOpt.executeQuery()) {
                        while (rsOpt.next()) {
                            int optId = rsOpt.getInt("option_id");
                            int paramVal = rsOpt.getInt("param");
                            ItemOption io = new ItemOption(optId, paramVal);
                            optModel.addRow(new Object[]{optId, io.getOptionString(), paramVal});
                        }
                    }
                } catch (Exception exOpt) {
                    Log.error(AdminPanelFrame.class, exOpt);
                }
            }
        }

        // Presets Bar for Item Template
        JPanel pnlPresets = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        pnlPresets.setOpaque(false);
        pnlPresets.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK), "Gán Nhanh Bộ Chỉ Số (Presets)", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), COLOR_TEXT_LIGHT),
            new EmptyBorder(4, 6, 6, 6)
        ));

        JButton btnPreVip = createModernButton("Set Cải Trang VIP", new Color(14, 116, 144), Color.WHITE);
        JButton btnPreDmg = createModernButton("Sát Thương Khủng", new Color(217, 119, 6), Color.WHITE);
        JButton btnPreTank = createModernButton("HP & KI Tanker", new Color(16, 185, 129), Color.WHITE);
        JButton btnPreHsd = createModernButton("HSD 30 Ngày", new Color(147, 51, 234), Color.WHITE);

        java.util.function.Consumer<int[][]> applyPreset = (int[][] presetData) -> {
            for (int[] opt : presetData) {
                int optId = opt[0];
                int param = opt[1];
                ItemOption io = new ItemOption(optId, param);
                boolean exists = false;
                for (int r = 0; r < optModel.getRowCount(); r++) {
                    if ((int) optModel.getValueAt(r, 0) == optId) {
                        optModel.setValueAt(io.getOptionString(), r, 1);
                        optModel.setValueAt(param, r, 2);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    optModel.addRow(new Object[]{optId, io.getOptionString(), param});
                }
            }
        };

        btnPreVip.addActionListener(e -> applyPreset.accept(new int[][]{
            {77, 40}, {103, 40}, {50, 40}, {101, 100}, {14, 20}, {5, 100}, {106, 0}
        }));
        btnPreDmg.addActionListener(e -> applyPreset.accept(new int[][]{
            {50, 50}, {14, 30}, {5, 150}, {95, 15}, {96, 15}
        }));
        btnPreTank.addActionListener(e -> applyPreset.accept(new int[][]{
            {77, 80}, {103, 80}, {47, 1000}, {108, 25}
        }));
        btnPreHsd.addActionListener(e -> applyPreset.accept(new int[][]{
            {93, 30}, {30, 0}
        }));

        pnlPresets.add(btnPreVip);
        pnlPresets.add(btnPreDmg);
        pnlPresets.add(btnPreTank);
        pnlPresets.add(btnPreHsd);

        pnlOptsCenter.add(pnlPresets, BorderLayout.NORTH);
        pnlOptsCenter.add(new JScrollPane(tblOpts), BorderLayout.CENTER);

        // Options control panel
        JPanel pnlOptCtrl = new JPanel(new GridBagLayout());
        pnlOptCtrl.setOpaque(true);
        pnlOptCtrl.setBackground(COLOR_PANEL_DARK);
        pnlOptCtrl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK), "Thao Tác Thêm / Chỉnh Sửa Chỉ Số", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), COLOR_TEXT_LIGHT),
            new EmptyBorder(8, 10, 8, 10)
        ));
        GridBagConstraints gbcO = new GridBagConstraints();
        gbcO.fill = GridBagConstraints.HORIZONTAL;
        gbcO.insets = new Insets(4, 4, 4, 4);

        // Populate cbOptionList
        List<String> optLabels = new ArrayList<>();
        optLabels.add("50 - Sức đánh+#%");
        optLabels.add("77 - HP+#%");
        optLabels.add("103 - KI+#%");
        optLabels.add("14 - Chí mạng+#%");
        optLabels.add("0 - Tấn công+#");
        optLabels.add("93 - Hạn sử dụng # ngày");
        optLabels.add("30 - Không thể giao dịch");
        optLabels.add("73 - Có thể giao dịch");
        for (ItemOptionTemplate iot : Manager.ITEM_OPTION_TEMPLATES) {
            if (iot != null && iot.name != null && !iot.name.trim().isEmpty()) {
                String entry = iot.id + " - " + iot.name;
                if (!optLabels.contains(entry)) {
                    optLabels.add(entry);
                }
            }
        }
        JComboBox<String> cbOptionList = new JComboBox<>(optLabels.toArray(new String[0]));
        cbOptionList.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbOptionList.setBackground(new Color(15, 23, 42));
        cbOptionList.setForeground(Color.WHITE);

        JTextField txtSearchOpt = createDarkTextField("");
        txtSearchOpt.setToolTipText("Gõ tìm kiếm chỉ số (HP, KI, sức đánh, ID...)");
        txtSearchOpt.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                String q = txtSearchOpt.getText().toLowerCase().trim();
                cbOptionList.removeAllItems();
                for (String s : optLabels) {
                    if (q.isEmpty() || s.toLowerCase().contains(q)) {
                        cbOptionList.addItem(s);
                    }
                }
            }
        });

        JTextField txtParamInput = createDarkTextField("10");

        gbcO.gridx = 0; gbcO.gridy = 0;
        JLabel lblSearchTitle = new JLabel("Chọn Chỉ Số:");
        lblSearchTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSearchTitle.setForeground(Color.WHITE);
        pnlOptCtrl.add(lblSearchTitle, gbcO);

        gbcO.gridx = 1; gbcO.gridwidth = 2;
        JPanel pnlOptSearchBox = new JPanel(new BorderLayout(6, 0));
        pnlOptSearchBox.setOpaque(false);
        txtSearchOpt.setPreferredSize(new Dimension(110, 30));
        pnlOptSearchBox.add(txtSearchOpt, BorderLayout.WEST);
        pnlOptSearchBox.add(cbOptionList, BorderLayout.CENTER);
        pnlOptCtrl.add(pnlOptSearchBox, gbcO);

        gbcO.gridx = 0; gbcO.gridy = 1; gbcO.gridwidth = 1;
        JLabel lblParamTitle = new JLabel("Giá Trị (Param):");
        lblParamTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblParamTitle.setForeground(Color.WHITE);
        pnlOptCtrl.add(lblParamTitle, gbcO);

        gbcO.gridx = 1;
        JPanel pnlParamRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlParamRow.setOpaque(false);
        txtParamInput.setPreferredSize(new Dimension(100, 30));
        pnlParamRow.add(txtParamInput);

        JButton btnAdd10 = createModernButton("+10", new Color(51, 65, 85), Color.WHITE);
        JButton btnAdd50 = createModernButton("+50", new Color(51, 65, 85), Color.WHITE);
        JButton btnAdd100 = createModernButton("+100", new Color(51, 65, 85), Color.WHITE);
        JButton btnSet100 = createModernButton("100%", new Color(51, 65, 85), Color.WHITE);

        btnAdd10.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnAdd50.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnAdd100.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnSet100.setFont(new Font("Segoe UI", Font.BOLD, 10));

        btnAdd10.addActionListener(e -> {
            try { int v = Integer.parseInt(txtParamInput.getText().trim()); txtParamInput.setText(String.valueOf(v + 10)); } catch (Exception ex) { txtParamInput.setText("10"); }
        });
        btnAdd50.addActionListener(e -> {
            try { int v = Integer.parseInt(txtParamInput.getText().trim()); txtParamInput.setText(String.valueOf(v + 50)); } catch (Exception ex) { txtParamInput.setText("50"); }
        });
        btnAdd100.addActionListener(e -> {
            try { int v = Integer.parseInt(txtParamInput.getText().trim()); txtParamInput.setText(String.valueOf(v + 100)); } catch (Exception ex) { txtParamInput.setText("100"); }
        });
        btnSet100.addActionListener(e -> txtParamInput.setText("100"));

        pnlParamRow.add(btnAdd10);
        pnlParamRow.add(btnAdd50);
        pnlParamRow.add(btnAdd100);
        pnlParamRow.add(btnSet100);
        pnlOptCtrl.add(pnlParamRow, gbcO);

        JButton btnAddOpt = createModernButton("Thêm / Cập Nhật", new Color(14, 116, 144), Color.WHITE);
        JButton btnDelOpt = createModernButton("Xóa Đang Chọn", new Color(225, 29, 72), Color.WHITE);
        JButton btnClearOpt = createModernButton("Xóa Hết", new Color(71, 85, 105), Color.WHITE);

        btnAddOpt.addActionListener(e -> {
            String selStr = (String) cbOptionList.getSelectedItem();
            if (selStr == null || selStr.trim().isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn 1 loại chỉ số từ danh sách!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int optId;
            try {
                int dashIdx = selStr.indexOf(" -");
                optId = (dashIdx > 0) ? Integer.parseInt(selStr.substring(0, dashIdx).trim()) : Integer.parseInt(selStr.trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Mã chỉ số không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int paramVal;
            try {
                String pText = txtParamInput.getText().trim().replace(",", "").replace(".", "");
                if (pText.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Vui lòng nhập giá trị Param (ví dụ: 10, 50, 100)!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                paramVal = Integer.parseInt(pText);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Giá trị Param phải là số nguyên hợp lệ (ví dụ: 10, 50, 100)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ItemOption io = new ItemOption(optId, paramVal);
            String optName = io.getOptionString();

            boolean found = false;
            for (int i = 0; i < optModel.getRowCount(); i++) {
                int currId = (int) optModel.getValueAt(i, 0);
                if (currId == optId) {
                    optModel.setValueAt(paramVal, i, 2);
                    optModel.setValueAt(optName, i, 1);
                    found = true;
                    break;
                }
            }
            if (!found) {
                optModel.addRow(new Object[]{optId, optName, paramVal});
            }
        });

        btnDelOpt.addActionListener(e -> {
            int sel = tblOpts.getSelectedRow();
            if (sel >= 0) {
                optModel.removeRow(sel);
            } else {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn 1 dòng chỉ số trong bảng để xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnClearOpt.addActionListener(e -> {
            if (optModel.getRowCount() > 0) {
                optModel.setRowCount(0);
            }
        });

        tblOpts.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblOpts.getSelectedRow() >= 0) {
                int r = tblOpts.getSelectedRow();
                int optId = (int) optModel.getValueAt(r, 0);
                int paramVal = (int) optModel.getValueAt(r, 2);
                txtParamInput.setText(String.valueOf(paramVal));
                for (int i = 0; i < cbOptionList.getItemCount(); i++) {
                    if (cbOptionList.getItemAt(i).startsWith(optId + " -")) {
                        cbOptionList.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });

        JPanel pnlOptBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlOptBtns.setOpaque(false);
        pnlOptBtns.add(btnAddOpt);
        pnlOptBtns.add(btnDelOpt);
        pnlOptBtns.add(btnClearOpt);

        gbcO.gridx = 0; gbcO.gridy = 2; gbcO.gridwidth = 3;
        pnlOptCtrl.add(pnlOptBtns, gbcO);

        pnlRight.add(pnlOptsCenter, BorderLayout.CENTER);
        pnlRight.add(pnlOptCtrl, BorderLayout.SOUTH);

        // Center split pane directly without double ScrollPane wrappers (Prevents ghosting / screen tearing completely!)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formLeft, pnlRight);
        splitPane.setResizeWeight(0.48);
        splitPane.setBorder(null);
        splitPane.setOpaque(true);
        splitPane.setBackground(COLOR_PANEL_DARK);
        dlg.add(splitPane, BorderLayout.CENTER);

        // Bottom action buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        btnPanel.setOpaque(true);
        btnPanel.setBackground(COLOR_PANEL_DARK);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER_DARK));

        JButton btnSave = createModernButton("LƯU VẬT PHẨM & CHỈ SỐ", new Color(16, 185, 129), Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.addActionListener(e -> {
            try {
                int id = Integer.parseInt(txtId.getText().trim());
                String name = txtName.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Tên vật phẩm không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String typeStr = (String) cbType.getSelectedItem();
                int type = Integer.parseInt(typeStr.substring(0, typeStr.indexOf(" -")).trim());
                int gender = cbGender.getSelectedIndex();
                String desc = txtDesc.getText().trim();
                short icon = Short.parseShort(txtIcon.getText().trim());
                short part = Short.parseShort(txtPart.getText().trim());
                int power = Integer.parseInt(txtPower.getText().trim());
                boolean stack = chkStack.isSelected();

                // Collect options
                List<ItemOption> optList = new ArrayList<>();
                for (int r = 0; r < optModel.getRowCount(); r++) {
                    int optId = (int) optModel.getValueAt(r, 0);
                    int param = (int) optModel.getValueAt(r, 2);
                    optList.add(new ItemOption(optId, param));
                }

                try (Connection con = DBService.gI().getConnection()) {
                    if (editItem == null) {
                        // Check exists
                        try (PreparedStatement psCheck = con.prepareStatement("SELECT id FROM item_template WHERE id = ?")) {
                            psCheck.setInt(1, id);
                            try (ResultSet rsCheck = psCheck.executeQuery()) {
                                if (rsCheck.next()) {
                                    JOptionPane.showMessageDialog(dlg, "Mã Template ID: " + id + " đã tồn tại trong CSDL!", "Lỗi trùng ID", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                            }
                        }

                        // INSERT
                        String sqlIns = "INSERT INTO item_template (id, type, gender, name, description, icon_id, part, is_up_to_up, power_require) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement ps = con.prepareStatement(sqlIns)) {
                            ps.setInt(1, id);
                            ps.setInt(2, type);
                            ps.setInt(3, gender);
                            ps.setString(4, name);
                            ps.setString(5, desc);
                            ps.setInt(6, icon);
                            ps.setInt(7, part);
                            ps.setInt(8, stack ? 1 : 0);
                            ps.setInt(9, power);
                            ps.executeUpdate();
                        }
                    } else {
                        // UPDATE
                        String sqlUpd = "UPDATE item_template SET type = ?, gender = ?, name = ?, description = ?, icon_id = ?, part = ?, is_up_to_up = ?, power_require = ? WHERE id = ?";
                        try (PreparedStatement ps = con.prepareStatement(sqlUpd)) {
                            ps.setInt(1, type);
                            ps.setInt(2, gender);
                            ps.setString(3, name);
                            ps.setString(4, desc);
                            ps.setInt(5, icon);
                            ps.setInt(6, part);
                            ps.setInt(7, stack ? 1 : 0);
                            ps.setInt(8, power);
                            ps.setInt(9, id);
                            ps.executeUpdate();
                        }
                    }

                    // Save Options
                    try (PreparedStatement psDelOpt = con.prepareStatement("DELETE FROM item_template_option WHERE template_id = ?")) {
                        psDelOpt.setInt(1, id);
                        psDelOpt.executeUpdate();
                    }
                    if (!optList.isEmpty()) {
                        String sqlInsOpt = "INSERT INTO item_template_option (template_id, option_id, param) VALUES (?, ?, ?)";
                        try (PreparedStatement psInsOpt = con.prepareStatement(sqlInsOpt)) {
                            for (ItemOption io : optList) {
                                psInsOpt.setInt(1, id);
                                psInsOpt.setInt(2, io.optionTemplate.id);
                                psInsOpt.setInt(3, io.param);
                                psInsOpt.addBatch();
                            }
                            psInsOpt.executeBatch();
                        }
                    }

                    // Part Cải Trang (Head, Body, Leg, Bag)
                    if (type == 5 || !txtCtHead.getText().trim().equals("-1") || !txtCtBody.getText().trim().equals("-1")) {
                        try {
                            int h = Integer.parseInt(txtCtHead.getText().trim());
                            int b = Integer.parseInt(txtCtBody.getText().trim());
                            int l = Integer.parseInt(txtCtLeg.getText().trim());
                            int bg = Integer.parseInt(txtCtBag.getText().trim());
                            String sqlCt = "INSERT INTO cai_trang (id_temp, head, body, leg, bag) VALUES (?, ?, ?, ?, ?) "
                                         + "ON DUPLICATE KEY UPDATE head = VALUES(head), body = VALUES(body), leg = VALUES(leg), bag = VALUES(bag)";
                            try (PreparedStatement psCt = con.prepareStatement(sqlCt)) {
                                psCt.setInt(1, id);
                                psCt.setInt(2, h);
                                psCt.setInt(3, b);
                                psCt.setInt(4, l);
                                psCt.setInt(5, bg);
                                psCt.executeUpdate();
                            }
                            CaiTrang ctExist = Manager.getCaiTrangByItemId(id);
                            if (ctExist != null && ctExist.getID() != null) {
                                ctExist.getID()[0] = h;
                                ctExist.getID()[1] = b;
                                ctExist.getID()[2] = l;
                                if (ctExist.getID().length >= 4) ctExist.getID()[3] = bg;
                            } else {
                                Manager.CAI_TRANGS.add(new CaiTrang(id, h, b, l, bg));
                            }
                        } catch (Exception exCt) {
                            System.err.println("Lỗi lưu part cải trang: " + exCt.getMessage());
                        }
                    }

                    // Sync item_shop_option & Manager.SHOPS
                    try {
                        List<Integer> shopItemIds = new ArrayList<>();
                        try (PreparedStatement psShop = con.prepareStatement("SELECT id FROM item_shop WHERE temp_id = ?")) {
                            psShop.setInt(1, id);
                            try (ResultSet rsShop = psShop.executeQuery()) {
                                while (rsShop.next()) {
                                    shopItemIds.add(rsShop.getInt("id"));
                                }
                            }
                        }
                        for (int shopId : shopItemIds) {
                            try (PreparedStatement psDelShop = con.prepareStatement("DELETE FROM item_shop_option WHERE item_shop_id = ?")) {
                                psDelShop.setInt(1, shopId);
                                psDelShop.executeUpdate();
                            }
                            if (!optList.isEmpty()) {
                                try (PreparedStatement psInsShop = con.prepareStatement("INSERT INTO item_shop_option (item_shop_id, option_id, param) VALUES (?, ?, ?)")) {
                                    for (ItemOption io : optList) {
                                        psInsShop.setInt(1, shopId);
                                        psInsShop.setInt(2, io.optionTemplate.id);
                                        psInsShop.setInt(3, io.param);
                                        psInsShop.addBatch();
                                    }
                                    psInsShop.executeBatch();
                                }
                            }
                        }
                        synchronized (Manager.SHOPS) {
                            for (nro.models.shop.Shop s : Manager.SHOPS) {
                                for (nro.models.shop.TabShop tab : s.tabShops) {
                                    for (nro.models.shop.ItemShop ishop : tab.itemShops) {
                                        if (ishop.temp != null && ishop.temp.id == id) {
                                            ishop.options.clear();
                                            for (ItemOption io : optList) {
                                                ishop.options.add(new ItemOption(io));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception exShop) {
                        Log.error(AdminPanelFrame.class, exShop);
                    }
                }

                reloadItemTemplatesFromDB();
                loadItemTemplatesToTable();
                dlg.dispose();
                JOptionPane.showMessageDialog(this, (editItem == null ? "Thêm mới" : "Cập nhật") + " vật phẩm và chỉ số thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                Log.error(AdminPanelFrame.class, ex);
                JOptionPane.showMessageDialog(dlg, "Lỗi khi lưu vật phẩm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnPanel.add(btnSave);

        JButton btnCancel = createModernButton("Đóng", new Color(71, 85, 105), Color.WHITE);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancel.addActionListener(e -> dlg.dispose());
        btnPanel.add(btnCancel);

        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void handleEditSelectedItem() {
        int row = itemTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 vật phẩm từ bảng để sửa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = itemTable.convertRowIndexToModel(row);
        short tempId = (short) itemTableModel.getValueAt(modelRow, 0);
        ItemTemplate tmpl = ItemService.gI().getTemplate(tempId);
        if (tmpl != null) {
            openItemTemplateDialog(tmpl);
        } else {
            JOptionPane.showMessageDialog(this, "Không tìm thấy dữ liệu template của vật phẩm ID " + tempId, "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteSelectedItemFromDB() {
        int row = itemTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 vật phẩm từ bảng để xóa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = itemTable.convertRowIndexToModel(row);
        short tempId = (short) itemTableModel.getValueAt(modelRow, 0);
        String name = (String) itemTableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn XÓA VĨNH VIỄN vật phẩm: [" + tempId + "] " + name + " khỏi CSDL?\n(Hành động này sẽ xóa dữ liệu trong bảng item_template)",
            "Xác nhận xóa vật phẩm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection con = DBService.gI().getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM item_template WHERE id = ?")) {
                ps.setShort(1, tempId);
                int count = ps.executeUpdate();
                if (count > 0) {
                    try (PreparedStatement psDelOpt = con.prepareStatement("DELETE FROM item_template_option WHERE template_id = ?")) {
                        psDelOpt.setShort(1, tempId);
                        psDelOpt.executeUpdate();
                    } catch (Exception ignored) {}
                    try (PreparedStatement psDelCt = con.prepareStatement("DELETE FROM cai_trang WHERE id_temp = ?")) {
                        psDelCt.setShort(1, tempId);
                        psDelCt.executeUpdate();
                    } catch (Exception ignored) {}
                    Manager.CAI_TRANGS.removeIf(ct -> ct.tempId == tempId);
                    reloadItemTemplatesFromDB();
                    loadItemTemplatesToTable();
                    JOptionPane.showMessageDialog(this, "Đã xóa vật phẩm [" + tempId + "] " + name + " thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy vật phẩm để xóa trong database!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                Log.error(AdminPanelFrame.class, ex);
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa vật phẩm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==========================================
    // QUẢN LÝ ICON (TÀI NGUYÊN GAME NRO)
    // ==========================================
    private void openIconManagerDialog() {
        JDialog dlg = new JDialog(this, "Quản Lý Icon Vật Phẩm (Tài Nguyên Game)", true);
        dlg.setSize(520, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0; gbc.gridy = 0;

        JTextField txtIconId = new JTextField("457", 12);
        JLabel lblPreview = new JLabel();
        lblPreview.setPreferredSize(new Dimension(80, 80));
        lblPreview.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        lblPreview.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblInfo = new JLabel("Kích thước file: ...");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        Runnable loadIcon = () -> {
            try {
                int id = Integer.parseInt(txtIconId.getText().trim());
                File f = new File("resources/normal/image/2/icon/" + id + ".png");
                if (!f.exists()) f = new File("resources/normal/image/1/icon/" + id + ".png");
                if (f.exists()) {
                    ImageIcon ic = new ImageIcon(f.getAbsolutePath());
                    Image scaled = ic.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                    lblPreview.setIcon(new ImageIcon(scaled));
                    lblPreview.setText("");
                    lblInfo.setText("<html>Đang có: <b>" + id + ".png</b><br>Kích thước: " + f.length() + " bytes | " + ic.getIconWidth() + "x" + ic.getIconHeight() + "px</html>");
                } else {
                    lblPreview.setIcon(null);
                    lblPreview.setText("Chưa có");
                    lblInfo.setText("<html>Icon ID <b>" + id + "</b> chưa tồn tại trên server.</html>");
                }
            } catch (Exception ex) {
                lblPreview.setIcon(null);
                lblPreview.setText("Lỗi");
                lblInfo.setText("ID không hợp lệ!");
            }
        };

        loadIcon.run();
        txtIconId.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { loadIcon.run(); }
        });

        form.add(new JLabel("Mã Icon ID:"), gbc);
        gbc.gridx = 1; form.add(txtIconId, gbc);

        gbc.gridx = 0; gbc.gridy++; form.add(new JLabel("Hình ảnh Icon:"), gbc);
        gbc.gridx = 1; form.add(lblPreview, gbc);

        gbc.gridx = 0; gbc.gridy++; form.add(new JLabel("Thông tin:"), gbc);
        gbc.gridx = 1; form.add(lblInfo, gbc);

        dlg.add(form, BorderLayout.CENTER);

        // Actions
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton btnUpload = new JButton("Chọn File PNG Để Lưu / Thay Thế");
        btnUpload.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpload.setBackground(new Color(40, 167, 69));
        btnUpload.setForeground(Color.WHITE);
        btnUpload.addActionListener(e -> {
            try {
                int id = Integer.parseInt(txtIconId.getText().trim());
                if (id < 0) {
                    JOptionPane.showMessageDialog(dlg, "Vui lòng nhập Icon ID hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Chọn file ảnh PNG làm Icon ID " + id);
                chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Hình ảnh PNG (*.png)", "png"));
                int res = chooser.showOpenDialog(dlg);
                if (res == JFileChooser.APPROVE_OPTION) {
                    File selected = chooser.getSelectedFile();
                    byte[] bytes = java.nio.file.Files.readAllBytes(selected.toPath());
                    for (int z = 1; z <= 4; z++) {
                        File tDir = new File("resources/normal/image/" + z + "/icon");
                        if (!tDir.exists()) tDir.mkdirs();
                        File tFile = new File(tDir, id + ".png");
                        java.nio.file.Files.write(tFile.toPath(), bytes);
                    }
                    try {
                        if (nro.resources.Resources.getInstance().find(0) != null) {
                            nro.resources.Resources.getInstance().find(0).initSmallVersion();
                        }
                    } catch (Exception ignored) {}
                    loadIcon.run();
                    JOptionPane.showMessageDialog(dlg, "Đã lưu thành công Icon " + id + ".png vào toàn bộ zoom x1-x4 và cập nhật cache server!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnBar.add(btnUpload);

        JButton btnDelete = new JButton("Xóa Icon Này");
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDelete.setForeground(new Color(204, 0, 0));
        btnDelete.addActionListener(e -> {
            try {
                int id = Integer.parseInt(txtIconId.getText().trim());
                int confirm = JOptionPane.showConfirmDialog(dlg, "Bạn có chắc chắn muốn xóa Icon " + id + ".png?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    for (int z = 1; z <= 4; z++) {
                        File f = new File("resources/normal/image/" + z + "/icon/" + id + ".png");
                        if (f.exists()) f.delete();
                    }
                    try {
                        if (nro.resources.Resources.getInstance().find(0) != null) {
                            nro.resources.Resources.getInstance().find(0).initSmallVersion();
                        }
                    } catch (Exception ignored) {}
                    loadIcon.run();
                    JOptionPane.showMessageDialog(dlg, "Đã xóa Icon " + id + ".png thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnBar.add(btnDelete);

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dlg.dispose());
        btnBar.add(btnClose);

        dlg.add(btnBar, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ==========================================
    // 4. TAB QUẢN LÝ BOSS (REALTIME)
    // ==========================================
    private JPanel createBossPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        JButton btnRefreshBoss = new JButton("Làm Mới Danh Sách Boss");
        btnRefreshBoss.addActionListener(e -> refreshBossTable());
        topPanel.add(btnRefreshBoss);

        topPanel.add(new JLabel("Triệu hồi Boss nhanh:"));
        String[] bossPresets = {
            "Broly (" + BossFactory.BROLY + ")",
            "Super Broly (" + BossFactory.SUPER_BROLY + ")",
            "Xên Bọ Hung (" + BossFactory.XEN_BO_HUNG + ")",
            "Siêu Bọ Hung (" + BossFactory.SIEU_BO_HUNG + ")",
            "Kuku (" + BossFactory.KUKU + ")",
            "Mập Đầu Đinh (" + BossFactory.MAP_DAU_DINH + ")",
            "Rambo (" + BossFactory.RAMBO + ")",
            "Hủy Diệt (" + BossFactory.HUYDIET + ")",
            "Thiên Sứ (" + BossFactory.THIENSU + ")",
            "Bojack (" + BossFactory.BOJACK + ")",
            "Super Bojack (" + BossFactory.SUPERBOJACK + ")",
            "Fu (" + BossFactory.FU + ")"
        };
        cbBossSpawn = new JComboBox<>(bossPresets);
        topPanel.add(cbBossSpawn);

        JButton btnSpawn = new JButton("Triệu Hồi Boss Này");
        btnSpawn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSpawn.setBackground(new Color(255, 140, 0));
        btnSpawn.setForeground(Color.WHITE);
        btnSpawn.addActionListener(e -> handleSpawnBoss());
        topPanel.add(btnSpawn);

        JButton btnKillBoss = new JButton("Thu Hồi / Xóa Boss Đang Chọn");
        btnKillBoss.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnKillBoss.setForeground(new Color(204, 0, 0));
        btnKillBoss.addActionListener(e -> handleRemoveBoss());
        topPanel.add(btnKillBoss);

        panel.add(topPanel, BorderLayout.NORTH);

        // Center Table
        String[] bossCols = {"ID Boss", "Tên Boss", "Bản Đồ", "Khu Vực", "Máu Hiện Tại", "Máu Tối Đa", "Sát Thương"};
        bossTableModel = new DefaultTableModel(bossCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bossTable = new JTable(bossTableModel);
        styleDarkTable(bossTable);
        bossTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bossTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        panel.add(new JScrollPane(bossTable), BorderLayout.CENTER);

        refreshBossTable();
        return panel;
    }

    private void refreshBossTable() {
        bossTableModel.setRowCount(0);
        try {
            List<Boss> bosses = new ArrayList<>(BossManager.BOSSES_IN_GAME);
            for (Boss boss : bosses) {
                if (boss != null && boss.nPoint != null) {
                    String mapName = (boss.zone != null && boss.zone.map != null) ? boss.zone.map.mapName : "Chưa vào map";
                    int zoneId = (boss.zone != null) ? boss.zone.zoneId : -1;
                    bossTableModel.addRow(new Object[]{
                        boss.id,
                        boss.name,
                        mapName,
                        zoneId,
                        numFmt.format(boss.nPoint.hp),
                        numFmt.format(boss.nPoint.hpMax),
                        numFmt.format(boss.nPoint.dame)
                    });
                }
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    private void handleSpawnBoss() {
        try {
            String selected = (String) cbBossSpawn.getSelectedItem();
            if (selected == null) return;
            int start = selected.indexOf('(');
            int end = selected.indexOf(')');
            if (start != -1 && end != -1) {
                int bossId = Integer.parseInt(selected.substring(start + 1, end).trim());
                Boss boss = BossFactory.createBoss(bossId);
                if (boss != null) {
                    BossManager.gI().addBoss(boss);
                    refreshBossTable();
                    JOptionPane.showMessageDialog(this, "Triệu hồi Boss " + boss.name + " thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể khởi tạo Boss với ID " + bossId + "!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
            JOptionPane.showMessageDialog(this, "Lỗi khi triệu hồi Boss: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRemoveBoss() {
        int row = bossTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 Boss từ bảng để thu hồi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            long bossId = (long) bossTableModel.getValueAt(row, 0);
            Boss target = null;
            for (Boss b : BossManager.BOSSES_IN_GAME) {
                if (b.id == bossId) {
                    target = b;
                    break;
                }
            }
            if (target != null) {
                target.leaveMap();
                BossManager.gI().removeBoss(target);
                refreshBossTable();
                JOptionPane.showMessageDialog(this, "Đã thu hồi Boss " + target.name + " thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    // ==========================================
    // 5. TAB QUẢN LÝ TÀI KHOẢN (DATABASE)
    // ==========================================
    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Search Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topBar.setBackground(COLOR_PANEL_DARK);
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER_DARK));

        JLabel lblSearch = new JLabel("Tìm tài khoản (Username hoặc ID):");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearch.setForeground(COLOR_ACCENT);
        topBar.add(lblSearch);

        txtAccountSearch = createDarkTextField("");
        txtAccountSearch.setPreferredSize(new Dimension(160, 32));
        topBar.add(txtAccountSearch);

        JButton btnSearchAcc = createModernButton("Tìm Kiếm", new Color(14, 165, 233), Color.WHITE);
        btnSearchAcc.addActionListener(e -> searchAccounts());
        topBar.add(btnSearchAcc);

        JButton btnLoadRecent = createModernButton("100 Mới Nhất", new Color(100, 116, 139), Color.WHITE);
        btnLoadRecent.addActionListener(e -> loadRecentAccounts());
        topBar.add(btnLoadRecent);

        JButton btnAddAcc = createModernButton("+ Thêm Tài Khoản Mới", new Color(16, 185, 129), Color.WHITE);
        btnAddAcc.addActionListener(e -> handleCreateAccount());
        topBar.add(btnAddAcc);

        panel.add(topBar, BorderLayout.NORTH);

        // Center Table
        String[] accCols = {"ID", "Tên Đăng Nhập", "Mật Khẩu", "Kích Hoạt (Active)", "Khóa (Ban)", "Admin", "Thỏi Vàng", "Đăng Nhập Cuối"};
        accountTableModel = new DefaultTableModel(accCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        accountTable = new JTable(accountTableModel);
        styleDarkTable(accountTable);
        accountTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        accountTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        panel.add(new JScrollPane(accountTable), BorderLayout.CENTER);

        // South Action Buttons
        JPanel southActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        southActions.setBackground(COLOR_PANEL_DARK);
        southActions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER_DARK));

        JButton btnActive = createModernButton("Kích Hoạt (Active)", new Color(16, 185, 129), Color.WHITE);
        btnActive.addActionListener(e -> toggleAccountActive(1));
        southActions.add(btnActive);

        JButton btnBan = createModernButton("Khóa / Mở Khóa", new Color(245, 158, 11), Color.WHITE);
        btnBan.addActionListener(e -> toggleAccountBan());
        southActions.add(btnBan);

        JButton btnAdmin = createModernButton("Cấp / Hủy Admin", new Color(168, 85, 247), Color.WHITE);
        btnAdmin.addActionListener(e -> toggleAccountAdmin());
        southActions.add(btnAdmin);

        JButton btnChangePass = createModernButton("Đổi Mật Khẩu", new Color(14, 165, 233), Color.WHITE);
        btnChangePass.addActionListener(e -> handleChangePassword());
        southActions.add(btnChangePass);

        JButton btnAddGoldBar = createModernButton("Cộng Thỏi Vàng", new Color(245, 158, 11), Color.WHITE);
        btnAddGoldBar.addActionListener(e -> handleAddGoldBar());
        southActions.add(btnAddGoldBar);

        JButton btnDeleteAcc = createModernButton("Xóa Tài Khoản", new Color(225, 29, 72), Color.WHITE);
        btnDeleteAcc.addActionListener(e -> handleDeleteAccount());
        southActions.add(btnDeleteAcc);

        panel.add(southActions, BorderLayout.SOUTH);

        loadRecentAccounts();
        return panel;
    }

    private void searchAccounts() {
        String keyword = txtAccountSearch.getText().trim();
        if (keyword.isEmpty()) {
            loadRecentAccounts();
            return;
        }

        accountTableModel.setRowCount(0);
        String sql = "SELECT id, username, password, active, ban, is_admin, thoi_vang, last_time_login FROM account WHERE username LIKE ? OR id = ? LIMIT 50";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            int idVal = -1;
            try {
                idVal = Integer.parseInt(keyword);
            } catch (Exception ignored) {
            }
            ps.setInt(2, idVal);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accountTableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getInt("active") == 1 ? "Đã kích hoạt" : "Chưa kích hoạt",
                        rs.getInt("ban") == 1 ? "ĐANG BỊ KHÓA" : "Bình thường",
                        rs.getInt("is_admin") == 1 ? "ADMIN" : "Người chơi",
                        rs.getInt("thoi_vang"),
                        rs.getTimestamp("last_time_login") != null ? rs.getTimestamp("last_time_login").toString() : "N/A"
                    });
                }
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
            JOptionPane.showMessageDialog(this, "Lỗi truy vấn tài khoản: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadRecentAccounts() {
        accountTableModel.setRowCount(0);
        String sql = "SELECT id, username, password, active, ban, is_admin, thoi_vang, last_time_login FROM account ORDER BY id DESC LIMIT 100";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                accountTableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getInt("active") == 1 ? "Đã kích hoạt" : "Chưa kích hoạt",
                    rs.getInt("ban") == 1 ? "ĐANG BỊ KHÓA" : "Bình thường",
                    rs.getInt("is_admin") == 1 ? "ADMIN" : "Người chơi",
                    rs.getInt("thoi_vang"),
                    rs.getTimestamp("last_time_login") != null ? rs.getTimestamp("last_time_login").toString() : "N/A"
                });
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    private void toggleAccountActive(int val) {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tài khoản từ bảng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int accId = (int) accountTableModel.getValueAt(row, 0);
        String sql = "UPDATE account SET active = ? WHERE id = ?";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, val);
            ps.setInt(2, accId);
            ps.executeUpdate();
            searchAccounts();
            JOptionPane.showMessageDialog(this, "Đã cập nhật trạng thái kích hoạt tài khoản ID: " + accId, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    private void toggleAccountBan() {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tài khoản từ bảng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int accId = (int) accountTableModel.getValueAt(row, 0);
        String status = (String) accountTableModel.getValueAt(row, 4);
        int newBan = status.contains("KHÓA") ? 0 : 1;

        String sql = "UPDATE account SET ban = ? WHERE id = ?";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, newBan);
            ps.setInt(2, accId);
            ps.executeUpdate();
            searchAccounts();
            JOptionPane.showMessageDialog(this, "Đã " + (newBan == 1 ? "KHÓA" : "MỞ KHÓA") + " tài khoản ID: " + accId, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    private void toggleAccountAdmin() {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tài khoản từ bảng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int accId = (int) accountTableModel.getValueAt(row, 0);
        String roleStr = (String) accountTableModel.getValueAt(row, 5);
        int newAdmin = roleStr.contains("ADMIN") ? 0 : 1;
        int newRole = (newAdmin == 1) ? 1 : -1;

        String sql = "UPDATE account SET is_admin = ?, role = ? WHERE id = ?";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, newAdmin);
            ps.setInt(2, newRole);
            ps.setInt(3, accId);
            ps.executeUpdate();
            searchAccounts();
            JOptionPane.showMessageDialog(this, "Đã " + (newAdmin == 1 ? "CẤP" : "HỦY") + " quyền Admin cho tài khoản ID: " + accId, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    private void handleChangePassword() {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tài khoản từ bảng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int accId = (int) accountTableModel.getValueAt(row, 0);
        String username = (String) accountTableModel.getValueAt(row, 1);
        String newPass = JOptionPane.showInputDialog(this, "Nhập mật khẩu mới cho tài khoản '" + username + "':");
        if (newPass != null && !newPass.trim().isEmpty()) {
            String sql = "UPDATE account SET password = ? WHERE id = ?";
            try (Connection con = DBService.gI().getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, newPass.trim());
                ps.setInt(2, accId);
                ps.executeUpdate();
                searchAccounts();
                JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công cho '" + username + "'!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                Log.error(AdminPanelFrame.class, ex);
            }
        }
    }

    private void handleAddGoldBar() {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tài khoản từ bảng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int accId = (int) accountTableModel.getValueAt(row, 0);
        String countStr = JOptionPane.showInputDialog(this, "Nhập số thỏi vàng cần cộng (+):", "100");
        if (countStr != null && !countStr.trim().isEmpty()) {
            try {
                int count = Integer.parseInt(countStr.trim());
                String sql = "UPDATE account SET thoi_vang = thoi_vang + ? WHERE id = ?";
                try (Connection con = DBService.gI().getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, count);
                    ps.setInt(2, accId);
                    ps.executeUpdate();
                    searchAccounts();
                    JOptionPane.showMessageDialog(this, "Cộng " + count + " thỏi vàng thành công cho tài khoản ID: " + accId, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số thỏi vàng không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleCreateAccount() {
        JDialog dlg = new JDialog(this, "Thêm Tài Khoản Mới Vào CSDL", true);
        dlg.setSize(520, 460);
        dlg.setMinimumSize(new Dimension(460, 420));
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(COLOR_BG_DARK);
        dlg.setLayout(new BorderLayout(10, 10));

        // Header Card
        JPanel pnlHeader = new JPanel(new BorderLayout(0, 4));
        pnlHeader.setBackground(COLOR_PANEL_DARK);
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER_DARK),
            new EmptyBorder(12, 16, 12, 16)
        ));
        JLabel lblTitle = new JLabel("● TẠO TÀI KHOẢN NGƯỜI CHƠI MỚI");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(COLOR_ACCENT);
        JLabel lblSub = new JLabel("Khởi tạo thông tin đăng nhập và phân quyền trực tiếp vào bảng account");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_MUTED);
        pnlHeader.add(lblTitle, BorderLayout.NORTH);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        dlg.add(pnlHeader, BorderLayout.NORTH);

        // Form Center
        JPanel formCenter = new JPanel();
        formCenter.setLayout(new BoxLayout(formCenter, BoxLayout.Y_AXIS));
        formCenter.setBackground(COLOR_PANEL_DARK);
        formCenter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 16, 10, 16),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(12, 14, 12, 14)
            )
        ));

        // 1. Tên đăng nhập
        JTextField txtUser = createDarkTextField("");

        // 2. Mật khẩu
        JPasswordField txtPass = new JPasswordField("1");
        txtPass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtPass.setBackground(new Color(15, 23, 42));
        txtPass.setForeground(Color.WHITE);
        txtPass.setCaretColor(Color.WHITE);
        txtPass.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(56, 189, 248), 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));

        // 3. Trạng thái Active
        JComboBox<String> cbActive = new JComboBox<>(new String[]{"1 - Đã kích hoạt (Active)", "0 - Chưa kích hoạt"});
        cbActive.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbActive.setBackground(new Color(15, 23, 42));
        cbActive.setForeground(Color.WHITE);
        cbActive.setPreferredSize(new Dimension(cbActive.getPreferredSize().width, 32));

        // 4. Quyền hạn
        JComboBox<String> cbAdmin = new JComboBox<>(new String[]{"0 - Người chơi bình thường", "1 - Quản trị viên (ADMIN)"});
        cbAdmin.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbAdmin.setBackground(new Color(15, 23, 42));
        cbAdmin.setForeground(Color.WHITE);
        cbAdmin.setPreferredSize(new Dimension(cbAdmin.getPreferredSize().width, 32));

        // 5. Thỏi vàng
        JTextField txtThoiVang = createDarkTextField("0");
        JPanel pnlGoldBar = new JPanel(new BorderLayout(8, 0));
        pnlGoldBar.setOpaque(false);
        pnlGoldBar.add(txtThoiVang, BorderLayout.CENTER);

        JPanel pnlGoldPills = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlGoldPills.setOpaque(false);
        JButton btnP0 = createModernButton("0", new Color(100, 116, 139), Color.WHITE);
        btnP0.addActionListener(e -> txtThoiVang.setText("0"));
        JButton btnP100 = createModernButton("100", new Color(245, 158, 11), Color.WHITE);
        btnP100.addActionListener(e -> txtThoiVang.setText("100"));
        JButton btnP500 = createModernButton("500", new Color(245, 158, 11), Color.WHITE);
        btnP500.addActionListener(e -> txtThoiVang.setText("500"));
        JButton btnP1k = createModernButton("1K", new Color(245, 158, 11), Color.WHITE);
        btnP1k.addActionListener(e -> txtThoiVang.setText("1000"));
        pnlGoldPills.add(btnP0);
        pnlGoldPills.add(btnP100);
        pnlGoldPills.add(btnP500);
        pnlGoldPills.add(btnP1k);
        pnlGoldBar.add(pnlGoldPills, BorderLayout.EAST);

        formCenter.add(createMiniFieldPanel("Tên Đăng Nhập:", txtUser));
        formCenter.add(Box.createVerticalStrut(8));
        formCenter.add(createMiniFieldPanel("Mật Khẩu:", txtPass));
        formCenter.add(Box.createVerticalStrut(8));
        formCenter.add(createMiniFieldPanel("Trạng Thái Kích Hoạt:", cbActive));
        formCenter.add(Box.createVerticalStrut(8));
        formCenter.add(createMiniFieldPanel("Phân Quyền (Role):", cbAdmin));
        formCenter.add(Box.createVerticalStrut(8));
        formCenter.add(createMiniFieldPanel("Thỏi Vàng Khởi Tạo:", pnlGoldBar));

        dlg.add(formCenter, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER_DARK));

        JButton btnCreate = createModernButton("Tạo Tài Khoản", new Color(16, 185, 129), Color.WHITE);
        btnCreate.setPreferredSize(new Dimension(140, 36));
        btnCreate.addActionListener(e -> {
            String u = txtUser.getText().trim();
            String p = new String(txtPass.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Tên đăng nhập và mật khẩu không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int act = cbActive.getSelectedIndex() == 0 ? 1 : 0;
            int adm = cbAdmin.getSelectedIndex() == 1 ? 1 : 0;
            int tv = 0;
            try { tv = Integer.parseInt(txtThoiVang.getText().trim().replace(",", "").replace(".", "")); } catch (Exception ignored) {}

            try (Connection con = DBService.gI().getConnection()) {
                try (PreparedStatement psCheck = con.prepareStatement("SELECT id FROM account WHERE username = ?")) {
                    psCheck.setString(1, u);
                    if (psCheck.executeQuery().next()) {
                        JOptionPane.showMessageDialog(dlg, "Tên tài khoản '" + u + "' đã tồn tại trong CSDL!", "Lỗi trùng lặp", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                String sql = "INSERT INTO account (username, password, active, ban, is_admin, thoi_vang, vnd, tongnap, role, server_login, create_time, update_time) "
                           + "VALUES (?, ?, ?, 0, ?, ?, 0, 0, -1, 1, NOW(), NOW())";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, u);
                    ps.setString(2, p);
                    ps.setInt(3, act);
                    ps.setInt(4, adm);
                    ps.setInt(5, tv);
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    int newId = rs.next() ? rs.getInt(1) : -1;
                    dlg.dispose();
                    JOptionPane.showMessageDialog(this, "Tạo tài khoản '" + u + "' (ID: " + newId + ") thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    loadRecentAccounts();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Lỗi tạo tài khoản: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = createModernButton("Hủy Bỏ", new Color(100, 116, 139), Color.WHITE);
        btnCancel.setPreferredSize(new Dimension(100, 36));
        btnCancel.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnCreate);
        btnPanel.add(btnCancel);

        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void handleDeleteAccount() {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn tài khoản trong bảng để xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int accId = (int) accountTableModel.getValueAt(row, 0);
        String username = (String) accountTableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this, 
            "CẢNH BÁO: Bạn có chắc chắn muốn XÓA VĨNH VIỄN tài khoản '" + username + "' (ID: " + accId + ")?\n"
            + "Thao tác này sẽ xóa sạch tài khoản và mọi nhân vật liên quan khỏi CSDL!", 
            "Xác nhận xóa tài khoản", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = DBService.gI().getConnection()) {
            // Kick online player
            try {
                Player pl = Client.gI().getPlayerByUser(accId);
                if (pl != null && pl.getSession() != null) {
                    Client.gI().kickSession(pl.getSession());
                }
            } catch (Exception ignored) {}

            // Delete player child records
            Long playerId = null;
            try (PreparedStatement psPl = con.prepareStatement("SELECT id FROM player WHERE account_id = ?")) {
                psPl.setInt(1, accId);
                try (ResultSet rs = psPl.executeQuery()) {
                    if (rs.next()) {
                        playerId = rs.getLong("id");
                    }
                }
            }
            if (playerId != null) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM gift_code_histories WHERE player_id = ?")) {
                    ps.setLong(1, playerId); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM history_receive_goldbar WHERE player_id = ?")) {
                    ps.setLong(1, playerId); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM shop_ky_gui WHERE player_id = ?")) {
                    ps.setLong(1, playerId); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM super_rank WHERE player_id = ?")) {
                    ps.setLong(1, playerId); ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM player WHERE id = ?")) {
                    ps.setLong(1, playerId); ps.executeUpdate();
                } catch (Exception ignored) {}
            }

            // Delete account
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM account WHERE id = ?")) {
                ps.setInt(1, accId);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Đã xóa vĩnh viễn tài khoản '" + username + "' thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            loadRecentAccounts();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xóa tài khoản: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    public static Border createDarkTitledBorder(String title) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DARK),
                title,
                TitledBorder.LEADING,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                COLOR_ACCENT
            ),
            new EmptyBorder(6, 6, 6, 6)
        );
    }

    public static void styleDarkTable(JTable table) {
        table.setBackground(COLOR_TABLE_ROW1);
        table.setForeground(COLOR_TEXT_LIGHT);
        table.setGridColor(COLOR_BORDER_DARK);
        table.setSelectionBackground(COLOR_TABLE_SEL);
        table.setSelectionForeground(Color.WHITE);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setBackground(COLOR_TABLE_HEADER);
        header.setForeground(COLOR_ACCENT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER_DARK));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? COLOR_TABLE_ROW1 : COLOR_TABLE_ROW2);
                    c.setForeground(COLOR_TEXT_LIGHT);
                } else {
                    c.setBackground(COLOR_TABLE_SEL);
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    public static String getNpcName(int npcId) {
        if (Manager.NPC_TEMPLATES != null) {
            for (nro.models.npc.NpcTemplate nt : Manager.NPC_TEMPLATES) {
                if (nt != null && nt.id == npcId && nt.name != null && !nt.name.trim().isEmpty()) {
                    return nt.name;
                }
            }
        }
        switch (npcId) {
            case ConstNpc.ONG_GOHAN: return "Ông Gôhan";
            case ConstNpc.ONG_PARAGUS: return "Ông Paragus";
            case ConstNpc.ONG_MOORI: return "Ông Moori";
            case ConstNpc.RUONG_DO: return "Rương Đồ";
            case ConstNpc.DAU_THAN: return "Đậu Thần";
            case ConstNpc.CON_MEO: return "Con Mèo";
            case ConstNpc.KHU_VUC: return "Cột Chuyển Khu";
            case ConstNpc.BUNMA: return "Bunma";
            case ConstNpc.DENDE: return "Dende";
            case ConstNpc.APPULE: return "Appule";
            case ConstNpc.DR_DRIEF: return "Dr. Brief";
            case ConstNpc.CARGO: return "Cargo";
            case ConstNpc.CUI: return "Cui";
            case ConstNpc.QUY_LAO_KAME: return "Quy Lão Kame";
            case ConstNpc.TRUONG_LAO_GURU: return "Trưởng Lão Guru";
            case ConstNpc.VUA_VEGETA: return "Vua Vegeta";
            case ConstNpc.URON: return "Uron";
            case ConstNpc.BO_MONG: return "Bò Mộng";
            case ConstNpc.THAN_MEO_KARIN: return "Thần Mèo Karin";
            case ConstNpc.THUONG_DE: return "Thượng Đế";
            case ConstNpc.THAN_VU_TRU: return "Thần Vũ Trụ";
            case ConstNpc.BA_HAT_MIT: return "Bà Hạt Mít";
            case ConstNpc.TRONG_TAI: return "Trọng Tài";
            case ConstNpc.GHI_DANH: return "Ghi Danh ĐHVT";
            case ConstNpc.RONG_THIENG: return "Rồng Thiêng";
            case ConstNpc.CUA_HANG_KY_GUI: return "Cửa Hàng Ký Gửi";
            case ConstNpc.BUNMA_TL: return "Bunma Tương Lai";
            case ConstNpc.CALICK: return "Calick";
            case ConstNpc.SANTA: return "Santa";
            case ConstNpc.MABU_MAP: return "Mabu Mập";
            case ConstNpc.TRUNG_THU: return "Chị Hằng";
            case ConstNpc.QUOC_VUONG: return "Quốc Vương";
            case ConstNpc.TO_SU_KAIO: return "Tổ Sư Kaio";
            case ConstNpc.OSIN: return "Osin";
            case ConstNpc.KIBIT: return "Kibit";
            case ConstNpc.BABIDAY: return "Babiday";
            case ConstNpc.GIUMA_DAU_BO: return "Giuma Đầu Bò";
            case ConstNpc.NGO_KHONG: return "Ngộ Không";
            case ConstNpc.DUONG_TANG: return "Đường Tăng";
            case ConstNpc.QUA_TRUNG: return "Quả Trứng Mabu";
            case ConstNpc.DUA_HAU: return "Dưa Hấu Mai An Tiêm";
            case ConstNpc.HUNG_VUONG: return "Hùng Vương";
            case ConstNpc.TAPION: return "Tapion";
            case ConstNpc.LY_TIEU_NUONG: return "Lý Tiểu Nương";
            case ConstNpc.BILL: return "Thần Huỷ Diệt Beerus";
            case ConstNpc.WHIS: return "Whis";
            case ConstNpc.CHAMPA: return "Champa";
            case ConstNpc.VADOS: return "Vados";
            case ConstNpc.GOKU_SSJ: return "Goku SSJ";
            case ConstNpc.POTAGE: return "Potage";
            case ConstNpc.JACO: return "Jaco";
            case ConstNpc.YARIROBE: return "Yajirobe";
            case ConstNpc.NOI_BANH: return "Nồi Bánh Chưng";
            case ConstNpc.MR_POPO: return "Mr. Popo";
            case ConstNpc.PANCHY: return "Panchy";
            case ConstNpc.THO_DAI_CA: return "Thỏ Đại Ca";
            case ConstNpc.BARDOCK: return "Bardock";
            case ConstNpc.TORIBOT: return "Toribot";
            case ConstNpc.EVENT: return "NPC Sự Kiện Hè";
            case ConstNpc.CAY_THONG: return "Cây Thông Noel";
            case ConstNpc.OBITO: return "Obito";
            case ConstNpc.GOGETA_SSJ4: return "Gogeta SSJ4";
            default: return "NPC #" + npcId;
        }
    }

    // ==========================================
    // ==========================================
    // 5. TAB CỬA HÀNG (NPC SHOP PHÂN CẤP THEO MAP)
    // ==========================================
    private JPanel createShopPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(COLOR_BG_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
            createDarkTitledBorder("Quản Lý Cửa Hàng Shop NPC (Phân Cấp Theo Bản Đồ & NPC)"),
            new EmptyBorder(6, 8, 8, 8)
        ));

        // Toolbar: Map -> NPC -> Tab -> Buttons
        JPanel pnlShopToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        pnlShopToolbar.setBackground(COLOR_PANEL_DARK);
        pnlShopToolbar.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK));

        // 1. CHỌN MAP
        JLabel lblMap = new JLabel("Bản Đồ:");
        lblMap.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMap.setForeground(COLOR_ACCENT);
        pnlShopToolbar.add(lblMap);

        cbMapSelect = new JComboBox<>();
        cbMapSelect.setPreferredSize(new Dimension(230, 28));
        cbMapSelect.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbMapSelect.addActionListener(e -> onMapSelectChanged());
        pnlShopToolbar.add(cbMapSelect);

        // 2. CHỌN NPC SHOP
        JLabel lblNpc = new JLabel("NPC Shop:");
        lblNpc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNpc.setForeground(COLOR_ACCENT);
        pnlShopToolbar.add(lblNpc);

        cbShopNpcSelect = new JComboBox<>();
        cbShopNpcSelect.setPreferredSize(new Dimension(250, 28));
        cbShopNpcSelect.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbShopNpcSelect.addActionListener(e -> loadTabsForSelectedShop());
        pnlShopToolbar.add(cbShopNpcSelect);

        // 3. CHỌN TAB CỦA SHOP
        JLabel lblTab = new JLabel("Tab:");
        lblTab.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTab.setForeground(COLOR_ACCENT);
        pnlShopToolbar.add(lblTab);

        cbShopTabSelect = new JComboBox<>();
        cbShopTabSelect.setPreferredSize(new Dimension(160, 28));
        cbShopTabSelect.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbShopTabSelect.addActionListener(e -> loadItemsForSelectedShopTab());
        pnlShopToolbar.add(cbShopTabSelect);

        // ACTION BUTTONS
        JButton btnAddShopItem = new JButton("Thêm Món");
        btnAddShopItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddShopItem.setBackground(new Color(40, 167, 69));
        btnAddShopItem.setForeground(Color.WHITE);
        btnAddShopItem.addActionListener(e -> handleAddShopItem());
        pnlShopToolbar.add(btnAddShopItem);

        JButton btnEditShopItem = new JButton("Sửa Món");
        btnEditShopItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditShopItem.setBackground(new Color(255, 193, 7));
        btnEditShopItem.setForeground(Color.BLACK);
        btnEditShopItem.addActionListener(e -> handleEditSelectedShopItem());
        pnlShopToolbar.add(btnEditShopItem);

        JButton btnDeleteShopItem = new JButton("Xóa Món");
        btnDeleteShopItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeleteShopItem.setBackground(new Color(220, 53, 69));
        btnDeleteShopItem.setForeground(Color.WHITE);
        btnDeleteShopItem.addActionListener(e -> handleDeleteShopItem());
        pnlShopToolbar.add(btnDeleteShopItem);

        JButton btnReloadShop = new JButton("Nạp Lại");
        btnReloadShop.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReloadShop.setBackground(new Color(31, 111, 235));
        btnReloadShop.setForeground(Color.WHITE);
        btnReloadShop.addActionListener(e -> reloadShops());
        pnlShopToolbar.add(btnReloadShop);

        panel.add(pnlShopToolbar, BorderLayout.NORTH);

        // TABLE
        String[] shopCols = {"ID Bán", "Mã Item", "Tên Vật Phẩm", "Giá Vàng", "Giá Ngọc", "Vật Phẩm Đổi", "SL Đổi", "Chỉ Số (Options)", "Trạng Thái"};
        shopItemTableModel = new DefaultTableModel(shopCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        shopItemTable = new JTable(shopItemTableModel);
        styleDarkTable(shopItemTable);
        shopItemTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        shopItemTable.getColumnModel().getColumn(1).setPreferredWidth(65);
        shopItemTable.getColumnModel().getColumn(2).setPreferredWidth(170);
        shopItemTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        shopItemTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        shopItemTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        shopItemTable.getColumnModel().getColumn(6).setPreferredWidth(60);
        shopItemTable.getColumnModel().getColumn(7).setPreferredWidth(210);
        shopItemTable.getColumnModel().getColumn(8).setPreferredWidth(70);

        JScrollPane scrollPane = new JScrollPane(shopItemTable);
        scrollPane.getViewport().setBackground(COLOR_BG_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK));
        panel.add(scrollPane, BorderLayout.CENTER);

        loadMapsToCombo();
        return panel;
    }

    // ==========================================
    // 6. TAB MÃ QUÀ TẶNG (GIFTCODE ĐỘC LẬP)
    // ==========================================
    private JPanel createGiftCodePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(COLOR_BG_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
            createDarkTitledBorder("Quản Lý Mã Quà Tặng (GiftCode) - Chọn Đồ Trực Quan"),
            new EmptyBorder(6, 8, 8, 8)
        ));

        JPanel pnlCodeTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pnlCodeTop.setBackground(COLOR_PANEL_DARK);
        pnlCodeTop.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK));

        JButton btnRefreshCodes = new JButton("Làm Mới Danh Sách");
        btnRefreshCodes.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefreshCodes.setBackground(new Color(31, 111, 235));
        btnRefreshCodes.setForeground(Color.WHITE);
        btnRefreshCodes.addActionListener(e -> refreshGiftcodeTable());
        pnlCodeTop.add(btnRefreshCodes);

        JButton btnCreateCode = new JButton("Tạo GiftCode Mới (Chọn Đồ)");
        btnCreateCode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCreateCode.setBackground(new Color(40, 167, 69));
        btnCreateCode.setForeground(Color.WHITE);
        btnCreateCode.addActionListener(e -> openVisualGiftCodeDialog(null));
        pnlCodeTop.add(btnCreateCode);

        JButton btnEditCode = new JButton("Sửa GiftCode Đang Chọn");
        btnEditCode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditCode.setBackground(new Color(255, 193, 7));
        btnEditCode.setForeground(Color.BLACK);
        btnEditCode.addActionListener(e -> handleEditSelectedGiftCode());
        pnlCodeTop.add(btnEditCode);

        JButton btnDeleteCode = new JButton("Xóa GiftCode Đang Chọn");
        btnDeleteCode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeleteCode.setBackground(new Color(220, 53, 69));
        btnDeleteCode.setForeground(Color.WHITE);
        btnDeleteCode.addActionListener(e -> handleDeleteGiftcode());
        pnlCodeTop.add(btnDeleteCode);

        panel.add(pnlCodeTop, BorderLayout.NORTH);

        String[] codeCols = {"ID", "Mã GiftCode", "Vàng", "Ngọc", "Ruby", "Số Món Đồ", "Chi Tiết Vật Phẩm", "Ngày Tạo"};
        giftcodeTableModel = new DefaultTableModel(codeCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        giftcodeTable = new JTable(giftcodeTableModel);
        styleDarkTable(giftcodeTable);
        giftcodeTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        giftcodeTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        giftcodeTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        giftcodeTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        giftcodeTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        giftcodeTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        giftcodeTable.getColumnModel().getColumn(6).setPreferredWidth(320);
        giftcodeTable.getColumnModel().getColumn(7).setPreferredWidth(140);

        JScrollPane scrollPane = new JScrollPane(giftcodeTable);
        scrollPane.getViewport().setBackground(COLOR_BG_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK));
        panel.add(scrollPane, BorderLayout.CENTER);

        refreshGiftcodeTable();
        return panel;
    }

    private void refreshShopTables() {
        loadMapsToCombo();
        refreshGiftcodeTable();
    }

    private void loadMapsToCombo() {
        if (cbMapSelect == null) return;
        cbMapSelect.removeAllItems();
        cbMapSelect.addItem("[ Tất Cả Bản Đồ - Toàn Bộ Shop ]");

        java.util.Map<Integer, String> mapNames = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, Integer> mapShopCounts = new java.util.LinkedHashMap<>();
        java.util.Set<Integer> mappedShopNpcIds = new java.util.HashSet<>();

        if (Manager.MAP_TEMPLATES != null && Manager.SHOPS != null) {
            for (nro.models.map.MapTemplate mt : Manager.MAP_TEMPLATES) {
                if (mt != null && mt.npcId != null) {
                    for (byte nid : mt.npcId) {
                        for (nro.models.shop.Shop s : Manager.SHOPS) {
                            if (s != null && s.npcId == nid) {
                                mapNames.put(mt.id, mt.name);
                                mapShopCounts.put(mt.id, mapShopCounts.getOrDefault(mt.id, 0) + 1);
                                mappedShopNpcIds.add((int) s.npcId);
                            }
                        }
                    }
                }
            }
        }

        for (java.util.Map.Entry<Integer, String> entry : mapNames.entrySet()) {
            int mId = entry.getKey();
            String mName = entry.getValue();
            int count = mapShopCounts.getOrDefault(mId, 0);
            cbMapSelect.addItem("Map " + mId + ": " + mName + " (" + count + " shop)");
        }

        // Special shops
        int specialCount = 0;
        if (Manager.SHOPS != null) {
            for (nro.models.shop.Shop s : Manager.SHOPS) {
                if (s != null && !mappedShopNpcIds.contains((int) s.npcId)) {
                    specialCount++;
                }
            }
        }
        if (specialCount > 0) {
            cbMapSelect.addItem("[NPC Sự Kiện & Đặc Biệt] (" + specialCount + " shop)");
        }

        if (cbMapSelect.getItemCount() > 0) {
            cbMapSelect.setSelectedIndex(0);
        }
    }

    private void onMapSelectChanged() {
        if (cbMapSelect == null || cbShopNpcSelect == null) return;
        String selMap = (String) cbMapSelect.getSelectedItem();
        cbShopNpcSelect.removeAllItems();
        if (selMap == null || Manager.SHOPS == null) return;

        if (selMap.contains("Tất Cả")) {
            loadAllShopsToCombo();
            return;
        }

        if (selMap.contains("Sự Kiện")) {
            java.util.Set<Integer> staticNpcIds = new java.util.HashSet<>();
            if (Manager.MAP_TEMPLATES != null) {
                for (nro.models.map.MapTemplate mt : Manager.MAP_TEMPLATES) {
                    if (mt != null && mt.npcId != null) {
                        for (byte nid : mt.npcId) staticNpcIds.add((int) nid);
                    }
                }
            }
            for (nro.models.shop.Shop s : Manager.SHOPS) {
                if (s != null && !staticNpcIds.contains((int) s.npcId)) {
                    String npcName = getNpcName(s.npcId);
                    cbShopNpcSelect.addItem(npcName + " - Shop #" + s.id + " (NPC " + s.npcId + ", Order " + s.shopOrder + ")");
                }
            }
        } else if (selMap.startsWith("Map ")) {
            int colon = selMap.indexOf(":");
            if (colon > 4) {
                try {
                    int mapId = Integer.parseInt(selMap.substring(4, colon).trim());
                    nro.models.map.MapTemplate mt = null;
                    if (Manager.MAP_TEMPLATES != null) {
                        for (nro.models.map.MapTemplate t : Manager.MAP_TEMPLATES) {
                            if (t != null && t.id == mapId) {
                                mt = t;
                                break;
                            }
                        }
                    }
                    if (mt != null && mt.npcId != null) {
                        java.util.Set<Byte> nids = new java.util.HashSet<>();
                        for (byte b : mt.npcId) nids.add(b);
                        for (nro.models.shop.Shop s : Manager.SHOPS) {
                            if (s != null && nids.contains(s.npcId)) {
                                String npcName = getNpcName(s.npcId);
                                cbShopNpcSelect.addItem(npcName + " - Shop #" + s.id + " (NPC " + s.npcId + ", Order " + s.shopOrder + ")");
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (cbShopNpcSelect.getItemCount() > 0) {
            cbShopNpcSelect.setSelectedIndex(0);
        } else {
            if (cbShopTabSelect != null) cbShopTabSelect.removeAllItems();
            if (shopItemTableModel != null) shopItemTableModel.setRowCount(0);
        }
    }

    private void loadAllShopsToCombo() {
        if (cbShopNpcSelect == null) return;
        cbShopNpcSelect.removeAllItems();
        if (Manager.SHOPS != null) {
            List<nro.models.shop.Shop> sortedShops = new ArrayList<>(Manager.SHOPS);
            sortedShops.sort((s1, s2) -> {
                String n1 = getNpcName(s1.npcId);
                String n2 = getNpcName(s2.npcId);
                return n1.compareToIgnoreCase(n2);
            });
            for (nro.models.shop.Shop s : sortedShops) {
                if (s != null) {
                    String npcName = getNpcName(s.npcId);
                    cbShopNpcSelect.addItem(npcName + " - Shop #" + s.id + " (NPC " + s.npcId + ", Order " + s.shopOrder + ")");
                }
            }
        }
        if (cbShopNpcSelect.getItemCount() > 0) {
            cbShopNpcSelect.setSelectedIndex(0);
        }
    }

    private void loadShopsToCombo() {
        loadMapsToCombo();
    }

    private int getSelectedShopId() {
        if (cbShopNpcSelect == null || cbShopNpcSelect.getSelectedItem() == null) return -1;
        String sel = (String) cbShopNpcSelect.getSelectedItem();
        int hash = sel.indexOf("Shop #");
        if (hash >= 0) {
            int end = sel.indexOf(" ", hash + 6);
            if (end < 0) end = sel.indexOf(")", hash + 6);
            if (end > 0) {
                try {
                    return Integer.parseInt(sel.substring(hash + 6, end).trim());
                } catch (Exception ignored) {}
            }
        }
        int dash = sel.indexOf(" -");
        if (dash > 0) {
            try { return Integer.parseInt(sel.substring(0, dash).trim()); } catch (Exception ignored) {}
        }
        return -1;
    }
    private nro.models.shop.Shop findShopById(int shopId) {
        if (Manager.SHOPS != null) {
            for (nro.models.shop.Shop s : Manager.SHOPS) {
                if (s != null && s.id == shopId) return s;
            }
        }
        return null;
    }

    private void loadTabsForSelectedShop() {
        if (cbShopTabSelect == null) return;
        cbShopTabSelect.removeAllItems();
        int shopId = getSelectedShopId();
        if (shopId <= 0) return;
        nro.models.shop.Shop shop = findShopById(shopId);
        if (shop != null && shop.tabShops != null) {
            for (nro.models.shop.TabShop tab : shop.tabShops) {
                if (tab != null) {
                    cbShopTabSelect.addItem(tab.id + " - " + tab.name.replace("\n", " "));
                }
            }
        }
        if (cbShopTabSelect.getItemCount() > 0) {
            cbShopTabSelect.setSelectedIndex(0);
        } else {
            shopItemTableModel.setRowCount(0);
        }
    }

    private int getSelectedTabId() {
        if (cbShopTabSelect == null || cbShopTabSelect.getSelectedItem() == null) return -1;
        String sel = (String) cbShopTabSelect.getSelectedItem();
        int dash = sel.indexOf(" -");
        if (dash > 0) {
            try { return Integer.parseInt(sel.substring(0, dash).trim()); } catch (Exception ignored) {}
        }
        return -1;
    }

    private void loadItemsForSelectedShopTab() {
        if (shopItemTableModel == null) return;
        shopItemTableModel.setRowCount(0);
        int tabId = getSelectedTabId();
        if (tabId <= 0) return;
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(
                "SELECT s.id, s.temp_id, it.name, s.gold, s.gem, s.item_exchange, s.quantity_exchange, s.is_sell, s.is_new "
                + "FROM item_shop s LEFT JOIN item_template it ON s.temp_id = it.id WHERE s.tab_id = ? ORDER BY s.id DESC")) {
            ps.setInt(1, tabId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int shopItemId = rs.getInt("id");
                    short tempId = rs.getShort("temp_id");
                    String itemName = rs.getString("name");
                    if (itemName == null) itemName = "Item #" + tempId;
                    int gold = rs.getInt("gold");
                    int gem = rs.getInt("gem");
                    int exId = rs.getInt("item_exchange");
                    int exQty = rs.getInt("quantity_exchange");
                    boolean isSell = rs.getBoolean("is_sell");

                    String exStr = "Không";
                    if (exId != -1) {
                        ItemTemplate exTmpl = ItemService.gI().getTemplate(exId);
                        String exName = (exTmpl != null) ? exTmpl.name : ("#" + exId);
                        exStr = exName + " (x" + exQty + ")";
                    }

                    // Options
                    StringBuilder optStr = new StringBuilder();
                    try (PreparedStatement psOpt = con.prepareStatement(
                        "SELECT o.option_id, o.param FROM item_shop_option o WHERE o.item_shop_id = ?")) {
                        psOpt.setInt(1, shopItemId);
                        try (ResultSet rsOpt = psOpt.executeQuery()) {
                            while (rsOpt.next()) {
                                int oId = rsOpt.getInt("option_id");
                                int oParam = rsOpt.getInt("param");
                                ItemOption io = new ItemOption(oId, oParam);
                                if (optStr.length() > 0) optStr.append(", ");
                                optStr.append(io.getOptionString());
                            }
                        }
                    }

                    shopItemTableModel.addRow(new Object[]{
                        shopItemId,
                        tempId,
                        itemName,
                        gold,
                        gem,
                        exStr,
                        exQty,
                        optStr.toString(),
                        isSell ? "Đang bán" : "Khóa"
                    });
                }
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    private void handleAddShopItem() {
        int tabId = getSelectedTabId();
        if (tabId <= 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 Tab Cửa Hàng trước!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        openShopItemDialog(null, tabId);
    }

    private void handleEditSelectedShopItem() {
        int row = shopItemTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 vật phẩm trong bảng Shop để sửa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = shopItemTable.convertRowIndexToModel(row);
        int shopItemId = (int) shopItemTableModel.getValueAt(modelRow, 0);
        int tabId = getSelectedTabId();
        openShopItemDialog(shopItemId, tabId);
    }

    private void handleDeleteShopItem() {
        int row = shopItemTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 vật phẩm trong bảng Shop để xóa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = shopItemTable.convertRowIndexToModel(row);
        int shopItemId = (int) shopItemTableModel.getValueAt(modelRow, 0);
        String name = (String) shopItemTableModel.getValueAt(modelRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn xóa món đồ [" + shopItemId + "] " + name + " khỏi Cửa Hàng này?",
            "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection con = DBService.gI().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_shop WHERE id = ?")) {
                    ps.setInt(1, shopItemId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM item_shop_option WHERE item_shop_id = ?")) {
                    ps2.setInt(1, shopItemId);
                    ps2.executeUpdate();
                }
                Manager.SHOPS = ShopDAO.getShops(con);
                loadItemsForSelectedShopTab();
                JOptionPane.showMessageDialog(this, "Đã xóa vật phẩm khỏi Shop thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                Log.error(AdminPanelFrame.class, ex);
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

        private void openShopItemDialog(Integer editShopItemId, int tabId) {
        JDialog dlg = new JDialog(this, editShopItemId == null ? "Thêm Vật Phẩm Vào Shop NPC" : "Chỉnh Sửa Vật Phẩm Trong Shop (ID: " + editShopItemId + ")", true);
        dlg.setSize(980, 640);
        dlg.setMinimumSize(new Dimension(880, 580));
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(COLOR_BG_DARK);
        dlg.setLayout(new BorderLayout(10, 10));

        // Center: 2 Columns Split (Left = Basic Info & Pricing, Right = Options & Presets)
        JPanel mainGrid = new JPanel(new GridLayout(1, 2, 12, 0));
        mainGrid.setOpaque(false);
        mainGrid.setBorder(new EmptyBorder(12, 14, 4, 14));

        // ==========================================
        // LEFT CARD: THÔNG TIN BÁN HÀNG TRONG SHOP
        // ==========================================
        JPanel leftCard = new JPanel();
        leftCard.setLayout(new BoxLayout(leftCard, BoxLayout.Y_AXIS));
        leftCard.setBackground(COLOR_PANEL_DARK);
        leftCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                "Thông Tin Cơ Bản Vật Phẩm Bán",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                COLOR_ACCENT
            ),
            new EmptyBorder(10, 12, 10, 12)
        ));

        // 1. MẪU VẬT PHẨM (ID, Chọn, Icon, Tên)
        JPanel pnlItemSelect = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        pnlItemSelect.setOpaque(false);

        JTextField txtTempId = createDarkTextField("0");
        txtTempId.setPreferredSize(new Dimension(80, 32));

        JButton btnPick = createModernButton("Chọn Mẫu Item", new Color(14, 165, 233), Color.WHITE);
        btnPick.setPreferredSize(new Dimension(120, 32));

        JLabel lblIconPreview = new JLabel();
        lblIconPreview.setPreferredSize(new Dimension(34, 34));
        lblIconPreview.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true));
        lblIconPreview.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblItemName = new JLabel("Chưa chọn vật phẩm");
        lblItemName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblItemName.setForeground(new Color(34, 197, 94)); // Emerald green

        Runnable updateItemPreview = () -> {
            try {
                int tId = Integer.parseInt(txtTempId.getText().trim());
                ItemTemplate tmpl = ItemService.gI().getTemplate(tId);
                if (tmpl != null) {
                    lblItemName.setText("<html><b style='color:#38bdf8;'>" + tmpl.name + "</b> <span style='color:#94a3b8;'>[ID: " + tmpl.id + "]</span></html>");
                    File f = new File("resources/normal/image/2/icon/" + tmpl.iconID + ".png");
                    if (!f.exists()) f = new File("resources/normal/image/1/icon/" + tmpl.iconID + ".png");
                    if (f.exists()) {
                        ImageIcon ic = new ImageIcon(f.getAbsolutePath());
                        Image img = ic.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                        lblIconPreview.setIcon(new ImageIcon(img));
                        lblIconPreview.setText("");
                    } else {
                        lblIconPreview.setIcon(null);
                        lblIconPreview.setText("N/A");
                    }
                } else {
                    lblItemName.setText("<html><span style='color:#f87171;'>Không tìm thấy Template #" + tId + "</span></html>");
                    lblIconPreview.setIcon(null);
                    lblIconPreview.setText("N/A");
                }
            } catch (Exception ex) {
                lblItemName.setText("<html><span style='color:#f87171;'>ID không hợp lệ</span></html>");
                lblIconPreview.setIcon(null);
                lblIconPreview.setText("N/A");
            }
        };

        txtTempId.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { updateItemPreview.run(); }
        });

        btnPick.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(dlg, "Nhập ID hoặc Tên Vật Phẩm muốn tìm:", "Tìm Mẫu Vật Phẩm", JOptionPane.QUESTION_MESSAGE);
            if (input != null && !input.trim().isEmpty()) {
                input = input.trim().toLowerCase();
                for (ItemTemplate t : Manager.ITEM_TEMPLATES) {
                    if (t != null && t.name != null && (String.valueOf(t.id).equals(input) || t.name.toLowerCase().contains(input))) {
                        txtTempId.setText(String.valueOf(t.id));
                        updateItemPreview.run();
                        break;
                    }
                }
            }
        });

        pnlItemSelect.add(new JLabel("Mã Item:"));
        pnlItemSelect.add(txtTempId);
        pnlItemSelect.add(btnPick);
        pnlItemSelect.add(lblIconPreview);

        JPanel pnlNameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlNameRow.setOpaque(false);
        pnlNameRow.add(lblItemName);

        leftCard.add(createMiniFieldPanel("Mẫu Vật Phẩm Bán:", pnlItemSelect));
        leftCard.add(pnlNameRow);
        leftCard.add(Box.createVerticalStrut(10));

        // 2. GIÁ VÀNG
        JTextField txtGold = createDarkTextField("0");
        txtGold.setPreferredSize(new Dimension(140, 32));

        JPanel pnlGold = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlGold.setOpaque(false);
        pnlGold.add(txtGold);

        JButton btnG0 = createModernButton("0", new Color(100, 116, 139), Color.WHITE);
        btnG0.addActionListener(e -> txtGold.setText("0"));
        JButton btnG1M = createModernButton("1 Tr", new Color(245, 158, 11), Color.WHITE);
        btnG1M.addActionListener(e -> txtGold.setText("1000000"));
        JButton btnG10M = createModernButton("10 Tr", new Color(245, 158, 11), Color.WHITE);
        btnG10M.addActionListener(e -> txtGold.setText("10000000"));
        JButton btnG50M = createModernButton("50 Tr", new Color(245, 158, 11), Color.WHITE);
        btnG50M.addActionListener(e -> txtGold.setText("50000000"));
        JButton btnG100M = createModernButton("100 Tr", new Color(245, 158, 11), Color.WHITE);
        btnG100M.addActionListener(e -> txtGold.setText("100000000"));

        pnlGold.add(btnG0);
        pnlGold.add(btnG1M);
        pnlGold.add(btnG10M);
        pnlGold.add(btnG50M);
        pnlGold.add(btnG100M);

        leftCard.add(createMiniFieldPanel("Giá Vàng (Gold):", pnlGold));
        leftCard.add(Box.createVerticalStrut(10));

        // 3. GIÁ NGỌC
        JTextField txtGem = createDarkTextField("0");
        txtGem.setPreferredSize(new Dimension(140, 32));

        JPanel pnlGem = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlGem.setOpaque(false);
        pnlGem.add(txtGem);

        JButton btnGem0 = createModernButton("0", new Color(100, 116, 139), Color.WHITE);
        btnGem0.addActionListener(e -> txtGem.setText("0"));
        JButton btnGem10 = createModernButton("10", new Color(6, 182, 212), Color.WHITE);
        btnGem10.addActionListener(e -> txtGem.setText("10"));
        JButton btnGem50 = createModernButton("50", new Color(6, 182, 212), Color.WHITE);
        btnGem50.addActionListener(e -> txtGem.setText("50"));
        JButton btnGem100 = createModernButton("100", new Color(6, 182, 212), Color.WHITE);
        btnGem100.addActionListener(e -> txtGem.setText("100"));
        JButton btnGem500 = createModernButton("500", new Color(6, 182, 212), Color.WHITE);
        btnGem500.addActionListener(e -> txtGem.setText("500"));

        pnlGem.add(btnGem0);
        pnlGem.add(btnGem10);
        pnlGem.add(btnGem50);
        pnlGem.add(btnGem100);
        pnlGem.add(btnGem500);

        leftCard.add(createMiniFieldPanel("Giá Ngọc (Gem / Hồng Ngọc):", pnlGem));
        leftCard.add(Box.createVerticalStrut(10));

        // 4. VẬT PHẨM ĐỔI KÈM
        JTextField txtExchangeId = createDarkTextField("-1");
        txtExchangeId.setPreferredSize(new Dimension(90, 32));
        JTextField txtExchangeQty = createDarkTextField("0");
        txtExchangeQty.setPreferredSize(new Dimension(90, 32));

        JPanel pnlEx = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlEx.setOpaque(false);
        pnlEx.add(new JLabel("Mã Item Đổi (-1 = Ko):"));
        pnlEx.add(txtExchangeId);
        pnlEx.add(new JLabel("Số Lượng Cần:"));
        pnlEx.add(txtExchangeQty);

        leftCard.add(createMiniFieldPanel("Đổi Bằng Item Khác:", pnlEx));
        leftCard.add(Box.createVerticalStrut(10));

        // 5. TRẠNG THÁI MỞ BÁN
        JCheckBox chkSell = new JCheckBox("Đang mở bán trong shop (is_sell)", true);
        chkSell.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkSell.setForeground(COLOR_TEXT_LIGHT);
        chkSell.setOpaque(false);

        JCheckBox chkNew = new JCheckBox("Hiển thị nhãn MỚI (is_new)", false);
        chkNew.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkNew.setForeground(COLOR_TEXT_LIGHT);
        chkNew.setOpaque(false);

        JPanel pnlChecks = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        pnlChecks.setOpaque(false);
        pnlChecks.add(chkSell);
        pnlChecks.add(chkNew);

        leftCard.add(createMiniFieldPanel("Trạng Thái Sản Phẩm:", pnlChecks));
        leftCard.add(Box.createVerticalGlue());

        mainGrid.add(leftCard);

        // ==========================================
        // RIGHT CARD: CHỈ SỐ OPTION CỦA VẬT PHẨM
        // ==========================================
        JPanel rightCard = new JPanel(new BorderLayout(0, 8));
        rightCard.setBackground(COLOR_PANEL_DARK);
        rightCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                "Chỉ Số Option Của Vật Phẩm",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                COLOR_ACCENT
            ),
            new EmptyBorder(10, 12, 10, 12)
        ));

        // 1. GÁN NHANH PRESETS
        JPanel pnlPresetsRow = new JPanel(new GridLayout(1, 4, 6, 0));
        pnlPresetsRow.setOpaque(false);

        JButton btnShopPreVip = createModernButton("Set Cải Trang VIP", new Color(14, 165, 233), Color.WHITE);
        JButton btnShopPreDmg = createModernButton("Sát Thương", new Color(245, 158, 11), Color.WHITE);
        JButton btnShopPreTank = createModernButton("HP & KI", new Color(16, 185, 129), Color.WHITE);
        JButton btnShopPreHsd = createModernButton("HSD 30 Ngày", new Color(168, 85, 247), Color.WHITE);

        pnlPresetsRow.add(btnShopPreVip);
        pnlPresetsRow.add(btnShopPreDmg);
        pnlPresetsRow.add(btnShopPreTank);
        pnlPresetsRow.add(btnShopPreHsd);

        JPanel pnlPresetsContainer = new JPanel(new BorderLayout(0, 4));
        pnlPresetsContainer.setOpaque(false);
        JLabel lblPreTitle = new JLabel("Gán Nhanh Bộ Chỉ Số (Presets):");
        lblPreTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblPreTitle.setForeground(COLOR_ACCENT);
        pnlPresetsContainer.add(lblPreTitle, BorderLayout.NORTH);
        pnlPresetsContainer.add(pnlPresetsRow, BorderLayout.CENTER);
        rightCard.add(pnlPresetsContainer, BorderLayout.NORTH);

        // 2. BẢNG OPTION TABLE
        String[] optCols = {"Mã Opt", "Tên Chỉ Số / Hiển Thị", "Param"};
        DefaultTableModel optModel = new DefaultTableModel(optCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tblOpt = new JTable(optModel);
        styleDarkTable(tblOpt);
        tblOpt.getColumnModel().getColumn(0).setPreferredWidth(60);
        tblOpt.getColumnModel().getColumn(1).setPreferredWidth(230);
        tblOpt.getColumnModel().getColumn(2).setPreferredWidth(70);

        JScrollPane spOpt = new JScrollPane(tblOpt);
        spOpt.getViewport().setBackground(COLOR_BG_DARK);
        spOpt.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true));
        rightCard.add(spOpt, BorderLayout.CENTER);

        // 3. THAO TÁC THÊM / XÓA OPTION
        JPanel pnlOptActions = new JPanel();
        pnlOptActions.setLayout(new BoxLayout(pnlOptActions, BoxLayout.Y_AXIS));
        pnlOptActions.setOpaque(false);

        List<String> optLabels = new ArrayList<>();
        optLabels.add("50 - Sức đánh+#%");
        optLabels.add("77 - HP+#%");
        optLabels.add("103 - KI+#%");
        optLabels.add("14 - Chí mạng+#%");
        optLabels.add("93 - Hạn sử dụng # ngày");
        optLabels.add("30 - Không thể giao dịch");
        optLabels.add("73 - Có thể giao dịch");
        for (ItemOptionTemplate iot : Manager.ITEM_OPTION_TEMPLATES) {
            if (iot != null && iot.name != null && !iot.name.trim().isEmpty()) {
                String entry = iot.id + " - " + iot.name;
                if (!optLabels.contains(entry)) optLabels.add(entry);
            }
        }

        JComboBox<String> cbOpt = new JComboBox<>(optLabels.toArray(new String[0]));
        cbOpt.setPreferredSize(new Dimension(220, 32));
        cbOpt.setBackground(new Color(15, 23, 42));
        cbOpt.setForeground(Color.WHITE);

        JTextField txtSearchShopOpt = createDarkTextField("");
        txtSearchShopOpt.setPreferredSize(new Dimension(90, 32));
        txtSearchShopOpt.setToolTipText("Gõ tìm chỉ số (HP, KI, sức đánh, ID...)");
        txtSearchShopOpt.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                String q = txtSearchShopOpt.getText().toLowerCase().trim();
                cbOpt.removeAllItems();
                for (String s : optLabels) {
                    if (q.isEmpty() || s.toLowerCase().contains(q)) {
                        cbOpt.addItem(s);
                    }
                }
            }
        });

        JTextField txtParam = createDarkTextField("10");
        txtParam.setPreferredSize(new Dimension(70, 32));

        JButton btnAddOpt = createModernButton("Thêm / Cập Nhật", new Color(14, 165, 233), Color.WHITE);
        btnAddOpt.addActionListener(e -> {
            try {
                String s = (String) cbOpt.getSelectedItem();
                if (s == null || !s.contains(" -")) {
                    JOptionPane.showMessageDialog(dlg, "Vui lòng chọn 1 option hợp lệ!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int oId = Integer.parseInt(s.substring(0, s.indexOf(" -")).trim());
                int pVal = Integer.parseInt(txtParam.getText().trim());
                ItemOption io = new ItemOption(oId, pVal);
                boolean exists = false;
                for (int r = 0; r < optModel.getRowCount(); r++) {
                    if ((int) optModel.getValueAt(r, 0) == oId) {
                        optModel.setValueAt(io.getOptionString(), r, 1);
                        optModel.setValueAt(pVal, r, 2);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    optModel.addRow(new Object[]{oId, io.getOptionString(), pVal});
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Giá trị Param không hợp lệ (phải là số nguyên)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnDelOpt = createModernButton("Xóa Đang Chọn", new Color(225, 29, 72), Color.WHITE);
        btnDelOpt.addActionListener(e -> {
            int sel = tblOpt.getSelectedRow();
            if (sel >= 0) {
                optModel.removeRow(sel);
            } else {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn 1 dòng option để xóa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            }
        });

        JButton btnClearAllOpt = createModernButton("Xóa Hết", new Color(100, 116, 139), Color.WHITE);
        btnClearAllOpt.addActionListener(e -> optModel.setRowCount(0));

        JPanel rowOptSelect = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        rowOptSelect.setOpaque(false);
        rowOptSelect.add(new JLabel("Tìm:"));
        rowOptSelect.add(txtSearchShopOpt);
        rowOptSelect.add(cbOpt);

        JPanel rowOptBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        rowOptBtns.setOpaque(false);
        rowOptBtns.add(new JLabel("Param:"));
        rowOptBtns.add(txtParam);
        rowOptBtns.add(btnAddOpt);
        rowOptBtns.add(btnDelOpt);
        rowOptBtns.add(btnClearAllOpt);

        pnlOptActions.add(rowOptSelect);
        pnlOptActions.add(rowOptBtns);
        rightCard.add(pnlOptActions, BorderLayout.SOUTH);

        mainGrid.add(rightCard);
        dlg.add(mainGrid, BorderLayout.CENTER);

        // ==========================================
        // PRESET ACTIONS LOGIC
        // ==========================================
        java.util.function.Consumer<int[][]> applyShopPreset = (int[][] presetData) -> {
            for (int[] opt : presetData) {
                int optId = opt[0];
                int param = opt[1];
                ItemOption io = new ItemOption(optId, param);
                boolean exists = false;
                for (int r = 0; r < optModel.getRowCount(); r++) {
                    if ((int) optModel.getValueAt(r, 0) == optId) {
                        optModel.setValueAt(io.getOptionString(), r, 1);
                        optModel.setValueAt(param, r, 2);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    optModel.addRow(new Object[]{optId, io.getOptionString(), param});
                }
            }
        };

        btnShopPreVip.addActionListener(e -> {
            applyShopPreset.accept(new int[][]{
                {77, 40}, {103, 40}, {50, 40}, {101, 100}, {14, 20}, {5, 100}, {106, 0}
            });
        });
        btnShopPreDmg.addActionListener(e -> {
            applyShopPreset.accept(new int[][]{
                {50, 50}, {14, 30}, {5, 150}, {95, 15}, {96, 15}
            });
        });
        btnShopPreTank.addActionListener(e -> {
            applyShopPreset.accept(new int[][]{
                {77, 80}, {103, 80}, {47, 1000}, {108, 25}
            });
        });
        btnShopPreHsd.addActionListener(e -> {
            applyShopPreset.accept(new int[][]{
                {93, 30}, {30, 0}
            });
        });

        // ==========================================
        // LOAD EXISTING DATA IF EDITING
        // ==========================================
        if (editShopItemId != null) {
            try (Connection con = DBService.gI().getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT * FROM item_shop WHERE id = ?")) {
                ps.setInt(1, editShopItemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        txtTempId.setText(String.valueOf(rs.getInt("temp_id")));
                        txtGold.setText(String.valueOf(rs.getInt("gold")));
                        txtGem.setText(String.valueOf(rs.getInt("gem")));
                        txtExchangeId.setText(String.valueOf(rs.getInt("item_exchange")));
                        txtExchangeQty.setText(String.valueOf(rs.getInt("quantity_exchange")));
                        chkSell.setSelected(rs.getBoolean("is_sell"));
                        chkNew.setSelected(rs.getBoolean("is_new"));
                    }
                }
                try (PreparedStatement psOpt = con.prepareStatement("SELECT option_id, param FROM item_shop_option WHERE item_shop_id = ?")) {
                    psOpt.setInt(1, editShopItemId);
                    try (ResultSet rsOpt = psOpt.executeQuery()) {
                        while (rsOpt.next()) {
                            int oId = rsOpt.getInt("option_id");
                            int oParam = rsOpt.getInt("param");
                            ItemOption io = new ItemOption(oId, oParam);
                            optModel.addRow(new Object[]{oId, io.getOptionString(), oParam});
                        }
                    }
                }
            } catch (Exception ex) {
                Log.error(AdminPanelFrame.class, ex);
            }
        }
        updateItemPreview.run();

        // ==========================================
        // BOTTOM ACTION BUTTONS
        // ==========================================
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(51, 65, 85)));

        JButton btnSave = createModernButton("Lưu Vào Cửa Hàng", new Color(16, 185, 129), Color.WHITE);
        btnSave.setPreferredSize(new Dimension(160, 36));
        btnSave.addActionListener(e -> {
            try {
                short tId = Short.parseShort(txtTempId.getText().trim());
                if (ItemService.gI().getTemplate(tId) == null) {
                    JOptionPane.showMessageDialog(dlg, "Mã Template #" + tId + " không tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int gold = Integer.parseInt(txtGold.getText().trim().replace(",", "").replace(".", ""));
                int gem = Integer.parseInt(txtGem.getText().trim().replace(",", "").replace(".", ""));
                int exId = Integer.parseInt(txtExchangeId.getText().trim());
                int exQty = Integer.parseInt(txtExchangeQty.getText().trim());
                boolean isSell = chkSell.isSelected();
                boolean isNew = chkNew.isSelected();

                try (Connection con = DBService.gI().getConnection()) {
                    int generatedShopItemId = editShopItemId != null ? editShopItemId : -1;
                    if (editShopItemId == null) {
                        String sql = "INSERT INTO item_shop (tab_id, temp_id, gold, gem, is_new, is_sell, item_exchange, quantity_exchange) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            ps.setInt(1, tabId);
                            ps.setShort(2, tId);
                            ps.setInt(3, gold);
                            ps.setInt(4, gem);
                            ps.setBoolean(5, isNew);
                            ps.setBoolean(6, isSell);
                            ps.setInt(7, exId);
                            ps.setInt(8, exQty);
                            ps.executeUpdate();
                            try (ResultSet rs = ps.getGeneratedKeys()) {
                                if (rs.next()) generatedShopItemId = rs.getInt(1);
                            }
                        }
                    } else {
                        String sql = "UPDATE item_shop SET temp_id=?, gold=?, gem=?, is_new=?, is_sell=?, item_exchange=?, quantity_exchange=? WHERE id=?";
                        try (PreparedStatement ps = con.prepareStatement(sql)) {
                            ps.setShort(1, tId);
                            ps.setInt(2, gold);
                            ps.setInt(3, gem);
                            ps.setBoolean(4, isNew);
                            ps.setBoolean(5, isSell);
                            ps.setInt(6, exId);
                            ps.setInt(7, exQty);
                            ps.setInt(8, editShopItemId);
                            ps.executeUpdate();
                        }
                        try (PreparedStatement psDel = con.prepareStatement("DELETE FROM item_shop_option WHERE item_shop_id = ?")) {
                            psDel.setInt(1, editShopItemId);
                            psDel.executeUpdate();
                        }
                    }

                    // Insert options
                    if (generatedShopItemId > 0 && optModel.getRowCount() > 0) {
                        try (PreparedStatement psIns = con.prepareStatement("INSERT INTO item_shop_option (item_shop_id, option_id, param) VALUES (?, ?, ?)")) {
                            for (int i = 0; i < optModel.getRowCount(); i++) {
                                int oId = (int) optModel.getValueAt(i, 0);
                                int oParam = (int) optModel.getValueAt(i, 2);
                                psIns.setInt(1, generatedShopItemId);
                                psIns.setInt(2, oId);
                                psIns.setInt(3, oParam);
                                psIns.addBatch();
                            }
                            psIns.executeBatch();
                        }
                    }

                    // Reload shop memory
                    Manager.SHOPS = ShopDAO.getShops(con);
                }

                loadItemsForSelectedShopTab();
                dlg.dispose();
                JOptionPane.showMessageDialog(this, (editShopItemId == null ? "Thêm mới" : "Cập nhật") + " vật phẩm trong Shop thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                Log.error(AdminPanelFrame.class, ex);
                JOptionPane.showMessageDialog(dlg, "Lỗi khi lưu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnClose = createModernButton("Đóng", new Color(100, 116, 139), Color.WHITE);
        btnClose.setPreferredSize(new Dimension(100, 36));
        btnClose.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnClose);

        dlg.add(btnPanel, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void reloadShops() {
        try (Connection con = DBService.gI().getConnection()) {
            Manager.SHOPS = ShopDAO.getShops(con);
            loadShopsToCombo();
            JOptionPane.showMessageDialog(this, "Nạp lại Cửa Hàng thành công (" + Manager.SHOPS.size() + " shop)!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
            JOptionPane.showMessageDialog(this, "Lỗi khi reload shop: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void refreshGiftcodeTable() {
        giftcodeTableModel.setRowCount(0);
        String sql = "SELECT id, code, gold, gem, ruby, items, created_at FROM gift_codes ORDER BY id DESC LIMIT 100";
        try (Connection con = DBService.gI().getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String itemsJson = rs.getString("items");
                int itemCount = 0;
                StringBuilder itemSummary = new StringBuilder();
                if (itemsJson != null && !itemsJson.trim().isEmpty()) {
                    try {
                        JSONArray arr = new JSONArray(itemsJson);
                        itemCount = arr.length();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            int tempId = obj.getInt("id");
                            int qty = obj.getInt("quantity");
                            ItemTemplate tmpl = ItemService.gI().getTemplate(tempId);
                            String name = (tmpl != null) ? tmpl.name : ("Item #" + tempId);
                            if (i > 0) itemSummary.append(", ");
                            itemSummary.append(name).append(" (x").append(qty).append(")");
                        }
                    } catch (Exception ignored) {
                        itemSummary.append(itemsJson);
                    }
                }

                giftcodeTableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("code"),
                    numFmt.format(rs.getInt("gold")),
                    numFmt.format(rs.getInt("gem")),
                    numFmt.format(rs.getInt("ruby")),
                    itemCount,
                    itemSummary.length() > 0 ? itemSummary.toString() : "(Chỉ tiền)",
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "N/A"
                });
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    // ==========================================
    // VISUAL GIFTCODE DIALOG (CHỌN ĐỒ TRỰC QUAN)
    // ==========================================
    static class GiftItemOption {
        int id;
        int param;
        String name;

        GiftItemOption(int id, int param, String name) {
            this.id = id;
            this.param = param;
            this.name = name;
        }

        @Override
        public String toString() {
            return "[" + id + "] " + name.replace("#", String.valueOf(param));
        }
    }

    static class GiftItemRow {
        short tempId;
        String name;
        int quantity;
        List<GiftItemOption> options = new ArrayList<>();

        GiftItemRow(short tempId, String name, int quantity) {
            this.tempId = tempId;
            this.name = name;
            this.quantity = quantity;
        }
    }

    private void openVisualGiftCodeDialog(Integer editId) {
        JDialog dlg = new JDialog(this, editId == null ? "Tạo GiftCode Mới (Chọn Đồ Trực Quan)" : "Chỉnh Sửa GiftCode (ID: " + editId + ")", true);
        dlg.setSize(1080, 720);
        dlg.setMinimumSize(new Dimension(950, 620));
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(8, 8));

        // In-memory list of items for this giftcode
        List<GiftItemRow> currentGiftItems = new ArrayList<>();

        // Top Form: Code, Type, Currencies
        JPanel topForm = new JPanel(new GridLayout(2, 4, 8, 8));
        topForm.setBorder(new TitledBorder("1. Thiết Lập Mã Quà Tặng & Tiền Tệ"));

        JTextField txtCode = new JTextField(editId == null ? "tanthu2026" : "", 12);
        String[] types = {"1 - Tất cả người chơi (1 lần/người)", "0 - Cá nhân (Dùng 1 lần)"};
        JComboBox<String> cbType = new JComboBox<>(types);
        JTextField txtGold = new JTextField("0", 10);
        JTextField txtGem = new JTextField("0", 8);
        JTextField txtRuby = new JTextField("0", 8);
        JTextField txtCount = new JTextField("9999", 8);
        JCheckBox chkActive = new JCheckBox("Cần kích hoạt tài khoản", false);

        topForm.add(new JLabel("Mã GiftCode:"));
        topForm.add(txtCode);
        topForm.add(new JLabel("Loại Mã:"));
        topForm.add(cbType);

        topForm.add(new JLabel("Vàng (Gold):"));
        topForm.add(txtGold);
        topForm.add(new JLabel("Ngọc Xanh (Gem):"));
        topForm.add(txtGem);

        topForm.add(new JLabel("Hồng Ngọc (Ruby):"));
        topForm.add(txtRuby);
        topForm.add(new JLabel("Số Lượt Nhập:"));
        topForm.add(txtCount);

        topForm.add(chkActive);

        dlg.add(topForm, BorderLayout.NORTH);

        // Center Split: Left Item Picker, Right Selected Gift Items
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setResizeWeight(0.52);

        // LEFT: Visual Item Picker
        JPanel pnlPicker = new JPanel(new BorderLayout(6, 6));
        pnlPicker.setBorder(new TitledBorder("2. Tìm & Chọn Vật Phẩm"));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchBar.add(new JLabel("Tìm Item:"));
        JTextField txtSearchItem = new JTextField(15);
        searchBar.add(txtSearchItem);
        pnlPicker.add(searchBar, BorderLayout.NORTH);

        String[] itemCols = {"ID", "Tên Vật Phẩm", "Loại", "Mô Tả"};
        DefaultTableModel tmplModel = new DefaultTableModel(itemCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblTmpl = new JTable(tmplModel);
        tblTmpl.setRowHeight(22);
        TableRowSorter<DefaultTableModel> tmplSorter = new TableRowSorter<>(tmplModel);
        tblTmpl.setRowSorter(tmplSorter);
        txtSearchItem.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String q = txtSearchItem.getText().trim();
                tmplSorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + q, 0, 1));
            }
        });
        tblTmpl.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblTmpl.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblTmpl.getColumnModel().getColumn(2).setPreferredWidth(60);
        tblTmpl.getColumnModel().getColumn(3).setPreferredWidth(180);

        for (ItemTemplate t : Manager.ITEM_TEMPLATES) {
            if (t != null) {
                tmplModel.addRow(new Object[]{t.id, t.name, t.type, t.description != null ? t.description : ""});
            }
        }
        pnlPicker.add(new JScrollPane(tblTmpl), BorderLayout.CENTER);

        // Picker controls (Quantity & Options)
        JPanel pnlItemControl = new JPanel();
        pnlItemControl.setLayout(new BoxLayout(pnlItemControl, BoxLayout.Y_AXIS));
        pnlItemControl.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel pQty = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        pQty.add(new JLabel("Số Lượng Cho:"));
        JTextField txtItemQty = new JTextField("1", 6);
        pQty.add(txtItemQty);
        pnlItemControl.add(pQty);

        // Options selector
        JPanel pOpt = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        pOpt.add(new JLabel("Chỉ Số Option:"));

        List<String> optLabels = new ArrayList<>();
        optLabels.add("50 - Sức đánh+#%");
        optLabels.add("77 - HP+#%");
        optLabels.add("103 - KI+#%");
        optLabels.add("14 - Chí mạng+#%");
        optLabels.add("93 - Hạn sử dụng # ngày");
        optLabels.add("30 - Không thể giao dịch");
        optLabels.add("73 - Tự động hồi phục HP");
        for (ItemOptionTemplate iot : Manager.ITEM_OPTION_TEMPLATES) {
            if (iot != null && iot.name != null && !iot.name.trim().isEmpty()) {
                String entry = iot.id + " - " + iot.name;
                if (!optLabels.contains(entry)) {
                    optLabels.add(entry);
                }
            }
        }
        JComboBox<String> cbOptions = new JComboBox<>(optLabels.toArray(new String[0]));
        cbOptions.setPreferredSize(new Dimension(240, 24));
        pOpt.add(cbOptions);

        pOpt.add(new JLabel("Param:"));
        JTextField txtOptParam = new JTextField("20", 5);
        pOpt.add(txtOptParam);
        pnlItemControl.add(pOpt);

        // Temp options list for currently selected item
        DefaultListModel<GiftItemOption> tempOptsModel = new DefaultListModel<>();
        JList<GiftItemOption> tempOptsList = new JList<>(tempOptsModel);
        tempOptsList.setVisibleRowCount(3);

        JPanel pOptBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JButton btnAddOpt = new JButton("Gắn Option Này");
        btnAddOpt.addActionListener(e -> {
            String sel = (String) cbOptions.getSelectedItem();
            if (sel != null) {
                int dash = sel.indexOf(" - ");
                int optId = Integer.parseInt(sel.substring(0, dash).trim());
                String optName = sel.substring(dash + 3);
                int pVal = 0;
                try {
                    pVal = Integer.parseInt(txtOptParam.getText().trim());
                } catch (Exception ignored) {
                }
                tempOptsModel.addElement(new GiftItemOption(optId, pVal, optName));
            }
        });
        pOptBtns.add(btnAddOpt);

        JButton btnRemoveOpt = new JButton("Xóa Option");
        btnRemoveOpt.addActionListener(e -> {
            int idx = tempOptsList.getSelectedIndex();
            if (idx >= 0) tempOptsModel.remove(idx);
        });
        pOptBtns.add(btnRemoveOpt);
        pnlItemControl.add(pOptBtns);
        pnlItemControl.add(new JScrollPane(tempOptsList));

        // Big Add To GiftCode Button
        JButton btnAddToGift = new JButton("THÊM MÓN NÀY VÀO GIFTCODE");
        btnAddToGift.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAddToGift.setBackground(new Color(0, 123, 255));
        btnAddToGift.setForeground(Color.WHITE);
        pnlItemControl.add(Box.createVerticalStrut(5));
        pnlItemControl.add(btnAddToGift);

        pnlPicker.add(pnlItemControl, BorderLayout.SOUTH);
        centerSplit.setLeftComponent(pnlPicker);

        // RIGHT: Added Items in this GiftCode
        JPanel pnlSelected = new JPanel(new BorderLayout(6, 6));
        pnlSelected.setBorder(new TitledBorder("3. Các Món Quà Sẽ Nhận Trong GiftCode Này"));

        String[] selCols = {"ID Item", "Tên Vật Phẩm", "Số Lượng", "Chỉ Số Options"};
        DefaultTableModel selectedGiftModel = new DefaultTableModel(selCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblSelected = new JTable(selectedGiftModel);
        tblSelected.setRowHeight(24);
        tblSelected.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblSelected.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblSelected.getColumnModel().getColumn(0).setPreferredWidth(60);
        tblSelected.getColumnModel().getColumn(1).setPreferredWidth(160);
        tblSelected.getColumnModel().getColumn(2).setPreferredWidth(65);
        tblSelected.getColumnModel().getColumn(3).setPreferredWidth(200);

        pnlSelected.add(new JScrollPane(tblSelected), BorderLayout.CENTER);

        // Right Actions & Live JSON
        JPanel pnlRightBottom = new JPanel();
        pnlRightBottom.setLayout(new BoxLayout(pnlRightBottom, BoxLayout.Y_AXIS));

        JPanel pnlDelBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btnRemoveGiftItem = new JButton("Xóa Món Đang Chọn");
        btnRemoveGiftItem.setForeground(new Color(204, 0, 0));
        pnlDelBtns.add(btnRemoveGiftItem);

        JButton btnClearAllGift = new JButton("Xóa Toàn Bộ Đồ");
        pnlDelBtns.add(btnClearAllGift);
        pnlRightBottom.add(pnlDelBtns);

        // Live JSON Preview
        JPanel pnlJson = new JPanel(new BorderLayout(4, 4));
        pnlJson.setBorder(new TitledBorder("Chuỗi JSON Tự Động Sinh:"));
        JTextArea txtJsonPreview = new JTextArea("[]", 3, 20);
        txtJsonPreview.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtJsonPreview.setLineWrap(true);
        txtJsonPreview.setEditable(false);
        pnlJson.add(new JScrollPane(txtJsonPreview), BorderLayout.CENTER);
        pnlRightBottom.add(pnlJson);

        pnlSelected.add(pnlRightBottom, BorderLayout.SOUTH);
        centerSplit.setRightComponent(pnlSelected);

        dlg.add(centerSplit, BorderLayout.CENTER);

        // Helper to refresh table & JSON
        Runnable refreshGiftView = () -> {
            selectedGiftModel.setRowCount(0);
            JSONArray arr = new JSONArray();
            for (GiftItemRow row : currentGiftItems) {
                StringBuilder optStr = new StringBuilder();
                JSONArray optArr = new JSONArray();
                for (int i = 0; i < row.options.size(); i++) {
                    GiftItemOption opt = row.options.get(i);
                    if (i > 0) optStr.append(", ");
                    optStr.append("[").append(opt.id).append(": ").append(opt.param).append("]");
                    JSONObject oObj = new JSONObject();
                    oObj.put("id", opt.id);
                    oObj.put("param", opt.param);
                    optArr.put(oObj);
                }

                selectedGiftModel.addRow(new Object[]{
                    row.tempId,
                    row.name,
                    row.quantity,
                    optStr.length() > 0 ? optStr.toString() : "(Mặc định)"
                });

                JSONObject iObj = new JSONObject();
                iObj.put("id", row.tempId);
                iObj.put("quantity", row.quantity);
                iObj.put("options", optArr);
                arr.put(iObj);
            }
            txtJsonPreview.setText(arr.toString());
        };

        // Add to gift action
        btnAddToGift.addActionListener(e -> {
            int row = tblTmpl.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn 1 vật phẩm từ bảng bên trái!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = tblTmpl.convertRowIndexToModel(row);
            short tempId = (short) tmplModel.getValueAt(modelRow, 0);
            String name = (String) tmplModel.getValueAt(modelRow, 1);

            int qty = 1;
            try {
                qty = Integer.parseInt(txtItemQty.getText().trim());
                if (qty <= 0) qty = 1;
            } catch (Exception ignored) {
            }

            GiftItemRow newRow = new GiftItemRow(tempId, name, qty);
            for (int i = 0; i < tempOptsModel.size(); i++) {
                newRow.options.add(tempOptsModel.get(i));
            }
            currentGiftItems.add(newRow);
            tempOptsModel.clear();
            refreshGiftView.run();
        });

        // Remove item from gift
        btnRemoveGiftItem.addActionListener(e -> {
            int r = tblSelected.getSelectedRow();
            if (r >= 0 && r < currentGiftItems.size()) {
                currentGiftItems.remove(r);
                refreshGiftView.run();
            }
        });

        // Clear all
        btnClearAllGift.addActionListener(e -> {
            currentGiftItems.clear();
            refreshGiftView.run();
        });

        // If Editing, load existing data
        if (editId != null) {
            try (Connection con = DBService.gI().getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT * FROM gift_codes WHERE id = ?")) {
                ps.setInt(1, editId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        txtCode.setText(rs.getString("code"));
                        cbType.setSelectedIndex(rs.getByte("type") == 0 ? 1 : 0);
                        txtGold.setText(String.valueOf(rs.getInt("gold")));
                        txtGem.setText(String.valueOf(rs.getInt("gem")));
                        txtRuby.setText(String.valueOf(rs.getInt("ruby")));
                        chkActive.setSelected(rs.getInt("active") == 1);

                        String itemsJson = rs.getString("items");
                        if (itemsJson != null && !itemsJson.trim().isEmpty()) {
                            try {
                                JSONArray arr = new JSONArray(itemsJson);
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject o = arr.getJSONObject(i);
                                    short tempId = (short) o.getInt("id");
                                    int qty = o.getInt("quantity");
                                    ItemTemplate t = ItemService.gI().getTemplate(tempId);
                                    String name = (t != null) ? t.name : ("Vật phẩm #" + tempId);
                                    GiftItemRow gir = new GiftItemRow(tempId, name, qty);
                                    if (o.has("options")) {
                                        JSONArray optArr = o.getJSONArray("options");
                                        for (int j = 0; j < optArr.length(); j++) {
                                            JSONObject optO = optArr.getJSONObject(j);
                                            int optId = optO.getInt("id");
                                            int param = optO.getInt("param");
                                            ItemOptionTemplate iot = ItemService.gI().getItemOptionTemplate(optId);
                                            String optName = (iot != null && iot.name != null) ? iot.name : ("Option " + optId);
                                            gir.options.add(new GiftItemOption(optId, param, optName));
                                        }
                                    }
                                    currentGiftItems.add(gir);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Log.error(AdminPanelFrame.class, ex);
            }
            refreshGiftView.run();
        }

        // Bottom: Save & Cancel Buttons
        JPanel southBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnSave = new JButton("LƯU GIFTCODE VÀO CSDL");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            String code = txtCode.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Mã GiftCode không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int type = cbType.getSelectedIndex() == 0 ? 1 : 0;
            int gold = 0, gem = 0, ruby = 0, countLeft = 9999;
            try { gold = Integer.parseInt(txtGold.getText().trim().replace(",", "").replace(".", "")); } catch (Exception ignored) {}
            try { gem = Integer.parseInt(txtGem.getText().trim().replace(",", "").replace(".", "")); } catch (Exception ignored) {}
            try { ruby = Integer.parseInt(txtRuby.getText().trim().replace(",", "").replace(".", "")); } catch (Exception ignored) {}
            try { countLeft = Integer.parseInt(txtCount.getText().trim().replace(",", "").replace(".", "")); } catch (Exception ignored) {}
            int active = chkActive.isSelected() ? 1 : 0;
            String itemsJson = txtJsonPreview.getText().trim();

            try (Connection con = DBService.gI().getConnection()) {
                // 1. Save to gift_codes
                if (editId == null) {
                    String sql = "INSERT INTO gift_codes (code, type, gold, gem, ruby, items, status, active, created_at) VALUES (?, ?, ?, ?, ?, ?, 0, ?, NOW())";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, code);
                        ps.setByte(2, (byte) type);
                        ps.setInt(3, gold);
                        ps.setInt(4, gem);
                        ps.setInt(5, ruby);
                        ps.setString(6, itemsJson);
                        ps.setInt(7, active);
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "UPDATE gift_codes SET code=?, type=?, gold=?, gem=?, ruby=?, items=?, active=?, updated_at=NOW() WHERE id=?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, code);
                        ps.setByte(2, (byte) type);
                        ps.setInt(3, gold);
                        ps.setInt(4, gem);
                        ps.setInt(5, ruby);
                        ps.setString(6, itemsJson);
                        ps.setInt(7, active);
                        ps.setInt(8, editId);
                        ps.executeUpdate();
                    }
                }

                // 2. Sync to giftcode table (legacy compatibility)
                try {
                    String sqlGiftcode = "INSERT INTO giftcode (code, count_left, detail) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE count_left=?, detail=?";
                    try (PreparedStatement ps = con.prepareStatement(sqlGiftcode)) {
                        ps.setString(1, code);
                        ps.setInt(2, countLeft);
                        ps.setString(3, itemsJson);
                        ps.setInt(4, countLeft);
                        ps.setString(5, itemsJson);
                        ps.executeUpdate();
                    }
                } catch (Exception ignored) {
                }

                refreshGiftcodeTable();
                dlg.dispose();
                JOptionPane.showMessageDialog(this, "Lưu GiftCode '" + code + "' thành công với " + currentGiftItems.size() + " món đồ!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                Log.error(AdminPanelFrame.class, ex);
                JOptionPane.showMessageDialog(dlg, "Lỗi khi lưu GiftCode: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        southBar.add(btnSave);

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dlg.dispose());
        southBar.add(btnClose);

        dlg.add(southBar, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void handleEditSelectedGiftCode() {
        int row = giftcodeTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 GiftCode từ bảng để sửa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int codeId = (int) giftcodeTableModel.getValueAt(row, 0);
        openVisualGiftCodeDialog(codeId);
    }

    private void handleDeleteGiftcode() {
        int row = giftcodeTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn GiftCode cần xóa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int codeId = (int) giftcodeTableModel.getValueAt(row, 0);
        String code = (String) giftcodeTableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa GiftCode '" + code + "' khỏi CSDL?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection con = DBService.gI().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM gift_codes WHERE id = ?")) {
                    ps.setInt(1, codeId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM giftcode WHERE code = ?")) {
                    ps.setString(1, code);
                    ps.executeUpdate();
                }
                refreshGiftcodeTable();
                JOptionPane.showMessageDialog(this, "Đã xóa GiftCode '" + code + "' thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                Log.error(AdminPanelFrame.class, ex);
            }
        }
    }

    // ==========================================
    // 7. TAB THÔNG BÁO & BẢO TRÌ HỆ THỐNG
    // ==========================================
    private JPanel createSystemPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Broadcast Section
        JPanel pnlBroadcast = new JPanel(new BorderLayout(10, 10));
        pnlBroadcast.setBorder(new TitledBorder("Gửi Thông Báo Toàn Server (Broadcast)"));

        JTextArea txtBroadcast = new JTextArea("Chào mừng các cư dân đến với Dragon Boy Polar Server!");
        txtBroadcast.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBroadcast.setLineWrap(true);
        pnlBroadcast.add(new JScrollPane(txtBroadcast), BorderLayout.CENTER);

        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnSendYellow = new JButton("Gửi Thông Báo Chữ Vàng (Toàn Máy Chủ)");
        btnSendYellow.setBackground(new Color(255, 193, 7));
        btnSendYellow.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSendYellow.addActionListener(e -> {
            String text = txtBroadcast.getText().trim();
            if (!text.isEmpty()) {
                Service.getInstance().sendThongBaoAllPlayer(text);
                JOptionPane.showMessageDialog(this, "Đã gửi thông báo đến toàn server thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        pnlBtn.add(btnSendYellow);

        JButton btnSendGlobal = new JButton("Gửi Kênh Thế Giới [ADMIN]");
        btnSendGlobal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSendGlobal.addActionListener(e -> {
            String text = txtBroadcast.getText().trim();
            if (!text.isEmpty()) {
                Service.getInstance().sendThongBaoAllPlayer("[ADMIN]: " + text);
                JOptionPane.showMessageDialog(this, "Đã phát tin nhắn kênh thế giới!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        pnlBtn.add(btnSendGlobal);
        pnlBroadcast.add(pnlBtn, BorderLayout.SOUTH);

        panel.add(pnlBroadcast);

        // Maintenance Section
        JPanel pnlMaintain = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        pnlMaintain.setBorder(new TitledBorder("Bảo Trì Máy Chủ"));

        pnlMaintain.add(new JLabel("Hẹn giờ đếm ngược bảo trì server sau (giây):"));
        JTextField txtMaintainSeconds = new JTextField("60", 6);
        pnlMaintain.add(txtMaintainSeconds);

        JButton btnStartMaintain = new JButton("Bắt Đầu Đếm Ngược Bảo Trì");
        btnStartMaintain.setBackground(new Color(220, 53, 69));
        btnStartMaintain.setForeground(Color.WHITE);
        btnStartMaintain.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnStartMaintain.addActionListener(e -> {
            try {
                int sec = Integer.parseInt(txtMaintainSeconds.getText().trim());
                int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận đếm ngược bảo trì server sau " + sec + " giây?", "Cảnh báo bảo trì", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Maintenance.gI().start(sec);
                    JOptionPane.showMessageDialog(this, "Đã kích hoạt đếm ngược bảo trì sau " + sec + " giây!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số giây không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        pnlMaintain.add(btnStartMaintain);

        panel.add(pnlMaintain);

        // Restart Server Section
        JPanel pnlRestart = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        pnlRestart.setBorder(new TitledBorder("Khởi Động Lại Server (Restart Server)"));

        JButton btnQuickRestart = new JButton("Khởi Động Lại Ngay");
        btnQuickRestart.setBackground(new Color(220, 53, 69));
        btnQuickRestart.setForeground(Color.WHITE);
        btnQuickRestart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnQuickRestart.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Xác nhận lưu toàn bộ dữ liệu và khởi động lại Server ngay?", 
                "Xác nhận Restart", 
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this, "Đang lưu dữ liệu và khởi động lại Server trong giây lát...", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                ServerManager.gI().restart(false);
            }
        });
        pnlRestart.add(btnQuickRestart);

        JButton btnFullRestart = new JButton("Khởi Động Lại Toàn Bộ");
        btnFullRestart.setBackground(new Color(111, 66, 193));
        btnFullRestart.setForeground(Color.WHITE);
        btnFullRestart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnFullRestart.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Xác nhận lưu dữ liệu và khởi động lại cả Login Server và Game Server qua Restart_Server.bat?", 
                "Xác nhận Restart Toàn Bộ", 
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this, "Đang lưu dữ liệu và kích hoạt Restart_Server.bat...", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                ServerManager.gI().restart(true);
            }
        });
        pnlRestart.add(btnFullRestart);

        pnlRestart.add(new JLabel("  |  Đếm ngược (giây):"));
        JTextField txtRestartCountdown = new JTextField("15", 5);
        pnlRestart.add(txtRestartCountdown);

        JButton btnCountdownRestart = new JButton("Đếm Ngược & Khởi Động Lại");
        btnCountdownRestart.setBackground(new Color(255, 193, 7));
        btnCountdownRestart.setForeground(Color.BLACK);
        btnCountdownRestart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCountdownRestart.addActionListener(e -> {
            try {
                int sec = Integer.parseInt(txtRestartCountdown.getText().trim());
                if (sec < 5) sec = 5;
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "Xác nhận đếm ngược " + sec + " giây và khởi động lại Server?", 
                    "Xác nhận đếm ngược", 
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    ServerManager.gI().restartWithCountdown(sec, false);
                    JOptionPane.showMessageDialog(this, "Đã phát thông báo đếm ngược " + sec + " giây và sẽ tự động Restart!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số giây không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        pnlRestart.add(btnCountdownRestart);

        panel.add(pnlRestart);

        // Event Management Section
        JPanel pnlEvent = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        pnlEvent.setBorder(new TitledBorder("Cấu Hình Sự Kiện Server (Realtime Event Switcher)"));

        lblCurrentEventStatus = new JLabel("Sự kiện hiện tại: " + getEventName(Manager.EVENT_SEVER));
        lblCurrentEventStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCurrentEventStatus.setForeground(new Color(255, 152, 0));
        pnlEvent.add(lblCurrentEventStatus);

        pnlEvent.add(new JLabel("  |  Chuyển sang:"));
        String[] eventOptions = {
            "0 - Không có sự kiện (Mặc định khi mở server)",
            "1 - Sự kiện Halloween (Kẹo bí ngô, Ma Trơi, Bí Ma)",
            "2 - Sự kiện 20/11 (Nhà giáo, hoa hồng)",
            "3 - Sự kiện Noel (Giáng sinh, Tuần Lộc, Ông Già Noel)",
            "4 - Sự kiện Tết Nguyên Đán (Bánh chưng, Nồi Bánh, Lân Con)",
            "5 - Sự kiện 8/3 (Quốc tế Phụ nữ, Capsule hồng)",
            "6 - Sự kiện Giỗ Tổ Hùng Vương (Vua Hùng, Sơn Tinh, Thủy Tinh)",
            "7 - Sự kiện Trung Thu (Khỉ Đột, Nguyệt Thần)",
            "8 - Sự kiện Mùa Hè (Xe Nước Mía)"
        };
        JComboBox<String> cbEvents = new JComboBox<>(eventOptions);
        if (Manager.EVENT_SEVER >= 0 && Manager.EVENT_SEVER < eventOptions.length) {
            cbEvents.setSelectedIndex(Manager.EVENT_SEVER);
        }
        pnlEvent.add(cbEvents);

        JCheckBox chkBroadcastEv = new JCheckBox("Gửi thông báo chữ vàng toàn server", true);
        pnlEvent.add(chkBroadcastEv);

        JButton btnApplyEv = new JButton("Kích Hoạt Sự Kiện Ngay");
        btnApplyEv.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnApplyEv.setBackground(new Color(40, 167, 69));
        btnApplyEv.setForeground(Color.WHITE);
        btnApplyEv.addActionListener(e -> {
            int sel = cbEvents.getSelectedIndex();
            setServerEvent(sel, chkBroadcastEv.isSelected());
            lblCurrentEventStatus.setText("Sự kiện hiện tại: " + getEventName(Manager.EVENT_SEVER));
            JOptionPane.showMessageDialog(this, "Đã chuyển đổi sang: " + getEventName(sel) + " thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        });
        pnlEvent.add(btnApplyEv);

        panel.add(pnlEvent);
        return panel;
    }

    public static String getEventName(int eventId) {
        return nro.event.EventManager.gI().getEventName(eventId);
    }

    public static void setServerEvent(int eventId, boolean broadcast) {
        // Kích hoạt sự kiện thời gian thực (sinh/xóa Boss, NPC, cờ sự kiện)
        nro.event.EventManager.gI().applyEvent(eventId);
        String evName = getEventName(eventId);
        if (broadcast) {
            Service.getInstance().sendThongBaoAllPlayer("[THÔNG BÁO] Máy chủ đã chuyển sang: " + evName + "!");
        }
    }

    // ==========================================
    // 8. AUTO REFRESH TIMER & STATUS BAR
    // ==========================================
    private void startAutoRefreshTimer() {
        Timer timer = new Timer(3000, e -> updateServerStatus());
        timer.start();
    }

    private void updateServerStatus() {
        if (isDialogOpen || !this.isShowing() || !this.isActive()) {
            return;
        }
        try {
            // Online count
            int online = Client.gI().getPlayers().size();
            lblOnline.setText("Online: " + online);

            // Sessions count
            int sessions = Client.gI().getSessions().size();
            lblSessions.setText("Sessions: " + sessions);

            // Boss count
            int bossCount = BossManager.BOSSES_IN_GAME.size();
            lblBossCount.setText("Boss: " + bossCount);

            // Memory
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long total = rt.totalMemory() / 1024 / 1024;
            lblMemory.setText("RAM: " + used + " MB / " + total + " MB");

            // Uptime
            long uptime = (System.currentTimeMillis() - startTime) / 1000;
            long h = uptime / 3600;
            long m = (uptime % 3600) / 60;
            long s = uptime % 60;
            lblUptime.setText(String.format("Uptime: %02d:%02d:%02d", h, m, s));
        } catch (Exception ignored) {
        }
    }

    private void refreshAllData() {
        updateServerStatus();
        refreshPlayerList();
        refreshBossTable();
        refreshShopTables();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdminPanelFrame.gI().setVisible(true);
        });
    }

    // ==========================================
    // TAB QUẢN LÝ NPC BẢN ĐỒ (MAP NPC MANAGER)
    // ==========================================
    private JPanel createNpcPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(COLOR_BG_DARK);
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Map Selection Bar
        JPanel topMapBar = new JPanel(new BorderLayout(10, 10));
        topMapBar.setBackground(COLOR_PANEL_DARK);
        topMapBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER_DARK),
            new EmptyBorder(8, 12, 8, 12)
        ));

        JPanel leftMap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftMap.setOpaque(false);
        JLabel lblChooseMap = new JLabel("Chọn Bản Đồ Quản Lý NPC:");
        lblChooseMap.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblChooseMap.setForeground(COLOR_TEXT_LIGHT);

        cbMapList = new JComboBox<>();
        cbMapList.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbMapList.setBackground(new Color(15, 23, 42));
        cbMapList.setForeground(Color.WHITE);
        cbMapList.setPreferredSize(new Dimension(340, 32));
        loadMapsToComboBox();
        cbMapList.addActionListener(e -> onMapSelected());

        leftMap.add(lblChooseMap);
        leftMap.add(cbMapList);

        JPanel rightMap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightMap.setOpaque(false);

        JButton btnReloadMapNpc = createModernButton("Tải Lại NPC Của Map", new Color(99, 102, 241), Color.WHITE);
        btnReloadMapNpc.addActionListener(e -> reloadCurrentMapNpcs());

        JButton btnBroadcastNpc = createModernButton("NPC Phát Biểu (Chat)", new Color(245, 158, 11), Color.WHITE);
        btnBroadcastNpc.addActionListener(e -> makeNpcChat());

        rightMap.add(btnReloadMapNpc);
        rightMap.add(btnBroadcastNpc);

        topMapBar.add(leftMap, BorderLayout.WEST);
        topMapBar.add(rightMap, BorderLayout.EAST);
        root.add(topMapBar, BorderLayout.NORTH);

        // Center: SplitPane with Table (Left) and Edit/Add Form (Right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.62);
        splitPane.setBackground(COLOR_BG_DARK);
        splitPane.setBorder(null);

        // NPC Table
        String[] npcCols = {"STT", "Temp ID", "Tên NPC", "Tọa Độ X", "Tọa Độ Y", "Avatar ID", "Trạng Thái"};
        modelNpcs = new DefaultTableModel(npcCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblNpcs = new JTable(modelNpcs);
        styleDarkTable(tblNpcs);
        tblNpcs.setRowHeight(26);
        tblNpcs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblNpcs.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblNpcs.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblNpcs.getColumnModel().getColumn(1).setPreferredWidth(65);
        tblNpcs.getColumnModel().getColumn(2).setPreferredWidth(170);
        tblNpcs.getColumnModel().getColumn(3).setPreferredWidth(70);
        tblNpcs.getColumnModel().getColumn(4).setPreferredWidth(70);
        tblNpcs.getColumnModel().getColumn(5).setPreferredWidth(70);
        tblNpcs.getColumnModel().getColumn(6).setPreferredWidth(80);

        tblNpcs.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onNpcRowClicked();
            }
        });

        JScrollPane scrollTable = new JScrollPane(tblNpcs);
        scrollTable.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_DARK));
        splitPane.setLeftComponent(scrollTable);

        // Right Form: Add/Edit NPC
        JPanel rightForm = new JPanel(new BorderLayout(8, 8));
        rightForm.setBackground(COLOR_PANEL_DARK);
        rightForm.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DARK),
                "Thêm Mới Hoặc Điều Chỉnh NPC",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), COLOR_ACCENT
            ),
            new EmptyBorder(10, 12, 10, 12)
        ));

        JPanel formFields = new JPanel(new GridLayout(6, 2, 8, 10));
        formFields.setOpaque(false);

        JLabel lblPreset = new JLabel("Chọn mẫu NPC nhanh:");
        lblPreset.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPreset.setForeground(Color.WHITE);
        cbNpcTemplates = new JComboBox<>(getPresetNpcOptions());
        cbNpcTemplates.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbNpcTemplates.setBackground(new Color(15, 23, 42));
        cbNpcTemplates.setForeground(Color.WHITE);
        cbNpcTemplates.addActionListener(e -> onPresetNpcSelected());

        JLabel lblTempId = new JLabel("Mã NPC Template ID:");
        lblTempId.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTempId.setForeground(Color.WHITE);
        txtNpcTempId = createDarkTextField("13");

        JLabel lblX = new JLabel("Tọa Độ X (pixels):");
        lblX.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblX.setForeground(Color.WHITE);
        txtNpcX = createDarkTextField("500");

        JLabel lblY = new JLabel("Tọa Độ Y (pixels):");
        lblY.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblY.setForeground(Color.WHITE);
        txtNpcY = createDarkTextField("400");

        JLabel lblAvatar = new JLabel("Avatar ID (Icon/Đầu):");
        lblAvatar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAvatar.setForeground(Color.WHITE);
        txtNpcAvatar = createDarkTextField("564");

        JLabel lblStatus = new JLabel("Trạng Thái NPC (status):");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(Color.WHITE);
        txtNpcStatus = createDarkTextField("0");

        formFields.add(lblPreset); formFields.add(cbNpcTemplates);
        formFields.add(lblTempId); formFields.add(txtNpcTempId);
        formFields.add(lblX); formFields.add(txtNpcX);
        formFields.add(lblY); formFields.add(txtNpcY);
        formFields.add(lblAvatar); formFields.add(txtNpcAvatar);
        formFields.add(lblStatus); formFields.add(txtNpcStatus);

        rightForm.add(formFields, BorderLayout.NORTH);

        // Buttons & Options
        JPanel formBottom = new JPanel(new BorderLayout(6, 6));
        formBottom.setOpaque(false);

        chkSaveDbNpc = new JCheckBox("Lưu vĩnh viễn vào CSDL (bảng map_template)", true);
        chkSaveDbNpc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkSaveDbNpc.setForeground(COLOR_ACCENT);
        chkSaveDbNpc.setOpaque(false);
        formBottom.add(chkSaveDbNpc, BorderLayout.NORTH);

        JPanel btnActions = new JPanel(new GridLayout(3, 1, 6, 8));
        btnActions.setOpaque(false);

        JButton btnAddNpc = createModernButton("Thêm NPC Vào Map Này", new Color(34, 197, 94), Color.WHITE);
        btnAddNpc.addActionListener(e -> addNpcToCurrentMap());

        JButton btnUpdateNpc = createModernButton("Cập Nhật NPC Đang Chọn", new Color(245, 158, 11), Color.WHITE);
        btnUpdateNpc.addActionListener(e -> updateSelectedNpc());

        JButton btnDeleteNpc = createModernButton("Xóa NPC Khỏi Map", new Color(225, 29, 72), Color.WHITE);
        btnDeleteNpc.addActionListener(e -> deleteSelectedNpc());

        btnActions.add(btnAddNpc);
        btnActions.add(btnUpdateNpc);
        btnActions.add(btnDeleteNpc);
        formBottom.add(btnActions, BorderLayout.CENTER);

        rightForm.add(formBottom, BorderLayout.SOUTH);
        splitPane.setRightComponent(rightForm);

        root.add(splitPane, BorderLayout.CENTER);
        return root;
    }

    private String[] getPresetNpcOptions() {
        return new String[]{
            "-- Chọn mẫu hoặc nhập ID bên dưới --",
            "13 - Quy Lão Kame (Avatar: 564)",
            "21 - Bà Hạt Mít (Avatar: 1410)",
            "39 - Santa (Avatar: 2993)",
            "7 - Bunma (Avatar: 562)",
            "37 - Bunma Tương Lai (Avatar: 562)",
            "8 - Dende (Avatar: 350)",
            "9 - Appule (Avatar: 565)",
            "18 - Thần Mèo Karin (Avatar: 352)",
            "19 - Thượng Đế (Avatar: 353)",
            "20 - Thần Vũ Trụ (Avatar: 354)",
            "24 - Rồng Thiêng",
            "5 - Con Mèo (Avatar: 351)",
            "78 - Obito (Avatar: 14047)",
            "77 - Cây Thông Noel",
            "72 - Toribot (Avatar: 12172)",
            "66 - Nồi Bánh Chưng",
            "67 - Mr. Popo",
            "69 - Thỏ Đại Ca",
            "52 - Hùng Vương",
            "53 - Tapion",
            "54 - Ly Tiểu Nương",
            "55 - Bill",
            "56 - Whis",
            "60 - Goku SSJ",
            "22 - Trọng Tài ĐHVT",
            "28 - Cửa Hàng Ký Gửi"
        };
    }

    private void onPresetNpcSelected() {
        int index = cbNpcTemplates.getSelectedIndex();
        if (index <= 0) return;
        String selected = (String) cbNpcTemplates.getSelectedItem();
        if (selected == null) return;
        try {
            int dashIdx = selected.indexOf(" - ");
            if (dashIdx > 0) {
                int id = Integer.parseInt(selected.substring(0, dashIdx).trim());
                txtNpcTempId.setText(String.valueOf(id));

                int avtIdx = selected.indexOf("Avatar: ");
                if (avtIdx > 0) {
                    int endParen = selected.indexOf(")", avtIdx);
                    String avtStr = selected.substring(avtIdx + 8, endParen > 0 ? endParen : selected.length()).trim();
                    txtNpcAvatar.setText(avtStr);
                } else {
                    txtNpcAvatar.setText("0");
                }
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }

    private void loadMapsToComboBox() {
        if (cbMapList == null) return;
        cbMapList.removeAllItems();
        if (Manager.MAPS != null) {
            for (nro.models.map.Map m : Manager.MAPS) {
                if (m != null) {
                    cbMapList.addItem(m.mapId + " - " + m.mapName);
                }
            }
        }
        if (cbMapList.getItemCount() > 0) {
            cbMapList.setSelectedIndex(0);
        }
    }

    private void onMapSelected() {
        int idx = cbMapList.getSelectedIndex();
        if (idx < 0 || Manager.MAPS == null || idx >= Manager.MAPS.size()) return;
        selectedMap = Manager.MAPS.get(idx);
        loadNpcsForSelectedMap();
    }

    private void loadNpcsForSelectedMap() {
        if (modelNpcs == null) return;
        modelNpcs.setRowCount(0);
        currentMapNpcList.clear();
        if (selectedMap == null || selectedMap.npcs == null) return;

        int stt = 1;
        for (nro.models.npc.Npc npc : selectedMap.npcs) {
            if (npc != null) {
                currentMapNpcList.add(npc);
                String name = getNpcName(npc.tempId);
                modelNpcs.addRow(new Object[]{
                    stt++, npc.tempId, name, npc.cx, npc.cy, npc.avartar, npc.status
                });
            }
        }
    }

    private void onNpcRowClicked() {
        int row = tblNpcs.getSelectedRow();
        if (row >= 0 && row < currentMapNpcList.size()) {
            nro.models.npc.Npc npc = currentMapNpcList.get(row);
            txtNpcTempId.setText(String.valueOf(npc.tempId));
            txtNpcX.setText(String.valueOf(npc.cx));
            txtNpcY.setText(String.valueOf(npc.cy));
            txtNpcAvatar.setText(String.valueOf(npc.avartar));
            txtNpcStatus.setText(String.valueOf(npc.status));
        }
    }

    private void addNpcToCurrentMap() {
        if (selectedMap == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn bản đồ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int tempId = Integer.parseInt(txtNpcTempId.getText().trim());
            int x = Integer.parseInt(txtNpcX.getText().trim());
            int y = Integer.parseInt(txtNpcY.getText().trim());
            int avatar = Integer.parseInt(txtNpcAvatar.getText().trim());
            int status = Integer.parseInt(txtNpcStatus.getText().trim());

            nro.models.npc.Npc npc = NpcFactory.createNPC(selectedMap.mapId, status, x, y, tempId, avatar);
            if (npc != null) {
                selectedMap.addNpc(npc);
                refreshMapNpcInGame(selectedMap);

                if (chkSaveDbNpc.isSelected()) {
                    saveMapNpcsToDatabase(selectedMap.mapId, selectedMap.npcs);
                }

                logConsole("Đã thêm NPC [" + getNpcName(tempId) + " (ID: " + tempId + ")] vào Map " + selectedMap.mapId + " (" + selectedMap.mapName + ")");
                JOptionPane.showMessageDialog(this, "Đã thêm NPC thành công vào bản đồ!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadNpcsForSelectedMap();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể khởi tạo NPC này!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi nhập liệu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSelectedNpc() {
        int row = tblNpcs.getSelectedRow();
        if (row < 0 || row >= currentMapNpcList.size() || selectedMap == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một NPC trong bảng để sửa!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            nro.models.npc.Npc npc = currentMapNpcList.get(row);
            int tempId = Integer.parseInt(txtNpcTempId.getText().trim());
            int x = Integer.parseInt(txtNpcX.getText().trim());
            int y = Integer.parseInt(txtNpcY.getText().trim());
            int avatar = Integer.parseInt(txtNpcAvatar.getText().trim());
            int status = Integer.parseInt(txtNpcStatus.getText().trim());

            npc.tempId = tempId;
            npc.cx = x;
            npc.cy = y;
            npc.avartar = avatar;
            npc.status = status;

            refreshMapNpcInGame(selectedMap);

            if (chkSaveDbNpc.isSelected()) {
                saveMapNpcsToDatabase(selectedMap.mapId, selectedMap.npcs);
            }

            logConsole("Đã cập nhật NPC [" + getNpcName(tempId) + " (ID: " + tempId + ")] trên Map " + selectedMap.mapId);
            JOptionPane.showMessageDialog(this, "Đã cập nhật NPC thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            loadNpcsForSelectedMap();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật NPC: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedNpc() {
        int row = tblNpcs.getSelectedRow();
        if (row < 0 || row >= currentMapNpcList.size() || selectedMap == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn NPC muốn xóa!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        nro.models.npc.Npc npc = currentMapNpcList.get(row);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Bạn có chắc muốn xóa NPC [" + getNpcName(npc.tempId) + " (ID: " + npc.tempId + ")] khỏi map " + selectedMap.mapName + "?",
            "Xác nhận xóa NPC", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            selectedMap.npcs.remove(npc);
            Manager.NPCS.remove(npc);
            refreshMapNpcInGame(selectedMap);

            if (chkSaveDbNpc.isSelected()) {
                saveMapNpcsToDatabase(selectedMap.mapId, selectedMap.npcs);
            }

            logConsole("Đã xóa NPC " + npc.tempId + " khỏi Map " + selectedMap.mapId);
            JOptionPane.showMessageDialog(this, "Đã xóa NPC khỏi bản đồ!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            loadNpcsForSelectedMap();
        }
    }

    private void reloadCurrentMapNpcs() {
        if (selectedMap == null) return;
        refreshMapNpcInGame(selectedMap);
        loadNpcsForSelectedMap();
        logConsole("Đã làm mới NPC trên Map " + selectedMap.mapId + " cho toàn bộ người chơi.");
        JOptionPane.showMessageDialog(this, "Đã làm mới danh sách NPC trên bản đồ!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void makeNpcChat() {
        int row = tblNpcs.getSelectedRow();
        if (row < 0 || row >= currentMapNpcList.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một NPC trong bảng để phát biểu!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }
        nro.models.npc.Npc npc = currentMapNpcList.get(row);
        String chat = JOptionPane.showInputDialog(this, "Nhập câu nói cho NPC [" + getNpcName(npc.tempId) + "]:", "Chúc các cư dân online vui vẻ!");
        if (chat != null && !chat.trim().isEmpty()) {
            npc.npcChat(chat.trim());
            logConsole("NPC [" + getNpcName(npc.tempId) + "] nói: " + chat);
        }
    }

    public static void refreshMapNpcInGame(nro.models.map.Map map) {
        if (map == null || map.zones == null) return;
        for (Zone zone : map.zones) {
            if (zone == null) continue;
            for (Player pl : zone.getPlayers()) {
                if (pl != null && pl.getSession() != null) {
                    try {
                        Service.getInstance().clearMap(pl);
                        zone.mapInfo(pl);
                        zone.loadAnotherToMe(pl);
                        zone.load_Me_To_Another(pl);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public static void saveMapNpcsToDatabase(int mapId, List<nro.models.npc.Npc> npcs) {
        try {
            List<String> items = new ArrayList<>();
            for (nro.models.npc.Npc npc : npcs) {
                if (npc != null) {
                    items.add("\"[" + npc.tempId + "," + npc.cx + "," + npc.cy + "," + npc.avartar + "]\"");
                }
            }
            String npcsJson = "[" + String.join(",", items) + "]";

            try (Connection con = DBService.gI().getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE map_template SET npcs = ? WHERE id = ?")) {
                ps.setString(1, npcsJson);
                ps.setInt(2, mapId);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
    }



    // ==========================================
    // TAB QUẢN LÝ SỰ KIỆN & TỶ LỆ MÁY CHỦ
    // ==========================================
    private JPanel createEventPanel() {
        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBackground(COLOR_BG_DARK);
        root.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel formCard = new JPanel(new GridLayout(6, 2, 12, 12));
        formCard.setBackground(COLOR_PANEL_DARK);
        formCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DARK),
                "Cấu Hình & Bật Tắt Sự Kiện Server",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), COLOR_ACCENT
            ),
            new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel lblEv = new JLabel("Chọn Sự Kiện Kích Hoạt:");
        lblEv.setForeground(Color.WHITE);
        lblEv.setFont(new Font("Segoe UI", Font.BOLD, 13));

        String[] evNames = {
            "0 - Không Có Sự Kiện (Tắt hết hoạt động mùa)",
            "1 - Sự Kiện Halloween (Kẹo ma quỷ / Bí ngô / Obito)",
            "2 - Sự Kiện Nhà Giáo 20/11 (Bông hoa / Tri ân)",
            "3 - Sự Kiện Giáng Sinh Noel (Cây thông / Quà / Tuần lộc)",
            "4 - Sự Kiện Tết Cổ Truyền (Bánh chưng / Nồi bánh / Lân)",
            "5 - Sự Kiện Quốc Tế Phụ Nữ 8/3 (Capsule hồng)",
            "6 - Sự Kiện Giỗ Tổ Hùng Vương (Bánh giầy / Vua Hùng)",
            "7 - Sự Kiện Trung Thu (Lồng đèn / Khỉ đột / Nguyệt thần)",
            "8 - Sự Kiện Mùa Hè (Xe nước mía / Giải nhiệt)"
        };
        cbEventList = new JComboBox<>(evNames);
        cbEventList.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbEventList.setBackground(new Color(15, 23, 42));
        cbEventList.setForeground(Color.WHITE);
        if (Manager.EVENT_SEVER >= 0 && Manager.EVENT_SEVER < evNames.length) {
            cbEventList.setSelectedIndex(Manager.EVENT_SEVER);
        }

        JLabel lblExp = new JLabel("Tỷ Lệ Kinh Nghiệm (EXP Rate):");
        lblExp.setForeground(Color.WHITE);
        lblExp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        spExpRate = new JSpinner(new SpinnerNumberModel(Manager.RATE_EXP_SERVER, 1, 100, 1));
        spExpRate.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel lblGem = new JLabel("Đánh Quái Rơi Ngọc Xanh:");
        lblGem.setForeground(Color.WHITE);
        lblGem.setFont(new Font("Segoe UI", Font.BOLD, 13));
        chkMonsterDropGem = new JCheckBox("Bật tính năng đánh quái rơi ngọc", Manager.danhquairoingoc == 1);
        chkMonsterDropGem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkMonsterDropGem.setForeground(COLOR_SUCCESS);
        chkMonsterDropGem.setOpaque(false);
        chkMonsterDropGem.addActionListener(e -> Manager.danhquairoingoc = chkMonsterDropGem.isSelected() ? 1 : 0);

        JLabel lblDuaTop = new JLabel("Thời Gian Đua Top:");
        lblDuaTop.setForeground(Color.WHITE);
        lblDuaTop.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtDuaTopTime = createDarkTextField(Manager.timeStartDuaTop + " - " + Manager.timeEndDuaTop);

        JButton btnApplyEvent = createModernButton("Áp Dụng Cấu Hình Sự Kiện", new Color(16, 185, 129), Color.WHITE);
        btnApplyEvent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnApplyEvent.addActionListener(e -> {
            int ev = cbEventList.getSelectedIndex();
            byte exp = ((Number) spExpRate.getValue()).byteValue();
            Manager.RATE_EXP_SERVER = exp;
            Manager.danhquairoingoc = chkMonsterDropGem.isSelected() ? 1 : 0;
            setServerEvent(ev, true);
            lblEventCurrentDisplay.setText("Sự kiện hiện tại: " + getEventName(Manager.EVENT_SEVER) + " (EXP x" + exp + ")");
            logConsole("Đã cập nhật sự kiện: [" + getEventName(ev) + "], EXP: x" + exp + ", Quái rơi ngọc: " + (Manager.danhquairoingoc == 1 ? "BẬT" : "TẮT"));
            JOptionPane.showMessageDialog(this, "Đã lưu và áp dụng sự kiện máy chủ thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnBroadcastEvent = createModernButton("Báo Sự Kiện Toàn Server", new Color(99, 102, 241), Color.WHITE);
        btnBroadcastEvent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBroadcastEvent.addActionListener(e -> {
            int ev = cbEventList.getSelectedIndex();
            String evName = getEventName(ev);
            String text = "Máy chủ đang diễn ra [" + evName + "], EXP x" + Manager.RATE_EXP_SERVER + "! Chúc các cư dân chơi game vui vẻ!";
            Service.getInstance().sendThongBaoAllPlayer(text);
            logConsole("Đã phát thông báo sự kiện: " + text);
            JOptionPane.showMessageDialog(this, "Đã gửi thông báo sự kiện tới tất cả người chơi!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        });

        formCard.add(lblEv); formCard.add(cbEventList);
        formCard.add(lblExp); formCard.add(spExpRate);
        formCard.add(lblGem); formCard.add(chkMonsterDropGem);
        formCard.add(lblDuaTop); formCard.add(txtDuaTopTime);
        formCard.add(btnApplyEvent); formCard.add(btnBroadcastEvent);

        lblEventCurrentDisplay = new JLabel("Sự kiện hiện tại: " + getEventName(Manager.EVENT_SEVER) + " (EXP x" + Manager.RATE_EXP_SERVER + ")");
        lblEventCurrentDisplay.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEventCurrentDisplay.setForeground(new Color(251, 146, 60));

        JPanel topContainer = new JPanel(new BorderLayout(8, 8));
        topContainer.setOpaque(false);
        topContainer.add(lblEventCurrentDisplay, BorderLayout.NORTH);
        topContainer.add(formCard, BorderLayout.CENTER);
        root.add(topContainer, BorderLayout.NORTH);

        // Event descriptions card
        JPanel descCard = new JPanel(new BorderLayout(8, 8));
        descCard.setBackground(COLOR_PANEL_DARK);
        descCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DARK),
                "Chi Tiết & Hướng Dẫn Về Các Sự Kiện Trong Game",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), COLOR_ACCENT
            ),
            new EmptyBorder(10, 12, 10, 12)
        ));

        JTextArea txtDesc = new JTextArea();
        txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDesc.setBackground(new Color(15, 23, 42));
        txtDesc.setForeground(new Color(226, 232, 240));
        txtDesc.setEditable(false);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBorder(new EmptyBorder(10, 12, 10, 12));
        txtDesc.setText(
            "★ 0 - Không Có Sự Kiện: Tắt mọi hoạt động đổi quà, nấu bánh của các mùa.\n\n" +
            "★ 1 - Sự Kiện Halloween: Kích hoạt NPC Obito, rơi Kẹo Ma Quỷ, Bí Ngô, làm Rương Ma Quỷ.\n\n" +
            "★ 2 - Sự Kiện 20/11: Kích hoạt tặng Hoa Hồng, Hoa Vàng, Bó Hoa tri ân thầy cô tại NPC Quy Lão.\n\n" +
            "★ 3 - Sự Kiện Noel: Rơi Tất Giáng Sinh, Cây Thông, làm Hộp Quà Giáng Sinh, tuần lộc.\n\n" +
            "★ 4 - Sự Kiện Tết Cổ Truyền: Kích hoạt Nồi Bánh Chưng, Lân Con, thu thập Gạo Nếp, Đỗ Xanh, Thịt Heo, Lá Dong gói Bánh Tét, Bánh Chưng.\n\n" +
            "★ 5 - Sự Kiện 8/3: Kích hoạt Capsule Hồng, đổi quà tri ân phái đẹp.\n\n" +
            "★ 6 - Sự Kiện Giỗ Tổ Hùng Vương: Sơn Tinh, Thủy Tinh, dâng lễ Voi 9 Ngà, Gà 9 Cựa, Ngựa 9 Hồng Mao.\n\n" +
            "★ 7 - Sự Kiện Trung Thu: Rước Đèn Ông Sao, đánh Khỉ Đột, bánh Trung Thu Thập Cẩm.\n\n" +
            "★ 8 - Sự Kiện Mùa Hè: Xe Nước Mía, Kem Giải Nhiệt, dưa hấu mát lạnh."
        );

        descCard.add(new JScrollPane(txtDesc), BorderLayout.CENTER);
        root.add(descCard, BorderLayout.CENTER);
        return root;
    }

    // ==========================================
    // TAB CONSOLE LOG & TRUY VẤN SQL TRỰC TIẾP
    // ==========================================
    private JPanel createConsolePanel() {
        JPanel root = new JPanel(new GridLayout(2, 1, 12, 12));
        root.setBackground(COLOR_BG_DARK);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Part 1: Console Log
        JPanel pnlLog = new JPanel(new BorderLayout(8, 8));
        pnlLog.setBackground(COLOR_PANEL_DARK);
        pnlLog.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DARK),
                "Nhật Ký Hoạt Động Server (Console Log Realtime)",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), COLOR_ACCENT
            ),
            new EmptyBorder(8, 10, 8, 10)
        ));

        txtConsoleLog = new JTextArea();
        txtConsoleLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtConsoleLog.setBackground(new Color(15, 23, 42));
        txtConsoleLog.setForeground(new Color(52, 211, 153));
        txtConsoleLog.setCaretColor(Color.WHITE);
        txtConsoleLog.setEditable(false);
        txtConsoleLog.setLineWrap(true);
        txtConsoleLog.setWrapStyleWord(true);
        txtConsoleLog.setText("=== BẮT ĐẦU GHI NHẬT KÝ ADMIN PANEL ===\n");

        JPanel logActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        logActions.setOpaque(false);

        JButton btnClearLog = createModernButton("Xóa Màn Hình Log", new Color(71, 85, 105), Color.WHITE);
        btnClearLog.addActionListener(e -> txtConsoleLog.setText("=== ĐÃ XÓA MÀN HÌNH LOG ===\n"));

        JButton btnCopyLog = createModernButton("Sao Chép Log", new Color(14, 116, 144), Color.WHITE);
        btnCopyLog.addActionListener(e -> {
            txtConsoleLog.selectAll();
            txtConsoleLog.copy();
            JOptionPane.showMessageDialog(this, "Đã sao chép toàn bộ log vào bộ nhớ tạm!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });

        logActions.add(btnCopyLog);
        logActions.add(btnClearLog);

        pnlLog.add(new JScrollPane(txtConsoleLog), BorderLayout.CENTER);
        pnlLog.add(logActions, BorderLayout.SOUTH);
        root.add(pnlLog);

        // Part 2: SQL Direct Query
        JPanel pnlSql = new JPanel(new BorderLayout(8, 8));
        pnlSql.setBackground(COLOR_PANEL_DARK);
        pnlSql.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_DARK),
                "Truy Vấn Cơ Sở Dữ Liệu SQL Trực Tiếp (Execute Direct SQL)",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), COLOR_ACCENT
            ),
            new EmptyBorder(8, 10, 8, 10)
        ));

        JPanel queryBar = new JPanel(new BorderLayout(8, 6));
        queryBar.setOpaque(false);

        txtSqlQuery = new JTextArea("SELECT id, name, power, gender, vnd FROM player ORDER BY power DESC LIMIT 10;", 3, 20);
        txtSqlQuery.setFont(new Font("Consolas", Font.BOLD, 13));
        txtSqlQuery.setBackground(new Color(15, 23, 42));
        txtSqlQuery.setForeground(Color.WHITE);
        txtSqlQuery.setCaretColor(Color.WHITE);
        txtSqlQuery.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(56, 189, 248), 1),
            new EmptyBorder(4, 6, 4, 6)
        ));

        JPanel sqlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sqlButtons.setOpaque(false);

        JButton btnRunSql = createModernButton("Thực Thi SQL (F5)", new Color(34, 197, 94), Color.WHITE);
        btnRunSql.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRunSql.addActionListener(e -> executeDirectSql());

        JButton btnClearSql = createModernButton("Xóa Lệnh", new Color(71, 85, 105), Color.WHITE);
        btnClearSql.addActionListener(e -> txtSqlQuery.setText(""));

        sqlButtons.add(btnClearSql);
        sqlButtons.add(btnRunSql);

        queryBar.add(new JScrollPane(txtSqlQuery), BorderLayout.CENTER);
        queryBar.add(sqlButtons, BorderLayout.SOUTH);
        pnlSql.add(queryBar, BorderLayout.NORTH);

        modelSqlResult = new DefaultTableModel();
        tblSqlResult = new JTable(modelSqlResult);
        styleDarkTable(tblSqlResult);
        tblSqlResult.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblSqlResult.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        pnlSql.add(new JScrollPane(tblSqlResult), BorderLayout.CENTER);

        root.add(pnlSql);
        return root;
    }

    private void executeDirectSql() {
        String query = txtSqlQuery.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập câu lệnh SQL cần thực thi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new Thread(() -> {
            try (Connection con = DBService.gI().getConnection();
                 PreparedStatement ps = con.prepareStatement(query)) {

                boolean isResultSet = ps.execute();
                if (isResultSet) {
                    try (ResultSet rs = ps.getResultSet()) {
                        ResultSetMetaData md = rs.getMetaData();
                        int colCount = md.getColumnCount();
                        Vector<String> colNames = new Vector<>();
                        for (int i = 1; i <= colCount; i++) {
                            colNames.add(md.getColumnLabel(i));
                        }
                        Vector<Vector<Object>> data = new Vector<>();
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            for (int i = 1; i <= colCount; i++) {
                                row.add(rs.getObject(i));
                            }
                            data.add(row);
                        }
                        SwingUtilities.invokeLater(() -> {
                            modelSqlResult.setDataVector(data, colNames);
                            logConsole("SQL SELECT thành công: " + data.size() + " dòng kết quả.");
                        });
                    }
                } else {
                    int affected = ps.getUpdateCount();
                    SwingUtilities.invokeLater(() -> {
                        modelSqlResult.setRowCount(0);
                        modelSqlResult.setColumnCount(0);
                        logConsole("SQL UPDATE/INSERT thành công, số dòng ảnh hưởng: " + affected);
                        JOptionPane.showMessageDialog(this, "Thực thi thành công! " + affected + " dòng dữ liệu bị ảnh hưởng.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    logConsole("Lỗi SQL: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Lỗi thực thi SQL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    public static void logConsole(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String logLine = "[" + timestamp + "] " + message;
        
        RECENT_LOGS.add(logLine);
        if (RECENT_LOGS.size() > 200) {
            RECENT_LOGS.remove(0);
        }

        SwingUtilities.invokeLater(() -> {
            if (instance != null && instance.txtConsoleLog != null) {
                instance.txtConsoleLog.append(logLine + "\n");
                instance.txtConsoleLog.setCaretPosition(instance.txtConsoleLog.getDocument().getLength());
            }
        });
    }

}
