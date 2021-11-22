package cn.wolfcode.car.business.service.impl;

import cn.wolfcode.car.business.domain.Appointment;
import cn.wolfcode.car.business.domain.Statement;
import cn.wolfcode.car.business.domain.StatementItem;
import cn.wolfcode.car.business.mapper.StatementItemMapper;
import cn.wolfcode.car.business.query.StatementItemQuery;
import cn.wolfcode.car.business.service.IAppointmentService;
import cn.wolfcode.car.business.service.IStatementItemService;
import cn.wolfcode.car.business.service.IStatementService;
import cn.wolfcode.car.common.base.page.TablePageInfo;
import cn.wolfcode.car.common.exception.BusinessException;
import cn.wolfcode.car.shiro.ShiroUtils;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StatementItemImpl implements IStatementItemService {
    @Autowired
    private StatementItemMapper statementItemMapper;
    @Autowired
    private IStatementService statementService;
    @Autowired
    private IAppointmentService appointmentService;
    @Override
    public TablePageInfo<StatementItem> query(StatementItemQuery qo) {
        PageHelper.startPage(qo.getPageNum(),qo.getPageSize());
        return new TablePageInfo<StatementItem>(statementItemMapper.selectForList(qo));
    }

    @Override
    @Transactional
    public void saveItems(List<StatementItem> items) {
        //1.获取最后一条记录
        StatementItem tempItem = items.remove(items.size() - 1);
        //获取前台传入的折扣价格
        BigDecimal disCountPrice = tempItem.getItemPrice();
        //合理性校验--->判断结算单的状态 是否消费中
        Long statementId = tempItem.getStatementId();
        //根据ID获取结算对象
        Statement statement = statementService.get(statementId);
        if (Statement.STATUS_PAID.equals(statement.getStatus())){
            throw new BusinessException("已支付的结算单不能保存明细");
        }
        //插入之前把之前的明细项集合删除
        statementItemMapper.deleteByStatementId(statementId);
        //计算整个明细的总金额和总数量
        BigDecimal totalAmount = new BigDecimal("0.0");
        BigDecimal totalQuantity = new BigDecimal("0");
        for (StatementItem item : items) {
            //把每个结算单明细都保存到数据库中
            statementItemMapper.insert(item);
            //明细 * 单价 ==> 累加到总金额中
            totalAmount = totalAmount.add(item.getItemPrice().multiply(item.getItemQuantity()));
            totalQuantity = totalQuantity.add(item.getItemQuantity());
        }
        //如果折扣金额大于总金额 属于非法操作 抛出异常
        if (disCountPrice.compareTo(totalAmount)>0){
            throw new BusinessException("非法操作,折扣金额大于总金额");
        }
        //更新结算单信息
        statementService.updateAmount(statementId,totalAmount,totalQuantity,disCountPrice);
    }

    @Override
    @Transactional
    public void payStatement(Long statementId) {
        //合理性判断
        Statement statement =statementService.get(statementId);
        if (Statement.STATUS_PAID.equals(statement.getStatus())){
            throw new BusinessException("结算单已支付 不要重复支付");
        }
        //更新结算单状态
        statementService.updatePayStatus(statementId,Statement.STATUS_PAID, ShiroUtils.getUserId());
        if (statement.getAppointmentId()!=null){
            //说明结算单是预约单生成的
            //修改预约单的状态
            appointmentService.updateStatus(statement.getAppointmentId(), Appointment.STATUS_SETTLE);
        }
    }

    @Override
    public void removeByStatementId(Long statementId) {
        statementItemMapper.deleteByStatementId(statementId);
    }
}
