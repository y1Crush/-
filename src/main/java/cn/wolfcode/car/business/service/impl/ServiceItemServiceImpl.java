package cn.wolfcode.car.business.service.impl;

import cn.wolfcode.car.business.domain.BpmnInfo;
import cn.wolfcode.car.business.domain.CarPackageAudit;
import cn.wolfcode.car.business.domain.ServiceItem;
import cn.wolfcode.car.business.mapper.ServiceItemMapper;
import cn.wolfcode.car.business.query.ServiceItemQuery;
import cn.wolfcode.car.business.service.IBpmnInfoService;
import cn.wolfcode.car.business.service.ICarPackageAuditService;
import cn.wolfcode.car.business.service.IServiceItemService;
import cn.wolfcode.car.common.base.page.TablePageInfo;
import cn.wolfcode.car.common.exception.BusinessException;
import cn.wolfcode.car.common.util.Convert;
import cn.wolfcode.car.shiro.ShiroUtils;
import com.github.pagehelper.PageHelper;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ServiceItemServiceImpl implements IServiceItemService {

    @Autowired
    private ServiceItemMapper serviceItemMapper;
    @Autowired
    private ICarPackageAuditService carPackageAuditService;
    @Autowired
    private IBpmnInfoService bpmnInfoService;
    @Autowired
    private RuntimeService runtimeService;


    @Override
    public TablePageInfo<ServiceItem> query(ServiceItemQuery qo) {
        PageHelper.startPage(qo.getPageNum(), qo.getPageSize());
        return new TablePageInfo<ServiceItem>(serviceItemMapper.selectForList(qo));
    }

    @Override
    public void save(ServiceItem serviceItem) {
        //判断是否套餐 如果是套餐 需要把审核状态设置初始化
        if (ServiceItem.CARPACKAGE_YES.equals(serviceItem.getCarPackage())) {
            serviceItem.setAuditStatus(ServiceItem.AUDITSTATUS_INIT);//把状态设置为初始化
        } else {
            serviceItem.setAuditStatus(ServiceItem.AUDITSTATUS_NO_REQUIRED);//设置默认的审核状态为无需审核
        }
        serviceItem.setAuditStatus(ServiceItem.SALESTATUS_OFF);//设置默认的销售状态
        serviceItem.setCreateTime(new Date());
        serviceItemMapper.insert(serviceItem);
    }

    @Override
    public ServiceItem get(Long id) {
        return serviceItemMapper.selectByPrimaryKey(id);
    }


    @Override
    public void update(ServiceItem serviceItem) {
        /*
         * 1.已上架 不能直接编辑的 需要下架之后才能进行编辑
         * 2.未上架 但是是套餐且处于审核状态 这时候也是不能进行编辑的
         * 如果是未上架 审核通过了 再重新编辑?
         * 把审核状态修改成初始化
         */
        ServiceItem oldObj = this.get(serviceItem.getId());
        if (ServiceItem.SALESTATUS_ON.equals(oldObj.getSaleStatus())) {
            //说明当前的服务单项处于上架状态 不允许进行修改
            throw new BusinessException("服务单项处于上架状态 不允许进行修改");
        }
        //2.未上架 但是是套餐且处于审核状态 这时候也是不能进行编辑的
        if (ServiceItem.CARPACKAGE_YES.equals(oldObj.getCarPackage()) &&
                ServiceItem.AUDITSTATUS_AUDITING.equals(oldObj.getAuditStatus())
        ) {
            throw new BusinessException("套餐且处于审核状态 这时候也是不能进行编辑的");
        }
        //如果未上架 审核通过了 再重新编辑?
        if (ServiceItem.CARPACKAGE_YES.equals(oldObj.getAuditStatus())
        ) {
            serviceItem.setAuditStatus(ServiceItem.AUDITSTATUS_INIT);
        } else {
            serviceItem.setAuditStatus(oldObj.getAuditStatus());
        }
        serviceItem.setSaleStatus(ServiceItem.SALESTATUS_OFF);
        serviceItemMapper.updateByPrimaryKey(serviceItem);
    }

    @Override
    public void deleteBatch(String ids) {
        Long[] dictIds = Convert.toLongArray(ids);
        for (Long dictId : dictIds) {
            serviceItemMapper.deleteByPrimaryKey(dictId);
        }
    }

    @Override
    public List<ServiceItem> list() {
        return serviceItemMapper.selectAll();
    }

    //上架
    @Override
    public void saleOn(Long id) {
        /*
         * 目前的养修服务单项状态上架
         * 数据处于下架的状态
         * 非套餐---->直接可以上架
         * 对于套餐---->处于审核通过的状态才可以进行上架
         * 2.把状态saleStatus修改成上架状态
         */
        ServiceItem serviceItem = this.get(id);
        if (ServiceItem.SALESTATUS_ON.equals(serviceItem.getSaleStatus())) {
            //目前的养修服务单项状态上架
            throw new BusinessException("服务单项已经处于上架状态");
        }
        if (ServiceItem.CARPACKAGE_YES.equals(serviceItem.getCarPackage()) && //是套餐
                !ServiceItem.AUDITSTATUS_APPROVED.equals(serviceItem.getAuditStatus()) //不是审核通过状态
        ) {
            throw new BusinessException("套餐审核通过才能上架");
        }
        //把状态saleStatus修改成上架状态
        serviceItemMapper.changeSaleStatus(id, ServiceItem.SALESTATUS_ON);
    }

    //下架
    @Override
    public void saleOff(Long id) {
        //把状态saleStatus修改成下架状态
        serviceItemMapper.changeSaleStatus(id, ServiceItem.SALESTATUS_OFF);
    }

    @Override
    @Transactional
    public void startAudit(Long id, Long bpmdInfoId, Long director, Long finance, String info) {
        //1.合理性校验
        ServiceItem serviceItem = this.get(id);
        if (ServiceItem.SALESTATUS_ON.equals(serviceItem.getSaleStatus())//处于上架状态
                || ServiceItem.CARPACKAGE_NO.equals(serviceItem.getCarPackage())//非套餐
                || ServiceItem.AUDITSTATUS_AUDITING.equals(serviceItem.getAuditStatus())//处于审核中
                || ServiceItem.AUDITSTATUS_APPROVED.equals(serviceItem.getAuditStatus())//处于审核通过
                || ServiceItem.AUDITSTATUS_NO_REQUIRED.equals(serviceItem.getAuditStatus())//无需审核
        ) {
            throw new BusinessException("非法的操作");
        }
        CarPackageAudit audit = new CarPackageAudit();
        audit.setCreateTime(new Date());//创建时间
        audit.setAuditorId(director);//审核人
        audit.setBpmnInfoId(bpmdInfoId);//流程定义的ID
        audit.setCreator(ShiroUtils.getUser().getUserName());//创建人当前登录用户
        audit.setInfo(info);//备注信息
        audit.setServiceItemId(id);//服务单项ID
        audit.setServiceItemName(serviceItem.getName());//服务单项名称
        audit.setServiceItemPrice(serviceItem.getDiscountPrice());//服务单项价格
        audit.setServiceItemInfo(serviceItem.getInfo());//服务单项备注信息
        //保存审核对象
        carPackageAuditService.save(audit);
        //启动流程
        //获取BpmnInfo对象
        BpmnInfo bpmnInfo = bpmnInfoService.get(bpmdInfoId);
        Map<String, Object> params = new HashMap<>();
        params.put("discountPrice",serviceItem.getDiscountPrice().longValue());
        params.put("director",director);
        if (finance!=null){
            params.put("finance",finance);
        }
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(bpmnInfo.getActProcessKey(),String.valueOf(audit.getId()),params);
        //审核兑现关联流程实例ID
        audit.setInstanceId(processInstance.getId());
        carPackageAuditService.update(audit);
        //服务单项的审核状态修改
        serviceItemMapper.changeAuditStatus(id,ServiceItem.AUDITSTATUS_AUDITING);
    }

    @Override
    public void changeAuditStatus(Long serviceItemId, Integer status) {
        serviceItemMapper.changeAuditStatus(serviceItemId, status);
    }
}
