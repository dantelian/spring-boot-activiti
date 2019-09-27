package com.example.springbootactiviti.common.activitiLitener;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;

public class PurchaseGateWayListener implements ExecutionListener {
//    private BuyApplyMapper buyApplyMapper = SpringContextHolder.getBean(BuyApplyMapper.class);
//    private BuyApplyProductMapper buyApplyProductMapper = SpringContextHolder.getBean(BuyApplyProductMapper.class);

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        String name = delegateExecution.getCurrentActivityName();
        String piid = delegateExecution.getProcessInstanceId();
        switch (name) {
            case "是否通过审批":
                delegateExecution.setVariable("access", 0);
                break;
            case "条件审核":
                // 全部通过
                delegateExecution.setVariable("all", 1);
                break;
            case "是否全部通过":
                // 各材料部门全部通过
                delegateExecution.setVariable("allAccess", 1);
                break;
            default:
        }
    }
}
