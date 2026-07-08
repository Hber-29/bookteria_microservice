package com.devteria.post.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

// class định dạng kiểu đầu ra thời gian
@Component
public class DateTimeFormatter {
    Map<Long, Function<Instant,String>> strategyMap = new LinkedHashMap<>();

    public DateTimeFormatter() {
        // đây là lưu dạng key-value
        strategyMap.put(60L,this::formatInseconds);
        strategyMap.put(3600L,this::formmatInMinutes);
        strategyMap.put(84600L,this::formmatInHours);
        strategyMap.put(Long.MAX_VALUE,this::formmatInDate);
    }


    // hàm chính để xử lý logic
    public String format(Instant instant) {

        long elapseSeconds = ChronoUnit.SECONDS.between(instant, Instant.now());

        var strategy= strategyMap
                .entrySet()
                .stream()
                .filter(longFunctionEntry-> elapseSeconds<longFunctionEntry.getKey())
                .findFirst().get();

        // trả về lấy ra value ở map nó là một cái hàm và truyền đầu vào instant vào
        return strategy.getValue().apply(instant);



    }


    // đây là các hàm con
    private String formatInseconds(Instant instant) {
        long elapseSeconds = ChronoUnit.SECONDS.between(instant, Instant.now());
        return String.format("%s second(s) ago", elapseSeconds);

    }

    private String formmatInMinutes(Instant instant) {
        var elapseMultiplier = ChronoUnit.MINUTES.between(instant, Instant.now());
        return String.format("%s minute(s) ago", elapseMultiplier);
    }

    private String formmatInHours(Instant instant) {
        var elapseMultiplier = ChronoUnit.HOURS.between(instant, Instant.now());
        return String.format("%s hour(s) ago", elapseMultiplier);
    }
    private String formmatInDate(Instant instant) {
        LocalDateTime localDateTime =instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_DATE;
        return formatter.format(localDateTime);
    }


}
