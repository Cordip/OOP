package org.markdowngenerator;

import java.util.ArrayList;
import java.util.List;

public class TableRow<T extends Object> extends Element{

    private List<T> columns;

    public TableRow() {
        this.columns = new ArrayList<>();
    }

    public TableRow(List<T> columns) {
        this.columns = columns;
    }

    @Override
    public String serialize() throws MarkdownException {
        StringBuilder sb = new StringBuilder();
        for (Object item : columns) {
            if (item == null) {
                throw new MarkdownException("Column is null");
            }
            if (item.toString().contains(Table.SEPARATOR)) {
                throw new MarkdownException("Column contains seperator char \"" + Table.SEPARATOR + "\"");
            }
            sb.append(Table.SEPARATOR);
            sb.append(StringUtil.surroundValueWith(item.toString(), " "));
            if (columns.indexOf(item) == columns.size() - 1) {
                sb.append(Table.SEPARATOR);
            }
        }
        return sb.toString();
    }

    public List<T> getColumns() {
        return columns;
    }

    public void setColumns(List<T> columns) {
        this.columns = columns;
    }
    
    public boolean equals(Object obj) {
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