package com.example.springbootactiviti.common.util;

import com.alibaba.fastjson.JSONObject;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivitiUtil {
    private static ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
    private static RuntimeService runtimeService = processEngine.getRuntimeService();
    private static TaskService taskService = processEngine.getTaskService();
    private static RepositoryService repositoryService = processEngine.getRepositoryService();
    private static HistoryService historyService = processEngine.getHistoryService();
    private static IdentityService identityService = processEngine.getIdentityService();

    /**
    * 启动流程
    * @author      dengzhili
     * @param processTemplate   流程模板名称
     * @param starter   流程启动人
     * @param processInstanceName   流程实例名称
     * @param parameter 参数
     * @exception
    * @date        2019/6/25 0025 下午 2:10
    */
    public static ProcessInstance startProcess(String processTemplate, String starter, String processInstanceName, Map<String, Object> parameter) {
        Map<String, Object> map = new HashMap<>();// 参数
        map.put("user", starter);
        // 将变量存入map中
        if (parameter != null) {
            parameter.forEach((key, value) -> {
                map.put(key, value);
            });
        }
        identityService.setAuthenticatedUserId(starter);// 设置流程发起人START_USER_ID_
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processTemplate, map);
        runtimeService.setProcessInstanceName(processInstance.getProcessInstanceId(), processInstanceName);
        return processInstance;
    }

    /**
    *  执行任务
    * @author      dengzhili
     * @param taskId   任务实例id
     * @param parameter 参数
    * @exception
    * @date        2019/6/25 0025 下午 2:14
    */
    public static Boolean next(String taskId, Map<String, Object> parameter) {
        if (parameter == null) {
            taskService.complete(taskId);
        } else {
            taskService.complete(taskId, parameter);
        }
        return true;
    }

    /**
     *  执行任务
     * @author      dengzhili
     * @param taskId    任务实例id
     * @param parameter 参数
     * @param opinion   处理意见
     * @exception
     * @date        2019/6/25 0025 下午 2:14
     */
    public static Boolean nextAndComment(String taskId, Map<String, Object> parameter, String opinion) {
        Boolean flag = true;
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            identityService.setAuthenticatedUserId(task.getAssignee());// 设置意见填写人
            taskService.addComment(taskId, task.getProcessInstanceId(), opinion);
            if (parameter == null) {
                taskService.complete(taskId);
            } else {
                taskService.complete(taskId, parameter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        } finally {
            return flag;
        }
    }

    /**
     * 启动流程并执行下一步
     * @author      dengzhili
     * @param processTemplate   流程模板名称
     * @param starter   流程启动人
     * @param processInstanceName   流程实例名称
     * @param parameter 下一节点参数
     * @exception
     * @date        2019/6/25 0025 下午 2:10
     */
    public static String startAndDoNext(String processTemplate, String starter, String processInstanceName, Map<String, Object> parameter) {
        ProcessInstance processInstance = startProcess(processTemplate, starter, processInstanceName, null);
        List<Task> list = getTaskInstenceByPIID(processInstance.getProcessInstanceId());
        next(list.get(0).getId(), parameter);
        return processInstance.getProcessInstanceId();
    }

    /**
    *  认领任务(申领)
    * @author      dengzhili
     * @param taskId    任务实例id
     * @param userId    申领人id
    * @exception
    * @date        2019/6/25 0025 下午 2:15
    */
    public static JSONObject claim(String taskId, String userId) {
        JSONObject result = new JSONObject();
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task != null) {
                if (task.getAssignee() == null || task.getAssignee().isEmpty()) {
                    // 进行申领
                    taskService.claim(taskId, userId);
                    result.put("status", 200);
                    result.put("message", "申领成功");
                } else if (task.getAssignee().equals(userId)) {
                    result.put("status", 200);
                    result.put("message", "节点已申领");
                } else {
                    result.put("status", -1);
                    result.put("message", "节点已被他人申领");
                }
            } else {
                // 判断流程节点是否存在
                List<HistoricIdentityLink> list = historyService.getHistoricIdentityLinksForTask(taskId);
                if (list != null && list.size() > 0) {
                    result.put("status", -1);
                    result.put("message", "节点已流转");
                }
            }
        } catch (ActivitiObjectNotFoundException e) {
            e.printStackTrace();
            result.put("status", -1);
            result.put("message", "节点不存在");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", -1);
            result.put("message", "程序异常");
        } finally {
            return result;
        }
    }

    /**
    * 终止流程实例(终止流程)
    * @author      dengzhili
     * @param taskId 任务实例id
    * @exception
    * @date        2019/6/25 0025 下午 2:18
    */
    public static Boolean deleteProcessInstance(String taskId) {
        Boolean flag = true;
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "终止流程");
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        } finally {
            return flag;
        }
    }

    /**
    *  保存审核批注
    * @author      dengzhili
     * @param taskId    任务实例id
     * @param comment   批注
    * @exception
    * @date        2019/6/25 0025 下午 2:19
    */
    public static Boolean addComment(String taskId, String comment) {
        Boolean flag = true;
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            identityService.setAuthenticatedUserId(task.getAssignee());// 设置意见填写人
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        } finally {
            return flag;
        }
    }

    /**
    *  挂起流程实例
    * @author      dengzhili
     * @param piid  流程实例id
    * @exception
    * @date        2019/6/25 0025 下午 2:22
    */
    public static Boolean suspendProcessInstance(String piid) {
        Boolean flag = true;
        try {
            runtimeService.suspendProcessInstanceById(piid);
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        } finally {
            return flag;
        }
    }

    /**
    *  流程实例解除挂起
    * @author      dengzhili
     * @param piid  流程实例id
    * @exception
    * @date        2019/6/25 0025 下午 2:25
    */
    public static Boolean activateProcessInstance(String piid) {
        Boolean flag = true;
        try {
            runtimeService.activateProcessInstanceById(piid);
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        } finally {
            return flag;
        }
    }

    /**
    *  activiti节点跳转
    * @author      dengzhili
     * @param processDefinitionId   流程定义id
     * @param executionId   执行id
     * @param activityId    当前节点活动id
     * @param destinationActivityId 目标节点活动id
    * @exception
    * @date        2019/6/28 0028 下午 4:12
    */
    public static Boolean jump(String processDefinitionId, String executionId, String activityId, String destinationActivityId) {
        Boolean flag = true;
        try {
            Map<String, Object> params = new HashMap<>();
            ReadOnlyProcessDefinition processDefinitionEntity = (ReadOnlyProcessDefinition) repositoryService.getProcessDefinition(processDefinitionId);
            // 当前节点
            ActivityImpl currentActivity = (ActivityImpl)processDefinitionEntity.findActivity(activityId);
            // 目标节点
            ActivityImpl destinationActivity = (ActivityImpl) processDefinitionEntity.findActivity(destinationActivityId);

            processEngine.getManagementService().executeCommand(new JDJumpTaskCmd(executionId, destinationActivity, params, currentActivity));
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        } finally {
            return flag;
        }
    }

    /**
     *  activiti节点跳转
     * @author      dengzhili
     * @param taskId   任务实例id
     * @param destinationActivityId 目标节点活动id
     * @exception
     * @date        2019/6/28 0028 下午 4:12
     */
    public static Boolean jump(String taskId, String destinationActivityId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        return jump(task.getProcessDefinitionId(), task.getExecutionId(), task.getTaskDefinitionKey(), destinationActivityId);
    }

    /**
    * activiti活动回退至上一节点(1、第一个节点、并行网关节点、包括网关节点不能回退；2、节点只有一个上级节点时直接回退；3、节点有多个上级节点时，回退到最新任务实例节点)
    * @author      dengzhili
     * @param taskId    任务实例id
    * @exception
    * @date        2019/6/28 0028 下午 4:18
    */
    public static JSONObject regression(String taskId) {
        JSONObject result = new JSONObject();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        // 流程定义id
        String processDefinitionId = task.getProcessDefinitionId();
        // 当前节点
        String activityId = task.getTaskDefinitionKey();
        // 流程定义及走向信息
        ProcessDefinitionEntity def = (ProcessDefinitionEntity) ((RepositoryServiceImpl)repositoryService).getDeployedProcessDefinition(processDefinitionId);
        List<ActivityImpl> activitiList = def.getActivities();

        // 判断是否为不可回退节点（第一节点、并行网关接节点）
        for(ActivityImpl activityImpl:activitiList){
            if("startEvent".equals(activityImpl.getProperty("type"))) {
                List<PvmTransition> outTransitions = activityImpl.getOutgoingTransitions();//获取从某个节点出来的所有线路
                for(PvmTransition tr:outTransitions){
                    PvmActivity ac = tr.getDestination(); //获取线路的终点节点
                    if (activityId.equals(ac.getId())) {
                        // 当前节点为第一个节点，不能回退
                        result.put("status", 201);
                        result.put("message", "第一个节点，不能回退");
                        return result;
                    }
                }
            }
            if ("parallelGateway".equals(activityImpl.getProperty("type")) || "inclusiveGateway".equals(activityImpl.getProperty("type"))) {
                List<PvmTransition> outTransitions = activityImpl.getOutgoingTransitions();//获取从某个节点出来的所有线路
                for(PvmTransition tr:outTransitions){
                    PvmActivity ac = tr.getDestination(); //获取线路的终点节点
                    if (activityId.equals(ac.getId())) {
                        // 并行网关节点，不能回退
                        result.put("status", 202);
                        result.put("message", "并行网关节点，不能回退");
                        return result;
                    }
                }
            }
        }

        // 执行id
        String executionId = task.getExecutionId();

        // 查询历史节点记录
        List<HistoricActivityInstance> historyList = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .list();

        List<String> returnableNodeId = getReturnableNodeId(activityId, new ArrayList<>(), activitiList);

        if (returnableNodeId.size() == 1) {
            // 只有一个上级节点，直接回退
            if (jump(processDefinitionId, executionId, activityId, returnableNodeId.get(0))) {
                result.put("status", 200);
                result.put("message", "回退成功");
                return result;
            }
        } else if (returnableNodeId.size() > 1) {
            // 有多个上级节点
            String destinationActivityId = null;
            for (int i = historyList.size() - 1; i >= 0; i--) {
                if (returnableNodeId.contains(historyList.get(i).getActivityId())) {
                    // 获取最新上级节点任务，执行回退
                    if (jump(processDefinitionId, executionId, activityId, historyList.get(i).getActivityId())) {
                        result.put("status", 200);
                        result.put("message", "回退成功");
                        return result;
                    }
                }
            }
        }
        result.put("status", 203);
        result.put("message", "回退失败");
        return result;
    }

    /**
    *  判断流程此节点的上级节点（userTask类型节点）
    * @author      dengzhili
     * @param activitiId    当前活动id
     * @param result    返回结果
     * @param activitiList  流程定义信息
    * @exception
    * @date        2019/7/1 0001 下午 5:32
    */
    private static List<String> getReturnableNodeId(String activitiId, List<String> result, List<ActivityImpl> activitiList) {
        for (ActivityImpl activityImpl: activitiList) {
            if("userTask".equals(activityImpl.getProperty("type"))) {
                List<PvmTransition> outTransitions = activityImpl.getOutgoingTransitions();//获取从某个节点出来的所有线路
                for(PvmTransition tr:outTransitions){
                    PvmActivity ac = tr.getDestination(); //获取线路的终点节点
                    if (activitiId.equals(ac.getId())) {
                        result.add(activityImpl.getId());
                    }
                }
            } else {
                List<PvmTransition> outTransitions = activityImpl.getOutgoingTransitions();//获取从某个节点出来的所有线路
                for(PvmTransition tr:outTransitions){
                    PvmActivity ac = tr.getDestination(); //获取线路的终点节点
                    if (activitiId.equals(ac.getId())) {
                        result.addAll(getReturnableNodeId(activityImpl.getId(), new ArrayList<>(), activitiList));
                    }
                }
            }
        }
        return result;
    }

    /**
    * 根据流程实例查询任务实例
    * @author      dengzhili
     * @param piid  流程实例id
    * @exception
    * @date        2019/7/4 0004 上午 10:08
    */
    public static List<Task> getTaskInstenceByPIID(String piid) {
        TaskQuery taskQuery = taskService.createTaskQuery();
        taskQuery.processInstanceId(piid);
        List<Task> list = taskQuery.list();
        return list;
    }

    /**
    * 查询流程实例信息-根据任务实例id
    * @author      dengzhili
     * @param taskId    任务实例id
    * @exception
    * @date        2019/7/8 0008 下午 4:40
    */
    public static JSONObject getProcessInstanceByTaskId(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        ProcessDefinitionEntity def = (ProcessDefinitionEntity) ((RepositoryServiceImpl)repositoryService).getDeployedProcessDefinition(task.getProcessDefinitionId());
        JSONObject result = new JSONObject();
        result.put("proCode", def.getKey());
        result.put("proName", def.getName());
        result.put("actName", task.getName());
        result.put("piid", task.getProcessInstanceId());

        return result;
    }


}
