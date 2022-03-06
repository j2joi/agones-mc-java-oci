package dev.agones.cmd;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Ultis {
    public static void main(String[] args) {
        Clock clock = Clock.systemDefaultZone();
        System.out.println("Test Format : "+clock.instant());
        System.out.println("Failed Health check at : "+clock.instant());
        //now = LocalDateTime.now();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("After 3 Secs init now again. : "+clock.instant());
    }
}
