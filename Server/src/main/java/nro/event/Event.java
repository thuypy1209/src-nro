package nro.event;

import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.utils.Log;

import java.util.List;

/**
 * Lớp trừu tượng định nghĩa khung cho các sự kiện (Event) trong Game.
 * Mọi sự kiện mới chỉ cần kế thừa lớp Event này và đặt trong package nro.event.list.
 * 
 * @author outcast c-cute hột me / Refactored by Antigravity
 */
public abstract class Event {

    private static Event instance;

    public static Event getInstance() {
        return instance;
    }

    public static void setInstance(Event event) {
        instance = event;
    }

    public static boolean isEvent() {
        return instance != null;
    }

    public static void initEvent(String event) {
        if (event != null && !event.isEmpty() && !event.equalsIgnoreCase("null")) {
            try {
                if (!event.contains(".")) {
                    try {
                        instance = (Event) Class.forName("nro.event.list." + event).getDeclaredConstructor().newInstance();
                    } catch (ClassNotFoundException e) {
                        try {
                            instance = (Event) Class.forName("nro.event.event_list." + event).getDeclaredConstructor().newInstance();
                        } catch (ClassNotFoundException e2) {
                            instance = (Event) Class.forName("nro.event." + event).getDeclaredConstructor().newInstance();
                        }
                    }
                } else {
                    instance = (Event) Class.forName(event).getDeclaredConstructor().newInstance();
                }
            } catch (Exception e) {
                Log.error(Event.class, e, "Lỗi khởi tạo event: " + event);
            }
        }
    }

    /**
     * ID định danh sự kiện (tương ứng với ConstEvent)
     */
    public abstract int getId();

    /**
     * Tên hiển thị của sự kiện
     */
    public abstract String getName();

    /**
     * Kích hoạt sự kiện (sinh Boss, sinh NPC, đặt cờ hiệu, hiệu ứng...)
     */
    public abstract void init();

    /**
     * Hủy sự kiện (dọn dẹp Boss, dọn dẹp NPC, hạ cờ...)
     */
    public abstract void destroy();

    public void initNpc() {}

    public void initMap() {}

    public void dropItem(Player player, Mob mob, List<ItemMap> list, int x, int yEnd) {}

    public boolean useItem(Player player, Item item) {
        return false;
    }
}
