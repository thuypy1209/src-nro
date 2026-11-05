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
 * NPC Santa
 */
public class Santa extends Npc {

    public Santa(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào, ta có một số vật phẩm đặt biệt cậu có muốn xem không?",
                    "Cửa hàng",
                    "Mở rộng\nHành trang\nRương đồ",
                    "Nhập mã\n quà tặng",
                    "Cửa hàng\nĐặt biệt");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 5 || this.mapId == 13 || this.mapId == 20) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0: // shop
                            this.openShopWithGender(player, ConstNpc.SHOP_SANTA_0, 0);
                            break;
                        case 1:
                            this.openShopWithGender(player, ConstNpc.SHOP_SANTA_2, 2);
                            break;
                        case 2: // giftcode
                            Input.gI().createFormGiftCode(player);
                            break;

                        case 3: // tiệm hớt tóc
                            this.openShopWithGender1(player, ConstNpc.SHOP_SANTA_1, 1);
                            // this.openShopWithGender(player, ConstNpc.SHOP_SANTA_1, 1);
                            break;
                        case 4:
                            this.createOtherMenu(player, ConstNpc.MENU_SU_KIEN,
                                    "Sự kiện hấp dẫn đang chờ đón bạn!!!\n"
                                            + "Con muốn làm gì nào ?\n",
                                    "Quay cải\nTrang", "Đóng");
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_SU_KIEN) {
                    switch (select) {
                        case 0:
                            this.createOtherMenu(player, ConstNpc.MENU_QUAY_CT,
                                    "Chào bạn! Chúng tôi có nhiều cải trang đồng giá 555 thỏi vàng để bạn lựa chọn.\n"
                                            + "Hit Ramdom 50-120% Ramdom SĐCM 1-10% SĐ, HP, KI\nGoten Ramdom  60-90% HP Ramdom  20-30% SĐ, KI\nFu Ramdom  60-90% Ki Ramdom  20-30% SĐ, HP",
                                    "Cải trang\nhit", "Cải trang\ngoten",
                                    "Cải trang\nfu", "Đóng");
                            break;
                        // case 1:
                        // this.createOtherMenu(player, ConstNpc.MENU_DOI_CANH,
                        // "Hãy đem cho ta 10 viên vũ thạch và 999 bí kiếp ta sẽ tặng lại cậu.\n"
                        // + "Những cặp cánh với chỉ số Random từ 10-30% HP, KI, SĐ và đặt biệt là
                        // chỉ số dịch chuyển tức thời\n May mắn sẽ ra vỉnh viễn",
                        // "Cánh hắc\nÁm", "Cánh mặt\nTrời",
                        // "Cánh vinh\nQuang", "Đóng");
                        // break;
                        // case 2:
                        // this.createOtherMenu(player, ConstNpc.MENU_TRUNG_THU,
                        // "Mang 999 bí kiếp, 99 củ cà rốt và 15 bánh trung thu đặc biệt, rồi tôi sẽ
                        // cho cậu.\n"
                        // + "Hộp quà trung thu, mở ra sẽ nhận được 1 điểm đua top sẽ được cập nhật
                        // liên tực tại website nronotbuff.com và thú cưỡi Ramdom 5-15% SĐ,HP,KI\n"
                        // + "Hoặc x999 củ cà rốt để đổi lấy\n"
                        // + "Lồng đèn Ông Sao [Ramdom 10-20% SĐ,HP,KI Vv]\n"
                        // + "Lồng đèn Cá chép [Ramdom 10-20% SĐ,HP,KI Vv]\n"
                        // + "Lồng đèn Kéo Quân [Ramdom 10-20% SĐ,HP,KI Vv]\n"
                        // + "Lồng đèn Ông trăng [Ramdom 10-20% SĐ,HP,KI Vv]\n"
                        // + "Lồng đèn Hội An [Ramdom 10-20% SĐ,HP,KI Vv]\n"
                        // + "Sự kiện sẽ diễn ra đến hết ngày 01/10/2024",
                        // "Đổi hộp\nQuà", "Lòng đèn\nÔng sao", "Lòng dèn\nCá chép", "Lòng dèn\nKéo
                        // Quân", "Lòng dèn\nÔng trăng", "Lòng dèn\nHội An", "Đóng");
                        // break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_TRUNG_THU) {
                    switch (select) {
                        case 0: // Đổi hộp quà trung thu
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item carot = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 462);
                                Item banhtrungthudacbiet = InventoryService.gI()
                                        .findItemBagByTemp(player, (short) 472);
                                Item bikiep = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 590);
                                Item hopquatrungthu = InventoryService.gI()
                                        .findItemBagByTemp(player, (short) 2135);

                                // Kiểm tra hộp quà trung thu có tồn tại trong hành trang không
                                if (hopquatrungthu != null) {
                                    Service.getInstance().sendThongBao(player,
                                            "Vui lòng mở xong hộp quà trong hành trang rồi tiếp tục");
                                } else if (carot == null || banhtrungthudacbiet == null
                                        || bikiep == null) {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                } else if (bikiep.quantity >= 999 && carot.quantity >= 99
                                        && banhtrungthudacbiet.quantity >= 15) {
                                    bikiep.quantity -= 999;
                                    carot.quantity -= 99;
                                    banhtrungthudacbiet.quantity -= 15;
                                    Item canhhacam = ItemService.gI().createNewItem((short) 2135,
                                            1);
                                    canhhacam.itemOptions
                                            .add(new ItemOption(30, Util.nextInt(0, 0)));
                                    InventoryService.gI().addItemBag(player, canhhacam, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Đổi thành công hộp quà trung thu");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;
                        case 1:
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item carot = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 462);
                                if (carot == null) {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                } else if (carot.quantity >= 999) {
                                    carot.quantity -= 999;
                                    Item longden = ItemService.gI().createNewItem((short) 467, 1);
                                    longden.itemOptions
                                            .add(new ItemOption(50, Util.nextInt(10, 20)));
                                    longden.itemOptions
                                            .add(new ItemOption(77, Util.nextInt(10, 20)));
                                    longden.itemOptions
                                            .add(new ItemOption(103, Util.nextInt(10, 20)));
                                    InventoryService.gI().addItemBag(player, longden, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Đổi thành công lòng đèn");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ củ cà rót để quy đổi");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;
                        case 2:
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item carot = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 462);
                                if (carot == null) {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                } else if (carot.quantity >= 999) {
                                    carot.quantity -= 999;
                                    Item longden = ItemService.gI().createNewItem((short) 468, 1);
                                    longden.itemOptions
                                            .add(new ItemOption(50, Util.nextInt(10, 20)));
                                    longden.itemOptions
                                            .add(new ItemOption(77, Util.nextInt(10, 20)));
                                    longden.itemOptions
                                            .add(new ItemOption(103, Util.nextInt(10, 20)));
                                    InventoryService.gI().addItemBag(player, longden, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Đổi thành công lòng đèn");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ củ cà rót để quy đổi");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;

                        case 3:
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item carot = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 462);
                                if (carot == null) {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                } else if (carot.quantity >= 999) {
                                    carot.quantity -= 999;
                                    Item longden = ItemService.gI().createNewItem((short) 469, 1);
                                    longden.itemOptions
                                            .add(new ItemOption(50, Util.nextInt(10, 20)));
                                    longden.itemOptions
                                            .add(new ItemOption(77, Util.nextInt(10, 20)));
                                    longden.itemOptions
                                            .add(new ItemOption(103, Util.nextInt(10, 20)));
                                    InventoryService.gI().addItemBag(player, longden, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Đổi thành công lòng đèn");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ củ cà rót để quy đổi");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;

                        case 4:
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item carot = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 462);
                                if (carot == null) {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                } else if (carot.quantity >= 999) {
                                    carot.quantity -= 999;
                                    Item longden = ItemService.gI().createNewItem((short) 470, 1);
                                    longden.itemOptions
                                            .add(new ItemOption(50, Util.nextInt(10, 20)));
                                    longden.itemOptions
                                            .add(new ItemOption(77, Util.nextInt(10, 20)));
                                    longden.itemOptions
                                            .add(new ItemOption(103, Util.nextInt(10, 20)));
                                    InventoryService.gI().addItemBag(player, longden, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Đổi thành công lòng đèn");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ củ cà rót để quy đổi");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;

                        case 5:
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item carot = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 462);
                                if (carot == null) {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                } else if (carot.quantity >= 999) {
                                    carot.quantity -= 999;
                                    Item longden = ItemService.gI().createNewItem((short) 471, 1);
                                    longden.itemOptions
                                            .add(new ItemOption(50, Util.nextInt(10, 20)));
                                    longden.itemOptions
                                            .add(new ItemOption(77, Util.nextInt(10, 20)));
                                    longden.itemOptions
                                            .add(new ItemOption(103, Util.nextInt(10, 20)));
                                    InventoryService.gI().addItemBag(player, longden, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Đổi thành công lòng đèn");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ củ cà rót để quy đổi");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;
                    }

                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_DOI_CANH) {
                    switch (select) {
                        case 0: // cánh hắc ám
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item bikiep = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 590);
                                Item vuthach = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 2130);

                                // Kiểm tra bikiep và vuthach có phải là null không
                                if (bikiep == null || vuthach == null) {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                } else if (bikiep.quantity >= 999 && vuthach.quantity >= 10) {
                                    bikiep.quantity -= 999;
                                    vuthach.quantity -= 10;

                                    Item canhhacam = ItemService.gI().createNewItem((short) 2133,
                                            1);
                                    double tile = Math.random();
                                    if (tile < 0.1) {

                                    } else {
                                        canhhacam.itemOptions
                                                .add(new ItemOption(93, Util.nextInt(1, 15)));
                                    }
                                    canhhacam.itemOptions
                                            .add(new ItemOption(50, Util.nextInt(10, 30)));
                                    canhhacam.itemOptions
                                            .add(new ItemOption(77, Util.nextInt(10, 30)));
                                    canhhacam.itemOptions
                                            .add(new ItemOption(103, Util.nextInt(10, 30)));
                                    canhhacam.itemOptions
                                            .add(new ItemOption(33, Util.nextInt(0, 0)));
                                    InventoryService.gI().addItemBag(player, canhhacam, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Đổi thành công cánh hắc ám");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;

                        case 1: // cánh mặt trời
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item bikiep = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 590);
                                Item vuthach = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 2130);

                                // Kiểm tra bikiep và vuthach có phải là null không
                                if (bikiep == null || vuthach == null) {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                } else if (bikiep.quantity >= 999 && vuthach.quantity >= 10) {
                                    bikiep.quantity -= 999;
                                    vuthach.quantity -= 10;

                                    Item canhmattroi = ItemService.gI().createNewItem((short) 2132,
                                            1);
                                    double tile = Math.random();
                                    if (tile < 0.1) {

                                    } else {
                                        canhmattroi.itemOptions
                                                .add(new ItemOption(93, Util.nextInt(1, 15)));
                                    }
                                    canhmattroi.itemOptions
                                            .add(new ItemOption(50, Util.nextInt(10, 30)));
                                    canhmattroi.itemOptions
                                            .add(new ItemOption(77, Util.nextInt(10, 30)));
                                    canhmattroi.itemOptions
                                            .add(new ItemOption(103, Util.nextInt(10, 30)));
                                    canhmattroi.itemOptions
                                            .add(new ItemOption(33, Util.nextInt(0, 0)));
                                    InventoryService.gI().addItemBag(player, canhmattroi, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Đổi thành công cánh mặt trời");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;
                        case 2: // cánh vinh quang
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item bikiep = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 590);
                                Item vuthach = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 2130);

                                // Kiểm tra bikiep và vuthach có phải là null không
                                if (bikiep == null || vuthach == null) {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                } else if (bikiep.quantity >= 999 && vuthach.quantity >= 10) {
                                    bikiep.quantity -= 999;
                                    vuthach.quantity -= 10;

                                    Item canhvinhquang = ItemService.gI().createNewItem(
                                            (short) 2131,
                                            1);
                                    double tile = Math.random();
                                    if (tile < 0.1) {

                                    } else {
                                        canhvinhquang.itemOptions
                                                .add(new ItemOption(93, Util.nextInt(1, 15)));
                                    }
                                    canhvinhquang.itemOptions
                                            .add(new ItemOption(50, Util.nextInt(10, 30)));
                                    canhvinhquang.itemOptions
                                            .add(new ItemOption(77, Util.nextInt(10, 30)));
                                    canhvinhquang.itemOptions
                                            .add(new ItemOption(103, Util.nextInt(10, 30)));
                                    canhvinhquang.itemOptions
                                            .add(new ItemOption(33, Util.nextInt(0, 0)));
                                    InventoryService.gI().addItemBag(player, canhvinhquang, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Đổi thành công cánh vinh quang");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Không đủ vật phẩm để quy đổi");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_QUAY_CT) {
                    switch (select) {
                        case 0: // Hit
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item thoivang = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 457);
                                if (thoivang.quantity > 554) {
                                    thoivang.quantity -= 555;
                                    Item hit = ItemService.gI().createNewItem((short) 884, 1);
                                    double tile = Math.random();
                                    if (tile < 0.2) {
                                        hit.itemOptions
                                                .add(new ItemOption(5, Util.nextInt(70, 120)));
                                    } else {
                                        hit.itemOptions
                                                .add(new ItemOption(5, Util.nextInt(50, 70)));
                                    }
                                    hit.itemOptions.add(new ItemOption(50, Util.nextInt(1, 10)));
                                    hit.itemOptions.add(new ItemOption(77, Util.nextInt(1, 10)));
                                    hit.itemOptions.add(new ItemOption(103, Util.nextInt(1, 10)));
                                    InventoryService.gI().addItemBag(player, hit, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Mua thành công cải trang hit");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Cần có 555 thỏi vàng để mua vật phẩm này");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;
                        case 1: // Goten
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item thoivang = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 457);
                                if (thoivang.quantity > 554) {
                                    thoivang.quantity -= 555;
                                    Item goten = ItemService.gI().createNewItem((short) 2128, 1);
                                    double tile = Math.random();
                                    if (tile < 0.2) {
                                        goten.itemOptions
                                                .add(new ItemOption(77, Util.nextInt(70, 90)));
                                    } else {
                                        goten.itemOptions
                                                .add(new ItemOption(77, Util.nextInt(60, 70)));
                                    }
                                    goten.itemOptions.add(new ItemOption(50, Util.nextInt(20, 30)));
                                    goten.itemOptions
                                            .add(new ItemOption(103, Util.nextInt(20, 30)));
                                    InventoryService.gI().addItemBag(player, goten, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Mua thành công cải trang goten");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Cần có 555 thỏi vàng để mua vật phẩm này");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;
                        case 2: // Fu
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                Item thoivang = InventoryService.gI().findItemBagByTemp(player,
                                        (short) 457);
                                if (thoivang.quantity > 554) {
                                    thoivang.quantity -= 555;
                                    Item fu = ItemService.gI().createNewItem((short) 2129, 1);
                                    double tile = Math.random();
                                    if (tile < 0.2) {
                                        fu.itemOptions
                                                .add(new ItemOption(103, Util.nextInt(70, 90)));
                                    } else {
                                        fu.itemOptions
                                                .add(new ItemOption(103, Util.nextInt(60, 70)));
                                    }
                                    fu.itemOptions.add(new ItemOption(50, Util.nextInt(20, 30)));
                                    fu.itemOptions.add(new ItemOption(77, Util.nextInt(20, 30)));

                                    InventoryService.gI().addItemBag(player, fu, 1);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.getInstance().sendThongBao(player,
                                            "Mua thành công cải trang fu");
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Cần có 555 thỏi vàng để mua vật phẩm này");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player, "Hành trang đã đầy");
                            }
                            break;
                    }
                }
            }
        }
    }
}
