package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.microsoft.aad.msal4j.*;

import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.oauth2.*;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;

@Tag("ISOLATION")
class OAuthIsolationTest {
    @TempDir
    Path directory;

    private CustomAuthenticateCallbackHandler configured(Path certificate) {
        var handler = new CustomAuthenticateCallbackHandler();
        handler.configure(Map.of("bootstrap.servers", List.of("broker.invalid:9093"),
                "kafka.oauth.tenantId", "tenant", "kafka.oauth.appId", "app",
                "kafka.oauth.pfxPathName", certificate.toString(), "kafka.oauth.pwd", "password"),
                "OAUTHBEARER", List.of());
        return handler;
    }

    @Test
    void buildsCredentialAndReusesClientWhileRequestingFreshTokens() throws Exception {
        Path certificate = directory.resolve("client.pfx");
        Files.write(certificate, new byte[] {1, 2, 3});
        var handler = configured(certificate);
        var credential = mock(IClientCertificate.class);
        var client = mock(ConfidentialClientApplication.class);
        var builder = mock(ConfidentialClientApplication.Builder.class);
        when(builder.authority("https://login.microsoftonline.com/tenant")).thenReturn(builder);
        when(builder.build()).thenReturn(client);
        var result = mock(IAuthenticationResult.class);
        Date expiry = new Date(123456789L);
        when(result.accessToken()).thenReturn("access-token");
        when(result.expiresOnDate()).thenReturn(expiry);
        var failedToken = new CompletableFuture<IAuthenticationResult>();
        failedToken.completeExceptionally(new IllegalStateException("identity provider unavailable"));
        when(client.acquireToken(any(ClientCredentialParameters.class)))
                .thenReturn(CompletableFuture.completedFuture(result), CompletableFuture.completedFuture(result), failedToken);
        try (var credentials = mockStatic(ClientCredentialFactory.class);
             var applications = mockStatic(ConfidentialClientApplication.class)) {
            credentials.when(() -> ClientCredentialFactory.createFromCertificate(any(InputStream.class), eq("password")))
                    .thenReturn(credential);
            applications.when(() -> ConfidentialClientApplication.builder("app", credential)).thenReturn(builder);
            for (int i = 0; i < 2; i++) {
                var callback = new OAuthBearerTokenCallback();
                handler.handle(new Callback[] {callback});
                assertEquals("access-token", callback.token().value());
                assertEquals(expiry.getTime(), callback.token().lifetimeMs());
                assertNull(callback.token().scope());
                assertNull(callback.token().principalName());
                assertNull(callback.token().startTimeMs());
            }
            applications.verify(() -> ConfidentialClientApplication.builder("app", credential), times(1));
            var failedCallback = new OAuthBearerTokenCallback();
            assertDoesNotThrow(() -> handler.handle(new Callback[] {failedCallback}));
            assertNull(failedCallback.token());
            verify(client, times(3)).acquireToken(argThat((ClientCredentialParameters p) ->
                    p.scopes().equals(java.util.Set.of("https://broker.invalid/.default"))));
        }
        handler.close();
    }

    @Test
    void rejectsUnsupportedCallbackWithoutCredentialAccess() {
        var handler = configured(directory.resolve("absent.pfx"));
        var callback = new NameCallback("name");
        assertSame(callback, assertThrows(UnsupportedCallbackException.class,
                () -> handler.handle(new Callback[] {callback})).getCallback());
    }

    @Test
    void missingCertificatePreservesFileFailure() {
        var handler = configured(directory.resolve("absent.pfx"));
        assertInstanceOf(java.io.FileNotFoundException.class, assertThrows(BusinessException.class,
                () -> handler.handle(new Callback[] {new OAuthBearerTokenCallback()})).getCause());
    }

    @Test
    void malformedCertificateIsWrappedWithoutAttemptingTokenAcquisition() throws Exception {
        Path certificate = directory.resolve("invalid.pfx");
        Files.writeString(certificate, "not a certificate");
        var handler = configured(certificate);
        var failure = assertThrows(BusinessException.class,
                () -> handler.handle(new Callback[] {new OAuthBearerTokenCallback()}));
        assertNotNull(failure.getCause());
        assertFalse(failure.getCause() instanceof java.io.FileNotFoundException);
    }

    @Test
    void tokenValueAndExpiryAreRetained() {
        var token = new OAuthBearerTokenImp("token", new Date(4500));
        assertEquals("token", token.value());
        assertEquals(4500, token.lifetimeMs());
    }
}
