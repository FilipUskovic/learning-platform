package com.micro.learningplatform.event;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

// pattern za elegantnije upravljanje greškam
public sealed interface Result<S,E> {

    static <S, E> Result<S, E> of(Supplier<S> supplier) {
        try {
            return new Success<>(supplier.get());
        } catch (Exception e) {
            return (Result<S, E>) new Error<>(new EventError(e));
        }
    }

    Result<S, E> ifSuccess(Consumer<S> consumer);
    Result<S, E> ifError(Consumer<E> consumer);
    Optional<S> ifPresent(Consumer<S> consumer);

    record Success<S, E>(S value) implements Result<S, E> {
        @Override
        public Result<S, E> ifSuccess(Consumer<S> consumer) {
            consumer.accept(value);
            return this;
        }

        @Override
        public Result<S, E> ifError(Consumer<E> consumer) {
            return this;
        }

        @Override
        public Optional<S> ifPresent(Consumer<S> consumer) {
            consumer.accept(value);
            return Optional.of(value);
        }
    }

    record Error<S, E>(E error) implements Result<S, E> {
        @Override
        public Result<S, E> ifSuccess(Consumer<S> consumer) {
            return this;
        }

        @Override
        public Result<S, E> ifError(Consumer<E> consumer) {
            consumer.accept(error);
            return this;
        }

        @Override
        public Optional<S> ifPresent(Consumer<S> consumer) {
            return Optional.empty();
        }
    }
}
