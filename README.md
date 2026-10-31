
# Fascicolo Sanitario 2.0

# _it-fse-gtw-indexer_

Il microservizio in oggetto ha la responsabilità di richiamare il microservizio it-fse-gtw-ini-client per l'indicizzazione dei documenti su INI e, in caso di successo, richiamare il microservizio it-fse-gtw-publisher.

<br/>

## Repos
- [*it-fse-support*](https://github.com/ministero-salute/it-fse-support) - Pagina di supporto FSE 2.0
- [*it-fse-landing*](https://github.com/ministero-salute/it-fse-landing) - Landing page dei repository FSE 2.0

<br/>

## Isolation tests

With Java 21, run the isolated suite without Kafka, INI, or gtw-config:

```sh
MAVEN_OPTS=-Xmx768m mvn -PISOLATION clean test
```

`@Tag("ISOLATION")` selects these tests only with the Maven profile; normal
`mvn test` excludes them and retains the existing integration tests.
For Spring context tests, `@ActiveProfiles("ISOLATION")` activates the Spring
profile, not test selection. Load only the tested components with
`@SpringJUnitConfig`, never `AbstractTest` or the full application.

The suite tests real configuration services/cache, HTTP clients/routes,
Kafka processing/retries/status messages, lazy producer/consumer factories,
security options, topic naming, admin configuration, OAuth token callbacks,
health reporting, telemetry wiring, response contracts, file utilities,
accreditation simulation, and the in-memory liveness endpoint.
External HTTP, Kafka, DNS, and credential/token dependencies are mocked;
there is no embedded broker, network access, or application startup.
The test JVM is limited to 768 MB while preserving the JaCoCo agent.

JaCoCo reports are generated during `test`: `target/site/jacoco/index.html`
and `target/site/jacoco/jacoco.xml`. Real connectivity and full application startup remain
outside this suite's scope.

## License

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
