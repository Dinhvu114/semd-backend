package com.semd.backend.service.otp;

import com.semd.backend.exception.OtpDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.otp.delivery-mode", havingValue = "sms")
public class HttpSmsOtpDeliveryService implements OtpDeliveryService {

    private final RestClient restClient;
    private final String endpoint;
    private final String apiToken;
    private final String sender;
    private final String messageTemplate;

    public HttpSmsOtpDeliveryService(
            RestClient.Builder restClientBuilder,
            @Value("${app.sms.endpoint:}") String endpoint,
            @Value("${app.sms.api-token:}") String apiToken,
            @Value("${app.sms.sender:SEMD}") String sender,
            @Value("${app.sms.message-template:Ma xac thuc SEMD cua ban la %s. Ma co hieu luc trong 5 phut.}") String messageTemplate
    ) {
        this.restClient = restClientBuilder.build();
        this.endpoint = endpoint;
        this.apiToken = apiToken;
        this.sender = sender;
        this.messageTemplate = messageTemplate;
    }

    @Override
    public void send(String phoneNumber, String otpCode) {
        if (endpoint.isBlank() || apiToken.isBlank()) {
            throw new OtpDeliveryException("Chưa cấu hình endpoint hoặc API token của nhà cung cấp SMS");
        }

        Map<String, Object> payload = Map.of(
                "to", phoneNumber,
                "sender", sender,
                "message", messageTemplate.formatted(otpCode)
        );

        try {
            restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiToken)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new OtpDeliveryException("Nhà cung cấp SMS không thể gửi mã OTP", exception);
        }
    }

    @Override
    public String providerName() {
        return "HTTP_SMS";
    }
}
