package nro.models.npc;

import nro.models.npc_list.*;

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

/**
 * @author 💖 YTB KhanhDTK 💖
 *
 */
public class NpcFactory {

    // playerid - object
    public static final java.util.Map<Long, Object> PLAYERID_OBJECT = new HashMap<>();

    private NpcFactory() {

    }

    public static Npc createNPC(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        Npc npc = null;
        try {
            npc = switch (tempId) {
                case ConstNpc.OBITO -> new Obito(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.TORIBOT -> new ToriBot(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.NGO_KHONG -> new NgoKhong(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.DUONG_TANG -> new DuongTang(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.TAPION -> new Tapion(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.MR_POPO -> new MrPoPo(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.QUY_LAO_KAME -> new QuyLaoKame(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.TRUONG_LAO_GURU -> new TruongLaoGuru(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.VUA_VEGETA -> new VuaVegeta(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.ONG_GOHAN -> new OngGohan(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.ONG_MOORI -> new OngMoori(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.ONG_PARAGUS -> new OngParagus(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.GOGETA_SSJ4 -> new GogetaSSJ4(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.BUNMA -> new Bulma(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.DENDE -> new Dende(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.APPULE -> new Appule(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.DR_DRIEF -> new DrDrief(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.CARGO -> new Cargo(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.CUI -> new Cui(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.SANTA -> new Santa(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.URON -> new Uron(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.BA_HAT_MIT -> new BaHatMit(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.RUONG_DO -> new RuongDo(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.DAU_THAN -> new DauThan(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.CALICK -> new Calick(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.JACO -> new Jaco(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.POTAGE -> new Potage(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.THAN_MEO_KARIN -> new Karin(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.THUONG_DE -> new ThuongDe(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.THAN_VU_TRU -> new ThanVuTru(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.TO_SU_KAIO -> new ToSuKaio(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.DOC_NHAN -> new DocNhan(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.KIBIT -> new Kibit(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.TRONG_TAI -> new TrongTai(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.OSIN -> new Osin(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.BABIDAY -> new Babiday(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.LINH_CANH -> new LinhCanh(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.QUA_TRUNG -> new QuaTrung(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.QUOC_VUONG -> new QuocVuong(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.BUNMA_TL -> new BulmaTuongLai(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.RONG_OMEGA -> new RongOmega(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.RONG_1S -> new Rong1Sao(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.RONG_2S -> new Rong2Sao(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.RONG_3S -> new Rong3Sao(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.RONG_4S -> new Rong4Sao(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.RONG_5S -> new Rong5Sao(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.RONG_6S -> new Rong6Sao(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.RONG_7S -> new Rong7Sao(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.NPC_64 -> new DaiThienSu(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.BILL -> new Bill(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.WHIS -> new Whis(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.BO_MONG -> new BoMong(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.GOKU_SSJ -> new GokuSSJ(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.GOKU_SSJ_ -> new GokuSSJ2(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.GHI_DANH -> new GhiDanh(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.NOI_BANH -> new NoiBanh(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.KING_FURY -> new KingFury(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.CUA_HANG_KY_GUI -> new KyGui(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.BARDOCK -> new Bardock(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.CHI_CHI -> new ChiChi(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.DR_MYUU -> new DrMyuu(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.DUA_HAU -> new DuaHau(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.GIUMA_DAU_BO -> new GiuMaDauBo(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.LY_TIEU_NUONG -> new LyTieuNuong(mapId, status, cx, cy, tempId, avartar);
                case ConstNpc.VADOS -> new Vados(mapId, status, cx, cy, tempId, avartar);
                default -> new Npc(mapId, status, cx, cy, tempId, avartar) {
                    @Override
                    public void openBaseMenu(Player player) {
                        if (canOpenNpc(player)) {
                            super.openBaseMenu(player);
                        }
                    }

                    @Override
                    public void confirmMenu(Player player, int select) {
                        if (canOpenNpc(player)) {
                        }
                    }
                };
            };
        } catch (Exception e) {
            Log.error(NpcFactory.class, e, "L?i load npc");
        }
        return npc;
    }
    // girlkun75-mark

    public static void createNpcRongThieng() {
        Npc npc = new Npc(-1, -1, -1, -1, ConstNpc.RONG_THIENG, -1) {
            @Override
            public void confirmMenu(Player player, int select) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.IGNORE_MENU:

                        break;
                    case ConstNpc.SHENRON_CONFIRM:
                        if (select == 0) {
                            SummonDragon.gI().confirmWish();
                        } else if (select == 1) {
                            SummonDragon.gI().reOpenShenronWishes(player);
                        }
                        break;
                    case ConstNpc.SHENRON_1_1:
                        if (player.iDMark.getIndexMenu() == ConstNpc.SHENRON_1_1
                                && select == SHENRON_1_STAR_WISHES_1.length - 1) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.SHENRON_1_2, SHENRON_SAY,
                                    SHENRON_1_STAR_WISHES_2);
                            break;
                        }
                    case ConstNpc.SHENRON_1_2:
                        if (player.iDMark.getIndexMenu() == ConstNpc.SHENRON_1_2
                                && select == SHENRON_1_STAR_WISHES_2.length - 1) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.SHENRON_1_1, SHENRON_SAY,
                                    SHENRON_1_STAR_WISHES_1);
                            break;
                        }
                    case ConstNpc.BLACK_SHENRON:
                        if (player.iDMark.getIndexMenu() == ConstNpc.BLACK_SHENRON
                                && select == BLACK_SHENRON_WISHES.length) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.BLACK_SHENRON, BLACK_SHENRON_SAY,
                                    BLACK_SHENRON_WISHES);
                            break;
                        }
                    case ConstNpc.ICE_SHENRON:
                        if (player.iDMark.getIndexMenu() == ConstNpc.ICE_SHENRON
                                && select == ICE_SHENRON_WISHES.length) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.ICE_SHENRON, ICE_SHENRON_SAY,
                                    ICE_SHENRON_WISHES);
                            break;
                        }
                    default:
                        SummonDragon.gI().showConfirmShenron(player, player.iDMark.getIndexMenu(), (byte) select);
                        break;
                }
            }
        };
    }

    public static void createNpcConMeo() {
        Npc npc = new Npc(-1, -1, -1, -1, ConstNpc.CON_MEO, 351) {
            @Override
            public void confirmMenu(Player player, int select) {
                switch (player.iDMark.getIndexMenu()) {

                    case ConstNpc.CONFIRM_DIALOG:
                        ConfirmDialog confirmDialog = player.getConfirmDialog();
                        if (confirmDialog != null) {
                            if (confirmDialog instanceof MenuDialog menu) {
                                menu.getRunable().setIndexSelected(select);
                                menu.run();
                                return;
                            }
                            if (select == 0) {
                                confirmDialog.run();
                            } else {
                                confirmDialog.cancel();
                            }
                            player.setConfirmDialog(null);
                        }
                        break;
                    case 25100303:
                        switch (select) {
                            case 0:
                                PlayerDAO.saveMaBaoVe(player, player.MaBaoVe_TamThoi);
                                PlayerDAO.Bat_Tat_MaBaoVe(player, select);
                                player.MaBaoVe = player.MaBaoVe_TamThoi;
                                player.isUseMaBaoVe = true;
                                Service.getInstance().sendThongBao(player,
                                        "Kích hoạt thành công, tài khoản đang được bảo vệ");
                                PlayerService.gI().savePlayer(player);
                                break;
                            case 1:
                                break;
                        }
                        break;
                    case 25100304:
                        switch (select) {
                            case 0:
                                PlayerDAO.Bat_Tat_MaBaoVe(player, 1);
                                player.isUseMaBaoVe = false;
                                PlayerService.gI().savePlayer(player);
                                Service.getInstance().sendThongBao(player, "Chức năng bảo vệ tài khoản đang tắt");
                                break;
                            case 1:
                                break;
                        }
                        break;
                    case 25100305:
                        switch (select) {
                            case 0:
                                PlayerDAO.Bat_Tat_MaBaoVe(player, 0);
                                player.isUseMaBaoVe = true;
                                PlayerService.gI().savePlayer(player);
                                Service.getInstance().sendThongBao(player, "Tài khoản đang được bảo vệ");
                                break;
                            case 1:
                                break;
                        }
                        break;
                    case ConstNpc.HOP_QUA_THAN_LINH:

                        Item aotl_td = ItemService.gI().createNewItem((short) 555);
                        Item aotl_nm = ItemService.gI().createNewItem((short) 557);
                        Item aotl_xd = ItemService.gI().createNewItem((short) 559);

                        aotl_td.itemOptions.add(new ItemOption(47, 800 + new Random().nextInt(200)));

                        aotl_nm.itemOptions.add(new ItemOption(47, 900 + new Random().nextInt(100)));

                        aotl_xd.itemOptions.add(new ItemOption(47, 950 + new Random().nextInt(200)));

                        aotl_td.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        aotl_nm.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        aotl_xd.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ

                        Item quantl_td = ItemService.gI().createNewItem((short) 556);
                        Item quantl_nm = ItemService.gI().createNewItem((short) 558);
                        Item quantl_xd = ItemService.gI().createNewItem((short) 560);

                        quantl_td.itemOptions.add(new ItemOption(22, 47 + new Random().nextInt(5)));
                        quantl_td.itemOptions.add(new ItemOption(27, (47 + new Random().nextInt(5)) * 1000 * 15 / 100));

                        quantl_nm.itemOptions.add(new ItemOption(22, 45 + new Random().nextInt(5)));
                        quantl_nm.itemOptions.add(new ItemOption(27, (45 + new Random().nextInt(5)) * 1000 * 15 / 100));

                        quantl_xd.itemOptions.add(new ItemOption(22, 42 + new Random().nextInt(8)));
                        quantl_xd.itemOptions.add(new ItemOption(27, (42 + new Random().nextInt(8)) * 1000 * 15 / 100));

                        quantl_td.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        quantl_nm.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        quantl_xd.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ

                        Item gangtl_td = ItemService.gI().createNewItem((short) 562);
                        Item gangtl_nm = ItemService.gI().createNewItem((short) 564);
                        Item gangtl_xd = ItemService.gI().createNewItem((short) 566);

                        gangtl_td.itemOptions.add(new ItemOption(0, 3500 + new Random().nextInt(1200)));
                        gangtl_nm.itemOptions.add(new ItemOption(0, 3300 + new Random().nextInt(1100)));
                        gangtl_xd.itemOptions.add(new ItemOption(0, 3500 + new Random().nextInt(1400)));

                        gangtl_td.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        gangtl_nm.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        gangtl_xd.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ

                        Item giaytl_td = ItemService.gI().createNewItem((short) 563);
                        Item giaytl_nm = ItemService.gI().createNewItem((short) 565);
                        Item giaytl_xd = ItemService.gI().createNewItem((short) 567);

                        giaytl_td.itemOptions.add(new ItemOption(23, 42 + new Random().nextInt(5)));
                        giaytl_nm.itemOptions.add(new ItemOption(23, 47 + new Random().nextInt(5)));
                        giaytl_xd.itemOptions.add(new ItemOption(23, 45 + new Random().nextInt(4)));

                        giaytl_td.itemOptions.add(new ItemOption(28, (42 + new Random().nextInt(5)) * 1000 * 15 / 100));
                        giaytl_nm.itemOptions.add(new ItemOption(28, (47 + new Random().nextInt(5)) * 1000 * 15 / 100));
                        giaytl_xd.itemOptions.add(new ItemOption(28, (45 + new Random().nextInt(4)) * 1000 * 15 / 100));

                        giaytl_td.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        giaytl_nm.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ
                        giaytl_xd.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ

                        Item nhan = ItemService.gI().createNewItem((short) 561);

                        nhan.itemOptions.add(new ItemOption(14, 14 + new Random().nextInt(4)));
                        nhan.itemOptions.add(new ItemOption(21, 18)); // ycsm 18 tỉ

                        Item HopQuaThanLinh = InventoryService.gI().findItemBagByTemp(player, 1280);

                        switch (select) {

                            case 0:
                                if (InventoryService.gI().getCountEmptyBag(player) < 5) {
                                    Service.getInstance().sendThongBao(player, "Cần 5 ô hành trang mới có thể mở!!!");
                                    return;
                                }
                                InventoryService.gI().addItemBag(player, aotl_td, 1);
                                InventoryService.gI().addItemBag(player, quantl_td, 1);
                                InventoryService.gI().addItemBag(player, gangtl_td, 1);
                                InventoryService.gI().addItemBag(player, giaytl_td, 1);
                                InventoryService.gI().addItemBag(player, nhan, 1);
                                InventoryService.gI().subQuantityItemsBag(player, HopQuaThanLinh, 1);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được 1 set thần linh trái đất");
                                return;
                            case 1:
                                if (InventoryService.gI().getCountEmptyBag(player) < 5) {
                                    Service.getInstance().sendThongBao(player, "Cần 5 ô hành trang mới có thể mở!!!");
                                    return;
                                }

                                InventoryService.gI().addItemBag(player, aotl_nm, 1);
                                InventoryService.gI().addItemBag(player, quantl_nm, 1);
                                InventoryService.gI().addItemBag(player, gangtl_nm, 1);
                                InventoryService.gI().addItemBag(player, giaytl_nm, 1);
                                InventoryService.gI().addItemBag(player, nhan, 1);
                                InventoryService.gI().subQuantityItemsBag(player, HopQuaThanLinh, 1);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được 1 set thần linh namek");
                                InventoryService.gI().sendItemBags(player);
                                return;
                            case 2:
                                if (InventoryService.gI().getCountEmptyBag(player) < 5) {
                                    Service.getInstance().sendThongBao(player, "Cần 5 ô hành trang mới có thể mở!!!");
                                    return;
                                }

                                InventoryService.gI().addItemBag(player, aotl_xd, 1);
                                InventoryService.gI().addItemBag(player, quantl_xd, 1);
                                InventoryService.gI().addItemBag(player, gangtl_xd, 1);
                                InventoryService.gI().addItemBag(player, giaytl_xd, 1);
                                InventoryService.gI().addItemBag(player, nhan, 1);
                                InventoryService.gI().subQuantityItemsBag(player, HopQuaThanLinh, 1);
                                InventoryService.gI().sendItemBags(player);

                                Service.getInstance().sendThongBao(player, "Bạn nhận được 1 set thần linh xayda");
                                return;
                        }
                        return;
                    case ConstNpc.UP_TOP_ITEM:

                        break;
                    case ConstNpc.RUONG_GO:
                        int size = player.textRuongGo.size();
                        if (size > 0) {
                            String menuselect = "OK [" + (size - 1) + "]";
                            if (size == 1) {
                                menuselect = "OK";
                            }
                            NpcService.gI().createMenuConMeo(player, ConstNpc.RUONG_GO, -1,
                                    player.textRuongGo.get(size - 1), menuselect);
                            player.textRuongGo.remove(size - 1);
                        }
                        break;
                    case ConstNpc.MENU_MABU_WAR:
                        if (select == 0) {
                            if (player.zone.finishMabuWar) {
                                ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                            } else if (player.zone.map.mapId == 119) {
                                Zone zone = MabuWar.gI().getMapLastFloor(120);
                                if (zone != null) {
                                    ChangeMapService.gI().changeMap(player, zone, 354, 240);
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Trận đại chiến đã kết thúc, tàu vận chuyển sẽ đưa bạn về nhà");
                                    ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                                }
                            } else {
                                int idMapNextFloor = player.zone.map.mapId == 115 ? player.zone.map.mapId + 2
                                        : player.zone.map.mapId + 1;
                                ChangeMapService.gI().changeMap(player, idMapNextFloor, -1, 354, 240);
                            }
                            player.resetPowerPoint();
                            player.sendMenuGotoNextFloorMabuWar = false;
                            Service.getInstance().sendPowerInfo(player, "TL", player.getPowerPoint());
                            if (Util.isTrue(1, 30)) {
                                player.inventory.ruby += 1;
                                PlayerService.gI().sendInfoHpMpMoney(player);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được 1 Hồng Ngọc");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Bạn đen vô cùng luôn nên không nhận được gì cả");
                            }
                        }
                        break;
                    case ConstNpc.IGNORE_MENU:

                        break;
                    case ConstNpc.MAKE_MATCH_PVP:
                        // PVP_old.gI().sendInvitePVP(player, (byte) select);
                        PVPServcice.gI().sendInvitePVP(player, (byte) select);
                        break;
                    case ConstNpc.MAKE_FRIEND:
                        if (select == 0) {
                            Object playerId = PLAYERID_OBJECT.get(player.id);
                            if (playerId != null) {
                                FriendAndEnemyService.gI().acceptMakeFriend(player,
                                        Integer.parseInt(String.valueOf(playerId)));
                            }
                        }
                        break;
                    case ConstNpc.REVENGE:
                        if (select == 0) {
                            PVPServcice.gI().acceptRevenge(player);
                        }
                        break;
                    case ConstNpc.TUTORIAL_SUMMON_DRAGON:
                        if (select == 0) {
                            NpcService.gI().createTutorial(player, -1, SummonDragon.SUMMON_SHENRON_TUTORIAL);
                        }
                        break;
                    case ConstNpc.SUMMON_SHENRON:
                        if (select == 0) {
                            NpcService.gI().createTutorial(player, -1, SummonDragon.SUMMON_SHENRON_TUTORIAL);
                        } else if (select == 1) {
                            SummonDragon.gI().summonShenron(player);
                        }
                        break;
                    case ConstNpc.SUMMON_BLACK_SHENRON:
                        if (select == 0) {
                            SummonDragon.gI().summonBlackShenron(player);
                        }
                        break;
                    case ConstNpc.SUMMON_ICE_SHENRON:
                        if (select == 0) {
                            SummonDragon.gI().summonIceShenron(player);
                        }
                        break;
                    case ConstNpc.INTRINSIC:
                        try {
                            if (select == 0) {
                                IntrinsicService.gI().showAllIntrinsic(player);
                            } else if (select == 1) {
                                IntrinsicService.gI().showConfirmOpen(player);
                            } else if (select == 2) {
                                IntrinsicService.gI().showConfirmOpenVip(player);
                            }
                        } catch (Exception e) {
                            /// VMN
                        }
                        break;

                    case ConstNpc.CONFIRM_OPEN_INTRINSIC:
                        if (select == 0) {
                            IntrinsicService.gI().open(player);
                        }
                        break;
                    case ConstNpc.CONFIRM_OPEN_INTRINSIC_VIP:
                        if (select == 0) {
                            IntrinsicService.gI().openVip(player);
                        }
                        break;
                    case ConstNpc.CONFIRM_LEAVE_CLAN:
                        if (select == 0) {
                            ClanService.gI().leaveClan(player);
                        }
                        break;
                    case ConstNpc.CONFIRM_NHUONG_PC:
                        if (select == 0) {
                            ClanService.gI().phongPc(player, (int) PLAYERID_OBJECT.get(player.id));
                        }
                        break;
                    case ConstNpc.BAN_PLAYER:
                        if (select == 0) {
                            PlayerService.gI().banPlayer((Player) PLAYERID_OBJECT.get(player.id));
                            Service.getInstance().sendThongBao(player,
                                    "Ban người chơi " + ((Player) PLAYERID_OBJECT.get(player.id)).name + " thành công");
                        }
                        break;
                    case ConstNpc.BUFF_PET:
                        if (select == 0) {
                            Player pl = (Player) PLAYERID_OBJECT.get(player.id);
                            if (pl.pet == null) {
                                PetService.gI().createNormalPet(pl);
                                Service.getInstance().sendThongBao(player, "Phát đệ tử cho "
                                        + ((Player) PLAYERID_OBJECT.get(player.id)).name + " thành công");
                            }
                        }
                        break;

                    case ConstNpc.MENU_ADMIN:
                        switch (select) {
                            case 0:
                                for (int i = 14; i <= 20; i++) {
                                    Item item = ItemService.gI().createNewItem((short) i);
                                    InventoryService.gI().addItemBag(player, item, 0);
                                }
                                InventoryService.gI().sendItemBags(player);
                                break;
                            case 1:
                                if (player.pet == null) {
                                    PetService.gI().createNormalPet(player);
                                } else {
                                    if (player.pet.isMabu) {
                                        PetService.gI().changeNormalPet(player);
                                    } else {
                                        PetService.gI().changeMabuPet(player);
                                    }
                                    PetService.gI().changeSuperPet(player, player.gender, 1);
                                }
                                break;
                            case 2:
                                NpcService.gI().createMenuConMeo(player, ConstNpc.BAO_TRI, 17997,
                                        "Vui lòng chọn ?", "Bảo Trì 5s", "Bảo Trì 30s", "Kích All");
                                break;
                            case 3:
                                Input.gI().createFormFindPlayer(player);
                                break;
                        }
                        break;
                    case ConstNpc.MENU_BUFF_DO:
                        switch (select) {
                            case 0:
                                NpcService.gI().createMenuConMeo(player, ConstNpc.HOP_QUA_THAN_LINH, 9840,
                                        "-----YTB KhanhDTK-----",
                                        "Trái đất", "Namek", "Xayda");
                                break;
                            case 1:
                                Input.gI().createFormTangRuby(player);
                                break;
                            case 2:
                                Input.gI().createFormTangSKH(player);
                                break;
                        }
                        break;
                    case ConstNpc.BAO_TRI:
                        switch (select) {
                            case 0: {
                                Maintenance.gI().start(5);
                                break;
                            }
                            case 1: {
                                Maintenance.gI().start(30);
                                break;
                            }
                            case 2: {
                                new Thread(() -> {
                                    Client.gI().close();
                                }, "kick all player").start();

                                break;
                            }
                        }
                        break;
                    case ConstNpc.CONFIRM_REMOVE_ALL_ITEM_LUCKY_ROUND:
                        if (select == 0) {
                            for (int i = 0; i < player.inventory.itemsBoxCrackBall.size(); i++) {
                                player.inventory.itemsBoxCrackBall.set(i, ItemService.gI().createItemNull());
                            }
                            Service.getInstance().sendThongBao(player, "Đã xóa hết vật phẩm trong rương");
                        }
                        break;
                    case ConstNpc.MENU_FIND_PLAYER:
                        Player p = (Player) PLAYERID_OBJECT.get(player.id);
                        if (p != null) {
                            switch (select) {
                                case 0:
                                    if (p.zone != null) {
                                        ChangeMapService.gI().changeMapYardrat(player, p.zone, p.location.x,
                                                p.location.y);
                                    }
                                    break;
                                case 1:
                                    if (p.zone != null) {
                                        ChangeMapService.gI().changeMap(p, player.zone, player.location.x,
                                                player.location.y);
                                    }
                                    break;
                                case 2:
                                    if (p != null) {
                                        Input.gI().createFormChangeName(player, p);
                                    }
                                    break;
                                case 3:
                                    if (p != null) {
                                        String[] selects = new String[] { "Đồng ý", "Hủy" };
                                        NpcService.gI().createMenuConMeo(player, ConstNpc.BAN_PLAYER, -1,
                                                "Bạn có chắc chắn muốn ban " + p.name, selects, p);
                                    }
                                    break;
                            }
                        }
                        break;
                }
            }
        };
    }

    public static void processGemPurchase(Player player, int requiredVndBar, int gemAmount) {
        if (player.soDuVND >= requiredVndBar) {
            player.inventory.gem += gemAmount;
            player.soDuVND -= requiredVndBar;
            PlayerDAO.subVndBar(player, requiredVndBar);
            Service.getInstance().sendMoney(player);
            Service.getInstance().sendThongBao(player, "Bạn có thêm " + Util.mumberToLouis(gemAmount) + " ngọc xanh");
        } else {
            Service.getInstance().sendThongBao(player, "Bạn không đủ số dư");
        }
    }

    public static void processThoiVangPurchase(Player player, int requiredVndBar, int gemAmount) {
        if (player.soDuVND >= requiredVndBar) {
            player.soDuVND -= requiredVndBar;
            player.soThoiVang += gemAmount;
            PlayerDAO.subVndBar(player, requiredVndBar);
            PlayerDAO.addGoldBar(player, gemAmount);
            Service.getInstance().sendThongBao(player, "Bạn có thêm " + Util.mumberToLouis(gemAmount) + " thỏi vàng");
            int sodabv = 0;
            switch (requiredVndBar) {
                case 10000:
                    sodabv = 1;
                    break;
                case 20000:
                    sodabv = Util.nextInt(1, 3);
                    break;
                case 30000:
                    sodabv = Util.nextInt(1, 3);
                    break;
                case 50000:
                    sodabv = Util.nextInt(1, 3);
                    break;
                case 100000:
                    sodabv = Util.nextInt(1, 3);
                    break;
                case 200000:
                    sodabv = Util.nextInt(1, 3);
                    break;
                case 500000:
                    sodabv = Util.nextInt(1, 3);
                case 1000000:
                    sodabv = Util.nextInt(1, 3);
            }
            Item dabaove = ItemService.gI().createNewItem((short) 987, sodabv);
            InventoryService.gI().addItemBag(player, dabaove, 9999);
            InventoryService.gI().sendItemBags(player);
            Service.getInstance().sendThongBao(player, "Bạn nhận được " + sodabv + " " + dabaove.getName());
            return;

        } else {
            Service.getInstance().sendThongBao(player, "Bạn không đủ số dư");
        }
    }

    public static void openMenuSuKien(Player player, Npc npc, int tempId, int select) {
        switch (Manager.EVENT_SEVER) {
            case 0:
                break;
            case 1:// hlw
                switch (select) {
                    case 0:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            Item keo = InventoryService.gI().finditemnguyenlieuKeo(player);
                            Item banh = InventoryService.gI().finditemnguyenlieuBanh(player);
                            Item bingo = InventoryService.gI().finditemnguyenlieuBingo(player);

                            if (keo != null && banh != null && bingo != null) {
                                Item GioBingo = ItemService.gI().createNewItem((short) 2016, 1);

                                // - Số item sự kiện có trong rương
                                InventoryService.gI().subQuantityItemsBag(player, keo, 10);
                                InventoryService.gI().subQuantityItemsBag(player, banh, 10);
                                InventoryService.gI().subQuantityItemsBag(player, bingo, 10);

                                GioBingo.itemOptions.add(new ItemOption(74, 0));
                                InventoryService.gI().addItemBag(player, GioBingo, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Đổi quà sự kiện thành công");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Vui lòng chuẩn bị x10 Nguyên Liệu Kẹo, Bánh Quy, Bí Ngô để đổi vật phẩm sự kiện");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                        }
                        break;
                    case 1:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            Item ve = InventoryService.gI().finditemnguyenlieuVe(player);
                            Item giokeo = InventoryService.gI().finditemnguyenlieuGiokeo(player);

                            if (ve != null && giokeo != null) {
                                Item Hopmaquy = ItemService.gI().createNewItem((short) 2017, 1);
                                // - Số item sự kiện có trong rương
                                InventoryService.gI().subQuantityItemsBag(player, ve, 3);
                                InventoryService.gI().subQuantityItemsBag(player, giokeo, 3);

                                Hopmaquy.itemOptions.add(new ItemOption(74, 0));
                                InventoryService.gI().addItemBag(player, Hopmaquy, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Đổi quà sự kiện thành công");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Vui lòng chuẩn bị x3 Vé đổi Kẹo và x3 Giỏ kẹo để đổi vật phẩm sự kiện");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                        }
                        break;
                    case 2:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            Item ve = InventoryService.gI().finditemnguyenlieuVe(player);
                            Item giokeo = InventoryService.gI().finditemnguyenlieuGiokeo(player);
                            Item hopmaquy = InventoryService.gI().finditemnguyenlieuHopmaquy(player);

                            if (ve != null && giokeo != null && hopmaquy != null) {
                                Item HopQuaHLW = ItemService.gI().createNewItem((short) 2012, 1);
                                // - Số item sự kiện có trong rương
                                InventoryService.gI().subQuantityItemsBag(player, ve, 3);
                                InventoryService.gI().subQuantityItemsBag(player, giokeo, 3);
                                InventoryService.gI().subQuantityItemsBag(player, hopmaquy, 3);

                                HopQuaHLW.itemOptions.add(new ItemOption(74, 0));
                                HopQuaHLW.itemOptions.add(new ItemOption(30, 0));
                                InventoryService.gI().addItemBag(player, HopQuaHLW, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player,
                                        "Đổi quà hộp quà sự kiện Halloween thành công");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Vui lòng chuẩn bị x3 Hộp Ma Quỷ, x3 Vé đổi Kẹo và x3 Giỏ kẹo để đổi vật phẩm sự kiện");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                        }
                        break;
                }
                break;
            case 2:// 20/11
                switch (select) {
                    case 3:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            int evPoint = player.event.getEventPoint();
                            if (evPoint >= 999) {
                                Item HopQua = ItemService.gI().createNewItem((short) 2021, 1);
                                player.event.setEventPoint(evPoint - 999);

                                HopQua.itemOptions.add(new ItemOption(74, 0));
                                HopQua.itemOptions.add(new ItemOption(30, 0));
                                InventoryService.gI().addItemBag(player, HopQua, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được Hộp Quà Teacher Day");
                            } else {
                                Service.getInstance().sendThongBao(player, "Cần 999 điểm tích lũy để đổi");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                        }
                        break;
                    // case 4:
                    // ShopService.gI().openShopSpecial(player, npc, ConstNpc.SHOP_HONG_NGOC, 0,
                    // -1);
                    // break;
                    default:
                        int n = 0;
                        switch (select) {
                            case 0:
                                n = 1;
                                break;
                            case 1:
                                n = 10;
                                break;
                            case 2:
                                n = 99;
                                break;
                        }

                        if (n > 0) {
                            Item bonghoa = InventoryService.gI().finditemBongHoa(player, n);
                            if (bonghoa != null) {
                                int evPoint = player.event.getEventPoint();
                                player.event.setEventPoint(evPoint + n);
                                ;
                                InventoryService.gI().subQuantityItemsBag(player, bonghoa, n);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được " + n + " điểm sự kiện");
                                int pre;
                                int next;
                                String text = null;
                                AttributeManager am = ServerManager.gI().getAttributeManager();
                                switch (tempId) {
                                    case ConstNpc.THAN_MEO_KARIN:
                                        pre = EVENT_COUNT_THAN_MEO / 999;
                                        EVENT_COUNT_THAN_MEO += n;
                                        next = EVENT_COUNT_THAN_MEO / 999;
                                        if (pre != next) {
                                            am.setTime(ConstAttribute.TNSM, 3600);
                                            text = "Toàn bộ máy chủ tăng được 20% TNSM cho đệ tử khi đánh quái trong 60 phút.";
                                        }
                                        break;

                                    case ConstNpc.QUY_LAO_KAME:
                                        pre = EVENT_COUNT_QUY_LAO_KAME / 999;
                                        EVENT_COUNT_QUY_LAO_KAME += n;
                                        next = EVENT_COUNT_QUY_LAO_KAME / 999;
                                        if (pre != next) {
                                            am.setTime(ConstAttribute.VANG, 3600);
                                            text = "Toàn bộ máy chủ được tăng 100% vàng từ quái trong 60 phút.";
                                        }
                                        break;

                                    case ConstNpc.THUONG_DE:
                                        pre = EVENT_COUNT_THUONG_DE / 999;
                                        EVENT_COUNT_THUONG_DE += n;
                                        next = EVENT_COUNT_THUONG_DE / 999;
                                        if (pre != next) {
                                            am.setTime(ConstAttribute.KI, 3600);
                                            text = "Toàn bộ máy chủ được tăng 20% KI trong 60 phút.";
                                        }
                                        break;

                                    case ConstNpc.THAN_VU_TRU:
                                        pre = EVENT_COUNT_THAN_VU_TRU / 999;
                                        EVENT_COUNT_THAN_VU_TRU += n;
                                        next = EVENT_COUNT_THAN_VU_TRU / 999;
                                        if (pre != next) {
                                            am.setTime(ConstAttribute.HP, 3600);
                                            text = "Toàn bộ máy chủ được tăng 20% HP trong 60 phút.";
                                        }
                                        break;

                                    case ConstNpc.BILL:
                                        pre = EVENT_COUNT_THAN_HUY_DIET / 999;
                                        EVENT_COUNT_THAN_HUY_DIET += n;
                                        next = EVENT_COUNT_THAN_HUY_DIET / 999;
                                        if (pre != next) {
                                            am.setTime(ConstAttribute.SUC_DANH, 3600);
                                            text = "Toàn bộ máy chủ được tăng 20% Sức đánh trong 60 phút.";
                                        }
                                        break;
                                }
                                if (text != null) {
                                    Service.getInstance().sendThongBaoAllPlayer(text);
                                }

                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Cần ít nhất " + n + " bông hoa để có thể tặng");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Cần ít nhất " + n + " bông hoa để có thể tặng");
                        }
                }
                break;
            case 3:
                if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                    Item keogiangsinh = InventoryService.gI().finditemKeoGiangSinh(player);

                    if (keogiangsinh != null && keogiangsinh.quantity >= 99) {
                        Item tatgiangsinh = ItemService.gI().createNewItem((short) 649, 1);
                        // - Số item sự kiện có trong rương
                        InventoryService.gI().subQuantityItemsBag(player, keogiangsinh, 99);

                        tatgiangsinh.itemOptions.add(new ItemOption(74, 0));
                        tatgiangsinh.itemOptions.add(new ItemOption(30, 0));
                        InventoryService.gI().addItemBag(player, tatgiangsinh, 0);
                        InventoryService.gI().sendItemBags(player);
                        Service.getInstance().sendThongBao(player, "Bạn nhận được Tất,vớ giáng sinh");
                    } else {
                        Service.getInstance().sendThongBao(player,
                                "Vui lòng chuẩn bị x99 kẹo giáng sinh để đổi vớ tất giáng sinh");
                    }
                } else {
                    Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                }
                break;
            case 4:
                switch (select) {
                    case 0:
                        if (!player.event.isReceivedLuckyMoney()) {
                            Calendar cal = Calendar.getInstance();
                            int day = cal.get(Calendar.DAY_OF_MONTH);
                            if (day >= 22 && day <= 24) {
                                Item goldBar = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG,
                                        Util.nextInt(1, 3));
                                player.inventory.ruby += Util.nextInt(10, 30);
                                goldBar.quantity = Util.nextInt(1, 3);
                                InventoryService.gI().addItemBag(player, goldBar, 99999);
                                InventoryService.gI().sendItemBags(player);
                                PlayerService.gI().sendInfoHpMpMoney(player);
                                player.event.setReceivedLuckyMoney(true);
                                Service.getInstance().sendThongBao(player,
                                        "Nhận lì xì thành công,chúc bạn năm mới dui dẻ");
                            } else if (day > 24) {
                                Service.getInstance().sendThongBao(player, "Hết tết rồi còn đòi lì xì");
                            } else {
                                Service.getInstance().sendThongBao(player, "Đã tết đâu mà đòi lì xì");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Bạn đã nhận lì xì rồi");
                        }
                        break;
                    case 1:
                        ShopService.gI().openShopNormal(player, npc, ConstNpc.SHOP_SU_KIEN_TET, 1, -1);
                        break;
                }
                break;
            case ConstEvent.SU_KIEN_8_3:
                switch (select) {
                    case 3:
                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                            int evPoint = player.event.getEventPoint();
                            if (evPoint >= 999) {
                                Item capsule = ItemService.gI().createNewItem((short) 2052, 1);
                                player.event.setEventPoint(evPoint - 999);

                                capsule.itemOptions.add(new ItemOption(74, 0));
                                capsule.itemOptions.add(new ItemOption(30, 0));
                                InventoryService.gI().addItemBag(player, capsule, 0);
                                InventoryService.gI().sendItemBags(player);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được Capsule Hồng");
                            } else {
                                Service.getInstance().sendThongBao(player, "Cần 999 điểm tích lũy để đổi");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Hành trang đầy.");
                        }
                        break;
                    default:
                        int n = 0;
                        switch (select) {
                            case 0:
                                n = 1;
                                break;
                            case 1:
                                n = 10;
                                break;
                            case 2:
                                n = 99;
                                break;
                        }

                        if (n > 0) {
                            Item bonghoa = InventoryService.gI().finditemBongHoa(player, n);
                            if (bonghoa != null) {
                                int evPoint = player.event.getEventPoint();
                                player.event.setEventPoint(evPoint + n);
                                InventoryService.gI().subQuantityItemsBag(player, bonghoa, n);
                                Service.getInstance().sendThongBao(player, "Bạn nhận được " + n + " điểm sự kiện");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Cần ít nhất " + n + " bông hoa để có thể tặng");
                            }
                        } else {
                            Service.getInstance().sendThongBao(player, "Cần ít nhất " + n + " bông hoa để có thể tặng");
                        }
                }
                break;
        }
    }

    public static String getMenuSuKien(int id) {
        switch (id) {
            case ConstEvent.KHONG_CO_SU_KIEN:
                return "Chưa có\n Sự Kiện";
            case ConstEvent.SU_KIEN_HALLOWEEN:
                return "Sự Kiện\nHaloween";
            case ConstEvent.SU_KIEN_20_11:
                return "Sự Kiện\n 20/11";
            case ConstEvent.SU_KIEN_NOEL:
                return "Sự Kiện\n Giáng Sinh";
            case ConstEvent.SU_KIEN_TET:
                return "Sự Kiện\n Tết Nguyên\nĐán 2023";
            case ConstEvent.SU_KIEN_8_3:
                return "Sự Kiện\n 8/3";
        }
        return "Chưa có\n Sự Kiện";
    }

    public static String getMenuLamBanh(Player player, int type) {
        switch (type) {
            case 0:// bánh tét
                if (player.event.isCookingTetCake()) {
                    int timeCookTetCake = player.event.getTimeCookTetCake();
                    if (timeCookTetCake == 0) {
                        return "Lấy bánh";
                    } else if (timeCookTetCake > 0) {
                        return "Đang nấu\nBánh Tét\nCòn " + TimeUtil.secToTime(timeCookTetCake);
                    }
                } else {
                    return "Nấu\nBánh Tét";
                }
                break;
            case 1:// bánh chưng
                if (player.event.isCookingChungCake()) {
                    int timeCookChungCake = player.event.getTimeCookChungCake();
                    if (timeCookChungCake == 0) {
                        return "Lấy bánh";
                    } else if (timeCookChungCake > 0) {
                        return "Đang nấu\nBánh Chưng\nCòn " + TimeUtil.secToTime(timeCookChungCake);
                    }
                } else {
                    return "Nấu\nBánh Chưng";
                }
                break;
        }
        return "";
    }


    public static void claimVipGift(Player pl, int vipLevel, boolean isCapsule) {
        if (pl.event.luotNhanVipGift >= 4) {
            Service.getInstance().sendThongBao(pl, "|7|Bạn đã đạt giới hạn tối đa 4 lần nhận quà VIP!");
            return;
        }

        if (isCapsule) {
            if (InventoryService.gI().getCountEmptyBag(pl) < 2) {
                Service.getInstance().sendThongBao(pl, "Hành trang cần ít nhất 2 ô trống để nhận Capsule VIP!");
                return;
            }
            giveVipCapsule(pl, vipLevel);
        } else {
            if (InventoryService.gI().getCountEmptyBag(pl) < 5) {
                Service.getInstance().sendThongBao(pl, "Hành trang cần ít nhất 5 ô trống để nhận Trọn Bộ Set Kích Hoạt VIP!");
                return;
            }
            nro.services.func.UseItem.gI().openCapsuleSKHVip(pl, null, vipLevel);
        }

        pl.event.vipMask |= (1 << vipLevel);
        pl.event.luotNhanVipGift++;
        saveVipCheckNhanQua(pl);
        Service.getInstance().sendThongBao(pl, "|1|Đã nhận quà VIP " + vipLevel + " thành công!\n(Tiến độ: " + pl.event.luotNhanVipGift + "/4 lần)");
    }

    public static void saveVipCheckNhanQua(Player pl) {
        try (java.sql.Connection con = nro.jdbc.DBService.gI().getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement("UPDATE player SET checkNhanQua = ? WHERE id = ?")) {
            org.json.simple.JSONArray dataArray = new org.json.simple.JSONArray();
            dataArray.add(pl.event.luotNhanNgocMienPhi);
            dataArray.add(pl.event.luotNhanBuaMienPhi);
            dataArray.add(pl.event.luotNhanVipGift);
            dataArray.add(pl.event.vipMask);
            ps.setString(1, dataArray.toJSONString());
            ps.setLong(2, pl.id);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    public static void giveVipCapsule(Player pl, int vipLevel) {
        short capsuleId;
        short caiTrangId;
        switch (vipLevel) {
            case 1:
                capsuleId = 1559; // Capsule Set Kích Hoạt VIP 1
                caiTrangId = 604;
                break;
            case 2:
                capsuleId = 1538; // Capsule Set Kích Hoạt VIP 2
                caiTrangId = 605;
                break;
            case 3:
                capsuleId = 1655; // Capsule Set Kích Hoạt VIP 3
                caiTrangId = 606;
                break;
            case 4:
                capsuleId = 2083; // Capsule Set Kích Hoạt VIP 4
                caiTrangId = 760;
                break;
            default:
                capsuleId = 1559;
                caiTrangId = 604;
                break;
        }

        Item capsule = ItemService.gI().createNewItem(capsuleId, 1);
        capsule.itemOptions.add(new ItemOption(30, 0)); // Không thể giao dịch
        InventoryService.gI().addItemBag(pl, capsule, 99);

        Item caiTrang = ItemService.gI().createNewItem(caiTrangId, 1);
        caiTrang.itemOptions.add(new ItemOption(30, 0));
        caiTrang.itemOptions.add(new ItemOption(93, 30)); // 30 ngày
        InventoryService.gI().addItemBag(pl, caiTrang, 99);

        InventoryService.gI().sendItemBags(pl);
    }

}