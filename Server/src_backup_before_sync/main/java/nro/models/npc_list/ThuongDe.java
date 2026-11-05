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
 * NPC ThuongDe
 */
public class ThuongDe extends Npc {

    public ThuongDe(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }


    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 45) {
                // if (player.doneThachDauPoPo == 0) {
                // this.createOtherMenu(player, ConstNpc.THACH_DAU_POPO, "Pôpô là đệ tử của ta,
                // luyện tập với Pôpô con sẽ có thêm nhiều kinh nghiệm đánh bại được Pôpô ta sẽ
                // dạy võ công cho con", "Đăng ký\ntập\ntự động", "Tập luyện\nvới\nMr.Pôpô",
                // "Thách đấu\nMr.Pôpô", "Quay ngọc\nmay mắn");
                // }
                // if (player.doneThachDauPoPo == 1 && player.doneThachDauThuongDe == 0) {
                // this.createOtherMenu(player, ConstNpc.THACH_DAU_THUONG_DE, "Từ nay con sẽ là
                // đệ tử của ta. Ta sẽ truyền cho con tất cả tuyệt kĩ", "Đăng ký\ntập\nTự động",
                // "Tập luyện\nvới\nThượng Đế", "Thách đấu\nThượng Đế", "Quay ngọc\nMay mắn");
                // }
                // if (player.doneThachDauPoPo == 1 && player.doneThachDauThuongDe == 1) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Con đã mạnh hơn ta, ta sẽ chỉ đường cho con đến Kaio để gặp thần Vũ\nTrụ Phương Bắc\nNgài là thần cai quản vũ trụ này, hãy theo ngài ấy học võ công",
                        player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động" : "Đăng ký\ntập\ntự động", "Tập luyện\nvới\nMr.PôPô",
                        "Tập luyện\nvới\nThượng Đế", "Đến\nKaio", "Quay ngọc\nMay mắn");
                // }
            } else if (player.zone instanceof ZSnakeRoad) {
                if (mapId == ConstMap.CON_DUONG_RAN_DOC) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Hãy lắm lấy tay ta mau",
                            "Về thần điện");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        Boss boss = BossManager.gI().getBossById(Util.createIdDuongTank((int) player.id) - 200000);
        if (canOpenNpc(player)) {
            if (this.mapId == 45) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.BASE_MENU:
                        switch (select) {
                            case 0: // Đăng ký tập tự động
                                handleAutoTrainingMenu(player);
                                break;
                            case 1: // luyện tập với popo
                                player.activeYajiro = 1;
                                PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                player.thachDauNPC = 1;
                                break;
                            case 2: // tập luyện với thượng đế
                                ChangeMapService.gI().changeMap(player, 49, 0, 384, 440);
                                try {
                                    Boss_ThuongDe dt = new Boss_ThuongDe(
                                            Util.createIdDuongTank((int) player.id),
                                            BossData.THUONG_DE, player.zone, this.cx, this.cy,
                                            (int) player.id);
                                } catch (Exception ex) {
                                    Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE,
                                            null, ex);
                                }
                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                player.zone.load_Me_To_Another(player);
                                break;
                            case 3: // Đến kaio
                                ChangeMapService.gI().changeMapBySpaceShip(player, 48, -1, 354);
                                break;
                            case 4:// Vòng quay may mắn
                                this.createOtherMenu(player, ConstNpc.MENU_CHOOSE_LUCKY_ROUND,
                                        "Con có thể chọn từ 1 đến 7 viên\n"
                                                + "giá mỗi viên là 50 triệu vàng.\n"
                                                + "Ưu tiên dùng vé quay trước.",
                                        "Vòng quay\nVàng",
                                        "Rương phụ\nĐang có "
                                                + (player.inventory.itemsBoxCrackBall.size()
                                                        - InventoryService.gI()
                                                                .getCountEmptyListItem(
                                                                        player.inventory.itemsBoxCrackBall))
                                                + "\nmón",
                                        "Đóng");
                                break;
                        }
                        break;
                    case ConstNpc.MENU_CHOOSE_LUCKY_ROUND:
                        switch (select) {
                            case 0:
                                // LuckyRoundService.gI().openCrackBallUI(player,
                                // LuckyRoundService.USING_GOLD);
                                Service.gI().sendThongBao(player, "Chức năng đang bảo trì");
                                break;
                            case 1:
                                ShopService.gI().openBoxItemLuckyRound(player);
                                break;
                        }
                        break;
                    case ConstNpc.THACH_DAU_POPO:
                        switch (select) {
                            case 0:// đăng ký tập luyện tự động

                                break;
                            case 1:// luyện tập pôpô
                                player.activeYajiro = 1;
                                PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                player.thachDauNPC = 1;
                                break;
                            case 2:// thách đầu pôpô
                                player.thachDauNPC = 1;
                                player.activeYajiro = 1;
                                PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                player.thachDauNPC = 1;
                                break;
                            case 3:
                                this.createOtherMenu(player, ConstNpc.MENU_CHOOSE_LUCKY_ROUND,
                                        "Con có thể chọn từ 1 đến 7 viên\n"
                                                + "giá mỗi viên là 4 ngọc hoặc 50 triệu vàng.\n"
                                                + "Ưu tiên dùng vé quay trước.",
                                        "Vòng quay\nVàng", "Vòng quay\nMay mắn",
                                        "Rương phụ\nĐang có "
                                                + (player.inventory.itemsBoxCrackBall.size()
                                                        - InventoryService.gI()
                                                                .getCountEmptyListItem(
                                                                        player.inventory.itemsBoxCrackBall))
                                                + "\nmón",
                                        "Đóng");
                                break;
                        }
                        break;
                    case ConstNpc.THACH_DAU_THUONG_DE:
                        switch (select) {
                            case 0:
                                break;
                            case 1:
                                ChangeMapService.gI().changeMap(player, 49, 0, 384, 440);
                                try {
                                    Boss_ThuongDe dt = new Boss_ThuongDe(
                                            Util.createIdDuongTank((int) player.id),
                                            BossData.THUONG_DE, player.zone, this.cx, this.cy,
                                            (int) player.id);
                                } catch (Exception ex) {
                                    Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE,
                                            null, ex);
                                }
                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                player.zone.load_Me_To_Another(player);
                                break;
                            case 2:
                                ChangeMapService.gI().changeMap(player, 49, 0, 384, 440);
                                player.thachDauNPC = 1;
                                try {
                                    Boss_ThuongDe dt = new Boss_ThuongDe(
                                            Util.createIdDuongTank((int) player.id),
                                            BossData.THUONG_DE, player.zone, this.cx, this.cy,
                                            (int) player.id);
                                } catch (Exception ex) {
                                    Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE,
                                            null, ex);
                                }
                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                player.zone.load_Me_To_Another(player);
                                break;
                            case 3:
                                this.createOtherMenu(player, ConstNpc.MENU_CHOOSE_LUCKY_ROUND,
                                        "Con có thể chọn từ 1 đến 7 viên\n"
                                                + "giá mỗi viên là 4 ngọc hoặc 50 triệu vàng.\n"
                                                + "Ưu tiên dùng vé quay trước.",
                                        "Vòng quay\nVàng", "Vòng quay\nMay mắn",
                                        "Rương phụ\nĐang có "
                                                + (player.inventory.itemsBoxCrackBall.size()
                                                        - InventoryService.gI()
                                                                .getCountEmptyListItem(
                                                                        player.inventory.itemsBoxCrackBall))
                                                + "\nmón",
                                        "Đóng");
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
                }
            } else if (player.zone instanceof ZSnakeRoad) {
                if (mapId == ConstMap.CON_DUONG_RAN_DOC) {
                    ZSnakeRoad zroad = (ZSnakeRoad) player.zone;
                    if (zroad.isKilledAll()) {
                        SnakeRoad road = (SnakeRoad) zroad.getDungeon();
                        ZSnakeRoad egr = (ZSnakeRoad) road.find(ConstMap.THAN_DIEN);
                        egr.enter(player, 360, 408);
                        Service.getInstance().sendThongBao(player, "Hãy xuống gặp thần mèo Karin");
                    } else {
                        Service.getInstance().sendThongBao(player,
                                "Hãy tiêu diệt hết quái vật ở đây!");
                    }
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
