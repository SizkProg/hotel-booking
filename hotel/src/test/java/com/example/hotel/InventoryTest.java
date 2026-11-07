package com.example.hotel;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"eureka.client.enabled=false"})
class InventoryTest {
  @Autowired InventoryService inventory;
  @Autowired RoomRepository rooms;
  @Autowired SlotRepository slots;
  private Long room;
  private final LocalDate start = LocalDate.now().plusDays(3), end = start.plusDays(2);

  @BeforeEach
  void setup() {
    slots.deleteAll();
    var r = new Room();
    r.hotelId = 1L;
    r.number = UUID.randomUUID().toString();
    room = rooms.save(r).id;
  }

  private InventoryService.Reservation request(UUID id) {
    return new InventoryService.Reservation(id, start, end);
  }

  @Test
  void replayDoesNotIncrementCounter() {
    var id = UUID.randomUUID();
    inventory.confirm(room, request(id));
    inventory.confirm(room, request(id));
    assertEquals(1, rooms.findById(room).orElseThrow().timesBooked);
    assertEquals(1, slots.count());
  }

  @Test
  void releaseBeforeDelayedConfirmLeavesTombstone() {
    var id = UUID.randomUUID();
    inventory.release(room, request(id));
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> inventory.confirm(room, request(id)));
    assertEquals(0, rooms.findById(room).orElseThrow().timesBooked);
  }

  @Test
  void adjacentIntervalsAreAllowed() {
    inventory.confirm(room, request(UUID.randomUUID()));
    inventory.confirm(
        room, new InventoryService.Reservation(UUID.randomUUID(), end, end.plusDays(2)));
    assertEquals(2, slots.count());
  }

  @Test
  void onlyOneConcurrentBookingWins() throws Exception {
    var pool = Executors.newFixedThreadPool(8);
    try {
      var startGate = new CountDownLatch(1);
      var tasks = new ArrayList<Future<Boolean>>();
      for (int i = 0; i < 8; i++)
        tasks.add(
            pool.submit(
                () -> {
                  startGate.await();
                  try {
                    inventory.confirm(room, request(UUID.randomUUID()));
                    return true;
                  } catch (org.springframework.web.server.ResponseStatusException e) {
                    assertEquals(409, e.getStatusCode().value());
                    return false;
                  }
                }));
      startGate.countDown();
      int successes = 0;
      for (var task : tasks) if (task.get(10, TimeUnit.SECONDS)) successes++;
      assertEquals(1, successes);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void recommendationsHaveStableOrder() {
    var list = inventory.available(start, end, null, true);
    for (int i = 1; i < list.size(); i++) {
      var a = list.get(i - 1);
      var b = list.get(i);
      assertTrue(a.timesBooked < b.timesBooked || a.timesBooked == b.timesBooked && a.id < b.id);
    }
  }
}
