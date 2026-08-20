package app.hamdy.spec.web.dto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Builds the shareable link. */
@Component
public class PublicUrls {

    private final String publicUrl;

    public PublicUrls(@Value("${spec.public-url:https://spec.hamdy.app}") String publicUrl) {
        this.publicUrl = publicUrl.endsWith("/")
                ? publicUrl.substring(0, publicUrl.length() - 1)
                : publicUrl;
    }

    public String forSpec(String id) {
        return publicUrl + "/" + id;
    }
}
