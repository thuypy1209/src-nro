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
 * NPC Cui
 */
public class Cui extends Npc {

    public Cui(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }


    private final int COST_FIND_BOSS = 20000000;

    @Override
    public void openBaseMenu(Player pl) {
        if (canOpenNpc(pl)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(pl, this)) {
                if (pl.playerTask.taskMain.id == 7) {
                    NpcService.gI().createTutorial(pl, this.avartar,
                            "Hãy lên đường cứu đứa bé nhà tôi\n"
                                    + "Chắc bây giờ nó đang sợ hãi lắm rồi");
                } else {
                    if (this.mapId == 19) {

                        int taskId = TaskService.gI().getIdTask(pl);
                        switch (taskId) {
                            case ConstTask.TASK_21_0:
                                this.createOtherMenu(pl, ConstNpc.MENU_FIND_KUKU,
                                        "Đội quân của Fide đang ở Thung lũng Nappa, ta sẽ đưa ngươi đến đó",
                                        "Đến chỗ\nKuku\n(" + Util.numberToMoney(COST_FIND_BOSS)
                                                + " vàng)",
                                        "Đến Cold", "Đến\nNappa", "Từ chối");
                                break;
                            case ConstTask.TASK_21_1:
                                this.createOtherMenu(pl, ConstNpc.MENU_FIND_MAP_DAU_DINH,
                                        "Đội quân của Fide đang ở Thung lũng Nappa, ta sẽ đưa ngươi đến đó",
                                        "Đến chỗ\nMập đầu đinh\n("
                                                + Util.numberToMoney(COST_FIND_BOSS) + " vàng)",
                                        "Đến Cold", "Đến\nNappa", "Từ chối");
                                break;
                            case ConstTask.TASK_21_2:
                                this.createOtherMenu(pl, ConstNpc.MENU_FIND_RAMBO,
                                        "Đội quân của Fide đang ở Thung lũng Nappa, ta sẽ đưa ngươi đến đó",
                                        "Đến chỗ\nRambo\n(" + Util.numberToMoney(COST_FIND_BOSS)
                                                + " vàng)",
                                        "Đến Cold", "Đến\nNappa", "Từ chối");
                                break;
                            default:
                                this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                        "Đội quân của Fide đang ở Thung lũng Nappa, ta sẽ đưa ngươi đến đó",
                                        "Đến Cold", "Đến\nNappa", "Từ chối");

                                break;
                        }
                    } else if (this.mapId == 68) {
                        this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                "Ngươi muốn về Thành Phố Vegeta", "Đồng ý", "Từ chối");
                    } else {
                        this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                "Tàu vũ trụ Xayda sử dụng công nghệ mới nhất, có thể đưa ngươi đi bất kỳ đâu, chỉ cần trả tiền là được.",
                                "Đến\nTrái Đất", "Đến\nNamếc", "Siêu thị");
                    }
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 26) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 24, -1, -1);
                            break;
                        case 1:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 25, -1, -1);
                            break;
                        case 2:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 84, -1, -1);
                            break;
                    }
                }
            }
            if (this.mapId == 19) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 109, -1, 295);
                            break;
                        case 1:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 68, -1, 90);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_FIND_KUKU) {
                    switch (select) {
                        case 0:
                            Boss boss = BossManager.gI().getBossById(BossFactory.KUKU);
                            if (boss != null && !boss.isDie()) {
                                if (player.inventory.gold >= COST_FIND_BOSS) {
                                    player.inventory.gold -= COST_FIND_BOSS;
                                    ChangeMapService.gI().changeMap(player, boss.zone,
                                            boss.location.x, boss.location.y);
                                    Service.getInstance().sendMoney(player);
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vàng, còn thiếu "
                                                    + Util.numberToMoney(
                                                            COST_FIND_BOSS - player.inventory.gold)
                                                    + " vàng");
                                }
                            }
                            break;
                        case 1:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 109, -1, 295);
                            break;
                        case 2:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 68, -1, 90);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_FIND_MAP_DAU_DINH) {
                    switch (select) {
                        case 0:
                            Boss boss = BossManager.gI().getBossById(BossFactory.MAP_DAU_DINH);
                            if (boss != null && !boss.isDie()) {
                                if (player.inventory.gold >= COST_FIND_BOSS) {
                                    player.inventory.gold -= COST_FIND_BOSS;
                                    ChangeMapService.gI().changeMap(player, boss.zone,
                                            boss.location.x, boss.location.y);
                                    Service.getInstance().sendMoney(player);
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vàng, còn thiếu "
                                                    + Util.numberToMoney(
                                                            COST_FIND_BOSS - player.inventory.gold)
                                                    + " vàng");
                                }
                            }
                            break;
                        case 1:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 109, -1, 295);
                            break;
                        case 2:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 68, -1, 90);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_FIND_RAMBO) {
                    switch (select) {
                        case 0:
                            Boss boss = BossManager.gI().getBossById(BossFactory.RAMBO);
                            if (boss != null && !boss.isDie()) {
                                if (player.inventory.gold >= COST_FIND_BOSS) {
                                    player.inventory.gold -= COST_FIND_BOSS;
                                    ChangeMapService.gI().changeMap(player, boss.zone,
                                            boss.location.x, boss.location.y);
                                    Service.getInstance().sendMoney(player);
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vàng, còn thiếu "
                                                    + Util.numberToMoney(
                                                            COST_FIND_BOSS - player.inventory.gold)
                                                    + " vàng");
                                }
                            }
                            break;
                        case 1:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 109, -1, 295);
                            break;
                        case 2:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 68, -1, 90);
                            break;
                    }
                }
            }
            if (this.mapId == 68) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 19, -1, 1100);
                            break;
                    }
                }
            }
        }
    }
}
