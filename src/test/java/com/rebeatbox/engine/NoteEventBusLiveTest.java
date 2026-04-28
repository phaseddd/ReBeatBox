package com.rebeatbox.engine;

import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class NoteEventBusLiveTest {

    private static void drainEdt() {
        try {
            SwingUtilities.invokeAndWait(() -> {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldCallOnLiveNoteOnForSubscribedListener() {
        NoteEventBus bus = new NoteEventBus();
        AtomicInteger called = new AtomicInteger(0);
        int[] lastNote = {-1};
        int[] lastVel = {-1};

        bus.subscribeLive(new LiveNoteEventListener() {
            @Override
            public void onLiveNoteOn(int note, int velocity) {
                called.incrementAndGet();
                lastNote[0] = note;
                lastVel[0] = velocity;
            }
            @Override
            public void onLiveNoteOff(int note) {}
        });

        bus.fireLiveNoteOn(60, 100);
        drainEdt();
        assertEquals(1, called.get());
        assertEquals(60, lastNote[0]);
        assertEquals(100, lastVel[0]);
    }

    @Test
    void shouldCallOnLiveNoteOffForSubscribedListener() {
        NoteEventBus bus = new NoteEventBus();
        AtomicInteger called = new AtomicInteger(0);
        int[] lastNote = {-1};

        bus.subscribeLive(new LiveNoteEventListener() {
            @Override
            public void onLiveNoteOn(int note, int velocity) {}
            @Override
            public void onLiveNoteOff(int note) {
                called.incrementAndGet();
                lastNote[0] = note;
            }
        });

        bus.fireLiveNoteOff(60);
        drainEdt();
        assertEquals(1, called.get());
        assertEquals(60, lastNote[0]);
    }

    @Test
    void shouldNotCallUnsubscribedListener() {
        NoteEventBus bus = new NoteEventBus();
        AtomicInteger called = new AtomicInteger(0);

        LiveNoteEventListener listener = new LiveNoteEventListener() {
            @Override
            public void onLiveNoteOn(int note, int velocity) {
                called.incrementAndGet();
            }
            @Override
            public void onLiveNoteOff(int note) {}
        };

        bus.subscribeLive(listener);
        bus.fireLiveNoteOn(60, 100);
        drainEdt();
        assertEquals(1, called.get());

        bus.unsubscribeLive(listener);
        bus.fireLiveNoteOn(72, 100);
        drainEdt();
        assertEquals(1, called.get());
    }

    @Test
    void shouldCallMultipleListeners() {
        NoteEventBus bus = new NoteEventBus();
        AtomicInteger c1 = new AtomicInteger(0);
        AtomicInteger c2 = new AtomicInteger(0);

        bus.subscribeLive(new LiveNoteEventListener() {
            @Override
            public void onLiveNoteOn(int note, int velocity) { c1.incrementAndGet(); }
            @Override
            public void onLiveNoteOff(int note) {}
        });
        bus.subscribeLive(new LiveNoteEventListener() {
            @Override
            public void onLiveNoteOn(int note, int velocity) { c2.incrementAndGet(); }
            @Override
            public void onLiveNoteOff(int note) {}
        });

        bus.fireLiveNoteOn(60, 100);
        drainEdt();
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());
    }

    @Test
    void shouldCallLiveNoteOnViaEdt() {
        NoteEventBus bus = new NoteEventBus();
        boolean[] edtCheck = {false};

        bus.subscribeLive(new LiveNoteEventListener() {
            @Override
            public void onLiveNoteOn(int note, int velocity) {
                edtCheck[0] = javax.swing.SwingUtilities.isEventDispatchThread();
            }
            @Override
            public void onLiveNoteOff(int note) {}
        });

        bus.fireLiveNoteOn(60, 100);
        drainEdt();
        assertTrue(edtCheck[0], "onLiveNoteOn should be called on EDT");
    }
}
