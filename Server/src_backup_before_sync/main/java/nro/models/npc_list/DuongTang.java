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
 * NPC DuongTang
 */
public class DuongTang extends Npc {

    public DuongTang(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (this.mapId == MapName.LANG_ARU) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "A mi phò phò, thí chủ hãy giúp giải cứu đồ đệ của bần tăng đang bị phong ấn tại ngũ hành sơn.",
                    "Đồng ý", "Từ chối");
        }
        if (this.mapId == MapName.NGU_HANH_SON_3) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "A mi phò phò, thí chủ hãy thu thập bùa 'giải khai phong ấn', mỗi chữ 10 cái.",
                    "Về\nLàng Aru", "Từ chối");
        }
        if (this.mapId == MapName.NGU_HANH_SON) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "A mi phò phò, thí chủ hãy thu thập bùa 'giải khai phong ấn', mỗi chữ 10 cái.",
                    "Đổi đào chín", "Giải phong ấn", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == MapName.LANG_ARU) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0 -> {
                            // if (!Manager.gI().getGameConfig().isOpenPrisonPlanet()) {
                            // Service.getInstance().sendThongBao(player,
                            // "Lối vào ngũ hành sơn chưa mở");
                            // return;
                            // }
                            // Zone zone = MapService.gI().getZoneJoinByMapIdAndZoneId(player, 124,
                            // 0);
                            // if (zone != null) {
                            // player.location.x = 100;
                            // player.location.y = 384;
                            // MapService.gI().goToMap(player, zone);
                            // Service.getInstance().clearMap(player);
                            // zone.mapInfo(player);
                            // player.zone.loadAnotherToMe(player);
                            // player.zone.load_Me_To_Another(player);
                            // }
                            Service.getInstance().sendThongBao(player,
                                    "Lối vào ngũ hành sơn chưa mở");
                        }
                    }
                }
            }
            if (this.mapId == MapName.NGU_HANH_SON_3) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0 -> {
                            Zone zone = MapService.gI().getZoneJoinByMapIdAndZoneId(player, 0, 0);
                            if (zone != null) {
                                player.location.x = 600;
                                player.location.y = 432;
                                MapService.gI().goToMap(player, zone);
                                Service.getInstance().clearMap(player);
                                zone.mapInfo(player);
                                player.zone.loadAnotherToMe(player);
                                player.zone.load_Me_To_Another(player);
                            }
                        }
                    }
                }
            }
            if (this.mapId == MapName.NGU_HANH_SON) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            // Đổi đào
                            Item item = InventoryService.gI().findItemBagByTemp(player,
                                    ConstItem.QUA_HONG_DAO);
                            if (item == null || item.quantity < 10) {
                                npcChat(player,
                                        "Cần 10 quả đào xanh để đổi lấy đào chín từ bần tăng.");
                                return;
                            }
                            if (InventoryService.gI().getCountEmptyBag(player) == 0) {
                                npcChat(player, "Túi đầy rồi kìa.");
                                return;
                            }
                            Item newItem = ItemService.gI()
                                    .createNewItem((short) ConstItem.QUA_HONG_DAO_CHIN, 1);
                            InventoryService.gI().subQuantityItemsBag(player, item, 10);
                            InventoryService.gI().addItemBag(player, newItem, 0);
                            InventoryService.gI().sendItemBags(player);
                            npcChat(player,
                                    "Ta đã đổi cho thí chủ rồi đó, hãy mang cho đệ tử ta đi nào.");
                            break;

                        case 1:
                            // giải phong ấn
                            if (InventoryService.gI().getCountEmptyBag(player) == 0) {
                                npcChat(player, "Túi đầy rồi kìa.");
                                return;
                            }
                            int[] itemsNeed = { ConstItem.CHU_GIAI, ConstItem.CHU_KHAI,
                                    ConstItem.CHU_PHONG, ConstItem.CHU_AN };
                            List<Item> items = InventoryService.gI().getListItem(player, itemsNeed)
                                    .stream().filter(i -> i.quantity >= 10)
                                    .collect(Collectors.toList());
                            boolean[] flags = new boolean[4];
                            for (Item i : items) {
                                switch ((int) i.template.id) {
                                    case ConstItem.CHU_GIAI:
                                        flags[0] = true;
                                        break;

                                    case ConstItem.CHU_KHAI:
                                        flags[1] = true;
                                        break;

                                    case ConstItem.CHU_PHONG:
                                        flags[2] = true;
                                        break;

                                    case ConstItem.CHU_AN:
                                        flags[3] = true;
                                        break;
                                }
                            }
                            for (int i = 0; i < flags.length; i++) {
                                if (!flags[i]) {
                                    ItemTemplate template = ItemService.gI()
                                            .getTemplate(itemsNeed[i]);
                                    npcChat("Thí chủ còn thiếu " + template.name);
                                    return;
                                }
                            }

                            for (Item i : items) {
                                InventoryService.gI().subQuantityItemsBag(player, i, 10);
                            }

                            RandomCollection<Integer> rc = new RandomCollection<>();
                            rc.add(10, ConstItem.CAI_TRANG_TON_NGO_KHONG_DE_TU);
                            rc.add(10, ConstItem.CAI_TRANG_BAT_GIOI_DE_TU);
                            rc.add(50, ConstItem.GAY_NHU_Y);
                            switch (player.gender) {
                                case ConstPlayer.TRAI_DAT:
                                    rc.add(30, ConstItem.CAI_TRANG_TON_NGO_KHONG);
                                    break;

                                case ConstPlayer.NAMEC:
                                    rc.add(30, ConstItem.CAI_TRANG_TON_NGO_KHONG_545);
                                    break;

                                case ConstPlayer.XAYDA:
                                    rc.add(30, ConstItem.CAI_TRANG_TON_NGO_KHONG_546);
                                    break;
                            }
                            int itemID = rc.next();
                            Item nItem = ItemService.gI().createNewItem((short) itemID);
                            boolean all = itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_DE_TU
                                    || itemID == ConstItem.CAI_TRANG_BAT_GIOI_DE_TU
                                    || itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG
                                    || itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_545
                                    || itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_546;
                            if (all) {
                                nItem.itemOptions.add(new ItemOption(50, Util.nextInt(20, 35)));
                                nItem.itemOptions.add(new ItemOption(77, Util.nextInt(20, 35)));
                                nItem.itemOptions.add(new ItemOption(103, Util.nextInt(20, 35)));
                                nItem.itemOptions.add(new ItemOption(94, Util.nextInt(5, 10)));
                                nItem.itemOptions.add(new ItemOption(100, Util.nextInt(10, 20)));
                                nItem.itemOptions.add(new ItemOption(101, Util.nextInt(10, 20)));
                            }
                            if (itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG
                                    || itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_545
                                    || itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_546) {
                                nItem.itemOptions.add(new ItemOption(80, Util.nextInt(5, 15)));
                                nItem.itemOptions.add(new ItemOption(81, Util.nextInt(5, 15)));
                                nItem.itemOptions.add(new ItemOption(106, 0));
                            } else if (itemID == ConstItem.CAI_TRANG_TON_NGO_KHONG_DE_TU
                                    || itemID == ConstItem.CAI_TRANG_BAT_GIOI_DE_TU) {
                                nItem.itemOptions.add(new ItemOption(197, 0));
                            }
                            if (all) {
                                if (Util.isTrue(499, 500)) {
                                    nItem.itemOptions.add(new ItemOption(93, Util.nextInt(3, 30)));
                                }
                            } else if (itemID == ConstItem.GAY_NHU_Y) {
                                RandomCollection<Integer> rc2 = new RandomCollection<>();
                                rc2.add(60, 30);
                                rc2.add(30, 90);
                                rc2.add(10, 365);
                                nItem.itemOptions.add(new ItemOption(93, rc2.next()));
                            }
                            InventoryService.gI().addItemBag(player, nItem, 0);
                            InventoryService.gI().sendItemBags(player);
                            npcChat(player.zone,
                                    "A mi phò phò, đa tạ thí chủ tương trợ, xin hãy nhận món quà mọn này, bần tăng sẽ niệm chú giải thoát cho Ngộ Không");
                            break;
                    }
                }
            }
        }
    }
}
