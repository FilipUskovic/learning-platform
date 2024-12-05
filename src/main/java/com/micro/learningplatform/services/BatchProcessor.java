package com.micro.learningplatform.services;

import java.util.List;

@FunctionalInterface
public interface BatchProcessor <T>{
    void process(List<T> batch) throws Exception;
}
