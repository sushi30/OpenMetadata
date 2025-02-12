package org.openmetadata.service.governance.workflows;

import lombok.Getter;
import org.openmetadata.schema.governance.workflows.WorkflowDefinition;
import org.openmetadata.service.governance.workflows.flowable.MainWorkflow;
import org.openmetadata.service.governance.workflows.flowable.TriggerWorkflow;

@Getter
public class Workflow {
  public static final String RELATED_ENTITY_VARIABLE = "relatedEntity";
  public static final String RESULT_VARIABLE = "result";
  public static final String UPDATED_BY_VARIABLE = "updatedBy";
  public static final String STAGE_INSTANCE_STATE_ID_VARIABLE = "stageInstanceStateId";
  public static final String WORKFLOW_INSTANCE_EXECUTION_ID_VARIABLE =
      "workflowInstanceExecutionId";
  public static final String WORKFLOW_RUNTIME_EXCEPTION = "workflowRuntimeException";
  public static final String EXCEPTION_VARIABLE = "exception";
  public static final String GLOBAL_NAMESPACE = "global";
  private final TriggerWorkflow triggerWorkflow;
  private final MainWorkflow mainWorkflow;

  public Workflow(WorkflowDefinition workflowDefinition) {
    this.triggerWorkflow = new TriggerWorkflow(workflowDefinition);
    this.mainWorkflow = new MainWorkflow(workflowDefinition);
  }

  public static String getFlowableElementId(String parentName, String elementName) {
    return String.format("%s.%s", parentName, elementName);
  }
}
