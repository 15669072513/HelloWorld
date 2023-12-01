/*
 * Ant Group
 * Copyright (c) 2004-2023 All Rights Reserved.
 */
package com.antgroup.oss.gitprepushhook;

import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author khotyn
 * @version AddContentCollectDiffFormatter.java, v 0.1 2023年03月14日 14:00 khotyn
 */
public class AddContentCollectDiffFormatter extends DiffFormatter {
    private final SortedMap<Integer, String> addedLines = new ConcurrentSkipListMap<>();
    private final SortedMap<Integer, String> allChangeLines = new ConcurrentSkipListMap<>();

    /**
     * Create a new formatter with a default level of context.
     *
     * @param out the stream the formatter will write line data to. This stream
     *            should have buffering arranged by the caller, as many small
     *            writes are performed to it.
     */
    public AddContentCollectDiffFormatter(OutputStream out) {
        super(out);
    }

    @Override
    protected void writeAddedLine(RawText text, int line) throws IOException {
        super.writeAddedLine(text, line);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        text.writeLine(baos, line);
        addedLines.put(line, baos.toString());
        allChangeLines.put(line, baos.toString());
    }

    @Override
    protected void writeRemovedLine(RawText text, int line) throws IOException {
        super.writeRemovedLine(text, line);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        text.writeLine(baos, line);
        allChangeLines.put(line, baos.toString());
    }

    public SortedMap<Integer, String> getAddedLines() {
        return addedLines;
    }

    public SortedMap<Integer, String> getAllChangeLines() {
        return allChangeLines;
    }
}