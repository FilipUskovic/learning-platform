package com.micro.learningplatform.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.Link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ResourceSupport {

    // klasa za sve  reoursce reprezentacije koja omogucuje hateos funkcionlasnosti

    private final List<Link> links = new ArrayList<>();

    public void add(Link link) {
        links.add(link);
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return Collections.unmodifiableList(links);
    }

}
