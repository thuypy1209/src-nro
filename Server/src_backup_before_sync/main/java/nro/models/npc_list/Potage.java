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
 * NPC Potage
 */
public class Potage extends Npc {

    public Potage(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }


    @Override
    public void openBaseMenu(Player player) {
        player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                "Hãy giúp ta đánh bại bản sao\nNgươi chỉ có 5 phút để hạ hắn\nPhần thưởng cho ngươi là 1 bình Commeson",
                "Hướng\ndẫn\nthêm", "OK", "Từ chối");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0:
                        NpcService.gI().createTutorial(player, avartar,
                                "Thứ bị phong ấn tại đây là vũ khí có tên Commeson\b"
                                        + "được tạo ra nhằm bảo vệ cho hành tinh Potaufeu\b"
                                        + "Tuy nhiên nó đã tàn phá mọi thứ trong quá khứ\n"
                                        + "Khiến cư dân Potaufeu niêm phong nó với cái giá\b phải trả là mạng sống của họ\b Ta, Potage là người duy nhất sống sót\b"
                                        + "và ta đã bảo vệ phong ấn hơn một trăm năm.\n"
                                        + "Tuy nhiên bọn xâm lược Gryll đã đến và giải thoát Commeson\b"
                                        + "Hãy giúp ta tiêu diệt bản sao do Commeson tạo ra\b"
                                        + "và niêm phong Commeson một lần và mãi mãi");
                        break;
                    case 1:// gọi nhân bản
                        if (player.zone.getBosses().size() != 0) {
                            this.createOtherMenu(player, 251003,
                                    "Đang có 1 nhân bản của " + player.zone.getBosses().get(0).name
                                            + " hãy chờ kết quả trận đấu",
                                    "OK");
                            return;
                        }
                        if (!player.itemTime.doneDanhNhanBan) {
                            player.itemTime.isDanhNhanBan = true;
                            player.itemTime.lasttimeDanhNhanBan = System.currentTimeMillis();

                            ItemTimeService.gI().sendAllItemTime(player);
                            List<Skill> skillList = new ArrayList<>();
                            for (byte i = 0; i < player.playerSkill.skills.size(); i++) {
                                Skill skill = player.playerSkill.skills.get(i);
                                if (skill.point > 0) {
                                    skillList.add(skill);
                                }
                            }
                            int[][] skillTemp = new int[skillList.size()][5];
                            for (byte i = 0; i < skillList.size(); i++) {
                                Skill skill = skillList.get(i);
                                if (skill.point > 0) {
                                    skillTemp[i][0] = skill.template.id;
                                    skillTemp[i][1] = skill.point;
                                    skillTemp[i][2] = skill.coolDown;
                                }
                            }

                            BossData bossdataa = BossData.builder()
                                    .name(player.name)
                                    .gender(player.gender)
                                    .typeDame(Boss.DAME_NORMAL)
                                    .typeHp(Boss.HP_NORMAL)
                                    .dame(player.nPoint.hpMax / 10)
                                    .hp(new int[][] { { player.nPoint.dame * 10 } })
                                    .outfit(new short[] { player.getHead(), player.getBody(),
                                            player.getLeg(), player.getFlagBag(), player.getAura(),
                                            player.getEffFront() })
                                    .skillTemp(skillTemp)
                                    .secondsRest(BossData._0_GIAY)
                                    .build();

                            try {
                                Boss_NhanBan dt = new Boss_NhanBan(
                                        Util.createIdDuongTank((int) ((byte) player.id)), bossdataa,
                                        player.zone, player.location.x, player.location.y,
                                        (int) player.id);
                            } catch (Exception ex) {
                                Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE, null,
                                        ex);
                            }
                            break;
                        } else {
                            Service.getInstance().sendThongBao(player, "Hãy chờ đến ngày mai");
                        }
                }
            }
        }
    }
}
