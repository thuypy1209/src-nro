package test;

import nro.jdbc.DBHika;
import nro.jdbc.daos.GodGK;
import nro.models.player.Player;
import nro.server.Manager;
import nro.server.io.Session;
import nro.services.MapService;

public class TestLoadPlayer {
    public static void main(String[] args) {
        try {
            System.out.println("1. Initializing Manager...");
            Manager.gI();
            System.out.println("2. Loading player for user 3735 (t3h4u)...");
            Session session = new Session();
            session.userId = 3735;
            Player player = GodGK.loadPlayer(session);
            if (player != null) {
                System.out.println("SUCCESS! Loaded player: " + player.name + " (id: " + player.id + ")");
                System.out.println("Player HP: " + player.nPoint.hp + " / " + player.nPoint.hpMax);
                System.out.println("Player MP: " + player.nPoint.mp + " / " + player.nPoint.mpMax);
                System.out.println("Player Dame: " + player.nPoint.dame);
                System.out.println("Player Zone: " + (player.zone != null ? player.zone.map.mapId + " zone " + player.zone.zoneId : "null"));
                System.out.println("Player Task: " + player.playerTask.taskMain.id + " (" + player.playerTask.taskMain.name + ") index: " + player.playerTask.taskMain.index);
            } else {
                System.out.println("Player is null!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
