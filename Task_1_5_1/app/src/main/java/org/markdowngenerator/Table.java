package org.markdowngenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Table extends Element {

    public static final String SEPARATOR = "|";
    public static final String WHITESPACE = " ";
    public static final String DEFAULT_TRIMMING_INDICATOR = "~";
    public static final int DEFAULT_MINIMUM_COLUMN_WIDTH = 3;

    // for text
    public enum Alignment {
        ALIGN_LEFT,
        ALIGN_CENTER,
        ALIGN_RIGHT
    }

    public static final Alignment ALIGN_LEFT = Alignment.ALIGN_LEFT;
    public static final Alignment ALIGN_CENTER = Alignment.ALIGN_CENTER;
    public static final Alignment ALIGN_RIGHT = Alignment.ALIGN_RIGHT;

    private List<TableRow> rows;
    private List<Alignment> alignments;
    private boolean firstRowIsHeader;
    private int minimumColumnWidth = DEFAULT_MINIMUM_COLUMN_WIDTH;
    private String trimmingIndicator = DEFAULT_TRIMMING_INDICATOR;

    
    public Table() {
        this.rows = new ArrayList<>();
        this.alignments = new ArrayList<>();
        firstRowIsHeader = true;
    }

    public Table(List<TableRow> rows) {
        this();
        this.rows = rows;
    }

    public Table(List<TableRow> rows, List<Alignment> alignments) {
        this(rows);
        this.alignments = alignments;
    }

    
    public static class Builder implements org.markdowngenerator.Builder {
        private Table table;
        private int rowLimit;

        public Builder() {
            table = new Table();
        }

        public Builder withRows(List<TableRow> tableRows) {
            table.setRows(tableRows);
            return this;
        }

        public Builder addRow(TableRow tableRow) {
            table.getRows().add(tableRow);
            return this;
        }

        public Builder addRow(Object... objects) {
            TableRow tableRow = new TableRow(Arrays.asList(objects));
            table.getRows().add(tableRow);
            return this;
        }

        public Builder withAlignments(List<Alignment> alignments) {
            table.setAlignments(alignments);
            return this;
        }

        public Builder withAlignments(Alignment... alignments) {
            return withAlignments(Arrays.asList(alignments));
        }

        public Builder withAlignment(Alignment alignment) {
            return withAlignments(Collections.singletonList(alignment));
        }

        public Builder withRowLimit(int rowLimit) {
            this.rowLimit = rowLimit;
            return this;
        }

        public Builder withTrimmingIndicator(String trimmingIndicator) {
            table.setTrimmingIndicator(trimmingIndicator);
            return this;
        }

        public Table build() {
            if (rowLimit > 0) {
                table.trim(rowLimit);
            }
            return table;
        }
    }
    
    @Override
    public String serialize() {
        Map<Integer, Integer> columnWidths = getColumnWidths(rows, minimumColumnWidth);

        StringBuilder sb = new StringBuilder();

        String headerSeparator = generateHeaderSeparator(columnWidths, alignments);
        boolean headerSeperatorAdded = !firstRowIsHeader;
        if (!firstRowIsHeader) {
            sb.append(headerSeparator).append(System.lineSeparator());
        }

        for (TableRow row : rows) {
            for (int columnIndex = 0; columnIndex < columnWidths.size(); columnIndex++) {
                sb.append(SEPARATOR);

                String value = "";
                if (row.getColumns().size() > columnIndex) {
                    Object valueObject = row.getColumns().get(columnIndex);
                    if (valueObject != null) {
                        value = valueObject.toString();
                    }
                }

                if (value.equals(trimmingIndicator)) {
                    value = StringUtil.fillUpLeftAligned(value, trimmingIndicator, columnWidths.get(columnIndex));
                    value = StringUtil.surroundValueWith(value, WHITESPACE);
                } else {
                    Alignment alignment = getAlignment(alignments, columnIndex);
                    value = StringUtil.surroundValueWith(value, WHITESPACE);
                    value = StringUtil.fillUpAligned(value, WHITESPACE, columnWidths.get(columnIndex) + 2, alignment);
                }

                sb.append(value);

                if (columnIndex == row.getColumns().size() - 1) {
                    sb.append(SEPARATOR);
                }
            }

            if (rows.indexOf(row) < rows.size() - 1 || rows.size() == 1) {
                sb.append(System.lineSeparator());
            }

            if (!headerSeperatorAdded) {
                sb.append(headerSeparator).append(System.lineSeparator());
                headerSeperatorAdded = true;
            }
        }
        return sb.toString();
    }


    public Table trim(int rowsToKeep) {
        rows = trim(this, rowsToKeep, trimmingIndicator).getRows();
        return this;
    }


    public static Table trim(Table table, int rowsToKeep, String trimmingIndicator) {
        if (table.getRows().size() <= rowsToKeep) {
            return table;
        }

        int trimmedEntriesCount = table.getRows().size() - (table.getRows().size() - rowsToKeep);
        int trimmingStartIndex = Math.round(trimmedEntriesCount / 2) + 1;
        int trimmingStopIndex = table.getRows().size() - trimmingStartIndex;

        List<TableRow> trimmedRows = new ArrayList<>();
        for (int i = trimmingStartIndex; i <= trimmingStopIndex; i++) {
            trimmedRows.add(table.getRows().get(i));
        }

        table.getRows().removeAll(trimmedRows);

        TableRow trimmingIndicatorRow = new TableRow();
        for (int columnIndex = 0; columnIndex < table.getRows().get(0).getColumns().size(); columnIndex++) {
            trimmingIndicatorRow.getColumns().add(trimmingIndicator);
        }
        table.getRows().add(trimmingStartIndex, trimmingIndicatorRow);

        return table;
    }

    public static String generateHeaderSeparator(Map<Integer, Integer> columnWidths, List<Alignment> alignments) {
        StringBuilder sb = new StringBuilder();
        for (int columnIndex = 0; columnIndex < columnWidths.entrySet().size(); columnIndex++) {
            sb.append(SEPARATOR);

            String value = StringUtil.fillUpLeftAligned("", "-", columnWidths.get(columnIndex));

            Alignment alignment = getAlignment(alignments, columnIndex);
            switch (alignment) {
                case ALIGN_RIGHT: {
                    value = WHITESPACE + value + ":";
                    break;
                }
                case ALIGN_CENTER: {
                    value = ":" + value + ":";
                    break;
                }
                default: {
                    value = StringUtil.surroundValueWith(value, WHITESPACE);
                    break;
                }
            }

            sb.append(value);
            if (columnIndex == columnWidths.entrySet().size() - 1) {
                sb.append(SEPARATOR);
            }
        }
        return sb.toString();
    }

    public static Map<Integer, Integer> getColumnWidths(List<TableRow> rows, int minimumColumnWidth) {
        Map<Integer, Integer> columnWidths = new HashMap<Integer, Integer>();
        if (rows.isEmpty()) {
            return columnWidths;
        }
        for (int columnIndex = 0; columnIndex < rows.get(0).getColumns().size(); columnIndex++) {
            columnWidths.put(columnIndex, getMaximumItemLength(rows, columnIndex, minimumColumnWidth));
        }
        return columnWidths;
    }

    public static int getMaximumItemLength(List<TableRow> rows, int columnIndex, int minimumColumnWidth) {
        int maximum = minimumColumnWidth;
        for (TableRow row : rows) {
            if (row.getColumns().size() < columnIndex + 1) {
                continue;
            }
            Object value = row.getColumns().get(columnIndex);
            if (value == null) {
                continue;
            }
            maximum = Math.max(value.toString().length(), maximum);
        }
        return maximum;
    }

    public static Alignment getAlignment(List<Alignment> alignments, int columnIndex) {
        if (alignments.isEmpty()) {
            return ALIGN_LEFT;
        }
        if (columnIndex >= alignments.size()) {
            columnIndex = alignments.size() - 1;
        }
        return alignments.get(columnIndex);
    }

    public List<TableRow> getRows() {
        return rows;
    }

    public void setRows(List<TableRow> rows) {
        this.rows = rows;
    }

    public List<Alignment> getAlignments() {
        return alignments;
    }

    public void setAlignments(List<Alignment> alignments) {
        this.alignments = alignments;
    }

    public boolean isFirstRowHeader() {
        return firstRowIsHeader;
    }

    public void useFirstRowAsHeader(boolean firstRowIsHeader) {
        this.firstRowIsHeader = firstRowIsHeader;
    }

    public int getMinimumColumnWidth() {
        return minimumColumnWidth;
    }

    public void setMinimumColumnWidth(int minimumColumnWidth) {
        this.minimumColumnWidth = minimumColumnWidth;
    }

    public String getTrimmingIndicator() {
        return trimmingIndicator;
    }

    public void setTrimmingIndicator(String trimmingIndicator) {
        this.trimmingIndicator = trimmingIndicator;
    }

  

    public boolean equals (Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof Table) {
            Table other = (Table) obj;
            return this.toString().equals(other.toString());
        }

        return false;
    }

}