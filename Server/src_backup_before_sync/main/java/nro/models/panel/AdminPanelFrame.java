package nro.models.panel;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.ChristmasEventManager;
import nro.models.boss.Boss_Manager.HalloweenEventManager;
import nro.models.boss.Boss_Manager.HungVuongEventManager;
import nro.models.boss.Boss_Manager.LunarNewYearEventManager;
import nro.models.boss.Boss_Manager.TrungThuEventManager;
import nro.models.consts.ConstNpc;
import nro.models.database.PlayerDAO;
import nro.models.database.ShopDAO;
import nro.models.event.EventManager;
import nro.models.event_list.*;
import nro.models.item.Item;
import nro.models.managers.GiftCodeManager;
import nro.models.map.Map;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.player_system.GiftCode;
import nro.models.player_system.Template.ItemTemplate;
import nro.models.server.Client;
import nro.models.server.Manager;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.shop.Shop;
import nro.models.utils.Logger;
import nro.models.utils.Util;

/**
 * Admin Panel GUI JFrame để quản lý Sự kiện, Người chơi, Cửa hàng và Vật phẩm trực tiếp Realtime
 * không cần khởi động lại Server.
 */
public class AdminPanelFrame extends JFrame {

    private static AdminPanelFrame instance;

    // Header labels
    private JLabel lblOnline;
    private JLabel lblMemory;
    private JLabel lblUptime;
    private long startTime;

    // Tabs
    private JTabbedPane tabbedPane;

    // Event Tab components
    private DefaultTableModel eventTableModel;
    private JTable eventTable;

    // Player Tab components
    private DefaultTableModel playerTableModel;
    private JTable playerTable;
    private JTextField txtPlayerSearch;
    private JLabel lblPlayerDetail;
    private Player selectedPlayer;

    // Shop Tab components
    private DefaultTableModel shopTableModel;
    private JTable shopTable;
    private DefaultTableModel giftcodeTableModel;
    private JTable giftcodeTable;

    // Item Tab components
    private DefaultTableModel itemTableModel;
    private JTable itemTable;
    private TableRowSorter<DefaultTableModel> itemSorter;
    private JTextField txtItemSearch;
    private JTextField txtBuffPlayer;
    private JTextField txtBuffQuantity;
    private JComboBox<String> cbPresetOption;
    private JTextField txtOptionParam;

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
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        setTitle("Dragon Boy - Bảng Quản Trị Server Realtime (Admin Panel)");
        setSize(1150, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        // 1. TOP STATUS PANEL
        add(createTopStatusPanel(), BorderLayout.NORTH);

        // 2. MAIN TABBED PANE
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("🎪 Quản Lý Sự Kiện", createEventPanel());
        tabbedPane.addTab("👥 Quản Lý Người Chơi", createPlayerPanel());
        tabbedPane.addTab("🛒 Cửa Hàng & GiftCode", createShopPanel());
        tabbedPane.addTab("🎁 Quản Lý Vật Phẩm (Buff Item)", createItemPanel());
        tabbedPane.addTab("📢 Thông Báo & Hệ Thống", createSystemPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // 1. TOP STATUS PANEL
    // ==========================================
    private JPanel createTopStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(33, 43, 54));
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));

        // Left info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        infoPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("DRAGON BOY SERVER");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(0, 204, 255));
        infoPanel.add(lblTitle);

        lblOnline = new JLabel("Online: 0");
        lblOnline.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblOnline.setForeground(new Color(102, 255, 102));
        infoPanel.add(lblOnline);

        lblMemory = new JLabel("RAM: 0 MB");
        lblMemory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMemory.setForeground(Color.WHITE);
        infoPanel.add(lblMemory);

        lblUptime = new JLabel("Uptime: 00:00:00");
        lblUptime.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUptime.setForeground(new Color(255, 204, 102));
        infoPanel.add(lblUptime);

        panel.add(infoPanel, BorderLayout.WEST);

        // Right actions
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnGC = new JButton("🧹 Dọn Rác RAM (GC)");
        btnGC.addActionListener(e -> {
            System.gc();
            updateServerStatus();
            JOptionPane.showMessageDialog(this, "Đã thu hồi bộ nhớ JVM thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
        btnPanel.add(btnGC);

        JButton btnSaveAll = new JButton("💾 Lưu Dữ Liệu Tất Cả (Save All)");
        btnSaveAll.addActionListener(e -> saveAllPlayers());
        btnPanel.add(btnSaveAll);

        panel.add(btnPanel, BorderLayout.EAST);
        return panel;
    }

    // ==========================================
    // 2. TAB SỰ KIỆN (EVENTS - REALTIME)
    // ==========================================
    private JPanel createEventPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Description header
        JLabel desc = new JLabel("<html><b>Quản lý Sự Kiện Trực Tiếp:</b> Bật/Tắt sự kiện sẽ tự động triệu hồi hoặc thu hồi Boss, NPC trong map ngay lập tức mà <u>không cần khởi động lại Server</u>.</html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(desc, BorderLayout.NORTH);

        // Event Table
        String[] columns = {"ID", "Tên Sự Kiện", "Trạng Thái", "Mô Tả & Vật Phẩm", "Số Boss Đang Chạy"};
        eventTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        eventTable = new JTable(eventTableModel);
        eventTable.setRowHeight(32);
        eventTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        eventTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Center align
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        eventTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        eventTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        eventTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        eventTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        eventTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        eventTable.getColumnModel().getColumn(3).setPreferredWidth(450);
        eventTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        eventTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(eventTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Control Buttons
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));

        JButton btnToggle = new JButton("⚡ Bật / Tắt Sự Kiện Đang Chọn");
        btnToggle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnToggle.setBackground(new Color(0, 153, 204));
        btnToggle.setForeground(Color.WHITE);
        btnToggle.addActionListener(e -> toggleSelectedEvent());
        southPanel.add(btnToggle);

        JButton btnSaveConfig = new JButton("💾 Lưu Cấu Hình Vào Config.properties");
        btnSaveConfig.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSaveConfig.addActionListener(e -> saveEventsToProperties());
        southPanel.add(btnSaveConfig);

        JButton btnRefresh = new JButton("🔄 Làm Mới");
        btnRefresh.addActionListener(e -> refreshEventTable());
        southPanel.add(btnRefresh);

        panel.add(southPanel, BorderLayout.SOUTH);

        refreshEventTable();
        return panel;
    }

    private void refreshEventTable() {
        eventTableModel.setRowCount(0);

        int countNoel = ChristmasEventManager.gI().getBosses().size();
        int countTet = LunarNewYearEventManager.gI().getBosses().size();
        int countHalloween = HalloweenEventManager.gI().getBosses().size();
        int countHungVuong = HungVuongEventManager.gI().getBosses().size();
        int countTrungThu = TrungThuEventManager.gI().getBosses().size();

        eventTableModel.addRow(new Object[]{1, "Giáng Sinh (Noel)", EventManager.CHRISTMAS ? "🟢 ĐANG BẬT" : "🔴 ĐANG TẮT", "Boss Ông Già Noel phát hộp quà 648 (Mũ Noel, Cây Thông, Xe Trượt...)", countNoel});
        eventTableModel.addRow(new Object[]{2, "Tết Nguyên Đán", EventManager.LUNNAR_NEW_YEAR ? "🟢 ĐANG BẬT" : "🔴 ĐANG TẮT", "Boss Lân Con, dắt Lân về Quy Lão Kame nhận quà; Nồi Bánh Chưng", countTet});
        eventTableModel.addRow(new Object[]{3, "Halloween", EventManager.HALLOWEEN ? "🟢 ĐANG BẬT" : "🔴 ĐANG TẮT", "Boss Bí Ma, Ma Trơi, Dơi Quỷ (Rơi Kẹo Ma/Bí Ngô 585)", countHalloween});
        eventTableModel.addRow(new Object[]{4, "Giỗ Tổ Hùng Vương", EventManager.HUNG_VUONG ? "🟢 ĐANG BẬT" : "🔴 ĐANG TẮT", "Sơn Tinh & Thủy Tinh, NPC Vua Hùng (Trồng dưa hấu, sính lễ), Nồi Bánh", countHungVuong});
        eventTableModel.addRow(new Object[]{5, "Tết Trung Thu", EventManager.TRUNG_THU ? "🟢 ĐANG BẬT" : "🔴 ĐANG TẮT", "Khỉ Đột (Đuôi khỉ 1045), Nguyệt Thần & Nhật Thần (Vật phẩm VIP 2123, 2124)", countTrungThu});
        eventTableModel.addRow(new Object[]{6, "Sự Kiện Hè (Xe Nước Mía)", EventManager.SUMMER ? "🟢 ĐANG BẬT" : "🔴 ĐANG TẮT", "NPC Xe Nước Mía (84) tại các làng đổi Ly Nước Mía đua TOP sukien1", "0"});
        eventTableModel.addRow(new Object[]{7, "Quốc Tế Phụ Nữ (8/3)", EventManager.INTERNATIONAL_WOMANS_DAY ? "🟢 ĐANG BẬT" : "🔴 ĐANG TẮT", "Tăng buff HP/MP/Dame từ EventDAO", "0"});
        eventTableModel.addRow(new Object[]{8, "Đua Top Nạp (TopUp)", EventManager.TOP_UP ? "🟢 ĐANG BẬT" : "🔴 ĐANG TẮT", "Tích lũy nạp thẻ và xếp hạng bảng top server", "0"});
    }

    private void toggleSelectedEvent() {
        int row = eventTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một sự kiện từ danh sách để Bật/Tắt!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) eventTableModel.getValueAt(row, 0);
        switch (id) {
            case 1 -> { // Noel
                EventManager.CHRISTMAS = !EventManager.CHRISTMAS;
                if (EventManager.CHRISTMAS) {
                    new Christmas().init();
                    JOptionPane.showMessageDialog(this, "Đã BẬT Sự Kiện Giáng Sinh! 30 Ông Già Noel đã được triệu hồi vào các map.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    for (Boss b : new ArrayList<>(ChristmasEventManager.gI().getBosses())) {
                        b.leaveMap();
                    }
                    ChristmasEventManager.gI().getBosses().clear();
                    JOptionPane.showMessageDialog(this, "Đã TẮT Sự Kiện Giáng Sinh! Toàn bộ Ông Già Noel đã được dọn dẹp khỏi map.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            case 2 -> { // Tet
                EventManager.LUNNAR_NEW_YEAR = !EventManager.LUNNAR_NEW_YEAR;
                if (EventManager.LUNNAR_NEW_YEAR) {
                    new LunarNewYear().init();
                    JOptionPane.showMessageDialog(this, "Đã BẬT Sự Kiện Tết! 10 Boss Lân Con và Nồi Bánh đã xuất hiện.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    for (Boss b : new ArrayList<>(LunarNewYearEventManager.gI().getBosses())) {
                        b.leaveMap();
                    }
                    LunarNewYearEventManager.gI().getBosses().clear();
                    removeNpcFromMap(5, ConstNpc.NOI_BANH);
                    removeNpcFromMap(0, ConstNpc.NOI_BANH);
                    removeNpcFromMap(7, ConstNpc.NOI_BANH);
                    removeNpcFromMap(14, ConstNpc.NOI_BANH);
                    JOptionPane.showMessageDialog(this, "Đã TẮT Sự Kiện Tết! Dọn dẹp Boss Lân Con và Nồi Bánh thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            case 3 -> { // Halloween
                EventManager.HALLOWEEN = !EventManager.HALLOWEEN;
                if (EventManager.HALLOWEEN) {
                    new Halloween().init();
                    JOptionPane.showMessageDialog(this, "Đã BẬT Sự Kiện Halloween! Boss Bí Ma, Ma Trơi, Dơi Quỷ đã vào map.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    for (Boss b : new ArrayList<>(HalloweenEventManager.gI().getBosses())) {
                        b.leaveMap();
                    }
                    HalloweenEventManager.gI().getBosses().clear();
                    JOptionPane.showMessageDialog(this, "Đã TẮT Sự Kiện Halloween! Dọn dẹp toàn bộ quái Halloween.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            case 4 -> { // Hung Vuong
                EventManager.HUNG_VUONG = !EventManager.HUNG_VUONG;
                if (EventManager.HUNG_VUONG) {
                    new HungVuong().init();
                    JOptionPane.showMessageDialog(this, "Đã BẬT Sự Kiện Hùng Vương! Thủy Tinh, Sơn Tinh, Vua Hùng & Nồi Bánh đã xuất hiện.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    for (Boss b : new ArrayList<>(HungVuongEventManager.gI().getBosses())) {
                        b.leaveMap();
                    }
                    HungVuongEventManager.gI().getBosses().clear();
                    removeNpcFromMap(5, ConstNpc.HUNG_VUONG);
                    removeNpcFromMap(0, ConstNpc.HUNG_VUONG);
                    removeNpcFromMap(7, ConstNpc.HUNG_VUONG);
                    removeNpcFromMap(14, ConstNpc.HUNG_VUONG);
                    removeNpcFromMap(5, ConstNpc.NOI_BANH);
                    removeNpcFromMap(0, ConstNpc.NOI_BANH);
                    removeNpcFromMap(7, ConstNpc.NOI_BANH);
                    removeNpcFromMap(14, ConstNpc.NOI_BANH);
                    JOptionPane.showMessageDialog(this, "Đã TẮT Sự Kiện Hùng Vương! Đã dọn dẹp boss và NPC Vua Hùng.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            case 5 -> { // Trung Thu
                EventManager.TRUNG_THU = !EventManager.TRUNG_THU;
                if (EventManager.TRUNG_THU) {
                    new TrungThu().init();
                    JOptionPane.showMessageDialog(this, "Đã BẬT Sự Kiện Trung Thu! Boss Khỉ Đột, Nguyệt Thần & Nhật Thần đã xuất hiện.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    for (Boss b : new ArrayList<>(TrungThuEventManager.gI().getBosses())) {
                        b.leaveMap();
                    }
                    TrungThuEventManager.gI().getBosses().clear();
                    JOptionPane.showMessageDialog(this, "Đã TẮT Sự Kiện Trung Thu! Dọn dẹp boss thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            case 6 -> { // Summer
                EventManager.SUMMER = !EventManager.SUMMER;
                if (EventManager.SUMMER) {
                    new Summer().init();
                    JOptionPane.showMessageDialog(this, "Đã BẬT Sự Kiện Hè! NPC Xe Nước Mía đã có mặt tại các làng và Đảo Kame.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    removeNpcFromMap(5, ConstNpc.XE_NUOC_MIA);
                    removeNpcFromMap(0, ConstNpc.XE_NUOC_MIA);
                    removeNpcFromMap(7, ConstNpc.XE_NUOC_MIA);
                    removeNpcFromMap(14, ConstNpc.XE_NUOC_MIA);
                    JOptionPane.showMessageDialog(this, "Đã TẮT Sự Kiện Hè! Xe Nước Mía đã được dọn dẹp.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            case 7 -> { // 8/3
                EventManager.INTERNATIONAL_WOMANS_DAY = !EventManager.INTERNATIONAL_WOMANS_DAY;
                if (EventManager.INTERNATIONAL_WOMANS_DAY) {
                    new InternationalWomensDay().init();
                }
                JOptionPane.showMessageDialog(this, "Trạng thái Sự Kiện 8/3: " + (EventManager.INTERNATIONAL_WOMANS_DAY ? "BẬT" : "TẮT"), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
            case 8 -> { // TopUp
                EventManager.TOP_UP = !EventManager.TOP_UP;
                JOptionPane.showMessageDialog(this, "Trạng thái Đua Top Nạp: " + (EventManager.TOP_UP ? "BẬT" : "TẮT"), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        refreshEventTable();
    }

    private void removeNpcFromMap(int mapId, int npcTempId) {
        try {
            Map map = MapService.gI().getMapById(mapId);
            if (map != null && map.npcs != null) {
                map.npcs.removeIf(n -> n != null && n.tempId == npcTempId);
                if (map.zones != null) {
                    for (nro.models.map.Zone z : map.zones) {
                        if (z != null) {
                            for (nro.models.player.Player pl : z.getPlayers()) {
                                if (pl != null && pl.getSession() != null) {
                                    z.mapInfo(pl);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void saveEventsToProperties() {
        try {
            File configFile = new File("Config.properties");
            if (!configFile.exists()) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy file Config.properties!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
            List<String> newLines = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith("event.noel=")) line = "event.noel=" + EventManager.CHRISTMAS;
                else if (line.startsWith("event.tet=")) line = "event.tet=" + EventManager.LUNNAR_NEW_YEAR;
                else if (line.startsWith("event.halloween=")) line = "event.halloween=" + EventManager.HALLOWEEN;
                else if (line.startsWith("event.hungvuong=")) line = "event.hungvuong=" + EventManager.HUNG_VUONG;
                else if (line.startsWith("event.trungthu=")) line = "event.trungthu=" + EventManager.TRUNG_THU;
                else if (line.startsWith("event.summer=")) line = "event.summer=" + EventManager.SUMMER;
                else if (line.startsWith("event.womensday=")) line = "event.womensday=" + EventManager.INTERNATIONAL_WOMANS_DAY;
                else if (line.startsWith("event.topup=")) line = "event.topup=" + EventManager.TOP_UP;
                newLines.add(line);
            }
            Files.write(configFile.toPath(), newLines, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Đã lưu trạng thái các sự kiện vào Config.properties thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // 3. TAB NGƯỜI CHƠI (PLAYERS - REALTIME)
    // ==========================================
    private JPanel createPlayerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Search Bar
        JPanel topSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topSearch.add(new JLabel("Tìm kiếm người chơi (Tên hoặc ID):"));
        txtPlayerSearch = new JTextField(20);
        topSearch.add(txtPlayerSearch);

        JButton btnSearch = new JButton("🔍 Tìm Kiếm");
        btnSearch.addActionListener(e -> filterPlayers());
        topSearch.add(btnSearch);

        JButton btnRefreshPlayers = new JButton("🔄 Làm Mới Danh Sách");
        btnRefreshPlayers.addActionListener(e -> refreshPlayerList());
        topSearch.add(btnRefreshPlayers);

        panel.add(topSearch, BorderLayout.NORTH);

        // Center: SplitPane (Table left, Details & Actions right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.65);

        // Player Table
        String[] columns = {"ID", "Tên Nhân Vật", "Giới Tính", "Sức Mạnh", "Vàng", "Ngọc Xanh", "Hồng Ngọc", "Bản Đồ"};
        playerTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        playerTable = new JTable(playerTableModel);
        playerTable.setRowHeight(26);
        playerTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        playerTable.getSelectionModel().addListSelectionListener(e -> onPlayerSelected());

        JScrollPane scrollPane = new JScrollPane(playerTable);
        splitPane.setLeftComponent(scrollPane);

        // Right Detail & Action Panel
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(new TitledBorder("Thao Tác Trực Tiếp Với Nhân Vật"));

        lblPlayerDetail = new JLabel("<html><i>Hãy chọn 1 người chơi từ danh sách để thao tác...</i></html>");
        lblPlayerDetail.setBorder(new EmptyBorder(10, 10, 10, 10));
        lblPlayerDetail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rightPanel.add(lblPlayerDetail, BorderLayout.NORTH);

        // Action Buttons Grid
        JPanel actionGrid = new JPanel(new GridLayout(6, 2, 8, 8));
        actionGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton btnAddGold = new JButton("💰 Cộng Vàng (Gold)");
        btnAddGold.addActionListener(e -> handleAddMoney(1));
        actionGrid.add(btnAddGold);

        JButton btnAddGem = new JButton("💎 Cộng Ngọc (Gem)");
        btnAddGem.addActionListener(e -> handleAddMoney(2));
        actionGrid.add(btnAddGem);

        JButton btnAddRuby = new JButton("♦️ Cộng Hồng Ngọc (Ruby)");
        btnAddRuby.addActionListener(e -> handleAddMoney(3));
        actionGrid.add(btnAddRuby);

        JButton btnHeal = new JButton("❤️ Hồi Đầy HP & KI");
        btnHeal.addActionListener(e -> handleHealPlayer());
        actionGrid.add(btnHeal);

        JButton btnTeleport = new JButton("🚀 Dịch Chuyển (Teleport)");
        btnTeleport.addActionListener(e -> handleTeleport());
        actionGrid.add(btnTeleport);

        JButton btnMsg = new JButton("💬 Gửi Thông Báo Riêng");
        btnMsg.addActionListener(e -> handleSendPlayerMessage());
        actionGrid.add(btnMsg);

        JButton btnKick = new JButton("🚪 Kick Mất Kết Nối");
        btnKick.setForeground(new Color(204, 0, 0));
        btnKick.addActionListener(e -> handleKickPlayer());
        actionGrid.add(btnKick);

        JButton btnSavePl = new JButton("💾 Lưu Người Chơi Này");
        btnSavePl.addActionListener(e -> handleSaveSelectedPlayer());
        actionGrid.add(btnSavePl);

        rightPanel.add(actionGrid, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        panel.add(splitPane, BorderLayout.CENTER);
        refreshPlayerList();
        return panel;
    }

    private void refreshPlayerList() {
        playerTableModel.setRowCount(0);
        selectedPlayer = null;
        if (lblPlayerDetail != null) {
            lblPlayerDetail.setText("<html><i>Hãy chọn 1 người chơi từ danh sách để thao tác...</i></html>");
        }

        List<Player> players = Client.gI().getPlayers();
        for (Player pl : players) {
            if (pl != null && pl.nPoint != null && pl.inventory != null) {
                String genderStr = switch (pl.gender) {
                    case 0 -> "Trái Đất";
                    case 1 -> "Namếc";
                    case 2 -> "Xayda";
                    default -> "Khác";
                };
                String mapStr = pl.zone != null && pl.zone.map != null ? pl.zone.map.mapName + " (" + pl.zone.zoneId + ")" : "Không rõ";

                playerTableModel.addRow(new Object[]{
                    pl.id,
                    pl.name,
                    genderStr,
                    Util.powerToString(pl.nPoint.power),
                    Util.numberToMoney(pl.inventory.gold),
                    pl.inventory.gem,
                    pl.inventory.ruby,
                    mapStr
                });
            }
        }
    }

    private void filterPlayers() {
        String keyword = txtPlayerSearch.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            refreshPlayerList();
            return;
        }
        playerTableModel.setRowCount(0);
        for (Player pl : Client.gI().getPlayers()) {
            if (pl != null && (String.valueOf(pl.id).contains(keyword) || pl.name.toLowerCase().contains(keyword))) {
                String genderStr = switch (pl.gender) {
                    case 0 -> "Trái Đất";
                    case 1 -> "Namếc";
                    case 2 -> "Xayda";
                    default -> "Khác";
                };
                String mapStr = pl.zone != null && pl.zone.map != null ? pl.zone.map.mapName + " (" + pl.zone.zoneId + ")" : "Không rõ";

                playerTableModel.addRow(new Object[]{
                    pl.id,
                    pl.name,
                    genderStr,
                    Util.powerToString(pl.nPoint.power),
                    Util.numberToMoney(pl.inventory.gold),
                    pl.inventory.gem,
                    pl.inventory.ruby,
                    mapStr
                });
            }
        }
    }

    private void onPlayerSelected() {
        int row = playerTable.getSelectedRow();
        if (row < 0) {
            selectedPlayer = null;
            return;
        }
        long id = (long) playerTableModel.getValueAt(row, 0);
        selectedPlayer = Client.gI().getPlayer(id);

        if (selectedPlayer != null) {
            lblPlayerDetail.setText(String.format("<html><b>Người chơi:</b> <font color='blue'>%s</font> (ID: %d)<br>"
                    + "<b>HP:</b> %s / %s | <b>KI:</b> %s / %s<br>"
                    + "<b>Sức đánh:</b> %s | <b>Giáp:</b> %d | <b>Chí mạng:</b> %d%%<br>"
                    + "<b>Vàng:</b> %s | <b>Ngọc:</b> %s | <b>Hồng ngọc:</b> %s</html>",
                    selectedPlayer.name, selectedPlayer.id,
                    Util.numberToMoney(selectedPlayer.nPoint.hp), Util.numberToMoney(selectedPlayer.nPoint.hpMax),
                    Util.numberToMoney(selectedPlayer.nPoint.mp), Util.numberToMoney(selectedPlayer.nPoint.mpMax),
                    Util.numberToMoney(selectedPlayer.nPoint.dame), selectedPlayer.nPoint.def, selectedPlayer.nPoint.crit,
                    Util.numberToMoney(selectedPlayer.inventory.gold), selectedPlayer.inventory.gem, selectedPlayer.inventory.ruby));

            if (txtBuffPlayer != null) {
                txtBuffPlayer.setText(selectedPlayer.name);
            }
        }
    }

    private void handleAddMoney(int type) {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi từ danh sách!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String typeName = type == 1 ? "Vàng" : (type == 2 ? "Ngọc xanh" : "Hồng ngọc");
        String input = JOptionPane.showInputDialog(this, "Nhập số lượng " + typeName + " cần cộng/trừ (ví dụ: 1000000 hoặc -50000):", "Cộng Tiền Realtime", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            long val = Long.parseLong(input.trim().replaceAll("[,._]", ""));
            if (type == 1) {
                selectedPlayer.inventory.gold = Math.max(0, Math.min(2_000_000_000L, selectedPlayer.inventory.gold + val));
            } else if (type == 2) {
                selectedPlayer.inventory.gem = (int) Math.max(0, Math.min(2_000_000_000L, selectedPlayer.inventory.gem + val));
            } else {
                selectedPlayer.inventory.ruby = (int) Math.max(0, Math.min(2_000_000_000L, selectedPlayer.inventory.ruby + val));
            }
            Service.gI().sendMoney(selectedPlayer);
            PlayerService.gI().sendInfoHpMpMoney(selectedPlayer);
            Service.gI().sendThongBao(selectedPlayer, "Bạn vừa được Admin điều chỉnh " + typeName + ": " + (val > 0 ? "+" : "") + val);
            onPlayerSelected();
            refreshPlayerList();
            JOptionPane.showMessageDialog(this, "Cộng " + typeName + " thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Giá trị số không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleHealPlayer() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi từ danh sách!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        selectedPlayer.nPoint.setHp((int) selectedPlayer.nPoint.hpMax);
        selectedPlayer.nPoint.setMp((int) selectedPlayer.nPoint.mpMax);
        PlayerService.gI().sendInfoHpMp(selectedPlayer);
        Service.gI().sendThongBao(selectedPlayer, "Bạn đã được Admin hồi sinh / hồi phục toàn bộ HP & KI!");
        onPlayerSelected();
        JOptionPane.showMessageDialog(this, "Đã hồi phục đầy HP và KI cho người chơi!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleTeleport() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi từ danh sách!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Nhập Map ID cần dịch chuyển tới (Ví dụ: 0 - Làng Aru, 5 - Đảo Kame, 21 - Nhà Gôhan, 84 - Siêu thị...):", "Dịch Chuyển Người Chơi", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            int mapId = Integer.parseInt(input.trim());
            Map targetMap = MapService.gI().getMapById(mapId);
            if (targetMap == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy Map ID " + mapId, "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ChangeMapService.gI().changeMapInYard(selectedPlayer, mapId, -1, 300);
            Service.gI().sendThongBao(selectedPlayer, "Admin đã dịch chuyển bạn tới " + targetMap.mapName + "!");
            onPlayerSelected();
            refreshPlayerList();
            JOptionPane.showMessageDialog(this, "Đã dịch chuyển người chơi tới " + targetMap.mapName, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Map ID không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSendPlayerMessage() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi từ danh sách!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String msg = JOptionPane.showInputDialog(this, "Nhập nội dung thông báo gửi riêng cho " + selectedPlayer.name + ":", "Gửi Thông Báo Riêng", JOptionPane.QUESTION_MESSAGE);
        if (msg != null && !msg.trim().isEmpty()) {
            Service.gI().sendThongBao(selectedPlayer, msg.trim());
            JOptionPane.showMessageDialog(this, "Đã gửi thông báo thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleKickPlayer() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi từ danh sách!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn ngắt kết nối (kick) người chơi " + selectedPlayer.name + " không?", "Xác Nhận Kick", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (selectedPlayer.getSession() != null) {
                selectedPlayer.getSession().disconnect();
            }
            Client.gI().kickSession(selectedPlayer.getSession());
            refreshPlayerList();
            JOptionPane.showMessageDialog(this, "Đã kick người chơi thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleSaveSelectedPlayer() {
        if (selectedPlayer == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi từ danh sách!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PlayerDAO.updatePlayer(selectedPlayer);
        JOptionPane.showMessageDialog(this, "Đã lưu toàn bộ dữ liệu của " + selectedPlayer.name + " vào Database thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveAllPlayers() {
        List<Player> players = Client.gI().getPlayers();
        int count = 0;
        for (Player pl : players) {
            if (pl != null) {
                PlayerDAO.updatePlayer(pl);
                count++;
            }
        }
        JOptionPane.showMessageDialog(this, "Đã lưu thành công dữ liệu của " + count + " người chơi online vào Database!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==========================================
    // 4. TAB CỬA HÀNG & GIFTCODE (SHOPS & CODES)
    // ==========================================
    private JPanel createShopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Control Buttons
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));

        JButton btnReloadShop = new JButton("🔄 NẠP LẠI CỬA HÀNG (RELOAD SHOPS)");
        btnReloadShop.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnReloadShop.setBackground(new Color(40, 167, 69));
        btnReloadShop.setForeground(Color.WHITE);
        btnReloadShop.addActionListener(e -> {
            int count = ShopDAO.reloadShops();
            if (count >= 0) {
                refreshShopTables();
                JOptionPane.showMessageDialog(this, "Nạp lại thành công " + count + " cửa hàng từ Database!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Có lỗi xảy ra khi nạp lại cửa hàng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        topButtons.add(btnReloadShop);

        JButton btnReloadCode = new JButton("🎁 NẠP LẠI GIFTCODE (RELOAD CODES)");
        btnReloadCode.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnReloadCode.setBackground(new Color(255, 153, 0));
        btnReloadCode.setForeground(Color.WHITE);
        btnReloadCode.addActionListener(e -> {
            try {
                int count = GiftCodeManager.gI().reloadGiftCodes();
                refreshShopTables();
                JOptionPane.showMessageDialog(this, "Nạp lại thành công " + count + " GiftCode từ Database!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Có lỗi xảy ra khi nạp lại GiftCode: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        topButtons.add(btnReloadCode);

        panel.add(topButtons, BorderLayout.NORTH);

        // Center split: Shops Table & Giftcodes Table
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        // Shop Table
        JPanel pnlShop = new JPanel(new BorderLayout());
        pnlShop.setBorder(new TitledBorder("Danh Sách Cửa Hàng Trong Game"));
        String[] shopCols = {"ID Shop", "NPC Bán", "Tag Name", "Loại Shop", "Số Tab Bán"};
        shopTableModel = new DefaultTableModel(shopCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        shopTable = new JTable(shopTableModel);
        shopTable.setRowHeight(24);
        pnlShop.add(new JScrollPane(shopTable), BorderLayout.CENTER);
        splitPane.setTopComponent(pnlShop);

        // Giftcode Table
        JPanel pnlCode = new JPanel(new BorderLayout());
        pnlCode.setBorder(new TitledBorder("Danh Sách GiftCode Hiện Có"));
        String[] codeCols = {"Mã Code", "Số Lượt Còn Lại", "Ngày Tạo", "Hạn Sử Dụng"};
        giftcodeTableModel = new DefaultTableModel(codeCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        giftcodeTable = new JTable(giftcodeTableModel);
        giftcodeTable.setRowHeight(24);
        pnlCode.add(new JScrollPane(giftcodeTable), BorderLayout.CENTER);
        splitPane.setBottomComponent(pnlCode);

        panel.add(splitPane, BorderLayout.CENTER);
        refreshShopTables();
        return panel;
    }

    private void refreshShopTables() {
        // Shops
        shopTableModel.setRowCount(0);
        if (Manager.SHOPS != null) {
            for (Shop shop : Manager.SHOPS) {
                if (shop != null) {
                    shopTableModel.addRow(new Object[]{
                        shop.id,
                        "NPC ID: " + shop.npcId,
                        shop.tagName != null ? shop.tagName : "",
                        shop.typeShop,
                        shop.tabShops != null ? shop.tabShops.size() : 0
                    });
                }
            }
        }

        // Codes
        giftcodeTableModel.setRowCount(0);
        for (GiftCode code : GiftCodeManager.gI().listGiftCode) {
            if (code != null) {
                giftcodeTableModel.addRow(new Object[]{
                    code.code,
                    code.countLeft >= 900000000 ? "Không giới hạn" : code.countLeft,
                    code.datecreate != null ? code.datecreate.toString() : "N/A",
                    code.dateexpired != null ? code.dateexpired.toString() : "Vĩnh viễn"
                });
            }
        }
    }

    // ==========================================
    // 5. TAB QUẢN LÝ VẬT PHẨM (BUFF ITEM)
    // ==========================================
    private JPanel createItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Filter Bar
        JPanel topFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topFilter.add(new JLabel("Tìm kiếm vật phẩm (Tên hoặc ID):"));
        txtItemSearch = new JTextField(25);
        txtItemSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterItems();
            }
        });
        topFilter.add(txtItemSearch);
        panel.add(topFilter, BorderLayout.NORTH);

        // Center Split: Item Table left, Buff Form right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        String[] itemCols = {"ID Template", "Tên Vật Phẩm", "Loại", "Mô Tả"};
        itemTableModel = new DefaultTableModel(itemCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        itemTable = new JTable(itemTableModel);
        itemTable.setRowHeight(24);
        itemSorter = new TableRowSorter<>(itemTableModel);
        itemTable.setRowSorter(itemSorter);

        itemTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(300);

        JScrollPane scrollPane = new JScrollPane(itemTable);
        splitPane.setLeftComponent(scrollPane);

        // Right Buff Form
        JPanel rightForm = new JPanel();
        rightForm.setLayout(new BoxLayout(rightForm, BoxLayout.Y_AXIS));
        rightForm.setBorder(new TitledBorder("Thông Tin Buff Vật Phẩm Vào Hành Trang"));

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1.add(new JLabel("Người nhận (Tên nhân vật):"));
        txtBuffPlayer = new JTextField(15);
        p1.add(txtBuffPlayer);
        rightForm.add(p1);

        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p2.add(new JLabel("Số lượng buff:"));
        txtBuffQuantity = new JTextField("1", 10);
        p2.add(txtBuffQuantity);
        rightForm.add(p2);

        JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p3.add(new JLabel("Chỉ số bổ sung (Option Preset):"));
        String[] presets = {
            "Mặc định theo template",
            "50 - Tăng Sức Đánh (%)",
            "77 - Tăng HP (%)",
            "103 - Tăng KI (%)",
            "14 - Tăng Chí Mạng (%)",
            "93 - Hạn Sử Dụng (Ngày)",
            "30 - Không thể giao dịch",
            "73 - Tự động hồi phục HP"
        };
        cbPresetOption = new JComboBox<>(presets);
        p3.add(cbPresetOption);
        rightForm.add(p3);

        JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p4.add(new JLabel("Giá trị chỉ số (Param):"));
        txtOptionParam = new JTextField("20", 10);
        p4.add(txtOptionParam);
        rightForm.add(p4);

        rightForm.add(Box.createVerticalStrut(15));

        JPanel p5 = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnBuff = new JButton("🎁 BUFF VÀO HÀNH TRANG NGAY");
        btnBuff.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBuff.setBackground(new Color(0, 123, 255));
        btnBuff.setForeground(Color.WHITE);
        btnBuff.addActionListener(e -> handleBuffItem());
        p5.add(btnBuff);

        JButton btnDeleteItem = new JButton("🗑️ Xóa Item Này Khỏi Túi");
        btnDeleteItem.setForeground(new Color(204, 0, 0));
        btnDeleteItem.addActionListener(e -> handleDeleteItemFromBag());
        p5.add(btnDeleteItem);

        rightForm.add(p5);
        rightForm.add(Box.createVerticalGlue());

        splitPane.setRightComponent(rightForm);
        panel.add(splitPane, BorderLayout.CENTER);

        loadItemTemplates();
        return panel;
    }

    private void loadItemTemplates() {
        itemTableModel.setRowCount(0);
        for (ItemTemplate temp : Manager.ITEM_TEMPLATES) {
            if (temp != null) {
                itemTableModel.addRow(new Object[]{
                    temp.id,
                    temp.name,
                    temp.type,
                    temp.description != null ? temp.description : ""
                });
            }
        }
    }

    private void filterItems() {
        String text = txtItemSearch.getText().trim();
        if (text.isEmpty()) {
            itemSorter.setRowFilter(null);
        } else {
            itemSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1));
        }
    }

    private void handleBuffItem() {
        String plName = txtBuffPlayer.getText().trim();
        if (plName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên người chơi nhận item!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
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
        int tempId = (short) itemTableModel.getValueAt(modelRow, 0);

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

        Item item = ItemService.gI().createNewItem((short) tempId, quantity);

        int presetIdx = cbPresetOption.getSelectedIndex();
        if (presetIdx > 0) {
            int param = 0;
            try {
                param = Integer.parseInt(txtOptionParam.getText().trim());
            } catch (Exception ignored) {
            }
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
                item.itemOptions.add(new Item.ItemOption(optId, param));
            }
        }

        InventoryService.gI().addItemBag(target, item);
        InventoryService.gI().sendItemBags(target);
        Service.gI().sendThongBao(target, "Bạn vừa nhận được " + item.template.name + " x" + quantity + " từ Admin!");

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
        int tempId = (short) itemTableModel.getValueAt(modelRow, 0);

        Item inBag = InventoryService.gI().findItemBag(target, tempId);
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
        Service.gI().sendThongBao(target, "Admin đã thu hồi " + inBag.template.name + " x" + quantity + " khỏi túi đồ của bạn.");

        JOptionPane.showMessageDialog(this, "Đã xóa vật phẩm khỏi túi thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==========================================
    // 6. TAB THÔNG BÁO & HỆ THỐNG (SYSTEM)
    // ==========================================
    private JPanel createSystemPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Broadcast Section
        JPanel pnlBroadcast = new JPanel(new BorderLayout(10, 10));
        pnlBroadcast.setBorder(new TitledBorder("📢 Gửi Thông Báo Toàn Server (Broadcast)"));

        JTextArea txtBroadcast = new JTextArea("Chào mừng các cư dân đến với Dragon Boy Server!");
        txtBroadcast.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBroadcast.setLineWrap(true);
        pnlBroadcast.add(new JScrollPane(txtBroadcast), BorderLayout.CENTER);

        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSendYellow = new JButton("🔔 Gửi Thông Báo Chữ Vàng (Popup Giữa Màn Hình)");
        btnSendYellow.setBackground(new Color(255, 193, 7));
        btnSendYellow.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSendYellow.addActionListener(e -> {
            String text = txtBroadcast.getText().trim();
            if (!text.isEmpty()) {
                Service.gI().sendThongBaoAllPlayer(text);
                JOptionPane.showMessageDialog(this, "Đã gửi thông báo toàn server thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        pnlBtn.add(btnSendYellow);

        JButton btnSendGlobal = new JButton("💬 Gửi Kênh Thế Giới");
        btnSendGlobal.addActionListener(e -> {
            String text = txtBroadcast.getText().trim();
            if (!text.isEmpty()) {
                Service.gI().sendThongBaoAllPlayer("[ADMIN]: " + text);
                JOptionPane.showMessageDialog(this, "Đã gửi thông báo đến toàn máy chủ!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        pnlBtn.add(btnSendGlobal);
        pnlBroadcast.add(pnlBtn, BorderLayout.SOUTH);

        panel.add(pnlBroadcast);

        // Maintenance Section
        JPanel pnlMaintain = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        pnlMaintain.setBorder(new TitledBorder("⚙️ Bảo Trì Máy Chủ"));

        pnlMaintain.add(new JLabel("Hẹn giờ bảo trì server sau (giây):"));
        JTextField txtMaintainSeconds = new JTextField("60", 6);
        pnlMaintain.add(txtMaintainSeconds);

        JButton btnStartMaintain = new JButton("⚠️ Bắt Đầu Đếm Ngược Bảo Trì");
        btnStartMaintain.setBackground(new Color(220, 53, 69));
        btnStartMaintain.setForeground(Color.WHITE);
        btnStartMaintain.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnStartMaintain.addActionListener(e -> {
            try {
                int sec = Integer.parseInt(txtMaintainSeconds.getText().trim());
                int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận đếm ngược bảo trì server sau " + sec + " giây?", "Cảnh báo bảo trì", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    nro.models.server.Maintenance.gI().startSeconds(sec);
                    JOptionPane.showMessageDialog(this, "Đã phát lệnh bảo trì sau " + sec + " giây!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số giây không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        pnlMaintain.add(btnStartMaintain);

        panel.add(pnlMaintain);
        return panel;
    }

    // ==========================================
    // 7. AUTO REFRESH TIMER
    // ==========================================
    private void startAutoRefreshTimer() {
        Timer timer = new Timer(3000, e -> updateServerStatus());
        timer.start();
    }

    private void updateServerStatus() {
        // Online count
        int online = Client.gI().getPlayers().size();
        lblOnline.setText("Online: " + online);

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
    }
}
