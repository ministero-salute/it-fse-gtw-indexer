package it.finanze.sanita.fse2.ms.gtwindexer.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IConfigClient;

@Service
public class ConfigSRV implements IConfigSRV {

    private static final Long DELTA_MS = 300000L;

    @Autowired
    private IConfigClient configClient;

    private String edsStrategy;

    private Long lastUpdate;

    @Async
    @EventListener(ApplicationStartedEvent.class)
    void initialize() {
        refreshEdsStrategy();
      
    }

    private void refreshEdsStrategy() {
        synchronized (this) {
            edsStrategy = configClient.getEDSStrategy();  
            lastUpdate = new Date().getTime();
        }
    }

    @Override
    public String getEdsStrategy() {
        Long passedTime = new Date().getTime() - lastUpdate;
        if (passedTime >= DELTA_MS) {
            synchronized (this) {
                refreshEdsStrategy();
            }
        }
        return edsStrategy;
    }
}
