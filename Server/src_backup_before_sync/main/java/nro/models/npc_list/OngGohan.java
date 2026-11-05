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
 * NPC OngGohan
 */
public class OngGohan extends Npc {

    public OngGohan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }


    /// Hàm hỗ trợ nhiệm vụ
    private void hotroNv(Player player) {
        if (TaskService.gI().getIdTask(player) == ConstTask.TASK_9_0
                || TaskService.gI().getIdTask(player) == ConstTask.TASK_9_1
                || TaskService.gI().getIdTask(player) == ConstTask.TASK_9_2) {
            player.playerTask.taskMain.id = 10;
            player.playerTask.taskMain.index = 3;
            TaskService.gI().sendTaskMain(player);
            Service.getInstance().sendLogout(player);
        } else {
            Service.getInstance().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ Tàu 77.");
        }

        if (TaskService.gI().getIdTask(player) == ConstTask.TASK_16_0) {
            player.playerTask.taskMain.id = 17;
            player.playerTask.taskMain.index = 0;
            TaskService.gI().sendTaskMain(player);
            Service.getInstance().sendLogout(player);
        } else {
            Service.getInstance().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ Thách đấu 10 người.");
        }

        if (TaskService.gI().getIdTask(player) == ConstTask.TASK_18_0
                || TaskService.gI().getIdTask(player) == ConstTask.TASK_18_1) {
            player.playerTask.taskMain.id = 18;
            player.playerTask.taskMain.index = 2;
            TaskService.gI().sendTaskMain(player);
            Service.getInstance().sendLogout(player);
        } else {
            Service.getInstance().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ DHVT.");
        }

        if (TaskService.gI().getIdTask(player) == ConstTask.TASK_19_0
                || TaskService.gI().getIdTask(player) == ConstTask.TASK_19_1) {
            player.playerTask.taskMain.id = 19;
            player.playerTask.taskMain.index = 2;
            TaskService.gI().sendTaskMain(player);
            Service.getInstance().sendLogout(player);
        } else {
            Service.getInstance().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ Trung Úy Trắng.");
        }
    }

    @Override
    public void openBaseMenu(Player player) {
        // if (player.getSession().vndBar > 500000) {
        // // Cấm người chơi và kick ra khỏi hệ thống
        // PlayerService.gI().banPlayer(player);
        // Client.gI().kickSession(player.getSession());
        // return; // Dừng thực thi hàm nếu bị cấm
        // }
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "|7|Thành công là một hành trình, không phải là đích đến.\n"
                                + "|7|-----YTB KhanhDTK-----",
                        "Mã Quà Tặng", "Nhận Ngọc Xanh\n(Miễn phí)",
                        "Hỗ trợ\nnhiệm vụ", "Từ chối");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0 ->
                        Input.gI().createFormGiftCode(player);
                    case 1 -> {
                        if (player.inventory.gem >= 2000000) {
                            Service.getInstance().sendThongBao(player,
                                    "Tiêu bớt ngọc xanh đi bạn ơi");
                            return;
                        } else {
                            player.inventory.gem += 500000;
                            Service.getInstance().sendMoney(player);
                        }
                    }
                    case 2 -> {
                        hotroNv(player);
                    }
                }
            }
        }
    }
}
