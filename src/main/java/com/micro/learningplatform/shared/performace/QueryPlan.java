package com.micro.learningplatform.shared.performace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.micro.learningplatform.shared.exceptions.QueryPlanParseException;
import com.micro.learningplatform.shared.exceptions.QueryValidationParseException;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record QueryPlan(
        JsonNode planNode,
        double totalCost,
        double actualTime,
        List<String> tables,
        Map<String, String> scanTypes
) {


    private static final String NODE_TYPE = "Node Type";
    private static final String RELATION_NAME = "Relation Name";


    public boolean hasSequenceScan() {
        return scanTypes.containsValue("Seq Scan");
    }

    // Provjera za ugniježđene petlje
    public boolean hasNestedLoops() {
        return planNode.findValues(NODE_TYPE).stream()
                .anyMatch(node -> "Nested Loop".equals(node.asText()));
    }

    // Dohvaća tablice koje koriste sekvencijalno skeniranje
    public List<String> getTablesWithSequentialScans() {
        return scanTypes.entrySet().stream()
                .filter(entry -> "Seq Scan".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    // Dohvaća tablice uključene u Nested Loops
    public List<String> getTablesWithNestedLoops() {
        return planNode.findValues("Plans").stream()
                .filter(plan -> "Nested Loop".equals(plan.findPath(NODE_TYPE).asText()))
                .map(plan -> plan.findPath(RELATION_NAME).asText())
                .filter(relationName -> !relationName.isEmpty())
                .toList();
    }

    // Kreira QueryPlan iz JSON-a
    public static QueryPlan fromJson(String jsonPlan) throws QueryPlanParseException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonPlan);
            validateJsonNode(root);

            // Provjera ako je root polje
            JsonNode planNode = root.isArray() ? root.get(0).get("Plan") : root.get("Plan");
            if (planNode == null || planNode.isEmpty()) {
                throw new QueryValidationParseException("Plan node not found in JSON response.");
            }

            double totalCost = planNode.findPath("Total Cost").asDouble();
            double actualTime = planNode.findPath("Actual Total Time").asDouble();

            Map<String, String> scanTypes = new HashMap<>();
            List<String> tables = extractTablesAndScansWithStreams(planNode, scanTypes);


            return new QueryPlan(planNode, totalCost, actualTime, tables, scanTypes);
        } catch (Exception e) {
            throw new QueryPlanParseException("Failed to parse query plan", e);
        }
    }


    // Validacija JSON strukture
    private static void validateJsonNode(JsonNode root) {
        if (root == null || root.isEmpty()) {
            throw new QueryValidationParseException("Invalid query plan JSON: Root node is missing or empty");
        }
    }

    private static List<String> extractTablesAndScansWithStreams(JsonNode planNode, Map<String, String> scanTypes) {
        if (planNode == null || planNode.isEmpty()) {
            return List.of();
        }

        String relationName = Optional.ofNullable(planNode.findPath(RELATION_NAME).asText()).orElse("");
        String nodeType = Optional.ofNullable(planNode.findPath(NODE_TYPE).asText()).orElse("");

        if (!relationName.isEmpty() && !nodeType.isEmpty()) {
            scanTypes.put(relationName, nodeType);
        }

        return Stream.concat(
                Stream.of(relationName).filter(name -> !name.isEmpty()),
                Optional.ofNullable(planNode.get("Plans"))
                        .map(JsonNode::elements)
                        .stream()
                        .flatMap(iter -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED), false)
                        .flatMap(childNode -> extractTablesAndScansWithStreams(childNode, scanTypes).stream())))
                        .distinct().toList();
    }

}
