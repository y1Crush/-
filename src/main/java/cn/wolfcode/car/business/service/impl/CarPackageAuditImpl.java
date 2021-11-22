package cn.wolfcode.car.business.service.impl;

import cn.wolfcode.car.business.domain.BpmnInfo;
import cn.wolfcode.car.business.domain.CarPackageAudit;
import cn.wolfcode.car.business.domain.ServiceItem;
import cn.wolfcode.car.business.mapper.CarPackageAuditMapper;
import cn.wolfcode.car.business.query.CarPackageAuditQuery;
import cn.wolfcode.car.business.service.IBpmnInfoService;
import cn.wolfcode.car.business.service.ICarPackageAuditService;
import cn.wolfcode.car.business.service.IServiceItemService;
import cn.wolfcode.car.common.base.page.TablePageInfo;
import cn.wolfcode.car.common.exception.BusinessException;
import cn.wolfcode.car.common.util.Convert;
import cn.wolfcode.car.shiro.ShiroUtils;
import com.github.pagehelper.PageHelper;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class CarPackageAuditImpl implements ICarPackageAuditService {

    @Autowired
    private CarPackageAuditMapper carPackageAuditMapper;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private IBpmnInfoService bpmnInfoService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private IServiceItemService serviceItemService;
    @Autowired
    private TaskService taskService;

    @Override
    public TablePageInfo<CarPackageAudit> query(CarPackageAuditQuery qo) {
        PageHelper.startPage(qo.getPageNum(), qo.getPageSize());
        return new TablePageInfo<CarPackageAudit>(carPackageAuditMapper.selectForList(qo));
    }

    @Override
    public void save(CarPackageAudit carPackageAudit) {
        carPackageAudit.setCreateTime(new Date());
        carPackageAuditMapper.insert(carPackageAudit);
    }

    @Override
    public CarPackageAudit get(Long id) {
        return carPackageAuditMapper.selectByPrimaryKey(id);
    }


    @Override
    public void update(CarPackageAudit carPackageAudit) {
        carPackageAuditMapper.updateByPrimaryKey(carPackageAudit);
    }

    @Override
    public void deleteBatch(String ids) {
        Long[] dictIds = Convert.toLongArray(ids);
        for (Long dictId : dictIds) {
            carPackageAuditMapper.deleteByPrimaryKey(dictId);
        }
    }

    @Override
    public List<CarPackageAudit> list() {
        return carPackageAuditMapper.selectAll();
    }

    @Override
    public InputStream getProcessImgAsStream(Long id) {
        //根据id查询审核对象 判断审核对象的状态
        CarPackageAudit audit = this.get(id);
        String instanceId = audit.getInstanceId();//审核对象关联流程实例id
        List<String> highLightedActivities = null;
        if (CarPackageAudit.STATUS_IN_ROGRESS.equals(audit.getStatus())) {
            //获取流程实例对象
            //如果处于审核中 需要高亮目前运行节点
            //获取运行的任务
            highLightedActivities = runtimeService.getActiveActivityIds(instanceId);//根据流程实例ID获取到目前活动节点
        } else {
            //如果是审核通过 审核拒绝 不需要高亮任何节点
            highLightedActivities = new ArrayList<>();
        }
        //根据bpmnInfoId查询bpmnInfo赌侠u那个 对象存储流程定义的id
        BpmnInfo bpmnInfo = bpmnInfoService.get(audit.getBpmnInfoId());
        BpmnModel model = repositoryService.getBpmnModel(bpmnInfo.getActProcessId());
        DefaultProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
        return generator.generateDiagram(model, highLightedActivities, Collections.EMPTY_LIST, "宋体", "宋体", "宋体");
    }

    @Override
    @Transactional
    public void cancelApply(Long id) {
        //合理化校验
        CarPackageAudit audit = this.get(id);
        if (!CarPackageAudit.STATUS_IN_ROGRESS.equals(audit.getStatus())) {
            throw new BusinessException("只有处于审核中的记录才可以撤销");
        }
        //1.把单项的审核状态更新初始化
        Long serviceItemId = audit.getServiceItemId();
        serviceItemService.changeAuditStatus(serviceItemId, ServiceItem.AUDITSTATUS_INIT);
        //2.把流程实例删除
        runtimeService.deleteProcessInstance(audit.getInstanceId(), "流程撤销");
        //3.审核对象的状态修改已撤销
        carPackageAuditMapper.changeStatus(id, CarPackageAudit.STATUS_CANCEL);
    }

    @Override
    public void audit(Long id, Integer auditStatus, String info) {
        //合理化校验
        CarPackageAudit audit = this.get(id);
        //流程处于审批中才可以进行操作
        if (!(CarPackageAudit.STATUS_IN_ROGRESS.equals(audit.getStatus()))) {
            throw new BusinessException("非法操作");
        }
        //只有处理人才可以对这个记录进行审核
        if (!ShiroUtils.getUserId().equals(audit.getAuditorId())) {
            throw new BusinessException("非法操作");
        }
        String username = ShiroUtils.getUser().getUserName();
        if (CarPackageAudit.STATUS_PASS.equals(auditStatus)) {
            //审核通过
            info = "[" + username + "]同意,审批意见:" + info;
        } else {
            //审核拒绝
            info = "[" + username + "]拒绝,审批意见:" + info;
        }
        //根据auditStatus判断同意还是拒绝 需要在info信息中存储对应用户审核意见
        audit.setInfo(audit.getInfo() + "</br>" + info);
        //让Activiti中的流程往后流转
        Task currentTask = taskService.createTaskQuery().processInstanceId(audit.getInstanceId()).singleResult();
        //完成任务前设置流程变量 auditStatus
        taskService.setVariable(currentTask.getId(),"auditStatus",auditStatus);
        //完成任务
        taskService.complete(currentTask.getId());
        //更新审核时间
        audit.setAuditTime(new Date());
        //根据审核通过还是拒绝做不同业务逻辑
        if (CarPackageAudit.STATUS_PASS.equals(auditStatus)) {
            //如果是通过
            //查询是否有下一个节点
            Task nextTask = taskService.createTaskQuery().processInstanceId(audit.getInstanceId()).singleResult();
            if (nextTask == null) {
                //流程走完了 把auditorId设置为空
                audit.setAuditorId(null);
                //如果没有下一个节点 流程走完了 这种情况审核通过了 更新审核对象的审核状态 更新服务单项的审核状态
                audit.setStatus(CarPackageAudit.STATUS_PASS);
                serviceItemService.changeAuditStatus(audit.getServiceItemId(), ServiceItem.AUDITSTATUS_APPROVED);
            } else {
                //如果有下一个节点 获取到下一个结点的审核人Id 更新审核对象的审核人字段
                audit.setAuditorId(Long.parseLong(nextTask.getAssignee()));
            }
        } else {
            //如果是拒绝
            //流程走完了 把auditorId设置为空
            audit.setAuditorId(null);
            //审核对象的状态更新为审核拒绝
            audit.setStatus(CarPackageAudit.STATUS_REJECT);
            //服务单项的审核状态 重新调整
            serviceItemService.changeAuditStatus(audit.getServiceItemId(), ServiceItem.AUDITSTATUS_REPLY);
        }
        //审核对象进行更新
        carPackageAuditMapper.updateByPrimaryKey(audit);
    }
}

