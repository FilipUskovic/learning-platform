package com.micro.learningplatform.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ApiVersionIntreceptop implements HandlerInterceptor {

    private static final String VERSION_HEADER = "API-Version";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^/v(\\d+)/");


    /*
      Incepteroi koji upravljau verzioniranjem api-a podrzavaju verzioniranje kroz razlicite mehanizme
      URL, header, ili content negotiation.
     */

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestedVersion = determineRequestedVersion(request);

        // dodajemo verziju u attribute zahtijeva za kasnije koristenje
        request.setAttribute("apiVersion", requestedVersion);

        // dodajem verziju u respone header
        request.setAttribute(VERSION_HEADER, requestedVersion);

        // provjeravam pdržava li se zatrezena verzija
        validateApiVersion(requestedVersion);

        return true;

    }

    //TODO UnsupportedApiVersionException doati ovu excepton
    private void validateApiVersion(String requestedVersion) {
        // Podržane verzije
        List<String> supportedVersions = List.of("v1", "v2");

        // Provjera podržane verzije
        if (!supportedVersions.contains(requestedVersion)) {
            throw new IllegalArgumentException("API version " + requestedVersion + " is not supported");
        }
    }


    private String determineRequestedVersion(HttpServletRequest request) {
        // Prvo provjeravamo URL (/v1/resource)
        String pathVersion = extractVersionFromPath(request.getRequestURI());
        if (pathVersion != null) {
            return pathVersion;
        }

        // Zatim provjeravamo header
        String headerVersion = request.getHeader(VERSION_HEADER);
        if (headerVersion != null) {
            return headerVersion;
        }

        // Defaultna verzija ako nije specificirana
        return "v1";
    }


    private  String extractVersionFromPath(String path) {
        Matcher matcher = VERSION_PATTERN.matcher(path);
        if (matcher.find()) {
            return "v" + matcher.group(1);
        }
        return null;
    }


}
