import re

file_path = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\src\main\java\nro\panel\AdminPanelFrame.java'

with open(file_path, 'r', encoding='utf-8') as f:
    code = f.read()

# 1. Update createAccountPanel top and south buttons
old_account_panel = """        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.add(new JLabel("Tìm tài khoản (Username hoặc ID):"));
        txtAccountSearch = new JTextField(18);
        topBar.add(txtAccountSearch);

        JButton btnSearchAcc = new JButton("Tìm Kiếm");
        btnSearchAcc.addActionListener(e -> searchAccounts());
        topBar.add(btnSearchAcc);

        JButton btnLoadRecent = new JButton("100 Tài Khoản Mới Nhất");
        btnLoadRecent.addActionListener(e -> loadRecentAccounts());
        topBar.add(btnLoadRecent);

        JButton btnAddAcc = new JButton("Thêm Tài Khoản Mới Mới");
        btnAddAcc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddAcc.setBackground(new Color(40, 167, 69));
        btnAddAcc.setForeground(Color.WHITE);
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
        JPanel southActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton btnActive = new JButton("Kích Hoạt Tài Khoản");
        btnActive.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnActive.addActionListener(e -> toggleAccountActive(1));
        southActions.add(btnActive);

        JButton btnBan = new JButton("Khóa / Mở Khóa (Ban)");
        btnBan.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBan.addActionListener(e -> toggleAccountBan());
        southActions.add(btnBan);

        JButton btnAdmin = new JButton("Cấp / Hủy Admin");
        btnAdmin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdmin.addActionListener(e -> toggleAccountAdmin());
        southActions.add(btnAdmin);

        JButton btnChangePass = new JButton("Đổi Mật Khẩu");
        btnChangePass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnChangePass.addActionListener(e -> handleChangePassword());
        southActions.add(btnChangePass);

        JButton btnAddGoldBar = new JButton("Cộng Thỏi Vàng");
        btnAddGoldBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddGoldBar.addActionListener(e -> handleAddGoldBar());
        southActions.add(btnAddGoldBar);

        JButton btnDeleteAcc = new JButton("Xóa Tài Khoản");
        btnDeleteAcc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeleteAcc.setBackground(new Color(220, 53, 69));
        btnDeleteAcc.setForeground(Color.WHITE);
        btnDeleteAcc.addActionListener(e -> handleDeleteAccount());
        southActions.add(btnDeleteAcc);"""

new_account_panel = """        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
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
        southActions.add(btnDeleteAcc);"""

if old_account_panel in code:
    code = code.replace(old_account_panel, new_account_panel)
    print("1. Replaced createAccountPanel successfully.")
else:
    print("WARNING: old_account_panel not found!")

# 2. Update handleCreateAccount
old_create_account = """    private void handleCreateAccount() {
        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField("1");
        JComboBox<String> cbActive = new JComboBox<>(new String[]{"1 - Đã kích hoạt", "0 - Chưa kích hoạt"});
        JComboBox<String> cbAdmin = new JComboBox<>(new String[]{"0 - Người chơi", "1 - Quản trị viên (ADMIN)"});
        JTextField txtThoiVang = new JTextField("0");

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.add(new JLabel("Tên đăng nhập:")); form.add(txtUser);
        form.add(new JLabel("Mật khẩu:")); form.add(txtPass);
        form.add(new JLabel("Trạng thái active:")); form.add(cbActive);
        form.add(new JLabel("Quyền hạn:")); form.add(cbAdmin);
        form.add(new JLabel("Thỏi vàng khởi tạo:")); form.add(txtThoiVang);

        int result = JOptionPane.showConfirmDialog(this, form, "Thêm Tài Khoản Mới Mới Vào CSDL", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String u = txtUser.getText().trim();
            String p = new String(txtPass.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên đăng nhập và mật khẩu không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int act = cbActive.getSelectedIndex() == 0 ? 1 : 0;
            int adm = cbAdmin.getSelectedIndex() == 1 ? 1 : 0;
            int tv = 0;
            try { tv = Integer.parseInt(txtThoiVang.getText().trim()); } catch (Exception ignored) {}

            try (Connection con = DBService.gI().getConnection()) {
                try (PreparedStatement psCheck = con.prepareStatement("SELECT id FROM account WHERE username = ?")) {
                    psCheck.setString(1, u);
                    if (psCheck.executeQuery().next()) {
                        JOptionPane.showMessageDialog(this, "Tên tài khoản '" + u + "' đã tồn tại trong CSDL!", "Lỗi trùng lặp", JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.showMessageDialog(this, "Tạo tài khoản '" + u + "' (ID: " + newId + ") thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    loadRecentAccounts();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi tạo tài khoản: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }"""

new_create_account = """    private void handleCreateAccount() {
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
    }"""

if old_create_account in code:
    code = code.replace(old_create_account, new_create_account)
    print("2. Replaced handleCreateAccount successfully.")
else:
    print("WARNING: old_create_account not found!")

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(code)

print("Saved updated AdminPanelFrame.java successfully!")
