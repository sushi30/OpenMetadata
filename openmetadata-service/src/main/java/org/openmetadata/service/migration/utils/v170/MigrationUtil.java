package org.openmetadata.service.migration.utils.v170;

import static org.openmetadata.service.governance.workflows.Workflow.UPDATED_BY_VARIABLE;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openmetadata.schema.governance.workflows.WorkflowDefinition;
import org.openmetadata.schema.governance.workflows.elements.WorkflowNodeDefinitionInterface;
import org.openmetadata.service.Entity;
import org.openmetadata.service.governance.workflows.flowable.MainWorkflow;
import org.openmetadata.service.jdbi3.ListFilter;
import org.openmetadata.service.jdbi3.WorkflowDefinitionRepository;
import org.openmetadata.service.util.EntityUtil;
import org.openmetadata.service.util.JsonUtils;

@Slf4j
public class MigrationUtil {

  @SneakyThrows
  private static void setDefaultInputNamespaceMap(WorkflowNodeDefinitionInterface nodeDefinition) {
    try {
      Class<?> clazz = nodeDefinition.getClass();
      var field = clazz.getDeclaredField("inputNamespaceMap");

      field.setAccessible(true);

      Object fieldValue = field.get(nodeDefinition);

      if (fieldValue == null) {
        Class<?> fieldType = field.getType();

        Object newValue = fieldType.getDeclaredConstructor().newInstance();

        field.set(nodeDefinition, newValue);
      }
    } catch (NoSuchFieldException ignored) {

    }
  }

  @SneakyThrows
  private static void updateInputNamespaceMap(
      WorkflowNodeDefinitionInterface nodeDefinition, Map<String, String> inputNamespaceMap) {
    try {
      Class<?> clazz = nodeDefinition.getClass();
      var field = clazz.getDeclaredField("inputNamespaceMap");
      field.setAccessible(true);
      Object inputNamespaceMapObj = field.get(nodeDefinition);

      if (inputNamespaceMapObj != null) {
        Class<?> fieldType = field.getType();

        Field[] inputNamespaceMapFields = fieldType.getDeclaredFields();

        for (Field inputNamespaceMapField : inputNamespaceMapFields) {
          inputNamespaceMapField.setAccessible(true);
          String fieldName = inputNamespaceMapField.getName();

          if (inputNamespaceMap.containsKey(fieldName)) {
            inputNamespaceMapField.set(inputNamespaceMapObj, inputNamespaceMap.get(fieldName));
          }
        }
      }
    } catch (NoSuchFieldException ignored) {

    }
  }

  public static void updateGovernanceWorkflowDefinitions() {
    WorkflowDefinitionRepository repository =
        (WorkflowDefinitionRepository) Entity.getEntityRepository(Entity.WORKFLOW_DEFINITION);
    List<WorkflowDefinition> workflowDefinitions =
        repository.listAll(EntityUtil.Fields.EMPTY_FIELDS, new ListFilter());

    for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
      MainWorkflow.WorkflowGraph graph = new MainWorkflow.WorkflowGraph(workflowDefinition);

      for (WorkflowNodeDefinitionInterface nodeDefinition : workflowDefinition.getNodes()) {
        setDefaultInputNamespaceMap(nodeDefinition);

        Map<String, String> nodeInputNamespaceMap =
            (Map<String, String>)
                JsonUtils.readOrConvertValue(nodeDefinition.getInputNamespaceMap(), Map.class);

        if (nodeInputNamespaceMap == null) {
          continue;
        }

        if (nodeDefinition.getInput().contains(UPDATED_BY_VARIABLE)
            && nodeInputNamespaceMap.get(UPDATED_BY_VARIABLE) == null) {
          if (graph.getIncomingEdgesMap().containsKey(nodeDefinition.getName())) {
            for (String incomeNodeName :
                graph.getIncomingEdgesMap().get(nodeDefinition.getName())) {
              List<String> incomeNodeOutput = graph.getNodeMap().get(incomeNodeName).getOutput();
              if (incomeNodeOutput != null && incomeNodeOutput.contains(UPDATED_BY_VARIABLE)) {
                nodeInputNamespaceMap.put(UPDATED_BY_VARIABLE, incomeNodeName);
                updateInputNamespaceMap(nodeDefinition, nodeInputNamespaceMap);
                break;
              }
            }
          }
        }
      }
      repository.createOrUpdate(null, workflowDefinition);
    }
  }
}
