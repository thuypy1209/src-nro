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
 * NPC QuaTrung
 */
public class QuaTrung extends Npc {

    public QuaTrung(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }


    private final int COST_AP_TRUNG_NHANH = 1000000000;

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            player.mabuEgg.sendMabuEgg();
            if (player.mabuEgg.getSecondDone() != 0) {
                this.createOtherMenu(player, ConstNpc.CAN_NOT_OPEN_EGG,
                        "Hãy thu thập năng lượng\nbằng cách làm nhiệm vụ hằng ngày\ntại Bò Mộng từ mức độ khó trở lên\nđể trứng mau nở nhé.",
                        "Nở trứng\nnhanh\n1 tỷ vàng", "Hủy bỏ\ntrứng", "Đóng");
            } else {
                this.createOtherMenu(player, ConstNpc.CAN_OPEN_EGG,
                        "Hãy thu thập năng lượng\nbằng cách làm nhiệm vụ hằng ngày\ntại Bò Mộng từ mức độ khó trở lên\nđể trứng mau nở nhé.",
                        "Nở",
                        "Hủy bỏ\ntrứng", "Đóng");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.CAN_NOT_OPEN_EGG:
                    switch (select) {
                        case 0:
                            player.mabuEgg.timeDone = 0;
                            player.inventory.gold -= 1000000000;
                            Service.getInstance().sendMoney(player);
                            Service.getInstance().sendThongBao(player,
                                    "Đã nở trứng nhanh thành công");
                            break;
                        case 1:
                            this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_EGG,
                                    "Bạn có chắc chắn muốn hủy bỏ trứng Mabư?", "Đồng ý",
                                    "Từ chối");
                            break;
                    }
                    break;
                case ConstNpc.CAN_OPEN_EGG:
                    switch (select) {
                        case 0:
                            this.createOtherMenu(player, ConstNpc.CONFIRM_OPEN_EGG,
                                    "Bạn có chắc thay thế đệ tử hiện tại bằng Đệ tử Mabư",
                                    "Thay thế", "Từ chối");
                            break;
                        case 1:
                            this.createOtherMenu(player, ConstNpc.CONFIRM_DESTROY_EGG,
                                    "Bạn có chắc chắn muốn hủy bỏ trứng Mabư?", "Đồng ý",
                                    "Từ chối");
                            break;
                    }
                    break;
                case ConstNpc.CONFIRM_OPEN_EGG:
                    switch (select) {
                        case 0:
                            if (player.pet.nPoint.power >= 10000) {
                                player.mabuEgg.openEgg(player.pet.gender);
                            } else {
                                Service.gI().sendThongBao(player,
                                        "Yêu cầu đệ tử đạt 10k sức mạnh");
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case ConstNpc.CONFIRM_DESTROY_EGG:
                    if (select == 0) {
                        player.mabuEgg.destroyEgg();
                    }
                    break;
            }
        }
    }
}
