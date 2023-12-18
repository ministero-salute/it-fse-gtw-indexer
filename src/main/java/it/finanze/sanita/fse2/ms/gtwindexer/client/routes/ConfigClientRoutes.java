package it.finanze.sanita.fse2.ms.gtwindexer.client.routes;

import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.API_CONFIG_ITEMS;
import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.API_PROPS;
import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.API_STATUS;
import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.API_VERSION;
import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.API_WHOIS;
import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.IDENTIFIER;
import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.IDENTIFIER_MS;
import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.QP_PROPS;
import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.QP_TYPE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import it.finanze.sanita.fse2.ms.gtwindexer.enums.ConfigItemTypeEnum;

@Component
public final class ConfigClientRoutes {

    @Value("${ms.url.gtw-config}")
    private String configHost;

    public UriComponentsBuilder base() {
        return UriComponentsBuilder.fromHttpUrl(configHost);
    }

    public String identifier() {
        return IDENTIFIER;
    }

    public String microservice() {
        return IDENTIFIER_MS;
    }

    public String status() {
        return base()
                .pathSegment(API_STATUS)
                .build()
                .toUriString();
    }

    public String whois() {
        return base()
                .pathSegment(API_VERSION, API_WHOIS)
                .build()
                .toUriString();
    }

    public String getConfigItem(ConfigItemTypeEnum type, String props) {
        return base()
                .pathSegment(API_VERSION, API_CONFIG_ITEMS, API_PROPS)
                .queryParam(QP_TYPE, type.name())
                .queryParam(QP_PROPS, props)
                .build()
                .toUriString();
    }

    public String getConfigItems(ConfigItemTypeEnum type) {
        return base()
                .pathSegment(API_VERSION, API_CONFIG_ITEMS)
                .queryParam(QP_TYPE, type.name())
                .build()
                .toUriString();
    }
}