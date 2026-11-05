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
 * NPC Whis
 */
public class Whis extends Npc {

    public Whis(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        switch (mapId) {
            case 5:
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Ta là Whis được Đại thiên sứ cử xuống Trái đất để thu thập lại các trang bị Thần linh bị kẻ xấu đánh cắp. Ta sẽ bạn lại cho ngươi trang bị kích hoạt trong truyền thuyết nếu ngươi giao cho ta trang bị Thần linh.",
                        "Hiến tế\nThần linh", "Hướng\ndẫn", "Đóng");
                return;
            case 154:
                createOtherMenu(player, ConstNpc.BASE_MENU, "Thử đánh ta xem nào.\n"
                        + "Ngươi còn 1 lượt nữa cơ mà.", "Nói chuyện", "Học\ntuyệt kỹ", "Top 100",
                        "[LV:" + player.levelKillWhis + "]");
                return;
        }

    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.BASE_MENU:
                    switch (mapId) {
                        case 154:
                            switch (select) {
                                case 0: // nói chuyện => chế tạo đồ thiên thứ

                                    break;
                                case 1: // Học tuyệt kỹ
                                    Item biKipTuyetKy = InventoryService.gI()
                                            .findItemBagByTemp(player, (short) 1229);
                                    if (biKipTuyetKy != null && biKipTuyetKy.quantity >= 9999
                                            && player.inventory.gold >= 10_000_000
                                            && player.inventory.gem >= 99) {
                                        int skillID = player.gender == 0 ? 24
                                                : player.gender == 1 ? 26 : 25;
                                        Skill newSkill = SkillUtil.createSkill(skillID, 1);
                                        String npcSay = "|1|Qua sẽ dạy ngươi tuyệt kỹ "
                                                + newSkill.template.name + "\n";
                                        npcSay += "|2|" + biKipTuyetKy.getName() + " "
                                                + biKipTuyetKy.quantity + "/9999\n";
                                        npcSay += "Giá vàng: 10.000.000\n";
                                        npcSay += "Giá ngọc: 99";
                                        createOtherMenu(player, ConstNpc.HOC_TUYET_KY, npcSay,
                                                "Đồng ý", "Từ chối");
                                        return;
                                    } else {
                                        int skillID = player.gender == 0 ? 24
                                                : player.gender == 1 ? 26 : 25;
                                        Skill newSkill = SkillUtil.createSkill(skillID, 1);
                                        String npcSay = "|1|Qua sẽ dạy ngươi tuyệt kỹ "
                                                + newSkill.template.name + " 1\n";
                                        if (biKipTuyetKy == null || biKipTuyetKy.quantity < 9999) {
                                            if (biKipTuyetKy == null) {
                                                npcSay += "|7|Bí kíp tuyệt kỹ" + " " + "0/9999\n";
                                            } else {
                                                npcSay += "|7|Bí kíp tuyệt kỹ" + " "
                                                        + biKipTuyetKy.quantity + "/9999\n";
                                            }
                                        } else {
                                            npcSay += "|2|" + biKipTuyetKy.getName() + " "
                                                    + biKipTuyetKy.quantity + "/9999\n";
                                        }
                                        if (player.inventory.gold < 10_000_000) {
                                            npcSay += "|7|Giá vàng: 10.000.000\n";
                                        } else {
                                            npcSay += "|2|Giá vàng: 10.000.000\n";
                                        }
                                        if (player.inventory.gem < 99) {
                                            npcSay += "|7|Giá ngọc: 99";
                                        } else {
                                            npcSay += "|2|Giá ngọc: 99";
                                        }
                                        createOtherMenu(player, ConstNpc.HOC_TUYET_KY_2, npcSay,
                                                "Từ chối");
                                        return;
                                    }

                                case 2: // Top đánh NPC whis
                                    Service.getInstance().showToplevelWhis(player);
                                    break;
                                case 3:// khiêu chiến NPC whis
                                    player.lastTimeSwapWhis = System.currentTimeMillis();
                                    PlayerService.gI().savePlayer(player);

                                    ScheduledExecutorService scheduler = Executors
                                            .newSingleThreadScheduledExecutor();

                                    Runnable task = () -> {
                                        hide_npc(player, (int) ConstNpc.WHIS, 0);
                                        PlayerService.gI().changeAndSendTypePK(player,
                                                ConstPlayer.PK_PVP);
                                        PlayerService.gI().playerMove(player, 485, 360);
                                        PlayerService.gI().setPos(player, 488, 360, 55);
                                        scheduler.shutdown();
                                    };
                                    scheduler.schedule(task, 1, TimeUnit.SECONDS);

                                    try {
                                        Boss_Whis dt = new Boss_Whis(
                                                Util.createIdDuongTank((int) player.id),
                                                BossData.WHIS_NPC, player.zone, this.cx, this.cy,
                                                player);
                                    } catch (Exception ex) {
                                        Logger.getLogger(NpcFactory.class.getName())
                                                .log(Level.SEVERE, null, ex);
                                    }
                                    player.zone.load_Me_To_Another(player);
                            }
                            return;
                        case 5:
                            switch (select) {
                                case 0:
                                    createOtherMenu(player, 799455479,
                                            "Ngươi muốn hiến tế cho Bản thân hay Đệ tử",
                                            "Cho\nBản thân", "Cho\nĐệ tử", "Đóng");
                                    return;
                                case 1:
                                    NpcService.gI().createTutorial(player, avartar,
                                            "Ta là Whis được Đại thiên sứ cử xuống Trái đất để thu thập lại trang bị Thần linh\bbị kẻ xấu đánh cắp\n"
                                                    + "Hãy đi tiêu diệt kẻ xấu để giành lại trang bị Thần linh bị đánh cắp\n"
                                                    + "Hiến tế cho ta trang bị Thần linh, Ta sẽ ban cho ngươi trang bị kích hoạt tương ứng trong truyền thuyết\n"
                                                    + "Yêu cầu khi hiến tế:\b- Bản thân đang sử dụng trang bị Thần linh\b- Vàng trong hành trang: 2 Tỷ vàng\b(không giới hạn số trang bị Thần linh trong 1 lần hiến tế)");
                                    return;
                            }
                    }
                case ConstNpc.HOC_TUYET_KY:
                    switch (select) {
                        case 0:
                            Item biKipTuyetKy = InventoryService.gI().findItemBagByTemp(player,
                                    (short) 1229);
                            int skillID = player.gender == 0 ? 24 : player.gender == 1 ? 26 : 25;
                            Skill newSkill = SkillUtil.createSkill(skillID, 1);
                            try {
                                Message msg = Service.getInstance().messageSubCommand((byte) 23);
                                msg.writer().writeShort(newSkill.skillId);
                                player.sendMessage(msg);
                                msg.cleanup();
                            } catch (IOException e) {
                            }
                            try { // send effect susscess
                                Message msg = new Message(-81);
                                msg.writer().writeByte(0);
                                msg.writer().writeUTF("test");
                                msg.writer().writeUTF("test");
                                msg.writer().writeShort(tempId);
                                player.sendMessage(msg);
                                msg.cleanup();

                                msg = new Message(-81);
                                msg.writer().writeByte(1);
                                msg.writer().writeByte(2);
                                msg.writer().writeByte(
                                        InventoryService.gI().getIndexBag(player, biKipTuyetKy));
                                msg.writer().writeByte(-1);
                                player.sendMessage(msg);
                                msg.cleanup();

                                msg = new Message(-81);
                                msg.writer().writeByte(7);
                                msg.writer().writeShort(newSkill.template.iconId);
                                player.sendMessage(msg);
                                msg.cleanup();
                            } catch (Exception e) {
                            }
                            Inventory inv = player.inventory;
                            InventoryService.gI().subQuantityItemsBag(player, biKipTuyetKy, 9999);
                            InventoryService.gI().sendItemBags(player);
                            inv.subGold(10_000_000);
                            inv.subGem(99);
                            SkillUtil.setSkill(player, newSkill);
                    }
                    return;
                case 799455479:
                    switch (select) {
                        case 0:
                            if (!player.getSession().actived) {
                                NpcService.gI().createTutorial(player, avartar,
                                        "Truy cập Trang chủ NgocRongKakarot.Com để mở Thành viên");
                                return;
                            }
                            int gender = player.gender;
                            List<Integer> ao = Arrays.asList(555, 557, 559);
                            List<Integer> quan = Arrays.asList(556, 558, 560);
                            List<Integer> gang = Arrays.asList(562, 564, 566);
                            List<Integer> giay = Arrays.asList(563, 565, 567);
                            int nhan = 561;

                            boolean dieuKien1 = player.inventory.itemsBody.get(0).isNotNullItem();
                            boolean dieuKien2 = player.inventory.itemsBody.get(1).isNotNullItem();
                            boolean dieuKien3 = player.inventory.itemsBody.get(2).isNotNullItem();
                            boolean dieuKien4 = player.inventory.itemsBody.get(3).isNotNullItem();
                            boolean dieuKien5 = player.inventory.itemsBody.get(4).isNotNullItem();

                            boolean dieuKien1_1 = dieuKien1
                                    && (player.inventory.itemsBody.get(0).template.id == ao
                                            .get(gender));
                            boolean dieuKien2_1 = dieuKien2
                                    && (player.inventory.itemsBody.get(1).template.id == quan
                                            .get(gender));
                            boolean dieuKien3_1 = dieuKien3
                                    && (player.inventory.itemsBody.get(2).template.id == gang
                                            .get(gender));
                            boolean dieuKien4_1 = dieuKien4
                                    && (player.inventory.itemsBody.get(3).template.id == giay
                                            .get(gender));
                            boolean dieuKien5_1 = dieuKien5
                                    && (player.inventory.itemsBody.get(4).template.id == nhan);

                            boolean condition1 = dieuKien1_1;
                            boolean condition2 = dieuKien2_1;
                            boolean condition3 = dieuKien3_1;
                            boolean condition4 = dieuKien4_1;
                            boolean condition5 = dieuKien5_1;

                            if (condition1 || condition2 || condition3 || condition4
                                    || condition5) {
                                String npcsay = "Danh sách hiến tế cho Whis:\n";
                                int i = 1;
                                if (condition1) {
                                    npcsay += i + ". "
                                            + player.inventory.itemsBody.get(0).template.name
                                            + "\n";
                                    i++;
                                }
                                if (condition2) {
                                    npcsay += i + ". "
                                            + player.inventory.itemsBody.get(1).template.name
                                            + "\n";
                                    i++;
                                }
                                if (condition3) {
                                    npcsay += i + ". "
                                            + player.inventory.itemsBody.get(2).template.name
                                            + "\n";
                                    i++;
                                }
                                if (condition4) {
                                    npcsay += i + ". "
                                            + player.inventory.itemsBody.get(3).template.name
                                            + "\n";
                                    i++;
                                }
                                if (condition5) {
                                    npcsay += i + ". "
                                            + player.inventory.itemsBody.get(4).template.name
                                            + "\n";
                                    i++;
                                }

                                npcsay += "Ngươi sẽ nhận lại một trang bị kích hoạt tương ứng trong truyền thuyết.";

                                createOtherMenu(player, ConstNpc.HIEN_TE_SU_PHU, npcsay,
                                        "Hiến tế\n(2 Tỷ vàng)", "Đóng");
                                return;
                            } else {
                                NpcService.gI().createTutorial(player, avartar,
                                        "Khi nào ngươi mặc trang bị thần linh thì tới đây ta nói chuyện tiếp");
                                break;
                            }

                        case 1:

                            if (!player.getSession().actived) {
                                NpcService.gI().createTutorial(player, avartar,
                                        "Truy cập Trang chủ NgocRongKakarot.Com để mở Thành viên");
                                return;
                            }

                            if (player.pet == null) {
                                NpcService.gI().createTutorial(player, avartar,
                                        "Ngươi cần phải có Đệ tử trước khi thực hiện");
                                return;
                            }

                            int gender_detu = player.pet.gender;

                            List<Integer> ao2 = Arrays.asList(555, 557, 559);
                            List<Integer> quan2 = Arrays.asList(556, 558, 560);
                            List<Integer> gang2 = Arrays.asList(562, 564, 566);
                            List<Integer> giay2 = Arrays.asList(563, 565, 567);
                            int nhan2 = 561;

                            boolean dieuKien12 = player.pet.inventory.itemsBody.get(0)
                                    .isNotNullItem();
                            boolean dieuKien22 = player.pet.inventory.itemsBody.get(1)
                                    .isNotNullItem();
                            boolean dieuKien32 = player.pet.inventory.itemsBody.get(2)
                                    .isNotNullItem();
                            boolean dieuKien42 = player.pet.inventory.itemsBody.get(3)
                                    .isNotNullItem();
                            boolean dieuKien52 = player.pet.inventory.itemsBody.get(4)
                                    .isNotNullItem();

                            boolean dieuKien1_12 = dieuKien12
                                    && (player.pet.inventory.itemsBody.get(0).template.id == ao2
                                            .get(gender_detu));
                            boolean dieuKien2_12 = dieuKien22
                                    && (player.pet.inventory.itemsBody.get(1).template.id == quan2
                                            .get(gender_detu));
                            boolean dieuKien3_12 = dieuKien32
                                    && (player.pet.inventory.itemsBody.get(2).template.id == gang2
                                            .get(gender_detu));
                            boolean dieuKien4_12 = dieuKien42
                                    && (player.pet.inventory.itemsBody.get(3).template.id == giay2
                                            .get(gender_detu));
                            boolean dieuKien5_12 = dieuKien52
                                    && (player.pet.inventory.itemsBody.get(4).template.id == nhan2);

                            boolean condition12 = dieuKien1_12;
                            boolean condition22 = dieuKien2_12;
                            boolean condition32 = dieuKien3_12;
                            boolean condition42 = dieuKien4_12;
                            boolean condition52 = dieuKien5_12;

                            if (condition12 || condition22 || condition32 || condition42
                                    || condition52) {
                                String npcsay = "Danh sách hiến tế cho Whis:\n";
                                int i = 1;
                                if (condition12) {
                                    npcsay += i + ". "
                                            + player.pet.inventory.itemsBody.get(0).template.name
                                            + "\n";
                                    i++;
                                }
                                if (condition22) {
                                    npcsay += i + ". "
                                            + player.pet.inventory.itemsBody.get(1).template.name
                                            + "\n";
                                    i++;
                                }
                                if (condition32) {
                                    npcsay += i + ". "
                                            + player.pet.inventory.itemsBody.get(2).template.name
                                            + "\n";
                                    i++;
                                }
                                if (condition42) {
                                    npcsay += i + ". "
                                            + player.pet.inventory.itemsBody.get(3).template.name
                                            + "\n";
                                    i++;
                                }
                                if (condition52) {
                                    npcsay += i + ". "
                                            + player.pet.inventory.itemsBody.get(4).template.name
                                            + "\n";
                                    i++;
                                }

                                npcsay += "Ngươi sẽ nhận lại một trang bị kích hoạt tương ứng trong truyền thuyết.";

                                createOtherMenu(player, ConstNpc.HIEN_TE_DE_TU, npcsay, "OK");
                                return;
                            } else {
                                NpcService.gI().createTutorial(player, avartar,
                                        "Khi nào đệ tử ngươi mặc trang bị thần linh thì tới đây ta nói chuyện tiếp");
                                break;
                            }
                    }

                case ConstNpc.HIEN_TE_SU_PHU:
                    switch (select) {
                        case 0:
                            int i = 0;
                            int gender = player.gender;
                            List<Integer> ao = Arrays.asList(555, 557, 559);
                            List<Integer> quan = Arrays.asList(556, 558, 560);
                            List<Integer> gang = Arrays.asList(562, 564, 566);
                            List<Integer> giay = Arrays.asList(563, 565, 567);

                            List<Integer> aoSKH = Arrays.asList(555, 557, 559);
                            List<Integer> quanSKH = Arrays.asList(556, 558, 560);
                            List<Integer> gangSKH = Arrays.asList(562, 564, 566);
                            List<Integer> giaySKH = Arrays.asList(563, 565, 567);
                            int rada = 12;

                            int[][] options = { { 128, 129, 127 }, { 130, 131, 132 },
                                    { 133, 135, 134 } };
                            // int skhId = ItemService.gI().randomSKHId((byte) 0);

                            short itemId;

                            int nhan = 561;

                            boolean dieuKien1 = player.inventory.itemsBody.get(0).isNotNullItem();
                            boolean dieuKien2 = player.inventory.itemsBody.get(1).isNotNullItem();
                            boolean dieuKien3 = player.inventory.itemsBody.get(2).isNotNullItem();
                            boolean dieuKien4 = player.inventory.itemsBody.get(3).isNotNullItem();
                            boolean dieuKien5 = player.inventory.itemsBody.get(4).isNotNullItem();

                            boolean dieuKien1_1 = dieuKien1
                                    && (player.inventory.itemsBody.get(0).template.id == ao
                                            .get(gender));
                            boolean dieuKien2_1 = dieuKien2
                                    && (player.inventory.itemsBody.get(1).template.id == quan
                                            .get(gender));
                            boolean dieuKien3_1 = dieuKien3
                                    && (player.inventory.itemsBody.get(2).template.id == gang
                                            .get(gender));
                            boolean dieuKien4_1 = dieuKien4
                                    && (player.inventory.itemsBody.get(3).template.id == giay
                                            .get(gender));
                            boolean dieuKien5_1 = dieuKien5
                                    && (player.inventory.itemsBody.get(4).template.id == nhan);

                            boolean condition1 = dieuKien1_1;
                            boolean condition2 = dieuKien2_1;
                            boolean condition3 = dieuKien3_1;
                            boolean condition4 = dieuKien4_1;
                            boolean condition5 = dieuKien5_1;

                            if (condition1 || condition2 || condition3 || condition4
                                    || condition5) {

                                if (player.inventory.gold < 2_000_000_000) {
                                    NpcService.gI().createTutorial(player, avartar,
                                            "Ngươi còn thiếu "
                                                    + Util.numberToMoney(
                                                            2_000_000_000 - player.inventory.gold)
                                                    + " vàng");
                                    return;
                                }

                                player.inventory.gold -= 2000000000;
                                Service.getInstance().sendMoney(player);

                                if (condition1) { // ÁO
                                    Item ao2 = ItemService.gI().createNewItem((short) gender);

                                    InventoryService.gI().removeItem(player.inventory.itemsBody, 0);

                                    Random random = new Random();
                                    int tyle = random.nextInt(100);
                                    ao2.itemOptions.add(new ItemOption(47, 3));

                                    if (tyle < 30) {
                                        if (gender == 0) {
                                            ao2.itemOptions.add(new ItemOption(129, 1));
                                            ao2.itemOptions.add(new ItemOption(141, 1));
                                        }
                                        if (gender == 1) {
                                            ao2.itemOptions.add(new ItemOption(131, 1));
                                            ao2.itemOptions.add(new ItemOption(143, 1));
                                        } else {
                                            ao2.itemOptions.add(new ItemOption(135, 1));
                                            ao2.itemOptions.add(new ItemOption(138, 1));
                                        }
                                    } else if (tyle < 60) {
                                        if (gender == 0) {
                                            ao2.itemOptions.add(new ItemOption(128, 1));
                                            ao2.itemOptions.add(new ItemOption(140, 1));
                                        }
                                        if (gender == 1) {
                                            ao2.itemOptions.add(new ItemOption(130, 1));
                                            ao2.itemOptions.add(new ItemOption(142, 1));
                                        } else {
                                            ao2.itemOptions.add(new ItemOption(133, 1));
                                            ao2.itemOptions.add(new ItemOption(136, 1));
                                        }

                                    } else {
                                        ao2.itemOptions.add(new ItemOption(134, 1));
                                        ao2.itemOptions.add(new ItemOption(137, 1));
                                    }
                                    InventoryService.gI().addItemBody(player, ao2);
                                    InventoryService.gI().sendItemBody(player);
                                    i++;
                                }

                                if (condition2) { // QUẦN
                                    Item ao2 = ItemService.gI().createNewItem((short) (gender + 6));
                                    InventoryService.gI().removeItem(player.inventory.itemsBody, 1);
                                    // ao2.template.id = (short) (gender + 6);
                                    List<ItemOption> optionsToRemove = new ArrayList<>(); // Danh
                                    // sách
                                    // các
                                    // ItemOption
                                    // cần xóa
                                    for (ItemOption itopt : ao2.itemOptions) {
                                        int optionId = itopt.optionTemplate.id;
                                        if (optionId >= 0 && optionId <= 233) {
                                            optionsToRemove.add(itopt);
                                        }
                                    }

                                    Random random = new Random();
                                    int tyle = random.nextInt(100);

                                    ao2.itemOptions.removeAll(optionsToRemove);
                                    ao2.itemOptions.add(new ItemOption(6, 20));

                                    if (tyle < 30) {
                                        if (gender == 0) {
                                            ao2.itemOptions.add(new ItemOption(129, 1));
                                            ao2.itemOptions.add(new ItemOption(141, 1));
                                        }
                                        if (gender == 1) {
                                            ao2.itemOptions.add(new ItemOption(131, 1));
                                            ao2.itemOptions.add(new ItemOption(143, 1));
                                        } else {
                                            ao2.itemOptions.add(new ItemOption(135, 1));
                                            ao2.itemOptions.add(new ItemOption(138, 1));
                                        }
                                    } else if (tyle < 60) {
                                        if (gender == 0) {
                                            ao2.itemOptions.add(new ItemOption(128, 1));
                                            ao2.itemOptions.add(new ItemOption(140, 1));
                                        }
                                        if (gender == 1) {
                                            ao2.itemOptions.add(new ItemOption(130, 1));
                                            ao2.itemOptions.add(new ItemOption(142, 1));
                                        } else {
                                            ao2.itemOptions.add(new ItemOption(133, 1));
                                            ao2.itemOptions.add(new ItemOption(136, 1));
                                        }

                                    } else {
                                        ao2.itemOptions.add(new ItemOption(134, 1));
                                        ao2.itemOptions.add(new ItemOption(137, 1));
                                    }
                                    InventoryService.gI().addItemBody(player, ao2);
                                    InventoryService.gI().sendItemBody(player);
                                    Service.getInstance().Send_Caitrang(player);
                                    Service.getInstance().Send_Info_NV(player);
                                    i++;
                                }
                                if (condition3) { // GĂNG

                                    Item ao2 = ItemService.gI()
                                            .createNewItem((short) (gender + 21));
                                    InventoryService.gI().removeItem(player.inventory.itemsBody, 2);

                                    List<ItemOption> optionsToRemove = new ArrayList<>(); // Danh
                                    // sách
                                    // các
                                    // ItemOption
                                    // cần xóa
                                    for (ItemOption itopt : ao2.itemOptions) {
                                        int optionId = itopt.optionTemplate.id;
                                        if (optionId >= 0 && optionId <= 233) {
                                            optionsToRemove.add(itopt);
                                        }
                                    }

                                    Random random = new Random();
                                    int tyle = random.nextInt(100);

                                    ao2.itemOptions.removeAll(optionsToRemove);
                                    ao2.itemOptions.add(new ItemOption(0, 5));

                                    if (tyle < 30) {
                                        if (gender == 0) {
                                            ao2.itemOptions.add(new ItemOption(129, 1));
                                            ao2.itemOptions.add(new ItemOption(141, 1));
                                        }
                                        if (gender == 1) {
                                            ao2.itemOptions.add(new ItemOption(131, 1));
                                            ao2.itemOptions.add(new ItemOption(143, 1));
                                        } else {
                                            ao2.itemOptions.add(new ItemOption(135, 1));
                                            ao2.itemOptions.add(new ItemOption(138, 1));
                                        }
                                    } else if (tyle < 60) {
                                        if (gender == 0) {
                                            ao2.itemOptions.add(new ItemOption(128, 1));
                                            ao2.itemOptions.add(new ItemOption(140, 1));
                                        }
                                        if (gender == 1) {
                                            ao2.itemOptions.add(new ItemOption(130, 1));
                                            ao2.itemOptions.add(new ItemOption(142, 1));
                                        } else {
                                            ao2.itemOptions.add(new ItemOption(133, 1));
                                            ao2.itemOptions.add(new ItemOption(136, 1));
                                        }

                                    } else {
                                        ao2.itemOptions.add(new ItemOption(134, 1));
                                        ao2.itemOptions.add(new ItemOption(137, 1));
                                    }
                                    InventoryService.gI().addItemBody(player, ao2);
                                    InventoryService.gI().sendItemBody(player);
                                    Service.getInstance().Send_Caitrang(player);
                                    Service.getInstance().Send_Info_NV(player);
                                    i++;
                                }
                                if (condition4) { // GIẦY

                                    Item ao2 = ItemService.gI()
                                            .createNewItem((short) (gender + 27));
                                    InventoryService.gI().removeItem(player.inventory.itemsBody, 3);

                                    List<ItemOption> optionsToRemove = new ArrayList<>(); // Danh
                                    // sách
                                    // các
                                    // ItemOption
                                    // cần xóa
                                    for (ItemOption itopt : ao2.itemOptions) {
                                        int optionId = itopt.optionTemplate.id;
                                        if (optionId >= 0 && optionId <= 233) {
                                            optionsToRemove.add(itopt);
                                        }
                                    }

                                    Random random = new Random();
                                    int tyle = random.nextInt(100);

                                    ao2.itemOptions.removeAll(optionsToRemove);
                                    ao2.itemOptions.add(new ItemOption(7, 10));

                                    if (tyle < 30) {
                                        if (gender == 0) {
                                            ao2.itemOptions.add(new ItemOption(129, 1));
                                            ao2.itemOptions.add(new ItemOption(141, 1));
                                        }
                                        if (gender == 1) {
                                            ao2.itemOptions.add(new ItemOption(131, 1));
                                            ao2.itemOptions.add(new ItemOption(143, 1));
                                        } else {
                                            ao2.itemOptions.add(new ItemOption(135, 1));
                                            ao2.itemOptions.add(new ItemOption(138, 1));
                                        }
                                    } else if (tyle < 60) {
                                        if (gender == 0) {
                                            ao2.itemOptions.add(new ItemOption(128, 1));
                                            ao2.itemOptions.add(new ItemOption(140, 1));
                                        }
                                        if (gender == 1) {
                                            ao2.itemOptions.add(new ItemOption(130, 1));
                                            ao2.itemOptions.add(new ItemOption(142, 1));
                                        } else {
                                            ao2.itemOptions.add(new ItemOption(133, 1));
                                            ao2.itemOptions.add(new ItemOption(136, 1));
                                        }

                                    } else {
                                        ao2.itemOptions.add(new ItemOption(134, 1));
                                        ao2.itemOptions.add(new ItemOption(137, 1));
                                    }
                                    InventoryService.gI().addItemBody(player, ao2);
                                    InventoryService.gI().sendItemBody(player);
                                    Service.getInstance().Send_Caitrang(player);
                                    Service.getInstance().Send_Info_NV(player);
                                    i++;
                                }
                                if (condition5) { // RADA
                                    Item ao2 = ItemService.gI().createNewItem((short) 12);
                                    InventoryService.gI().removeItem(player.inventory.itemsBody, 4);

                                    List<ItemOption> optionsToRemove = new ArrayList<>(); // Danh
                                    // sách
                                    // các
                                    // ItemOption
                                    // cần xóa
                                    for (ItemOption itopt : ao2.itemOptions) {
                                        int optionId = itopt.optionTemplate.id;
                                        if (optionId >= 0 && optionId <= 233) {
                                            optionsToRemove.add(itopt);
                                        }
                                    }

                                    Random random = new Random();
                                    int tyle = random.nextInt(100);

                                    ao2.itemOptions.removeAll(optionsToRemove);
                                    ao2.itemOptions.add(new ItemOption(14, 1));

                                    if (tyle < 30) {
                                        if (gender == 0) {
                                            ao2.itemOptions.add(new ItemOption(129, 1));
                                            ao2.itemOptions.add(new ItemOption(141, 1));
                                        }
                                        if (gender == 1) {
                                            ao2.itemOptions.add(new ItemOption(131, 1));
                                            ao2.itemOptions.add(new ItemOption(143, 1));
                                        } else {
                                            ao2.itemOptions.add(new ItemOption(135, 1));
                                            ao2.itemOptions.add(new ItemOption(138, 1));
                                        }
                                    } else if (tyle < 60) {
                                        if (gender == 0) {
                                            ao2.itemOptions.add(new ItemOption(128, 1));
                                            ao2.itemOptions.add(new ItemOption(140, 1));
                                        }
                                        if (gender == 1) {
                                            ao2.itemOptions.add(new ItemOption(130, 1));
                                            ao2.itemOptions.add(new ItemOption(142, 1));
                                        } else {
                                            ao2.itemOptions.add(new ItemOption(133, 1));
                                            ao2.itemOptions.add(new ItemOption(136, 1));
                                        }

                                    } else {
                                        ao2.itemOptions.add(new ItemOption(134, 1));
                                        ao2.itemOptions.add(new ItemOption(137, 1));
                                    }
                                    InventoryService.gI().addItemBody(player, ao2);
                                    InventoryService.gI().sendItemBody(player);
                                    Service.getInstance().Send_Caitrang(player);
                                    Service.getInstance().Send_Info_NV(player);
                                    i++;
                                }
                                NpcService.gI().createTutorial(player, avartar,
                                        "Ba la ca ... ca ca ca... Um ba mi xa ki... ca ca...\n"
                                                + "Na na ca ca... la la... sa da ma ta ro bu ki....\n"
                                                + "....................\n"
                                                + "Bạn vừa hiến tế thành công cho Whis " + i
                                                + " trang bị Thần linh và nhận được " + i
                                                + " trang bị kích hoạt trong truyền thuyết.");
                                InventoryService.gI().sendItemBody(player);
                                Service.getInstance().Send_Caitrang(player);
                                Service.getInstance().Send_Info_NV(player);
                                break;
                            }
                            break;
                    }
            }
        }
    }
}
