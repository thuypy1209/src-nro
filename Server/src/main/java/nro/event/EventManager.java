package nro.event;

import nro.consts.ConstEvent;
import nro.event.list.*;
import nro.models.boss.Boss;
import nro.models.boss.BossFactory;
import nro.models.boss.BossManager;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import nro.models.player.Player;
import nro.server.Manager;
import nro.services.MapService;
import nro.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý tập trung toàn bộ các sự kiện trong Server (NRO NEW 01)
 * Kiến trúc tách rời theo event list: Mỗi sự kiện là một class độc lập kế thừa Event.
 * Mặc định khi server.event = 0: Không có bất kỳ sự kiện nào được kích hoạt.
 *
 * @author KhanhDTK / Mr Blue / Refactored by Antigravity
 */
public class EventManager {

    private static EventManager instance;

    // Các cờ cấu hình bật / tắt từng sự kiện (mặc định false khi server.event = 0)
    public static boolean LUNAR_NEW_YEAR = false;
    public static boolean INTERNATIONAL_WOMENS_DAY = false;
    public static boolean CHRISTMAS = false;
    public static boolean HALLOWEEN = false;
    public static boolean HUNG_VUONG = false;
    public static boolean TRUNG_THU = false;
    public static boolean SUMMER = false;
    public static boolean TOP_UP = false;

    // Danh sách các event đang kích hoạt
    public final List<Event> activeEvents = new ArrayList<>();

    public static EventManager gI() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    /**
     * Khởi tạo sự kiện từ cấu hình server (Manager.EVENT_SEVER / server.properties)
     */
    public void init() {
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Đợi server nạp map & database xong
                System.out.println("-> [EventManager] Kiểm tra cấu hình sự kiện máy chủ (EVENT_SEVER = " + Manager.EVENT_SEVER + ")...");

                if (Manager.EVENT_SEVER == ConstEvent.KHONG_CO_SU_KIEN) {
                    System.out.println("-> [EventManager] Máy chủ đang ở chế độ Mặc định: KHÔNG CÓ SỰ KIỆN NÀO ĐƯỢC BẬT (server.event = 0).");
                    return;
                }

                applyEvent(Manager.EVENT_SEVER);
            } catch (Exception e) {
                Log.error(EventManager.class, e, "Lỗi khi kích hoạt danh sách sự kiện");
            }
        }).start();
    }

    public void addEvent(Event event) {
        if (event != null) {
            Event.setInstance(event);
            event.init();
            activeEvents.add(event);
        }
    }

    public void removeEvent(Event event) {
        if (event != null) {
            event.destroy();
            activeEvents.remove(event);
            if (Event.getInstance() == event) {
                Event.setInstance(null);
            }
        }
    }

    public String getEventName(int eventId) {
        switch (eventId) {
            case ConstEvent.SU_KIEN_HALLOWEEN:
                return "Sự kiện Halloween";
            case ConstEvent.SU_KIEN_20_11:
                return "Sự kiện 20/11";
            case ConstEvent.SU_KIEN_NOEL:
                return "Sự kiện Noel";
            case ConstEvent.SU_KIEN_TET:
                return "Sự kiện Tết Nguyên Đán";
            case ConstEvent.SU_KIEN_8_3:
                return "Sự kiện 8/3";
            case ConstEvent.SU_KIEN_HUNG_VUONG:
                return "Sự kiện Giỗ Tổ Hùng Vương";
            case ConstEvent.SU_KIEN_TRUNG_THU:
                return "Sự kiện Trung Thu";
            case ConstEvent.SU_KIEN_HE:
                return "Sự kiện Hè";
            default:
                return "Không có sự kiện";
        }
    }

    public void applyEvent(int eventId) {
        Manager.EVENT_SEVER = (byte) eventId;
        // Dọn dẹp toàn bộ các sự kiện đang chạy
        for (Event ev : new ArrayList<>(activeEvents)) {
            removeEvent(ev);
        }

        // Reset toàn bộ cờ sự kiện về false
        LUNAR_NEW_YEAR = false;
        INTERNATIONAL_WOMENS_DAY = false;
        CHRISTMAS = false;
        HALLOWEEN = false;
        HUNG_VUONG = false;
        TRUNG_THU = false;
        SUMMER = false;

        // Kích hoạt duy nhất sự kiện được chỉ định
        switch (eventId) {
            case ConstEvent.SU_KIEN_HALLOWEEN:
                HALLOWEEN = true;
                addEvent(new HalloweenEvent());
                break;
            case ConstEvent.SU_KIEN_20_11:
                addEvent(new Event2011());
                break;
            case ConstEvent.SU_KIEN_NOEL:
                CHRISTMAS = true;
                addEvent(new NoelEvent());
                break;
            case ConstEvent.SU_KIEN_TET:
                LUNAR_NEW_YEAR = true;
                addEvent(new TetEvent());
                break;
            case ConstEvent.SU_KIEN_8_3:
                INTERNATIONAL_WOMENS_DAY = true;
                addEvent(new Event83());
                break;
            case ConstEvent.SU_KIEN_HUNG_VUONG:
                HUNG_VUONG = true;
                addEvent(new HungVuongEvent());
                break;
            case ConstEvent.SU_KIEN_TRUNG_THU:
                TRUNG_THU = true;
                addEvent(new TrungThuEvent());
                break;
            case ConstEvent.SU_KIEN_HE:
                SUMMER = true;
                addEvent(new SummerEvent());
                break;
            default:
                break;
        }
        System.out.println("-> [EventManager] Trạng thái sự kiện: " + getEventName(eventId) + " (ID: " + eventId + ")");
    }

    public void createNpc(int mapId, int npcId, int x, int y) {
        try {
            Map map = MapService.gI().getMapById(mapId);
            if (map != null && map.npcs != null) {
                map.npcs.removeIf(n -> n != null && n.tempId == npcId);
                Npc npc = NpcFactory.createNPC(mapId, 1, x, y, npcId, 0);
                if (npc != null) {
                    map.npcs.add(npc);
                    if (map.zones != null) {
                        for (Zone z : map.zones) {
                            if (z != null) {
                                for (Player pl : z.getPlayers()) {
                                    if (pl != null && pl.getSession() != null) {
                                        z.mapInfo(pl);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error(EventManager.class, e, "Lỗi createNpc: " + npcId + " map: " + mapId);
        }
    }

    public void createNpc(int mapId, byte npcId, int x, int y) {
        createNpc(mapId, (int) npcId, x, y);
    }

    public void removeNpc(int mapId, int npcId) {
        try {
            Map map = MapService.gI().getMapById(mapId);
            if (map != null && map.npcs != null) {
                map.npcs.removeIf(n -> n != null && n.tempId == npcId);
                if (map.zones != null) {
                    for (Zone z : map.zones) {
                        if (z != null) {
                            for (Player pl : z.getPlayers()) {
                                if (pl != null && pl.getSession() != null) {
                                    z.mapInfo(pl);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error(EventManager.class, e, "Lỗi removeNpc: " + npcId + " map: " + mapId);
        }
    }

    public void removeNpc(int mapId, byte npcId) {
        removeNpc(mapId, (int) npcId);
    }

    public void createBoss(int bossId) {
        try {
            Boss boss = BossFactory.createBoss(bossId);
            if (boss != null) {
                BossManager.gI().addBoss(boss);
            }
        } catch (Exception e) {
            Log.error(EventManager.class, e, "Lỗi createBoss: " + bossId);
        }
    }

    public void removeBoss(int bossId) {
        try {
            for (int i = BossManager.BOSSES_IN_GAME.size() - 1; i >= 0; i--) {
                Boss b = BossManager.BOSSES_IN_GAME.get(i);
                if (b != null && b.id == bossId) {
                    BossManager.gI().removeBoss(b);
                }
            }
        } catch (Exception e) {
            Log.error(EventManager.class, e, "Lỗi removeBoss: " + bossId);
        }
    }
}
