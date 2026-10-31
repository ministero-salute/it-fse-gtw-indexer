package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.security.auth.callback.Callback;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.oauth2.CustomAuthenticateCallbackHandler;

@Tag("ISOLATION")
class CustomAuthenticateCallbackHandlerIsolationTest {
    private CustomAuthenticateCallbackHandler handler;
    private ConfidentialClientApplication client;
    private ClientCredentialParameters parameters;

    @BeforeEach
    void setUp() {
        handler = new CustomAuthenticateCallbackHandler();
        client = mock(ConfidentialClientApplication.class);
        parameters = ClientCredentialParameters.builder(Set.of("https://example.invalid/.default")).build();
        ReflectionTestUtils.setField(handler, "aadClient", client);
        ReflectionTestUtils.setField(handler, "aadParameters", parameters);
    }

    @Test
    @SuppressWarnings("unchecked")
    void interruptionRestoresFlagAndPropagatesAsIOException() throws Exception {
        CompletableFuture<IAuthenticationResult> future = mock(CompletableFuture.class);
        InterruptedException interruption = new InterruptedException("interrupted");
        when(client.acquireToken(parameters)).thenReturn(future);
        when(future.get()).thenThrow(interruption);
        OAuthBearerTokenCallback first = new OAuthBearerTokenCallback();
        OAuthBearerTokenCallback second = new OAuthBearerTokenCallback();

        try {
            IOException failure = assertThrows(IOException.class,
                () -> handler.handle(new Callback[] {first, second}));

            assertSame(interruption, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            assertNull(first.token());
            assertNull(second.token());
            verify(client).acquireToken(parameters);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void reusesInitializedClientForSuccessiveCallbacks() throws Exception {
        IAuthenticationResult result = mock(IAuthenticationResult.class);
        when(result.accessToken()).thenReturn("access-token");
        when(result.expiresOnDate()).thenReturn(new Date(123456789L));
        when(client.acquireToken(parameters)).thenReturn(CompletableFuture.completedFuture(result));
        OAuthBearerTokenCallback first = new OAuthBearerTokenCallback();
        OAuthBearerTokenCallback second = new OAuthBearerTokenCallback();

        handler.handle(new Callback[] {first, second});

        assertEquals("access-token", first.token().value());
        assertEquals(123456789L, first.token().lifetimeMs());
        assertEquals("access-token", second.token().value());
        verify(client, times(2)).acquireToken(parameters);
        assertSame(client, ReflectionTestUtils.getField(handler, "aadClient"));
    }
}
