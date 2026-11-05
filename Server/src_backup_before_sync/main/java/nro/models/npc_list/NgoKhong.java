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
 * NPC NgoKhong
 */
public class NgoKhong extends Npc {

    public NgoKhong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Chu mi nga", "Tặng quả\nHồng đào\nChín");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            int itemNeed = ConstItem.QUA_HONG_DAO_CHIN;
            Item item = InventoryService.gI().findItemBagByTemp(player, itemNeed);
            if (item != null) {
                RandomCollection<Integer> rc = Manager.HONG_DAO_CHIN;
                int itemID = rc.next();
                int x = cx + Util.nextInt(-50, 50);
                int y = player.zone.map.yPhysicInTop(x, cy - 24);
                int quantity = 1;
                if (itemID == ConstItem.HONG_NGOC) {
                    quantity = Util.nextInt(1, 2);
                }
                InventoryService.gI().subQuantityItemsBag(player, item, 1);
                InventoryService.gI().sendItemBags(player);
                ItemMap itemMap = new ItemMap(player.zone, itemID, quantity, x, y, player.id);
                Service.getInstance().dropItemMap(player.zone, itemMap);
                npcChat(player.zone, "Xie xie");
            } else {
                Service.getInstance().sendThongBao(player, "Không tìm thấy!");
            }
        }
    }
}
