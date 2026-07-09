package com.miirphys.bodiala.provider.rezlive.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import com.miirphys.bodiala.provider.rezlive.client.dto.content.HotelDetailsRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.content.HotelDetailsResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

class RezLiveXmlCodecTest {

    private final RezLiveXmlCodec codec = new RezLiveXmlCodec();

    @Test
    void marshalsHotelDetailsRequestWithAuthAndHotelIds() {
        HotelDetailsRequest request = new HotelDetailsRequest(
                new Authentication("AG123", "myuser"), List.of("XHUB18"));

        String xml = codec.marshal(request).replaceAll("\\s+", "");

        assertThat(xml).contains("<HotelDetailsRequest>");
        assertThat(xml).contains("<Authentication><AgentCode>AG123</AgentCode><UserName>myuser</UserName></Authentication>");
        assertThat(xml).contains("<Hotels><HotelId>XHUB18</HotelId></Hotels>");
    }

    @Test
    void unmarshalsHotelDetailsResponse() {
        String xml = """
                <?xml version="1.0"?>
                <HotelDetailsResponse>
                  <Hotels>
                    <HotelId>XHBE9179</HotelId>
                    <HotelName>Minotel Prince de Liege</HotelName>
                    <Rating>3</Rating>
                    <City>Brussels</City>
                    <Country>Belgium</Country>
                    <Description>Ideally located hotel</Description>
                    <HotelAddress>CHAUSSEE DE NINOVE 664 Brussels</HotelAddress>
                    <Latitude>50.8468</Latitude>
                    <Longitude>4.29978</Longitude>
                    <HotelPostalCode>1070</HotelPostalCode>
                    <HotelAmenities>Bathroom,Shower</HotelAmenities>
                    <Images>
                      <Image>http://img/1</Image>
                      <Image>http://img/2</Image>
                    </Images>
                  </Hotels>
                </HotelDetailsResponse>
                """;

        HotelDetailsResponse response = codec.unmarshal(xml, HotelDetailsResponse.class);

        assertThat(response.hasError()).isFalse();
        assertThat(response.getHotels()).hasSize(1);
        HotelDetailsResponse.HotelDetail hotel = response.getHotels().get(0);
        assertThat(hotel.getHotelId()).isEqualTo("XHBE9179");
        assertThat(hotel.getHotelName()).isEqualTo("Minotel Prince de Liege");
        assertThat(hotel.getHotelPostalCode()).isEqualTo("1070");
        assertThat(hotel.getImages()).containsExactly("http://img/1", "http://img/2");
    }

    @Test
    void unmarshalsErrorResponse() {
        String xml = "<HotelDetailsResponse><error>Invalid API key</error></HotelDetailsResponse>";

        HotelDetailsResponse response = codec.unmarshal(xml, HotelDetailsResponse.class);

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError()).contains("Invalid API key");
        assertThat(response.getHotels()).isEmpty();
    }

    @Test
    void decodeBodyGunzipsCompressedPayload() throws IOException {
        String original = "<HotelDetailsResponse><error>x</error></HotelDetailsResponse>";
        byte[] gzipped;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            gos.write(original.getBytes(StandardCharsets.UTF_8));
            gos.finish();
            gzipped = bos.toByteArray();
        }

        assertThat(codec.decodeBody(gzipped)).isEqualTo(original);
    }

    @Test
    void decodeBodyHandlesPlainTextAndEmpty() {
        assertThat(codec.decodeBody("<x/>".getBytes(StandardCharsets.UTF_8))).isEqualTo("<x/>");
        assertThat(codec.decodeBody(new byte[0])).isEmpty();
        assertThat(codec.decodeBody(null)).isEmpty();
    }
}
