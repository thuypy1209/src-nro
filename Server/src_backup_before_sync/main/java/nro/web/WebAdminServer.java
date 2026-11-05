package nro.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import nro.jdbc.DBService;
import nro.jdbc.daos.ShopDAO;
import nro.models.shop.Shop;
import nro.models.shop.TabShop;
import nro.models.boss.Boss;
import nro.models.boss.BossFactory;
import nro.models.boss.BossManager;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.item.ItemOptionTemplate;
import nro.models.item.ItemTemplate;
import nro.models.player.Player;
import nro.panel.AdminPanelFrame;
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
 * Web Admin Panel HTTP Server cho SRC NRO NEW 01:
 * - Chạy trên cổng cấu hình api.port (mặc định 8181).
 * - Cho phép truy cập qua trình duyệt Web hoặc nhúng trong iframe (X-Frame-Options / CSP frame-ancestors).
 * - Cung cấp đầy đủ REST API và giao diện Web SPA trực quan:
 *   1. Dashboard: Online, Session, Boss, RAM, Uptime, GC, Save All
 *   2. Quản lý người chơi: Buff tiền/ngọc/ruby/sức mạnh, hồi phục HP/KI, dịch chuyển map, kick, save
 *   3. Quản lý vật phẩm: CRUD Item Template trong MySQL + reload, Buff đồ kèm Option, Thu hồi đồ
 *   4. Quản lý Boss: Danh sách boss realtime, xóa/thu hồi boss, triệu hồi boss nhanh
 *   5. Quản lý Tài khoản: Tìm kiếm, Kích hoạt, Khóa/Mở khóa ban, Cấp Admin, Đổi pass, Thỏi vàng
 *   6. Quản lý Cửa Hàng & GiftCode: Bộ chọn quà trực quan, sửa/xóa giftcode, nạp lại Shop
 *   7. Hệ thống: Broadcast toàn server, hẹn giờ đếm ngược bảo trì
 */
public class WebAdminServer {

    private static WebAdminServer instance;
    private HttpServer server;
    private int port = 8181;
    private final long startTime;
    private final NumberFormat numFmt = NumberFormat.getInstance(Locale.US);

    public static synchronized WebAdminServer gI() {
        if (instance == null) {
            instance = new WebAdminServer();
        }
        return instance;
    }

    private WebAdminServer() {
        this.startTime = System.currentTimeMillis();
    }

    public synchronized void start(int serverPort) {
        if (server != null) {
            Log.warning("Web Admin Server da dang chay tren cong " + this.port);
            return;
        }

        this.port = (serverPort > 0) ? serverPort : 8181;
        try {
            server = HttpServer.create(new InetSocketAddress(this.port), 0);
            server.setExecutor(Executors.newFixedThreadPool(10));

            // Static Web Page & Assets Handler
            server.createContext("/", new StaticFileHandler());

            // API Endpoints
            server.createContext("/api/status", new StatusHandler());
            server.createContext("/api/system/gc", new GcHandler());
            server.createContext("/api/system/save_all", new SaveAllHandler());
            server.createContext("/api/system/broadcast", new BroadcastHandler());
            server.createContext("/api/system/maintenance", new MaintenanceHandler());
            server.createContext("/api/system/restart", new RestartHandler());

            server.createContext("/api/players", new PlayersHandler());
            server.createContext("/api/player/action", new PlayerActionHandler());

            server.createContext("/api/items", new ItemsHandler());
            server.createContext("/api/item/buff", new BuffItemHandler());
            server.createContext("/api/item/remove", new RemoveItemBagHandler());
            server.createContext("/api/item_template/save", new SaveItemTemplateHandler());
            server.createContext("/api/item_template/delete", new DeleteItemTemplateHandler());
            server.createContext("/api/item_options", new ItemOptionsHandler());

            server.createContext("/api/bosses", new BossesHandler());
            server.createContext("/api/boss/spawn", new SpawnBossHandler());
            server.createContext("/api/boss/remove", new RemoveBossHandler());

            server.createContext("/api/accounts", new AccountsHandler());
            server.createContext("/api/account/action", new AccountActionHandler());

            server.createContext("/api/giftcodes", new GiftcodesHandler());
            server.createContext("/api/giftcode/save", new SaveGiftcodeHandler());
            server.createContext("/api/giftcode/delete", new DeleteGiftcodeHandler());

            server.createContext("/api/shop/reload", new ReloadShopHandler());
            server.createContext("/api/shops", new ShopsHandler());
            server.createContext("/api/shop/items", new ShopItemsHandler());
            server.createContext("/api/shop/item/save", new SaveShopItemHandler());
            server.createContext("/api/shop/item/delete", new DeleteShopItemHandler());
            server.createContext("/api/shop/create", new CreateShopHandler());
            server.createContext("/api/shop/tab/save", new SaveShopTabHandler());
            server.createContext("/api/event", new EventStatusHandler());
            server.createContext("/api/event/set", new SetEventHandler());

            server.createContext("/api/icon", new IconHandler());
            server.createContext("/api/icons", new IconsListHandler());
            server.createContext("/api/icon/save", new SaveIconHandler());
            server.createContext("/api/icon/delete", new DeleteIconHandler());

            server.start();
            Log.success("==========================================================");
            Log.success("🚀 Web Admin Panel da khoi dong thanh cong tren cong: " + this.port);
            Log.success("🌐 Truy cap tai: http://localhost:" + this.port + "/");
            Log.success("==========================================================");
        } catch (Exception ex) {
            Log.error(WebAdminServer.class, ex);
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            Log.success("Web Admin Server da dung.");
        }
    }

    public static void main(String[] args) {
        int p = (args.length > 0) ? Integer.parseInt(args[0]) : 8181;
        WebAdminServer.gI().start(p);
    }

    // ==========================================
    // HTTP UTILITIES & CORS & IFRAME HEADERS
    // ==========================================

    private static void applyCorsAndIframeHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        exchange.getResponseHeaders().set("X-Frame-Options", "ALLOWALL");
        exchange.getResponseHeaders().set("Content-Security-Policy", "frame-ancestors *");
    }

    private static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            applyCorsAndIframeHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, JSONObject json) throws IOException {
        sendResponse(exchange, statusCode, "application/json; charset=UTF-8", json.toString());
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        applyCorsAndIframeHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
            return os.toString(StandardCharsets.UTF_8);
        }
    }

    // ==========================================
    // 1. STATIC FILE HANDLER (HTML / CSS / JS)
    // ==========================================
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            String path = exchange.getRequestURI().getPath();
            if (path == null || path.isEmpty() || path.equals("/")) {
                path = "/index.html";
            }

            File webDir = new File("resources/web");
            File requestedFile = new File(webDir, path.replace("/", File.separator));

            // Security check to avoid directory traversal
            try {
                if (!requestedFile.getCanonicalPath().startsWith(webDir.getCanonicalPath())) {
                    sendResponse(exchange, 403, "text/plain; charset=UTF-8", "Forbidden");
                    return;
                }
            } catch (Exception ignored) {
            }

            if (requestedFile.exists() && requestedFile.isFile()) {
                String contentType = "text/html; charset=UTF-8";
                String nameLower = requestedFile.getName().toLowerCase();
                if (nameLower.endsWith(".css")) {
                    contentType = "text/css; charset=UTF-8";
                } else if (nameLower.endsWith(".js")) {
                    contentType = "application/javascript; charset=UTF-8";
                } else if (nameLower.endsWith(".png")) {
                    contentType = "image/png";
                } else if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (nameLower.endsWith(".ico")) {
                    contentType = "image/x-icon";
                } else if (nameLower.endsWith(".json")) {
                    contentType = "application/json; charset=UTF-8";
                }

                byte[] bytes = Files.readAllBytes(requestedFile.toPath());
                applyCorsAndIframeHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // If index.html not yet on disk, return built-in welcome message
                sendResponse(exchange, 404, "text/html; charset=UTF-8",
                    "<html><body style='font-family:sans-serif;background:#111;color:#eee;padding:40px;'>"
                    + "<h2>Dragon Boy - Web Admin Panel</h2>"
                    + "<p>File <code>resources/web/index.html</code> chua ton tai tren disk.</p>"
                    + "</body></html>");
            }
        }
    }

    // ==========================================
    // 2. DASHBOARD STATUS & SYSTEM ACTIONS
    // ==========================================
    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            JSONObject res = new JSONObject();
            try {
                int online = (Client.gI() != null && Client.gI().getPlayers() != null) ? Client.gI().getPlayers().size() : 0;
                int sessions = (Client.gI() != null && Client.gI().getSessions() != null)
                        ? Client.gI().getSessions().size() : 0;
                int bosses = (BossManager.BOSSES_IN_GAME != null) ? BossManager.BOSSES_IN_GAME.size() : 0;

                Runtime rt = Runtime.getRuntime();
                long usedMem = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
                long maxMem = rt.maxMemory() / 1024 / 1024;

                long upSeconds = (System.currentTimeMillis() - startTime) / 1000;
                long hours = upSeconds / 3600;
                long mins = (upSeconds % 3600) / 60;
                long secs = upSeconds % 60;
                String uptime = String.format("%02d:%02d:%02d", hours, mins, secs);

                res.put("status", "success");
                res.put("online", online);
                res.put("sessions", sessions);
                res.put("bosses", bosses);
                res.put("usedMem", usedMem);
                res.put("maxMem", maxMem);
                res.put("uptime", uptime);
                res.put("apiPort", port);
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class GcHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            System.gc();
            JSONObject res = new JSONObject();
            res.put("status", "success");
            res.put("message", "Đã chạy Garbage Collection (Dọn RAM) thành công!");
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class SaveAllHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                int count = 0;
                if (Client.gI() != null && Client.gI().getPlayers() != null) {
                    for (Player pl : Client.gI().getPlayers()) {
                        if (pl != null) {
                            PlayerService.gI().savePlayer(pl);
                            count++;
                        }
                    }
                }
                res.put("status", "success");
                res.put("message", "Đã lưu dữ liệu cho toàn bộ " + count + " người chơi online!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class BroadcastHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                String msg = req.optString("message", "").trim();
                String type = req.optString("type", "yellow");

                if (msg.isEmpty()) {
                    res.put("status", "error");
                    res.put("message", "Nội dung thông báo không được để trống!");
                } else {
                    if ("global".equalsIgnoreCase(type)) {
                        Service.getInstance().sendThongBaoAllPlayer("[ADMIN]: " + msg);
                    } else {
                        Service.getInstance().sendThongBaoAllPlayer(msg);
                    }
                    res.put("status", "success");
                    res.put("message", "Đã phát thông báo toàn server thành công!");
                }
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class MaintenanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                int seconds = req.optInt("seconds", 60);
                if (seconds < 5) seconds = 5;

                Maintenance.gI().start(seconds);
                res.put("status", "success");
                res.put("message", "Đã bắt đầu đếm ngược bảo trì server sau " + seconds + " giây!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class RestartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = (body == null || body.trim().isEmpty()) ? new JSONObject() : new JSONObject(body);
                String type = req.optString("type", "quick");
                int seconds = req.optInt("seconds", 15);
                boolean full = "full".equalsIgnoreCase(type);

                if ("countdown".equalsIgnoreCase(type)) {
                    if (seconds < 5) seconds = 5;
                    ServerManager.gI().restartWithCountdown(seconds, full);
                    res.put("status", "success");
                    res.put("message", "Đã phát thông báo đếm ngược " + seconds + " giây và sẽ tự động Restart Server!");
                } else {
                    res.put("status", "success");
                    res.put("message", full ? "Đang lưu toàn bộ dữ liệu và kích hoạt Restart_Server.bat..." : "Đang lưu toàn bộ dữ liệu và khởi động lại Server...");
                    new Thread(() -> {
                        try {
                            Thread.sleep(800);
                        } catch (Exception ignored) {}
                        ServerManager.gI().restart(full);
                    }).start();
                }
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    // ==========================================
    // 3. QUẢN LÝ NGƯỜI CHƠI (PLAYERS)
    // ==========================================
    private class PlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            JSONObject res = new JSONObject();
            JSONArray list = new JSONArray();
            try {
                List<Player> players = Client.gI().getPlayers();
                if (players != null) {
                    for (Player pl : players) {
                        if (pl != null && pl.nPoint != null && pl.inventory != null) {
                            JSONObject pObj = new JSONObject();
                            pObj.put("id", pl.id);
                            pObj.put("name", pl.name);
                            pObj.put("userId", (pl.getSession() != null) ? pl.getSession().userId : -1);
                            pObj.put("gender", pl.gender);
                            String genderStr = switch (pl.gender) {
                                case 0 -> "Trái Đất";
                                case 1 -> "Namếc";
                                case 2 -> "Xayda";
                                default -> "Khác";
                            };
                            pObj.put("genderName", genderStr);
                            pObj.put("mapName", (pl.zone != null && pl.zone.map != null) ? pl.zone.map.mapName : "Chưa rõ");
                            pObj.put("zoneId", (pl.zone != null) ? pl.zone.zoneId : -1);
                            pObj.put("power", pl.nPoint.power);
                            pObj.put("powerFmt", numFmt.format(pl.nPoint.power));
                            pObj.put("tiemNang", pl.nPoint.tiemNang);
                            pObj.put("tiemNangFmt", numFmt.format(pl.nPoint.tiemNang));
                            pObj.put("gold", pl.inventory.gold);
                            pObj.put("goldFmt", numFmt.format(pl.inventory.gold));
                            pObj.put("gem", pl.inventory.gem);
                            pObj.put("gemFmt", numFmt.format(pl.inventory.gem));
                            pObj.put("ruby", pl.inventory.ruby);
                            pObj.put("rubyFmt", numFmt.format(pl.inventory.ruby));
                            pObj.put("hp", pl.nPoint.hp);
                            pObj.put("hpMax", pl.nPoint.hpMax);
                            pObj.put("mp", pl.nPoint.mp);
                            pObj.put("mpMax", pl.nPoint.mpMax);
                            pObj.put("dame", pl.nPoint.dame);
                            list.put(pObj);
                        }
                    }
                }
                res.put("status", "success");
                res.put("data", list);
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private class PlayerActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);

                String action = req.optString("action", "");
                long playerId = req.optLong("playerId", -1);
                String playerName = req.optString("playerName", "");

                Player target = (playerId != -1) ? Client.gI().getPlayer(playerId) : Client.gI().getPlayer(playerName);
                if (target == null) {
                    res.put("status", "error");
                    res.put("message", "Người chơi không online hoặc không tồn tại!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                switch (action) {
                    case "buff_gold": {
                        long amount = req.optLong("amount", 0);
                        if (amount >= 0) target.inventory.addGold(amount);
                        else target.inventory.subGold(Math.abs(amount));
                        Service.getInstance().sendMoney(target);
                        Service.getInstance().sendThongBao(target, "Admin đã cập nhật " + numFmt.format(amount) + " Vàng cho bạn.");
                        res.put("status", "success");
                        res.put("message", "Đã cập nhật Vàng cho " + target.name + " thành công!");
                        break;
                    }
                    case "buff_gem": {
                        int amount = req.optInt("amount", 0);
                        target.inventory.gem += amount;
                        if (target.inventory.gem < 0) target.inventory.gem = 0;
                        Service.getInstance().sendMoney(target);
                        Service.getInstance().sendThongBao(target, "Admin đã cập nhật " + numFmt.format(amount) + " Ngọc Xanh cho bạn.");
                        res.put("status", "success");
                        res.put("message", "Đã cập nhật Ngọc Xanh cho " + target.name + " thành công!");
                        break;
                    }
                    case "buff_ruby": {
                        int amount = req.optInt("amount", 0);
                        target.inventory.ruby += amount;
                        if (target.inventory.ruby < 0) target.inventory.ruby = 0;
                        Service.getInstance().sendMoney(target);
                        Service.getInstance().sendThongBao(target, "Admin đã cập nhật " + numFmt.format(amount) + " Hồng Ngọc cho bạn.");
                        res.put("status", "success");
                        res.put("message", "Đã cập nhật Hồng Ngọc cho " + target.name + " thành công!");
                        break;
                    }
                    case "buff_power": {
                        long amount = req.optLong("amount", 0);
                        target.nPoint.power += amount;
                        target.nPoint.tiemNang += amount;
                        if (target.nPoint.power < 0) target.nPoint.power = 0;
                        if (target.nPoint.tiemNang < 0) target.nPoint.tiemNang = 0;
                        PlayerService.gI().sendInfoHpMpMoney(target);
                        Service.getInstance().sendThongBao(target, "Admin đã cộng " + numFmt.format(amount) + " Sức mạnh & Tiềm năng cho bạn!");
                        res.put("status", "success");
                        res.put("message", "Đã cập nhật Sức mạnh cho " + target.name + " thành công!");
                        break;
                    }
                    case "heal": {
                        target.nPoint.hp = target.nPoint.hpMax;
                        target.nPoint.mp = target.nPoint.mpMax;
                        PlayerService.gI().sendInfoHpMpMoney(target);
                        Service.getInstance().sendThongBao(target, "Admin đã hồi phục đầy đủ HP và KI cho bạn!");
                        res.put("status", "success");
                        res.put("message", "Đã hồi đầy HP/KI cho " + target.name + "!");
                        break;
                    }
                    case "teleport": {
                        int mapId = req.optInt("mapId", 0);
                        int zoneId = req.optInt("zoneId", 0);
                        ChangeMapService.gI().changeMapInYard(target, mapId, zoneId, 400);
                        Service.getInstance().sendThongBao(target, "Admin đã dịch chuyển bạn sang Map " + mapId + " Khu " + zoneId + "!");
                        res.put("status", "success");
                        res.put("message", "Đã dịch chuyển " + target.name + " sang Map " + mapId + "!");
                        break;
                    }
                    case "message": {
                        String msg = req.optString("text", "").trim();
                        if (!msg.isEmpty()) {
                            Service.getInstance().sendThongBao(target, "[ADMIN]: " + msg);
                            res.put("status", "success");
                            res.put("message", "Đã gửi thông báo đến " + target.name + "!");
                        } else {
                            res.put("status", "error");
                            res.put("message", "Nội dung tin nhắn không được để trống!");
                        }
                        break;
                    }
                    case "kick": {
                        Service.getInstance().sendThongBao(target, "Bạn đã bị Admin ngắt kết nối khỏi máy chủ!");
                        try { Thread.sleep(500); } catch (Exception ignored) {}
                        if (target.getSession() != null) {
                            Client.gI().kickSession(target.getSession());
                        }
                        res.put("status", "success");
                        res.put("message", "Đã ngắt kết nối (kick) " + target.name + " khỏi game!");
                        break;
                    }
                    case "save": {
                        PlayerService.gI().savePlayer(target);
                        res.put("status", "success");
                        res.put("message", "Đã lưu dữ liệu nhân vật " + target.name + " thành công!");
                        break;
                    }
                    default:
                        res.put("status", "error");
                        res.put("message", "Hành động không hợp lệ: " + action);
                        break;
                }
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    // ==========================================
    // 4. QUẢN LÝ VẬT PHẨM & MẪU ITEM (CRUD)
    // ==========================================
    private static class ItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            JSONArray list = new JSONArray();
            try {
                synchronized (Manager.ITEM_TEMPLATES) {
                    for (ItemTemplate temp : Manager.ITEM_TEMPLATES) {
                        if (temp != null && temp.name != null && !temp.name.startsWith("Unknown #")) {
                            JSONObject it = new JSONObject();
                            it.put("id", temp.id);
                            it.put("name", temp.name);
                            it.put("type", temp.type);
                            it.put("gender", temp.gender);
                            it.put("iconId", temp.iconID);
                            it.put("part", temp.part);
                            it.put("isUpToUp", temp.isUpToUp);
                            it.put("powerRequire", temp.strRequire);
                            it.put("description", temp.description != null ? temp.description : "");
                            JSONArray opts = new JSONArray();
                            if (temp.itemOptions != null) {
                                for (ItemOption io : temp.itemOptions) {
                                    if (io != null && io.optionTemplate != null) {
                                        JSONObject op = new JSONObject();
                                        op.put("id", io.optionTemplate.id);
                                        op.put("param", io.param);
                                        op.put("name", io.getOptionString());
                                        opts.put(op);
                                    }
                                }
                            }
                            it.put("options", opts);
                            list.put(it);
                        }
                    }
                }
                res.put("status", "success");
                res.put("data", list);
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class ItemOptionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            JSONArray list = new JSONArray();
            try {
                synchronized (Manager.ITEM_OPTION_TEMPLATES) {
                    for (ItemOptionTemplate iot : Manager.ITEM_OPTION_TEMPLATES) {
                        if (iot != null) {
                            JSONObject obj = new JSONObject();
                            obj.put("id", iot.id);
                            obj.put("name", iot.name != null ? iot.name : ("Option #" + iot.id));
                            list.put(obj);
                        }
                    }
                }
                res.put("status", "success");
                res.put("data", list);
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class BuffItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);

                String playerName = req.optString("playerName", "").trim();
                short tempId = (short) req.optInt("itemId", -1);
                int quantity = req.optInt("quantity", 1);
                if (quantity <= 0) quantity = 1;

                Player target = Client.gI().getPlayer(playerName);
                if (target == null) {
                    res.put("status", "error");
                    res.put("message", "Người chơi '" + playerName + "' không online!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                if (InventoryService.gI().getCountEmptyBag(target) <= 0) {
                    res.put("status", "error");
                    res.put("message", "Hành trang của " + target.name + " đã đầy!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                Item item = ItemService.gI().createNewItem(tempId, quantity);
                if (req.has("options")) {
                    JSONArray optArr = req.getJSONArray("options");
                    for (int i = 0; i < optArr.length(); i++) {
                        JSONObject optObj = optArr.getJSONObject(i);
                        int optId = optObj.getInt("id");
                        int param = optObj.optInt("param", 0);
                        item.itemOptions.add(new ItemOption(optId, param));
                    }
                }

                InventoryService.gI().addItemBag(target, item, 99);
                InventoryService.gI().sendItemBags(target);
                Service.getInstance().sendThongBao(target, "Bạn vừa nhận được " + item.template.name + " x" + quantity + " từ Admin!");

                res.put("status", "success");
                res.put("message", "Buff thành công " + item.template.name + " x" + quantity + " cho " + target.name + "!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class RemoveItemBagHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                String playerName = req.optString("playerName", "").trim();
                short tempId = (short) req.optInt("itemId", -1);
                int quantity = req.optInt("quantity", 1);

                Player target = Client.gI().getPlayer(playerName);
                if (target == null) {
                    res.put("status", "error");
                    res.put("message", "Người chơi '" + playerName + "' không online!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                Item inBag = InventoryService.gI().findItemBagByTemp(target, tempId);
                if (inBag == null) {
                    res.put("status", "error");
                    res.put("message", "Người chơi không có vật phẩm này trong hành trang!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                InventoryService.gI().subQuantityItemsBag(target, inBag, quantity);
                InventoryService.gI().sendItemBags(target);
                Service.getInstance().sendThongBao(target, "Admin đã thu hồi " + inBag.template.name + " x" + quantity + " khỏi túi đồ của bạn.");

                res.put("status", "success");
                res.put("message", "Đã thu hồi " + inBag.template.name + " thành công khỏi túi đồ của " + target.name + "!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    // CRUD ITEM TEMPLATE IN DATABASE
    private static class SaveItemTemplateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);

                boolean isEdit = req.optBoolean("isEdit", false);
                int id = req.optInt("id", -1);
                String name = req.optString("name", "").trim();
                byte type = (byte) req.optInt("type", 0);
                byte gender = (byte) req.optInt("gender", 3);
                short icon = (short) req.optInt("iconId", 0);
                short part = (short) req.optInt("part", -1);
                String desc = req.optString("description", "").trim();
                boolean isUpToUp = req.optBoolean("isUpToUp", false);
                int strRequire = req.optInt("powerRequire", 0);

                if (name.isEmpty()) {
                    res.put("status", "error");
                    res.put("message", "Tên vật phẩm không được để trống!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                JSONArray optionsArray = req.optJSONArray("options");
                List<ItemOption> optList = new ArrayList<>();
                if (optionsArray != null) {
                    for (int i = 0; i < optionsArray.length(); i++) {
                        JSONObject op = optionsArray.getJSONObject(i);
                        int optId = op.getInt("id");
                        int paramVal = op.getInt("param");
                        optList.add(new ItemOption(optId, paramVal));
                    }
                }

                try (Connection con = DBService.gI().getConnection()) {
                    if (!isEdit) {
                        // Insert new
                        if (id <= 0) {
                            try (Statement st = con.createStatement();
                                 ResultSet rs = st.executeQuery("SELECT MAX(id) AS max_id FROM item_template")) {
                                if (rs.next()) {
                                    id = rs.getInt("max_id") + 1;
                                } else {
                                    id = 1;
                                }
                            }
                        }
                        String sql = "INSERT INTO item_template (id, type, gender, name, description, icon_id, part, is_up_to_up, power_require) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement ps = con.prepareStatement(sql)) {
                            ps.setInt(1, id);
                            ps.setByte(2, type);
                            ps.setByte(3, gender);
                            ps.setString(4, name);
                            ps.setString(5, desc);
                            ps.setShort(6, icon);
                            ps.setShort(7, part);
                            ps.setBoolean(8, isUpToUp);
                            ps.setInt(9, strRequire);
                            ps.executeUpdate();
                        }
                    } else {
                        // Update
                        String sql = "UPDATE item_template SET type=?, gender=?, name=?, description=?, icon_id=?, part=?, is_up_to_up=?, power_require=? WHERE id=?";
                        try (PreparedStatement ps = con.prepareStatement(sql)) {
                            ps.setByte(1, type);
                            ps.setByte(2, gender);
                            ps.setString(3, name);
                            ps.setString(4, desc);
                            ps.setShort(5, icon);
                            ps.setShort(6, part);
                            ps.setBoolean(7, isUpToUp);
                            ps.setInt(8, strRequire);
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
                        Log.error(WebAdminServer.class, exShop);
                    }
                }

                // Hot reload into memory
                AdminPanelFrame.reloadItemTemplatesFromDB();

                res.put("status", "success");
                res.put("id", id);
                res.put("message", (isEdit ? "Cập nhật" : "Thêm mới") + " Item Template (ID: " + id + ") vào CSDL và đồng bộ bộ nhớ thành công!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class DeleteItemTemplateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                int id = req.optInt("id", -1);

                if (id <= 0) {
                    res.put("status", "error");
                    res.put("message", "ID vật phẩm không hợp lệ!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                try (Connection con = DBService.gI().getConnection();
                     PreparedStatement ps = con.prepareStatement("DELETE FROM item_template WHERE id = ?")) {
                    ps.setInt(1, id);
                    int affected = ps.executeUpdate();
                    if (affected > 0) {
                        try (PreparedStatement psDelOpt = con.prepareStatement("DELETE FROM item_template_option WHERE template_id = ?")) {
                            psDelOpt.setInt(1, id);
                            psDelOpt.executeUpdate();
                        } catch (Exception ignored) {}
                        AdminPanelFrame.reloadItemTemplatesFromDB();
                        res.put("status", "success");
                        res.put("message", "Đã xóa Item Template ID " + id + " khỏi CSDL và tải lại bộ nhớ!");
                    } else {
                        res.put("status", "error");
                        res.put("message", "Không tìm thấy Item Template ID " + id + " để xóa!");
                    }
                }
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    // ==========================================
    // 5. QUẢN LÝ BOSS
    // ==========================================
    private class BossesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            JSONArray list = new JSONArray();
            try {
                List<Boss> bosses = new ArrayList<>(BossManager.BOSSES_IN_GAME);
                for (Boss boss : bosses) {
                    if (boss != null && boss.nPoint != null) {
                        JSONObject bObj = new JSONObject();
                        bObj.put("id", boss.id);
                        bObj.put("name", boss.name);
                        bObj.put("mapName", (boss.zone != null && boss.zone.map != null) ? boss.zone.map.mapName : "Chưa vào map");
                        bObj.put("zoneId", (boss.zone != null) ? boss.zone.zoneId : -1);
                        bObj.put("hp", boss.nPoint.hp);
                        bObj.put("hpFmt", numFmt.format(boss.nPoint.hp));
                        bObj.put("hpMax", boss.nPoint.hpMax);
                        bObj.put("hpMaxFmt", numFmt.format(boss.nPoint.hpMax));
                        bObj.put("dame", boss.nPoint.dame);
                        bObj.put("dameFmt", numFmt.format(boss.nPoint.dame));
                        list.put(bObj);
                    }
                }
                res.put("status", "success");
                res.put("data", list);
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class SpawnBossHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                int bossId = req.optInt("bossId", -1);

                Boss boss = BossFactory.createBoss(bossId);
                if (boss != null) {
                    BossManager.gI().addBoss(boss);
                    res.put("status", "success");
                    res.put("message", "Triệu hồi Boss " + boss.name + " (ID: " + bossId + ") thành công!");
                } else {
                    res.put("status", "error");
                    res.put("message", "Không thể tạo Boss với ID " + bossId + "!");
                }
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class RemoveBossHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                long bossId = req.optLong("bossId", -1);

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
                    res.put("status", "success");
                    res.put("message", "Đã thu hồi Boss " + target.name + " khỏi bản đồ!");
                } else {
                    res.put("status", "error");
                    res.put("message", "Không tìm thấy Boss ID " + bossId + " trong game!");
                }
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    // ==========================================
    // 6. QUẢN LÝ TÀI KHOẢN (ACCOUNTS)
    // ==========================================
    private static class AccountsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            JSONArray list = new JSONArray();
            try {
                String query = exchange.getRequestURI().getQuery();
                String keyword = "";
                if (query != null && query.contains("search=")) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("search=")) {
                            keyword = java.net.URLDecoder.decode(param.substring(7), StandardCharsets.UTF_8).trim();
                            break;
                        }
                    }
                }

                String sql = keyword.isEmpty()
                    ? "SELECT id, username, password, active, ban, is_admin, thoi_vang, last_time_login FROM account ORDER BY id DESC LIMIT 100"
                    : "SELECT id, username, password, active, ban, is_admin, thoi_vang, last_time_login FROM account WHERE username LIKE ? OR id = ? LIMIT 50";

                try (Connection con = DBService.gI().getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    if (!keyword.isEmpty()) {
                        ps.setString(1, "%" + keyword + "%");
                        int idVal = -1;
                        try { idVal = Integer.parseInt(keyword); } catch (Exception ignored) {}
                        ps.setInt(2, idVal);
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject a = new JSONObject();
                            a.put("id", rs.getInt("id"));
                            a.put("username", rs.getString("username"));
                            a.put("password", rs.getString("password"));
                            a.put("active", rs.getInt("active"));
                            a.put("ban", rs.getInt("ban"));
                            a.put("isAdmin", rs.getInt("is_admin"));
                            a.put("thoiVang", rs.getInt("thoi_vang"));
                            a.put("lastLogin", rs.getTimestamp("last_time_login") != null ? rs.getTimestamp("last_time_login").toString() : "N/A");
                            list.put(a);
                        }
                    }
                }
                res.put("status", "success");
                res.put("data", list);
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class AccountActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);

                int accountId = req.optInt("accountId", -1);
                String action = req.optString("action", "");

                if (accountId <= 0) {
                    res.put("status", "error");
                    res.put("message", "ID tài khoản không hợp lệ!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                try (Connection con = DBService.gI().getConnection()) {
                    switch (action) {
                        case "toggle_active": {
                            int newVal = req.optInt("value", 1);
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET active = ? WHERE id = ?")) {
                                ps.setInt(1, newVal);
                                ps.setInt(2, accountId);
                                ps.executeUpdate();
                            }
                            res.put("status", "success");
                            res.put("message", "Đã cập nhật trạng thái kích hoạt tài khoản ID " + accountId + " -> " + newVal);
                            break;
                        }
                        case "toggle_ban": {
                            int newVal = req.optInt("value", 1);
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET ban = ? WHERE id = ?")) {
                                ps.setInt(1, newVal);
                                ps.setInt(2, accountId);
                                ps.executeUpdate();
                            }
                            res.put("status", "success");
                            res.put("message", (newVal == 1 ? "Đã khóa (Ban)" : "Đã mở khóa (Unban)") + " tài khoản ID " + accountId + "!");
                            break;
                        }
                        case "toggle_admin": {
                            int newVal = req.optInt("value", 1);
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET is_admin = ? WHERE id = ?")) {
                                ps.setInt(1, newVal);
                                ps.setInt(2, accountId);
                                ps.executeUpdate();
                            }
                            res.put("status", "success");
                            res.put("message", (newVal == 1 ? "Đã cấp quyền Admin" : "Đã hủy quyền Admin") + " cho tài khoản ID " + accountId + "!");
                            break;
                        }
                        case "change_password": {
                            String newPass = req.optString("newPassword", "").trim();
                            if (newPass.isEmpty()) {
                                res.put("status", "error");
                                res.put("message", "Mật khẩu mới không được để trống!");
                            } else {
                                try (PreparedStatement ps = con.prepareStatement("UPDATE account SET password = ? WHERE id = ?")) {
                                    ps.setString(1, newPass);
                                    ps.setInt(2, accountId);
                                    ps.executeUpdate();
                                }
                                res.put("status", "success");
                                res.put("message", "Đổi mật khẩu cho tài khoản ID " + accountId + " thành công!");
                            }
                            break;
                        }
                        case "add_gold_bar": {
                            int amount = req.optInt("amount", 0);
                            try (PreparedStatement ps = con.prepareStatement("UPDATE account SET thoi_vang = thoi_vang + ? WHERE id = ?")) {
                                ps.setInt(1, amount);
                                ps.setInt(2, accountId);
                                ps.executeUpdate();
                            }
                            res.put("status", "success");
                            res.put("message", "Cộng " + amount + " Thỏi Vàng cho tài khoản ID " + accountId + " thành công!");
                            break;
                        }
                        default:
                            res.put("status", "error");
                            res.put("message", "Hành động tài khoản không hợp lệ!");
                            break;
                    }
                }
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    // ==========================================
    // 7. QUẢN LÝ GIFTCODE (CHỌN ĐỒ TRỰC QUAN & CRUD)
    // ==========================================
    private class GiftcodesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            JSONArray list = new JSONArray();
            try {
                String sql = "SELECT id, code, type, gold, gem, ruby, items, active, created_at FROM gift_codes ORDER BY id DESC LIMIT 100";
                try (Connection con = DBService.gI().getConnection();
                     Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        JSONObject g = new JSONObject();
                        g.put("id", rs.getInt("id"));
                        g.put("code", rs.getString("code"));
                        g.put("type", rs.getInt("type"));
                        g.put("gold", rs.getInt("gold"));
                        g.put("goldFmt", numFmt.format(rs.getInt("gold")));
                        g.put("gem", rs.getInt("gem"));
                        g.put("gemFmt", numFmt.format(rs.getInt("gem")));
                        g.put("ruby", rs.getInt("ruby"));
                        g.put("rubyFmt", numFmt.format(rs.getInt("ruby")));
                        g.put("active", rs.getInt("active"));
                        g.put("createdAt", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "N/A");

                        String itemsJson = rs.getString("items");
                        g.put("itemsRaw", itemsJson != null ? itemsJson : "[]");

                        int count = 0;
                        StringBuilder summary = new StringBuilder();
                        if (itemsJson != null && !itemsJson.trim().isEmpty()) {
                            try {
                                JSONArray arr = new JSONArray(itemsJson);
                                count = arr.length();
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject it = arr.getJSONObject(i);
                                    int tempId = it.getInt("id");
                                    int qty = it.getInt("quantity");
                                    ItemTemplate tmpl = ItemService.gI().getTemplate(tempId);
                                    String name = (tmpl != null) ? tmpl.name : ("Item #" + tempId);
                                    if (i > 0) summary.append(", ");
                                    summary.append(name).append(" (x").append(qty).append(")");
                                }
                            } catch (Exception ignored) {
                                summary.append(itemsJson);
                            }
                        }
                        g.put("itemCount", count);
                        g.put("itemSummary", summary.length() > 0 ? summary.toString() : "(Chỉ tiền)");
                        list.put(g);
                    }
                }
                res.put("status", "success");
                res.put("data", list);
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class SaveGiftcodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);

                int editId = req.optInt("id", -1);
                String code = req.optString("code", "").trim();
                int type = req.optInt("type", 1);
                int gold = req.optInt("gold", 0);
                int gem = req.optInt("gem", 0);
                int ruby = req.optInt("ruby", 0);
                int countLeft = req.optInt("countLeft", 9999);
                int active = req.optInt("active", 0);
                String itemsJson = req.optString("items", "[]");

                if (code.isEmpty()) {
                    res.put("status", "error");
                    res.put("message", "Mã GiftCode không được để trống!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                try (Connection con = DBService.gI().getConnection()) {
                    // 1. Sync to gift_codes
                    if (editId <= 0) {
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

                    // 2. Sync to legacy giftcode table
                    try {
                        String sqlLegacy = "INSERT INTO giftcode (code, count_left, detail) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE count_left=?, detail=?";
                        try (PreparedStatement ps = con.prepareStatement(sqlLegacy)) {
                            ps.setString(1, code);
                            ps.setInt(2, countLeft);
                            ps.setString(3, itemsJson);
                            ps.setInt(4, countLeft);
                            ps.setString(5, itemsJson);
                            ps.executeUpdate();
                        }
                    } catch (Exception ignored) {
                    }
                }

                res.put("status", "success");
                res.put("message", "Đã lưu GiftCode '" + code + "' thành công vào cả 2 bảng CSDL!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class DeleteGiftcodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                int id = req.optInt("id", -1);
                String code = req.optString("code", "").trim();

                try (Connection con = DBService.gI().getConnection()) {
                    if (id > 0) {
                        try (PreparedStatement ps = con.prepareStatement("DELETE FROM gift_codes WHERE id = ?")) {
                            ps.setInt(1, id);
                            ps.executeUpdate();
                        }
                    }
                    if (!code.isEmpty()) {
                        try (PreparedStatement ps = con.prepareStatement("DELETE FROM giftcode WHERE code = ?")) {
                            ps.setString(1, code);
                            ps.executeUpdate();
                        }
                    }
                }
                res.put("status", "success");
                res.put("message", "Đã xóa GiftCode '" + code + "' thành công!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class ReloadShopHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try (Connection con = DBService.gI().getConnection()) {
                Manager.SHOPS = ShopDAO.getShops(con);
                res.put("status", "success");
                res.put("message", "Tải lại CSDL Cửa Hàng (Shop) vào bộ nhớ thành công!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class ShopsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                if (Manager.SHOPS == null) {
                    try (Connection con = DBService.gI().getConnection()) {
                        Manager.SHOPS = ShopDAO.getShops(con);
                    }
                }
                JSONArray arr = new JSONArray();
                if (Manager.SHOPS != null) {
                    List<Shop> sortedShops = new ArrayList<>(Manager.SHOPS);
                    sortedShops.sort((s1, s2) -> {
                        String n1 = AdminPanelFrame.getNpcName(s1.npcId);
                        String n2 = AdminPanelFrame.getNpcName(s2.npcId);
                        return n1.compareToIgnoreCase(n2);
                    });
                    for (Shop s : sortedShops) {
                        if (s != null) {
                            JSONObject sObj = new JSONObject();
                            sObj.put("id", s.id);
                            sObj.put("npcId", s.npcId);
                            sObj.put("npcName", AdminPanelFrame.getNpcName(s.npcId));
                            sObj.put("shopOrder", s.shopOrder);

                            JSONArray tArr = new JSONArray();
                            if (s.tabShops != null) {
                                for (TabShop t : s.tabShops) {
                                    if (t != null) {
                                        JSONObject tObj = new JSONObject();
                                        tObj.put("id", t.id);
                                        tObj.put("name", t.name != null ? t.name.replace("\n", " ") : ("Tab " + t.id));
                                        tArr.put(tObj);
                                    }
                                }
                            }
                            sObj.put("tabs", tArr);
                            arr.put(sObj);
                        }
                    }
                }
                res.put("status", "success");
                res.put("shops", arr);
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class ShopItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                String query = exchange.getRequestURI().getQuery();
                int tabId = -1;
                if (query != null && query.contains("tabId=")) {
                    for (String part : query.split("&")) {
                        if (part.startsWith("tabId=")) {
                            tabId = Integer.parseInt(part.substring(6).trim());
                            break;
                        }
                    }
                }
                if (tabId <= 0) {
                    res.put("status", "error");
                    res.put("message", "Thiếu hoặc sai tham số tabId");
                    sendJsonResponse(exchange, 400, res);
                    return;
                }

                JSONArray itemsArr = new JSONArray();
                try (Connection con = DBService.gI().getConnection();
                     PreparedStatement ps = con.prepareStatement(
                        "SELECT s.id, s.tab_id, s.temp_id, it.name, it.icon_id, s.gold, s.gem, s.item_exchange, s.quantity_exchange, s.is_sell, s.is_new "
                        + "FROM item_shop s LEFT JOIN item_template it ON s.temp_id = it.id WHERE s.tab_id = ? ORDER BY s.id DESC")) {
                    ps.setInt(1, tabId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject itemObj = new JSONObject();
                            int shopItemId = rs.getInt("id");
                            short tempId = rs.getShort("temp_id");
                            String name = rs.getString("name");
                            if (name == null) name = "Item #" + tempId;
                            int iconId = rs.getInt("icon_id");
                            int gold = rs.getInt("gold");
                            int gem = rs.getInt("gem");
                            int exId = rs.getInt("item_exchange");
                            int exQty = rs.getInt("quantity_exchange");
                            boolean isSell = rs.getBoolean("is_sell");
                            boolean isNew = rs.getBoolean("is_new");

                            String exName = "Không";
                            if (exId != -1) {
                                ItemTemplate exTmpl = ItemService.gI().getTemplate(exId);
                                exName = (exTmpl != null) ? exTmpl.name : ("#" + exId);
                            }

                            // Options
                            JSONArray optArr = new JSONArray();
                            try (PreparedStatement psOpt = con.prepareStatement("SELECT option_id, param FROM item_shop_option WHERE item_shop_id = ?")) {
                                psOpt.setInt(1, shopItemId);
                                try (ResultSet rsOpt = psOpt.executeQuery()) {
                                    while (rsOpt.next()) {
                                        int oId = rsOpt.getInt("option_id");
                                        int oParam = rsOpt.getInt("param");
                                        ItemOption io = new ItemOption(oId, oParam);
                                        JSONObject oObj = new JSONObject();
                                        oObj.put("id", oId);
                                        oObj.put("param", oParam);
                                        oObj.put("text", io.getOptionString());
                                        optArr.put(oObj);
                                    }
                                }
                            }

                            itemObj.put("id", shopItemId);
                            itemObj.put("tabId", tabId);
                            itemObj.put("tempId", tempId);
                            itemObj.put("name", name);
                            itemObj.put("iconId", iconId);
                            itemObj.put("gold", gold);
                            itemObj.put("gem", gem);
                            itemObj.put("itemExchange", exId);
                            itemObj.put("exchangeName", exName);
                            itemObj.put("quantityExchange", exQty);
                            itemObj.put("isSell", isSell);
                            itemObj.put("isNew", isNew);
                            itemObj.put("options", optArr);

                            itemsArr.put(itemObj);
                        }
                    }
                }
                res.put("status", "success");
                res.put("items", itemsArr);
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class SaveShopItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, new JSONObject().put("status", "error").put("message", "Method Not Allowed"));
                return;
            }

            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);

                int id = req.optInt("id", -1);
                int tabId = req.getInt("tabId");
                short tempId = (short) req.getInt("tempId");
                int gold = req.optInt("gold", 0);
                int gem = req.optInt("gem", 0);
                int exId = req.optInt("itemExchange", -1);
                int exQty = req.optInt("quantityExchange", 0);
                boolean isSell = req.optBoolean("isSell", true);
                boolean isNew = req.optBoolean("isNew", false);
                JSONArray options = req.optJSONArray("options");

                ItemTemplate tmpl = ItemService.gI().getTemplate(tempId);
                if (tmpl == null) {
                    res.put("status", "error");
                    res.put("message", "Mã Template #" + tempId + " không tồn tại!");
                    sendJsonResponse(exchange, 400, res);
                    return;
                }

                int savedId = id;
                try (Connection con = DBService.gI().getConnection()) {
                    if (id <= 0) {
                        String sql = "INSERT INTO item_shop (tab_id, temp_id, gold, gem, is_new, is_sell, item_exchange, quantity_exchange) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            ps.setInt(1, tabId);
                            ps.setShort(2, tempId);
                            ps.setInt(3, gold);
                            ps.setInt(4, gem);
                            ps.setBoolean(5, isNew);
                            ps.setBoolean(6, isSell);
                            ps.setInt(7, exId);
                            ps.setInt(8, exQty);
                            ps.executeUpdate();
                            try (ResultSet rs = ps.getGeneratedKeys()) {
                                if (rs.next()) savedId = rs.getInt(1);
                            }
                        }
                    } else {
                        String sql = "UPDATE item_shop SET temp_id=?, gold=?, gem=?, is_new=?, is_sell=?, item_exchange=?, quantity_exchange=? WHERE id=?";
                        try (PreparedStatement ps = con.prepareStatement(sql)) {
                            ps.setShort(1, tempId);
                            ps.setInt(2, gold);
                            ps.setInt(3, gem);
                            ps.setBoolean(4, isNew);
                            ps.setBoolean(5, isSell);
                            ps.setInt(6, exId);
                            ps.setInt(7, exQty);
                            ps.setInt(8, id);
                            ps.executeUpdate();
                        }
                        try (PreparedStatement psDel = con.prepareStatement("DELETE FROM item_shop_option WHERE item_shop_id = ?")) {
                            psDel.setInt(1, id);
                            psDel.executeUpdate();
                        }
                    }

                    // Save Options
                    if (savedId > 0 && options != null && options.length() > 0) {
                        try (PreparedStatement psIns = con.prepareStatement("INSERT INTO item_shop_option (item_shop_id, option_id, param) VALUES (?, ?, ?)")) {
                            for (int i = 0; i < options.length(); i++) {
                                JSONObject opt = options.getJSONObject(i);
                                int oId = opt.getInt("id");
                                int oParam = opt.getInt("param");
                                psIns.setInt(1, savedId);
                                psIns.setInt(2, oId);
                                psIns.setInt(3, oParam);
                                psIns.addBatch();
                            }
                            psIns.executeBatch();
                        }
                    }

                    // Reload shop cache
                    Manager.SHOPS = ShopDAO.getShops(con);
                }

                res.put("status", "success");
                res.put("message", "Đã lưu vật phẩm [" + tmpl.name + "] vào Shop thành công!");
                res.put("shopItemId", savedId);
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class DeleteShopItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, new JSONObject().put("status", "error").put("message", "Method Not Allowed"));
                return;
            }

            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                int id = req.optInt("id", -1);
                if (id <= 0) {
                    res.put("status", "error");
                    res.put("message", "Thiếu hoặc sai ID vật phẩm cần xóa");
                    sendJsonResponse(exchange, 400, res);
                    return;
                }

                try (Connection con = DBService.gI().getConnection()) {
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_shop WHERE id = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM item_shop_option WHERE item_shop_id = ?")) {
                        ps2.setInt(1, id);
                        ps2.executeUpdate();
                    }
                    Manager.SHOPS = ShopDAO.getShops(con);
                }

                res.put("status", "success");
                res.put("message", "Đã xóa vật phẩm khỏi Cửa Hàng thành công!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class CreateShopHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, new JSONObject().put("status", "error").put("message", "Method Not Allowed"));
                return;
            }
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                int npcId = req.getInt("npcId");
                int shopOrder = req.optInt("shopOrder", 0);
                String tabName = req.optString("tabName", "Cửa Hàng").trim();
                if (tabName.isEmpty()) tabName = "Cửa Hàng";

                int newShopId = -1;
                try (Connection con = DBService.gI().getConnection()) {
                    String sql = "INSERT INTO shop (npc_id, shop_order) VALUES (?, ?)";
                    try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, npcId);
                        ps.setInt(2, shopOrder);
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) newShopId = rs.getInt(1);
                        }
                    }
                    if (newShopId > 0) {
                        try (PreparedStatement psTab = con.prepareStatement("INSERT INTO tab_shop (shop_id, name) VALUES (?, ?)")) {
                            psTab.setInt(1, newShopId);
                            psTab.setString(2, tabName);
                            psTab.executeUpdate();
                        }
                    }
                    Manager.SHOPS = ShopDAO.getShops(con);
                }
                res.put("status", "success");
                res.put("shopId", newShopId);
                res.put("message", "Đã tạo Cửa Hàng #" + newShopId + " cho NPC " + AdminPanelFrame.getNpcName(npcId) + " thành công!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class SaveShopTabHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, new JSONObject().put("status", "error").put("message", "Method Not Allowed"));
                return;
            }
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                int shopId = req.getInt("shopId");
                String name = req.getString("name").trim();
                if (name.isEmpty()) {
                    res.put("status", "error");
                    res.put("message", "Tên Tab không được để trống!");
                    sendJsonResponse(exchange, 400, res);
                    return;
                }

                try (Connection con = DBService.gI().getConnection();
                     PreparedStatement ps = con.prepareStatement("INSERT INTO tab_shop (shop_id, name) VALUES (?, ?)")) {
                    ps.setInt(1, shopId);
                    ps.setString(2, name);
                    ps.executeUpdate();
                    Manager.SHOPS = ShopDAO.getShops(con);
                }
                res.put("status", "success");
                res.put("message", "Đã tạo Tab '" + name + "' cho Shop #" + shopId + " thành công!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class EventStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            JSONObject res = new JSONObject();
            try {
                res.put("status", "success");
                res.put("event", (int) Manager.EVENT_SEVER);
                res.put("eventName", AdminPanelFrame.getEventName(Manager.EVENT_SEVER));
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    private static class SetEventHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, new JSONObject().put("status", "error").put("message", "Method Not Allowed"));
                return;
            }
            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                int eventId = req.getInt("event");
                boolean broadcast = req.optBoolean("broadcast", true);

                AdminPanelFrame.setServerEvent(eventId, broadcast);

                res.put("status", "success");
                res.put("event", (int) Manager.EVENT_SEVER);
                res.put("eventName", AdminPanelFrame.getEventName(Manager.EVENT_SEVER));
                res.put("message", "Đã chuyển đổi sự kiện máy chủ thành công sang: " + AdminPanelFrame.getEventName(Manager.EVENT_SEVER));
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    // ==========================================
    // 8. QUẢN LÝ ICON (TÀI NGUYÊN GAME NRO)
    // ==========================================

    /**
     * Phục vụ dữ liệu ảnh Icon cho Web:
     * GET /api/icon?id=457 hoặc /api/icon/457 hoặc /api/icon/457.png
     */
    private static class IconHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            int iconId = -1;

            if (query != null && query.contains("id=")) {
                for (String p : query.split("&")) {
                    if (p.startsWith("id=")) {
                        try {
                            iconId = Integer.parseInt(p.substring(3).trim());
                        } catch (Exception ignored) {}
                        break;
                    }
                }
            }

            if (iconId < 0) {
                String[] parts = path.split("/");
                if (parts.length > 0) {
                    String last = parts[parts.length - 1];
                    if (last.toLowerCase().endsWith(".png")) {
                        last = last.substring(0, last.length() - 4);
                    }
                    try {
                        iconId = Integer.parseInt(last);
                    } catch (Exception ignored) {}
                }
            }

            if (iconId < 0) {
                sendResponse(exchange, 400, "text/plain; charset=UTF-8", "Thiếu hoặc sai mã Icon ID");
                return;
            }

            File iconFile = null;
            int[] zooms = {2, 1, 3, 4};
            for (int z : zooms) {
                File f = new File("resources/normal/image/" + z + "/icon/" + iconId + ".png");
                if (f.exists() && f.isFile()) {
                    iconFile = f;
                    break;
                }
            }

            if (iconFile != null && iconFile.exists()) {
                byte[] bytes = Files.readAllBytes(iconFile.toPath());
                applyCorsAndIframeHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "image/png");
                exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                sendResponse(exchange, 404, "text/plain; charset=UTF-8", "Icon không tồn tại");
            }
        }
    }

    /**
     * Liệt kê danh sách Icon (Phân trang và tìm kiếm theo ID)
     * GET /api/icons?search=45&page=1&limit=60
     */
    private static class IconsListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            JSONObject res = new JSONObject();
            JSONArray list = new JSONArray();

            String query = exchange.getRequestURI().getQuery();
            String search = "";
            int page = 1;
            int limit = 60;

            if (query != null) {
                for (String p : query.split("&")) {
                    if (p.startsWith("search=")) {
                        search = URLDecoder.decode(p.substring(7), StandardCharsets.UTF_8).trim();
                    } else if (p.startsWith("page=")) {
                        try { page = Math.max(1, Integer.parseInt(p.substring(5))); } catch (Exception ignored) {}
                    } else if (p.startsWith("limit=")) {
                        try { limit = Math.max(1, Math.min(200, Integer.parseInt(p.substring(6)))); } catch (Exception ignored) {}
                    }
                }
            }

            File iconDir = new File("resources/normal/image/1/icon");
            List<Integer> matched = new ArrayList<>();
            int maxId = 0;

            if (iconDir.exists() && iconDir.isDirectory()) {
                File[] files = iconDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                if (files != null) {
                    for (File f : files) {
                        String name = f.getName();
                        String idStr = name.substring(0, name.length() - 4);
                        try {
                            int id = Integer.parseInt(idStr);
                            if (id > maxId) maxId = id;
                            if (search.isEmpty() || idStr.contains(search)) {
                                matched.add(id);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            Collections.sort(matched);

            int total = matched.size();
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, total);

            if (start < total) {
                for (int i = start; i < end; i++) {
                    list.put(matched.get(i));
                }
            }

            res.put("status", "success");
            res.put("page", page);
            res.put("limit", limit);
            res.put("total", total);
            res.put("maxId", maxId);
            res.put("data", list);

            sendJsonResponse(exchange, 200, res);
        }
    }

    /**
     * Thêm mới hoặc thay thế file ảnh Icon (Base64)
     * POST /api/icon/save
     * Body: { id: 1234, imageBase64: "data:image/png;base64,..." }
     */
    private static class SaveIconHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);

                int id = req.optInt("id", -1);
                String base64 = req.optString("imageBase64", "").trim();

                if (base64.contains(",")) {
                    base64 = base64.substring(base64.indexOf(",") + 1);
                }

                if (base64.isEmpty()) {
                    res.put("status", "error");
                    res.put("message", "Dữ liệu hình ảnh trống!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                byte[] imgBytes = Base64.getDecoder().decode(base64);

                // Auto ID if <= 0
                if (id <= 0) {
                    File dir = new File("resources/normal/image/1/icon");
                    int max = 0;
                    File[] fl = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
                    if (fl != null) {
                        for (File f : fl) {
                            try {
                                int v = Integer.parseInt(f.getName().substring(0, f.getName().length() - 4));
                                if (v > max) max = v;
                            } catch (Exception ignored) {}
                        }
                    }
                    id = max + 1;
                }

                // Lưu ảnh vào cả 4 thư mục zoom (1, 2, 3, 4)
                for (int z = 1; z <= 4; z++) {
                    File targetDir = new File("resources/normal/image/" + z + "/icon");
                    if (!targetDir.exists()) targetDir.mkdirs();
                    File targetFile = new File(targetDir, id + ".png");
                    Files.write(targetFile.toPath(), imgBytes);
                }

                // Tải lại smallVersion trong bộ nhớ
                try {
                    if (nro.resources.Resources.getInstance().find(0) != null) {
                        nro.resources.Resources.getInstance().find(0).initSmallVersion();
                    }
                } catch (Exception e) {
                    Log.error(WebAdminServer.class, e);
                }

                res.put("status", "success");
                res.put("id", id);
                res.put("message", "Đã lưu Icon ID " + id + " thành công vào toàn bộ thư mục tài nguyên và cập nhật cache server!");
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }

    /**
     * Xóa Icon khỏi server:
     * POST /api/icon/delete
     * Body: { id: 1234 }
     */
    private static class DeleteIconHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            JSONObject res = new JSONObject();
            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                int id = req.optInt("id", -1);

                if (id < 0) {
                    res.put("status", "error");
                    res.put("message", "Mã Icon ID không hợp lệ!");
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                boolean deleted = false;
                for (int z = 1; z <= 4; z++) {
                    File f = new File("resources/normal/image/" + z + "/icon/" + id + ".png");
                    if (f.exists()) {
                        if (f.delete()) deleted = true;
                    }
                }

                if (deleted) {
                    try {
                        if (nro.resources.Resources.getInstance().find(0) != null) {
                            nro.resources.Resources.getInstance().find(0).initSmallVersion();
                        }
                    } catch (Exception ignored) {}
                    res.put("status", "success");
                    res.put("message", "Đã xóa Icon ID " + id + " thành công!");
                } else {
                    res.put("status", "error");
                    res.put("message", "Không tìm thấy file Icon ID " + id + " để xóa!");
                }
            } catch (Exception ex) {
                res.put("status", "error");
                res.put("message", ex.getMessage());
            }
            sendJsonResponse(exchange, 200, res);
        }
    }
}
