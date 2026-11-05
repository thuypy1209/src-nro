import re

file_path = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\src\main\java\nro\panel\AdminPanelFrame.java'

with open(file_path, 'r', encoding='utf-8') as f:
    code = f.read()

# Let's locate openShopItemDialog
start_idx = code.find('private void openShopItemDialog(Integer editShopItemId, int tabId) {')
if start_idx == -1:
    print("ERROR: openShopItemDialog not found!")
    exit(1)

# Find end of method before private void reloadShops()
end_idx = code.find('private void reloadShops() {', start_idx)
if end_idx == -1:
    print("ERROR: reloadShops not found!")
    exit(1)

new_dialog_code = """    private void openShopItemDialog(Integer editShopItemId, int tabId) {
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

    """

code = code[:start_idx] + new_dialog_code + code[end_idx:]

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(code)

print("Updated openShopItemDialog successfully!")
