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
 * NPC GhiDanh
 */
public class GhiDanh extends Npc {

    public GhiDanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    String[] menuselect = new String[] {};

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == ConstMap.DAI_HOI_VO_THUAT) {
                int crrHOUR = TimeUtil.getCurrHour();
                if (DaiHoiVoThuatManager.gI().openDHVT
                        && (System.currentTimeMillis() <= DaiHoiVoThuatManager.gI().tOpenDHVT)) {
                    String nameDH = DaiHoiVoThuatManager.gI().nameRoundDHVT();
                    this.createOtherMenu(player, ConstNpc.DAI_HOI_VO_THUAT,
                            "Chào mừng bạn đến với đại hội võ thuật\n"
                                    + "Giải " + nameDH + " đang có "
                                    + DaiHoiVoThuatManager.gI().lstIDPlayers.size()
                                    + " người đăng ký thi đấu\n"
                                    + DaiHoiVoThuatService.gI().textDaiHoi(player.nPoint.power),
                            "Thông tin\nChi tiết", "Đăng kí", "Giải\nSiêu Hạng",
                            "Đại Hội\nVõ Thuật\nLần thứ\n23");
                } else {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Đã hết hạn đăng ký thi đấu, xin vui lòng chờ đến giải sau"
                                    + DaiHoiVoThuatManager.gI().timeDHVTnext(crrHOUR) + "\n"
                                    + DaiHoiVoThuatService.gI().textDaiHoi(player.nPoint.power),
                            new String[] { "Thông tin\bChi tiết", "Giải\nSiêu Hạng",
                                    "Đại Hội\nVõ Thuật\nLần thứ\n23", "Ðóng" });
                }
            } else if (this.mapId == ConstMap.DAI_HOI_VO_THUAT_129) {
                int goldchallenge = player.goldChallenge;
                if (player.levelWoodChest == 0) {
                    menuselect = new String[] {
                            "Hướng\ndẫn\nthêm",
                            "Thi đấu\n" + player.gemChallenge + " ngọc",
                            "Thi đấu\n" + Util.numberToMoney(goldchallenge) + "\nvàng",
                            "Về\nĐại Hội\nVõ Thuật" };
                } else {
                    menuselect = new String[] {
                            "Hướng\ndẫn\nthêm",
                            "Thi đấu\n" + player.gemChallenge + " ngọc",
                            "Thi đấu\n" + Util.numberToMoney(goldchallenge) + "\nvàng",
                            "Nhận\nthưởng\n Rương Cấp " + player.levelWoodChest,
                            "Về\nĐại Hội\nVõ Thuật" };
                }
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Đại hội võ thuật lần thứ 23\n"
                                + "Diễn ra bất kể ngày đêm, ngày nghỉ, ngày lễ\n"
                                + "Phần thưởng vô cùng quý giá\n"
                                + "Nhanh chóng tham gia nào",
                        menuselect, "Từ chối");
            } else {
                super.openBaseMenu(player);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.DAI_HOI_VO_THUAT:
                    switch (select) {
                        case 0:
                            NpcService.gI().createTutorial(player, avartar,
                                    "Lịch thi đấu trong ngày\b Giải Nhi đồng: 8,14,18h\b Giải Siêu cấp 1: 9,13,19h\b Giải Siêu cấp 2: 10,15,20h\b Giải Siêu cấp 3: 11,16,21h\b Giải Ngoại hạng: 12,17,22,23h\n"
                                            + "Giải thưởng khi thắng mỗi vòng\b Giải Nhi đồng: 2 ngọc\b Giải Siêu cấp 1: 4 ngọc\b Giải Siêu cấp 2: 6 ngọc\b Giải Siêu cấp 3: 8 ngọc\b Giải Ngoại hạng: 10.000 vàng\b Vô địch: 5 viên đá nâng cấp\n"
                                            + "Lệ phí đăng ký các giải đấu\b Giải Nhi đồng: 2 ngọc\b Giải Siêu cấp 1: 4 ngọc\b Giải Siêu cấp 2: 6 ngọc\b Giải Siêu cấp 3: 8 ngọc\b Giải Ngoại hạng: 10.000 vàng\n"
                                            + "Vui lòng đến đúng giờ để đăng ký thi đấu");
                            break;
                        case 1:
                            this.createOtherMenu(player, ConstNpc.DANG_KY_DAI_HOI_VO_THUAT,
                                    "Hiện đang có giải đấu "
                                            + DaiHoiVoThuatManager.gI().nameRoundDHVT()
                                            + " bạn có muốn đăng ký không?",
                                    "Giải\n " + DaiHoiVoThuatManager.gI().nameRoundDHVT() + "\n("
                                            + DaiHoiVoThuatManager.gI().costRoundDHVT() + ")",
                                    "Từ chối");
                            break;
                        case 2:
                            NpcService.gI().createTutorial(player, avartar,
                                    "Chức năng đang được phát triển");
                            // ChangeMapService.gI().changeMapNonSpaceship(player, 113,
                            // player.location.x, 360);
                            break;
                        case 3:
                            ChangeMapService.gI().changeMapNonSpaceship(player, 129,
                                    player.location.x, 360);
                            break;
                    }
                    return;
                case ConstNpc.DANG_KY_DAI_HOI_VO_THUAT:
                    switch (select) {
                        case 0:
                            if (DaiHoiVoThuatService.gI().canRegisDHVT(player.nPoint.power)) {
                                if (DaiHoiVoThuatManager.gI().lstIDPlayers.size() < 256) {
                                    if (DaiHoiVoThuatManager.gI().typeDHVT == (byte) 5
                                            && player.inventory.gold >= 10000) {
                                        if (DaiHoiVoThuatManager.gI().isAssignDHVT(player.id)) {
                                            Service.getInstance().sendThongBao(player,
                                                    "Bạn đã đăng ký tham gia đại hội võ thuật rồi");
                                        } else {
                                            player.inventory.gold -= 10000;
                                            Service.getInstance().sendMoney(player);
                                            Service.getInstance().sendThongBao(player,
                                                    "Bạn đã đăng ký thành công, nhớ có mặt tại đây trước giờ thi đấu");
                                            DaiHoiVoThuatManager.gI().lstIDPlayers.add(player.id);
                                        }
                                    } else if (DaiHoiVoThuatManager.gI().typeDHVT > (byte) 0
                                            && DaiHoiVoThuatManager.gI().typeDHVT < (byte) 5
                                            && player.inventory.gem >= (int) (2
                                                    * DaiHoiVoThuatManager.gI().typeDHVT)) {
                                        if (DaiHoiVoThuatManager.gI().isAssignDHVT(player.id)) {
                                            Service.getInstance().sendThongBao(player,
                                                    "Bạn đã đăng ký tham gia đại hội võ thuật rồi");
                                        } else {
                                            player.inventory.gem -= (int) (2
                                                    * DaiHoiVoThuatManager.gI().typeDHVT);
                                            Service.getInstance().sendMoney(player);
                                            Service.getInstance().sendThongBao(player,
                                                    "Bạn đã đăng ký thành công, nhớ có mặt tại đây trước giờ thi đấu");
                                            DaiHoiVoThuatManager.gI().lstIDPlayers.add(player.id);
                                        }
                                    } else {
                                        Service.getInstance().sendThongBao(player,
                                                "Không đủ vàng ngọc để đăng ký thi đấu");
                                    }
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Hiện tại đã đạt tới số lượng người đăng ký tối đa, xin hãy chờ đến giải sau");
                                }

                            } else {
                                NpcService.gI().createTutorial(player, avartar,
                                        DaiHoiVoThuatService.gI().textDaiHoi2(player.nPoint.power));
                            }
                    }
                    return;
                case ConstNpc.MENU_NHAN_RUONG:
                    switch (select) {
                        case 0:
                            if (!player.receivedWoodChest) {
                                if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                    Item it = ItemService.gI()
                                            .createNewItem((short) ConstItem.RUONG_GO);
                                    it.itemOptions.add(new ItemOption(72, player.levelWoodChest));
                                    it.createTime = System.currentTimeMillis();
                                    InventoryService.gI().addItemBag(player, it, 0);
                                    InventoryService.gI().sendItemBags(player);
                                    NpcService.gI().createMenuConMeo(player, 251020003, -1,
                                            "Bạn nhận được\n"
                                                    + "|1|Rương gỗ\n"
                                                    + "|2|Giấu bên trong nhiều vật phẩm quý giá",
                                            "OK");
                                    player.receivedWoodChest = true;
                                    player.levelWoodChest = 0;
                                    return;
                                } else {
                                    this.npcChat(player, "Hành trang đã đầy");
                                }
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Mỗi ngày chỉ có thể nhận rương báu 1 lần");
                            }
                            break;
                    }
                    break;
                case ConstNpc.BASE_MENU:
                    if (this.mapId == ConstMap.DAI_HOI_VO_THUAT) {
                        if (player.iDMark.isBaseMenu()) {
                            switch (select) {
                                case 0:
                                    NpcService.gI().createTutorial(player, avartar,
                                            "Lịch thi đấu trong ngày\b Giải Nhi đồng: 8,14,18h\b Giải Siêu cấp 1: 9,13,19h\b Giải Siêu cấp 2: 10,15,20h\b Giải Siêu cấp 3: 11,16,21h\b Giải Ngoại hạng: 12,17,22,23h\n"
                                                    + "Giải thưởng khi thắng mỗi vòng\b Giải Nhi đồng: 2 ngọc\b Giải Siêu cấp 1: 4 ngọc\b Giải Siêu cấp 2: 6 ngọc\b Giải Siêu cấp 3: 8 ngọc\b Giải Ngoại hạng: 10.000 vàng\b Vô địch: 5 viên đá nâng cấp\n"
                                                    + "Lệ phí đăng ký các giải đấu\b Giải Nhi đồng: 2 ngọc\b Giải Siêu cấp 1: 4 ngọc\b Giải Siêu cấp 2: 6 ngọc\b Giải Siêu cấp 3: 8 ngọc\b Giải Ngoại hạng: 10.000 vàng\n"
                                                    + "Vui lòng đến đúng giờ để đăng ký thi đấu");
                                    break;
                                case 1:
                                    NpcService.gI().createTutorial(player, avartar,
                                            "Chức năng đang được phát triển");
                                    // ChangeMapService.gI().changeMapNonSpaceship(player, 113,
                                    // player.location.x, 360);
                                    break;
                                case 2:
                                    ChangeMapService.gI().changeMapNonSpaceship(player, 129,
                                            player.location.x, 360);
                                    break;
                            }
                        }
                    } else if (this.mapId == ConstMap.DAI_HOI_VO_THUAT_129) {
                        int goldchallenge = player.goldChallenge;
                        if (player.levelWoodChest == 0) {
                            switch (select) {
                                case 0:
                                    NpcService.gI().createTutorial(player, avartar,
                                            "Đại hội quy tụ nhiều cao thủ như Jacky Chun, Thiên Xin Hăng, Tàu Bảy Bảy...\bPhần thưởng là 1 rương gỗ chứa nhiều vật phẩm giá trị\bKhi hạ được 1 đối thủ, phần thưởng sẽ nâng lên 1 cấp\bRương càng cao cấp, vật phẩm trong đó càng giá trị hơn\n"
                                                    + "Mỗi ngày bạn chỉ được nhận 1 phần thưởng\bBạn hãy cố gắng hết sức mình để\b nhận phần thưởng xứng đáng nhất nhé");
                                    break;
                                case 1:
                                    if (!player.receivedWoodChest) {
                                        if (InventoryService.gI().finditemWoodChest(player)) {
                                            if (player.inventory.getGem() >= player.gemChallenge) {
                                                DHVT23Service.gI().startChallenge(player);
                                                player.inventory.subGem(player.gemChallenge);
                                                PlayerService.gI().sendInfoHpMpMoney(player);
                                                player.goldChallenge += 50000;
                                                player.gemChallenge += 1;
                                            } else {
                                                Service.getInstance().sendThongBao(player,
                                                        "Không đủ ngọc, còn thiếu "
                                                                + Util.numberToMoney(
                                                                        player.gemChallenge
                                                                                - player.inventory.gem)
                                                                + " ngọc nữa");
                                            }
                                        } else {
                                            Service.getInstance().sendThongBao(player,
                                                    "Hãy mở rương báu vật trước");
                                        }
                                    } else {
                                        Service.getInstance().sendThongBao(player,
                                                "Hãy chờ đến ngày mai");
                                    }
                                    break;
                                case 2:
                                    if (!player.receivedWoodChest) {
                                        if (InventoryService.gI().finditemWoodChest(player)) {
                                            if (player.inventory.getGold() >= goldchallenge) {
                                                DHVT23Service.gI().startChallenge(player);
                                                player.inventory.subGold(goldchallenge);
                                                PlayerService.gI().sendInfoHpMpMoney(player);
                                                player.goldChallenge += 50000;
                                                player.gemChallenge += 1;
                                            } else {
                                                Service.getInstance().sendThongBao(player,
                                                        "Không đủ vàng, còn thiếu "
                                                                + Util.numberToMoney(goldchallenge
                                                                        - player.inventory.gold)
                                                                + " vàng nữa");
                                            }
                                        } else {
                                            Service.getInstance().sendThongBao(player,
                                                    "Hãy mở rương báu vật trước");
                                        }
                                        break;
                                    } else {
                                        Service.getInstance().sendThongBao(player,
                                                "Hãy chờ đến ngày mai");
                                    }
                                    break;
                                case 3:
                                    ChangeMapService.gI().changeMapNonSpaceship(player,
                                            ConstMap.DAI_HOI_VO_THUAT, player.location.x, 336);
                                    break;
                            }
                        } else {
                            switch (select) {
                                case 0:
                                    NpcService.gI().createTutorial(player, avartar,
                                            "Đại hội quy tụ nhiều cao thủ như Jacky Chun, Thiên Xin Hăng, Tàu Bảy Bảy...\bPhần thưởng là 1 rương gỗ chứa nhiều vật phẩm giá trị\bKhi hạ được 1 đối thủ, phần thưởng sẽ nâng lên 1 cấp\bRương càng cao cấp, vật phẩm trong đó càng giá trị hơn\n"
                                                    + "Mỗi ngày bạn chỉ được nhận 1 phần thưởng\bBạn hãy cố gắng hết sức mình để\b nhận phần thưởng xứng đáng nhất nhé");
                                    break;
                                case 1:
                                    if (!player.receivedWoodChest) {
                                        if (InventoryService.gI().finditemWoodChest(player)) {
                                            if (player.inventory.getGem() >= player.gemChallenge) {
                                                DHVT23Service.gI().startChallenge(player);
                                                player.inventory.subGem(player.gemChallenge);
                                                PlayerService.gI().sendInfoHpMpMoney(player);
                                                player.goldChallenge += 50000;
                                                player.gemChallenge += 1;
                                            } else {
                                                Service.getInstance().sendThongBao(player,
                                                        "Không đủ ngọc, còn thiếu "
                                                                + Util.numberToMoney(
                                                                        player.gemChallenge
                                                                                - player.inventory.gem)
                                                                + " ngọc nữa");
                                            }
                                        } else {
                                            Service.getInstance().sendThongBao(player,
                                                    "Hãy mở rương báu vật trước");
                                        }
                                    } else {
                                        Service.getInstance().sendThongBao(player,
                                                "Hãy chờ đến ngày mai");
                                    }

                                    break;
                                case 2:
                                    if (!player.receivedWoodChest) {
                                        if (InventoryService.gI().finditemWoodChest(player)) {
                                            if (player.inventory.getGold() >= goldchallenge) {
                                                DHVT23Service.gI().startChallenge(player);
                                                player.inventory.subGold(goldchallenge);
                                                PlayerService.gI().sendInfoHpMpMoney(player);
                                                player.goldChallenge += 2000000;
                                            } else {
                                                Service.getInstance().sendThongBao(player,
                                                        "Không đủ vàng, còn thiếu "
                                                                + Util.numberToMoney(goldchallenge
                                                                        - player.inventory.gold)
                                                                + " vàng");
                                            }
                                        } else {
                                            Service.getInstance().sendThongBao(player,
                                                    "Hãy mở rương báu vật trước");
                                        }
                                    } else {
                                        Service.getInstance().sendThongBao(player,
                                                "Hãy chờ đến ngày mai");
                                    }
                                    break;
                                case 3:
                                    createOtherMenu(player, ConstNpc.MENU_NHAN_RUONG,
                                            "Phần thưởng của bạn đang ở cấp "
                                                    + player.levelWoodChest + " / 12\n"
                                                    + "Mỗi ngày chỉ được nhận phần thưởng 1 lần\n"
                                                    + "bạn có chắc sẽ nhận phần thưởng ngay bây giờ?",
                                            "OK", "Từ chối");
                                    break;
                                case 4:
                                    ChangeMapService.gI().changeMapNonSpaceship(player,
                                            ConstMap.DAI_HOI_VO_THUAT, player.location.x, 336);
                                    break;
                            }
                        }
                    }
                    break;
            }
        }
    }
}
