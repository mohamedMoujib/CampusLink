package org.example.campusLink.utils;

import org.example.campusLink.services.Gestion_Matching;

import java.util.Timer;
import java.util.TimerTask;

/**
 * ⏰ SCHEDULER POUR LE MATCHING AUTOMATIQUE
 *
 * Lance l'analyse des publications toutes les 5 minutes.
 * À démarrer au lancement de l'application.
 */
public class MatchingScheduler {

    private Timer timer;
    private Gestion_Matching matchingService;

    /** Démarrer le scheduler. */
    public void start() {
        try {
            matchingService = new Gestion_Matching();
            timer = new Timer("MatchingScheduler", true);

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        System.out.println("⏰ [" + java.time.LocalDateTime.now()
                                + "] Analyse des nouvelles publications...");
                        matchingService.analyserNouvellesPublications();
                    } catch (Exception e) {
                        System.err.println("❌ Erreur dans le scheduler : " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            };

            // Démarrer immédiatement, puis toutes les 5 minutes
            timer.scheduleAtFixedRate(task, 0L, 5 * 60 * 1000L);
            System.out.println("✅ Matching scheduler démarré (intervalle : 5 minutes)");

        } catch (Exception e) {
            System.err.println("❌ Erreur démarrage scheduler : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Arrêter le scheduler. */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            System.out.println("🛑 Matching scheduler arrêté");
        }
    }

    /** Exécution manuelle (pour tests). */
    public void runManually() {
        try {
            System.out.println("🔧 Exécution manuelle de l'analyse des publications...");
            matchingService.analyserNouvellesPublications();
        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MatchingScheduler scheduler = new MatchingScheduler();
        scheduler.start();
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}