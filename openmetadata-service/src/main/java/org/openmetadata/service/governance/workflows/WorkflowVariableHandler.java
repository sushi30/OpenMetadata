package org.openmetadata.service.governance.workflows;

import static org.openmetadata.service.governance.workflows.Workflow.GLOBAL_NAMESPACE;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.variable.api.delegate.VariableScope;

@Slf4j
public class WorkflowVariableHandler {
  private final VariableScope varScope;

  public WorkflowVariableHandler(VariableScope varScope) {
    this.varScope = varScope;
  }

  public static String getNamespacedVariableName(String namespace, String varName) {
    if (namespace != null) {
      return String.format("%s_%s", namespace, varName);
    } else {
      return null;
    }
  }

  public Object getNamespacedVariable(String namespace, String varName) {
    String namespacedVarName = getNamespacedVariableName(namespace, varName);
    if (namespacedVarName != null) {
      return varScope.getVariable(namespacedVarName);
    } else {
      return null;
    }
  }

  public void setNamespacedVariable(String namespace, String varName, Object varValue) {
    String namespacedVarName = getNamespacedVariableName(namespace, varName);
    if (namespacedVarName != null) {
      varScope.setVariable(namespacedVarName, varValue);
      LOG.debug(String.format("%s variable set to %s", namespacedVarName, varValue));
    } else {
      throw new RuntimeException("Namespace can't be null when setting a namespaced variable.");
    }
  }

  public void setGlobalVariable(String varName, Object varValue) {
    setNamespacedVariable(GLOBAL_NAMESPACE, varName, varValue);
  }

  private String getNodeNamespace() {
    if (varScope instanceof DelegateExecution) {
      return ((DelegateExecution) varScope).getParent().getCurrentActivityId();
    } else if (varScope instanceof DelegateTask) {
      return WorkflowHandler.getInstance()
          .getParentActivityId(((DelegateTask) varScope).getExecutionId());
    } else {
      throw new RuntimeException(
          "varScope must be either an instance of 'DelegateExecution' or 'DelegateTask'.");
    }
  }

  public void setNodeVariable(String varName, Object varValue) {
    String namespace = getNodeNamespace();
    setNamespacedVariable(namespace, varName, varValue);
  }
}
