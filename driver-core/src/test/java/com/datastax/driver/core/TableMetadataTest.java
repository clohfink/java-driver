/*
 *      Copyright (C) 2012-2015 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.core;

import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import static com.datastax.driver.core.DataType.*;
import static com.datastax.driver.core.DataType.cint;
import static com.datastax.driver.core.DataType.text;
import static com.datastax.driver.core.DataType.timeuuid;
import static com.datastax.driver.core.TableMetadata.Order.ASC;
import static com.datastax.driver.core.TableMetadata.Order.DESC;

public class TableMetadataTest extends CCMBridge.PerClassSingleNodeCluster {

    @Test(groups = "short")
    public void should_parse_table_without_clustering_columns() {
        // given
        String cql = String.format("CREATE TABLE %s.static (\n"
            + "    k text,\n"
            + "    i int,\n"
            + "    m map<text, timeuuid>,\n"
            + "    v int,\n"
            + "    PRIMARY KEY (k)\n"
            + ");", keyspace);
        // when
        session.execute(cql);
        TableMetadata table = cluster.getMetadata().getKeyspace(keyspace).getTable("static");
        // then
        Assertions.assertThat(table).isNotNull().hasName("static").hasNumberOfColumns(4).isNotCompactStorage();
        Assertions.assertThat(table.getColumns().get(0)).isNotNull().hasName("k").isPartitionKey().hasType(text());
        Assertions.assertThat(table.getColumns().get(1)).isNotNull().hasName("i").isRegularColumn().hasType(cint());
        Assertions.assertThat(table.getColumns().get(2)).isNotNull().hasName("m").isRegularColumn().hasType(map(text(), timeuuid()));
        Assertions.assertThat(table.getColumns().get(3)).isNotNull().hasName("v").isRegularColumn().hasType(cint());
    }

    @Test(groups = "short")
    public void should_parse_table_with_clustering_columns() {
        // given
        String cql = String.format("CREATE TABLE %s.sparse (\n"
            + "    k text,\n"
            + "    c1 int,\n"
            + "    c2 float,\n"
            + "    l list<text>,\n"
            + "    v int,\n"
            + "    PRIMARY KEY (k, c1, c2)\n"
            + ");", keyspace);
        // when
        session.execute(cql);
        TableMetadata table = cluster.getMetadata().getKeyspace(keyspace).getTable("sparse");
        // then
        Assertions.assertThat(table).isNotNull().hasName("sparse").hasNumberOfColumns(5).isNotCompactStorage();
        Assertions.assertThat(table.getColumns().get(0)).isNotNull().hasName("k").isPartitionKey().hasType(text());
        Assertions.assertThat(table.getColumns().get(1)).isNotNull().hasName("c1").isClusteringColumn().hasClusteringOrder(ASC).hasType(cint());
        Assertions.assertThat(table.getColumns().get(2)).isNotNull().hasName("c2").isClusteringColumn().hasClusteringOrder(ASC).hasType(cfloat());
        Assertions.assertThat(table.getColumns().get(3)).isNotNull().hasName("l").isRegularColumn().hasType(list(text()));
        Assertions.assertThat(table.getColumns().get(4)).isNotNull().hasName("v").isRegularColumn().hasType(cint());
    }

    @Test(groups = "short")
    public void should_parse_counter_table() {
        // given
        String cql = String.format("CREATE TABLE %s.counters (\n"
            + "    k text,\n"
            + "    c counter,\n"
            + "    PRIMARY KEY (k)\n"
            + ");", keyspace);
        // when
        session.execute(cql);
        TableMetadata table = cluster.getMetadata().getKeyspace(keyspace).getTable("counters");
        // then
        Assertions.assertThat(table).isNotNull().hasName("counters").hasNumberOfColumns(2).isNotCompactStorage();
        Assertions.assertThat(table.getColumns().get(0)).isNotNull().hasName("k").isPartitionKey().hasType(text());
        Assertions.assertThat(table.getColumns().get(1)).isNotNull().hasName("c").isRegularColumn().hasType(counter());
    }

    @Test(groups = "short")
    public void should_parse_compact_static_table() {
        // given
        String cql = String.format("CREATE TABLE %s.compact_static (\n"
            + "    k text,\n"
            + "    i int,\n"
            + "    t timeuuid,\n"
            + "    v int,\n"
            + "    PRIMARY KEY (k)\n"
            + ") WITH COMPACT STORAGE;", keyspace);
        // when
        session.execute(cql);
        TableMetadata table = cluster.getMetadata().getKeyspace(keyspace).getTable("compact_static");
        // then
        VersionNumber version = TestUtils.findHost(cluster, 1).getCassandraVersion();
        // Cassandra 3.0 +
        if (version.getMajor() > 2) {
            Assertions.assertThat(table).isNotNull().hasName("compact_static").hasNumberOfColumns(6).isCompactStorage();
            Assertions.assertThat(table.getColumns().get(0)).isNotNull().hasName("k").isPartitionKey().hasType(text());
            Assertions.assertThat(table.getColumns().get(1)).isNotNull().hasName("column1").isClusteringColumn().hasType(text()).isNotStatic();
            Assertions.assertThat(table.getColumns().get(2)).isNotNull().hasName("i").isRegularColumn().hasType(cint()).isStatic();
            Assertions.assertThat(table.getColumns().get(3)).isNotNull().hasName("t").isRegularColumn().hasType(timeuuid()).isStatic();
            Assertions.assertThat(table.getColumns().get(4)).isNotNull().hasName("v").isRegularColumn().hasType(cint()).isStatic();
            Assertions.assertThat(table.getColumns().get(5)).isNotNull().hasName("value").isRegularColumn().hasType(blob()).isNotStatic();
        } else {
            Assertions.assertThat(table).isNotNull().hasName("compact_static").hasNumberOfColumns(4).isCompactStorage();
            Assertions.assertThat(table.getColumns().get(0)).isNotNull().hasName("k").isPartitionKey().hasType(text());
            Assertions.assertThat(table.getColumns().get(1)).isNotNull().hasName("i").isRegularColumn().hasType(cint());
            Assertions.assertThat(table.getColumns().get(2)).isNotNull().hasName("t").isRegularColumn().hasType(timeuuid());
            Assertions.assertThat(table.getColumns().get(3)).isNotNull().hasName("v").isRegularColumn().hasType(cint());
        }
    }

    @Test(groups = "short")
    public void should_parse_compact_dynamic_table() {
        // given
        String cql = String.format("CREATE TABLE %s.compact_dynamic (\n"
            + "    k text,\n"
            + "    c int,\n"
            + "    v timeuuid,\n"
            + "    PRIMARY KEY (k, c)\n"
            + ") WITH COMPACT STORAGE;", keyspace);
        // when
        session.execute(cql);
        TableMetadata table = cluster.getMetadata().getKeyspace(keyspace).getTable("compact_dynamic");
        // then
        VersionNumber version = TestUtils.findHost(cluster, 1).getCassandraVersion();
        // Cassandra 3.0 +
        Assertions.assertThat(table).isNotNull().hasName("compact_dynamic").hasNumberOfColumns(3).isCompactStorage();
        Assertions.assertThat(table.getColumns().get(0)).isNotNull().hasName("k").isPartitionKey().hasType(text());
        Assertions.assertThat(table.getColumns().get(1)).isNotNull().hasName("c").isClusteringColumn().hasClusteringOrder(ASC).hasType(cint());
        Assertions.assertThat(table.getColumns().get(2)).isNotNull().hasName("v").isRegularColumn().hasType(timeuuid());
    }

    @Test(groups = "short")
    public void should_parse_compact_table_with_multiple_clustering_columns() {
        // given
        String cql = String.format("CREATE TABLE %s.compact_composite (\n"
            + "    k text,\n"
            + "    c1 int,\n"
            + "    c2 float,\n"
            + "    c3 double,\n"
            + "    v timeuuid,\n"
            + "    PRIMARY KEY (k, c1, c2, c3)\n"
            + ") WITH COMPACT STORAGE;", keyspace);
        // when
        session.execute(cql);
        TableMetadata table = cluster.getMetadata().getKeyspace(keyspace).getTable("compact_composite");
        // then
        Assertions.assertThat(table).isNotNull().hasName("compact_composite").hasNumberOfColumns(5).isCompactStorage();
        Assertions.assertThat(table.getColumns().get(0)).isNotNull().hasName("k").isPartitionKey().hasType(text());
        Assertions.assertThat(table.getColumns().get(1)).isNotNull().hasName("c1").isClusteringColumn().hasClusteringOrder(ASC).hasType(cint());
        Assertions.assertThat(table.getColumns().get(2)).isNotNull().hasName("c2").isClusteringColumn().hasClusteringOrder(ASC).hasType(cfloat());
        Assertions.assertThat(table.getColumns().get(3)).isNotNull().hasName("c3").isClusteringColumn().hasClusteringOrder(ASC).hasType(cdouble());
        Assertions.assertThat(table.getColumns().get(4)).isNotNull().hasName("v").isRegularColumn().hasType(timeuuid());
    }

    @Test(groups = "short")
    public void should_parse_table_options() {
        // given
        String cql = String.format("CREATE TABLE %s.with_options (\n"
                + "    k text,\n"
                + "    c1 int,\n"
                + "    c2 int,\n"
                + "    i int,\n"
                + "    PRIMARY KEY (k, c1, c2)\n"
                + ") WITH CLUSTERING ORDER BY (c1 DESC, c2 ASC)\n"
                + "   AND read_repair_chance = 0.5\n"
                + "   AND dclocal_read_repair_chance = 0.6\n"
                + "   AND replicate_on_write = true\n"
                + "   AND gc_grace_seconds = 42\n"
                + "   AND bloom_filter_fp_chance = 0.01\n"
                + "   AND caching = 'ALL'\n"
                + "   AND comment = 'My awesome table'\n"
                + "   AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy', 'sstable_size_in_mb' : 15 }\n"
                + "   AND compression = { 'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor', 'chunk_length_kb' : 128 };",
            keyspace);
        // when
        session.execute(cql);
        TableMetadata table = cluster.getMetadata().getKeyspace(keyspace).getTable("with_options");
        // then
        Assertions.assertThat(table).isNotNull().hasName("with_options").hasNumberOfColumns(4).isNotCompactStorage();
        Assertions.assertThat(table.getColumns().get(0)).isNotNull().hasName("k").isPartitionKey().hasType(text());
        Assertions.assertThat(table.getColumns().get(1)).isNotNull().hasName("c1").isClusteringColumn().hasClusteringOrder(DESC).hasType(cint());
        Assertions.assertThat(table.getColumns().get(2)).isNotNull().hasName("c2").isClusteringColumn().hasClusteringOrder(ASC).hasType(cint());
        Assertions.assertThat(table.getColumns().get(3)).isNotNull().hasName("i").isRegularColumn().hasType(cint());
        Assertions.assertThat(table);
        VersionNumber version = TestUtils.findHost(cluster, 1).getCassandraVersion();
        // Cassandra 3.0 +
        if (version.getMajor() > 2) {

            assertThat(table.getOptions().getReadRepairChance()).isEqualTo(0.5);
            assertThat(table.getOptions().getLocalReadRepairChance()).isEqualTo(0.6);
            assertThat(table.getOptions().getGcGraceInSeconds()).isEqualTo(42);
            assertThat(table.getOptions().getBloomFilterFalsePositiveChance()).isEqualTo(0.01);
            assertThat(table.getOptions().getComment()).isEqualTo("My awesome table");
            assertThat(table.getOptions().getCaching()).contains(entry("keys", "ALL" ));
            assertThat(table.getOptions().getCaching()).contains(entry("rows_per_partition", "ALL"));
            assertThat(table.getOptions().getCompaction()).contains(entry("class", "org.apache.cassandra.db.compaction.LeveledCompactionStrategy"));
            assertThat(table.getOptions().getCompaction()).contains(entry("sstable_size_in_mb", "15"));
            assertThat(table.getOptions().getCompression()).contains(entry("class", "org.apache.cassandra.io.compress.SnappyCompressor")); // sstable_compression becomes class
            assertThat(table.getOptions().getCompression()).contains(entry("chunk_length_in_kb", "128")); // note the "in" prefix
            assertThat(table.getOptions().getDefaultTimeToLive()).isEqualTo(0);
            assertThat(table.getOptions().getSpeculativeRetry()).isEqualTo("99.0PERCENTILE");
            assertThat(table.getOptions().getIndexInterval()).isNull();
            assertThat(table.getOptions().getMinIndexInterval()).isEqualTo(128);
            assertThat(table.getOptions().getMaxIndexInterval()).isEqualTo(2048);
            assertThat(table.getOptions().getReplicateOnWrite()).isTrue(); // default
            assertThat(table.asCQLQuery())
                .contains("read_repair_chance = 0.5")
                .contains("dclocal_read_repair_chance = 0.6")
                .contains("gc_grace_seconds = 42")
                .contains("bloom_filter_fp_chance = 0.01")
                .contains("comment = 'My awesome table'")
                .contains("'keys' : 'ALL'")
                .contains("'rows_per_partition' : 'ALL'")
                .contains("'class' : 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'")
                .contains("'sstable_size_in_mb' : 15")
                .contains("'class' : 'org.apache.cassandra.io.compress.SnappyCompressor'") // sstable_compression becomes class
                .contains("'chunk_length_in_kb' : 128") // note the "in" prefix
                .contains("default_time_to_live = 0")
                .contains("speculative_retry = '99.0PERCENTILE'")
                .contains("min_index_interval = 128")
                .contains("max_index_interval = 2048")
                .doesNotContain(" index_interval")
                .doesNotContain("replicate_on_write");

            // Cassandra 2.1 and 2.2
        } else if (version.getMajor() == 2 && version.getMinor() > 0) {

            // With 2.1 we have different options, the caching option changes and replicate_on_write disappears
            assertThat(table.getOptions().getReadRepairChance()).isEqualTo(0.5);
            assertThat(table.getOptions().getLocalReadRepairChance()).isEqualTo(0.6);
            assertThat(table.getOptions().getGcGraceInSeconds()).isEqualTo(42);
            assertThat(table.getOptions().getBloomFilterFalsePositiveChance()).isEqualTo(0.01);
            assertThat(table.getOptions().getComment()).isEqualTo("My awesome table");
            assertThat(table.getOptions().getCaching()).contains(entry("keys", "ALL" ));
            assertThat(table.getOptions().getCaching()).contains(entry("rows_per_partition", "ALL"));
            assertThat(table.getOptions().getCompaction()).contains(entry("class", "org.apache.cassandra.db.compaction.LeveledCompactionStrategy"));
            assertThat(table.getOptions().getCompaction()).contains(entry("sstable_size_in_mb", "15"));
            assertThat(table.getOptions().getCompression()).contains(entry("sstable_compression", "org.apache.cassandra.io.compress.SnappyCompressor"));
            assertThat(table.getOptions().getCompression()).contains(entry("chunk_length_kb", "128"));
            assertThat(table.getOptions().getDefaultTimeToLive()).isEqualTo(0);
            assertThat(table.getOptions().getSpeculativeRetry()).isEqualTo("99.0PERCENTILE");
            assertThat(table.getOptions().getIndexInterval()).isNull();
            assertThat(table.getOptions().getMinIndexInterval()).isEqualTo(128);
            assertThat(table.getOptions().getMaxIndexInterval()).isEqualTo(2048);
            assertThat(table.getOptions().getReplicateOnWrite()).isTrue(); // default
            assertThat(table.asCQLQuery())
                .contains("read_repair_chance = 0.5")
                .contains("dclocal_read_repair_chance = 0.6")
                .contains("gc_grace_seconds = 42")
                .contains("bloom_filter_fp_chance = 0.01")
                .contains("comment = 'My awesome table'")
                .contains("'keys' : 'ALL'")
                .contains("'rows_per_partition' : 'ALL'")
                .contains("'class' : 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'")
                .contains("'sstable_size_in_mb' : 15")
                .contains("'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'")
                .contains("'chunk_length_kb' : 128")
                .contains("default_time_to_live = 0")
                .contains("speculative_retry = '99.0PERCENTILE'")
                .contains("min_index_interval = 128")
                .contains("max_index_interval = 2048")
                .doesNotContain(" index_interval")
                .doesNotContain("replicate_on_write");

            // Cassandra 2.0
        } else if (version.getMajor() == 2 && version.getMinor() == 0) {

            assertThat(table.getOptions().getReadRepairChance()).isEqualTo(0.5);
            assertThat(table.getOptions().getLocalReadRepairChance()).isEqualTo(0.6);
            assertThat(table.getOptions().getGcGraceInSeconds()).isEqualTo(42);
            assertThat(table.getOptions().getBloomFilterFalsePositiveChance()).isEqualTo(0.01);
            assertThat(table.getOptions().getComment()).isEqualTo("My awesome table");
            assertThat(table.getOptions().getCaching()).contains(entry("keys", "ALL" ));
            assertThat(table.getOptions().getCaching()).doesNotContain(entry("rows_per_partition", "ALL")); // 2.1 +
            assertThat(table.getOptions().getCompaction()).contains(entry("class", "org.apache.cassandra.db.compaction.LeveledCompactionStrategy"));
            assertThat(table.getOptions().getCompaction()).contains(entry("sstable_size_in_mb", "15"));
            assertThat(table.getOptions().getCompression()).contains(entry("sstable_compression", "org.apache.cassandra.io.compress.SnappyCompressor"));
            assertThat(table.getOptions().getCompression()).contains(entry("chunk_length_kb", "128"));
            assertThat(table.getOptions().getDefaultTimeToLive()).isEqualTo(0);
            assertThat(table.getOptions().getSpeculativeRetry()).isEqualTo("99.0PERCENTILE"); // default
            assertThat(table.getOptions().getIndexInterval()).isEqualTo(128);
            assertThat(table.getOptions().getMinIndexInterval()).isNull();
            assertThat(table.getOptions().getMaxIndexInterval()).isNull();
            assertThat(table.getOptions().getReplicateOnWrite()).isTrue(); // explicitly set
            assertThat(table.asCQLQuery())
                .contains("read_repair_chance = 0.5")
                .contains("dclocal_read_repair_chance = 0.6")
                .contains("gc_grace_seconds = 42")
                .contains("bloom_filter_fp_chance = 0.01")
                .contains("comment = 'My awesome table'")
                .contains("caching = 'ALL'")
                .contains("'class' : 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'")
                .contains("'sstable_size_in_mb' : 15")
                .contains("'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'")
                .contains("'chunk_length_kb' : 128")
                .contains("replicate_on_write = true")
                .contains("index_interval = 128")
                .contains("speculative_retry = '99.0PERCENTILE'")
                .contains("default_time_to_live = 0")
                .doesNotContain("min_index_interval") // 2.1 +
                .doesNotContain("max_index_interval"); // 2.1 +

            // Cassandra 1.2
        } else {

            assertThat(table.getOptions().getReadRepairChance()).isEqualTo(0.5);
            assertThat(table.getOptions().getLocalReadRepairChance()).isEqualTo(0.6);
            assertThat(table.getOptions().getGcGraceInSeconds()).isEqualTo(42);
            assertThat(table.getOptions().getBloomFilterFalsePositiveChance()).isEqualTo(0.01);
            assertThat(table.getOptions().getComment()).isEqualTo("My awesome table");
            assertThat(table.getOptions().getCaching()).contains(entry("keys", "ALL" ));
            assertThat(table.getOptions().getCaching()).doesNotContain(entry("rows_per_partition", "ALL")); // 2.1 +
            assertThat(table.getOptions().getCompaction()).contains(entry("class", "org.apache.cassandra.db.compaction.LeveledCompactionStrategy"));
            assertThat(table.getOptions().getCompaction()).contains(entry("sstable_size_in_mb", "15"));
            assertThat(table.getOptions().getCompression()).contains(entry("sstable_compression", "org.apache.cassandra.io.compress.SnappyCompressor"));
            assertThat(table.getOptions().getCompression()).contains(entry("chunk_length_kb", "128"));
            assertThat(table.getOptions().getDefaultTimeToLive()).isEqualTo(0); // default
            assertThat(table.getOptions().getSpeculativeRetry()).isEqualTo("NONE"); // default
            assertThat(table.getOptions().getIndexInterval()).isNull();
            assertThat(table.getOptions().getMinIndexInterval()).isNull();
            assertThat(table.getOptions().getMaxIndexInterval()).isNull();
            assertThat(table.getOptions().getReplicateOnWrite()).isTrue(); // explicitly set
            assertThat(table.asCQLQuery())
                .contains("read_repair_chance = 0.5")
                .contains("dclocal_read_repair_chance = 0.6")
                .contains("gc_grace_seconds = 42")
                .contains("bloom_filter_fp_chance = 0.01")
                .contains("comment = 'My awesome table'")
                .contains("caching = 'ALL'")
                .contains("'class' : 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'")
                .contains("'sstable_size_in_mb' : 15")
                .contains("'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'")
                .contains("'chunk_length_kb' : 128")
                .contains("replicate_on_write = true")
                .doesNotContain("index_interval")  // 2.0
                .doesNotContain("min_index_interval")  // 2.1 +
                .doesNotContain("max_index_interval")  // 2.1 +
                .doesNotContain("speculative_retry")  // 2.0 +
                .doesNotContain("default_time_to_live"); // 2.0 +

        }

    }

    @Test(groups = "short")
    public void should_escape_single_quote_table_comment() {
        // given
        String cql = String.format("CREATE TABLE %s.single_quote (\n"
            + "    c1 int PRIMARY KEY\n"
            + ") WITH  comment = 'comment with single quote '' should work'",
            keyspace);
        // when
        session.execute(cql);
        TableMetadata table = cluster.getMetadata().getKeyspace(keyspace).getTable("single_quote");
        // then
        assertThat(table.getOptions().getComment()).isEqualTo("comment with single quote ' should work");
        assertThat(table.asCQLQuery()).contains("comment = 'comment with single quote '' should work'");
    }

    @Override
    protected Collection<String> getTableDefinitions() {
        return Collections.emptyList();
    }

}
