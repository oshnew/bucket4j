/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j

import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification

import java.time.Duration

class HandlingArithmeticOverflowSpecification extends Specification {

    def "regression test for https://github.com/vladimir-bukhtoyarov/bucket4j/issues/51"() {
        setup:
            Bandwidth limit1 = Bandwidth.simple(700000, Duration.ofHours(1))
            Bandwidth limit2 = Bandwidth.simple(14500, Duration.ofMinutes(1))
            Bandwidth limit3 = Bandwidth.simple(300, Duration.ofSeconds(1))
            TimeMeterMock customTimeMeter = new TimeMeterMock(0)
            long twelveHourNanos = 12 * 60 * 60 * 1_000_000_000L;
            Bucket bucket = Bucket4j.builder()
                .addLimit(limit1)
                .addLimit(limit2)
                .addLimit(limit3)
                .withCustomTimePrecision(customTimeMeter)
                .build()
        when:
            // shift time to 12 hours forward
            customTimeMeter.addTime(twelveHourNanos)
        then:
            bucket.tryConsume(1)
            bucket.tryConsume(300 - 1)
            !bucket.tryConsume(1)
    }

    def "Should check ArithmeticOverflow when add tokens to bucket"() {
        setup:
            Bandwidth limit = Bandwidth.simple(10, Duration.ofSeconds(1))
            TimeMeterMock customTimeMeter = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                .addLimit(9, limit)
                .withCustomTimePrecision(customTimeMeter)
                .build()
        when:
            bucket.addTokens(Long.MAX_VALUE - 1)
        then:
            bucket.tryConsume(10)
            !bucket.tryConsume(1)
    }

    def "Should firstly do refill by completed periods"() {
        setup:
            Bandwidth limit = Bandwidth.simple((long) Long.MAX_VALUE / 16, Duration.ofNanos((long) Long.MAX_VALUE / 8))
            TimeMeterMock meter = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                .addLimit(7, limit)
                .withCustomTimePrecision(meter)
                .build()
        when:
            // emulate time shift which equal of 3 refill periods
            meter.addTime((long) Long.MAX_VALUE / 8 * 3)
        then:
            bucket.tryConsume((long) Long.MAX_VALUE / 16)
            !bucket.tryConsume(1)
    }

    def "Should check ArithmeticOverflow when refilling by completed periods"() {
        setup:
            Bandwidth limit = Bandwidth.classic((long) Long.MAX_VALUE - 10, Refill.smooth(1, Duration.ofNanos(1)))
            TimeMeterMock meter = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                .addLimit((long) Long.MAX_VALUE - 13, limit)
                .withCustomTimePrecision(meter)
                .build()
        when:
            // add time shift enough to overflow
            meter.addTime(20)
        then:
            bucket.tryConsume(Long.MAX_VALUE - 10)
            !bucket.tryConsume(1)
    }

    def "Should down to floating point arithmetic if necessary during refill"() {
        setup:
            Bandwidth limit = Bandwidth.simple((long) Long.MAX_VALUE / 16, Duration.ofNanos((long) Long.MAX_VALUE / 8))
            TimeMeterMock meter = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                .addLimit(0, limit)
                .withCustomTimePrecision(meter)
                .build()
        when:
            // emulate time shift which little bit less then one refill period
            meter.addTime((long) Long.MAX_VALUE / 16 - 1)
        then:
            // should down into floating point arithmetic and successfully refill
            bucket.tryConsume((long) Long.MAX_VALUE / 32)
            bucket.tryConsumeAsMuchAsPossible() == 1
    }

    def "Should check ArithmeticOverflow when refilling by uncompleted periods"() {
        setup:
            Bandwidth limit = Bandwidth.classic((long) Long.MAX_VALUE - 10, Refill.smooth(100, Duration.ofNanos(100)))
            TimeMeterMock meter = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                .addLimit((long) Long.MAX_VALUE - 13, limit)
                .withCustomTimePrecision(meter)
                .build()
        when:
            // add time shift enough to overflow
            meter.addTime(50)
        then:
            bucket.tryConsume(Long.MAX_VALUE - 10)
            !bucket.tryConsume(1)
    }

    def "Should down to floating point arithmetic when having deal with big number during deficit calculation"() {
        setup:
            Bandwidth limit = Bandwidth.simple ((long) Long.MAX_VALUE / 2, Duration.ofNanos((long) Long.MAX_VALUE / 2))
            TimeMeterMock meter = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                .addLimit(0, limit)
                .withCustomTimePrecision(meter)
                .build()
            BucketState state = bucket.createSnapshot()
            Bandwidth[] limits = bucket.configuration.bandwidths

        expect:
            state.delayNanosAfterWillBePossibleToConsume(limits, 10) == 10
    }

}