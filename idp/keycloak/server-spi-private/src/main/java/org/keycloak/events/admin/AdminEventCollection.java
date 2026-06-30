package org.keycloak.events.admin;

import org.keycloak.events.Event;

import java.util.ArrayList;
import java.util.List;

public class AdminEventCollection {
    private int total;
    private List<AdminEvent> events;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<AdminEvent> getEvents() {
        return events;
    }

    public void setEvents(List<AdminEvent> events) {
        this.events = events;
    }

    public static AdminEventCollection empty(){
        return new AdminEventCollection(){{
            setEvents(new ArrayList<>());
            setTotal(0);
        }};
    }
}
