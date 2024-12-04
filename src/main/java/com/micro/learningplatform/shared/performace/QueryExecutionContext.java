package com.micro.learningplatform.shared.performace;


import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Kontekst izvršavanja upita koji sadrži sve relevantne informacije
 * potrebne za debugiranje i analizu problema.
 */

@Builder
@Getter
public class QueryExecutionContext {

    private final LocalDateTime executionTime;
    private final Map<String, Object> parameters;
    private final String executionEnvironment;
    private final Map<String, String> additionalInfo;

    /**
     * Kreira kopiju konteksta s dodatnim informacijama.
     */
    public QueryExecutionContext withAdditionalInfo(String key, String value) {
        Map<String, String> newInfo = new HashMap<>(additionalInfo);
        newInfo.put(key, value);
        return new QueryExecutionContext(executionTime, parameters,
                executionEnvironment, newInfo);
    }

    /**
     * Kreira string reprezentaciju konteksta pogodnu za logiranje.
     */
    public String toLogString() {
        return String.format(
                "Execution Context [Time: %s, Environment: %s, Parameters: %s, Additional Info: %s]",
                executionTime,
                executionEnvironment,
                formatParameters(),
                additionalInfo
        );
    }

    private String formatParameters() {
        return parameters.entrySet().stream()
                .map(e -> String.format("%s: %s", e.getKey(),
                        maskSensitiveData(e.getValue())))
                .collect(Collectors.joining(", "));
    }

    private String maskSensitiveData(Object value) {
        // Maskiramo potencijalno osjetljive podatke u logovima
        if (value == null) return "null";
        return value.toString().length() > 50
                ? value.toString().substring(0, 47) + "..."
                : value.toString();
    }
}
