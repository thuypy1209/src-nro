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
 * NPC RongOmega
 */
public class RongOmega extends Npc {

    public RongOmega(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            BlackBallWar.gI().setTime();
            if (this.mapId == 24 || this.mapId == 25 || this.mapId == 26) {
                try {
                    long now = System.currentTimeMillis();
                    if (now > BlackBallWar.TIME_OPEN && now < BlackBallWar.TIME_CLOSE) {
                        this.createOtherMenu(player, ConstNpc.MENU_OPEN_BDW,
                                "Đường đến với ngọc rồng sao đen đã mở, "
                                        + "ngươi có muốn tham gia không?",
                                "Hướng dẫn\nthêm", "Tham gia", "Từ chối");
                    } else {
                        String[] optionRewards = new String[7];
                        int index = 0;
                        for (int i = 0; i < 7; i++) {
                            if (player.rewardBlackBall.timeOutOfDateReward[i] > System
                                    .currentTimeMillis()) {
                                optionRewards[index] = "Nhận thưởng\n" + (i + 1) + " sao";
                                index++;
                            }
                        }
                        if (index != 0) {
                            String[] options = new String[index + 1];
                            for (int i = 0; i < index; i++) {
                                options[i] = optionRewards[i];
                            }
                            options[options.length - 1] = "Từ chối";
                            this.createOtherMenu(player, ConstNpc.MENU_REWARD_BDW,
                                    "Ngươi có một vài phần thưởng ngọc " + "rồng sao đen đây!",
                                    options);
                        } else {
                            this.createOtherMenu(player, ConstNpc.MENU_NOT_OPEN_BDW,
                                    "Ta có thể giúp gì cho ngươi?", "Hướng dẫn", "Từ chối");
                        }
                    }
                } catch (Exception ex) {
                    Log.error("Lỗi mở menu rồng Omega");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.MENU_REWARD_BDW:
                    player.rewardBlackBall.getRewardSelect((byte) select);
                    break;
                case ConstNpc.MENU_OPEN_BDW:
                    if (select == 0) {
                        NpcService.gI().createTutorial(player, this.avartar,
                                ConstNpc.HUONG_DAN_BLACK_BALL_WAR);
                    } else if (select == 1) {
                        player.iDMark.setTypeChangeMap(ConstMap.CHANGE_BLACK_BALL);
                        ChangeMapService.gI().openChangeMapTab(player);
                    }
                    break;
                case ConstNpc.MENU_NOT_OPEN_BDW:
                    if (select == 0) {
                        NpcService.gI().createTutorial(player, this.avartar,
                                ConstNpc.HUONG_DAN_BLACK_BALL_WAR);
                    }
                    break;
            }
        }
    }
}
