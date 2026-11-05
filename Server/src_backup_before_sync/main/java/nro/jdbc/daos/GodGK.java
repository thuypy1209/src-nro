package nro.jdbc.daos;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.net.InterfaceAddress;
import nro.card.Card;
import nro.card.CollectionBook;
import nro.consts.ConstAchive;
import nro.consts.ConstMap;
import nro.consts.ConstPlayer;
import nro.jdbc.DBService;
import nro.manager.AchiveManager;
import nro.manager.PetFollowManager;
import nro.models.player.PetFollow;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.item.ItemTime;
import nro.models.npc.specialnpc.MabuEgg;
import nro.models.npc.specialnpc.MagicTree;
import nro.models.player.*;
import nro.models.skill.Skill;
import nro.models.task.Achivement;
import nro.models.task.AchivementTemplate;
import nro.models.task.TaskMain;
import nro.server.Client;
import nro.server.Manager;
import nro.server.io.Session;
import nro.server.model.AntiLogin;
import nro.services.*;
import nro.utils.SkillUtil;
import nro.utils.TimeUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author ❤Girlkun75❤
 * @copyright ❤YTB KhanhDTK❤
 */
public class GodGK {
    // ttest da sua

    public static boolean parseBoolSafe(Object obj) {
        if (obj == null) return false;
        String s = String.valueOf(obj).trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    public static int parseIntSafe(Object obj, int defaultVal) {
        if (obj == null) return defaultVal;
        String s = String.valueOf(obj).trim();
        if ("false".equalsIgnoreCase(s) || "null".equalsIgnoreCase(s) || s.isEmpty()) return defaultVal;
        if ("true".equalsIgnoreCase(s)) return 1;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            try {
                return (int) Double.parseDouble(s);
            } catch (Exception ex) {
                return defaultVal;
            }
        }
    }

    public static long parseLongSafe(Object obj, long defaultVal) {
        if (obj == null) return defaultVal;
        String s = String.valueOf(obj).trim();
        if ("false".equalsIgnoreCase(s) || "null".equalsIgnoreCase(s) || s.isEmpty()) return defaultVal;
        if ("true".equalsIgnoreCase(s)) return 1L;
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            try {
                return (long) Double.parseDouble(s);
            } catch (Exception ex) {
                return defaultVal;
            }
        }
    }

    public static boolean login(Session session, AntiLogin al) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = DBService.gI().getConnectionForLogin();
            String query = "select * from account where username = ? and password = ? limit 1";
            ps = conn.prepareStatement(query);
            ps.setString(1, session.uu);
            ps.setString(2, session.pp);
            rs = ps.executeQuery();
            if (rs.next()) {
                session.userId = rs.getInt("account.id");
                Session plInGame = Client.gI().getSession(session);
                if (plInGame != null) {
                    Service.getInstance().sendThongBaoOK(plInGame, "Máy chủ tắt hoặc mất sóng!");
                    Client.gI().kickSession(plInGame);
                    Service.getInstance().sendThongBaoOK(session, "Máy chủ tắt hoặc mất sóng!");
                    return false;
                }

                session.isAdmin = rs.getBoolean("is_admin");
                session.lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                session.actived = rs.getBoolean("active");
                session.goldBar = rs.getInt("thoi_vang");
                session.vndBar = rs.getInt("vnd");
                session.tongnap = rs.getInt("tongnap");
                session.dataReward = rs.getString("reward");
                if (rs.getTimestamp("last_time_login").getTime() > session.lastTimeLogout) {
                    Service.getInstance().sendThongBaoOK(session, "Tài khoản đang đăng nhập máy chủ khác!");
                    return false;
                }
                if (rs.getBoolean("ban")) {
                    Service.getInstance().sendThongBaoOK(session, "Tài khoản đã bị khóa do vi phạm điều khoản!");
                } else {
                    long lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                    int secondsPass = (int) ((System.currentTimeMillis() - lastTimeLogout) / 1000);
                    if (secondsPass < Manager.SECOND_WAIT_LOGIN && !session.isAdmin) {
                        Service.getInstance().sendThongBaoOK(session, "Vui lòng chờ "
                                + (Manager.SECOND_WAIT_LOGIN - secondsPass) + " giây để đăng nhập lại.");
                    }
                }
                al.reset();
                return true;
            } else {
                Service.getInstance().sendThongBaoOK(session, "Thông tin tài khoản hoặc mật khẩu không chính xác");
                al.wrong();
                // Anti login
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                }
            }
        }
        return false;
    }

    public static Player loadPlayer(Session session) {
        try {
            Connection connection = DBService.gI().getConnectionForLogin();
            PreparedStatement ps = connection.prepareStatement("select * from player where account_id = ? limit 1");
            ps.setInt(1, session.userId);
            ResultSet rs = ps.executeQuery();
            try {
                if (rs.next()) {
                    int plHp = 200000000;
                    int plMp = 200000000;
                    JSONValue jv = new JSONValue();
                    JSONArray dataArray = null;
                    JSONObject dataObject = null;

                    Player player = new Player();

                    // base info
                    player.id = rs.getInt("id");
                    player.name = rs.getString("name");
                    player.head = rs.getShort("head");
                    player.gender = rs.getByte("gender");
                    player.haveTennisSpaceShip = rs.getBoolean("have_tennis_space_ship");

                    int clanId = rs.getInt("clan_id_sv" + Manager.SERVER);
                    if (clanId != -1) {
                        Clan clan = ClanService.gI().getClanById(clanId);
                        if (clan != null) {
                            for (ClanMember cm : clan.getMembers()) {
                                if (cm.id == player.id) {
                                    clan.addMemberOnline(player);
                                    player.clan = clan;
                                    player.clanMember = cm;
                                    player.setBuff(clan.getBuff());
                                    break;
                                }
                            }
                        }
                    }
                    // diem su kien
                    int evPoint = rs.getInt("event_point");
                    player.event.setEventPoint(evPoint);

                    dataArray = (JSONArray) JSONValue.parse(rs.getString("sk_tet"));
                    int timeBanhTet = Integer.parseInt(dataArray.get(0).toString());
                    int timeBanhChung = Integer.parseInt(dataArray.get(1).toString());
                    boolean isNauBanhTet = Integer.parseInt(dataArray.get(2).toString()) == 1;
                    boolean isNauBanhChung = Integer.parseInt(dataArray.get(3).toString()) == 1;
                    boolean receivedLuckMoney = Integer.parseInt(dataArray.get(4).toString()) == 1;

                    player.event.setTimeCookTetCake(timeBanhTet);
                    player.event.setTimeCookChungCake(timeBanhChung);
                    player.event.setCookingTetCake(isNauBanhTet);
                    player.event.setCookingChungCake(isNauBanhChung);
                    player.event.setReceivedLuckyMoney(receivedLuckMoney);
                    dataArray.clear();

                    // data kim lượng
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("data_inventory"));
                    player.inventory.gold = Long.parseLong(dataArray.get(0).toString());
                    player.inventory.gem = Integer.parseInt(dataArray.get(1).toString());
                    player.inventory.ruby = Integer.parseInt(dataArray.get(2).toString());
                    if (dataArray.size() >= 4) {
                        player.inventory.goldLimit = Long.parseLong(dataArray.get(3).toString());
                    }
                    dataArray.clear();

                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("dhtime"));
                        if (dataArray != null && dataArray.size() >= 6) {
                            player.isTitleUse = parseBoolSafe(dataArray.get(0));
                            player.lastTimeTitle1 = parseLongSafe(dataArray.get(1), 0L);
                            player.IdDanhHieu_1 = parseIntSafe(dataArray.get(2), -1);
                            player.ChiSoHP_1 = parseIntSafe(dataArray.get(3), 0);
                            player.ChiSoKI_1 = parseIntSafe(dataArray.get(4), 0);
                            player.ChiSoSD_1 = parseIntSafe(dataArray.get(5), 0);
                            dataArray.clear();
                        }
                    } catch (Exception e) {}

                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("dhtime2"));
                        if (dataArray != null && dataArray.size() >= 6) {
                            player.isTitleUse2 = parseBoolSafe(dataArray.get(0));
                            player.lastTimeTitle2 = parseLongSafe(dataArray.get(1), 0L);
                            player.IdDanhHieu_2 = parseIntSafe(dataArray.get(2), -1);
                            player.ChiSoHP_2 = parseIntSafe(dataArray.get(3), 0);
                            player.ChiSoKI_2 = parseIntSafe(dataArray.get(4), 0);
                            player.ChiSoSD_2 = parseIntSafe(dataArray.get(5), 0);
                            dataArray.clear();
                        }
                    } catch (Exception e) {}

                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("dhtime3"));
                        if (dataArray != null && dataArray.size() >= 6) {
                            player.isTitleUse3 = parseBoolSafe(dataArray.get(0));
                            player.lastTimeTitle3 = parseLongSafe(dataArray.get(1), 0L);
                            player.IdDanhHieu_3 = parseIntSafe(dataArray.get(2), -1);
                            player.ChiSoHP_3 = parseIntSafe(dataArray.get(3), 0);
                            player.ChiSoKI_3 = parseIntSafe(dataArray.get(4), 0);
                            player.ChiSoSD_3 = parseIntSafe(dataArray.get(5), 0);
                            dataArray.clear();
                        }
                    } catch (Exception e) {}

                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("dhtime4"));
                        if (dataArray != null && dataArray.size() >= 6) {
                            player.isTitleUse4 = parseBoolSafe(dataArray.get(0));
                            player.lastTimeTitle4 = parseLongSafe(dataArray.get(1), 0L);
                            player.IdDanhHieu_4 = parseIntSafe(dataArray.get(2), -1);
                            player.ChiSoHP_4 = parseIntSafe(dataArray.get(3), 0);
                            player.ChiSoKI_4 = parseIntSafe(dataArray.get(4), 0);
                            player.ChiSoSD_4 = parseIntSafe(dataArray.get(5), 0);
                            dataArray.clear();
                        }
                    } catch (Exception e) {}

                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("dhtime5"));
                        if (dataArray != null && dataArray.size() >= 6) {
                            player.isTitleUse5 = parseBoolSafe(dataArray.get(0));
                            player.lastTimeTitle5 = parseLongSafe(dataArray.get(1), 0L);
                            player.IdDanhHieu_5 = parseIntSafe(dataArray.get(2), -1);
                            player.ChiSoHP_5 = parseIntSafe(dataArray.get(3), 0);
                            player.ChiSoKI_5 = parseIntSafe(dataArray.get(4), 0);
                            player.ChiSoSD_5 = parseIntSafe(dataArray.get(5), 0);
                            dataArray.clear();
                        }
                    } catch (Exception e) {}

                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("killWhis"));
                        if (dataArray != null && dataArray.size() >= 3) {
                            player.lastTimeSwapWhis = parseLongSafe(dataArray.get(0), 0L);
                            player.lastTimeKillWhis = parseLongSafe(dataArray.get(1), 0L);
                            player.levelKillWhis = parseIntSafe(dataArray.get(2), 0);
                            dataArray.clear();
                        }
                    } catch (Exception e) {}

                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("MaBaoVe"));
                        if (dataArray != null && dataArray.size() >= 2) {
                            player.isUseMaBaoVe = parseBoolSafe(dataArray.get(0));
                            player.MaBaoVe = parseIntSafe(dataArray.get(1), 0);
                            dataArray.clear();
                        } else {
                            player.isUseMaBaoVe = false;
                            player.MaBaoVe = 0;
                        }
                    } catch (Exception e) {
                        player.isUseMaBaoVe = false;
                        player.MaBaoVe = 0;
                    }

                    player.levelKillWhisDone = rs.getInt("levelKillWhis");

                    player.timeKillWhis = rs.getLong("timeKillWhis");

                    player.event.setDiemTichLuy(session.diemTichNap);

                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("checkNhanQua"));
                        if (dataArray != null) {
                            if (dataArray.size() >= 1) {
                                player.event.luotNhanNgocMienPhi = parseIntSafe(dataArray.get(0), 0);
                            }
                            if (dataArray.size() >= 2) {
                                player.event.luotNhanBuaMienPhi = parseIntSafe(dataArray.get(1), 0);
                            }
                            if (dataArray.size() >= 3) {
                                player.event.luotNhanVipGift = parseIntSafe(dataArray.get(2), 0);
                            }
                            if (dataArray.size() >= 4) {
                                player.event.vipMask = parseIntSafe(dataArray.get(3), 0);
                            }
                            dataArray.clear();
                        }
                    } catch (Exception e) {}

                    player.event.setMocNapDaNhan(rs.getInt("moc_nap"));

                    player.server = session.server;
                    // data tọa độ
                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("data_location"));
                        int x = 300;
                        int y = 336;
                        int mapId = 21 + player.gender;
                        if (dataArray != null && dataArray.size() >= 3) {
                            int v0 = parseIntSafe(dataArray.get(0), 300);
                            int v1 = parseIntSafe(dataArray.get(1), 336);
                            int v2 = parseIntSafe(dataArray.get(2), 21 + player.gender);
                            if (MapService.gI().getMapById(v2) != null) {
                                x = v0;
                                y = v1;
                                mapId = v2;
                            } else if (MapService.gI().getMapById(v0) != null) {
                                mapId = v0;
                                x = v1;
                                y = v2;
                            }
                        }
                        if (MapService.gI().isMapDoanhTrai(mapId) || MapService.gI().isMapBlackBallWar(mapId)
                                || MapService.gI().isMapBanDoKhoBau(mapId) || mapId == 126
                                || mapId == ConstMap.CON_DUONG_RAN_DOC
                                || mapId == ConstMap.CON_DUONG_RAN_DOC_142 || mapId == ConstMap.CON_DUONG_RAN_DOC_143
                                || mapId == ConstMap.HOANG_MAC || MapService.gI().getMapById(mapId) == null) {
                            mapId = player.gender + 21;
                            x = 300;
                            y = 336;
                        } else if (MapService.gI().isMapKhiGas(mapId)) {
                            mapId = 5;
                            x = 106;
                            y = 228;
                        }
                        player.location.x = x;
                        player.location.y = y;
                        player.zone = MapService.gI().getMapCanJoin(player, mapId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (player.zone == null) {
                        player.zone = MapService.gI().getMapCanJoin(player, 21 + player.gender);
                        player.location.x = 300;
                        player.location.y = 336;
                    }
                    if (dataArray != null) dataArray.clear();

                    // data chỉ số
                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("data_point"));
                        if (dataArray != null && dataArray.size() >= 13) {
                            plMp = (int) Math.min(parseLongSafe(dataArray.get(1), 100L), Integer.MAX_VALUE);
                            player.nPoint.mpg = (int) Math.min(parseLongSafe(dataArray.get(2), 100L), Integer.MAX_VALUE);
                            player.nPoint.critg = (byte) parseIntSafe(dataArray.get(3), 0);
                            player.nPoint.limitPower = (byte) parseIntSafe(dataArray.get(4), 0);
                            player.nPoint.stamina = (short) parseIntSafe(dataArray.get(5), 1000);
                            plHp = (int) Math.min(parseLongSafe(dataArray.get(6), 100L), Integer.MAX_VALUE);
                            player.nPoint.defg = parseIntSafe(dataArray.get(7), 0);
                            player.nPoint.tiemNang = parseLongSafe(dataArray.get(8), 0L);
                            player.nPoint.maxStamina = (short) parseIntSafe(dataArray.get(9), 1000);
                            player.nPoint.dameg = parseIntSafe(dataArray.get(10), 10);
                            player.nPoint.power = parseLongSafe(dataArray.get(11), 2000L);
                            player.nPoint.hpg = (int) Math.min(parseLongSafe(dataArray.get(12), 100L), Integer.MAX_VALUE);
                            dataArray.clear();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // data đậu thần
                    dataArray = (JSONArray) jv.parse(rs.getString("data_magic_tree"));
                    boolean isUpgrade = Byte.parseByte(dataArray.get(0).toString()) == 1;
                    long lastTimeUpgrade = Long.parseLong(dataArray.get(1).toString());
                    byte level = Byte.parseByte(dataArray.get(2).toString());
                    long lastTimeHarvest = Long.parseLong(dataArray.get(3).toString());
                    byte currPea = Byte.parseByte(dataArray.get(4).toString());
                    player.magicTree = new MagicTree(player, level, currPea, lastTimeHarvest, isUpgrade,
                            lastTimeUpgrade);
                    dataArray.clear();

                    // data phần thưởng sao đen
                    dataArray = (JSONArray) jv.parse(rs.getString("data_black_ball"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONArray reward = (JSONArray) jv.parse(String.valueOf(dataArray.get(i)));
                        player.rewardBlackBall.timeOutOfDateReward[i] = Long.parseLong(reward.get(0).toString());
                        player.rewardBlackBall.lastTimeGetReward[i] = Long.parseLong(reward.get(1).toString());
                        reward.clear();
                    }
                    dataArray.clear();

                    // data body
                    dataArray = (JSONArray) jv.parse(rs.getString("items_body"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        Item item = null;
                        dataObject = (JSONObject) dataArray.get(i);
                        short tempId = Short.parseShort(String.valueOf(dataObject.get("temp_id")));
                        if (tempId != -1) {
                            item = ItemService.gI().createNewItem(tempId,
                                    Integer.parseInt(String.valueOf(dataObject.get("quantity"))));
                            JSONArray options = (JSONArray) dataObject.get("option");
                            for (int j = 0; j < options.size(); j++) {
                                JSONArray opt = (JSONArray) options.get(j);
                                item.itemOptions.add(new ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                        Integer.parseInt(String.valueOf(opt.get(1)))));
                            }
                            item.createTime = Long.parseLong(String.valueOf(dataObject.get("create_time")));
                            if (ItemService.gI().isOutOfDateTime(item)) {
                                item = ItemService.gI().createItemNull();
                            }
                        } else {
                            item = ItemService.gI().createItemNull();
                        }
                        player.inventory.itemsBody.add(item);
                    }
                    if (player.inventory.itemsBody.size() == 12) {
                        player.inventory.itemsBody.add(ItemService.gI().createItemNull());
                    }
                    dataArray.clear();
                    dataObject.clear();

                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("items_bag"));
                        for (int i = 0; i < dataArray.size(); i++) {
                            Item item = null;
                            dataObject = (JSONObject) dataArray.get(i);
                            short tempId = Short.parseShort(String.valueOf(dataObject.get("temp_id")));
                            if (tempId != -1) {
                                item = ItemService.gI().createNewItem(tempId,
                                        Integer.parseInt(String.valueOf(dataObject.get("quantity"))));
                                JSONArray options = (JSONArray) dataObject.get("option");
                                for (int j = 0; j < options.size(); j++) {
                                    JSONArray opt = (JSONArray) options.get(j);
                                    item.itemOptions.add(new ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                            Integer.parseInt(String.valueOf(opt.get(1)))));
                                }
                                item.createTime = Long.parseLong(String.valueOf(dataObject.get("create_time")));
                                if (ItemService.gI().isOutOfDateTime(item)) {
                                    item = ItemService.gI().createItemNull();
                                }
                            } else {
                                item = ItemService.gI().createItemNull();
                            }
                            player.inventory.itemsBag.add(item);
                        }
                        dataArray.clear();
                        dataObject.clear();
                    } catch (Exception e) {
                        System.out.println("Lỗi hành trang hành trang người chơi");
                        e.printStackTrace();
                    }

                    // data box
                    dataArray = (JSONArray) jv.parse(rs.getString("items_box"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        Item item = null;
                        dataObject = (JSONObject) dataArray.get(i);
                        short tempId = Short.parseShort(String.valueOf(dataObject.get("temp_id")));
                        if (tempId != -1) {
                            item = ItemService.gI().createNewItem(tempId,
                                    Integer.parseInt(String.valueOf(dataObject.get("quantity"))));
                            JSONArray options = (JSONArray) dataObject.get("option");
                            for (int j = 0; j < options.size(); j++) {
                                JSONArray opt = (JSONArray) options.get(j);
                                item.itemOptions.add(new ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                        Integer.parseInt(String.valueOf(opt.get(1)))));
                            }
                            item.createTime = Long.parseLong(String.valueOf(dataObject.get("create_time")));
                            if (ItemService.gI().isOutOfDateTime(item)) {
                                item = ItemService.gI().createItemNull();
                            }
                        } else {
                            item = ItemService.gI().createItemNull();
                        }

                        player.inventory.itemsBox.add(item);
                    }
                    dataArray.clear();
                    dataObject.clear();

                    // data box lucky round
                    dataArray = (JSONArray) jv.parse(rs.getString("items_box_lucky_round"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        Item item = null;
                        dataObject = (JSONObject) dataArray.get(i);
                        short tempId = Short.parseShort(String.valueOf(dataObject.get("temp_id")));
                        if (tempId != -1) {
                            item = ItemService.gI().createNewItem(tempId,
                                    Integer.parseInt(String.valueOf(dataObject.get("quantity"))));
                            JSONArray options = (JSONArray) dataObject.get("option");
                            for (int j = 0; j < options.size(); j++) {
                                JSONArray opt = (JSONArray) options.get(j);
                                item.itemOptions.add(new ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                                        Integer.parseInt(String.valueOf(opt.get(1)))));
                            }
                        } else {
                            item = ItemService.gI().createItemNull();
                        }
                        player.inventory.itemsBoxCrackBall.add(item);
                    }
                    dataArray.clear();
                    dataObject.clear();

                    // data friends
                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("friends"));
                        if (dataArray != null) {
                            for (int i = 0; i < dataArray.size(); i++) {
                                if (dataArray.get(i) instanceof JSONObject) {
                                    dataObject = (JSONObject) dataArray.get(i);
                                    Friend friend = new Friend();
                                    friend.id = parseIntSafe(dataObject.get("id"), 0);
                                    friend.name = String.valueOf(dataObject.get("name"));
                                    friend.head = (short) parseIntSafe(dataObject.get("head"), 0);
                                    friend.body = (short) parseIntSafe(dataObject.get("body"), 0);
                                    friend.leg = (short) parseIntSafe(dataObject.get("leg"), 0);
                                    friend.bag = (byte) parseIntSafe(dataObject.get("bag"), 0);
                                    friend.power = parseLongSafe(dataObject.get("power"), 0L);
                                    player.friends.add(friend);
                                    dataObject.clear();
                                }
                            }
                            dataArray.clear();
                        }
                    } catch (Exception e) {}

                    // data enemies
                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("enemies"));
                        if (dataArray != null) {
                            for (int i = 0; i < dataArray.size(); i++) {
                                if (dataArray.get(i) instanceof JSONObject) {
                                    dataObject = (JSONObject) dataArray.get(i);
                                    Enemy enemy = new Enemy();
                                    enemy.id = parseIntSafe(dataObject.get("id"), 0);
                                    enemy.name = String.valueOf(dataObject.get("name"));
                                    enemy.head = (short) parseIntSafe(dataObject.get("head"), 0);
                                    enemy.body = (short) parseIntSafe(dataObject.get("body"), 0);
                                    enemy.leg = (short) parseIntSafe(dataObject.get("leg"), 0);
                                    enemy.bag = (byte) parseIntSafe(dataObject.get("bag"), 0);
                                    enemy.power = parseLongSafe(dataObject.get("power"), 0L);
                                    player.enemies.add(enemy);
                                    dataObject.clear();
                                }
                            }
                            dataArray.clear();
                        }
                    } catch (Exception e) {}

                    // data nội tại
                    dataArray = (JSONArray) jv.parse(rs.getString("data_intrinsic"));
                    byte intrinsicId = Byte.parseByte(dataArray.get(0).toString());
                    player.playerIntrinsic.intrinsic = IntrinsicService.gI().getIntrinsicById(intrinsicId);
                    player.playerIntrinsic.intrinsic.param1 = Short.parseShort(dataArray.get(1).toString());
                    player.playerIntrinsic.countOpen = Byte.parseByte(dataArray.get(2).toString());
                    player.playerIntrinsic.intrinsic.param2 = Short.parseShort(dataArray.get(3).toString());
                    dataArray.clear();

                    // data item time
                    dataArray = (JSONArray) jv.parse(rs.getString("data_item_time"));
                    int timeBoKhi = Integer.parseInt(dataArray.get(0).toString());
                    int timeAnDanh = Integer.parseInt(dataArray.get(1).toString());
                    int timeOpenPower = Integer.parseInt(dataArray.get(2).toString());
                    int timeCuongNo = Integer.parseInt(dataArray.get(3).toString());
                    int timeBoHuyet = Integer.parseInt(dataArray.get(5).toString());
                    int timeGiapXen = Integer.parseInt(dataArray.get(8).toString());
                    int timeMayDo = 0;
                    int timeMeal = 0;
                    int iconMeal = 0;
                    try {
                        timeMayDo = Integer.parseInt(dataArray.get(4).toString());
                        timeMeal = Integer.parseInt(dataArray.get(7).toString());
                        iconMeal = Integer.parseInt(dataArray.get(6).toString());
                    } catch (Exception e) {
                    }
                    int timeBanhChung1 = 0;
                    int timeBanhTet1 = 0;
                    int timeBoKhi2 = 0;
                    int timeGiapXen2 = 0;
                    int timeCuongNo2 = 0;
                    int timeBoHuyet2 = 0;
                    if (dataArray.size() >= 15) {
                        timeBanhChung1 = Integer.parseInt(dataArray.get(9).toString());
                        timeBanhTet1 = Integer.parseInt(dataArray.get(10).toString());
                        timeBoKhi2 = Integer.parseInt(dataArray.get(11).toString());
                        timeGiapXen2 = Integer.parseInt(dataArray.get(12).toString());
                        timeCuongNo2 = Integer.parseInt(dataArray.get(13).toString());
                        timeBoHuyet2 = Integer.parseInt(dataArray.get(14).toString());
                    }
                    
                    // BiNgo
                    int timeBiNgo = 0;
                    if (dataArray.size() >= 16) {
                        timeBiNgo = Integer.parseInt(dataArray.get(15).toString());
                    }
                    // BiNgo
                    
                    player.itemTime.lastTimeBoHuyet = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoHuyet);
                    player.itemTime.lastTimeBoKhi = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoKhi);
                    player.itemTime.lastTimeGiapXen = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeGiapXen);
                    player.itemTime.lastTimeCuongNo = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeCuongNo);
                    player.itemTime.lastTimeBoHuyet2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoHuyet2);
                    player.itemTime.lastTimeBoKhi2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoKhi2);
                    player.itemTime.lastTimeGiapXen2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeGiapXen2);
                    player.itemTime.lastTimeCuongNo2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeCuongNo2);
                    player.itemTime.lastTimeAnDanh = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeAnDanh);
                    player.itemTime.lastTimeOpenPower = System.currentTimeMillis()
                            - (ItemTime.TIME_OPEN_POWER - timeOpenPower);
                    player.itemTime.lastTimeUseMayDo = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO - timeMayDo);
                    player.itemTime.lastTimeEatMeal = System.currentTimeMillis() - (ItemTime.TIME_EAT_MEAL - timeMeal);
                    player.itemTime.lastTimeBanhChung = System.currentTimeMillis()
                            - (ItemTime.TIME_EAT_MEAL - timeBanhChung1);
                    player.itemTime.lastTimeBanhTet = System.currentTimeMillis()
                            - (ItemTime.TIME_EAT_MEAL - timeBanhTet1);
                    player.itemTime.iconMeal = iconMeal;
                    player.itemTime.isUseBoHuyet = timeBoHuyet != 0;
                    player.itemTime.isUseBoKhi = timeBoKhi != 0;
                    player.itemTime.isUseGiapXen = timeGiapXen != 0;
                    player.itemTime.isUseCuongNo = timeCuongNo != 0;
                    player.itemTime.isUseBoHuyet2 = timeBoHuyet2 != 0;
                    player.itemTime.isUseBoKhi2 = timeBoKhi2 != 0;
                    player.itemTime.isUseGiapXen2 = timeGiapXen2 != 0;
                    player.itemTime.isUseCuongNo2 = timeCuongNo2 != 0;
                    player.itemTime.isUseAnDanh = timeAnDanh != 0;
                    player.itemTime.isOpenPower = timeOpenPower != 0;
                    player.itemTime.isUseMayDo = timeMayDo != 0;
                    player.itemTime.isEatMeal = timeMeal != 0;
                    player.itemTime.isUseBanhChung = timeBanhChung1 != 0;
                    player.itemTime.isUseBanhTet = timeBanhTet1 != 0;
                    
                    // BiNgo
                    player.effectSkill.isBiNgo = timeBiNgo != 0;
                    player.effectSkill.lastBiNgo = System.currentTimeMillis() - (30_000 - timeBiNgo);
                    // BiNgo
                    
                    dataArray.clear();

                    // data nhiệm vụ
                    dataArray = (JSONArray) jv.parse(rs.getString("data_task"));
                    TaskMain taskMain = TaskService.gI().getTaskMainById(player,
                            Byte.parseByte(dataArray.get(1).toString()));
                    taskMain.subTasks.get(Integer.parseInt(dataArray.get(2).toString())).count = Short
                            .parseShort(dataArray.get(0).toString());
                    taskMain.index = Byte.parseByte(dataArray.get(2).toString());
                    player.playerTask.taskMain = taskMain;
                    dataArray.clear();

                    // data nhiệm vụ hàng ngày
                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("data_side_task"));
                        String format = "dd-MM-yyyy";
                        long receivedTime = Long.parseLong(String.valueOf(dataArray.get(4)));
                        Date date = new Date(receivedTime);
                        if (TimeUtil.formatTime(date, format).equals(TimeUtil.formatTime(new Date(), format))) {
                            player.playerTask.sideTask.level = Integer
                                    .parseInt(String.valueOf(dataArray.get(0).toString()));
                            player.playerTask.sideTask.count = Integer.parseInt(dataArray.get(1).toString());
                            player.playerTask.sideTask.leftTask = Integer
                                    .parseInt(String.valueOf(dataArray.get(2).toString()));
                            player.playerTask.sideTask.template = TaskService.gI()
                                    .getSideTaskTemplateById(Integer.parseInt(dataArray.get(3).toString()));
                            player.playerTask.sideTask.maxCount = Integer
                                    .parseInt(String.valueOf(dataArray.get(5).toString()));
                            player.playerTask.sideTask.receivedTime = receivedTime;
                        }
                    } catch (Exception e) {
                    }

                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("achivements"));
                        if (dataArray != null) {
                            for (int i = 0; i < dataArray.size(); i++) {
                                Object achiveObj = jv.parse(String.valueOf(dataArray.get(i)));
                                if (achiveObj instanceof JSONObject) {
                                    dataObject = (JSONObject) achiveObj;
                                    Achivement achivement = new Achivement();
                                    achivement.setId(parseIntSafe(dataObject.get("id"), i));
                                    achivement.setCount(parseIntSafe(dataObject.get("count"), 0));
                                    achivement.setFinish(parseIntSafe(dataObject.get("finish"), 0) == 1);
                                    achivement.setReceive(parseIntSafe(dataObject.get("receive"), 0) == 1);
                                    AchivementTemplate a = AchiveManager.getInstance().findByID(achivement.getId());
                                    if (a != null) {
                                        achivement.setName(a.getName());
                                        achivement.setDetail(a.getDetail());
                                        achivement.setMaxCount(a.getMaxCount());
                                        achivement.setMoney(a.getMoney());
                                    }
                                    player.playerTask.achivements.add(achivement);
                                }
                            }
                        }
                    } catch (Exception e) {}

                    List<AchivementTemplate> listAchivements = AchiveManager.getInstance().getList();
                    if (dataArray.size() < listAchivements.size()) { // add thêm nhiệm vụ khi có nhiệm vụ mới
                        for (int i = dataArray.size(); i < listAchivements.size(); i++) {
                            AchivementTemplate a = AchiveManager.getInstance().findByID(i);
                            Achivement achivement = new Achivement();
                            if (a != null) {
                                achivement.setId(a.getId());
                                achivement.setCount(0);
                                achivement.setFinish(false);
                                achivement.setReceive(false);
                                achivement.setName(a.getName());
                                achivement.setDetail(a.getDetail());
                                achivement.setMaxCount(a.getMaxCount());
                                achivement.setMoney(a.getMoney());
                                player.playerTask.achivements.add(achivement);
                            }
                        }
                    }
                    dataArray.clear();

                    // data trứng bư
                    try {
                        Object mabuEggObj = jv.parse(rs.getString("data_mabu_egg"));
                        if (mabuEggObj instanceof JSONObject) {
                            dataObject = (JSONObject) mabuEggObj;
                            Object createTime = dataObject.get("create_time");
                            if (createTime != null) {
                                player.mabuEgg = new MabuEgg(player, parseLongSafe(createTime, 0L),
                                        parseLongSafe(dataObject.get("time_done"), 0L));
                            }
                            dataObject.clear();
                        }
                    } catch (Exception e) {}

                    // data bùa
                    dataArray = (JSONArray) jv.parse(rs.getString("data_charm"));
                    player.charms.tdTriTue = Long.parseLong(dataArray.get(0).toString());
                    player.charms.tdManhMe = Long.parseLong(dataArray.get(1).toString());
                    player.charms.tdDaTrau = Long.parseLong(dataArray.get(2).toString());
                    player.charms.tdOaiHung = Long.parseLong(dataArray.get(3).toString());
                    player.charms.tdBatTu = Long.parseLong(dataArray.get(4).toString());
                    player.charms.tdDeoDai = Long.parseLong(dataArray.get(5).toString());
                    player.charms.tdThuHut = Long.parseLong(dataArray.get(6).toString());
                    player.charms.tdDeTu = Long.parseLong(dataArray.get(7).toString());
                    player.charms.tdTriTue3 = Long.parseLong(dataArray.get(8).toString());
                    player.charms.tdTriTue4 = Long.parseLong(dataArray.get(9).toString());
                    if (dataArray.size() >= 11) {
                        player.charms.tdDeTuMabu = Long.parseLong(dataArray.get(10).toString());
                    }
                    dataArray.clear();

                    // data skill
                    try {
                        dataArray = (JSONArray) jv.parse(rs.getString("skills"));
                        if (dataArray != null) {
                            for (int i = 0; i < dataArray.size(); i++) {
                                Object sObj = jv.parse(String.valueOf(dataArray.get(i)));
                                if (sObj instanceof JSONArray) {
                                    JSONArray skillTemp = (JSONArray) sObj;
                                    int tempId = parseIntSafe(skillTemp.get(0), 0);
                                    byte point = (byte) parseIntSafe(skillTemp.get(2), 0);
                                    Skill skill = null;
                                    if (point != 0) {
                                        skill = SkillUtil.createSkill(tempId, point);
                                    } else {
                                        skill = SkillUtil.createSkillLevel0(tempId);
                                    }
                                    skill.lastTimeUseThisSkill = parseLongSafe(skillTemp.get(1), 0L);
                                    player.playerSkill.skills.add(skill);
                                    skillTemp.clear();
                                }
                            }
                            dataArray.clear();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // data skill shortcut
                    dataArray = (JSONArray) jv.parse(rs.getString("skills_shortcut"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        player.playerSkill.skillShortCut[i] = Byte.parseByte(String.valueOf(dataArray.get(i)));
                    }
                    for (int i : player.playerSkill.skillShortCut) {
                        if (player.playerSkill.getSkillbyId(i) != null
                                && player.playerSkill.getSkillbyId(i).damage > 0) {
                            player.playerSkill.skillSelect = player.playerSkill.getSkillbyId(i);
                            break;
                        }
                    }
                    if (player.playerSkill.skillSelect == null) {
                        player.playerSkill.skillSelect = player.playerSkill
                                .getSkillbyId(player.gender == ConstPlayer.TRAI_DAT
                                        ? Skill.DRAGON
                                        : (player.gender == ConstPlayer.NAMEC ? Skill.DEMON : Skill.GALICK));
                    }
                    dataArray.clear();

                    Gson gson = new Gson();
                    List<Card> cards = gson.fromJson(rs.getString("collection_book"), new TypeToken<List<Card>>() {
                    }.getType());

                    CollectionBook book = new CollectionBook(player);
                    if (cards != null) {
                        book.setCards(cards);
                    } else {
                        book.setCards(new ArrayList<>());
                    }
                    book.init();
                    player.setCollectionBook(book);
                    List<Item> itemsBody = player.inventory.itemsBody;

                    if (itemsBody.size() > 11 && itemsBody.get(11) != null && itemsBody.get(11).isNotNullItem()) {
                        MiniPet.callMiniPet(player, (player.inventory.itemsBody.get(11).template.id));
                    }

                    if (itemsBody.size() > 10 && itemsBody.get(10) != null && itemsBody.get(10).isNotNullItem()) {
                        PetFollow pet = PetFollowManager.gI().findByID(itemsBody.get(10).getId());
                        player.setPetFollow(pet);
                        PlayerService.gI().sendPetFollow(player);
                    }

                    player.firstTimeLogin = rs.getTimestamp("firstTimeLogin");

                    dataArray = (JSONArray) JSONValue.parse(rs.getString("buy_limit"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        player.buyLimit[i] = Byte.parseByte(dataArray.get(i).toString());
                    }

                    dataArray = (JSONArray) JSONValue.parse(rs.getString("reward_limit"));

                    player.rewardLimit = new byte[dataArray.size()];
                    for (int i = 0; i < dataArray.size(); i++) {
                        player.rewardLimit[i] = Byte.parseByte(dataArray.get(i).toString());
                    }

                    // dhvt23
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("challenge"));
                    player.goldChallenge = Integer.parseInt(dataArray.get(0).toString());
                    player.levelWoodChest = Integer.parseInt(dataArray.get(1).toString());
                    player.receivedWoodChest = Integer.parseInt(dataArray.get(2).toString()) == 1;
                    player.gemChallenge = Integer.parseInt(dataArray.get(3).toString());
                    dataArray.clear();

                    PlayerService.gI().dailyLogin(player);

                    // data pet
                    try {
                        Object petInfoRaw = jv.parse(rs.getString("pet_info"));
                        if (petInfoRaw instanceof JSONObject) {
                            dataObject = (JSONObject) petInfoRaw;
                            if (!dataObject.isEmpty() && !String.valueOf(dataObject).equals("{}")) {
                                Pet pet = new Pet(player);
                                pet.id = -player.id;
                                pet.gender = (byte) parseIntSafe(dataObject.get("gender"), 0);
                                pet.isMabu = parseIntSafe(dataObject.get("is_mabu"), 0) == 1;
                                pet.isBulo = parseIntSafe(dataObject.get("is_Bulo"), 0) == 1;
                                pet.isCellBao = parseIntSafe(dataObject.get("is_CellBao"), 0) == 1;
                                pet.isBillNhi = parseIntSafe(dataObject.get("is_BillNhi"), 0) == 1;
                                pet.isFideTrau = parseIntSafe(dataObject.get("is_FideTrau"), 0) == 1;
                                pet.isSuperPicolo = parseIntSafe(dataObject.get("is_SuperPicolo"), 0) == 1;
                                pet.name = String.valueOf(dataObject.get("name"));
                                player.fusion.typeFusion = (byte) parseIntSafe(dataObject.get("type_fusion"), 0);
                                player.fusion.lastTimeFusion = System.currentTimeMillis()
                                        - (Fusion.TIME_FUSION
                                        - parseIntSafe(dataObject.get("left_fusion"), 0));
                                pet.status = (byte) parseIntSafe(dataObject.get("status"), 0);

                                // data chỉ số
                                Object petPointRaw = jv.parse(rs.getString("pet_point"));
                                if (petPointRaw instanceof JSONObject) {
                                    JSONObject pPoint = (JSONObject) petPointRaw;
                                    pet.nPoint.stamina = (short) parseIntSafe(pPoint.get("stamina"), 1000);
                                    pet.nPoint.maxStamina = (short) parseIntSafe(pPoint.get("max_stamina"), 1000);
                                    pet.nPoint.hpg = parseIntSafe(pPoint.get("hpg"), 100);
                                    pet.nPoint.mpg = parseIntSafe(pPoint.get("mpg"), 100);
                                    pet.nPoint.dameg = parseIntSafe(pPoint.get("damg"), 10);
                                    pet.nPoint.defg = parseIntSafe(pPoint.get("defg"), 0);
                                    pet.nPoint.critg = parseIntSafe(pPoint.get("critg"), 0);
                                    pet.nPoint.power = parseLongSafe(pPoint.get("power"), 2000L);
                                    pet.nPoint.tiemNang = parseLongSafe(pPoint.get("tiem_nang"), 0L);
                                    pet.nPoint.limitPower = (byte) parseIntSafe(pPoint.get("limit_power"), 0);
                                    int hp = parseIntSafe(pPoint.get("hp"), 100);
                                    int mp = parseIntSafe(pPoint.get("mp"), 100);
                                    pet.nPoint.hp = hp;
                                    pet.nPoint.mp = mp;
                                }

                                // data body
                                try {
                                    Object petBodyRaw = jv.parse(rs.getString("pet_body"));
                                    if (petBodyRaw instanceof JSONArray) {
                                        dataArray = (JSONArray) petBodyRaw;
                                        for (int i = 0; i < dataArray.size(); i++) {
                                            if (dataArray.get(i) instanceof JSONObject) {
                                                dataObject = (JSONObject) dataArray.get(i);
                                                Item item = null;
                                                short tempId = (short) parseIntSafe(dataObject.get("temp_id"), -1);
                                                if (tempId != -1) {
                                                    item = ItemService.gI().createNewItem(tempId,
                                                            parseIntSafe(dataObject.get("quantity"), 1));
                                                    JSONArray options = (JSONArray) dataObject.get("option");
                                                    if (options != null) {
                                                        for (int j = 0; j < options.size(); j++) {
                                                            JSONArray opt = (JSONArray) options.get(j);
                                                            item.itemOptions.add(new ItemOption(parseIntSafe(opt.get(0), 0),
                                                                    parseIntSafe(opt.get(1), 0)));
                                                        }
                                                    }
                                                    item.createTime = parseLongSafe(dataObject.get("create_time"), 0L);
                                                    if (ItemService.gI().isOutOfDateTime(item)) {
                                                        item = ItemService.gI().createItemNull();
                                                    }
                                                } else {
                                                    item = ItemService.gI().createItemNull();
                                                }
                                                pet.inventory.itemsBody.add(item);
                                            }
                                        }
                                    }
                                } catch (Exception pbe) {}

                                // data skills
                                try {
                                    Object petSkillRaw = jv.parse(rs.getString("pet_skill"));
                                    if (petSkillRaw instanceof JSONArray) {
                                        dataArray = (JSONArray) petSkillRaw;
                                        for (int i = 0; i < dataArray.size(); i++) {
                                            if (dataArray.get(i) instanceof JSONArray) {
                                                JSONArray skillTemp = (JSONArray) dataArray.get(i);
                                                int tempId = parseIntSafe(skillTemp.get(0), -1);
                                                byte point = (byte) parseIntSafe(skillTemp.get(1), 0);
                                                Skill skill = null;
                                                if (point != 0) {
                                                    skill = SkillUtil.createSkill(tempId, point);
                                                } else {
                                                    skill = SkillUtil.createSkillLevel0(tempId);
                                                }
                                                if (skill != null) {
                                                    switch (skill.template.id) {
                                                        case Skill.KAMEJOKO:
                                                        case Skill.MASENKO:
                                                        case Skill.ANTOMIC:
                                                            skill.coolDown = 1000;
                                                            break;
                                                    }
                                                    pet.playerSkill.skills.add(skill);
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception pse) {}
                                player.pet = pet;
                            }
                        }
                    } catch (Exception petEx) {
                        petEx.printStackTrace();
                    }
                    if (session.ruby > 0) {
                        player.inventory.ruby += session.ruby;
                        if (player.playerTask.achivements.size() > ConstAchive.LAN_DAU_NAP_NGOC) {
                            player.playerTask.achivements.get(ConstAchive.LAN_DAU_NAP_NGOC).count += session.ruby;
                        }
                        PlayerDAO.subRuby(player, session.userId, session.ruby);
                    }
                    player.nPoint.hp = plHp;
                    player.nPoint.mp = plMp;
                    session.player = player;
                    PreparedStatement ps2 = connection
                            .prepareStatement("update account set last_time_login = ?, ip_address = ? where id = ?");
                    ps2.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps2.setString(2, session.ipAddress);
                    ps2.setInt(3, session.userId);
                    ps2.executeUpdate();
                    ps2.close();

                    PreparedStatement ps3 = connection
                            .prepareStatement("update player set lastimelogin = ? where account_id = ?");
                    ps3.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps3.setInt(2, session.userId);
                    ps3.executeUpdate();
                    ps3.close();

                    PreparedStatement ps4 = connection
                            .prepareStatement("update player set tongnap = ? where account_id = ?");
                    ps4.setInt(1, session.tongnap);
                    ps4.setInt(2, session.userId);
                    ps4.executeUpdate();
                    ps4.close();
                    return player;
                }
            } finally {
                rs.close();
                ps.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            session.dataLoadFailed = true;
        }
        return null;
    }
}
