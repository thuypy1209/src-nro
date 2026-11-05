package nro.models.npc_list;

import nro.attr.AttributeManager;
import nro.consts.*;
import nro.dialog.ConfirmDialog;
import nro.dialog.MenuDialog;
import nro.jdbc.daos.PlayerDAO;
import nro.lib.RandomCollection;
import nro.models.boss.Boss;
import nro.models.boss.BossFactory;
import nro.models.boss.BossManager;
import nro.models.boss.event.EscortedBoss;
import nro.models.boss.event.Qilin;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import nro.models.kigui.KiGuiShop;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.item.ItemTemplate;
import nro.models.map.ItemMap;
import nro.models.map.Map;
import nro.models.map.SantaCity;
import nro.models.map.Zone;
import nro.models.map.DaiHoiVoThuat.DHVT23Service;
import nro.models.map.dungeon.SnakeRoad;
import nro.models.map.dungeon.zones.ZSnakeRoad;
import nro.models.map.mabu.MabuWar;
import nro.models.map.phoban.DoanhTrai;
import nro.models.map.war.BlackBallWar;
import nro.models.map.war.NamekBallWar;
import nro.models.player.Inventory;
import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.noti.NotiManager;
import nro.server.Maintenance;
import nro.server.Manager;
import nro.server.ServerManager;
import nro.server.io.Message;
import nro.services.*;
import nro.services.func.*;
import nro.services.func.DoiQua.DoiVND;
import nro.utils.Log;
import nro.utils.SkillUtil;
import nro.utils.TimeUtil;
import nro.utils.Util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import nro.jdbc.DBService;
import nro.models.boss.BossData;
import nro.models.boss.Potaufeu.Boss_NhanBan;
import nro.models.boss.mapoffline.Boss_ThanMeo;
import nro.models.boss.mapoffline.Boss_ThuongDe;
import nro.models.boss.mapoffline.Boss_Whis;
import nro.models.boss.mapoffline.NPC_ToSuKaio;
import nro.models.map.DaiHoiVoThuat.DaiHoiVoThuatManager;
import nro.models.map.DaiHoiVoThuat.DaiHoiVoThuatService;
import nro.models.map.VoDaiSinhTu.VoDaiSinhTuService;
import nro.models.map.phoban.BanDoKhoBau;
import nro.server.Client;
import static nro.server.Manager.*;
import static nro.services.func.SummonDragon.*;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import static nro.models.npc.NpcFactory.*;

/**
 * NPC ThanVuTru
 */
public class ThanVuTru extends Npc {

    public ThanVuTru(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 48) {
                if (player.doneThachDauBubbles == 0) {
                    this.createOtherMenu(player, ConstNpc.THACH_DAU_BUBBLES,
                            "Thượng đế đưa ngươi đến đây, chắc muốn ta dạy võ chứ gì\n"
                                    + "Bắt được con khỉ Bubbles rồi hãy tính",
                            player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động" : "Đăng ký\ntập\ntự động", "Tập luyện\nvới\nBubbles",
                            "Thách đấu\nBubbles", "Di chuyển");
                } else if (player.doneThachDauThuongDe == 0) {
                    this.createOtherMenu(player, ConstNpc.THACH_DAU_THAN_VU_TRU,
                            "Ta là Thần Vũ Trụ Phương Bắc cai quản khu vực bắc vũ trụ\n"
                                    + "nếu thắng được ta, ngươi sẽ được đến\n"
                                    + "Lành Đại Kaio, nơi ở của Thần Linh",
                            player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động" : "Đăng ký\ntập\ntự động", "Tập luyện\nvới\nThần Vũ Trụ",
                            "Thách đấu\nThần Vũ Trụ", "Di chuyển");
                } else {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Con mạnh nhất phía bắc vũ trụ này rồi đấy\n"
                                    + "nhưng ngoài vũ trụ bao la kia vẫn có những kẻ mạnh hơn nhìu\n"
                                    + "con cần phải luyện tập để mạnh hơn nữa",
                            player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động" : "Đăng ký\ntập\ntự động", "Tập luyện\nvới\nBubbles",
                            "Tập luyện\nvới\nThần Vũ Trụ", "Di chuyển");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 48) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.BASE_MENU:
                        switch (select) {
                            case 0:// tập tự động
                                handleAutoTrainingMenu(player);
                                break;
                            case 1:// tập luyện với bubbles

                                break;
                            case 2:// tập luyện với thần vũ trụ

                                break;
                            case 3:
                                this.createOtherMenu(player, ConstNpc.MENU_DI_CHUYEN,
                                        "Con muốn đi đâu?", "Về\nthần điện", "Thánh địa\nKaio",
                                        "Con\nđường\nrắn độc", "Từ chối");
                                break;
                        }
                        break;
                    case ConstNpc.THACH_DAU_BUBBLES:
                        switch (select) {
                            case 0:// tập tự động
                                handleAutoTrainingMenu(player);
                                break;
                            case 1:// tập luyện với bubbles

                                break;
                            case 2:// tập luyện với thần vũ trụ

                                break;
                            case 3:
                                this.createOtherMenu(player, ConstNpc.MENU_DI_CHUYEN,
                                        "Con muốn đi đâu?", "Về\nthần điện", "Thánh địa\nKaio",
                                        "Con\nđường\nrắn độc", "Từ chối");
                                break;
                        }
                        break;

                    case ConstNpc.THACH_DAU_THAN_VU_TRU:
                        switch (select) {
                            case 0:// tập tự động
                                handleAutoTrainingMenu(player);
                                break;
                            case 1:// tập luyện với bubbles

                                break;
                            case 2:// tập luyện với thần vũ trụ

                                break;
                            case 3:
                                this.createOtherMenu(player, ConstNpc.MENU_DI_CHUYEN,
                                        "Con muốn đi đâu?", "Về\nthần điện", "Thánh địa\nKaio",
                                        "Con\nđường\nrắn độc", "Từ chối");
                                break;
                        }
                        break;

                    case 2001:
                        switch (select) {
                            case 0 -> NpcService.gI().createTutorial(player, tempId, avartar,
                                    "Tập tự động: Khi Offline quá 30 phút, nhân vật sẽ tự động nhận thêm tiềm năng và sức mạnh.");
                            case 1 -> {
                                player.mapIdDangTapTuDong = mapId;
                                player.dangKyTapTuDong = true;
                                NpcService.gI().createTutorial(player, tempId, avartar,
                                        "Từ giờ, quá 30 phút Offline con sẽ được tự động luyện tập");
                            }
                        }
                        break;
                    case ConstNpc.MENU_DI_CHUYEN:
                        switch (select) {
                            case 0:
                                ChangeMapService.gI().changeMapBySpaceShip(player, 45, -1, 354);
                                break;
                            case 1:
                                ChangeMapService.gI().changeMap(player, 50, -1, 318, 336);
                                break;
                            case 2:
                                // con đường rắn độc
                                if (player.clan != null) {
                                    // Calendar calendar = Calendar.getInstance();
                                    // int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                                    // if (!(dayOfWeek == Calendar.MONDAY
                                    // || dayOfWeek == Calendar.WEDNESDAY
                                    // || dayOfWeek == Calendar.FRIDAY
                                    // || dayOfWeek == Calendar.SUNDAY)) {
                                    // Service.getInstance().sendThongBao(player,
                                    // "Chỉ mở vào thứ 2, 4, 6, CN hàng tuần!");
                                    // return;
                                    // }
                                    if (player.clanMember.getNumDateFromJoinTimeToToday() < 1) {
                                        Service.getInstance().sendThongBao(player,
                                                "Phải tham gia bang hội ít nhất 1 ngày mới có thể tham gia!");
                                        return;
                                    }
                                    if (player.clan.snakeRoad == null) {
                                        this.createOtherMenu(player, ConstNpc.MENU_CHON_CAP_DO,
                                                "Hãy mau trở về bằng con đường rắn độc\nbọn Xayda đã đến Trái Đất",
                                                "Chọn\ncấp độ", "Từ chối");
                                        return;
                                    }
                                    if (player.clan.snakeRoad.isClosed()) {
                                        Service.getInstance().sendThongBao(player,
                                                "Bang hội đã hết lượt tham gia!");
                                        return;
                                    }
                                    this.createOtherMenu(player,
                                            ConstNpc.MENU_ACCEPT_GO_TO_CDRD,
                                            "Con có chắc chắn muốn đến con đường rắn độc cấp độ " +
                                                    player.clan.snakeRoad.getLevel() + "?",
                                            "Đồng ý", "Từ chối");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Chỉ dành cho những người trong bang hội!");
                                }
                                break;
                        }
                        break;
                    case ConstNpc.MENU_ACCEPT_GO_TO_CDRD:
                        switch (select) {
                            case 0:
                                if (player.clan != null) {
                                    synchronized (player.clan) {
                                        if (player.clan.snakeRoad == null) {
                                            int level = Byte.parseByte(
                                                    String.valueOf(PLAYERID_OBJECT.get(player.id)));
                                            SnakeRoad road = new SnakeRoad(level);
                                            ServerManager.gI().getDungeonManager().addDungeon(road);
                                            road.join(player);
                                            player.clan.snakeRoad = road;
                                        } else {
                                            player.clan.snakeRoad.join(player);
                                        }
                                    }
                                }
                                break;
                        }
                        break;
                    case ConstNpc.MENU_CHON_CAP_DO:
                        switch (select) {
                            case 0:
                                Input.gI().createFormChooseLevelCDRD(player);
                                break;
                        }
                        break;
                }
            }
        }
    }


    private void handleAutoTrainingMenu(Player player) {
        if (player.dangKyTapTuDong) {
            player.dangKyTapTuDong = false;
            NpcService.gI().createTutorial(player, tempId, avartar,
                    "Con đã hủy thành công đăng ký tập tự động\ntừ giờ con muốn tập Offline hãy tự đến đây trước");
        } else {
            this.createOtherMenu(player, 2001,
                    "Đăng ký để mỗi khi Offline quá 30 phút, con sẽ được tự động luyện tập với tốc độ 1280 sức mạnh mỗi phút",
                    "Hướng\ndẫn\nthêm", "Đồng ý\n1 ngọc\nmỗi lần", "Không\nđồng ý");
        }
    }
}
