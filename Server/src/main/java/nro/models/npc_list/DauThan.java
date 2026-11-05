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
 * NPC DauThan
 */
public class DauThan extends Npc {

    public DauThan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            player.magicTree.openMenuTree();
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            TaskService.gI().checkDoneTaskConfirmMenuNpc(player, this, (byte) select);
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.MAGIC_TREE_NON_UPGRADE_LEFT_PEA:
                    if (select == 0) {
                        player.magicTree.harvestPea();
                    } else if (select == 1) {
                        if (player.magicTree.level == 10) {
                            player.magicTree.fastRespawnPea();
                        } else {
                            player.magicTree.showConfirmUpgradeMagicTree();
                        }
                    } else if (select == 2) {
                        player.magicTree.fastRespawnPea();
                    }
                    break;
                case ConstNpc.MAGIC_TREE_NON_UPGRADE_FULL_PEA:
                    if (select == 0) {
                        player.magicTree.harvestPea();
                    } else if (select == 1) {
                        player.magicTree.showConfirmUpgradeMagicTree();
                    }
                    break;
                case ConstNpc.MAGIC_TREE_CONFIRM_UPGRADE:
                    if (select == 0) {
                        player.magicTree.upgradeMagicTree();
                    }
                    break;
                case ConstNpc.MAGIC_TREE_UPGRADE:
                    if (select == 0) {
                        player.magicTree.fastUpgradeMagicTree();
                    } else if (select == 1) {
                        player.magicTree.showConfirmUnuppgradeMagicTree();
                    }
                    break;
                case ConstNpc.MAGIC_TREE_CONFIRM_UNUPGRADE:
                    if (select == 0) {
                        player.magicTree.unupgradeMagicTree();
                    }
                    break;
            }
        }
    }
}
