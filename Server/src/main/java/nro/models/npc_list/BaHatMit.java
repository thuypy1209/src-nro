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
 * NPC BaHatMit
 */
public class BaHatMit extends Npc {

    public BaHatMit(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }


    @Override
    public void openBaseMenu(Player player) {
        Item bongTai = InventoryService.gI().findItemBagByTemp(player, (short) 454);
        Item bongTaiCap2 = InventoryService.gI().findItemBagByTemp(player, (short) 921);
        if (canOpenNpc(player)) {
            if (this.mapId == 168) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                        "Về Đảo Kame", "Chế Tạo Hộp Quà Helloween");
            }
            if (this.mapId == 5) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "|7|Đừng chạy theo thành công, hãy trở nên xuất sắc và thành công sẽ tìm đến bạn.",
                        "Chức năng\nPha lê", "Nâng Cấp\n Set Đồ","Trang Bị\n Thiên Sứ");
            } else if (this.mapId == 112) {
                if (player.DoneVoDaiBaHatMit == 1) {
                    this.createOtherMenu(player, ConstNpc.NHAN_QUA_VO_DAI,
                            "Đây là phẩn thưởng của con.", "1 vệ tinh\n bất kì",
                            "1 bùa 1h\n bất kỳ");
                } else {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi muốn đăng ký thi đấu võ đài?\n"
                                    + "nhiều phần thưởng giá trị đang đợi ngươi đó",
                            "Top 100", "Đồng ý\n0 ngọc", "Từ chối", "Về\nđảo rùa");
                }
            } else {
                if (player.event.luotNhanBuaMienPhi == 1) {
                    if (bongTaiCap2 != null) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                "Thưởng Bùa 1h\nngẫu nhiên", "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa",
                                "Nâng cấp\n Vật phẩm", "Mở chỉ số Bông tai\nPorata cấp 2",
                                "Làm phép\nNhập đá", "Nhập\nNgọc Rồng");
                    } else if (bongTai != null) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                "Thưởng Bùa 1h\nngẫu nhiên", "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa",
                                "Nâng cấp\n Vật phẩm", "Nâng cấp Bông tai\nPorata",
                                "Làm phép\nNhập đá", "Nhập\nNgọc Rồng");
                    } else {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                "Thưởng Bùa 1h\nngẫu nhiên", "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa",
                                "Nâng cấp\n Vật phẩm", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng");
                    }
                } else {
                    if (bongTaiCap2 != null) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa", "Nâng cấp\n Vật phẩm",
                                "Mở chỉ số Bông tai\nPorata cấp 2", "Làm phép\nNhập đá",
                                "Nhập\nNgọc Rồng");
                    } else if (bongTai != null) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa", "Nâng cấp\n Vật phẩm",
                                "Nâng cấp Bông tai\nPorata", "Làm phép\nNhập đá",
                                "Nhập\nNgọc Rồng");
                    } else {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?",
                                "Sách\nTuyệt Kỹ", "Cửa hàng\n Bùa", "Nâng cấp\n Vật phẩm",
                                "Làm phép\nNhập đá", "Nhập\nNgọc Rồng");
                    }
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        Item bongTai = InventoryService.gI().findItemBagByTemp(player, (short) 454);
        Item bongTaiCap2 = InventoryService.gI().findItemBagByTemp(player, (short) 921);
        if (canOpenNpc(player)) {
            if (this.mapId == 5) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0: // Chức năng pha lê
                            createOtherMenu(player, ConstNpc.CHUC_NANG_SAO_PHA_LE,
                                    "Ta có thể giúp gì cho ngươi?",
                                    "Ép sao\ntrang bị",
                                    "Pha lê\nhóa\ntrang bị",
                                    "Nâng cấp\nSao pha lê",
                                    "Đánh bóng\nSao pha lê",
                                    "Cường hóa\nlỗ sao\npha lê",
                                    "Tạo đá\nHematite");
                            return;
                        case 1: // Nâng cấp Set đồ SKH VIP
                            createOtherMenu(player, ConstNpc.MENU_CHUYEN_HOA_SKH,
                                    "Ta sẽ nâng trang bị hủy diệt của người\nlên một tầm cao mới hoàn toàn khác",
                                    "Nâng cấp\nSKH VIP",
                                    "Nâng cấp\n Đồ thần");
                            return;
                        case 2: // Chế tạo đồ thiên sứ
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.CHE_TAO_DO_THIEN_SU);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.CHUC_NANG_SAO_PHA_LE) {
                    switch (select) {
                        case 0:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.EP_SAO_TRANG_BI);
                            break;
                        case 1:
                            createOtherMenu(player, ConstNpc.MENU_PHA_LE_HOA_TRANG_BI,
                                    "Ngươi muốn pha lê hóa trang bị bằng cách nào?", "Bằng ngọc",
                                    "Từ chối");
                            break;
                        case 2: // NANG CAP SAO PHA LE
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.NANG_CAP_SAO_PHA_LE);
                            break;
                        case 3:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.DANH_BONG_SAO_PHA_LE);
                            break;
                        case 4:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.CUONG_HOA_LO_SAO_PHA_LE);
                            break;
                        case 5:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.TAO_DA_HEMATILE);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_CHUYEN_HOA_SKH) {
                    switch (select) {
                        case 0: // Nâng skh vip
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.DAP_SET_KICH_HOAT_CAO_CAP);
                            break;
                        case 1: // nâng cấp đồ hủy diệt
                             CombineServiceNew.gI().openTabCombine(player,
                             CombineServiceNew.NANG_CAP_SKH);
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.NANG_CAP_DO_TL);
                            break;

                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_PHA_LE_HOA_TRANG_BI) {
                    switch (select) {
                        case 0:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.PHA_LE_HOA_TRANG_BI);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_CHUYEN_HOA_TRANG_BI) {
                    switch (select) {
                        case 0:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.CHUYEN_HOA_BANG_VANG);
                            break;
                        case 1:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.CHUYEN_HOA_BANG_NGOC);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_CHUYEN_HOA_TRANG_BI) {
                    switch (select) {
                        case 0:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.CHUYEN_HOA_BANG_VANG);
                            break;
                        case 1:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.CHUYEN_HOA_BANG_NGOC);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                    switch (player.combineNew.typeCombine) {
                        case CombineServiceNew.EP_SAO_TRANG_BI:
                        case CombineServiceNew.PHA_LE_HOA_TRANG_BI:
                        case CombineServiceNew.DOI_VE_HUY_DIET:
                        case CombineServiceNew.DAP_SET_KICH_HOAT:
                        case CombineServiceNew.DAP_SET_KICH_HOAT_CAO_CAP:
                        case CombineServiceNew.CHUYEN_HOA_BANG_VANG:
                        case CombineServiceNew.CHUYEN_HOA_BANG_NGOC:
                        case CombineServiceNew.GIA_HAN_CAI_TRANG:
                        case CombineServiceNew.NANG_CAP_SKH:
                        case CombineServiceNew.CHE_TAO_DO_THIEN_SU:
                        case CombineServiceNew.NANG_CAP_DO_TL:
                            CombineServiceNew.gI().startCombine(player, select);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_NANG_CAP_DO_TS) {
                    if (select == 0) {
                        CombineServiceNew.gI().startCombine(player, select);
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.ORTHER_MENU) {
                    switch (select) {
                        case 0:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.DAP_SET_KICH_HOAT);
                            break;
                        case 1:
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.DAP_SET_KICH_HOAT_CAO_CAP);
                            break;

                    }
                }
            } else if (this.mapId == 112) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:// Top 100 gì đó đéo biết
                            break;
                        case 1:// xác nhận lên võ đài
                            VoDaiSinhTuService.gI().startChallenge(player);
                            break;
                        case 2:// từ chối

                            break;
                        case 3:
                            ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.NHAN_QUA_VO_DAI) {
                    switch (select) {
                        case 0:
                            if (player.DoneVoDaiBaHatMit == 1) {
                                player.DoneVoDaiBaHatMit = 0;
                                Service.getInstance().sendThongBao(player,
                                        "Bạn đã nhận được 1 vệ tinh ngẫu nhiên");
                                break;
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Bạn đã nhận phần thưởng này rồi");
                            }
                            break;
                        case 1:
                            if (player.DoneVoDaiBaHatMit == 1) {
                                player.DoneVoDaiBaHatMit = 0;
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Bạn đã nhận được 1 bùa 1h ngẫu nhiên");
                            }
                            break;
                    }
                }
            } else if (this.mapId == 42 || this.mapId == 43 || this.mapId == 44
                    || this.mapId == 84) { // BA_HAT_MIT_BUA
                if (player.iDMark.isBaseMenu()) {
                    if (player.event.luotNhanBuaMienPhi == 1) {
                        if (bongTaiCap2 != null) {
                            switch (select) {
                                case 0: // Ngẫu nhiên bùa 1h
                                    if (player.event.luotNhanBuaMienPhi == 1) {
                                        int idItem = Util.nextInt(213, 219);
                                        player.charms.addTimeCharms(idItem, 60);
                                        Item bua = ItemService.gI().createNewItem((short) idItem);
                                        Service.getInstance().sendThongBao(player,
                                                "Bạn vừa nhận thưởng " + bua.getName());
                                        player.event.luotNhanBuaMienPhi = 0;
                                    } else {
                                        Service.getInstance().sendThongBao(player,
                                                "Hôm nay bạn đã nhận bùa miễn phí rồi!!!");
                                    }
                                    break;
                                case 1: // Sách tuyệt kỹ
                                    createOtherMenu(player, ConstNpc.SACH_TUYET_KY,
                                            "Ta có thể giúp gì cho ngươi ?",
                                            "Đóng thành\nSách cũ",
                                            "Đổi Sách\nTuyệt kỹ",
                                            "Giám định\nSách",
                                            "Tẩy\nSách",
                                            "Nâng cấp\nSách\nTuyệt kỹ",
                                            "Hồi phục\nSách",
                                            "Phân rã\nSách");
                                    break;
                                case 2: // shop bùa
                                    createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                            "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                    + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                            "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                            "Đóng");
                                    break;
                                case 3: // nâng cấp vật phẩm
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NANG_CAP_VAT_PHAM);
                                    break;
                                case 4: // mở chỉ số bông tai cấp 2
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.MO_CHI_SO_BONG_TAI);
                                    break;
                                case 5: // Làm phép nhập đá
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.LAM_PHEP_NHAP_DA);
                                    break;
                                case 6://
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NHAP_NGOC_RONG);
                                    break;
                            }
                        } else if (bongTai != null) {
                            switch (select) {
                                case 0: // Ngẫu nhiên bùa 1h
                                    if (player.event.luotNhanBuaMienPhi == 1) {
                                        int idItem = Util.nextInt(213, 219);
                                        player.charms.addTimeCharms(idItem, 60);
                                        Item bua = ItemService.gI().createNewItem((short) idItem);
                                        Service.getInstance().sendThongBao(player,
                                                "Bạn vừa nhận thưởng " + bua.getName());
                                        player.event.luotNhanBuaMienPhi = 0;
                                    } else {
                                        Service.getInstance().sendThongBao(player,
                                                "Hôm nay bạn đã nhận bùa miễn phí rồi!!!");
                                    }
                                    break;
                                case 1: // Sách tuyệt kỹ
                                    createOtherMenu(player, ConstNpc.SACH_TUYET_KY,
                                            "Ta có thể giúp gì cho ngươi ?",
                                            "Đóng thành\nSách cũ",
                                            "Đổi Sách\nTuyệt kỹ",
                                            "Giám định\nSách",
                                            "Tẩy\nSách",
                                            "Nâng cấp\nSách\nTuyệt kỹ",
                                            "Hồi phục\nSách",
                                            "Phân rã\nSách");
                                    break;
                                case 2: // shop bùa
                                    createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                            "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                    + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                            "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                            "Đóng");
                                    break;
                                case 3: // nâng cấp vật phẩm
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NANG_CAP_VAT_PHAM);
                                    break;
                                case 4: // nâng cấp bông tai cấp 2
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NANG_CAP_BONG_TAI);
                                    break;
                                case 5: // Làm phép nhập đá
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.LAM_PHEP_NHAP_DA);
                                    break;
                                case 6://
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NHAP_NGOC_RONG);
                                    break;
                            }
                        } else {
                            switch (select) {
                                case 0: // Ngẫu nhiên bùa 1h
                                    if (player.event.luotNhanBuaMienPhi == 1) {
                                        int idItem = Util.nextInt(213, 219);
                                        player.charms.addTimeCharms(idItem, 60);
                                        Item bua = ItemService.gI().createNewItem((short) idItem);
                                        Service.getInstance().sendThongBao(player,
                                                "Bạn vừa nhận thưởng " + bua.getName());
                                        player.event.luotNhanBuaMienPhi = 0;
                                    } else {
                                        Service.getInstance().sendThongBao(player,
                                                "Hôm nay bạn đã nhận bùa miễn phí rồi!!!");
                                    }
                                    break;
                                case 1: // Sách tuyệt kỹ
                                    createOtherMenu(player, ConstNpc.SACH_TUYET_KY,
                                            "Ta có thể giúp gì cho ngươi ?",
                                            "Đóng thành\nSách cũ",
                                            "Đổi Sách\nTuyệt kỹ",
                                            "Giám định\nSách",
                                            "Tẩy\nSách",
                                            "Nâng cấp\nSách\nTuyệt kỹ",
                                            "Hồi phục\nSách",
                                            "Phân rã\nSách");
                                    break;
                                case 2: // shop bùa
                                    createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                            "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                    + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                            "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                            "Đóng");
                                    break;
                                case 3: // nâng cấp vật phẩm
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NANG_CAP_VAT_PHAM);
                                    break;
                                case 4: // Làm phép nhập đá
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.LAM_PHEP_NHAP_DA);
                                    break;
                                case 5://
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NHAP_NGOC_RONG);
                                    break;
                            }
                        }
                    } else {
                        if (bongTaiCap2 != null) {
                            switch (select) {
                                case 0: // Sách tuyệt kỹ
                                    createOtherMenu(player, ConstNpc.SACH_TUYET_KY,
                                            "Ta có thể giúp gì cho ngươi ?",
                                            "Đóng thành\nSách cũ",
                                            "Đổi Sách\nTuyệt kỹ",
                                            "Giám định\nSách",
                                            "Tẩy\nSách",
                                            "Nâng cấp\nSách\nTuyệt kỹ",
                                            "Hồi phục\nSách",
                                            "Phân rã\nSách");
                                    break;
                                case 1: // shop bùa
                                    createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                            "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                    + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                            "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                            "Đóng");
                                    break;
                                case 2: // nâng cấp vật phẩm
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NANG_CAP_VAT_PHAM);
                                    break;
                                case 3:
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.MO_CHI_SO_BONG_TAI);
                                    break;
                                case 4: // Làm phép nhập đá
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.LAM_PHEP_NHAP_DA);
                                    break;
                                case 5://
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NHAP_NGOC_RONG);
                                    break;
                            }
                        } else if (bongTai != null) {
                            switch (select) {
                                case 0: // Sách tuyệt kỹ
                                    createOtherMenu(player, ConstNpc.SACH_TUYET_KY,
                                            "Ta có thể giúp gì cho ngươi ?",
                                            "Đóng thành\nSách cũ",
                                            "Đổi Sách\nTuyệt kỹ",
                                            "Giám định\nSách",
                                            "Tẩy\nSách",
                                            "Nâng cấp\nSách\nTuyệt kỹ",
                                            "Hồi phục\nSách",
                                            "Phân rã\nSách");
                                    break;
                                case 1: // shop bùa
                                    createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                            "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                    + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                            "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                            "Đóng");
                                    break;
                                case 2: // nâng cấp vật phẩm
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NANG_CAP_VAT_PHAM);
                                    break;
                                case 3:
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NANG_CAP_BONG_TAI);
                                    break;
                                case 4: // Làm phép nhập đá
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.LAM_PHEP_NHAP_DA);
                                    break;
                                case 5://
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NHAP_NGOC_RONG);
                                    break;
                            }
                        } else {
                            switch (select) {
                                case 0: // Sách tuyệt kỹ
                                    createOtherMenu(player, ConstNpc.SACH_TUYET_KY,
                                            "Ta có thể giúp gì cho ngươi ?",
                                            "Đóng thành\nSách cũ",
                                            "Đổi Sách\nTuyệt kỹ",
                                            "Giám định\nSách",
                                            "Tẩy\nSách",
                                            "Nâng cấp\nSách\nTuyệt kỹ",
                                            "Hồi phục\nSách",
                                            "Phân rã\nSách");
                                    break;
                                case 1: // shop bùa
                                    createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA,
                                            "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để "
                                                    + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                            "Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng",
                                            "Đóng");
                                    break;
                                case 2: // nâng cấp vật phẩm
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NANG_CAP_VAT_PHAM);
                                    break;
                                case 3: // Làm phép nhập đá
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.LAM_PHEP_NHAP_DA);
                                    break;
                                case 4://
                                    CombineServiceNew.gI().openTabCombine(player,
                                            CombineServiceNew.NHAP_NGOC_RONG);
                                    break;
                            }
                        }
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.SACH_TUYET_KY) {
                    switch (select) {
                        case 0:
                            Item trangSachCu = InventoryService.gI().findItemBagByTemp(player,
                                    1291);

                            Item biaSach = InventoryService.gI().findItemBagByTemp(player, 1281);
                            if ((trangSachCu != null && trangSachCu.quantity >= 9999)
                                    && (biaSach != null && biaSach.quantity >= 1)) {
                                createOtherMenu(player, ConstNpc.DONG_THANH_SACH_CU,
                                        "|2|Chế tạo Cuốn sách cũ\n"
                                                + "|1|Trang sách cũ " + trangSachCu.quantity
                                                + "/9999\n"
                                                + "Bìa sách " + biaSach.quantity + "/1\n"
                                                + "Tỉ lệ thành công: 20%\n"
                                                + "Thất bại mất 99 trang sách và 1 bìa sách",
                                        "Đồng ý", "Từ chối");
                                break;
                            } else {
                                String NpcSay = "|2|Chế tạo Cuốn sách cũ\n";
                                if (trangSachCu == null) {
                                    NpcSay += "|7|Trang sách cũ " + "0/9999\n";
                                } else {
                                    NpcSay += "|1|Trang sách cũ " + trangSachCu.quantity
                                            + "/9999\n";
                                }
                                if (biaSach == null) {
                                    NpcSay += "|7|Bìa sách " + "0/1\n";
                                } else {
                                    NpcSay += "|1|Bìa sách " + biaSach.quantity + "/1\n";
                                }

                                NpcSay += "|7|Tỉ lệ thành công: 20%\n";
                                NpcSay += "|7|Thất bại mất 99 trang sách và 1 bìa sách";
                                createOtherMenu(player, ConstNpc.DONG_THANH_SACH_CU_2,
                                        NpcSay, "Từ chối");
                                break;
                            }
                        case 1:
                            Item cuonSachCu = InventoryService.gI().findItemBagByTemp(player, 1284);
                            Item kimBam = InventoryService.gI().findItemBagByTemp(player, 1282);

                            if ((cuonSachCu != null && cuonSachCu.quantity >= 10)
                                    && (kimBam != null && kimBam.quantity >= 1)) {
                                createOtherMenu(player, ConstNpc.DOI_SACH_TUYET_KY,
                                        "|2|Đổi sách tuyệt kỹ 1\n"
                                                + "|1|Cuốn sách cũ " + cuonSachCu.quantity + "/10\n"
                                                + "Kìm bấm giấy " + kimBam.quantity + "/1\n"
                                                + "Tỉ lệ thành công: 20%\n",
                                        "Đồng ý", "Từ chối");
                                break;
                            } else {
                                String NpcSay = "|2|Đổi sách Tuyệt kỹ 1\n";
                                if (cuonSachCu == null) {
                                    NpcSay += "|7|Cuốn sách cũ " + "0/10\n";
                                } else {
                                    NpcSay += "|1|Cuốn sách cũ " + cuonSachCu.quantity + "/10\n";
                                }
                                if (kimBam == null) {
                                    NpcSay += "|7|Kìm bấm giấy " + "0/1\n";
                                } else {
                                    NpcSay += "|1|Kìm bấm giấy " + kimBam.quantity + "/1\n";
                                }
                                NpcSay += "|7|Tỉ lệ thành công: 20%\n";
                                createOtherMenu(player, ConstNpc.DOI_SACH_TUYET_KY_2,
                                        NpcSay, "Từ chối");
                            }
                            break;
                        case 2:// giám định sách
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.GIAM_DINH_SACH);
                            break;
                        case 3:// tẩy sách
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.TAY_SACH);
                            break;
                        case 4:// nâng cấp sách
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.NANG_CAP_SACH_TUYET_KY);
                            break;
                        case 5:// phục hồi sách
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.PHUC_HOI_SACH);
                            break;
                        case 6:// phân rã sách
                            CombineServiceNew.gI().openTabCombine(player,
                                    CombineServiceNew.PHAN_RA_SACH);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.DOI_SACH_TUYET_KY) {
                    switch (select) {
                        case 0:
                            Item cuonSachCu = InventoryService.gI().findItemBagByTemp(player, 1284);
                            Item kimBam = InventoryService.gI().findItemBagByTemp(player, 1282);

                            short baseValue = 1287;
                            short genderModifier = (player.gender == 0) ? -2
                                    : ((player.gender == 2) ? 2 : (short) 0);

                            Item sachTuyetKy = ItemService.gI()
                                    .createNewItem((short) (baseValue + genderModifier));

                            if (Util.isTrue(20, 100)) {

                                sachTuyetKy.itemOptions.add(new ItemOption(229, 0));
                                sachTuyetKy.itemOptions.add(new ItemOption(21, 40));
                                sachTuyetKy.itemOptions.add(new ItemOption(30, 0));
                                sachTuyetKy.itemOptions.add(new ItemOption(87, 1));
                                sachTuyetKy.itemOptions.add(new ItemOption(230, 5));
                                sachTuyetKy.itemOptions.add(new ItemOption(231, 1000));
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
                                            InventoryService.gI().getIndexBag(player, kimBam));
                                    msg.writer().writeByte(
                                            InventoryService.gI().getIndexBag(player, cuonSachCu));
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                    msg = new Message(-81);
                                    msg.writer().writeByte(7);
                                    msg.writer().writeShort(sachTuyetKy.template.iconID);
                                    msg.writer().writeShort(-1);
                                    msg.writer().writeShort(-1);
                                    msg.writer().writeShort(-1);
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                } catch (Exception e) {
                                }
                                InventoryService.gI().addItemList(player.inventory.itemsBag,
                                        sachTuyetKy, 1);
                                InventoryService.gI().subQuantityItemsBag(player, cuonSachCu, 10);
                                InventoryService.gI().subQuantityItemsBag(player, kimBam, 1);
                                InventoryService.gI().sendItemBags(player);
                                return;
                            } else {
                                try { // send effect faile
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
                                            InventoryService.gI().getIndexBag(player, kimBam));
                                    msg.writer().writeByte(
                                            InventoryService.gI().getIndexBag(player, cuonSachCu));
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                    msg = new Message(-81);
                                    msg.writer().writeByte(8);
                                    msg.writer().writeShort(-1);
                                    msg.writer().writeShort(-1);
                                    msg.writer().writeShort(-1);
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                } catch (Exception e) {
                                }
                                InventoryService.gI().subQuantityItemsBag(player, cuonSachCu, 5);
                                InventoryService.gI().subQuantityItemsBag(player, kimBam, 1);
                                InventoryService.gI().sendItemBags(player);
                            }
                            return;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.DONG_THANH_SACH_CU) {
                    switch (select) {
                        case 0:
                            Item trangSachCu = InventoryService.gI().findItemBagByTemp(player,
                                    1291);
                            Item biaSach = InventoryService.gI().findItemBagByTemp(player, 1281);
                            Item cuonSachCu = ItemService.gI().createNewItem((short) 1284);
                            if (Util.isTrue(20, 100)) {
                                cuonSachCu.itemOptions.add(new ItemOption(30, 0));

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
                                            InventoryService.gI().getIndexBag(player, trangSachCu));
                                    msg.writer().writeByte(
                                            InventoryService.gI().getIndexBag(player, biaSach));
                                    player.sendMessage(msg);
                                    msg.cleanup();

                                    msg = new Message(-81);
                                    msg.writer().writeByte(7);
                                    msg.writer().writeShort(cuonSachCu.template.iconID);
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                } catch (Exception e) {
                                }

                                InventoryService.gI().addItemList(player.inventory.itemsBag,
                                        cuonSachCu, 99);
                                InventoryService.gI().subQuantityItemsBag(player, trangSachCu,
                                        9999);
                                InventoryService.gI().subQuantityItemsBag(player, biaSach, 1);
                                InventoryService.gI().sendItemBags(player);
                                return;
                            } else {
                                try { // send effect faile
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
                                            InventoryService.gI().getIndexBag(player, biaSach));
                                    msg.writer().writeByte(
                                            InventoryService.gI().getIndexBag(player, trangSachCu));
                                    player.sendMessage(msg);
                                    msg.cleanup();

                                    msg = new Message(-81);
                                    msg.writer().writeByte(8);
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                } catch (Exception e) {
                                }
                                InventoryService.gI().subQuantityItemsBag(player, trangSachCu, 99);
                                InventoryService.gI().subQuantityItemsBag(player, biaSach, 1);
                                InventoryService.gI().sendItemBags(player);
                            }
                            return;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_SHOP_BUA) {
                    switch (select) {
                        case 0:
                            ShopService.gI().openShopBua(player, ConstNpc.SHOP_BA_HAT_MIT_0, 0);
                            break;
                        case 1:
                            ShopService.gI().openShopBua(player, ConstNpc.SHOP_BA_HAT_MIT_1, 1);
                            break;
                        case 2:
                            ShopService.gI().openShopBua(player, ConstNpc.SHOP_BA_HAT_MIT_2, 2);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                    switch (player.combineNew.typeCombine) {
                        case CombineServiceNew.NANG_CAP_VAT_PHAM:
                            if (select == 0) {
                                player.iDMark.isUseTuiBaoVeNangCap = false;
                                CombineServiceNew.gI().startCombine(player, select);
                            } else if (select == 1) {
                                player.iDMark.isUseTuiBaoVeNangCap = true;
                                CombineServiceNew.gI().startCombine(player, select);
                            }
                            break;
                        case CombineServiceNew.NANG_CAP_BONG_TAI:
                        case CombineServiceNew.MO_CHI_SO_BONG_TAI:
                        case CombineServiceNew.LAM_PHEP_NHAP_DA:
                        case CombineServiceNew.NHAP_NGOC_RONG:
                            // START _ SÁCH TUYỆT KỸ//
                        case CombineServiceNew.GIAM_DINH_SACH:
                        case CombineServiceNew.TAY_SACH:
                        case CombineServiceNew.NANG_CAP_SACH_TUYET_KY:
                        case CombineServiceNew.PHUC_HOI_SACH:
                        case CombineServiceNew.PHAN_RA_SACH:
                            // END _ SÁCH TUYỆT KỸ//
                            CombineServiceNew.gI().startCombine(player, select);
                            break;
                    }
                }
            }
        }
    }
}
