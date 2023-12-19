package it.finanze.sanita.fse2.ms.gtwindexer.service.impl;

import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.*;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.ConfigItemTypeEnum.INDEXER;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtwindexer.client.routes.IConfigClient;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.ConfigItemDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.ConfigItemDTO.ConfigDataItemDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ConfigItemTypeEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.ProfileUtility;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConfigSRV implements IConfigSRV {

	private static final long DELTA_MS = 300_000L;

	@Autowired
	private IConfigClient client;

	@Autowired
	private ProfileUtility profiles;

	private final Map<String, Pair<Long, String>> props;

	public ConfigSRV() {
		this.props = new HashMap<>();
	}

	@PostConstruct
	public void postConstruct() {
		if(!profiles.isTestProfile()) {
			init();
		} else {
			log.info("Skipping gtw-config initialization due to test profile");
		}
	}

	    
	@Override
	public Boolean isRemoveEds() {
		long lastUpdate = props.get(PROPS_NAME_REMOVE_EDS_ENABLE).getKey();
		if (new Date().getTime() - lastUpdate >= DELTA_MS) {
			synchronized(Locks.REMOVE_METADATA_ENABLE) {
				if (new Date().getTime() - lastUpdate >= DELTA_MS) {
					refresh(PROPS_NAME_REMOVE_EDS_ENABLE);
				}
			}
		}
		return Boolean.parseBoolean(props.get(PROPS_NAME_REMOVE_EDS_ENABLE).getValue());
	}

	private void refresh(String name) {
		String previous = props.getOrDefault(name, Pair.of(0L, null)).getValue();
		String prop = client.getProps(name, previous, INDEXER);
		props.put(name, Pair.of(new Date().getTime(), prop));
	}

	private void integrity() {
		String err = "Missing props {} from indexer";
		String[] out = new String[]{
			PROPS_NAME_REMOVE_EDS_ENABLE
		};
		for (String prop : out) {
			if(!props.containsKey(prop)) throw new IllegalStateException(err.replace("{}", prop));
		}
	}

	private void init() {
		for(ConfigItemTypeEnum en : ConfigItemTypeEnum.priority()) {
			log.info("[GTW-CFG] Retrieving {} properties ...", en.name());
			ConfigItemDTO items = client.getConfigurationItems(en);
			List<ConfigDataItemDTO> opts = items.getConfigurationItems();
			for(ConfigDataItemDTO opt : opts) {
				opt.getItems().forEach((key, value) -> {
					log.info("[GTW-CFG] Property {} is set as {}", key, value);
					props.put(key, Pair.of(new Date().getTime(), value));
				});
			}
			if(opts.isEmpty()) log.info("[GTW-CFG] No props were found");
		}
		integrity();
	}

	private static final class Locks {
		public static final Object REMOVE_METADATA_ENABLE = new Object();
	}

}
