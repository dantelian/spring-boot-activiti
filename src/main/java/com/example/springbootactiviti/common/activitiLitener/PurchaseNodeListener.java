package com.example.springbootactiviti.common.activitiLitener;

import com.example.springbootactiviti.common.util.SpringContextHolder;
import com.example.springbootactiviti.mapper.UserMapper;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.persistence.entity.VariableInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PurchaseNodeListener implements TaskListener {

//    private UserMapper userRoleMapper = SpringContextHolder.getBean(UserMapper.class);

    @Override
    public void notify(DelegateTask delegateTask) {


        String processName = "采购审批流程";
        String taskName = delegateTask.getName();
        List<String> users = null;
        users = new ArrayList<>();
        users.add("manager");

        switch (taskName){
            case "采购申请":
                break;
            case "部门领导审批":
                // 查询发起人所在部门的领导
                ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
                RuntimeService runtimeService = processEngine.getRuntimeService();
                String piid = delegateTask.getProcessInstanceId();
                Map<String, VariableInstance> map = runtimeService.getVariableInstances(piid);
                String starter = map.get("user").getTextValue();
//                users = userRoleMapper.selectNodeUserToPuchaseLeader(processName, taskName, starter);
                break;
            case "生产部审批":
            case "安环部审批":
            case "动力车间审批":
            case "综管部审批":
            case "供销部汇总采购物品":
//                users = userRoleMapper.selectNodeUserByProcessNode(processName, taskName);
                break;
            default:
        }
        if (users != null) {
            // 节点赋处理人
            delegateTask.addCandidateUsers(users);
        }
    }
}
