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
 * NPC ToriBot
 */
public class ToriBot extends Npc {

    public ToriBot(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "|2|Ta là Tori-Bot VIP!\nBạn đã nhận: [" + player.event.luotNhanVipGift + "/4 lần]\n(Có thể chọn 1 VIP nhận 4 lần, tối đa 4 lần)",
                    "VIP 1", "VIP 2", "VIP 3", "VIP 4", "Hành Tinh\nBóng Tối", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> { // VIP 1
                        if (player.event.luotNhanVipGift >= 4) {
                            Service.getInstance().sendThongBao(player, "|7|Bạn đã đạt giới hạn tối đa 4 lần nhận quà VIP!");
                            return;
                        }
                        createOtherMenu(player, ConstNpc.MENU_TORIBOT_VIP_1,
                                "|2|[GÓI VIP 1 - Đã nhận: " + player.event.luotNhanVipGift + "/4 lần]\nBạn muốn nhận Capsule hay mở trực tiếp Set Kích Hoạt?",
                                "Nhận Capsule", "Mở Trọn Bộ Đồ", "Quay lại");
                    }
                    case 1 -> { // VIP 2
                        if (player.event.luotNhanVipGift >= 4) {
                            Service.getInstance().sendThongBao(player, "|7|Bạn đã đạt giới hạn tối đa 4 lần nhận quà VIP!");
                            return;
                        }
                        createOtherMenu(player, ConstNpc.MENU_TORIBOT_VIP_2,
                                "|2|[GÓI VIP 2 - Đã nhận: " + player.event.luotNhanVipGift + "/4 lần]\nBạn muốn nhận Capsule hay mở trực tiếp Set Kích Hoạt?",
                                "Nhận Capsule", "Mở Trọn Bộ Đồ", "Quay lại");
                    }
                    case 2 -> { // VIP 3
                        if (player.event.luotNhanVipGift >= 4) {
                            Service.getInstance().sendThongBao(player, "|7|Bạn đã đạt giới hạn tối đa 4 lần nhận quà VIP!");
                            return;
                        }
                        createOtherMenu(player, ConstNpc.MENU_TORIBOT_VIP_3,
                                "|2|[GÓI VIP 3 - Đã nhận: " + player.event.luotNhanVipGift + "/4 lần]\nBạn muốn nhận Capsule hay mở trực tiếp Set Kích Hoạt?",
                                "Nhận Capsule", "Mở Trọn Bộ Đồ", "Quay lại");
                    }
                    case 3 -> { // VIP 4
                        if (player.event.luotNhanVipGift >= 4) {
                            Service.getInstance().sendThongBao(player, "|7|Bạn đã đạt giới hạn tối đa 4 lần nhận quà VIP!");
                            return;
                        }
                        createOtherMenu(player, ConstNpc.MENU_TORIBOT_VIP_4,
                                "|2|[GÓI VIP 4 - Đã nhận: " + player.event.luotNhanVipGift + "/4 lần]\nBạn muốn nhận Capsule hay mở trực tiếp Set Kích Hoạt?",
                                "Nhận Capsule", "Mở Trọn Bộ Đồ", "Quay lại");
                    }
                    case 4 -> { // Hành Tinh Bóng Tối
                        if (TaskService.gI().getIdTask(player) >= ConstTask.TASK_21_0) {
                            ChangeMapService.gI().changeMapBySpaceShip(player, 168, -1, 168);
                        } else {
                            Service.gI().sendThongBaoOK(player, "Yêu Cầu Làm qua NV Nappa");
                        }
                    }
                }
            } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_TORIBOT_VIP_1) {
                switch (select) {
                    case 0 -> claimVipGift(player, 1, true);
                    case 1 -> claimVipGift(player, 1, false);
                    case 2 -> openBaseMenu(player);
                }
            } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_TORIBOT_VIP_2) {
                switch (select) {
                    case 0 -> claimVipGift(player, 2, true);
                    case 1 -> claimVipGift(player, 2, false);
                    case 2 -> openBaseMenu(player);
                }
            } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_TORIBOT_VIP_3) {
                switch (select) {
                    case 0 -> claimVipGift(player, 3, true);
                    case 1 -> claimVipGift(player, 3, false);
                    case 2 -> openBaseMenu(player);
                }
            } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_TORIBOT_VIP_4) {
                switch (select) {
                    case 0 -> claimVipGift(player, 4, true);
                    case 1 -> claimVipGift(player, 4, false);
                    case 2 -> openBaseMenu(player);
                }
            }
        }
    }
}
