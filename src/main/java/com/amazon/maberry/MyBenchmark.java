/*
 * Copyright (c) 2019 Amazon.com and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Amazon designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Amazon in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package com.amazon.maberry;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@Fork()
public class MyBenchmark {

    private static final String URL = "https://raw.githubusercontent.com/corretto/amazon-corretto-crypto-provider/master/etc/amazon-corretto-crypto-provider.security";

    @Benchmark
    public void testMethod(MyBenchmark state) throws InterruptedException {
        Thread.sleep(1000);
    }

    public static void main(String[] args) throws RunnerException, IOException {

        // Download the java.security.properties file from Corretto
        URL securityFileUrl = new URL(URL);
        Path securityFilePath = Files.createTempFile("amazon-corretto-crypto-provider.",".security").toAbsolutePath();

        // Java Util Logging configuration file
        Path julConfigPath = Files.createTempFile("jul.", ".properties").toAbsolutePath();

        try (
                ReadableByteChannel readableByteChannel = Channels.newChannel(securityFileUrl.openStream());
                FileChannel writableByteChannel = FileChannel.open(securityFilePath, WRITE, TRUNCATE_EXISTING);
                BufferedWriter writer = Files.newBufferedWriter(julConfigPath)
        ) {
            writableByteChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            writableByteChannel.close();

            // Up until July the log level for the offending message was INFO, since then one must lower the level to CONFIG
            writer.write("handlers=java.util.logging.ConsoleHandler\n");
            writer.write("java.util.logging.ConsoleHandler.level=CONFIG\n");
            writer.write("AmazonCorrettoCryptoProvider.level=CONFIG\n");
            writer.flush();

            new Runner(new OptionsBuilder()
                    .include(MyBenchmark.class.getSimpleName())
                    .jvmArgs("-Djava.security.properties=" + securityFilePath,
                             "-Djava.util.logging.config.file=" + julConfigPath)
                    .verbosity(VerboseMode.EXTRA)
                    .build())
                    .run();

        } finally {
            Files.deleteIfExists(securityFilePath);
            Files.deleteIfExists(julConfigPath);
        }
    }

}
