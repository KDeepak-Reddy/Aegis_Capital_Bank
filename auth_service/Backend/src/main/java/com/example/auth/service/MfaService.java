package com.example.auth.service;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class MfaService {

    public String generateSecretKey() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        return secretGenerator.generate();
    }

    public String generateQrCodeImageUri(String secret,String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("SecureAuth")
                .build();

        QrGenerator generator = new ZxingPngQrGenerator();
        byte[] imageData;
        try {
            imageData = generator.generate(data);
        } catch (QrGenerationException e) {
            throw new RuntimeException("Error while generating QR code", e);
        }

        return getDataUriForImage(imageData, generator.getImageMimeType());
    }

    public boolean verifyCode(String secret, String code) {
        if(code == null || !code.matches("\\d{6}")){
            return false;
        }
        TimeProvider timeProvider = new SystemTimeProvider();
        DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

        // Allow a 1-step (30s) window on either side to handle slight clock drift
        verifier.setTimePeriod(30);
        verifier.setAllowedTimePeriodDiscrepancy(2);

        return verifier.isValidCode(secret, code);
    }
}
