package uk.ac.ebi.biosamples.service;

import org.springframework.core.convert.converter.Converter;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;
import uk.ac.ebi.biosamples.model.structured.amr.AmrPair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class AmrTableConverter implements Converter<AMRTable, List<List<String>>> {

    @Override
    public List<List<String>> convert(AMRTable amrTable) {

        HtmlAmrTable htmlAmrTable = new HtmlAmrTable();
//        List<List<String>> amrTableList = new ArrayList<>();
//
//        List<String> tableHeaderSorted = Arrays.asList("Species", "Antibiotic Name", "Ast Standard", "Breakpoint Version",
//                "Laboratory Typing Method", "Measurement Value", "Measurement Units", "Measurement Sign",
//                "Resistance Phenotype", "Platform", "Laboratory Typing Platform");
//        amrTableList.add(tableHeaderSorted);

//        List<Method> methods = Arrays.stream(AMREntry.class.getDeclaredMethods())
//                .filter(m -> htmlAmrTable.headers.getFieldFromMethod(m) )
//                .collect(Collectors.toList());

        for(AMREntry amrEntry: amrTable.getStructuredData()) {
            Map<String, String> amrTableRow = new HashMap<>();
            for ( String h: htmlAmrTable.headers ) {
                String methodName = getMethodFromHeader(h);
                String value = "";
                try {
                    Object returnValue = AMREntry.class.getDeclaredMethod(methodName).invoke(amrEntry);

                    if(returnValue instanceof AmrPair) {
                        AmrPair amrPair = (AmrPair) returnValue;
                        value = amrPair.getValue();
                    } else {
                        value = (String) returnValue;
                    }

                    if (value != null && !value.isEmpty()) {
                        amrTableRow.put(h, value);
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }

            htmlAmrTable.addRow(amrTableRow);
        }


        return htmlAmrTable.getCompleteTable();

    }

    private String getFieldFromMethod(String methodName) {
        return methodName
                .replaceFirst("get", "")
                .replaceAll(
                    String.format("%s|%s|%s",
                            "(?<=[A-Z])(?=[A-Z][a-z])",
                            "(?<=[^A-Z])(?=[A-Z])",
                            "(?<=[A-Za-z])(?=[^A-Za-z])"
                    ),
                    " "
            );
    }

    private String getMethodFromHeader(String header) {
        String[] methodParts = header.toLowerCase().split(" ");
        return "get" + Arrays.stream(methodParts).map( p -> p.substring(0,1).toUpperCase() + p.substring(1) ).collect(Collectors.joining());

    }

    public class HtmlAmrTable {
        public final String[] headers = {
                "Species", "Antibiotic Name", "Ast Standard", "Breakpoint Version",
                "Laboratory Typing Method", "Measurement", "Measurement Units", "Measurement Sign",
                "Resistance Phenotype", "Platform"
        };

        private final List<List<String>> content;
        private final Map<String, Boolean> columnElementCount;

        public HtmlAmrTable() {
            this.columnElementCount = new TreeMap<>();
            Arrays.stream(headers).forEachOrdered(k -> columnElementCount.put(k, false));
            this.content = new ArrayList<>();
        }

        public void addRow(Map<String, String> amrEntry) {
            List<String> row = new ArrayList<>();
            for(String head: headers) {
                String amrValue = amrEntry.get(head);
                if (amrValue != null) {
                    this.columnElementCount.put(head, Boolean.TRUE);
                    row.add(amrEntry.get(head));
                }

            }
            this.content.add(row);
        }

        public List<List<String>> getCompleteTable() {
            List<String> headerList = Arrays.stream(headers).filter(
                    this.columnElementCount::get
            ).collect(Collectors.toList());

            List<List<String>> finalContent = new ArrayList<>();
            finalContent.add(headerList);
            finalContent.addAll(this.content);
            return finalContent;
        }
    }


}
