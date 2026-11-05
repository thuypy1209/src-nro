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
 * NPC NoiBanh
 */
public class NoiBanh extends Npc {

    public NoiBanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào " + player.name + "\nTôi là nồi nấu bánh\nTôi có thể giúp gì cho bạn",
                    "Làm\nBánh Tét", "Làm\nBánh Chưng", getMenuLamBanh(player, 0),
                    getMenuLamBanh(player, 1), "Đổi Hộp\nQuà Tết");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.BASE_MENU:
                    switch (select) {
                        case 0:
                            Item thitBaChi = InventoryService.gI().findItem(player,
                                    ConstItem.THIT_BA_CHI, 99);
                            Item gaoNep = InventoryService.gI().findItem(player, ConstItem.GAO_NEP,
                                    99);
                            Item doXanh = InventoryService.gI().findItem(player, ConstItem.DO_XANH,
                                    99);
                            Item laChuoi = InventoryService.gI().findItem(player,
                                    ConstItem.LA_CHUOI, 99);
                            if (thitBaChi != null && gaoNep != null && doXanh != null
                                    && laChuoi != null) {
                                InventoryService.gI().subQuantityItemsBag(player, thitBaChi, 99);
                                InventoryService.gI().subQuantityItemsBag(player, gaoNep, 99);
                                InventoryService.gI().subQuantityItemsBag(player, doXanh, 99);
                                InventoryService.gI().subQuantityItemsBag(player, laChuoi, 99);
                                Item banhtet = ItemService.gI()
                                        .createNewItem((short) ConstItem.BANH_TET_2023);
                                banhtet.itemOptions.add(new ItemOption(74, 0));
                                InventoryService.gI().addItemBag(player, banhtet, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player,
                                        "Bạn nhận được Bánh Tét");
                            } else {
                                Service.getInstance().sendThongBao(player, "Không đủ nguyên liệu");
                            }
                            break;
                        case 1:
                            Item thitHeo1 = InventoryService.gI().findItem(player,
                                    ConstItem.THIT_HEO_2023, 99);
                            Item gaoNep1 = InventoryService.gI().findItem(player, ConstItem.GAO_NEP,
                                    99);
                            Item doXanh1 = InventoryService.gI().findItem(player, ConstItem.DO_XANH,
                                    99);
                            Item laDong1 = InventoryService.gI().findItem(player,
                                    ConstItem.LA_DONG_2023, 99);
                            if (thitHeo1 != null && gaoNep1 != null && doXanh1 != null
                                    && laDong1 != null) {
                                InventoryService.gI().subQuantityItemsBag(player, thitHeo1, 99);
                                InventoryService.gI().subQuantityItemsBag(player, gaoNep1, 99);
                                InventoryService.gI().subQuantityItemsBag(player, doXanh1, 99);
                                InventoryService.gI().subQuantityItemsBag(player, laDong1, 99);
                                Item banhChung = ItemService.gI()
                                        .createNewItem((short) ConstItem.BANH_CHUNG_2023);
                                banhChung.itemOptions.add(new ItemOption(74, 0));
                                InventoryService.gI().addItemBag(player, banhChung, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player,
                                        "Bạn nhận được Bánh Chưng");
                            } else {
                                Service.getInstance().sendThongBao(player, "Không đủ nguyên liệu");
                            }
                            break;
                        case 2:
                            if (!player.event.isCookingTetCake()) {
                                Item banhTet2 = InventoryService.gI().findItem(player,
                                        ConstItem.BANH_TET_2023, 1);
                                Item phuGiaTaoMau2 = InventoryService.gI().findItem(player,
                                        ConstItem.PHU_GIA_TAO_MAU, 1);
                                Item giaVi2 = InventoryService.gI().findItem(player,
                                        ConstItem.GIA_VI_TONG_HOP, 1);

                                if (banhTet2 != null && phuGiaTaoMau2 != null && giaVi2 != null) {
                                    InventoryService.gI().subQuantityItemsBag(player, banhTet2, 1);
                                    InventoryService.gI().subQuantityItemsBag(player, phuGiaTaoMau2,
                                            1);
                                    InventoryService.gI().subQuantityItemsBag(player, giaVi2, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    player.event.setTimeCookTetCake(300);
                                    player.event.setCookingTetCake(true);
                                    Service.getInstance().sendThongBao(player,
                                            "Bắt đầu nấu bánh,thời gian nấu bánh là 5 phút");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ nguyên liệu");
                                }
                            } else if (player.event.isCookingTetCake()
                                    && player.event.getTimeCookTetCake() == 0) {
                                Item cake = ItemService.gI()
                                        .createNewItem((short) ConstItem.BANH_TET_CHIN, 1);
                                cake.itemOptions.add(new ItemOption(77, 20));
                                cake.itemOptions.add(new ItemOption(103, 20));
                                cake.itemOptions.add(new ItemOption(74, 0));
                                InventoryService.gI().addItemBag(player, cake, 0);
                                InventoryService.gI().sendItemBags(player);
                                player.event.setCookingTetCake(false);
                                player.event.addEventPoint(1);
                                Service.getInstance().sendThongBao(player,
                                        "Bạn nhận được Bánh Tét (đã chính) và 1 điểm sự kiện");
                            }
                            break;
                        case 3:
                            if (!player.event.isCookingChungCake()) {
                                Item banhChung3 = InventoryService.gI().findItem(player,
                                        ConstItem.BANH_CHUNG_2023, 1);
                                Item phuGiaTaoMau3 = InventoryService.gI().findItem(player,
                                        ConstItem.PHU_GIA_TAO_MAU, 1);
                                Item giaVi3 = InventoryService.gI().findItem(player,
                                        ConstItem.GIA_VI_TONG_HOP, 1);

                                if (banhChung3 != null && phuGiaTaoMau3 != null && giaVi3 != null) {
                                    InventoryService.gI().subQuantityItemsBag(player, banhChung3,
                                            1);
                                    InventoryService.gI().subQuantityItemsBag(player, phuGiaTaoMau3,
                                            1);
                                    InventoryService.gI().subQuantityItemsBag(player, giaVi3, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    player.event.setTimeCookChungCake(300);
                                    player.event.setCookingChungCake(true);
                                    Service.getInstance().sendThongBao(player,
                                            "Bắt đầu nấu bánh,thời gian nấu bánh là 5 phút");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ nguyên liệu");
                                }
                            } else if (player.event.isCookingChungCake()
                                    && player.event.getTimeCookChungCake() == 0) {
                                Item cake = ItemService.gI()
                                        .createNewItem((short) ConstItem.BANH_CHUNG_CHIN, 1);
                                cake.itemOptions.add(new ItemOption(50, 20));
                                cake.itemOptions.add(new ItemOption(5, 15));
                                cake.itemOptions.add(new ItemOption(74, 0));
                                InventoryService.gI().addItemBag(player, cake, 0);
                                InventoryService.gI().sendItemBags(player);
                                player.event.setCookingChungCake(false);
                                player.event.addEventPoint(1);
                                Service.getInstance().sendThongBao(player,
                                        "Bạn nhận được Bánh Chưng (đã chín) và 1 điểm sự kiện");
                            }
                            break;
                        case 4:
                            Item tetCake = InventoryService.gI().findItem(player,
                                    ConstItem.BANH_TET_CHIN, 5);
                            Item chungCake = InventoryService.gI().findItem(player,
                                    ConstItem.BANH_CHUNG_CHIN, 5);
                            if (chungCake != null && tetCake != null) {
                                Item hopQua = ItemService.gI()
                                        .createNewItem((short) ConstItem.HOP_QUA_TET_2023, 1);
                                hopQua.itemOptions.add(new ItemOption(30, 0));
                                hopQua.itemOptions.add(new ItemOption(74, 0));

                                InventoryService.gI().subQuantityItemsBag(player, tetCake, 5);
                                InventoryService.gI().subQuantityItemsBag(player, chungCake, 5);
                                InventoryService.gI().addItemBag(player, hopQua, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player,
                                        "Bạn nhận được Hộp quà tết");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Không đủ nguyên liệu để đổi");
                            }
                            break;
                    }
                    break;
            }
        }
    }
}
