package com.universalshield.ml.training;

import com.universalshield.ml.LabeledText;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a labeled CSV dataset with header {@code id,label,text} (RFC-4180 quoting).
 * See data/README.md for the schema.
 */
public final class DatasetLoader {

    private DatasetLoader() {
    }

    public static List<LabeledText> load(Path file) throws IOException {
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    static List<LabeledText> parse(String csv) {
        List<List<String>> rows = parseCsv(csv);
        if (rows.isEmpty() || !rows.get(0).equals(List.of("id", "label", "text"))) {
            throw new IllegalArgumentException("Dataset must start with the header: id,label,text");
        }
        List<LabeledText> out = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            if (r.size() == 1 && r.get(0).isBlank()) {
                continue;
            }
            if (r.size() != 3) {
                throw new IllegalArgumentException("Row " + (i + 1) + " must have exactly 3 columns");
            }
            int label;
            try {
                label = Integer.parseInt(r.get(1).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Row " + (i + 1) + " has a non-numeric label");
            }
            out.add(new LabeledText(r.get(0).trim(), label, r.get(2)));
        }
        return out;
    }

    static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
