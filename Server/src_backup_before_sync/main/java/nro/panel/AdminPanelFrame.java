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
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import nro.jdbc.DBService;
import nro.jdbc.daos.ShopDAO;
import nro.models.boss.Boss;
import nro.models.boss.BossFactory;
import nro.models.boss.BossManager;
import nro.models.item.Item;
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

    // Shop & Giftcode Tab
    private DefaultTableModel shopItemTableModel;
    private JTable shopItemTable;
    private JComboBox<String> cbShopNpcSelect;
    private JComboBox<String> cbShopTabSelect;
    private DefaultTableModel giftcodeTableModel;
    private JTable giftcodeTable;
    private JLabel lblCurrentEventStatus;

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

        setTitle("NRO NEW 01 - BẢNG QUẢN TRỊ SERVER REALTIME (ADMIN PANEL)");
        setSize(1220, 760);
        setMinimumSize(new Dimension(1050, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        // 1. TOP STATUS PANEL
        add(createTopStatusPanel(), BorderLayout.NORTH);

        // 2. MAIN TABBED PANE
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("👥 Quản Lý Người Chơi", createPlayerPanel());
        tabbedPane.addTab("🎁 Quản Lý Vật Phẩm & Buff", createItemPanel());
        tabbedPane.addTab("👾 Quản Lý Boss", createBossPanel());
        tabbedPane.addTab("🔑 Quản Lý Tài Khoản (DB)", createAccountPanel());
        tabbedPane.addTab("🎟️ Cửa Hàng & GiftCode", createShopPanel());
        tabbedPane.addTab("📢 Thông Báo & Bảo Trì", createSystemPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // 1. TOP STATUS PANEL
    // ==========================================
    private JPanel createTopStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(24, 32, 47));
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Left info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        infoPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("🐲 NRO NEW 01 SERVER");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(51, 204, 255));
        infoPanel.add(lblTitle);

        lblOnline = new JLabel("🟢 Online: 0");
        lblOnline.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblOnline.setForeground(new Color(110, 240, 140));
        infoPanel.add(lblOnline);

        lblSessions = new JLabel("🌐 Sessions: 0");
        lblSessions.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSessions.setForeground(new Color(220, 230, 242));
        infoPanel.add(lblSessions);

        lblBossCount = new JLabel("👾 Boss: 0");
        lblBossCount.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblBossCount.setForeground(new Color(255, 179, 102));
        infoPanel.add(lblBossCount);

        lblMemory = new JLabel("💾 RAM: 0 MB");
        lblMemory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMemory.setForeground(new Color(200, 215, 230));
        infoPanel.add(lblMemory);

        lblUptime = new JLabel("⏱️ Uptime: 00:00:00");
        lblUptime.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUptime.setForeground(new Color(255, 217, 102));
        infoPanel.add(lblUptime);

        panel.add(infoPanel, BorderLayout.WEST);

        // Right actions
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnGC = new JButton("🧹 Dọn Rác RAM (GC)");
        btnGC.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnGC.addActionListener(e -> {
            System.gc();
            updateServerStatus();
            JOptionPane.showMessageDialog(this, "Đã thu hồi bộ nhớ JVM thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
        btnPanel.add(btnGC);

        JButton btnSaveAll = new JButton("💾 Lưu Toàn Bộ (Save All)");
        btnSaveAll.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSaveAll.addActionListener(e -> saveAllPlayers());
        btnPanel.add(btnSaveAll);

        JButton btnRestartTop = new JButton("🔄 Restart Server");
        btnRestartTop.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRestartTop.setBackground(new Color(220, 53, 69));
        btnRestartTop.setForeground(Color.WHITE);
        btnRestartTop.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Xác nhận lưu toàn bộ dữ liệu và khởi động lại Server?\n- Chọn YES: Khởi động lại Server\n- Chọn NO: Hủy thao tác", 
                "Xác nhận Restart Server", 
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this, "Đang lưu toàn bộ dữ liệu và khởi động lại Server...", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                ServerManager.gI().restart(false);
            }
        });
        btnPanel.add(btnRestartTop);

        JButton btnOpenWeb = new JButton("🌐 Bản Web (Browser)");
        btnOpenWeb.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOpenWeb.setBackground(new Color(0, 122, 204));
        btnOpenWeb.setForeground(Color.WHITE);
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
        btnPanel.add(btnOpenWeb);

        JButton btnRefreshAll = new JButton("🔄 Làm Mới Tất Cả");
        btnRefreshAll.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefreshAll.addActionListener(e -> refreshAllData());
        btnPanel.add(btnRefreshAll);

        panel.add(btnPanel, BorderLayout.EAST);
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

        JButton btnRefreshPlayers = new JButton("🔄 Làm mới danh sách");
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
        playerTable.setRowHeight(26);
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
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(new TitledBorder("Thao Tác Trực Tiếp Với Nhân Vật"));

        lblPlayerDetail = new JLabel("<html><div style='padding:8px;'><i>Hãy chọn một người chơi từ danh sách để thao tác...</i></div></html>");
        lblPlayerDetail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rightPanel.add(lblPlayerDetail, BorderLayout.NORTH);

        // Action Buttons Grid
        JPanel actionGrid = new JPanel(new GridLayout(5, 2, 8, 8));
        actionGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton btnAddGold = new JButton("💰 Cộng / Trừ Vàng");
        btnAddGold.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddGold.addActionListener(e -> handleAddGold());
        actionGrid.add(btnAddGold);

        JButton btnAddGem = new JButton("💎 Cộng / Trừ Ngọc Xanh");
        btnAddGem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddGem.addActionListener(e -> handleAddGem());
        actionGrid.add(btnAddGem);

        JButton btnAddRuby = new JButton("♦️ Cộng / Trừ Hồng Ngọc");
        btnAddRuby.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddRuby.addActionListener(e -> handleAddRuby());
        actionGrid.add(btnAddRuby);

        JButton btnAddPower = new JButton("⚡ Cộng Sức Mạnh & TN");
        btnAddPower.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddPower.addActionListener(e -> handleAddPower());
        actionGrid.add(btnAddPower);

        JButton btnHeal = new JButton("❤️ Hồi Đầy HP & KI");
        btnHeal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnHeal.addActionListener(e -> handleHealPlayer());
        actionGrid.add(btnHeal);

        JButton btnTeleport = new JButton("🚀 Dịch Chuyển (Map/Khu)");
        btnTeleport.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnTeleport.addActionListener(e -> handleTeleport());
        actionGrid.add(btnTeleport);

        JButton btnMsg = new JButton("💬 Gửi Thông Báo Riêng");
        btnMsg.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnMsg.addActionListener(e -> handleSendPlayerMessage());
        actionGrid.add(btnMsg);

        JButton btnSelectToBuff = new JButton("🎁 Chọn Để Buff Đồ (Tab 2)");
        btnSelectToBuff.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSelectToBuff.addActionListener(e -> handleSelectToBuff());
        actionGrid.add(btnSelectToBuff);

        JButton btnKick = new JButton("🚪 Kick Thoát Game");
        btnKick.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnKick.setForeground(new Color(204, 0, 0));
        btnKick.addActionListener(e -> handleKickPlayer());
        actionGrid.add(btnKick);

        JButton btnSavePl = new JButton("💾 Lưu Người Chơi Này");
        btnSavePl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSavePl.addActionListener(e -> handleSaveSelectedPlayer());
        actionGrid.add(btnSavePl);

        rightPanel.add(actionGrid, BorderLayout.CENTER);
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
            String detailHtml = String.format(
                "<html><div style='padding:8px; line-height: 1.5;'>"
                + "<b style='color:#007acc; font-size:14px;'>%s</b> (ID: %d)<br>"
                + "📍 <b>Vị trí:</b> %s (Zone: %d)<br>"
                + "❤️ <b>HP:</b> %s / %s | 🔷 <b>KI:</b> %s / %s<br>"
                + "⚔️ <b>Sức đánh:</b> %s | ⚡ <b>Sức mạnh:</b> %s<br>"
                + "💰 <b>Vàng:</b> %s | 💎 <b>Ngọc:</b> %s | ♦️ <b>Hồng ngọc:</b> %s"
                + "</div></html>",
                selectedPlayer.name,
                selectedPlayer.id,
                (selectedPlayer.zone != null && selectedPlayer.zone.map != null) ? selectedPlayer.zone.map.mapName : "N/A",
                (selectedPlayer.zone != null) ? selectedPlayer.zone.zoneId : -1,
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
        txtItemSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterItems();
            }
        });
        searchPanel.add(txtItemSearch);

        JButton btnReloadTemplates = new JButton("🔄 Nạp Lại CSDL");
        btnReloadTemplates.addActionListener(e -> {
            reloadItemTemplatesFromDB();
            loadItemTemplatesToTable();
        });
        searchPanel.add(btnReloadTemplates);

        topBar.add(searchPanel, BorderLayout.WEST);

        // CRUD Action Buttons
        JPanel crudBtnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));

        JButton btnAddItem = new JButton("➕ Thêm Item Mới");
        btnAddItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddItem.setBackground(new Color(40, 167, 69));
        btnAddItem.setForeground(Color.WHITE);
        btnAddItem.addActionListener(e -> openItemTemplateDialog(null));
        crudBtnBar.add(btnAddItem);

        JButton btnEditItem = new JButton("✏️ Sửa Item Đang Chọn");
        btnEditItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditItem.setBackground(new Color(255, 193, 7));
        btnEditItem.addActionListener(e -> handleEditSelectedItem());
        crudBtnBar.add(btnEditItem);

        JButton btnDeleteItemDB = new JButton("🗑️ Xóa Khỏi CSDL");
        btnDeleteItemDB.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeleteItemDB.setForeground(new Color(204, 0, 0));
        btnDeleteItemDB.addActionListener(e -> handleDeleteSelectedItemFromDB());
        crudBtnBar.add(btnDeleteItemDB);

        JButton btnManageIcon = new JButton("🎨 Quản Lý Icon (Thêm / Sửa)");
        btnManageIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnManageIcon.setBackground(new Color(111, 66, 193));
        btnManageIcon.setForeground(Color.WHITE);
        btnManageIcon.addActionListener(e -> openIconManagerDialog());
        crudBtnBar.add(btnManageIcon);

        topBar.add(crudBtnBar, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        // Center Split: Item Table left, Buff Form right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.62);

        String[] itemCols = {"ID Template", "Tên Vật Phẩm", "Loại (Type)", "Giới Tính", "Mô Tả", "Chỉ Số (Options)"};
        itemTableModel = new DefaultTableModel(itemCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        itemTable = new JTable(itemTableModel);
        itemTable.setRowHeight(24);
        itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        itemSorter = new TableRowSorter<>(itemTableModel);
        itemTable.setRowSorter(itemSorter);

        itemTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(190);
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        itemTable.getColumnModel().getColumn(4).setPreferredWidth(320);

        JScrollPane scrollPane = new JScrollPane(itemTable);
        splitPane.setLeftComponent(scrollPane);

        // Right Buff Form
        JPanel rightForm = new JPanel();
        rightForm.setLayout(new BoxLayout(rightForm, BoxLayout.Y_AXIS));
        rightForm.setBorder(new TitledBorder("Buff Vật Phẩm Vào Hành Trang Người Chơi"));

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        p1.add(new JLabel("Người nhận (Tên nhân vật):"));
        txtBuffPlayer = new JTextField(16);
        p1.add(txtBuffPlayer);
        rightForm.add(p1);

        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        p2.add(new JLabel("Số lượng buff:"));
        txtBuffQuantity = new JTextField("1", 8);
        p2.add(txtBuffQuantity);
        rightForm.add(p2);

        JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        p3.add(new JLabel("Chỉ số bổ sung (Option Preset):"));
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
        cbPresetOption.addActionListener(e -> {
            boolean isCustom = cbPresetOption.getSelectedIndex() == 8;
            txtCustomOptId.setEnabled(isCustom);
        });
        p3.add(cbPresetOption);
        rightForm.add(p3);

        JPanel p3b = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        p3b.add(new JLabel("Option ID tùy chỉnh:"));
        txtCustomOptId = new JTextField("50", 6);
        txtCustomOptId.setEnabled(false);
        p3b.add(txtCustomOptId);

        p3b.add(new JLabel("Giá trị chỉ số (Param):"));
        txtOptionParam = new JTextField("20", 6);
        p3b.add(txtOptionParam);
        rightForm.add(p3b);

        rightForm.add(Box.createVerticalStrut(15));

        JPanel p5 = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnBuff = new JButton("🎁 BUFF VÀO HÀNH TRANG NGAY");
        btnBuff.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBuff.setBackground(new Color(0, 123, 255));
        btnBuff.setForeground(Color.WHITE);
        btnBuff.addActionListener(e -> handleBuffItem());
        p5.add(btnBuff);

        JButton btnDeleteItem = new JButton("🗑️ Thu Hồi Khỏi Túi Đồ");
        btnDeleteItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDeleteItem.setForeground(new Color(204, 0, 0));
        btnDeleteItem.addActionListener(e -> handleDeleteItemFromBag());
        p5.add(btnDeleteItem);

        rightForm.add(p5);
        rightForm.add(Box.createVerticalGlue());

        splitPane.setRightComponent(rightForm);
        panel.add(splitPane, BorderLayout.CENTER);

        loadItemTemplatesToTable();
        return panel;
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
        JDialog dlg = new JDialog(this, editItem == null ? "Thêm Vật Phẩm Mới Vào CSDL" : "Chỉnh Sửa Vật Phẩm (ID: " + editItem.id + ")", true);
        dlg.setSize(880, 640);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        // LEFT PANEL: THÔNG TIN CƠ BẢN
        JPanel formLeft = new JPanel(new GridBagLayout());
        formLeft.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("📦 Thông Tin Cơ Bản Vật Phẩm"),
            new EmptyBorder(10, 12, 10, 12)
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

        JTextField txtId = new JTextField(editItem == null ? String.valueOf(nextId) : String.valueOf(editItem.id), 10);
        txtId.setEnabled(editItem == null);
        JTextField txtName = new JTextField(editItem == null ? "" : editItem.name, 16);

        String[] types = {
            "0 - Áo (Shirt)", "1 - Quần (Pants)", "2 - Găng tay (Glove)", "3 - Giày (Shoes)",
            "4 - Ra-đa (Radar)", "5 - Cải trang (Costume)", "6 - Đậu thần (Magic Bean)",
            "11 - Đeo lưng / Phụ kiện", "12 - Ngọc Rồng (Dragon Ball)", "14 - Thẻ quà",
            "21 - Pet / Đệ tử", "23 - Ván bay (Flying board)", "27 - Hộp quà (Giftbox)",
            "29 - Máy quét / Dò Boss", "72 - Linh thú", "73 - Hộp hủy diệt", "99 - Đặc biệt"
        };
        JComboBox<String> cbType = new JComboBox<>(types);
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
        if (editItem != null && editItem.gender >= 0 && editItem.gender < genders.length) {
            cbGender.setSelectedIndex(editItem.gender);
        } else if (editItem == null) {
            cbGender.setSelectedIndex(3);
        }

        JTextArea txtDesc = new JTextArea(editItem == null ? "" : editItem.description, 3, 16);
        txtDesc.setLineWrap(true);
        JTextField txtIcon = new JTextField(editItem == null ? "0" : String.valueOf(editItem.iconID), 8);
        JTextField txtPart = new JTextField(editItem == null ? "-1" : String.valueOf(editItem.part), 8);
        JTextField txtPower = new JTextField(editItem == null ? "0" : String.valueOf(editItem.strRequire), 10);
        JCheckBox chkStack = new JCheckBox("Có thể cộng dồn số lượng", editItem != null && editItem.isUpToUp);

        // Add components to formLeft
        formLeft.add(new JLabel("Mã Template ID:"), gbc);
        gbc.gridx = 1; formLeft.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy++; formLeft.add(new JLabel("Tên Vật Phẩm:"), gbc);
        gbc.gridx = 1; formLeft.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy++; formLeft.add(new JLabel("Phân Loại:"), gbc);
        gbc.gridx = 1; formLeft.add(cbType, gbc);

        gbc.gridx = 0; gbc.gridy++; formLeft.add(new JLabel("Giới Tính:"), gbc);
        gbc.gridx = 1; formLeft.add(cbGender, gbc);

        gbc.gridx = 0; gbc.gridy++; formLeft.add(new JLabel("Icon ID:"), gbc);
        JPanel pnlIconRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pnlIconRow.add(txtIcon);

        JLabel lblIconPreview = new JLabel();
        lblIconPreview.setPreferredSize(new Dimension(32, 32));
        lblIconPreview.setBorder(BorderFactory.createLineBorder(new Color(120, 120, 120)));
        lblIconPreview.setHorizontalAlignment(SwingConstants.CENTER);
        pnlIconRow.add(lblIconPreview);

        Runnable updateIconPreview = () -> {
            try {
                int id = Integer.parseInt(txtIcon.getText().trim());
                File f = new File("resources/normal/image/2/icon/" + id + ".png");
                if (!f.exists()) f = new File("resources/normal/image/1/icon/" + id + ".png");
                if (f.exists()) {
                    ImageIcon ic = new ImageIcon(f.getAbsolutePath());
                    Image img = ic.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
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
        txtIcon.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { updateIconPreview.run(); }
        });

        JButton btnChangeIconImg = new JButton("📁 Thay PNG");
        btnChangeIconImg.setFont(new Font("Segoe UI", Font.PLAIN, 10));
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
        gbc.gridx = 1; formLeft.add(pnlIconRow, gbc);

        gbc.gridx = 0; gbc.gridy++; formLeft.add(new JLabel("Part ID:"), gbc);
        gbc.gridx = 1; formLeft.add(txtPart, gbc);

        gbc.gridx = 0; gbc.gridy++; formLeft.add(new JLabel("Yêu Cầu SM:"), gbc);
        gbc.gridx = 1; formLeft.add(txtPower, gbc);

        gbc.gridx = 0; gbc.gridy++; formLeft.add(new JLabel("Mô Tả:"), gbc);
        gbc.gridx = 1; formLeft.add(new JScrollPane(txtDesc), gbc);

        gbc.gridx = 1; gbc.gridy++; formLeft.add(chkStack, gbc);

        // RIGHT PANEL: CHỈ SỐ (ITEM OPTIONS)
        JPanel pnlRight = new JPanel(new BorderLayout(8, 8));
        pnlRight.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("⚡ Chỉ Số Option Của Vật Phẩm"),
            new EmptyBorder(8, 8, 8, 8)
        ));

        String[] optTableCols = {"Mã Opt", "Tên Chỉ Số / Hiển Thị", "Param"};
        DefaultTableModel optModel = new DefaultTableModel(optTableCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tblOpts = new JTable(optModel);
        tblOpts.setRowHeight(22);
        tblOpts.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblOpts.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblOpts.getColumnModel().getColumn(0).setPreferredWidth(55);
        tblOpts.getColumnModel().getColumn(1).setPreferredWidth(210);
        tblOpts.getColumnModel().getColumn(2).setPreferredWidth(65);

        // Populate options from editItem
        if (editItem != null) {
            if (editItem.itemOptions != null && !editItem.itemOptions.isEmpty()) {
                for (ItemOption io : editItem.itemOptions) {
                    optModel.addRow(new Object[]{io.optionTemplate.id, io.getOptionString(), io.param});
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
                } catch (Exception ignored) {}
            }
        }

        // Presets Bar for Item Template
        JPanel pnlPresets = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        pnlPresets.setBorder(BorderFactory.createTitledBorder("⚡ Gán Nhanh Bộ Chỉ Số (Presets)"));

        JButton btnPreVip = new JButton("⚡ Set Cải Trang VIP");
        btnPreVip.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnPreVip.setBackground(new Color(23, 162, 184));
        btnPreVip.setForeground(Color.WHITE);

        JButton btnPreDmg = new JButton("⚔️ Sát Thương");
        btnPreDmg.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JButton btnPreTank = new JButton("🛡️ HP & KI");
        btnPreTank.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JButton btnPreHsd = new JButton("⏳ HSD 30N");
        btnPreHsd.setFont(new Font("Segoe UI", Font.PLAIN, 11));

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

        btnPreVip.addActionListener(e -> {
            applyPreset.accept(new int[][]{
                {77, 40}, {103, 40}, {50, 40}, {101, 100}, {14, 20}, {5, 100}, {106, 0}
            });
        });
        btnPreDmg.addActionListener(e -> {
            applyPreset.accept(new int[][]{
                {50, 50}, {14, 30}, {5, 150}, {95, 15}, {96, 15}
            });
        });
        btnPreTank.addActionListener(e -> {
            applyPreset.accept(new int[][]{
                {77, 80}, {103, 80}, {47, 1000}, {108, 25}
            });
        });
        btnPreHsd.addActionListener(e -> {
            applyPreset.accept(new int[][]{
                {93, 30}, {30, 0}
            });
        });

        pnlPresets.add(btnPreVip);
        pnlPresets.add(btnPreDmg);
        pnlPresets.add(btnPreTank);
        pnlPresets.add(btnPreHsd);

        pnlRight.add(pnlPresets, BorderLayout.NORTH);
        pnlRight.add(new JScrollPane(tblOpts), BorderLayout.CENTER);

        // Options control panel
        JPanel pnlOptCtrl = new JPanel(new GridBagLayout());
        pnlOptCtrl.setBorder(BorderFactory.createTitledBorder("Thao Tác Chỉ Số"));
        GridBagConstraints gbcO = new GridBagConstraints();
        gbcO.fill = GridBagConstraints.HORIZONTAL;
        gbcO.insets = new Insets(3, 4, 3, 4);

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
        JTextField txtSearchOpt = new JTextField(10);
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

        JTextField txtParamInput = new JTextField("10", 8);

        gbcO.gridx = 0; gbcO.gridy = 0; pnlOptCtrl.add(new JLabel("Tìm Chỉ Số:"), gbcO);
        gbcO.gridx = 1; gbcO.gridwidth = 2;
        JPanel pnlOptSearchBox = new JPanel(new BorderLayout(5, 0));
        pnlOptSearchBox.add(txtSearchOpt, BorderLayout.WEST);
        pnlOptSearchBox.add(cbOptionList, BorderLayout.CENTER);
        pnlOptCtrl.add(pnlOptSearchBox, gbcO);

        gbcO.gridx = 0; gbcO.gridy = 1; gbcO.gridwidth = 1; pnlOptCtrl.add(new JLabel("Giá Trị (Param):"), gbcO);
        gbcO.gridx = 1; pnlOptCtrl.add(txtParamInput, gbcO);

        JButton btnAddOpt = new JButton("➕ Thêm / Cập Nhật");
        btnAddOpt.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnAddOpt.setBackground(new Color(23, 162, 184));
        btnAddOpt.setForeground(Color.WHITE);

        JButton btnDelOpt = new JButton("🗑️ Xóa Đang Chọn");
        btnDelOpt.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnDelOpt.setBackground(new Color(220, 53, 69));
        btnDelOpt.setForeground(Color.WHITE);

        JButton btnClearOpt = new JButton("🧹 Xóa Hết");
        btnClearOpt.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        btnAddOpt.addActionListener(e -> {
            try {
                String selStr = (String) cbOptionList.getSelectedItem();
                int optId = Integer.parseInt(selStr.substring(0, selStr.indexOf(" -")).trim());
                int paramVal = Integer.parseInt(txtParamInput.getText().trim());
                ItemOption io = new ItemOption(optId, paramVal);
                String optName = io.getOptionString();

                // Check if already in table
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
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập giá trị Param hợp lệ (số nguyên)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
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

        JPanel pnlOptBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlOptBtns.add(btnAddOpt);
        pnlOptBtns.add(btnDelOpt);
        pnlOptBtns.add(btnClearOpt);

        gbcO.gridx = 0; gbcO.gridy = 2; gbcO.gridwidth = 3; pnlOptCtrl.add(pnlOptBtns, gbcO);
        pnlRight.add(pnlOptCtrl, BorderLayout.SOUTH);

        // Center split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formLeft, pnlRight);
        splitPane.setResizeWeight(0.48);
        dlg.add(splitPane, BorderLayout.CENTER);

        // Bottom action buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        JButton btnSave = new JButton("💾 Lưu Vật Phẩm & Chỉ Số");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
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
                for (int i = 0; i < optModel.getRowCount(); i++) {
                    int optId = (int) optModel.getValueAt(i, 0);
                    int paramVal = (int) optModel.getValueAt(i, 2);
                    optList.add(new ItemOption(optId, paramVal));
                }

                try (Connection con = DBService.gI().getConnection()) {
                    if (editItem == null) {
                        String sql = "INSERT INTO item_template (id, type, gender, name, description, icon_id, part, is_up_to_up, power_require) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement ps = con.prepareStatement(sql)) {
                            ps.setInt(1, id);
                            ps.setInt(2, type);
                            ps.setInt(3, gender);
                            ps.setString(4, name);
                            ps.setString(5, desc);
                            ps.setShort(6, icon);
                            ps.setShort(7, part);
                            ps.setBoolean(8, stack);
                            ps.setInt(9, power);
                            ps.executeUpdate();
                        }
                    } else {
                        String sql = "UPDATE item_template SET type=?, gender=?, name=?, description=?, icon_id=?, part=?, is_up_to_up=?, power_require=? WHERE id=?";
                        try (PreparedStatement ps = con.prepareStatement(sql)) {
                            ps.setInt(1, type);
                            ps.setInt(2, gender);
                            ps.setString(3, name);
                            ps.setString(4, desc);
                            ps.setShort(5, icon);
                            ps.setShort(6, part);
                            ps.setBoolean(7, stack);
                            ps.setInt(8, power);
                            ps.setInt(9, id);
                            ps.executeUpdate();
                        }
                    }

                    // Save options to item_template_option
                    String delOpt = "DELETE FROM item_template_option WHERE template_id = ?";
                    try (PreparedStatement psDelOpt = con.prepareStatement(delOpt)) {
                        psDelOpt.setInt(1, id);
                        psDelOpt.executeUpdate();
                    }
                    if (!optList.isEmpty()) {
                        String insOpt = "INSERT INTO item_template_option (template_id, option_id, param) VALUES (?, ?, ?)";
                        try (PreparedStatement psInsOpt = con.prepareStatement(insOpt)) {
                            for (ItemOption io : optList) {
                                psInsOpt.setInt(1, id);
                                psInsOpt.setInt(2, io.optionTemplate.id);
                                psInsOpt.setInt(3, io.param);
                                psInsOpt.addBatch();
                            }
                            psInsOpt.executeBatch();
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

        JButton btnCancel = new JButton("Đóng");
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

        JButton btnUpload = new JButton("📁 Chọn File PNG Để Lưu / Thay Thế");
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

        JButton btnDelete = new JButton("🗑️ Xóa Icon Này");
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
        JButton btnRefreshBoss = new JButton("🔄 Làm Mới Danh Sách Boss");
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

        JButton btnSpawn = new JButton("⚡ Triệu Hồi Boss Này");
        btnSpawn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSpawn.setBackground(new Color(255, 140, 0));
        btnSpawn.setForeground(Color.WHITE);
        btnSpawn.addActionListener(e -> handleSpawnBoss());
        topPanel.add(btnSpawn);

        JButton btnKillBoss = new JButton("🚪 Thu Hồi / Xóa Boss Đang Chọn");
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
        bossTable.setRowHeight(25);
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
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.add(new JLabel("Tìm tài khoản (Username hoặc ID):"));
        txtAccountSearch = new JTextField(18);
        topBar.add(txtAccountSearch);

        JButton btnSearchAcc = new JButton("🔍 Tìm Kiếm");
        btnSearchAcc.addActionListener(e -> searchAccounts());
        topBar.add(btnSearchAcc);

        JButton btnLoadRecent = new JButton("📋 100 Tài Khoản Mới Nhất");
        btnLoadRecent.addActionListener(e -> loadRecentAccounts());
        topBar.add(btnLoadRecent);

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
        accountTable.setRowHeight(25);
        accountTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        accountTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        panel.add(new JScrollPane(accountTable), BorderLayout.CENTER);

        // South Action Buttons
        JPanel southActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton btnActive = new JButton("🔓 Kích Hoạt Tài Khoản");
        btnActive.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnActive.addActionListener(e -> toggleAccountActive(1));
        southActions.add(btnActive);

        JButton btnBan = new JButton("⛔ Khóa / Mở Khóa (Ban)");
        btnBan.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBan.addActionListener(e -> toggleAccountBan());
        southActions.add(btnBan);

        JButton btnAdmin = new JButton("👑 Cấp / Hủy Admin");
        btnAdmin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdmin.addActionListener(e -> toggleAccountAdmin());
        southActions.add(btnAdmin);

        JButton btnChangePass = new JButton("🔑 Đổi Mật Khẩu");
        btnChangePass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnChangePass.addActionListener(e -> handleChangePassword());
        southActions.add(btnChangePass);

        JButton btnAddGoldBar = new JButton("🪙 Cộng Thỏi Vàng");
        btnAddGoldBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddGoldBar.addActionListener(e -> handleAddGoldBar());
        southActions.add(btnAddGoldBar);

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
                        rs.getInt("active") == 1 ? "🟢 Đã kích hoạt" : "🔴 Chưa kích hoạt",
                        rs.getInt("ban") == 1 ? "⛔ ĐANG BỊ KHÓA" : "🟢 Bình thường",
                        rs.getInt("is_admin") == 1 ? "👑 ADMIN" : "Người chơi",
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
                    rs.getInt("active") == 1 ? "🟢 Đã kích hoạt" : "🔴 Chưa kích hoạt",
                    rs.getInt("ban") == 1 ? "⛔ ĐANG BỊ KHÓA" : "🟢 Bình thường",
                    rs.getInt("is_admin") == 1 ? "👑 ADMIN" : "Người chơi",
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

    // ==========================================
    public static String getNpcName(int npcId) {
        if (Manager.NPC_TEMPLATES != null) {
            for (nro.models.npc.NpcTemplate nt : Manager.NPC_TEMPLATES) {
                if (nt != null && nt.id == npcId) {
                    return nt.name;
                }
            }
        }
        return "NPC #" + npcId;
    }

    // ==========================================
    // 6. TAB CỬA HÀNG (NPC SHOP) & GIFTCODE
    // ==========================================
    private JPanel createShopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.52);

        // Top: Shop Management
        JPanel pnlShop = new JPanel(new BorderLayout(6, 6));
        pnlShop.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("🏪 Quản Lý Cửa Hàng Shop NPC (Thêm / Sửa / Xóa Vật Phẩm Bán Trực Quan)"),
            new EmptyBorder(4, 6, 4, 6)
        ));

        JPanel pnlShopToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        pnlShopToolbar.add(new JLabel("Chọn NPC:"));
        JTextField txtFilterNpc = new JTextField(8);
        txtFilterNpc.setToolTipText("Gõ tìm nhanh NPC (Santa, Quy Lão, Gohan...)");
        pnlShopToolbar.add(txtFilterNpc);

        cbShopNpcSelect = new JComboBox<>();
        cbShopNpcSelect.setPreferredSize(new Dimension(220, 26));
        cbShopNpcSelect.addActionListener(e -> loadTabsForSelectedShop());
        pnlShopToolbar.add(cbShopNpcSelect);

        txtFilterNpc.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                String q = txtFilterNpc.getText().toLowerCase().trim();
                cbShopNpcSelect.removeAllItems();
                if (Manager.SHOPS != null) {
                    for (nro.models.shop.Shop s : Manager.SHOPS) {
                        if (s != null) {
                            String npcName = getNpcName(s.npcId);
                            String itemStr = s.id + " - " + npcName + " (NPC " + s.npcId + ", Order " + s.shopOrder + ")";
                            if (q.isEmpty() || itemStr.toLowerCase().contains(q) || npcName.toLowerCase().contains(q)) {
                                cbShopNpcSelect.addItem(itemStr);
                            }
                        }
                    }
                }
            }
        });

        pnlShopToolbar.add(new JLabel("Tab:"));
        cbShopTabSelect = new JComboBox<>();
        cbShopTabSelect.setPreferredSize(new Dimension(170, 26));
        cbShopTabSelect.addActionListener(e -> loadItemsForSelectedShopTab());
        pnlShopToolbar.add(cbShopTabSelect);

        JButton btnAddShopItem = new JButton("➕ Thêm Món Vào Shop");
        btnAddShopItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddShopItem.setBackground(new Color(40, 167, 69));
        btnAddShopItem.setForeground(Color.WHITE);
        btnAddShopItem.addActionListener(e -> handleAddShopItem());
        pnlShopToolbar.add(btnAddShopItem);

        JButton btnEditShopItem = new JButton("✏️ Sửa Món Đang Chọn");
        btnEditShopItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditShopItem.setBackground(new Color(255, 193, 7));
        btnEditShopItem.addActionListener(e -> handleEditSelectedShopItem());
        pnlShopToolbar.add(btnEditShopItem);

        JButton btnDeleteShopItem = new JButton("🗑️ Xóa Khỏi Shop");
        btnDeleteShopItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDeleteShopItem.setBackground(new Color(220, 53, 69));
        btnDeleteShopItem.setForeground(Color.WHITE);
        btnDeleteShopItem.addActionListener(e -> handleDeleteShopItem());
        pnlShopToolbar.add(btnDeleteShopItem);

        JButton btnReloadShop = new JButton("🔄 Nạp Lại Cửa Hàng");
        btnReloadShop.addActionListener(e -> reloadShops());
        pnlShopToolbar.add(btnReloadShop);

        pnlShop.add(pnlShopToolbar, BorderLayout.NORTH);

        String[] shopCols = {"ID Bán", "Mã Item", "Tên Vật Phẩm", "Giá Vàng", "Giá Ngọc", "Vật Phẩm Đổi", "SL Đổi", "Chỉ Số (Options)", "Trạng Thái"};
        shopItemTableModel = new DefaultTableModel(shopCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        shopItemTable = new JTable(shopItemTableModel);
        shopItemTable.setRowHeight(24);
        shopItemTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        shopItemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        shopItemTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        shopItemTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        shopItemTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        shopItemTable.getColumnModel().getColumn(3).setPreferredWidth(75);
        shopItemTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        shopItemTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        shopItemTable.getColumnModel().getColumn(6).setPreferredWidth(55);
        shopItemTable.getColumnModel().getColumn(7).setPreferredWidth(190);
        shopItemTable.getColumnModel().getColumn(8).setPreferredWidth(65);

        pnlShop.add(new JScrollPane(shopItemTable), BorderLayout.CENTER);
        splitPane.setTopComponent(pnlShop);

        // Bottom: Giftcode Management
        JPanel pnlCode = new JPanel(new BorderLayout(5, 5));
        pnlCode.setBorder(new TitledBorder("Mã Quà Tặng (GiftCode) - Chọn Đồ Trực Quan"));

        JPanel pnlCodeTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton btnRefreshCodes = new JButton("🔄 Làm Mới GiftCode");
        btnRefreshCodes.addActionListener(e -> refreshGiftcodeTable());
        pnlCodeTop.add(btnRefreshCodes);

        JButton btnCreateCode = new JButton("🎁 ➕ Tạo GiftCode Mới (Chọn Đồ)");
        btnCreateCode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCreateCode.setBackground(new Color(40, 167, 69));
        btnCreateCode.setForeground(Color.WHITE);
        btnCreateCode.addActionListener(e -> openVisualGiftCodeDialog(null));
        pnlCodeTop.add(btnCreateCode);

        JButton btnEditCode = new JButton("✏️ Sửa GiftCode Đang Chọn");
        btnEditCode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditCode.setBackground(new Color(255, 193, 7));
        btnEditCode.addActionListener(e -> handleEditSelectedGiftCode());
        pnlCodeTop.add(btnEditCode);

        JButton btnDeleteCode = new JButton("🗑️ Xóa GiftCode Đang Chọn");
        btnDeleteCode.setForeground(new Color(204, 0, 0));
        btnDeleteCode.addActionListener(e -> handleDeleteGiftcode());
        pnlCodeTop.add(btnDeleteCode);

        pnlCode.add(pnlCodeTop, BorderLayout.NORTH);

        String[] codeCols = {"ID", "Mã GiftCode", "Vàng", "Ngọc", "Ruby", "Số Món Đồ", "Chi Tiết Vật Phẩm", "Ngày Tạo"};
        giftcodeTableModel = new DefaultTableModel(codeCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        giftcodeTable = new JTable(giftcodeTableModel);
        giftcodeTable.setRowHeight(25);
        giftcodeTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        giftcodeTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        pnlCode.add(new JScrollPane(giftcodeTable), BorderLayout.CENTER);
        splitPane.setBottomComponent(pnlCode);

        panel.add(splitPane, BorderLayout.CENTER);
        refreshShopTables();
        return panel;
    }

    private void refreshShopTables() {
        loadShopsToCombo();
        refreshGiftcodeTable();
    }

    private void loadShopsToCombo() {
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
        dlg.setSize(680, 580);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(12, 16, 12, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField txtTempId = new JTextField("0", 8);
        JLabel lblItemName = new JLabel("Chưa chọn vật phẩm");
        lblItemName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblItemName.setForeground(new Color(40, 167, 69));

        JLabel lblIconPreview = new JLabel();
        lblIconPreview.setPreferredSize(new Dimension(32, 32));
        lblIconPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblIconPreview.setHorizontalAlignment(SwingConstants.CENTER);

        Runnable updateItemPreview = () -> {
            try {
                int tId = Integer.parseInt(txtTempId.getText().trim());
                ItemTemplate tmpl = ItemService.gI().getTemplate(tId);
                if (tmpl != null) {
                    lblItemName.setText(tmpl.name + " [ID: " + tmpl.id + "]");
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
                    lblItemName.setText("Không tìm thấy Template #" + tId);
                    lblIconPreview.setIcon(null);
                    lblIconPreview.setText("N/A");
                }
            } catch (Exception ex) {
                lblItemName.setText("ID không hợp lệ");
                lblIconPreview.setIcon(null);
                lblIconPreview.setText("N/A");
            }
        };

        txtTempId.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { updateItemPreview.run(); }
        });

        JButton btnPick = new JButton("🔍 Chọn Mẫu Item");
        btnPick.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(dlg, "Nhập ID hoặc Tên Vật Phẩm muốn tìm:", "Tìm Vật Phẩm", JOptionPane.QUESTION_MESSAGE);
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

        JTextField txtGold = new JTextField("0", 10);
        JTextField txtGem = new JTextField("0", 10);
        JTextField txtExchangeId = new JTextField("-1", 8);
        JTextField txtExchangeQty = new JTextField("0", 8);
        JCheckBox chkSell = new JCheckBox("Đang mở bán trong shop (is_sell)", true);
        JCheckBox chkNew = new JCheckBox("Hiển thị nhãn MỚI (is_new)", false);

        // Options Table
        String[] optCols = {"Mã Opt", "Tên Chỉ Số", "Param"};
        DefaultTableModel optModel = new DefaultTableModel(optCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tblOpt = new JTable(optModel);
        tblOpt.setRowHeight(22);

        // Load existing data if edit
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

        // Layout Form
        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Mẫu Vật Phẩm (ID):"), gbc);
        JPanel pnlItemSelect = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlItemSelect.add(txtTempId);
        pnlItemSelect.add(btnPick);
        pnlItemSelect.add(lblIconPreview);
        pnlItemSelect.add(lblItemName);
        gbc.gridx = 1; form.add(pnlItemSelect, gbc);

        gbc.gridx = 0; gbc.gridy++; form.add(new JLabel("Giá Vàng:"), gbc);
        gbc.gridx = 1; form.add(txtGold, gbc);

        gbc.gridx = 0; gbc.gridy++; form.add(new JLabel("Giá Ngọc (Gem / Hồng ngọc):"), gbc);
        gbc.gridx = 1; form.add(txtGem, gbc);

        gbc.gridx = 0; gbc.gridy++; form.add(new JLabel("Đổi bằng Item khác:"), gbc);
        JPanel pnlEx = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlEx.add(new JLabel("Mã Item Đổi (-1 = Ko):"));
        pnlEx.add(txtExchangeId);
        pnlEx.add(new JLabel("Số Lượng Cần:"));
        pnlEx.add(txtExchangeQty);
        gbc.gridx = 1; form.add(pnlEx, gbc);

        gbc.gridx = 1; gbc.gridy++;
        JPanel pnlChecks = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlChecks.add(chkSell);
        pnlChecks.add(chkNew);
        form.add(pnlChecks, gbc);

        // Options controls
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
        JTextField txtSearchShopOpt = new JTextField(8);
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
        JTextField txtParam = new JTextField("10", 6);

        JButton btnAddOpt = new JButton("➕ Thêm Option");
        btnAddOpt.addActionListener(e -> {
            try {
                String s = (String) cbOpt.getSelectedItem();
                int oId = Integer.parseInt(s.substring(0, s.indexOf(" -")).trim());
                int pVal = Integer.parseInt(txtParam.getText().trim());
                ItemOption io = new ItemOption(oId, pVal);
                optModel.addRow(new Object[]{oId, io.getOptionString(), pVal});
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Param không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnDelOpt = new JButton("🗑️ Xóa");
        btnDelOpt.addActionListener(e -> {
            int sel = tblOpt.getSelectedRow();
            if (sel >= 0) optModel.removeRow(sel);
        });

        // Presets for Shop Item
        JPanel pnlShopPresets = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnShopPreVip = new JButton("⚡ Set Cải Trang VIP");
        btnShopPreVip.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnShopPreVip.setBackground(new Color(23, 162, 184));
        btnShopPreVip.setForeground(Color.WHITE);

        JButton btnShopPreDmg = new JButton("⚔️ Sát Thương");
        btnShopPreDmg.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        JButton btnShopPreTank = new JButton("🛡️ HP & KI");
        btnShopPreTank.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        JButton btnShopPreHsd = new JButton("⏳ HSD 30N");
        btnShopPreHsd.setFont(new Font("Segoe UI", Font.PLAIN, 10));

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

        pnlShopPresets.add(btnShopPreVip);
        pnlShopPresets.add(btnShopPreDmg);
        pnlShopPresets.add(btnShopPreTank);
        pnlShopPresets.add(btnShopPreHsd);

        JPanel pnlOptInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlOptInputs.add(new JLabel("Tìm:"));
        pnlOptInputs.add(txtSearchShopOpt);
        pnlOptInputs.add(cbOpt);
        pnlOptInputs.add(new JLabel("Param:"));
        pnlOptInputs.add(txtParam);
        pnlOptInputs.add(btnAddOpt);
        pnlOptInputs.add(btnDelOpt);

        gbc.gridx = 0; gbc.gridy++; form.add(new JLabel("Mẫu Nhanh:"), gbc);
        gbc.gridx = 1; form.add(pnlShopPresets, gbc);

        gbc.gridx = 0; gbc.gridy++; form.add(new JLabel("Chỉ Số Option:"), gbc);
        gbc.gridx = 1; form.add(pnlOptInputs, gbc);

        gbc.gridx = 1; gbc.gridy++;
        JScrollPane spOpt = new JScrollPane(tblOpt);
        spOpt.setPreferredSize(new Dimension(380, 110));
        form.add(spOpt, gbc);

        dlg.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        JButton btnSave = new JButton("💾 Lưu Vào Cửa Hàng");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            try {
                short tId = Short.parseShort(txtTempId.getText().trim());
                if (ItemService.gI().getTemplate(tId) == null) {
                    JOptionPane.showMessageDialog(dlg, "Mã Template #" + tId + " không tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int gold = Integer.parseInt(txtGold.getText().trim());
                int gem = Integer.parseInt(txtGem.getText().trim());
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
        btnPanel.add(btnSave);

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dlg.dispose());
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
        JButton btnAddOpt = new JButton("➕ Gắn Option Này");
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

        JButton btnRemoveOpt = new JButton("❌ Xóa Option");
        btnRemoveOpt.addActionListener(e -> {
            int idx = tempOptsList.getSelectedIndex();
            if (idx >= 0) tempOptsModel.remove(idx);
        });
        pOptBtns.add(btnRemoveOpt);
        pnlItemControl.add(pOptBtns);
        pnlItemControl.add(new JScrollPane(tempOptsList));

        // Big Add To GiftCode Button
        JButton btnAddToGift = new JButton("🎁 ➕ THÊM MÓN NÀY VÀO GIFTCODE");
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
        JButton btnRemoveGiftItem = new JButton("🗑️ Xóa Món Đang Chọn");
        btnRemoveGiftItem.setForeground(new Color(204, 0, 0));
        pnlDelBtns.add(btnRemoveGiftItem);

        JButton btnClearAllGift = new JButton("🧹 Xóa Toàn Bộ Đồ");
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
        JButton btnSave = new JButton("💾 LƯU GIFTCODE VÀO DATABASE");
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
        pnlBroadcast.setBorder(new TitledBorder("📢 Gửi Thông Báo Toàn Server (Broadcast)"));

        JTextArea txtBroadcast = new JTextArea("Chào mừng các cư dân đến với Dragon Boy Server NRO NEW 01!");
        txtBroadcast.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBroadcast.setLineWrap(true);
        pnlBroadcast.add(new JScrollPane(txtBroadcast), BorderLayout.CENTER);

        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnSendYellow = new JButton("🔔 Gửi Thông Báo Chữ Vàng (Toàn Máy Chủ)");
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

        JButton btnSendGlobal = new JButton("💬 Gửi Kênh Thế Giới [ADMIN]");
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
        pnlMaintain.setBorder(new TitledBorder("⚙️ Bảo Trì Máy Chủ"));

        pnlMaintain.add(new JLabel("Hẹn giờ đếm ngược bảo trì server sau (giây):"));
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
        pnlRestart.setBorder(new TitledBorder("🔄 Khởi Động Lại Server (Restart Server)"));

        JButton btnQuickRestart = new JButton("⚡ Restart Server Ngay");
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

        JButton btnFullRestart = new JButton("🚀 Restart Toàn Bộ (Login + Server)");
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

        JButton btnCountdownRestart = new JButton("⏱️ Đếm Ngược & Restart");
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
        pnlEvent.setBorder(new TitledBorder("🎉 Cấu Hình Sự Kiện Server (Realtime Event Switcher)"));

        lblCurrentEventStatus = new JLabel("Sự kiện hiện tại: " + getEventName(Manager.EVENT_SEVER));
        lblCurrentEventStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCurrentEventStatus.setForeground(new Color(255, 152, 0));
        pnlEvent.add(lblCurrentEventStatus);

        pnlEvent.add(new JLabel("  |  Chuyển sang:"));
        String[] eventOptions = {
            "0 - Không có sự kiện (Mặc định)",
            "1 - Sự kiện Halloween (Kẹo bí ngô, phù thủy)",
            "2 - Sự kiện 20/11 (Nhà giáo, hoa hồng)",
            "3 - Sự kiện Noel (Giáng sinh, tuần lộc)",
            "4 - Sự kiện Tết Nguyên Đán (Bánh chưng, lì xì)",
            "5 - Sự kiện 8/3 (Quốc tế Phụ nữ, Capsule hồng)"
        };
        JComboBox<String> cbEvents = new JComboBox<>(eventOptions);
        if (Manager.EVENT_SEVER >= 0 && Manager.EVENT_SEVER < eventOptions.length) {
            cbEvents.setSelectedIndex(Manager.EVENT_SEVER);
        }
        pnlEvent.add(cbEvents);

        JCheckBox chkBroadcastEv = new JCheckBox("Gửi thông báo chữ vàng toàn server", true);
        pnlEvent.add(chkBroadcastEv);

        JButton btnApplyEv = new JButton("🚀 Kích Hoạt Sự Kiện Ngay");
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
        return switch (eventId) {
            case 1 -> "Sự kiện Halloween";
            case 2 -> "Sự kiện 20/11 (Nhà Giáo)";
            case 3 -> "Sự kiện Noel (Giáng Sinh)";
            case 4 -> "Sự kiện Tết Nguyên Đán";
            case 5 -> "Sự kiện 8/3 (Quốc Tế Phụ Nữ)";
            default -> "Chế độ bình thường (Không sự kiện)";
        };
    }

    public static void setServerEvent(int eventId, boolean broadcast) {
        Manager.EVENT_SEVER = (byte) eventId;
        String evName = getEventName(eventId);
        try {
            File propFile = new File("resources/config/server.properties");
            if (propFile.exists()) {
                List<String> lines = java.nio.file.Files.readAllLines(propFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                boolean found = false;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).trim().startsWith("server.event=")) {
                        lines.set(i, "server.event=" + eventId);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    lines.add("server.event=" + eventId);
                }
                java.nio.file.Files.write(propFile.toPath(), lines, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            Log.error(AdminPanelFrame.class, ex);
        }
        if (broadcast) {
            Service.getInstance().sendThongBaoAllPlayer("🎉 Máy chủ đã chuyển sang: " + evName + "!");
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
        try {
            // Online count
            int online = Client.gI().getPlayers().size();
            lblOnline.setText("🟢 Online: " + online);

            // Sessions count
            int sessions = Client.gI().getSessions().size();
            lblSessions.setText("🌐 Sessions: " + sessions);

            // Boss count
            int bossCount = BossManager.BOSSES_IN_GAME.size();
            lblBossCount.setText("👾 Boss: " + bossCount);

            // Memory
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long total = rt.totalMemory() / 1024 / 1024;
            lblMemory.setText("💾 RAM: " + used + " MB / " + total + " MB");

            // Uptime
            long uptime = (System.currentTimeMillis() - startTime) / 1000;
            long h = uptime / 3600;
            long m = (uptime % 3600) / 60;
            long s = uptime % 60;
            lblUptime.setText(String.format("⏱️ Uptime: %02d:%02d:%02d", h, m, s));
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
}
