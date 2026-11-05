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
 * NPC LinhCanh
 */
public class LinhCanh extends Npc {

    public LinhCanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (player.clan == null) {
                this.createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_DT,
                        "Chỉ tiếp các bang hội, miễn tiếp khách vãng lai", "Đóng");
            } else if (player.clan.getMembers().size() < 5) {
                NpcService.gI().createTutorial(player, avartar,
                        "Bang hội phải có từ 5 thành viên mới được tham gia ");
            } else {
                ClanMember clanMember = player.clan.getClanMember((int) player.id);
                int days = (int) (((System.currentTimeMillis() / 1000) - clanMember.joinTime) / 60
                        / 60 / 24);
                if (days < 2) {
                    NpcService.gI().createTutorial(player, avartar,
                            "Chỉ những thành viên gia nhập bang hội tối thiểu 2 ngày mới có thể tham gia");
                    return;
                }
                /*
                 * if (days < 999999999) {
                 * NpcService.gI().createTutorial(player, avartar,
                 * "Chức năng tạm đóng để đua top open");
                 * return;
                 * }
                 */
                if (!player.clan.haveGoneDoanhTrai && player.clan.timeOpenDoanhTrai != 0) {
                    createOtherMenu(player, ConstNpc.MENU_VAO_DT,
                            "Bang hội của ngươi đang đánh trại độc nhãn\n" + "Thời gian còn lại là "
                                    + TimeUtil.chuyenDoiTuGiaySangPhut(
                                            TimeUtil.getSecondLeft(player.clan.timeOpenDoanhTrai,
                                                    DoanhTrai.TIME_DOANH_TRAI / 1000))
                                    + ". Ngươi có muốn tham gia không?",
                            "Tham gia", "Không", "Hướng\ndẫn\nthêm");
                } else {
                    List<Player> plSameClans = new ArrayList<>();
                    List<Player> playersMap = player.zone.getPlayers();
                    synchronized (playersMap) {
                        for (Player pl : playersMap) {
                            if (!pl.equals(player) && pl.clan != null
                                    && pl.clan.id == player.clan.id && pl.location.x >= 1285
                                    && pl.location.x <= 1645) {
                                plSameClans.add(pl);
                            }

                        }
                    }
                    if (plSameClans.size() >= 2) {
                        if (!player.isAdmin() && player.clanMember
                                .getNumDateFromJoinTimeToToday() < DoanhTrai.DATE_WAIT_FROM_JOIN_CLAN) {
                            createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_DT,
                                    "Bang hội chỉ cho phép những người ở trong bang trên 1 ngày. Hẹn ngươi quay lại vào lúc khác",
                                    "OK", "Hướng\ndẫn\nthêm");
                        } else if (player.clan.haveGoneDoanhTrai) {
                            createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_DT,
                                    "Bang hội của ngươi đã đi trại lúc "
                                            + Util.formatTime(player.clan.timeOpenDoanhTrai)
                                            + " hôm nay. Người mở\n" + "("
                                            + player.clan.playerOpenDoanhTrai.name
                                            + "). Hẹn ngươi quay lại vào ngày mai",
                                    "OK", "Hướng\ndẫn\nthêm");

                        } else {
                            createOtherMenu(player, ConstNpc.MENU_CHO_VAO_DT,
                                    "Hôm nay bang hội của ngươi chưa vào trại lần nào. Ngươi có muốn vào\n"
                                            + "không?\nĐể vào, ta khuyên ngươi nên có 3-4 người cùng bang đi cùng",
                                    "Vào\n(miễn phí)", "Không", "Hướng\ndẫn\nthêm");
                        }
                    } else {
                        createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_DT,
                                "Ngươi phải có ít nhất 2 đồng đội cùng bang đứng gần mới có thể vào "
                                        + "tuy nhiên ta khuyên ngươi nên đi cùng với 3-4 người để khỏi chết.\n"
                                        + "Hahaha.",
                                "OK", "Hướng\ndẫn\nthêm");
                    }
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 27) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.MENU_KHONG_CHO_VAO_DT:
                        if (select == 1) {
                            NpcService.gI().createTutorial(player, this.avartar,
                                    ConstNpc.HUONG_DAN_DOANH_TRAI);
                        }
                        break;
                    case ConstNpc.MENU_CHO_VAO_DT:
                        switch (select) {
                            case 0:
                                DoanhTraiService.gI().openDoanhTrai(player);
                                break;
                            case 2:
                                NpcService.gI().createTutorial(player, this.avartar,
                                        ConstNpc.HUONG_DAN_DOANH_TRAI);
                                break;
                        }
                        break;
                    case ConstNpc.MENU_VAO_DT:
                        switch (select) {
                            case 0:
                                // ChangeMapService.gI().changeMap(player, 53, 0, 35, 432);
                                Service.gI().sendThongBao(player, "Chức Năng bảo trì");
                                break;
                            case 2:
                                NpcService.gI().createTutorial(player, this.avartar,
                                        ConstNpc.HUONG_DAN_DOANH_TRAI);
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
