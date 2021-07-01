package com.example.springbootactiviti;

import com.example.springbootactiviti.common.util.ActivitiUtil;
import org.activiti.engine.*;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBootActivitiApplicationTests {

	@Test
	public void contextLoads() {
	}

	//部署流程
	@Test
	public void deployProcess(){
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

		RepositoryService repositoryService = processEngine.getRepositoryService();
		DeploymentBuilder builder = repositoryService.createDeployment();
//		builder.disableSchemaValidation();
		builder.addClasspathResource("processes/purchase.bpmn");
		builder.deploy();
	}

	//启动流程
	@Test
	public void startProcess(){
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

		RuntimeService runtimeService = processEngine.getRuntimeService();
		Map<String, Object> map = new HashMap<>();// 参数
		map.put("user", "d861d55aa123b6414e55d936d51cb683");
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("purchase", map);//流程的名称，也可以使用ByID来启动流程
		System.out.println(processInstance);
	}

	@Test
	public void next() {
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

		ActivitiUtil.startProcess("purchase", "d861d55aa123b6414e55d936d51cb683", "test", null);

//		TaskService taskService = processEngine.getTaskService();
//		taskService.complete("12506");

//		ActivitiUtil.claim("30002", "manager");
//		ActivitiUtil.next("27504", null);

//		ActivitiUtil.regression("30002");
	}

}
