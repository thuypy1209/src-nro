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
 * NPC MrPoPo
 */
public class MrPoPo extends Npc {

    public MrPoPo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 0) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Thượng đế vừa phát hiện 1 loại khí đang âm thầm\n"
                                + "hủy diệt mọi mầm sống trên Trái Đất,\n"
                                + "nó được gọi là Destron Gas.\n"
                                + "Ta sẽ đưa các cậu đến nơi ấy, các cậu sẵn sàng chưa?",
                        "Thông tin\nChi tiết", "Top 100\nBang hội",
                        "Thành tích\nBang", "OK", "Từ chối");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 0) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:// Thông tin chi tiết
                            NpcService.gI().createTutorial(player, avartar,
                                    "Chúng ta gặp rắc rối rồi\b"
                                            + "Thượng Đế nói với tôi rằng có 1 loại khí\bgọi là Destron Gas, thứ này không thuộc về nơi đây\n"
                                            + "Nó tích tụ trên Trái Đất\bvà nó sẽ hủy diệt mọi mô tế bào sống\b"
                                            + "Có tất cả 4 địa điểm mà Thượng Đế bảo tôi nói với cậu\bCậu có thể đến kiểm tra...\n"
                                            + "Đầu tiên là Thành phố Santa tọa lạc ở phía Tây nam của thủ đô ở Viễn Đông.\n"
                                            + "Thứ hai là gần Kim Tự Tháp ở vùng Sa Mạc viễn tây của thủ đô phía Bắc\n"
                                            + "Thứ ba Vùng Đất Băng Giá ở Phương Bắc xa xôi\n"
                                            + "Thứ tư là Hành tinh Bóng Tối đang che phủ một phần địa cầu\bCậu đã hiểu rõ chưa?");
                            break;
                        case 1:// Top 100 bang hội

                            break;
                        case 2:// Thành tích Bang

                            break;
                        case 3: // OK
                            if (player.clan != null) {
                                if (player.clan.isLeader(player)) {
                                    if (player.clan.khiGas != null) {
                                        this.createOtherMenu(player, ConstNpc.MENU_OPENED_KGHD,
                                                "Bang hội của cậu đang tham gia Destron Gas cấp độ 110\n"
                                                        + "cậu có muốn đi cùng họ không? ("
                                                        + Util.convertSecondsToTime2((System
                                                                .currentTimeMillis()
                                                                - player.clan.khiGas.lastTimeOpen)
                                                                / 1000)
                                                        + ")",
                                                "Đồng ý", "Từ chối");
                                    } else {
                                        Input.gI().createFormChooseLevelKhiGas(player);
                                    }
                                } else {
                                    Service.getInstance().sendThongBao(player,
                                            "Chức năng chỉ dành cho bang chủ");
                                }
                            }
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_ACCEPT_GO_TO_KGHD) {
                    switch (select) {
                        case 0:
                            KhiGasHuyDietService.gI().openKhiGas(player,
                                    Byte.parseByte(String.valueOf(PLAYERID_OBJECT.get(player.id))));
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPENED_KGHD) {
                    switch (select) {
                        case 0:
                            ChangeMapService.gI().goToKhiGas(player);
                            break;
                    }
                }
            }
        }
    }
}
