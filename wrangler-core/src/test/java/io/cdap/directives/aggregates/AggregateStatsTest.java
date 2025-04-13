/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.directives.aggregates;

import com.google.gson.JsonElement;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.Token;
import io.cdap.wrangler.api.parser.TokenType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AggregateStatsTest {

    private Arguments mockArguments() {
        return new Arguments() {
            @Override
            public <T extends Token> T value(String name) {
                switch (name) {
                    case "inputSizeColumn":
                        return (T) new ColumnName("data_size");
                    case "inputTimeColumn":
                        return (T) new ColumnName("response_time");
                    case "outputSizeColumn":
                        return (T) new ColumnName("final_size");
                    case "outputTimeColumn":
                        return (T) new ColumnName("final_time");
                    case "sizeUnit":
                        return (T) new Text("MB");
                    case "timeUnit":
                        return (T) new Text("s");
                    case "aggregateType":
                        return (T) new Text("average");
                }
                return null;
            }

            @Override
            public int size() {
                return 4;
            }

            @Override
            public boolean contains(String name) {
                return true;
            }

            @Override
            public TokenType type(String name) {
                return null;
            }

            @Override
            public int line() {
                return 0;
            }

            @Override
            public int column() {
                return 1;
            }

            @Override
            public String source() {
                return "aggregate-stats :data_size :response_time avg_size avg_time MB s average";
            }

            @Override
            public JsonElement toJson() {
                return null;
            }
        };
    }

    @Test
    public void testAverageAggregation() throws Exception {
        List<Row> inputRows = Arrays.asList(
                new Row("data_size", new ByteSize("2MB")).add("response_time", new TimeDuration("2s")),
                new Row("data_size", new ByteSize("2MB")).add("response_time", new TimeDuration("1s"))
        );

        AggregateStats directive = new AggregateStats();
        directive.initialize(mockArguments());
        List<Row> result = directive.execute(inputRows, null);

        Assert.assertEquals(1, result.size());
        Row out = result.get(0);

        Assert.assertEquals("2097152MB", out.getValue("final_size"));
        Assert.assertEquals("1500000000s", out.getValue("final_time"));
    }

    @Test
    public void testTotalAggregation() throws Exception {
        List<Row> inputRows = Arrays.asList(
                new Row("data_size", new ByteSize("1MB")).add("response_time", new TimeDuration("500ms")),
                new Row("data_size", new ByteSize("512KB")).add("response_time", new TimeDuration("200ms")),
                new Row("data_size", new ByteSize("2MB")).add("response_time", new TimeDuration("1s"))
        );

        AggregateStats directive = new AggregateStats();
        List<Row> result = directive.execute(inputRows, null);

        Assert.assertEquals(1, result.size());
        Row out = result.get(0);

        Assert.assertEquals("3670016MB", out.getValue("final_size"));
        Assert.assertEquals("1700000000s", out.getValue("final_time"));
    }

    @Test
    public void testSkipsInvalidValues() throws Exception {
        List<Row> inputRows = Arrays.asList(
                new Row("data_size", "not-a-byte-size").add("response_time", "not-a-time"),
                new Row("data_size", new ByteSize("1MB")).add("response_time", new TimeDuration("1s"))
        );

        AggregateStats directive = new AggregateStats();
        List<Row> result = directive.execute(inputRows, null);

        Assert.assertEquals(1, result.size());
        Row out = result.get(0);

        Assert.assertEquals("1048576MB", out.getValue("final_size"));
        Assert.assertEquals("1000000000s", out.getValue("final_time"));
    }

    @Test
    public void testHandlesEmptyInput() throws Exception {
        List<Row> inputRows = Collections.emptyList();

        String[] recipe = {
                "aggregate-stats :data_size :response_time total_size total_time MB s total"
        };

        AggregateStats directive = new AggregateStats();
        List<Row> result = directive.execute(inputRows, null);

        Assert.assertEquals(1, result.size());
        Row out = result.get(0);

        Assert.assertEquals("0MB", out.getValue("final_size"));
        Assert.assertEquals("0s", out.getValue("final_time"));
    }
}
