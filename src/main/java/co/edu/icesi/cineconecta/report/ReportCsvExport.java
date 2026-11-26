package co.edu.icesi.cineconecta.report;

import java.math.BigDecimal;
import java.util.List;

/** Helpers comunes para exportar CSV que se abre de forma segura en hojas de cálculo. */
final class ReportCsvExport {
    private ReportCsvExport() {}

    static String row(Object... values) {
        StringBuilder row = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                row.append(',');
            }
            Object value = values[index];
            String cell = value == null ? "" : format(value);
            if (!(value instanceof Number) && startsWithFormula(cell)) {
                cell = "'" + cell;
            }
            row.append('"').append(cell.replace("\"", "\"\"")).append('"');
        }
        return row.toString();
    }

    static String document(List<String> rows) {
        return "\uFEFF" + String.join("\r\n", rows) + "\r\n";
    }

    private static String format(Object value) {
        return value instanceof BigDecimal decimal ? decimal.toPlainString() : value.toString();
    }

    private static boolean startsWithFormula(String value) {
        int index = 0;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index < value.length() && "=+-@".indexOf(value.charAt(index)) >= 0;
    }
}
