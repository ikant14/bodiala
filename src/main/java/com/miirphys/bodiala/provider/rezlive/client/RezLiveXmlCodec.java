package com.miirphys.bodiala.provider.rezlive.client;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.stereotype.Component;

/**
 * Marshals request DTOs to XML and unmarshals RezLive XML responses, with defensive handling
 * for gzip-compressed payloads.
 *
 * <p>Kept free of any HTTP concern so it can be unit-tested directly against the documented
 * sample XML.
 */
@Component
public class RezLiveXmlCodec {

    private final ConcurrentHashMap<Class<?>, JAXBContext> contexts = new ConcurrentHashMap<>();

    /** Serialize a JAXB-annotated request object to an XML string (no XML declaration). */
    public String marshal(Object request) {
        try {
            StringWriter writer = new StringWriter();
            Marshaller marshaller = context(request.getClass()).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.marshal(request, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new RezLiveTransportException("Failed to marshal " + request.getClass().getSimpleName(), e);
        }
    }

    /** Parse an XML string into the given response type. Namespace/encoding tolerant. */
    public <T> T unmarshal(String xml, Class<T> type) {
        try {
            Unmarshaller unmarshaller = context(type).createUnmarshaller();
            XMLStreamReader reader = inputFactory().createXMLStreamReader(new StringReader(xml));
            try {
                return type.cast(unmarshaller.unmarshal(reader, type).getValue());
            } finally {
                reader.close();
            }
        } catch (JAXBException | XMLStreamException e) {
            throw new RezLiveTransportException("Failed to unmarshal " + type.getSimpleName(), e);
        }
    }

    /**
     * Decode a raw HTTP response body to a String. If the bytes are gzip-compressed (magic
     * {@code 0x1f 0x8b}), they are gunzipped first; otherwise treated as UTF-8 text. This
     * covers both a gzip body the client did not auto-decode and a plain-text body.
     */
    public String decodeBody(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return "";
        }
        try {
            if (raw.length >= 2 && (raw[0] & 0xff) == 0x1f && (raw[1] & 0xff) == 0x8b) {
                try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(raw))) {
                    return new String(gis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            return new String(raw, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RezLiveTransportException("Failed to decode response body (corrupt gzip?)", e);
        }
    }

    private JAXBContext context(Class<?> type) {
        return contexts.computeIfAbsent(type, t -> {
            try {
                return JAXBContext.newInstance(t);
            } catch (JAXBException e) {
                throw new RezLiveTransportException("Failed to create JAXBContext for " + t.getName(), e);
            }
        });
    }

    private static XMLInputFactory inputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        // Harden against XXE — these responses never legitimately contain external entities.
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        return factory;
    }
}
