package cloudSecurity.service.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP client for Keycloak that uses HttpURLConnection so it respects
 * the JVM default SSLSocketFactory and HostnameVerifier (trust-all for dev/self-signed).
 * The JAX-RS Client in Quarkus uses Vert.x and ignores SSLContext from the builder.
 */
@ApplicationScoped
public class KeycloakHttpClientProvider {

    private static final HostnameVerifier TRUST_ALL_HOSTS = (hostname, session) -> true;

    private SSLContext trustAllContext;

    public record KeycloakResponse(int statusCode, String body) {}

    @PostConstruct
    void init() {
        try {
            trustAllContext = createTrustAllSSLContext();
            HttpsURLConnection.setDefaultSSLSocketFactory(trustAllContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(TRUST_ALL_HOSTS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init trust-all SSL for Keycloak", e);
        }
    }

    /**
     * POST application/x-www-form-urlencoded to Keycloak. Uses HttpURLConnection so SSL trust-all is applied.
     */
    public KeycloakResponse postForm(String url, Map<String, String> formParams, String basicAuth) throws IOException {
        String formBody = formParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue() != null ? e.getValue() : "", StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return post(url, "application/x-www-form-urlencoded", formBody, basicAuth, null);
    }

    /**
     * POST application/json to Keycloak (e.g. admin API). Uses HttpURLConnection so SSL trust-all is applied.
     */
    public KeycloakResponse postJson(String url, String jsonBody, String bearerToken) throws IOException {
        return post(url, "application/json", jsonBody, null, bearerToken);
    }

    private KeycloakResponse post(String urlString, String contentType, String body, String basicAuth, String bearerToken) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", contentType);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);
            if (basicAuth != null) {
                conn.setRequestProperty("Authorization", "Basic " + basicAuth);
            }
            if (bearerToken != null) {
                conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }
            if (body != null && !body.isEmpty()) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }
            }
            int status = conn.getResponseCode();
            String responseBody = readBody(conn);
            return new KeycloakResponse(status, responseBody);
        } finally {
            conn.disconnect();
        }
    }

    private static String readBody(HttpURLConnection conn) throws IOException {
        java.io.InputStream in = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();
        if (in == null) return "";
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static SSLContext createTrustAllSSLContext() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }
                }
        };
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(null, trustAll, new SecureRandom());
        return ssl;
    }
}
