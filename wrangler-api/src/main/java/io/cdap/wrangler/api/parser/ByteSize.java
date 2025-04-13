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
 * Token implementation for parsing byte size strings like "10MB", "2.5GB", etc.
 */
public class ByteSize implements Token {
    private static final Pattern BYTE_PATTERN = Pattern.compile(
            "([0-9]+(?:\\.[0-9]+)?)\\s*(B|KB|MB|GB|TB|kb|mb|gb|tb)?"
    );

    private final String raw;
    private final long bytes;

    public ByteSize(String raw) {
        this.raw = raw;
        this.bytes = parseBytes(raw);
    }

    private long parseBytes(String input) {
        Matcher matcher = BYTE_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid byte size format: " + input);
        }

        double number = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2) == null ? "B" : matcher.group(2).toUpperCase(Locale.ROOT);

        switch (unit) {
            case "B":  return (long) number;
            case "KB": return (long) (number * 1024);
            case "MB": return (long) (number * 1024 * 1024);
            case "GB": return (long) (number * 1024 * 1024 * 1024);
            case "TB": return (long) (number * 1024L * 1024 * 1024 * 1024);
            default:   throw new IllegalArgumentException("Unsupported byte unit: " + unit);
        }
    }

    /**
     * Returns byte count in canonical unit.
     */
    public long getBytes() {
        return bytes;
    }

    @Override
    public Object value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "BYTE_SIZE");
        obj.addProperty("raw", raw);
        obj.addProperty("bytes", bytes);
        return obj;
    }
}
