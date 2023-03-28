package it.finanze.sanita.fse2.ms.gtwindexer.config.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

	@Autowired
    private AdminClient client;
	
	@Override
	public Health health() {
		Health health = null;
		try {
			client.listTopics().listings().get();
			health = Health.up().build();
		} catch (InterruptedException e) {
			log.warn("Interrupted!", e);
			health = Health.down(e).build();
			// Restore interrupted state...
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			health = Health.down(e).build();
		}
		return health;
	}
}