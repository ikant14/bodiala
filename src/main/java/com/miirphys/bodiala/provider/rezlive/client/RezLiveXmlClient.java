package com.miirphys.bodiala.provider.rezlive.client;

import com.miirphys.bodiala.provider.rezlive.RezLiveProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Low-level transport for the RezLive action-based XML API.
 *
 * <p>Every call: {@code POST {baseUrl}/{action}} with header {@code x-api-key}, a body of
 * {@code XML=<url-encoded xml>} as {@code application/x-www-form-urlencoded}, and a response
 * that may be gzip-compressed XML.
 */
@Component
public class RezLiveXmlClient {

    private static final Logger log = LoggerFactory.getLogger(RezLiveXmlClient.class);

    private final RezLiveProperties properties;
    private final RezLiveXmlCodec codec;
    private final RestClient restClient;

    public RezLiveXmlClient(RezLiveProperties properties, RezLiveXmlCodec codec) {
        this.properties = properties;
        this.codec = codec;
        // Finite timeouts so a slow/hung RezLive endpoint cannot pin a servlet thread forever.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * Marshal {@code request}, POST it to the given action, and unmarshal the response.
     *
     * @param action       RezLive action name, e.g. {@code gethoteldetails}
     * @param request      JAXB-annotated request object
     * @param responseType JAXB-annotated response type
     */
    public <T> T execute(String action, Object request, Class<T> responseType) {
        String xml = codec.marshal(request);
        String form = "XML=" + URLEncoder.encode(xml, StandardCharsets.UTF_8);
        String url = properties.getBaseUrl() + "/" + action;

        log.debug("POST {} ({} bytes of XML)", url, xml.length());
        byte[] raw = restClient.post()
                .uri(url)
                .header("x-api-key", properties.getApiKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .body(form)
                .retrieve()
                .body(byte[].class);

        String responseXml = codec.decodeBody(raw);
        if (log.isTraceEnabled()) {
            log.trace("RezLive {} response:\n{}", action, responseXml);
        }
        return codec.unmarshal(responseXml, responseType);
    }
}
