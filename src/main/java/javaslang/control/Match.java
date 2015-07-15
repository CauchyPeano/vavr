/*     / \____  _    ______   _____ / \____   ____  _____
 *    /  \__  \/ \  / \__  \ /  __//  \__  \ /    \/ __  \   Javaslang
 *  _/  // _\  \  \/  / _\  \\_  \/  // _\  \  /\  \__/  /   Copyright 2014-2015 Daniel Dietrich
 * /___/ \_____/\____/\_____/____/\___\_____/_/  \_/____/    Licensed under the Apache License, Version 2.0
 */
package javaslang.control;

import javaslang.Function1;
import javaslang.Function2;
import javaslang.Lazy;
import javaslang.Value;
import javaslang.collection.List;
import javaslang.collection.TraversableOnce;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * {@code Match} is a better switch for Java. Some characteristics of {@code Match} are:
 * <ul>
 * <li>it has a fluent API</li>
 * <li>it is a {@code Function<Object, R>}</li>
 * <li>it is able to match types, i.e. {@code Match.when((byte b) -> "a byte: " + b)}</li>
 * <li>it is able to match values, i.e. {@code Match.when(BigDecimal.ZERO, b -> "Zero: " + b)}</li>
 * </ul>
 *
 * Example of a Match as <a href="http://en.wikipedia.org/wiki/Partial_function"><strong>partial</strong> function</a>:
 *
 * <pre>
 * <code>
 * final Match&lt;Number&gt; toNumber = Match.as(Number.class)
 *     .when((Integer i) -&gt; i)
 *     .when((String s) -&gt; new BigDecimal(s));
 * final Number number = toNumber.apply(1.0d); // throws a MatchError
 * </code>
 * </pre>
 *
 * Example of a Match as <a href="http://en.wikipedia.org/wiki/Function_(mathematics)"><strong>total</strong> function</a>:
 *
 * <pre>
 * <code>
 * Match.as(Number.class)
 *     .when((Integer i) -&gt; i)
 *     .when((String s) -&gt; new BigDecimal(s))
 *     .otherwise(() -&gt; -1)
 *     .apply(1.0d); // result: -1
 * </code>
 * </pre>
 *
 * @param <R> The result type of the {@code Match}.
 * @since 1.0.0
 */
public interface Match<R> extends Function<Object, R> {

    /**
     * Internally used. Will be private in a future version.
     */
    Function2<Object, Object, Boolean> MATCH_BY_VALUE = (v, o) ->
            (v == o) || (v != null && v.equals(o));

    /**
     * Internally used. Will be private in a future version.
     */
    Function2<Class<?>, Object, Boolean> MATCH_BY_TYPE = (t, o) ->
            o != null && t.isAssignableFrom(o.getClass());

    /**
     * Internally used. Will be private in a future version.
     */
    Function2<Predicate<?>, Object, Boolean> MATCH_BY_PREDICATE = (p, o) -> null;

    /**
     * Internally used. Will be private in a future version.
     */
    Function2<Function<?, ?>, Object, Boolean> MATCH_BY_FUNCTION = (f, o) -> null;

    /**
     * Applies this {@code Match} to an {@code Object}.
     *
     * @param o an {@code Object}
     * @throws MatchError if no {@code Case} matched
     */
    @Override
    R apply(Object o);

    /**
     * Creates a type-safe match by fixating the value to be matched.
     *
     * @param value the value to be matched
     * @return a new type-safe match builder
     */
    static <T> MatchMonad.Of<T> of(T value) {
        return new MatchMonad.Of<>(value);
    }

    /**
     * Specifies the type of the match expression. In many cases it is not necessary to call {@code as}. This
     * method is intended to be used for readability reasons when the upper bound of the cases cannot be inferred,
     * i.e. instead of
     *
     * <pre>
     * <code>
     * final Match&lt;Number&gt; toNumber = Match
     *         .&lt;Number&gt; when((Integer i) -&gt; i)
     *         .when((String s) -&gt; new BigDecimal(s))
     * </code>
     * </pre>
     *
     * we write
     *
     * <pre>
     * <code>
     * final Match&lt;Number&gt; toNumber = Match.as(Number.class)
     *         .when((Integer i) -&gt; i)
     *         .when((String s) -&gt; new BigDecimal(s))
     * </code>
     * </pre>
     *
     * @param type the hint of type {@code R}
     * @param <R>  the type of the {@code Match} expression
     * @return a new match builder
     */
    static <R> MatchFunction.Typed<R> as(Class<R> type) {
        Objects.requireNonNull(type, "type is null");
        return new MatchFunction.Typed<>();
    }

    /**
     * Creates a {@code Match.Case} by value.
     *
     * @param <T>       type of the prototype value
     * @param prototype A specific value to be matched
     * @return a new {@code Case}
     * @throws NullPointerException if {@code function} is null
     */
    static <T> MatchFunction.WhenUntyped<T> when(T prototype) {
        return new MatchFunction.WhenUntyped<>(prototype);
    }

    static <T> MatchFunction.WhenTrueUntyped<T> when(Predicate<? super T> predicate) {
        return new MatchFunction.WhenTrueUntyped<>(predicate);
    }

    @SuppressWarnings("unchecked")
    static <T> MatchFunction.WhenInUntyped<T> whenIn(T... prototypes) {
        Objects.requireNonNull(prototypes, "prototypes is null");
        return new MatchFunction.WhenInUntyped<>(prototypes);
    }

    static <T> MatchFunction.WhenTypeUntyped<T> whenType(Class<T> type) {
        Objects.requireNonNull(type, "type is null");
        return new MatchFunction.WhenTypeUntyped<>(type);
    }

    @SuppressWarnings("unchecked")
    static <T> MatchFunction.WhenTypeInUntyped<T> whenTypeIn(Class<T>... types) {
        Objects.requireNonNull(types, "types is null");
        return new MatchFunction.WhenTypeInUntyped<>(types);
    }

    static <T, R> MatchFunction.WhenApplicable<T, R> whenApplicable(Function1<? super T, ? extends R> function) {
        return new MatchFunction.WhenApplicable<>(function, List.nil());
    }

    /**
     * @since 1.9.9
     */
    interface MatchFunction {

        interface WithWhen<R> {

            /**
             * Creates a {@code Match.When} by value.
             *
             * @param <T>       type of the prototype value
             * @param prototype A specific value to be matched
             * @return a new {@code Case}
             * @throws NullPointerException if {@code function} is null
             */
            <T> When<T, R> when(T prototype);

            <T> WhenTrue<T, R> whenTrue(Predicate<? super T> predicate);

            @SuppressWarnings("unchecked")
            <T> WhenIn<T, R> whenIn(T... prototypes);

            <T> WhenType<T, R> whenType(Class<T> type);

            @SuppressWarnings("unchecked")
            <T> WhenTypeIn<T, R> whenTypeIn(Class<T>... types);

            <T> WhenApplicable<T, R> whenApplicable(Function1<? super T, ? extends R> function);
        }

        interface WithThenUntyped<T> {

            <R> Then<R> then(Function<? super T, ? extends R> function);

            default <R> Then<R> then(R that) {
                return then(ignored -> that);
            }

            default <R> Then<R> then(Supplier<? extends R> supplier) {
                Objects.requireNonNull(supplier, "supplier is null");
                return then(ignored -> supplier.get());
            }
        }

        interface WithThen<T, R> {

            Then<R> then(Function<? super T, ? extends R> function);

            default Then<R> then(R that) {
                return then(ignored -> that);
            }

            default Then<R> then(Supplier<? extends R> supplier) {
                Objects.requireNonNull(supplier, "supplier is null");
                return then(ignored -> supplier.get());
            }
        }

        /**
         * The result of {@code Match.as(Class)}, which explicitly sets the {@code Match} result type.
         *
         * @param <R> the result type
         * @since 1.2.1
         */
        final class Typed<R> implements WithWhen<R> {

            private Typed() {
            }

            @Override
            public <T> When<T, R> when(T prototype) {
                return new When<>(prototype, List.nil());
            }

            @Override
            public <T> WhenTrue<T, R> whenTrue(Predicate<? super T> predicate) {
                Objects.requireNonNull(predicate, "predicate is null");
                return new WhenTrue<>(predicate, List.nil());
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> WhenIn<T, R> whenIn(T... prototypes) {
                Objects.requireNonNull(prototypes, "prototypes is null");
                return new WhenIn<>(prototypes, List.nil());
            }

            @Override
            public <T> WhenType<T, R> whenType(Class<T> type) {
                Objects.requireNonNull(type, "type is null");
                return new WhenType<>(type, List.nil());
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> WhenTypeIn<T, R> whenTypeIn(Class<T>... types) {
                Objects.requireNonNull(types, "types is null");
                return new WhenTypeIn<>(types, List.nil());
            }

            @Override
            public <T> WhenApplicable<T, R> whenApplicable(Function1<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                return new WhenApplicable<>(function, List.nil());
            }
        }

        final class WhenUntyped<T> implements WithThenUntyped<T> {

            private final T prototype;

            private WhenUntyped(T prototype) {
                this.prototype = prototype;
            }

            @Override
            public <R> Then<R> then(Function<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                return new Then<>(List.of(Case.byValue(prototype, function)));
            }
        }

        final class When<T, R> implements WithThen<T, R> {

            private final T prototype;
            private final List<Case<R>> cases;

            private When(T prototype, List<Case<R>> cases) {
                this.prototype = prototype;
                this.cases = cases;
            }

            @Override
            public Then<R> then(Function<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                return new Then<>(cases.prepend(Case.byValue(prototype, function)));
            }
        }

        final class WhenTrueUntyped<T> implements WithThenUntyped<T> {

            private final Predicate<? super T> predicate;

            private WhenTrueUntyped(Predicate<? super T> predicate) {
                this.predicate = predicate;
            }

            @Override
            public <R> Then<R> then(Function<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                return new Then<>(List.of(Case.byPredicate(predicate, function)));
            }
        }

        final class WhenTrue<T, R> implements WithThen<T, R> {

            private final Predicate<? super T> predicate;
            private final List<Case<R>> cases;

            private WhenTrue(Predicate<? super T> predicate, List<Case<R>> cases) {
                this.predicate = predicate;
                this.cases = cases;
            }

            @Override
            public Then<R> then(Function<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                return new Then<>(cases.prepend(Case.byPredicate(predicate, function)));
            }
        }

        final class WhenInUntyped<T> implements WithThenUntyped<T> {

            private final T[] prototypes;

            private WhenInUntyped(T[] prototypes) {
                this.prototypes = prototypes;
            }

            @Override
            public <R> Then<R> then(Function<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                final List<Case<R>> cases = List.of(prototypes)
                        .foldLeft(List.nil(), (ts, t) -> ts.prepend(Case.byValue(t, function)));
                return new Then<>(cases);
            }
        }

        final class WhenIn<T, R> implements WithThen<T, R> {

            private final T[] prototypes;
            private final List<Case<R>> cases;

            private WhenIn(T[] prototypes, List<Case<R>> cases) {
                this.prototypes = prototypes;
                this.cases = cases;
            }

            @Override
            public Then<R> then(Function<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                final List<Case<R>> newCases = List.of(prototypes)
                        .foldLeft(List.nil(), (ts, t) -> ts.prepend(Case.byValue(t, function)));
                return new Then<>(cases.prependAll(newCases));
            }
        }

        final class WhenTypeUntyped<T> implements WithThenUntyped<T> {

            private final Class<T> type;

            private WhenTypeUntyped(Class<T> type) {
                this.type = type;
            }

            @Override
            public <R> Then<R> then(Function<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                final List<Case<R>> cases = List.of(Case.byType(type, function));
                return new Then<>(cases);
            }
        }

        final class WhenType<T, R> implements WithThen<T, R> {

            private final Class<T> type;
            private final List<Case<R>> cases;

            private WhenType(Class<T> type, List<Case<R>> cases) {
                this.type = type;
                this.cases = cases;
            }

            @Override
            public Then<R> then(Function<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                final List<Case<R>> newCases = List.of(Case.byType(type, function));
                return new Then<>(cases.prependAll(newCases));
            }
        }

        final class WhenTypeInUntyped<T> implements WithThenUntyped<T> {

            private final Class<T>[] types;

            private WhenTypeInUntyped(Class<T>[] types) {
                this.types = types;
            }

            @Override
            public <R> Then<R> then(Function<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                final List<Case<R>> cases = List.of(types)
                        .foldLeft(List.nil(), (ts, t) -> ts.prepend(Case.byType(t, function)));
                return new Then<>(cases);
            }
        }

        final class WhenTypeIn<T, R> implements WithThen<T, R> {

            private final Class<T>[] types;
            private final List<Case<R>> cases;

            private WhenTypeIn(Class<T>[] types, List<Case<R>> cases) {
                this.types = types;
                this.cases = cases;
            }

            @Override
            public Then<R> then(Function<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                final List<Case<R>> newCases = List.of(types)
                        .foldLeft(List.nil(), (ts, t) -> ts.prepend(Case.byType(t, function)));
                return new Then<>(cases.prependAll(newCases));
            }
        }

        final class WhenApplicable<T, R> {

            private final Then<R> then;

            private WhenApplicable(Function1<? super T, ? extends R> function, List<Case<R>> cases) {
                this.then = new Then<>(cases.prepend(Case.byFunction(function)));
            }

            public Then<R> thenApply() {
                return then;
            }
        }

        final class Then<R> implements Match<R>, WithWhen<R> {

            private final List<Case<R>> cases;

            private Then(List<Case<R>> cases) {
                this.cases = cases;
            }

            @Override
            public R apply(Object o) {
                return cases
                        .reverse()
                        .findFirst(caze -> caze.isApplicable(o))
                        .map(caze -> caze.apply(o))
                        .orElseThrow(() -> new MatchError(o));
            }

            @Override
            public <T> When<T, R> when(T prototype) {
                return new When<>(prototype, cases);
            }

            @Override
            public <T> WhenTrue<T, R> whenTrue(Predicate<? super T> predicate) {
                Objects.requireNonNull(predicate, "predicate is null");
                return new WhenTrue<>(predicate, cases);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> WhenIn<T, R> whenIn(T... prototypes) {
                Objects.requireNonNull(prototypes, "prototypes is null");
                return new WhenIn<>(prototypes, cases);
            }

            @Override
            public <T> WhenType<T, R> whenType(Class<T> type) {
                Objects.requireNonNull(type, "type is null");
                return new WhenType<>(type, cases);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> WhenTypeIn<T, R> whenTypeIn(Class<T>... types) {
                Objects.requireNonNull(types, "types is null");
                return new WhenTypeIn<>(types, cases);
            }

            @Override
            public <T> WhenApplicable<T, R> whenApplicable(Function1<? super T, ? extends R> function) {
                Objects.requireNonNull(function, "f is null");
                return new WhenApplicable<>(function, cases);
            }

            public Otherwise<R> otherwise(R that) {
                return new Otherwise<>(ignored -> that, cases);
            }

            public Otherwise<R> otherwise(Function<? super Object, ? extends R> function) {
                return new Otherwise<>(function, cases);
            }

            public Otherwise<R> otherwise(Supplier<? extends R> supplier) {
                return new Otherwise<>(ignored -> supplier.get(), cases);
            }
        }

        final class Otherwise<R> implements Match<R> {

            private final Function<? super Object, ? extends R> function;
            private final List<Case<R>> cases;

            private Otherwise(Function<? super Object, ? extends R> function, List<Case<R>> cases) {
                this.function = function;
                this.cases = cases;
            }

            @Override
            public R apply(Object o) {
                return cases
                        .reverse()
                        .findFirst(caze -> caze.isApplicable(o))
                        .map(caze -> caze.apply(o))
                        .orElseGet(() -> function.apply(o));
            }
        }

        final class Case<R> {

            private final Function<Object, Boolean> matchBy;

            private final Function<Object, ? extends R> f;

            private Case(Function<Object, Boolean> matchBy, Function<Object, ? extends R> f) {
                this.matchBy = matchBy;
                this.f = f;
            }

            @SuppressWarnings("unchecked")
            private static <T, R> Case<R> byType(Class<T> type, Function<? super T, ? extends R> function) {
                return new Case<>(MATCH_BY_TYPE.apply(type), (Function<Object, ? extends R>) function);
            }

            @SuppressWarnings("unchecked")
            private static <T, R> Case<R> byValue(T value, Function<? super T, ? extends R> function) {
                return new Case<>(MATCH_BY_VALUE.apply(value), (Function<Object, ? extends R>) function);
            }

            @SuppressWarnings("unchecked")
            private static <T, R> Case<R> byPredicate(Predicate<? super T> predicate, Function<? super T, ? extends R> function) {
                final Function1<? super T, Boolean> liftedPredicate = t -> predicate.test(t);
                return new Case<>(liftedPredicate::isApplicableTo, (Function<Object, ? extends R>) function);
            }

            @SuppressWarnings("unchecked")
            private static <T, R> Case<R> byFunction(Function1<? super T, ? extends R> function) {
                return new Case<>(function::isApplicableTo, (Function<Object, ? extends R>) function);
            }

            boolean isApplicable(Object o) {
                return matchBy.apply(o);
            }

            R apply(Object o) {
                return f.apply(o);
            }
        }
    }

    /**
     * @since 1.9.9
     */
    interface MatchMonad<R> extends TraversableOnce<R>, Value<R> {

        interface MatchMonadWithWhen<T, R> extends MatchMonad<R>, WithWhen<T, R> {

            Otherwise<R> otherwise(R that);

            Otherwise<R> otherwise(Supplier<? extends R> supplier);

            Otherwise<R> otherwise(Function<? super T, ? extends R> function);
        }

        interface WithWhenUntyped<T> {

            <U> WhenUntyped<T, U> when(U prototype);

            <U> WhenUntyped<T, U> when(Predicate<? super U> predicate);

            @SuppressWarnings("unchecked")
            <U> WhenUntyped<T, U> whenIn(U... prototypes);

            <U> WhenUntyped<T, U> whenType(Class<U> type);

            @SuppressWarnings("unchecked")
            <U> WhenUntyped<T, U> whenTypeIn(Class<? extends U>... type);

            <U, R> WithWhen.When<T, U, R> whenApplicable(Function<? super U, ? extends R> function);

            interface WhenUntyped<T, U> {

                <R> MatchMonadWithWhen<T, R> then(Function<? super U, ? extends R> function);

                default <R> MatchMonadWithWhen<T, R> then(R that) {
                    return then(ignored -> that);
                }

                default <R> MatchMonadWithWhen<T, R> then(Supplier<? extends R> supplier) {
                    Objects.requireNonNull(supplier, "supplier is null");
                    return then(ignored -> supplier.get());
                }
            }
        }

        interface WithWhen<T, R> {

            <U> When<T, U, R> when(U prototype);

            <U> When<T, U, R> when(Predicate<? super U> predicate);

            @SuppressWarnings("unchecked")
            <U> When<T, U, R> whenIn(U... prototypes);

            <U> When<T, U, R> whenType(Class<U> type);

            @SuppressWarnings("unchecked")
            <U> When<T, U, R> whenTypeIn(Class<? extends U>... type);

            <U> When<T, U, R> whenApplicable(Function1<? super U, ? extends R> function);

            interface When<T, U, R> {

                MatchMonadWithWhen<T, R> then(Function<? super U, ? extends R> function);

                default MatchMonadWithWhen<T, R> then(R that) {
                    return then(ignored -> that);
                }

                default MatchMonadWithWhen<T, R> then(Supplier<? extends R> supplier) {
                    Objects.requireNonNull(supplier, "supplier is null");
                    return then(ignored -> supplier.get());
                }
            }
        }

        // -- filter monadic operations

        // TODO: MatchMonad<R> filter(Predicate<? super R> predicate);

        <U> MatchMonad<U> flatMap(Function<? super R, ? extends MatchMonad<U>> mapper);

        <U> MatchMonad<U> flatten(Function<? super R, ? extends MatchMonad<U>> f);

        <U> MatchMonad<U> map(Function<? super R, ? extends U> mapper);

        // TODO: MatchMonad<R> peek(Consumer<? super R> action);

        final class Of<T> implements WithWhenUntyped<T> {

            private final T value;

            private Of(T value) {
                this.value = value;
            }

            public <R> Typed<T, R> as(Class<R> resultType) {
                Objects.requireNonNull(resultType, "resultType is null");
                return new Typed<>(value);
            }

            @Override
            public <U> WhenUnmatchedUntyped<T, U> when(U prototype) {
                final boolean isMatching = MATCH_BY_VALUE.apply(prototype, value);
                return new WhenUnmatchedUntyped<>(value, isMatching);
            }

            @Override
            public <U> WhenUnmatchedUntyped<T, U> when(Predicate<? super U> predicate) {
                Objects.requireNonNull(predicate, "predicate is null");
                final Function1<? super U, Boolean> liftedPredicate = u -> predicate.test(u);
                final boolean isMatching = liftedPredicate.isApplicableTo(value);
                return new WhenUnmatchedUntyped<>(value, isMatching);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> WhenUnmatchedUntyped<T, U> whenIn(U... prototypes) {
                Objects.requireNonNull(prototypes, "prototypes is null");
                final boolean isMatching = List.of(prototypes).findFirst(p -> MATCH_BY_VALUE.apply(p, value)).isDefined();
                return new WhenUnmatchedUntyped<>(value, isMatching);
            }

            @Override
            public <U> WhenUnmatchedUntyped<T, U> whenType(Class<U> type) {
                Objects.requireNonNull(type, "type is null");
                final boolean isMatching = MATCH_BY_TYPE.apply(type, value);
                return new WhenUnmatchedUntyped<>(value, isMatching);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> WhenUnmatchedUntyped<T, U> whenTypeIn(Class<? extends U>... types) {
                Objects.requireNonNull(types, "types is null");
                final boolean isMatching = List.of(types).findFirst(type -> MATCH_BY_TYPE.apply(type, value)).isDefined();
                return new WhenUnmatchedUntyped<>(value, isMatching);
            }

            @Override
            public <U, R> WhenUnmatched<T, U, R> whenApplicable(Function<? super U, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                final Function1<? super U, ? extends R> liftedFunction = u -> function.apply(u);
                final boolean isMatching = liftedFunction.isApplicableTo(value);
                return new WhenUnmatched<>(value, isMatching);
            }
        }

        final class Typed<T, R> implements WithWhen<T, R> {

            private final T value;

            private Typed(T value) {
                this.value = value;
            }

            @Override
            public <U> WhenUnmatched<T, U, R> when(U prototype) {
                final boolean isMatching = MATCH_BY_VALUE.apply(prototype, value);
                return new WhenUnmatched<>(value, isMatching);
            }

            @Override
            public <U> WhenUnmatched<T, U, R> when(Predicate<? super U> predicate) {
                Objects.requireNonNull(predicate, "predicate is null");
                final Function1<? super U, Boolean> liftedPredicate = u -> predicate.test(u);
                final boolean isMatching = liftedPredicate.isApplicableTo(value);
                return new WhenUnmatched<>(value, isMatching);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> WhenUnmatched<T, U, R> whenIn(U... prototypes) {
                Objects.requireNonNull(prototypes, "prototypes is null");
                final boolean isMatching = List.of(prototypes).findFirst(p -> MATCH_BY_VALUE.apply(p, value)).isDefined();
                return new WhenUnmatched<>(value, isMatching);
            }

            @Override
            public <U> WhenUnmatched<T, U, R> whenType(Class<U> type) {
                Objects.requireNonNull(type, "type is null");
                final boolean isMatching = MATCH_BY_TYPE.apply(type, value);
                return new WhenUnmatched<>(value, isMatching);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> WhenUnmatched<T, U, R> whenTypeIn(Class<? extends U>... types) {
                Objects.requireNonNull(types, "types is null");
                final boolean isMatching = List.of(types).findFirst(type -> MATCH_BY_TYPE.apply(type, value)).isDefined();
                return new WhenUnmatched<>(value, isMatching);
            }

            @Override
            public <U> WhenUnmatched<T, U, R> whenApplicable(Function1<? super U, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                final Function1<? super U, ? extends R> liftedFunction = u -> function.apply(u);
                final boolean isMatching = liftedFunction.isApplicableTo(value);
                return new WhenUnmatched<>(value, isMatching);
            }
        }

        final class WhenUnmatchedUntyped<T, U> implements WithWhenUntyped.WhenUntyped<T, U> {

            private final Object value;
            private final boolean isMatching;

            private WhenUnmatchedUntyped(Object value, boolean isMatching) {
                this.value = value;
                this.isMatching = isMatching;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> MatchMonadWithWhen<T, R> then(Function<? super U, ? extends R> function) {
                return isMatching ? new MatchedWithWhen<>(function.apply((U) value)) : new Unmatched<>((T) value);
            }
        }

        final class WhenUnmatched<T, U, R> implements WithWhen.When<T, U, R> {

            private final Object value;
            private final boolean isMatching;

            private WhenUnmatched(Object value, boolean isMatching) {
                this.value = value;
                this.isMatching = isMatching;
            }

            @SuppressWarnings("unchecked")
            @Override
            public MatchMonadWithWhen<T, R> then(Function<? super U, ? extends R> function) {
                return isMatching ? new MatchedWithWhen<>(function.apply((U) value)) : new Unmatched<>((T) value);
            }
        }

        final class WhenMatched<T, U, R> implements WithWhen.When<T, U, R> {

            private final MatchedWithWhen<T, R> matched;

            private WhenMatched(MatchedWithWhen<T, R> matched) {
                this.matched = matched;
            }

            @Override
            public MatchedWithWhen<T, R> then(Function<? super U, ? extends R> function) {
                return matched;
            }
        }

        final class MatchedWithWhen<T, R> implements MatchMonadWithWhen<T, R> {

            private final R result;
            private final Lazy<WhenMatched<T, ?, R>> when;

            private MatchedWithWhen(R result) {
                this.result = result;
                this.when = Lazy.of(() -> new WhenMatched<>(this));
            }

            // -- getters

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public R get() {
                return result;
            }

            // -- when cases

            @SuppressWarnings("unchecked")
            @Override
            public <U> When<T, U, R> when(U prototype) {
                return (When<T, U, R>) when.get();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> When<T, U, R> when(Predicate<? super U> predicate) {
                Objects.requireNonNull(predicate, "predicate is null");
                return (When<T, U, R>) when.get();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> When<T, U, R> whenIn(U... prototypes) {
                Objects.requireNonNull(prototypes, "prototypes is null");
                return (When<T, U, R>) when.get();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> When<T, U, R> whenType(Class<U> type) {
                Objects.requireNonNull(type, "type is null");
                return (When<T, U, R>) when.get();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> When<T, U, R> whenTypeIn(Class<? extends U>... types) {
                Objects.requireNonNull(types, "types is null");
                return (When<T, U, R>) when.get();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> When<T, U, R> whenApplicable(Function1<? super U, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                return (When<T, U, R>) when.get();
            }

            @Override
            public Otherwise<R> otherwise(R that) {
                return new Otherwise<>(that);
            }

            @Override
            public Otherwise<R> otherwise(Supplier<? extends R> supplier) {
                return new Otherwise<>(result);
            }

            @Override
            public Otherwise<R> otherwise(Function<? super T, ? extends R> function) {
                return new Otherwise<>(result);
            }

            // -- filter monadic operations

            @Override
            public <U> MatchMonad<U> flatMap(Function<? super R, ? extends MatchMonad<U>> mapper) {
                return mapper.apply(result);
            }

            @Override
            public <U> MatchMonad<U> flatten(Function<? super R, ? extends MatchMonad<U>> f) {
                return f.apply(result);
            }

            @Override
            public <U> MatchedWithWhen<T, U> map(Function<? super R, ? extends U> mapper) {
                return new MatchedWithWhen<>(mapper.apply(result));
            }

            // -- traversable once

            @Override
            public Iterator<R> iterator() {
                return Collections.singleton(result).iterator();
            }
        }

        final class Unmatched<T, R> implements MatchMonadWithWhen<T, R> {

            private final T value;

            private Unmatched(T value) {
                this.value = value;
            }

            // -- getters

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public R get() {
                throw new MatchError(value);
            }

            // -- when cases

            @Override
            public <U> WhenUnmatched<T, U, R> when(U prototype) {
                final boolean isMatching = MATCH_BY_VALUE.apply(prototype, value);
                return new WhenUnmatched<>(value, isMatching);
            }

            @Override
            public <U> When<T, U, R> when(Predicate<? super U> predicate) {
                Objects.requireNonNull(predicate, "predicate is null");
                final boolean isMatching = MATCH_BY_PREDICATE.apply(predicate, value);
                return new WhenUnmatched<>(value, isMatching);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> WhenUnmatched<T, U, R> whenIn(U... prototypes) {
                Objects.requireNonNull(prototypes, "prototypes is null");
                final boolean isMatching = List.of(prototypes).findFirst(p -> MATCH_BY_VALUE.apply(p, value)).isDefined();
                return new WhenUnmatched<>(value, isMatching);
            }

            @Override
            public <U> WhenUnmatched<T, U, R> whenType(Class<U> type) {
                Objects.requireNonNull(type, "type is null");
                final boolean isMatching = MATCH_BY_TYPE.apply(type, value);
                return new WhenUnmatched<>(value, isMatching);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> WhenUnmatched<T, U, R> whenTypeIn(Class<? extends U>... types) {
                Objects.requireNonNull(types, "types is null");
                final boolean isMatching = List.of(types).findFirst(type -> MATCH_BY_TYPE.apply(type, value)).isDefined();
                return new WhenUnmatched<>(value, isMatching);
            }

            @Override
            public <U> WhenUnmatched<T, U, R> whenApplicable(Function1<? super U, ? extends R> function) {
                Objects.requireNonNull(function, "function is null");
                final boolean isMatching = MATCH_BY_FUNCTION.apply(function, value);
                return new WhenUnmatched<>(value, isMatching);
            }

            @Override
            public Otherwise<R> otherwise(R that) {
                return new Otherwise<>(that);
            }

            @Override
            public Otherwise<R> otherwise(Supplier<? extends R> supplier) {
                return new Otherwise<>(supplier.get());
            }

            @Override
            public Otherwise<R> otherwise(Function<? super T, ? extends R> function) {
                return new Otherwise<>(function.apply(value));
            }

            // -- filter monadic operations

            @SuppressWarnings("unchecked")
            @Override
            public <U> MatchMonad<U> flatMap(Function<? super R, ? extends MatchMonad<U>> mapper) {
                return (MatchMonad<U>) this;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> MatchMonad<U> flatten(Function<? super R, ? extends MatchMonad<U>> f) {
                return (MatchMonad<U>) this;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <U> Unmatched<T, U> map(Function<? super R, ? extends U> mapper) {
                return (Unmatched<T, U>) this;
            }

            // -- traversable once

            @Override
            public Iterator<R> iterator() {
                return Collections.emptyIterator();
            }
        }

        final class Otherwise<R> implements MatchMonad<R> {

            private final R result;

            private Otherwise(R result) {
                this.result = result;
            }

            // -- getters

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public R get() {
                return result;
            }

            // -- filter monadic operations

            @Override
            public <U> MatchMonad<U> flatMap(Function<? super R, ? extends MatchMonad<U>> mapper) {
                return mapper.apply(result);
            }

            @Override
            public <U> MatchMonad<U> flatten(Function<? super R, ? extends MatchMonad<U>> f) {
                return f.apply(result);
            }

            @Override
            public <U> Otherwise<U> map(Function<? super R, ? extends U> mapper) {
                return new Otherwise<>(mapper.apply(result));
            }

            // -- traversable once

            @Override
            public Iterator<R> iterator() {
                return Collections.singleton(result).iterator();
            }
        }
    }
}
