package com.micro.learningplatform.cache;


import java.time.Duration;

public record CacheSpec(
        String name,
        long maxSize,
        Duration expireAfterWrite
) {
    public static CacheSpecBuilder builder() {
        return new CacheSpecBuilder();
    }

    public static class CacheSpecBuilder {
        private String name;
        private long maxSize;
        private Duration expireAfterWrite;

        public CacheSpecBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CacheSpecBuilder maxSize(long maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public CacheSpecBuilder expireAfterWrite(Duration expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
            return this;
        }

        public CacheSpec build() {
            return new CacheSpec(name, maxSize, expireAfterWrite);
        }
    }
}

