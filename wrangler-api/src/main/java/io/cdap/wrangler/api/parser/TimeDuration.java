/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Token implementation for parsing time durations like "150ms", "2.5s", etc.
 */
public class TimeDuration implements Token {
    private static final Pattern TIME_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(ns|us|ms|s|m|h)?");

    private final String raw;
    private final long nanoseconds;

    public TimeDuration(String raw) {
        this.raw = raw;
        this.nanoseconds = parseNanoseconds(raw);
    }

    private long parseNanoseconds(String input) {
        Matcher matcher = TIME_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time duration format: " + input);
        }

        double number = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2) == null ? "ms" : matcher.group(2).toLowerCase(Locale.ROOT);

        switch (unit) {
            case "ns": return (long) number;
            case "us": return (long) (number * 1_000);
            case "ms": return (long) (number * 1_000_000);
            case "s":  return (long) (number * 1_000_000_000);
            case "m":  return (long) (number * 60 * 1_000_000_000L);
            case "h":  return (long) (number * 3600 * 1_000_000_000L);
            default:   throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }

    /**
     * Returns time duration in canonical nanoseconds.
     */
    public long getNanoseconds() {
        return nanoseconds;
    }

    @Override
    public Object value() {
        return nanoseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "TIME_DURATION");
        obj.addProperty("raw", raw);
        obj.addProperty("nanoseconds", nanoseconds);
        return obj;
    }
}
