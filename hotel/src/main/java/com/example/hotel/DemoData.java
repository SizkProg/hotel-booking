package com.example.hotel;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
public class DemoData {
  @Bean
  CommandLineRunner seed(HotelRepository hotels, RoomRepository rooms) {
    return args -> {
      var h = new Hotel();
      h.name = "Сибирь";
      h.address = "Новосибирск, ул. Советская, 1";
      hotels.save(h);
      for (int i = 1; i <= 4; i++) {
        var r = new Room();
        r.hotelId = h.id;
        r.number = "10" + i;
        r.available = i != 4;
        r.timesBooked = i == 3 ? 5 : 0;
        rooms.save(r);
      }
    };
  }
}
