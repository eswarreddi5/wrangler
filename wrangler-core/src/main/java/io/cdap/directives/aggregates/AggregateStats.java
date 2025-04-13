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

import com.google.common.collect.ImmutableList;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.List;

/**
 * Aggregates total or average byte size and time duration across rows and outputs them into new columns.
 */
@Plugin(type = Directive.TYPE)
@Name(AggregateStats.NAME)
@Categories(categories = {"data-aggregation"})
@Description("Aggregates total or average byte size and time duration across rows.")
public class AggregateStats implements Directive {
    public static final String NAME = "aggregate-stats";

    private String inputSizeColumn;
    private String inputTimeColumn;
    private String outputSizeColumn;
    private String outputTimeColumn;
    private String sizeUnit = "MB";
    private String timeUnit = "s";
    private String aggregationType = "total";

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
        builder.define("inputSizeColumn", TokenType.COLUMN_NAME);
        builder.define("inputTimeColumn", TokenType.COLUMN_NAME);
        builder.define("outputSizeColumn", TokenType.COLUMN_NAME);
        builder.define("outputTimeColumn", TokenType.COLUMN_NAME);
        builder.define("sizeUnit", TokenType.IDENTIFIER, true);
        builder.define("timeUnit", TokenType.IDENTIFIER, true);
        builder.define("aggregationType", TokenType.IDENTIFIER, true);
        return builder.build();
    }

    @Override
    public void initialize(Arguments arguments) throws DirectiveParseException {
        this.inputSizeColumn = ((ColumnName) arguments.value("inputSizeColumn")).value();
        this.inputTimeColumn = ((ColumnName) arguments.value("inputTimeColumn")).value();
        this.outputSizeColumn = ((ColumnName) arguments.value("outputSizeColumn")).value();
        this.outputTimeColumn = ((ColumnName) arguments.value("outputTimeColumn")).value();

        if (((Text) arguments.value("sizeUnit")).value() != null) {
            this.sizeUnit = ((Text) arguments.value("sizeUnit")).value().toUpperCase();
        }
        if (arguments.value("timeUnit").value() != null) {
            this.timeUnit = arguments.value("timeUnit").value().toString().toLowerCase();
        }
        if (arguments.value("aggregateType").value() != null) {
            this.aggregationType = arguments.value("aggregateType").value().toString().toLowerCase();
        }
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        try {
            long totalBytes = 0;
            long totalNanos = 0;

            for (Row row : rows) {
                Object sizeVal = row.getValue("data_size");
                Object timeVal = row.getValue("response_time");
                if (sizeVal instanceof ByteSize) {
                    totalBytes += ((ByteSize) sizeVal).getBytes();
                }

                if (timeVal instanceof TimeDuration) {
                    totalNanos += ((TimeDuration) timeVal).getNanoseconds();
                }
            }

            if (aggregationType.equalsIgnoreCase("average") && totalBytes > 0 && totalNanos > 0) {
                totalBytes = totalBytes / rows.size();
                totalNanos = totalNanos / rows.size();
            }

            String totalBytesValue = String.format("%s%s", totalBytes, sizeUnit);
            String totalNanosValue = String.format("%s%s", totalNanos, timeUnit);

            Row result = new Row();
            result.add("final_size", totalBytesValue);
            result.add("final_time", totalNanosValue);

            return ImmutableList.of(result);
        } catch (Exception e) {
            throw new DirectiveExecutionException(NAME, e.getMessage(), e);
        }
    }

    public Schema getOutputSchema() {
        Schema.Field outputSizeField = Schema.Field.of(outputSizeColumn, Schema.of(Schema.Type.STRING));
        Schema.Field outputTimeField = Schema.Field.of(outputTimeColumn, Schema.of(Schema.Type.STRING));
        return Schema.recordOf(NAME, outputSizeField, outputTimeField);
    }

    @Override
    public void destroy() {
        // no-op
    }
}
