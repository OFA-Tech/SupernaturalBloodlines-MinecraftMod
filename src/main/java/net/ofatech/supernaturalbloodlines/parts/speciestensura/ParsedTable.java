package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import java.util.List;

/** A config list turned into a usable table, plus the config entries that could not be used. */
public record ParsedTable<T>(T table, List<String> rejected) {}
