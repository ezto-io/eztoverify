package org.keycloak.events;

import java.util.ArrayList;
import java.util.List;

public class EventCollection {
    private int total;
    private List<Event> events;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public static EventCollection empty(){
        return new EventCollection(){{
            setEvents(new ArrayList<>());
            setTotal(0);
        }};
    }
}
