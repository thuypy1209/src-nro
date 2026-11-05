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
 * NPC Osin
 */
public class Osin extends Npc {

    public Osin(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 50) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ta có thể giúp gì cho ngươi ?",
                        "Đến\nKaio", "Đến\nhành tinh\nBill", "Từ chối");
            } else if (this.mapId == 52) {
                if (MabuWar.gI().isTimeMabuWar()) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Bây giờ tôi sẽ bí mật...\n đuổi theo 2 tên đồ tể... \n"
                                    + "Quý vị nào muốn đi theo thì xin mời !",
                            "Ok", "Chê");
                } else {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Vào lúc 12h tôi sẽ bí mật...\n đuổi theo 2 tên đồ tể... \n"
                                    + "Quý vị nào muốn đi theo thì xin mời !",
                            "Ok");
                }
            } else if (this.mapId == 154) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ta có thể giúp gì cho ngươi ?",
                        "Về thánh địa", "Đến\nhành tinh\nngục tù", "Từ chối");
            } else if (this.mapId == 155) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ta có thể giúp gì cho ngươi ?",
                        "Quay về", "Từ chối");
            } else if (MapService.gI().isMapMabuWar(this.mapId)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Đừng vội xem thường Babyđây,ngay đến cha hắn là thần ma đạo sĩ\n"
                                + "Bibiđây khi còn sống cũng phải sợ hắn đấy",
                        "Giải trừ\nphép thuật\n50Tr Vàng",
                        player.zone.map.mapId != 120 ? "Xuống\nTầng Dưới" : "Rời\nKhỏi đây");
            } else {
                super.openBaseMenu(player);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 50) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            ChangeMapService.gI().changeMap(player, 48, -1, 354, 240);
                            break;
                        case 1:
                            ChangeMapService.gI().changeMap(player, 154, -1, 200, 312);
                            break;
                    }
                }
            } else if (this.mapId == 52) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            ChangeMapService.gI().changeMap(player, 114, -1, 354, 240);
                            break;
                    }
                }
            } else if (this.mapId == 154) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            ChangeMapService.gI().changeMap(player, 50, -1, 318, 336);
                            break;
                        case 1:
                            if (!Manager.gI().getGameConfig().isOpenPrisonPlanet()) {
                                Service.getInstance().sendThongBao(player,
                                        "Lối vào hành tinh ngục tù chưa mở");
                                return;
                            }
                            if (player.nPoint.power < 60000000000L) {
                                Service.getInstance().sendThongBao(player,
                                        "Yêu cầu tối thiếu 60tỷ sức mạnh");
                                return;
                            }
                            ChangeMapService.gI().changeMap(player, 155, -1, 111, 792);
                            break;
                    }
                }
            } else if (this.mapId == 155) {
                if (player.iDMark.isBaseMenu()) {
                    if (select == 0) {
                        ChangeMapService.gI().changeMap(player, 154, -1, 200, 312);
                    }
                }
            } else if (MapService.gI().isMapMabuWar(this.mapId)) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            if (player.inventory.getGold() >= 50000000) {
                                Service.getInstance().changeFlag(player, 9);
                                player.inventory.subGold(50000000);

                            } else {
                                Service.getInstance().sendThongBao(player, "Không đủ vàng");
                            }
                            break;
                        case 1:
                            if (player.zone.map.mapId == 120) {
                                ChangeMapService.gI().changeMapBySpaceShip(player,
                                        player.gender + 21, -1, 250);
                            }
                            if (player.cFlag == 9) {
                                if (player.getPowerPoint() >= 20) {
                                    if (!(player.zone.map.mapId == 119)) {
                                        int idMapNextFloor = player.zone.map.mapId == 115
                                                ? player.zone.map.mapId + 2
                                                : player.zone.map.mapId + 1;
                                        ChangeMapService.gI().changeMap(player, idMapNextFloor, -1,
                                                354, 240);
                                    } else {
                                        Zone zone = MabuWar.gI().getMapLastFloor(120);
                                        if (zone != null) {
                                            ChangeMapService.gI().changeMap(player, zone, 354, 240);
                                        } else {
                                            Service.getInstance().sendThongBao(player,
                                                    "Trận đại chiến đã kết thúc, tàu vận chuyển sẽ đưa bạn về nhà");
                                        }
                                    }
                                    player.resetPowerPoint();
                                    player.sendMenuGotoNextFloorMabuWar = false;
                                    Service.getInstance().sendPowerInfo(player, "%",
                                            player.getPowerPoint());
                                    if (Util.isTrue(1, 30)) {
                                        player.inventory.ruby += 1;
                                        PlayerService.gI().sendInfoHpMpMoney(player);
                                        Service.getInstance().sendThongBao(player,
                                                "Bạn nhận được 1 Hồng Ngọc");
                                    } else {
                                        Service.getInstance().sendThongBao(player,
                                                "Bạn đen vô cùng luôn nên không nhận được gì cả");
                                    }
                                } else {
                                    this.npcChat(player,
                                            "Ngươi cần có đủ điểm để xuống tầng tiếp theo");
                                }
                                break;
                            } else {
                                this.npcChat(player,
                                        "Ngươi đang theo phe Babiđây,Hãy qua bên đó mà thể hiện");
                            }
                    }
                }
            }
        }
    }
}
