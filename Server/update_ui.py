import re
import os

file_path = r'C:\Users\ADMIN\Desktop\SRC NRO NEW 01\Server\src\main\java\nro\panel\AdminPanelFrame.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update createModernButton and createStatusBadge, and add ModernTabbedPaneUI and createActionGroup
old_btn_defs = """    public static JButton createModernButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 1, true),
            new EmptyBorder(6, 12, 6, 12)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createStatusBadge(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(fg);
        lbl.setBackground(bg);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg.darker(), 1, true),
            new EmptyBorder(4, 10, 4, 10)
        ));
        return lbl;
    }"""

new_btn_defs = """    public static class ModernTabbedPaneUI extends javax.swing.plaf.basic.BasicTabbedPaneUI {
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
    }"""

if old_btn_defs in content:
    content = content.replace(old_btn_defs, new_btn_defs)
    print("1. Replaced createModernButton and createStatusBadge successfully.")
else:
    print("WARNING: old_btn_defs not found!")

# 2. Update tabbedPane in initComponents
old_tabs = """        // 2. MAIN TABBED PANE (TÁCH CỬA HÀNG VÀ GIFTCODE THÀNH 2 TAB RIÊNG)
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(COLOR_PANEL_DARK);
        tabbedPane.setForeground(COLOR_TEXT_LIGHT);

        tabbedPane.addTab("👤 Quản Lý Người Chơi", createPlayerPanel());
        tabbedPane.addTab("🎒 Vật Phẩm & Buff Đồ", createItemPanel());
        tabbedPane.addTab("🧙‍♂️ Quản Lý NPC Bản Đồ", createNpcPanel());
        tabbedPane.addTab("🎉 Quản Lý Sự Kiện & Tỷ Lệ", createEventPanel());
        tabbedPane.addTab("👹 Quản Lý Boss Realtime", createBossPanel());
        tabbedPane.addTab("🏪 Cửa Hàng (Shop NPC)", createShopPanel());
        tabbedPane.addTab("🎁 Mã Quà Tặng (GiftCode)", createGiftCodePanel());
        tabbedPane.addTab("🔐 Quản Lý Tài Khoản (CSDL)", createAccountPanel());
        tabbedPane.addTab("⚙️ Thông Báo & Bảo Trì", createSystemPanel());
        tabbedPane.addTab("💻 Console & Truy Vấn SQL", createConsolePanel());"""

new_tabs = """        // 2. MAIN TABBED PANE (TÁCH CỬA HÀNG VÀ GIFTCODE THÀNH 2 TAB RIÊNG)
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
        tabbedPane.addTab("Console & Truy Vấn SQL", createConsolePanel());"""

if old_tabs in content:
    content = content.replace(old_tabs, new_tabs)
    print("2. Replaced tabbedPane in initComponents successfully.")
else:
    print("WARNING: old_tabs not found!")

# 3. Update top title
old_top_title = 'JLabel lblTitle = new JLabel("NRO NEW 01 - BẢNG ĐIỀU KHIỂN MÁY CHỦ");'
new_top_title = 'JLabel lblTitle = new JLabel("● NRO NEW 01 • SERVER MANAGEMENT CONSOLE");'
if old_top_title in content:
    content = content.replace(old_top_title, new_top_title)
    print("3. Replaced top title successfully.")

# 4. Update Tab 1 Right Panel
old_right_panel = """        // Right Detail & Action Panel
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(new TitledBorder("Thao Tác Trực Tiếp Với Nhân Vật"));

        lblPlayerDetail = new JLabel("<html><div style='padding:8px;'><i>Hãy chọn một người chơi từ danh sách để thao tác...</i></div></html>");
        lblPlayerDetail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rightPanel.add(lblPlayerDetail, BorderLayout.NORTH);

        // Action Buttons Grid (Modern Colored Palette)
        JPanel actionGrid = new JPanel(new GridLayout(6, 2, 8, 8));
        actionGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton btnAddGold = createModernButton("💰 Cộng / Trừ Vàng", new Color(217, 119, 6), Color.WHITE);
        btnAddGold.addActionListener(e -> handleAddGold());
        actionGrid.add(btnAddGold);

        JButton btnAddGem = createModernButton("💎 Cộng / Trừ Ngọc Xanh", new Color(8, 145, 178), Color.WHITE);
        btnAddGem.addActionListener(e -> handleAddGem());
        actionGrid.add(btnAddGem);

        JButton btnAddRuby = createModernButton("🔴 Cộng / Trừ Hồng Ngọc", new Color(225, 29, 72), Color.WHITE);
        btnAddRuby.addActionListener(e -> handleAddRuby());
        actionGrid.add(btnAddRuby);

        JButton btnAddPower = createModernButton("⚡ Cộng Sức Mạnh & TN", new Color(147, 51, 234), Color.WHITE);
        btnAddPower.addActionListener(e -> handleAddPower());
        actionGrid.add(btnAddPower);

        JButton btnBuffPoint = createModernButton("🌟 Buff Chỉ Số (Sư Phụ)", new Color(16, 185, 129), Color.WHITE);
        btnBuffPoint.addActionListener(e -> openBuffPointDialog());
        actionGrid.add(btnBuffPoint);

        JButton btnHeal = createModernButton("❤️ Hồi Đầy HP & KI", new Color(37, 99, 235), Color.WHITE);
        btnHeal.addActionListener(e -> handleHealPlayer());
        actionGrid.add(btnHeal);

        JButton btnTeleport = createModernButton("🗺️ Dịch Chuyển (Map/Khu)", new Color(2, 132, 199), Color.WHITE);
        btnTeleport.addActionListener(e -> handleTeleport());
        actionGrid.add(btnTeleport);

        JButton btnMsg = createModernButton("📩 Gửi Thông Báo Riêng", new Color(99, 102, 241), Color.WHITE);
        btnMsg.addActionListener(e -> handleSendPlayerMessage());
        actionGrid.add(btnMsg);

        JButton btnSelectToBuff = createModernButton("🎁 Chọn Buff Đồ (Tab 2)", new Color(5, 150, 105), Color.WHITE);
        btnSelectToBuff.addActionListener(e -> handleSelectToBuff());
        actionGrid.add(btnSelectToBuff);

        JButton btnSavePl = createModernButton("💾 Lưu Người Chơi Này", new Color(71, 85, 105), Color.WHITE);
        btnSavePl.addActionListener(e -> handleSaveSelectedPlayer());
        actionGrid.add(btnSavePl);

        JButton btnKick = createModernButton("👢 Kick Thoát Game", new Color(220, 38, 38), Color.WHITE);
        btnKick.addActionListener(e -> handleKickPlayer());
        actionGrid.add(btnKick);

        JButton btnKickAll = createModernButton("🚪 Ngắt Kết Nối Toàn Bộ", new Color(185, 28, 28), Color.WHITE);
        btnKickAll.addActionListener(e -> handleKickAllPlayers());
        actionGrid.add(btnKickAll);

        rightPanel.add(actionGrid, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);"""

new_right_panel = """        // Right Detail & Action Panel
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
        splitPane.setRightComponent(rightPanel);"""

if old_right_panel in content:
    content = content.replace(old_right_panel, new_right_panel)
    print("4. Replaced Right Action Panel successfully.")
else:
    print("WARNING: old_right_panel not found!")

# 5. Update onPlayerSelected
old_on_player = """        if (selectedPlayer != null && selectedPlayer.nPoint != null && selectedPlayer.inventory != null) {
            String detailHtml = String.format(
                "<html><div style='padding:8px; line-height: 1.5;'>"
                + "<b style='color:#007acc; font-size:14px;'>%s</b> (ID: %d)<br>"
                + "<b>Vị trí:</b> %s (Zone: %d)<br>"
                + "<b>HP:</b> %s / %s | <b>KI:</b> %s / %s<br>"
                + "<b>Sức đánh:</b> %s | <b>Sức mạnh:</b> %s<br>"
                + "<b>Vàng:</b> %s | <b>Ngọc:</b> %s | <b>Hồng ngọc:</b> %s"
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
        }"""

new_on_player = """        if (selectedPlayer != null && selectedPlayer.nPoint != null && selectedPlayer.inventory != null) {
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
        }"""

if old_on_player in content:
    content = content.replace(old_on_player, new_on_player)
    print("5. Replaced onPlayerSelected successfully.")
else:
    print("WARNING: old_on_player not found!")

# 6. Replace remaining emoji strings
replacements = [
    # Item Panel
    ('createModernButton("➕ Thêm Item Mới"', 'createModernButton("Thêm Item Mới"'),
    ('createModernButton("✏️ Sửa Item Đang Chọn"', 'createModernButton("Sửa Item Đang Chọn"'),
    ('createModernButton("🗑️ Xóa Khỏi CSDL"', 'createModernButton("Xóa Khỏi CSDL"'),
    ('createModernButton("🎨 Quản Lý Icon"', 'createModernButton("Quản Lý Icon"'),
    ('"👁️ XEM TRƯỚC VẬT PHẨM (TOOLTIP PREVIEW)"', '"XEM TRƯỚC VẬT PHẨM (TOOLTIP PREVIEW)"'),
    ('JLabel lblOptsTitle = new JLabel("⚡ Chỉ số Option trong game:");', 'JLabel lblOptsTitle = new JLabel("Chỉ số Option trong game:");'),
    ('btnPreviewEdit = createModernButton("✏️ Sửa Mẫu Này"', 'btnPreviewEdit = createModernButton("Sửa Mẫu Này"'),
    ('btnPreviewDelete = createModernButton("🗑️ Xóa Khỏi CSDL"', 'btnPreviewDelete = createModernButton("Xóa Khỏi CSDL"'),
    ('🥋 Part Cải Trang:', 'Part Cải Trang:'),
    ('createModernButton("⚡ Set Cải Trang VIP"', 'createModernButton("Set Cải Trang VIP"'),
    ('createModernButton("⚔️ Sát Thương Khủng"', 'createModernButton("Sát Thương Khủng"'),
    ('createModernButton("🛡️ HP & KI Tanker"', 'createModernButton("HP & KI Tanker"'),
    ('createModernButton("⏳ HSD 30 Ngày"', 'createModernButton("HSD 30 Ngày"'),
    ('createModernButton("➕ Thêm / Cập Nhật"', 'createModernButton("Thêm / Cập Nhật"'),
    ('createModernButton("🗑️ Xóa Đang Chọn"', 'createModernButton("Xóa Đang Chọn"'),
    ('createModernButton("🧹 Xóa Hết"', 'createModernButton("Xóa Hết"'),
    ('createModernButton("💾 LƯU VẬT PHẨM & CHỈ SỐ"', 'createModernButton("LƯU VẬT PHẨM & CHỈ SỐ"'),
    
    # NPC Panel
    ('new JLabel("🗺️ Chọn Bản Đồ Quản Lý NPC:");', 'new JLabel("Chọn Bản Đồ Quản Lý NPC:");'),
    ('createModernButton("🔄 Tải Lại NPC Của Map"', 'createModernButton("Tải Lại NPC Của Map"'),
    ('createModernButton("💬 NPC Phát Biểu (Chat)"', 'createModernButton("NPC Phát Biểu (Chat)"'),
    ('"🛠️ Thêm Mới Hoặc Điều Chỉnh NPC"', '"Thêm Mới Hoặc Điều Chỉnh NPC"'),
    ('new JCheckBox("💾 Lưu vĩnh viễn vào CSDL (bảng map_template)"', 'new JCheckBox("Lưu vĩnh viễn vào CSDL (bảng map_template)"'),
    ('createModernButton("➕ Thêm NPC Vào Map Này"', 'createModernButton("Thêm NPC Vào Map Này"'),
    ('createModernButton("✏️ Cập Nhật NPC Đang Chọn"', 'createModernButton("Cập Nhật NPC Đang Chọn"'),
    ('createModernButton("🗑️ Xóa NPC Khỏi Map"', 'createModernButton("Xóa NPC Khỏi Map"'),

    # Event Panel
    ('"🎉 Cấu Hình & Bật Tắt Sự Kiện Server"', '"Cấu Hình & Bật Tắt Sự Kiện Server"'),
    ('createModernButton("💾 Áp Dụng Cấu Hình Sự Kiện"', 'createModernButton("Áp Dụng Cấu Hình Sự Kiện"'),
    ('createModernButton("📢 Báo Sự Kiện Toàn Server"', 'createModernButton("Báo Sự Kiện Toàn Server"'),
    ('"ℹ️ Chi Tiết & Hướng Dẫn Về Các Sự Kiện Trong Game"', '"Chi Tiết & Hướng Dẫn Về Các Sự Kiện Trong Game"'),

    # Console & SQL Panel
    ('"📝 Nhật Ký Hoạt Động Server (Console Log Realtime)"', '"Nhật Ký Hoạt Động Server (Console Log Realtime)"'),
    ('createModernButton("🧹 Xóa Màn Hình Log"', 'createModernButton("Xóa Màn Hình Log"'),
    ('createModernButton("📋 Sao Chép Log"', 'createModernButton("Sao Chép Log"'),
    ('"🗄️ Truy Vấn Cơ Sở Dữ Liệu SQL Trực Tiếp (Execute Direct SQL)"', '"Truy Vấn Cơ Sở Dữ Liệu SQL Trực Tiếp (Execute Direct SQL)"'),
    ('createModernButton("🚀 Thực Thi SQL (F5)"', 'createModernButton("Thực Thi SQL (F5)"'),
    ('createModernButton("🧹 Xóa Lệnh"', 'createModernButton("Xóa Lệnh"'),
]

rep_count = 0
for old_s, new_s in replacements:
    if old_s in content:
        content = content.replace(old_s, new_s)
        rep_count += 1
    else:
        print(f"Notice: '{old_s[:40]}' not matched exactly")

print(f"6. Applied {rep_count}/{len(replacements)} emoji string replacements.")

# Write back
with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Saved updated AdminPanelFrame.java successfully!")
